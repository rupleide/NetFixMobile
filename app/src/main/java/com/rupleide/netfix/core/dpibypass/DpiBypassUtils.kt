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
import com.rupleide.netfix.core.tgproxy.TgProxyService
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

private val connectionArgsWithValue = setOf("-i", "--ip", "-p", "--port")
private val connectionArgPrefixes = listOf("-i", "-p")

fun sanitizePastedByeDpiCommand(raw: String): String {
    val tokens = shellSplit(raw.replace('\n', ' ').trim()).toMutableList()
    if (tokens.firstOrNull()?.endsWith("ciadpi") == true) tokens.removeAt(0)
    val keptTokens = mutableListOf<String>()
    var skipNextValue = false
    for (token in tokens) {
        when {
            skipNextValue -> skipNextValue = false
            token in connectionArgsWithValue -> skipNextValue = true
            token.startsWith("--ip=") || token.startsWith("--port=") -> Unit
            connectionArgPrefixes.any { token.startsWith(it) && token.length > 2 && !token.startsWith("--") } -> Unit
            else -> keptTokens.add(if (token.contains(' ')) "\"$token\"" else token)
        }
    }
    return keptTokens.joinToString(" ")
}

enum class AppListMode { AllTraffic, SelectedAppsOnly }

const val APP_LIST_TYPE_KEY: String = "applist_type"
private const val APP_LIST_TYPE_ALL_TRAFFIC = "disable"
private const val APP_LIST_TYPE_SELECTED_ONLY = "whitelist"

val defaultYoutubeApps: Set<String> = setOf(
    "org.smarttube.stable",
    "org.smarttube.beta",
    "com.google.android.youtube",
    "com.google.android.youtube.tv",
    "com.liskovsoft.videomanager.v2",
    "com.liskovsoft.smarttubetv.beta",
    "com.teamsmart.videomanager.tv",
    "app.revanced.android.youtube",
    "com.google.android.apps.youtube.music",
    "com.google.android.apps.youtube.kids",
    "org.schabi.newpipe",
    "org.schabi.newpipe.legacy",
    "com.kapp.youtube",
    "com.bg.vanced",
    "com.libretube",
    "com.liskovsoft.smarttubetv"
)

val extraBypassApps: Set<String> = setOf(
    "com.discord",
    "com.twitter.android",
    "com.instagram.android",
    "com.instagram.barcelona",
    "com.facebook.katana",
    "com.facebook.orca",
    "com.linkedin.android",
    "com.reddit.frontpage",
    "com.viber.voip",
    "org.thoughtcrime.securesms",
    "com.whatsapp",
    "com.whatsapp.w4b",
    "tv.twitch.android.app",
    "com.spotify.music",
    "com.snapchat.android",
    "com.pinterest"
)

private const val APPS_DEFAULTS_VERSION_KEY = "apps_defaults_version"
private const val APPS_DEFAULTS_VERSION = 3

fun android.content.pm.PackageManager.launchIntentWithLeanback(packageName: String): Intent? =
    getLaunchIntentForPackage(packageName) ?: getLeanbackLaunchIntentForPackage(packageName)

fun SharedPreferences.appListMode(): AppListMode =
    when (getString(APP_LIST_TYPE_KEY, APP_LIST_TYPE_SELECTED_ONLY)) {
        APP_LIST_TYPE_ALL_TRAFFIC -> AppListMode.AllTraffic
        else -> AppListMode.SelectedAppsOnly
    }

fun SharedPreferences.setAppListMode(mode: AppListMode) {
    val storedValue = when (mode) {
        AppListMode.AllTraffic -> APP_LIST_TYPE_ALL_TRAFFIC
        AppListMode.SelectedAppsOnly -> APP_LIST_TYPE_SELECTED_ONLY
    }
    edit().putString(APP_LIST_TYPE_KEY, storedValue).apply()
}

fun SharedPreferences.selectedAppsWithDefaults(): Set<String> {
    val storedApps = getStringSet("selected_apps", null)
    if (storedApps != null) {
        if (getInt(APPS_DEFAULTS_VERSION_KEY, 0) >= APPS_DEFAULTS_VERSION) return storedApps
        val migrated = storedApps + extraBypassApps
        edit().putStringSet("selected_apps", migrated).putInt(APPS_DEFAULTS_VERSION_KEY, APPS_DEFAULTS_VERSION).apply()
        return migrated
    }
    val initial = defaultYoutubeApps + extraBypassApps
    edit().putStringSet("selected_apps", initial).putInt(APPS_DEFAULTS_VERSION_KEY, APPS_DEFAULTS_VERSION).apply()
    return initial
}

const val ACTIVE_STRATEGY_KEY: String = "byedpi_cmd_args"

enum class NetworkKind(val preferenceSuffix: String, val displayName: String) {
    Wifi("wifi", "Wi-Fi"),
    Mobile("mobile", "Мобильная сеть")
}

private fun networkKindOf(capabilities: android.net.NetworkCapabilities): NetworkKind? = when {
    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.Wifi
    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkKind.Mobile
    else -> null
}

fun Context.currentNetworkKind(): NetworkKind? {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    return try {
        val activeCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val direct = activeCapabilities?.let { networkKindOf(it) }
        if (direct != null) return direct
        val physical = connectivityManager.allNetworks.mapNotNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
            if (!capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
            if (!capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return@mapNotNull null
            networkKindOf(capabilities)
        }
        physical.firstOrNull { it == NetworkKind.Wifi } ?: physical.firstOrNull()
    } catch (_: SecurityException) {
        null
    }
}

val NetworkKind.shortName: String
    get() = when (this) {
        NetworkKind.Wifi -> "Wi-Fi"
        NetworkKind.Mobile -> "Моб. сеть"
    }

private fun strategyKeyForNetwork(kind: NetworkKind) = "${ACTIVE_STRATEGY_KEY}_${kind.preferenceSuffix}"

fun SharedPreferences.strategyForNetwork(kind: NetworkKind): String? =
    getString(strategyKeyForNetwork(kind), null)

fun SharedPreferences.rememberStrategyForNetwork(kind: NetworkKind, strategy: String) {
    if (getString(strategyKeyForNetwork(kind), null) == strategy) return
    edit().putString(strategyKeyForNetwork(kind), strategy).apply()
}

fun Context.applyStrategyForCurrentNetwork(): Boolean {
    val kind = currentNetworkKind() ?: return false
    val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
    val networkStrategy = prefs.strategyForNetwork(kind) ?: return false
    if (prefs.getString(ACTIVE_STRATEGY_KEY, null) == networkStrategy) return false
    prefs.edit()
        .putString(ACTIVE_STRATEGY_KEY, networkStrategy)
        .putBoolean("byedpi_enable_cmd_settings", true)
        .apply()
    return true
}

const val NETFIX_NOTIFICATION_ID: Int = 1
const val NETFIX_NOTIFICATION_CHANNEL_ID: String = "ByeDPIVpn"
const val EXTRA_STOP_ALL: String = "stop_all"

fun createStopAllPendingIntent(context: Context): PendingIntent {
    val stopAllIntent = Intent(context, TgProxyService::class.java).apply {
        action = STOP_ACTION
        putExtra(EXTRA_STOP_ALL, true)
    }
    return PendingIntent.getForegroundService(
        context, 4, stopAllIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun createNetFixNotification(context: Context, vpnActive: Boolean = true): Notification {
    val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    val builder = NotificationCompat.Builder(context, NETFIX_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setSilent(true)
        .setOngoing(true)
        .setContentTitle("NetFix")
        .setContentText(if (vpnActive) "Подключение защищено" else "Работает только прокси Telegram")
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
    if (vpnActive && prefs.getBoolean("wants_youtube_bypass", true)) {
        val pauseIntent = Intent(context, ByeDpiVpnService::class.java).apply { action = PAUSE_ACTION }
        val pausePendingIntent = PendingIntent.getForegroundService(
            context, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(R.drawable.ic_pause, "Приостановить", pausePendingIntent)
    }
    builder.addAction(R.drawable.ic_power, "Выключить", createStopAllPendingIntent(context))
    return builder.build()
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
    val pausePendingIntent = PendingIntent.getForegroundService(
        context, 1, pauseIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val stopIntent = Intent(context, service).apply { action = STOP_ACTION }
    val stopPendingIntent = PendingIntent.getForegroundService(
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
    customStopPendingIntent: PendingIntent? = null,
): Notification {
    val resumeIntent = Intent(context, service).apply { action = RESUME_ACTION }
    val resumePendingIntent = PendingIntent.getForegroundService(
        context, 2, resumeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val stopIntent = Intent(context, service).apply { action = STOP_ACTION }
    val stopPendingIntent = customStopPendingIntent ?: PendingIntent.getForegroundService(
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
