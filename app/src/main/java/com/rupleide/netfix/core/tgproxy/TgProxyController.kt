package com.rupleide.netfix.core.tgproxy

import android.content.Context
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
    private const val OWN_VPN_SETTLE_MS = 2_000L

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
        val secretKey = getOrGenerateSecret(context)
        val cacheDir = context.cacheDir.absolutePath
        val dcIps = getDcIps(context)
        val port = getPort(context)
        val poolSize = getPoolSize(context)
        val cfEnabled = isCfEnabled(context)
        val cfPriority = isCfPriority(context)

        Log.i("TgProxyController", "Запуск TG-прокси на порту $port (pool: $poolSize, cf: $cfEnabled, priority: $cfPriority, dc: \"$dcIps\")")

        val appContext = context.applicationContext

        nativeExecutor.execute {
            val started = try {
                NativeProxy.setPoolSize(poolSize)
                NativeProxy.setCfProxyCacheDir(cacheDir)
                NativeProxy.setCfProxyConfig(cfEnabled, cfPriority, "")
                var code = NativeProxy.startProxy(DEFAULT_BIND_IP, port, dcIps, secretKey, 1)
                if (code == NATIVE_ALREADY_RUNNING && !isRunning) {
                    Log.w("TgProxyController", "Нативное ядро уже запущено, но не принадлежит нам: перезапускаю")
                    NativeProxy.stopProxy()
                    code = NativeProxy.startProxy(DEFAULT_BIND_IP, port, dcIps, secretKey, 1)
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
                com.rupleide.netfix.service.NetFixTileService.requestRefresh(appContext)
                onSuccess()
            } else {
                Log.e("TgProxyController", "Не удалось запустить TG-прокси на порту $port")
                onError()
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
            NativeProxy.setCfProxyConfig(isCfEnabled(context), isCfPriority(context), "")
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

    fun stop() {
        Log.i("TgProxyController", "Остановка TG-прокси")
        isRunning = false
        stopForeignVpnMonitor()
        nativeExecutor.execute {
            try {
                NativeProxy.stopProxy()
            } catch (e: Throwable) {
                Log.w("TgProxyController", "StopProxy failed", e)
            }
        }
    }
}
