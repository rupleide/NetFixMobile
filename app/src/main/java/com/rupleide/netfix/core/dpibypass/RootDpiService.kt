package com.rupleide.netfix.core.dpibypass

import android.content.Intent
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rupleide.netfix.R
import com.rupleide.netfix.data.*
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.net.Socket

/** Root only owns the firewall; the existing ByeDPI engine stays in the app sandbox. */
class RootDpiService : LifecycleService() {
    private val mutex = Mutex()
    private val proxy = ByeDpiProxy()
    private var proxyJob: Job? = null
    private var root: RootDpiSession? = null
    private var monitor: Job? = null
    @Volatile private var shouldRun = false
    private var interfaces = emptySet<String>()
    private lateinit var connectivity: ConnectivityManager
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNetwork()
        override fun onLost(network: Network) = refreshNetwork()
        override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) = refreshNetwork()
    }

    companion object {
        private const val PORT = 11080
        @Volatile var isAlive = false
            private set
        @Volatile var isConnected = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isAlive = true
        connectivity = getSystemService(ConnectivityManager::class.java)
        registerNotificationChannel(this, NETFIX_NOTIFICATION_CHANNEL_ID, R.string.vpn_channel_name)
        connectivity.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(), callback
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification = createNetFixNotification(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NETFIX_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(NETFIX_NOTIFICATION_ID, notification)
        lifecycleScope.launch {
            mutex.withLock {
                try {
                    when (intent?.action) {
                        START_ACTION, RESUME_ACTION -> {
                            getSystemService(NotificationManager::class.java).cancel(3)
                            shouldRun = true
                            if (!isConnected) start()
                        }
                        PAUSE_ACTION -> {
                            shouldRun = false
                            stop()
                            getSystemService(NotificationManager::class.java).notify(3,
                                createPauseNotification(this@RootDpiService, NETFIX_NOTIFICATION_CHANNEL_ID,
                                    R.string.notification_title, R.string.service_paused_text, RootDpiService::class.java))
                            stopSelf()
                        }
                        else -> { shouldRun = false; stop(); stopSelf() }
                    }
                } catch (e: CancellationException) {
                    stop()
                    throw e
                } catch (e: Exception) {
                    fail(e)
                }
            }
        }
        // EOF on the guardian pipe cleans up after process death; restart requires a fresh start.
        return START_NOT_STICKY
    }

    private fun physicalInterfaces(): Set<String> = connectivity.allNetworks.mapNotNull { network ->
        val capabilities = connectivity.getNetworkCapabilities(network)
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            connectivity.getLinkProperties(network)?.interfaceName
        } else null
    }.flatMap { iface ->
        // Android's CLAT interface carries IPv4 on IPv6-only mobile networks.
        listOf(iface, "v4-$iface").filter { it.length <= 15 }
    }.toSet()

    private suspend fun start() {
        check(!ByeDpiVpnService.isAlive) { "Stop NetFix VPN before starting root mode" }
        val preferences = getPreferences()
        check(preferences.appListMode() == AppListMode.SelectedAppsOnly) {
            "Root mode requires selected applications"
        }
        val uids = preferences.selectedAppsWithDefaults().mapNotNull { packageName ->
            try { packageManager.getApplicationInfo(packageName, 0).uid }
            catch (_: android.content.pm.PackageManager.NameNotFoundException) { null }
        }.filter { it != Process.myUid() }.toSet()
        check(uids.isNotEmpty()) { "Select at least one installed application" }
        interfaces = physicalInterfaces()
        if (interfaces.isEmpty()) {
            updateStatus(false)
            return
        }
        val rules = RootDpiRules(Process.myUid(), uids, interfaces, PORT)
        applyStrategyForCurrentNetwork()
        val preferencesArgs = ByeDpiProxyPreferences.fromSharedPreferences(preferences, this)
        val args = when (preferencesArgs) {
            is ByeDpiProxyCmdPreferences -> preferencesArgs.args
            is ByeDpiProxyUIPreferences -> preferencesArgs.uiargs
        }
        require("--" !in args) { "Root mode does not support an argument terminator" }
        // Imported strategy arguments never enter a root shell. Final options fix the listener.
        val rootArgs = args + arrayOf("--ip", "127.0.0.1", "--port", PORT.toString(), "--transparent")
        proxyJob = lifecycleScope.launch(Dispatchers.IO) {
            val result = proxy.startProxy(ByeDpiProxyCmdPreferences(rootArgs))
            if (isConnected) lifecycleScope.launch(Dispatchers.Main) {
                mutex.withLock { fail(IllegalStateException("ByeDPI stopped: $result")) }
            }
        }
        withContext(Dispatchers.IO) {
            withTimeout(5000) {
                do {
                    delay(100)
                    check(proxyJob?.isActive == true) { "ByeDPI failed to start" }
                    val ready = try {
                        Socket().use { it.connect(InetSocketAddress("127.0.0.1", PORT), 100); true }
                    } catch (_: java.io.IOException) { false }
                } while (!ready)
            }
        }
        root = RootDpiSession.open(this, rules)
        check(proxyJob?.isActive == true) { "ByeDPI stopped during firewall setup" }
        updateStatus(true)
        if (!preferences.getBoolean("econom_mode", false)) {
            com.rupleide.netfix.service.WatchdogWorker.schedulePeriodicWork(this)
        }
        monitor = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                if (root?.isAlive != true || proxyJob?.isActive != true) {
                    lifecycleScope.launch {
                        mutex.withLock { fail(IllegalStateException("Root DPI process stopped")) }
                    }
                    break
                }
            }
        }
    }

    private suspend fun stop() = withContext(NonCancellable) {
        isConnected = false
        com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(this@RootDpiService)
        monitor?.cancel()
        monitor = null
        // Remove interception before shutting the listener down.
        try { root?.close() } finally {
            root = null
            proxy.stopProxy()
            val job = proxyJob
            job?.cancel()
            if (withTimeoutOrNull(2000) { job?.join(); true } != true) {
                proxy.jniForceClose()
                check(withTimeoutOrNull(2000) { job?.join(); true } == true) { "ByeDPI did not stop" }
            }
            proxyJob = null
            updateStatus(false)
        }
    }

    private suspend fun fail(error: Exception) {
        shouldRun = false
        getPreferences().edit().putBoolean("service_enabled", false).apply()
        Log.e("RootDpiService", "Root mode failed", error)
        try { stop() } catch (cleanup: Exception) { Log.e("RootDpiService", "Cleanup failed", cleanup) }
        Toast.makeText(this, "Root: ${error.message}", Toast.LENGTH_LONG).show()
        sendBroadcast(Intent(FAILED_BROADCAST).putExtra(SENDER, Sender.VPN.ordinal).setPackage(packageName))
        stopSelf()
    }

    private fun refreshNetwork() {
        if (!shouldRun) return
        lifecycleScope.launch {
            mutex.withLock {
                if (shouldRun && (physicalInterfaces() != interfaces || applyStrategyForCurrentNetwork())) {
                    try { stop(); start() }
                    catch (e: CancellationException) { stop(); throw e }
                    catch (e: Exception) { fail(e) }
                }
            }
        }
    }

    private fun updateStatus(connected: Boolean) {
        isConnected = connected
        setStatus(if (connected) AppStatus.Running else AppStatus.Halted, Mode.VPN)
        sendBroadcast(Intent(if (connected) STARTED_BROADCAST else STOPPED_BROADCAST)
            .putExtra(SENDER, Sender.VPN.ordinal).setPackage(packageName))
        com.rupleide.netfix.service.NetFixTileService.requestRefresh(this)
    }

    override fun onDestroy() {
        connectivity.unregisterNetworkCallback(callback)
        // Closing the pipe triggers root-side cleanup even when this scope is cancelled.
        try { root?.disconnect() } catch (e: Exception) { Log.e("RootDpiService", "Closing root pipe failed", e) }
        root = null
        monitor?.cancel()
        proxy.stopProxy()
        proxyJob?.cancel()
        updateStatus(false)
        isAlive = false
        super.onDestroy()
    }
}
