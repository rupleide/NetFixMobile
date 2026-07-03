package com.rupleide.netfix.core.tgproxy

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rupleide.netfix.R
import com.rupleide.netfix.core.dpibypass.createConnectionNotification
import com.rupleide.netfix.core.dpibypass.registerNotificationChannel
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.START_ACTION
import com.rupleide.netfix.data.STOP_ACTION
import com.rupleide.netfix.data.STARTED_BROADCAST
import com.rupleide.netfix.data.STOPPED_BROADCAST
import com.rupleide.netfix.data.FAILED_BROADCAST
import com.rupleide.netfix.data.SENDER
import com.rupleide.netfix.data.Sender
import com.rupleide.netfix.data.setStatus
import kotlinx.coroutines.launch

class TgProxyService : LifecycleService() {

    companion object {
        private val TAG = TgProxyService::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID = 100
        private const val NOTIFICATION_CHANNEL_ID = "TgProxyServiceChannel"
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "NetFix::TgProxyWakeLock").apply {
                acquire()
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.proxy_channel_name
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.action
        if (action == START_ACTION) {
            acquireWakeLock()
            startForeground()
            val openTg = intent.getBooleanExtra("open_tg", false)
            lifecycleScope.launch {
                TgProxyController.startAsync(
                    context = this@TgProxyService,
                    onSuccess = {
                        setStatus(AppStatus.Running, Mode.Proxy)
                        val broadcastIntent = Intent(STARTED_BROADCAST).apply {
                            putExtra(SENDER, Sender.Proxy.ordinal)
                        }
                        sendBroadcast(broadcastIntent)
                        
                        val prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
                        val alreadyConfigured = prefs.getBoolean("tg_proxy_configured", false)
                        if (openTg && !alreadyConfigured) {
                            val port = TgProxyController.getPort(this@TgProxyService)
                            val secret = TgProxyController.getOrGenerateSecret(this@TgProxyService)
                            val url = TgProxyController.getTgProxyUrl(
                                TgProxyController.DEFAULT_BIND_IP,
                                port,
                                secret
                            )
                            val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                startActivity(tgIntent)
                                prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                            } catch (_: Exception) {}
                        }
                    },
                    onError = {
                        setStatus(AppStatus.Halted, Mode.Proxy)
                        val broadcastIntent = Intent(FAILED_BROADCAST).apply {
                            putExtra(SENDER, Sender.Proxy.ordinal)
                        }
                        sendBroadcast(broadcastIntent)
                        stopSelf()
                    }
                )
            }
            return START_STICKY
        } else if (action == STOP_ACTION) {
            releaseWakeLock()
            TgProxyController.stop()
            setStatus(AppStatus.Halted, Mode.Proxy)
            val broadcastIntent = Intent(STOPPED_BROADCAST).apply {
                putExtra(SENDER, Sender.Proxy.ordinal)
            }
            sendBroadcast(broadcastIntent)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    private fun startForeground() {
        val notification: Notification = createConnectionNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.vpn_notification_content,
            TgProxyService::class.java
        )
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
}
