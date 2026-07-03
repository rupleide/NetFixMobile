package com.rupleide.netfix.core.dpibypass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.rupleide.netfix.R
import com.rupleide.netfix.MainActivity
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.PAUSE_ACTION
import com.rupleide.netfix.data.RESUME_ACTION
import com.rupleide.netfix.data.STOP_ACTION

data class DomainList(
    val id: String,
    val name: String,
    val domains: List<String>,
    val isActive: Boolean = true,
    val isBuiltIn: Boolean = false,
    val isModified: Boolean = false,
    val isDeleted: Boolean = false
)

object DomainListUtils {
    fun getLists(context: Context): List<DomainList> = emptyList()
}

fun Context.getPreferences(): SharedPreferences =
    getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)

fun SharedPreferences.getStringNotNull(key: String, defValue: String): String =
    getString(key, defValue) ?: defValue

fun SharedPreferences.mode(): Mode =
    Mode.fromString(getStringNotNull("byedpi_mode", "vpn"))

fun SharedPreferences.getSelectedApps(): List<String> =
    getStringSet("selected_apps", emptySet())?.toList() ?: emptyList()

fun SharedPreferences.checkIpAndPortInCmd(): Pair<String?, String?> {
    val cmdEnable = getBoolean("byedpi_enable_cmd_settings", false)
    if (!cmdEnable) return Pair(null, null)

    val cmdArgs = getString("byedpi_cmd_args", "")?.let { shellSplit(it) } ?: emptyList()

    fun getArgValue(argsList: List<String>, keys: List<String>): String? {
        for (i in argsList.indices) {
            val arg = argsList[i]
            for (key in keys) {
                if (key.startsWith("--")) {
                    if (arg == key && i + 1 < argsList.size) {
                        return argsList[i + 1]
                    } else if (arg.startsWith("$key=")) {
                        return arg.substringAfter('=')
                    }
                } else if (key.startsWith("-")) {
                    if (arg.startsWith(key) && arg.length > key.length) {
                        return arg.substring(key.length)
                    } else if (arg == key && i + 1 < argsList.size) {
                        return argsList[i + 1]
                    }
                }
            }
        }
        return null
    }

    val cmdIp = getArgValue(cmdArgs, listOf("--ip", "-i"))
    val cmdPort = getArgValue(cmdArgs, listOf("--port", "-p"))

    return Pair(cmdIp, cmdPort)
}

fun SharedPreferences.getProxyIpAndPort(): Pair<String, String> {
    val (cmdIp, cmdPort) = checkIpAndPortInCmd()
    val ip = cmdIp ?: getStringNotNull("byedpi_proxy_ip", "127.0.0.1")
    val port = cmdPort ?: getString("proxy_port", null) ?: getStringNotNull("byedpi_proxy_port", "1080")
    return Pair(ip, port)
}

fun shellSplit(string: CharSequence): List<String> {
    val tokens: MutableList<String> = ArrayList()
    var quoteChar = ' '
    var escaping = false
    var quoting = false
    var lastCloseQuoteIndex = Int.MIN_VALUE
    var current = StringBuilder()

    for (i in string.indices) {
        val c = string[i]

        if (escaping) {
            current.append(c)
            escaping = false
        } else if (c == '\\' && quoting) {
            if (i + 1 < string.length && string[i + 1] == quoteChar) {
                escaping = true
            } else {
                current.append(c)
            }
        } else if (quoting && c == quoteChar) {
            quoting = false
            lastCloseQuoteIndex = i
        } else if (!quoting && (c == '\'' || c == '"')) {
            quoting = true
            quoteChar = c
        } else if (!quoting && Character.isWhitespace(c)) {
            if (current.isNotEmpty() || lastCloseQuoteIndex == i - 1) {
                tokens.add(current.toString())
                current = StringBuilder()
            }
        } else {
            current.append(c)
        }
    }

    if (current.isNotEmpty() || lastCloseQuoteIndex == string.length - 1) {
        tokens.add(current.toString())
    }

    return tokens
}

fun registerNotificationChannel(context: Context, id: String, @StringRes name: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            id,
            context.getString(name),
            NotificationManager.IMPORTANCE_MIN
        )
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setShowBadge(false)

        manager.createNotificationChannel(channel)
    }
}

fun createConnectionNotification(
    context: Context,
    channelId: String,
    @StringRes title: Int,
    @StringRes content: Int,
    service: Class<*>,
): Notification {
    val pauseIntent = Intent(context, service).apply { action = PAUSE_ACTION }
    val pausePendingIntent = PendingIntent.getService(
        context, 1, pauseIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val stopIntent = Intent(context, service).apply { action = STOP_ACTION }
    val stopPendingIntent = PendingIntent.getService(
        context, 3, stopIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setSilent(true)
        .setContentTitle("NetFix")
        .setContentText("Подключение защищено")
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .addAction(R.drawable.ic_pause, "Приостановить", pausePendingIntent)
        .addAction(R.drawable.ic_power, "Выключить", stopPendingIntent)
        .build()
}

fun createPauseNotification(
    context: Context,
    channelId: String,
    @StringRes title: Int,
    @StringRes content: Int,
    service: Class<*>,
): Notification {
    val resumeIntent = Intent(context, service).apply { action = RESUME_ACTION }
    val resumePendingIntent = PendingIntent.getService(
        context, 2, resumeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val stopIntent = Intent(context, service).apply { action = STOP_ACTION }
    val stopPendingIntent = PendingIntent.getService(
        context, 3, stopIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setSilent(true)
        .setContentTitle("NetFix")
        .setContentText("Приостановлено")
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .addAction(R.drawable.ic_play, "Возобновить", resumePendingIntent)
        .addAction(R.drawable.ic_power, "Выключить", stopPendingIntent)
        .build()
}
