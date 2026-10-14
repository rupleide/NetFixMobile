package com.rupleide.netfix.core.dpibypass

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService.SERVICE_INTERFACE
import android.os.Build
import android.os.ParcelFileDescriptor
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import androidx.lifecycle.lifecycleScope
import com.rupleide.netfix.MainActivity
import com.rupleide.netfix.R
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.ServiceStatus
import com.rupleide.netfix.data.Sender
import com.rupleide.netfix.data.START_ACTION
import com.rupleide.netfix.data.STOP_ACTION
import com.rupleide.netfix.data.RESUME_ACTION
import com.rupleide.netfix.data.PAUSE_ACTION
import com.rupleide.netfix.data.STARTED_BROADCAST
import com.rupleide.netfix.data.STOPPED_BROADCAST
import com.rupleide.netfix.data.FAILED_BROADCAST
import com.rupleide.netfix.data.SENDER
import com.rupleide.netfix.data.setStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class ByeDpiVpnService : LifecycleVpnService() {
    private val byeDpiProxy = ByeDpiProxy()
    private var proxyJob: Job? = null
    private var tunFd: ParcelFileDescriptor? = null
    private val mutex = Mutex()
    private var wakeLock = null as android.os.PowerManager.WakeLock?
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private val isRunning: Boolean
        get() = status == ServiceStatus.Connected

    companion object {
        private val TAG: String = ByeDpiVpnService::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID: Int = NETFIX_NOTIFICATION_ID
        private const val PAUSE_NOTIFICATION_ID: Int = 3
        private const val NOTIFICATION_CHANNEL_ID: String = NETFIX_NOTIFICATION_CHANNEL_ID
        private var status: ServiceStatus = ServiceStatus.Disconnected
        private const val DEFAULT_TUNNEL_DNS = "1.1.1.1"

        val isConnected: Boolean
            get() = status == ServiceStatus.Connected

        @Volatile
        var isAlive: Boolean = false
            private set
    }

    private fun acquireWakeLock() {
    }

    private fun releaseWakeLock() {
    }

    override fun onCreate() {
        super.onCreate()
        isAlive = true
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.vpn_channel_name
        )
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val request = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            private var lastNetwork: android.net.Network? = null
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                if (lastNetwork != null && lastNetwork != network) {
                    if (isRunning) {
                        lifecycleScope.launch {
                            mutex.withLock {
                                try {
                                    switchStrategyForCurrentNetwork()
                                    stopProxy()
                                    stopTun2Socks()
                                    startProxy()
                                    startTun2Socks()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Watchdog reconnect failed", e)
                                }
                            }
                        }
                    }
                }
                lastNetwork = network
            }
            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                if (network == lastNetwork) {
                    lastNetwork = null
                }
            }
        }
        networkCallback = callback
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        isAlive = false
        com.rupleide.netfix.core.tgproxy.TgProxyController.onOwnVpnStateChanged()
        networkCallback?.let {
            val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        tunFd?.close()
        releaseWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground()

        return when (val action = intent?.action) {
            START_ACTION -> {
                lifecycleScope.launch {
                    start()
                }
                START_NOT_STICKY
            }

            STOP_ACTION -> {
                getSharedPreferences(packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("service_enabled", false)
                    .apply()
                lifecycleScope.launch {
                    stop()
                }
                START_NOT_STICKY
            }

            RESUME_ACTION -> {
                lifecycleScope.launch {
                    if (prepare(this@ByeDpiVpnService) == null) {
                        start()
                    }
                }
                START_NOT_STICKY
            }

            PAUSE_ACTION -> {
                lifecycleScope.launch {
                    pause()
                }
                START_NOT_STICKY
            }

            SERVICE_INTERFACE -> {
                Log.i(TAG, "Started by Android")

                lifecycleScope.launch {
                    start()
                }

                START_NOT_STICKY
            }

            else -> {
                Log.w(TAG, "Unknown action: $action")
                lifecycleScope.launch {
                    stop()
                }
                START_NOT_STICKY
            }
        }
    }

    override fun onRevoke() {
        Log.i(TAG, "VPN revoked")
        val tgRunning = com.rupleide.netfix.core.tgproxy.TgProxyController.isRunning
        if (tgRunning) {
            com.rupleide.netfix.core.tgproxy.TgProxyService.startInForeground(this, openTelegram = false, proxyOnly = true)
        }
        com.rupleide.netfix.core.tgproxy.TgProxyReminder.notifyVpnReplaced(this, tgRunning)
        getSharedPreferences(packageName + "_preferences", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("service_enabled", false)
            .apply()
        lifecycleScope.launch { stop() }
    }

    private fun switchStrategyForCurrentNetwork() {
        if (applyStrategyForCurrentNetwork()) {
            val kind = currentNetworkKind()
            Log.i(TAG, "Сеть: ${kind?.displayName}, применяю сохранённую для неё стратегию")
        }
    }

    private suspend fun start() {
        Log.i(TAG, "Starting")

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PAUSE_NOTIFICATION_ID)
        com.rupleide.netfix.core.tgproxy.TgProxyReminder.clearVpnReplaced(this)

        if (status == ServiceStatus.Connected) {
            Log.w(TAG, "VPN already connected")
            updateStatus(ServiceStatus.Connected)
            return
        }

        try {
            val prefs = getSharedPreferences(packageName + "_preferences", android.content.Context.MODE_PRIVATE)
            val economMode = prefs.getBoolean("econom_mode", false)
            if (!economMode) {
                acquireWakeLock()
                com.rupleide.netfix.service.WatchdogWorker.schedulePeriodicWork(this)
            }
            mutex.withLock {
                switchStrategyForCurrentNetwork()
                startProxy()
                startTun2Socks()
                updateStatus(ServiceStatus.Connected)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            updateStatus(ServiceStatus.Failed)
            stop()
        }
    }

    private fun startForeground() {
        val notification: Notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_SERVICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    private suspend fun pause() {
        Log.i(TAG, "Pausing")

        releaseWakeLock()
        com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(this)

        if (status == ServiceStatus.Connected) {
            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        stopProxy()
                        stopTun2Socks()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pause VPN", e)
                }
            }
        }

        updateStatus(ServiceStatus.Disconnected)

        val pausedNotification = createPauseNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.service_paused_text,
            ByeDpiVpnService::class.java,
            createStopAllPendingIntent(this)
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FOREGROUND_SERVICE_ID, pausedNotification)
    }

    private suspend fun stop() {
        Log.i(TAG, "Stopping")

        releaseWakeLock()
        com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(this)

        if (status != ServiceStatus.Connected) {
            Log.w(TAG, "VPN not connected")
            updateStatus(ServiceStatus.Disconnected)
            @Suppress("DEPRECATION")
            stopForeground(true)
            stopSelf()
            return
        }

        mutex.withLock {
            try {
                withContext(Dispatchers.IO) {
                    stopProxy()
                    stopTun2Socks()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop VPN", e)
            }
        }

        updateStatus(ServiceStatus.Disconnected)
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    private fun startProxy() {
        Log.i(TAG, "Starting proxy")

        if (proxyJob != null) {
            Log.w(TAG, "Proxy fields not null")
            throw IllegalStateException("Proxy fields not null")
        }

        val preferences = getByeDpiPreferences()

        proxyJob = lifecycleScope.launch(Dispatchers.IO) {
            val code = byeDpiProxy.startProxy(preferences)

            delay(500)

            if (code != 0) {
                Log.e(TAG, "Proxy stopped with code $code")
                updateStatus(ServiceStatus.Failed)
                stopTun2Socks()
                stopSelf()
            }
        }

        Log.i(TAG, "Proxy started")
    }

    private suspend fun stopProxy() {
        Log.i(TAG, "Stopping proxy")

        if (status == ServiceStatus.Disconnected) {
            Log.w(TAG, "Proxy already disconnected")
            return
        }

        try {
            byeDpiProxy.stopProxy()
            proxyJob?.cancel()

            val completed = withTimeoutOrNull(2000) {
                proxyJob?.join()
                true
            }

            if (completed == null) {
                Log.w(TAG, "proxy not finish in time, cancelling...")
                byeDpiProxy.jniForceClose()
            }

            proxyJob = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close proxyJob", e)
        }

        Log.i(TAG, "Proxy stopped")
    }

    private fun startTun2Socks() {
        Log.i(TAG, "Starting tun2socks")

        if (tunFd != null) {
            Log.w(TAG, "VPN field not null")
            throw IllegalStateException("VPN field not null")
        }

        val sharedPreferences = getPreferences()
        val (ip, port) = sharedPreferences.getProxyIpAndPort()

        val dnsIps = resolveTunnelDnsServers()
        val ipv6 = sharedPreferences.getBoolean("ipv6_enable", false)

        val tun2socksConfig = buildString {
            appendLine("tunnel:")
            appendLine("  mtu: 8500")

            appendLine("misc:")
            appendLine("  task-stack-size: 81920")

            appendLine("socks5:")
            appendLine("  address: $ip")
            appendLine("  port: $port")
            appendLine("  udp: udp")
        }

        val configPath = try {
            File.createTempFile("config", "tmp", cacheDir).apply {
                writeText(tun2socksConfig)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create config file", e)
            throw e
        }

        val isSmartTv = sharedPreferences.getBoolean("is_smart_tv", false)

        val fd = createBuilder(dnsIps, ipv6).establish()
            ?: throw IllegalStateException("VPN connection failed")

        this.tunFd = fd

        TProxyService.TProxyStartService(configPath.absolutePath, fd.fd, isSmartTv)

        Log.i(TAG, "Tun2Socks started. ip: $ip port: $port")
    }

    private fun stopTun2Socks() {
        Log.i(TAG, "Stopping tun2socks")

        if (tunFd == null) {
            Log.w(TAG, "VPN field is null, skipping")
            return
        }

        try {
            TProxyService.TProxyStopService()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop TProxyService", e)
        }

        try {
            File(cacheDir, "config.tmp").delete()
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to delete config file", e)
        }

        try {
            tunFd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close tunFd", e)
        } finally {
            tunFd = null
        }

        Log.i(TAG, "Tun2socks stopped")
    }

    private fun getByeDpiPreferences(): ByeDpiProxyPreferences =
        ByeDpiProxyPreferences.fromSharedPreferences(getPreferences(), this)

    private fun updateStatus(newStatus: ServiceStatus) {
        Log.d(TAG, "VPN status changed from $status to $newStatus")

        status = newStatus

        setStatus(
            when (newStatus) {
                ServiceStatus.Connected -> AppStatus.Running

                ServiceStatus.Disconnected,
                ServiceStatus.Failed -> {
                    proxyJob = null
                    AppStatus.Halted
                }
            },
            Mode.VPN
        )

        val intent = Intent(
            when (newStatus) {
                ServiceStatus.Connected -> STARTED_BROADCAST
                ServiceStatus.Disconnected -> STOPPED_BROADCAST
                ServiceStatus.Failed -> FAILED_BROADCAST
            }
        )
        intent.putExtra(SENDER, Sender.VPN.ordinal)
        com.rupleide.netfix.service.NetFixTileService.requestRefresh(this)
        sendBroadcast(intent.setPackage(packageName))
    }

    private fun createNotification(): Notification = createNetFixNotification(this)

    private fun createNotificationPause() {
        val notification = createPauseNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.service_paused_text,
            ByeDpiVpnService::class.java
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PAUSE_NOTIFICATION_ID, notification)
    }

    private fun resolveTunnelDnsServers(): List<String> {
        val isPrivateDnsConfigured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val linkProperties = activeNetwork?.let { connectivityManager.getLinkProperties(it) }
                !linkProperties?.privateDnsServerName.isNullOrBlank()
            } catch (e: SecurityException) {
                false
            }
        } else {
            false
        }
        return if (isPrivateDnsConfigured) emptyList() else listOf(DEFAULT_TUNNEL_DNS)
    }

    private fun createBuilder(dnsIps: List<String>, ipv6: Boolean): Builder {
        Log.d(TAG, "DNS: $dnsIps")
        val builder = Builder()
        val preferences = getPreferences()
        builder.setSession("NetFix")
        builder.setConfigureIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        builder.addAddress("10.10.10.10", 32)
            .addRoute("0.0.0.0", 0)

        if (ipv6) {
            builder.addAddress("fd00::1", 128)
                .addRoute("::", 0)
        }

        dnsIps.forEach { ip ->
            builder.addDnsServer(ip)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        when (preferences.appListMode()) {
            AppListMode.SelectedAppsOnly -> {
                for (packageName in preferences.selectedAppsWithDefaults()) {
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to whitelist app $packageName", e)
                    }
                }
            }
            AppListMode.AllTraffic -> {
                builder.addDisallowedApplication(applicationContext.packageName)
            }
        }

        return builder
    }
}
