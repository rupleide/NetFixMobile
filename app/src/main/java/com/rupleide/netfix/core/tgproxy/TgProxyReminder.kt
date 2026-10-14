package com.rupleide.netfix.core.tgproxy

import com.rupleide.netfix.core.dpibypass.launchIntentWithLeanback
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rupleide.netfix.R

object TgProxyReminder {
    private const val CHANNEL_ID = "netfix_tg_proxy_reminder"
    private const val NOTIFICATION_ID = 4711
    private const val REPLACED_CHANNEL_ID = "netfix_vpn_replaced"
    private const val REPLACED_NOTIFICATION_ID = 4712
    private val telegramPackages = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.plus",
        "org.thunderdog.challegram",
        "tw.nekomimi.nekogram"
    )

    fun notifyProxyStopped(context: Context) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("tg_proxy_configured", false)) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        val pending = PendingIntent.getActivity(
            context,
            0,
            openTelegramIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = "NetFix больше не проксирует Telegram. Откройте Telegram, Настройки, Данные и память, Прокси и выключите прокси, иначе соединение будет висеть."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Выключите прокси в Telegram")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .addAction(0, "Открыть Telegram", pending)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyVpnReplaced(context: Context, tgProxyKeepsRunning: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(REPLACED_CHANNEL_ID, "Другой VPN", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, com.rupleide.netfix.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (tgProxyKeepsRunning) {
            "Включён другой VPN, обход NetFix выключен. Прокси Telegram продолжает работать."
        } else {
            "Включён другой VPN, обход NetFix выключен. Чтобы вернуть его, запустите NetFix снова."
        }
        val notification = NotificationCompat.Builder(context, REPLACED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("NetFix отключён другим VPN")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Открыть NetFix", open)
            .build()
        try {
            manager.notify(REPLACED_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun clearVpnReplaced(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(REPLACED_NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    fun clear(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    private fun openTelegramIntent(context: Context): Intent {
        val pm = context.packageManager
        val launch = telegramPackages.firstNotNullOfOrNull { pm.launchIntentWithLeanback(it) }
        return (launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("tg://settings"))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Напоминания о прокси Telegram", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
