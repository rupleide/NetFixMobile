package com.rupleide.netfix.core.tgproxy

import android.content.Context
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.Executors

object TgProxyController {
    const val DEFAULT_PORT = 1443
    const val DEFAULT_BIND_IP = "127.0.0.1"
    private const val NATIVE_ALREADY_RUNNING = -1
    private const val NATIVE_BIND_FAILED = -3
    private const val NEARBY_PORT_ATTEMPTS = 20
    const val KEY_PORT_MOVED_FROM = "tgproxy_port_moved_from"
    private const val OWN_VPN_SETTLE_MS = 2_000L
    private const val WATCHDOG_INTERVAL_MS = 30_000L

    private val nativeExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tgproxy-native").apply { isDaemon = true }
    }

    @Volatile
    var isRunning: Boolean = false
        private set

    private fun generateSecret(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getSharedPrefs(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun getOrGenerateSecret(context: Context): String {
        val prefs = getSharedPrefs(context)
        var secret = prefs.getString("secret_key", "") ?: ""
        if (secret.isEmpty()) {
            secret = generateSecret()
            prefs.edit().putString("secret_key", secret).apply()
        }
        return secret
    }

    fun regenerateSecret(context: Context): String {
        val secret = generateSecret()
        getSharedPrefs(context).edit()
            .putString("secret_key", secret)
            .putBoolean("tg_proxy_configured", false)
            .apply()
        return secret
    }

    fun isAutostartEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean("autostart", false)
    }

    fun setAutostartEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean("autostart", enabled).apply()
    }

    fun getDcIps(context: Context): String {
        return getSharedPrefs(context).getString("tgproxy_dc_ips", "") ?: ""
    }

    fun setDcIps(context: Context, value: String) {
        getSharedPrefs(context).edit().putString("tgproxy_dc_ips", value).apply()
    }

    fun getPort(context: Context): Int {
        return getSharedPrefs(context).getInt("tgproxy_port", DEFAULT_PORT)
    }

    fun setPort(context: Context, port: Int) {
        getSharedPrefs(context).edit().putInt("tgproxy_port", port).apply()
    }

    fun getPoolSize(context: Context): Int {
        return getSharedPrefs(context).getInt("tgproxy_pool_size", 4)
    }

    fun setPoolSize(context: Context, size: Int) {
        getSharedPrefs(context).edit().putInt("tgproxy_pool_size", size).apply()
    }

    fun isCfEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean("tgproxy_cf_enabled", true)
    }

    fun setCfEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean("tgproxy_cf_enabled", enabled).apply()
    }

    fun isCfPriority(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean("tgproxy_cf_priority", true)
    }

    const val KEY_CF_PINNED_DOMAIN = "tgproxy_cf_pinned_domain"

    fun pinnedCfDomain(context: Context): String? =
        getSharedPrefs(context).getString(KEY_CF_PINNED_DOMAIN, null)?.takeIf { it.isNotBlank() }

    fun pinCfDomain(context: Context, domain: String?) {
        val appContext = context.applicationContext
        val editor = getSharedPrefs(appContext).edit()
        if (domain.isNullOrBlank()) editor.remove(KEY_CF_PINNED_DOMAIN) else editor.putString(KEY_CF_PINNED_DOMAIN, domain)
        editor.apply()
        Log.i("TgProxyController", if (domain.isNullOrBlank()) "Серверы Cloudflare: выбираются автоматически" else "Сервер Cloudflare закреплён: $domain")
        if (!isRunning) return
        nativeExecutor.execute {
            try {
                NativeProxy.setCfProxyConfig(isCfEnabled(appContext), isCfPriority(appContext), pinnedCfDomain(appContext).orEmpty())
            } catch (e: Throwable) {
                Log.e("TgProxyController", "Не удалось применить сервер Cloudflare", e)
            }
        }
        restartCore(appContext)
    }

    fun setCfPriority(context: Context, priority: Boolean) {
        getSharedPrefs(context).edit().putBoolean("tgproxy_cf_priority", priority).apply()
    }

    fun getTgProxyUrl(bindIp: String, port: Int, secret: String): String {
        return "tg://proxy?server=$bindIp&port=$port&secret=dd$secret"
    }

    fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun measureLatency(host: String, port: Int): Int {
        val startTime = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
                (System.currentTimeMillis() - startTime).toInt()
            }
        } catch (e: Exception) {
            -1
        }
    }

    fun startAsync(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        if (com.rupleide.netfix.core.beta.BetaExpiry.isExpiredNow(context)) {
            Log.w("TgProxyController", "Срок тестовой версии истёк, TG-прокси не запускается")
            onError()
            return
        }
        val secretKey = getOrGenerateSecret(context)
        val cacheDir = context.cacheDir.absolutePath
        val dcIps = getDcIps(context)
        var port = getPort(context)
        val poolSize = getPoolSize(context)
        val cfEnabled = isCfEnabled(context)
        val cfPriority = isCfPriority(context)

        Log.i("TgProxyController", "Запуск TG-прокси на порту $port (pool: $poolSize, cf: $cfEnabled, priority: $cfPriority, dc: \"$dcIps\")")

        val appContext = context.applicationContext

        nativeExecutor.execute {
            val started = try {
                NativeProxy.setPoolSize(poolSize)
                NativeProxy.setCfProxyCacheDir(cacheDir)
                NativeProxy.setCfProxyConfig(cfEnabled, cfPriority, pinnedCfDomain(appContext).orEmpty())
                var code = NativeProxy.startProxy(DEFAULT_BIND_IP, port, dcIps, secretKey, 1)
                if (code == NATIVE_ALREADY_RUNNING && !isRunning) {
                    Log.w("TgProxyController", "Нативное ядро уже запущено, но не принадлежит нам: перезапускаю")
                    NativeProxy.stopProxy()
                    code = NativeProxy.startProxy(DEFAULT_BIND_IP, port, dcIps, secretKey, 1)
                }
                if (code == NATIVE_BIND_FAILED && CompatibilityManager.freeProxyPortEnabled(getSharedPrefs(appContext))) {
                    val busyPort = port
                    val nearbyFreePort = findNearbyFreePort(busyPort)
                    if (nearbyFreePort != null) {
                        Log.w("TgProxyController", "Порт TG-прокси $busyPort занят, пробую $nearbyFreePort")
                        code = NativeProxy.startProxy(DEFAULT_BIND_IP, nearbyFreePort, dcIps, secretKey, 1)
                        if (code == 0) {
                            port = nearbyFreePort
                            getSharedPrefs(appContext).edit()
                                .putInt("tgproxy_port", nearbyFreePort)
                                .putInt(KEY_PORT_MOVED_FROM, busyPort)
                                .apply()
                        }
                    }
                }
                Log.i("TgProxyController", "StartProxy вернул код $code")
                code == 0 || (code == NATIVE_ALREADY_RUNNING && isRunning)
            } catch (e: Throwable) {
                Log.e("TgProxyController", "Error in startProxy", e)
                false
            }

            if (started) {
                Log.i("TgProxyController", "Порт TG-прокси $port успешно открыт")
                isRunning = true
                startForeignVpnMonitor(appContext)
                startScreenMonitor(appContext)
                startWatchdog(appContext)
                com.rupleide.netfix.service.NetFixTileService.requestRefresh(appContext)
                onSuccess()
            } else {
                Log.e("TgProxyController", "Не удалось запустить TG-прокси на порту $port")
                onError()
            }
        }
    }

    private fun findNearbyFreePort(busyPort: Int): Int? =
        (1..NEARBY_PORT_ATTEMPTS)
            .map { busyPort + it }
            .filter { it <= 65535 }
            .firstOrNull { com.rupleide.netfix.core.dpibypass.ProxyPortResolver.isPortFree(DEFAULT_BIND_IP, it) }

    fun dismissPortMovedHint(context: Context) {
        getSharedPrefs(context).edit().remove(KEY_PORT_MOVED_FROM).apply()
    }

    private val recoveryPlanner = TgProxyRecoveryPlanner()

    @Volatile
    private var watchdogThread: Thread? = null

    @Synchronized
    private fun startWatchdog(appContext: Context) {
        if (watchdogThread?.isAlive == true) return
        val thread = Thread({
            while (isRunning && !Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(WATCHDOG_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }
                if (isRunning) syncPowerSaveWithScreen(appContext)
                if (!isRunning || !CompatibilityManager.tgProxyWatchdogEnabled(getSharedPrefs(appContext))) continue
                val sample = try {
                    NativeProxy.getStats()?.let { TgProxyStatsSample.parse(it) }
                } catch (_: Throwable) {
                    null
                } ?: continue
                val action = synchronized(recoveryPlanner) { recoveryPlanner.onSample(sample, System.currentTimeMillis()) }
                when (action) {
                    TgProxyRecoveryAction.None -> Unit
                    TgProxyRecoveryAction.FlushPools -> {
                        Log.w("TgProxyController", "Прокси не пропускает трафик: сбрасываю пул соединений")
                        NativeProxy.flushPools()
                    }
                    TgProxyRecoveryAction.RestartCore -> {
                        if (pinnedCfDomain(appContext) != null) {
                            Log.w("TgProxyController", "Прокси завис с закреплённым сервером Cloudflare: возвращаю автоматический выбор")
                            pinCfDomain(appContext, null)
                        } else {
                            Log.w("TgProxyController", "Прокси по-прежнему завис: перезапускаю ядро")
                            restartCore(appContext)
                        }
                    }
                }
            }
        }, "tgproxy-watchdog")
        thread.isDaemon = true
        watchdogThread = thread
        thread.start()
    }

    private fun restartCore(appContext: Context) {
        nativeExecutor.execute {
            try {
                NativeProxy.stopProxy()
                val code = NativeProxy.startProxy(
                    DEFAULT_BIND_IP,
                    getPort(appContext),
                    getDcIps(appContext),
                    getOrGenerateSecret(appContext),
                    1
                )
                Log.i("TgProxyController", "Перезапуск ядра TG-прокси вернул код $code")
            } catch (e: Throwable) {
                Log.e("TgProxyController", "Не удалось перезапустить ядро TG-прокси", e)
            }
        }
    }

    @Volatile
    private var isRoutingForForeignVpn = false

    @Volatile
    private var defaultNetworkHasVpn = false
    private var foreignVpnMonitor: android.net.ConnectivityManager.NetworkCallback? = null
    private var foreignVpnMonitorContext: Context? = null

    fun onOwnVpnStateChanged() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val appContext = foreignVpnMonitorContext ?: return@postDelayed
            applyRoutingForForeignVpn(appContext, defaultNetworkHasVpn && !ByeDpiVpnService.isAlive)
        }, OWN_VPN_SETTLE_MS)
    }

    @Synchronized
    private fun applyRoutingForForeignVpn(context: Context, isForeignVpnActive: Boolean) {
        if (isForeignVpnActive == isRoutingForForeignVpn) return
        isRoutingForForeignVpn = isForeignVpnActive
        if (isForeignVpnActive) {
            Log.i("TgProxyController", "Активен сторонний VPN: TG-прокси идёт напрямую, без Cloudflare")
            NativeProxy.setCfProxyConfig(false, false, "")
        } else {
            Log.i("TgProxyController", "Сторонний VPN отключён: возвращаю настройки Cloudflare")
            NativeProxy.setCfProxyConfig(isCfEnabled(context), isCfPriority(context), pinnedCfDomain(context).orEmpty())
        }
    }

    @Synchronized
    private fun startForeignVpnMonitor(appContext: Context) {
        if (foreignVpnMonitor != null) return
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val monitor = object : android.net.ConnectivityManager.NetworkCallback() {
            private var lastNetwork: android.net.Network? = null

            override fun onAvailable(network: android.net.Network) {
                val isVpnNetwork = connectivityManager.getNetworkCapabilities(network)
                    ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
                if (isVpnNetwork) return
                val previous = lastNetwork
                lastNetwork = network
                if (previous != null && previous != network) {
                    Log.i("TgProxyController", "Сеть сменилась: сбрасываю пул заранее открытых соединений")
                    NativeProxy.flushPools()
                    synchronized(recoveryPlanner) { recoveryPlanner.onNetworkChanged() }
                }
            }

            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                defaultNetworkHasVpn = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
                applyRoutingForForeignVpn(appContext, defaultNetworkHasVpn && !ByeDpiVpnService.isAlive)
            }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(monitor)
            foreignVpnMonitor = monitor
            foreignVpnMonitorContext = appContext
        } catch (e: Exception) {
            Log.w("TgProxyController", "Не удалось следить за сетью", e)
        }
    }

    @Synchronized
    private fun stopForeignVpnMonitor() {
        val monitor = foreignVpnMonitor ?: return
        val appContext = foreignVpnMonitorContext ?: return
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        try {
            connectivityManager.unregisterNetworkCallback(monitor)
        } catch (_: Exception) {
        }
        foreignVpnMonitor = null
        foreignVpnMonitorContext = null
        isRoutingForForeignVpn = false
        defaultNetworkHasVpn = false
    }

    private var screenMonitor: android.content.BroadcastReceiver? = null
    private var screenMonitorContext: Context? = null

    @Volatile
    private var powerSaveApplied: Boolean? = null

    private fun syncPowerSaveWithScreen(appContext: Context) {
        val power = appContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        applyPowerSave(appContext, screenOn = power.isInteractive)
    }

    private fun applyPowerSave(appContext: Context, screenOn: Boolean) {
        val saving = !screenOn && isRunning && CompatibilityManager.batterySaverEnabled(getSharedPrefs(appContext))
        if (powerSaveApplied == saving) return
        powerSaveApplied = saving
        nativeExecutor.execute {
            try {
                NativeProxy.setPowerSave(saving)
            } catch (e: Throwable) {
                Log.w("TgProxyController", "Не удалось переключить экономию батареи", e)
            }
        }
        Log.i("TgProxyController", if (saving) "Экран выключен: прокси не держит запасные соединения" else "Экран включён: прокси держит запасные соединения")
    }

    @Synchronized
    private fun startScreenMonitor(appContext: Context) {
        if (screenMonitor != null) return
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: android.content.Intent) {
                when (intent.action) {
                    android.content.Intent.ACTION_SCREEN_OFF -> applyPowerSave(appContext, screenOn = false)
                    android.content.Intent.ACTION_SCREEN_ON -> applyPowerSave(appContext, screenOn = true)
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
            addAction(android.content.Intent.ACTION_SCREEN_ON)
        }
        try {
            androidx.core.content.ContextCompat.registerReceiver(appContext, receiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            screenMonitor = receiver
            screenMonitorContext = appContext
            val power = appContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!power.isInteractive) applyPowerSave(appContext, screenOn = false)
        } catch (e: Exception) {
            Log.w("TgProxyController", "Не удалось следить за экраном", e)
        }
    }

    @Synchronized
    private fun stopScreenMonitor() {
        val receiver = screenMonitor ?: return
        try {
            screenMonitorContext?.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        screenMonitor = null
        screenMonitorContext = null
    }

    fun restartAsync(context: Context, onDone: (Boolean) -> Unit = {}) {
        if (!isRunning) {
            onDone(false)
            return
        }
        Log.i("TgProxyController", "Перезапуск TG-прокси с новыми настройками")
        stop()
        startAsync(context.applicationContext, onSuccess = { onDone(true) }, onError = { onDone(false) })
    }

    fun stop() {
        Log.i("TgProxyController", "Остановка TG-прокси")
        isRunning = false
        watchdogThread?.interrupt()
        watchdogThread = null
        synchronized(recoveryPlanner) { recoveryPlanner.onNetworkChanged() }
        stopForeignVpnMonitor()
        stopScreenMonitor()
        powerSaveApplied = null
        nativeExecutor.execute {
            try {
                NativeProxy.setPowerSave(false)
                NativeProxy.stopProxy()
            } catch (e: Throwable) {
                Log.w("TgProxyController", "StopProxy failed", e)
            }
        }
    }
}
