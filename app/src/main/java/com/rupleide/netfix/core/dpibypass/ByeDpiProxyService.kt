package com.rupleide.netfix.core.dpibypass

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rupleide.netfix.R
import com.rupleide.netfix.MainActivity
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

class ByeDpiProxyService : LifecycleService() {
    private var proxy = ByeDpiProxy()
    private var proxyJob: Job? = null
    private val mutex = Mutex()

    companion object {
        private val TAG: String = ByeDpiProxyService::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID: Int = 2
        private const val PAUSE_NOTIFICATION_ID: Int = 3
        private const val NOTIFICATION_CHANNEL_ID: String = "ByeDPI Proxy"
        private var status: ServiceStatus = ServiceStatus.Disconnected
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.proxy_channel_name,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground()

        return when (val action = intent?.action) {
            START_ACTION -> {
                lifecycleScope.launch {
                    start()
                }
                START_STICKY
            }

            STOP_ACTION -> {
                lifecycleScope.launch {
                    stop()
                }
                START_NOT_STICKY
            }

            RESUME_ACTION -> {
                lifecycleScope.launch {
                    start()
                }
                START_STICKY
            }

            PAUSE_ACTION -> {
                lifecycleScope.launch {
                    stop()
                    createNotificationPause()
                }
                START_NOT_STICKY
            }

            else -> {
                Log.w(TAG, "Unknown action: $action")
                START_NOT_STICKY
            }
        }
    }

    private suspend fun start() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PAUSE_NOTIFICATION_ID)

        if (status == ServiceStatus.Connected) {
            updateStatus(ServiceStatus.Connected)
            return
        }

        try {
            mutex.withLock {
                startProxy()
                updateStatus(ServiceStatus.Connected)
            }
        } catch (e: Exception) {
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
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    private suspend fun stop() {
        if (status != ServiceStatus.Connected) {
            updateStatus(ServiceStatus.Disconnected)
            return
        }

        mutex.withLock {
            withContext(Dispatchers.IO) {
                stopProxy()
            }
        }

        updateStatus(ServiceStatus.Disconnected)
        stopSelf()
    }

    private fun startProxy() {
        if (proxyJob != null) {
            throw IllegalStateException("Proxy fields not null")
        }

        proxy = ByeDpiProxy()
        val preferences = getByeDpiPreferences()

        proxyJob = lifecycleScope.launch(Dispatchers.IO) {
            val code = proxy.startProxy(preferences)

            delay(500)

            if (code != 0) {
                updateStatus(ServiceStatus.Failed)
                stopSelf()
            }
        }
    }

    private suspend fun stopProxy() {
        if (status == ServiceStatus.Disconnected) {
            return
        }

        try {
            proxy.stopProxy()
            proxyJob?.cancel()

            val completed = withTimeoutOrNull(2000) {
                proxyJob?.join()
                true
            }

            if (completed == null) {
                proxy.jniForceClose()
            }

            proxyJob = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop proxy", e)
        }
    }

    private fun getByeDpiPreferences(): ByeDpiProxyPreferences =
        ByeDpiProxyPreferences.fromSharedPreferences(getPreferences(), this)

    private fun updateStatus(newStatus: ServiceStatus) {
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
            Mode.Proxy
        )

        val intent = Intent(
            when (newStatus) {
                ServiceStatus.Connected -> STARTED_BROADCAST
                ServiceStatus.Disconnected -> STOPPED_BROADCAST
                ServiceStatus.Failed -> FAILED_BROADCAST
            }
        )
        intent.putExtra(SENDER, Sender.Proxy.ordinal)
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification =
        createConnectionNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.vpn_notification_content,
            ByeDpiProxyService::class.java,
        )

    private fun createNotificationPause() {
        val notification = createPauseNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.service_paused_text,
            ByeDpiProxyService::class.java,
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PAUSE_NOTIFICATION_ID, notification)
    }
}
