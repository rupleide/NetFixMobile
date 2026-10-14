package com.rupleide.netfix.core.tgproxy

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rupleide.netfix.R
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.EXTRA_STOP_ALL
import com.rupleide.netfix.core.dpibypass.NETFIX_NOTIFICATION_CHANNEL_ID
import com.rupleide.netfix.core.dpibypass.NETFIX_NOTIFICATION_ID
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.dpibypass.createNetFixNotification
import com.rupleide.netfix.core.dpibypass.registerNotificationChannel
import com.rupleide.netfix.data.START_ACTION
import com.rupleide.netfix.data.STOP_ACTION
import com.rupleide.netfix.data.STARTED_BROADCAST
import com.rupleide.netfix.data.STOPPED_BROADCAST
import com.rupleide.netfix.data.FAILED_BROADCAST
import com.rupleide.netfix.data.SENDER
import com.rupleide.netfix.data.Sender
import kotlinx.coroutines.launch

class TgProxyService : LifecycleService() {

    companion object {
        private val TAG = TgProxyService::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID = NETFIX_NOTIFICATION_ID
        private const val LEGACY_NOTIFICATION_CHANNEL_ID = "TgProxyServiceChannel"
        private const val PROXY_ONLY_NOTIFICATION_ID = 6

        fun startInForeground(context: android.content.Context, openTelegram: Boolean, proxyOnly: Boolean = false): Boolean {
            val intent = Intent(context, TgProxyService::class.java).apply {
                action = START_ACTION
                putExtra("open_tg", openTelegram)
                putExtra("proxy_only", proxyOnly)
            }
            return try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: IllegalStateException) {
                Log.w(TAG, "TG proxy start not allowed from background: ${e.message}")
                false
            }
        }
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        val prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
        if (!prefs.getBoolean("tg_proxy_wakelock_enabled", false)) return
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
            NETFIX_NOTIFICATION_CHANNEL_ID,
            R.string.vpn_channel_name
        )
        getSystemService(NotificationManager::class.java)
            ?.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.action
        if (action == START_ACTION) {
            Log.i(TAG, "Служба TgProxyService: получен запрос на запуск")
            acquireWakeLock()
            startForeground(intent.getBooleanExtra("proxy_only", false))
            if (TgProxyController.isRunning) {
                Log.i(TAG, "TG-прокси уже работает, служба берёт его под себя")
                val broadcastIntent = Intent(STARTED_BROADCAST).apply {
                    putExtra(SENDER, Sender.Proxy.ordinal)
                }
                sendBroadcast(broadcastIntent.setPackage(packageName))
                return START_NOT_STICKY
            }
            val openTg = intent.getBooleanExtra("open_tg", false)
            lifecycleScope.launch {
                TgProxyController.startAsync(
                    context = this@TgProxyService,
                    onSuccess = {
                        Log.i(TAG, "Служба TgProxyService успешно запущена")
                        TgProxyReminder.clear(this@TgProxyService)
                        val broadcastIntent = Intent(STARTED_BROADCAST).apply {
                            putExtra(SENDER, Sender.Proxy.ordinal)
                        }
                        sendBroadcast(broadcastIntent.setPackage(packageName))
                        
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
                        Log.e(TAG, "Служба TgProxyService завершилась с ошибкой запуска")
                        val broadcastIntent = Intent(FAILED_BROADCAST).apply {
                            putExtra(SENDER, Sender.Proxy.ordinal)
                        }
                        sendBroadcast(broadcastIntent.setPackage(packageName))
                        stopSelf()
                    }
                )
            }
            return START_NOT_STICKY
        } else if (action == STOP_ACTION) {
            Log.i(TAG, "Служба TgProxyService: получен запрос на остановку")
            startForeground(false)
            releaseWakeLock()
            val wasRunning = TgProxyController.isRunning
            TgProxyController.stop()
            if (wasRunning) {
                TgProxyReminder.notifyProxyStopped(this)
            }
            if (intent.getBooleanExtra(EXTRA_STOP_ALL, false)) {
                getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
                    .edit()
                    .putBoolean("service_enabled", false)
                    .apply()
                if (ByeDpiVpnService.isAlive) {
                    ServiceManager.stop(this)
                }
            }
            com.rupleide.netfix.service.NetFixTileService.requestRefresh(this)
            val broadcastIntent = Intent(STOPPED_BROADCAST).apply {
                putExtra(SENDER, Sender.Proxy.ordinal)
            }
            sendBroadcast(broadcastIntent.setPackage(packageName))
            stopSelf()
            return START_NOT_STICKY
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private fun startForeground(proxyOnly: Boolean) {
        val notification: Notification = createNetFixNotification(this, vpnActive = !proxyOnly)
        val id = if (proxyOnly) PROXY_ONLY_NOTIFICATION_ID else FOREGROUND_SERVICE_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(id, notification)
        }
    }
}
