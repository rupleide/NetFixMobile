package com.rupleide.netfix.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.MainActivity

class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(appCtx.packageName + "_preferences", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("service_enabled", false)
        val autostart = prefs.getBoolean("autostart", false)

        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("showUpdateInstalledMessage", true)
            }
            context.startActivity(mainIntent)
            return
        }

        val isBoot = intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == Intent.ACTION_REBOOT ||
                intent.action == "android.intent.action.QUICKBOOT_POWERON"

        if (serviceEnabled || (isBoot && autostart)) {
            val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
            val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
            if (wantsYoutube && VpnService.prepare(appCtx) == null) {
                ServiceManager.start(appCtx, Mode.VPN)
            }
            if (wantsTelegram) {
                val port = TgProxyController.getPort(appCtx)
                val isTgProxyRunning = com.rupleide.netfix.data.isTgProxyRunningGlobal || TgProxyController.isPortOpen(
                    TgProxyController.DEFAULT_BIND_IP,
                    port,
                    500
                )
                if (!isTgProxyRunning) {
                    if (wantsYoutube) {
                        TgProxyController.startAsync(appCtx, {}, {})
                    } else {
                        val intent = Intent(appCtx, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                            action = com.rupleide.netfix.data.START_ACTION
                            putExtra("open_tg", false)
                        }
                        androidx.core.content.ContextCompat.startForegroundService(appCtx, intent)
                    }
                }
            }
        }

        if (intent.action == "com.rupleide.netfix.action.WATCHDOG_PING" && serviceEnabled) {
            scheduleWatchdogAlarm(appCtx)
        }
    }

    companion object {
        fun scheduleWatchdogAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java).apply {
                action = "com.rupleide.netfix.action.WATCHDOG_PING"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        fun cancelWatchdogAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java).apply {
                action = "com.rupleide.netfix.action.WATCHDOG_PING"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
