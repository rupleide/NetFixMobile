package com.rupleide.netfix.core.tgproxy

import android.content.Context
import android.content.SharedPreferences
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

enum class DcAddressesRefresh { Updated, Same, Failed }

object TgDcAddresses {
    private const val TAG = "TgDcAddresses"
    private const val KEY_VALUE = "tgproxy_dc_ips_remote"
    private const val KEY_CHECKED_AT = "tgproxy_dc_ips_checked_at"
    private const val SOURCE_URL = "https://raw.githubusercontent.com/Flowseal/tg-ws-proxy/main/proxy/tg_ws_proxy.py"
    private const val TIMEOUT_MS = 8000
    private const val CHECK_EVERY_MS = 24L * 60 * 60 * 1000
    private val dcEntry = Regex("""['"](\d{1,3}):((?:\d{1,3}\.){3}\d{1,3})['"]""")

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun isValid(value: String?): Boolean =
        !value.isNullOrBlank() && value.split(',').all { part ->
            val dc = part.substringBefore(':').toIntOrNull()
            val ip = part.substringAfter(':', "")
            dc != null && dc in 1..5 && ip.split('.').let { octets -> octets.size == 4 && octets.all { it.toIntOrNull() in 0..255 } }
        }

    fun parseDefaults(source: String): String? {
        val start = source.indexOf("dc_ip is None")
        if (start < 0) return null
        val end = source.indexOf("elif", start).takeIf { it > start } ?: (start + 600).coerceAtMost(source.length)
        val found = dcEntry.findAll(source.substring(start, end)).map { "${it.groupValues[1]}:${it.groupValues[2]}" }.distinct().toList()
        return found.joinToString(",").takeIf { isValid(it) }
    }

    fun current(context: Context): String =
        prefs(context).getString(KEY_VALUE, null)?.takeIf { isValid(it) } ?: TgProxyController.DEFAULT_DC_IPS

    fun checkedAt(context: Context): Long = prefs(context).getLong(KEY_CHECKED_AT, 0L)

    fun isStale(context: Context, now: Long = System.currentTimeMillis()): Boolean = now - checkedAt(context) !in 0 until CHECK_EVERY_MS

    fun refresh(context: Context): DcAddressesRefresh {
        val source = download() ?: return DcAddressesRefresh.Failed.also { Log.w(TAG, "Адреса DC: GitHub недоступен, оставляю ${current(context)}") }
        val parsed = parseDefaults(source) ?: return DcAddressesRefresh.Failed.also { Log.w(TAG, "Адреса DC: не нашёл их в tg-ws-proxy") }
        val before = current(context)
        prefs(context).edit().putString(KEY_VALUE, parsed).putLong(KEY_CHECKED_AT, System.currentTimeMillis()).apply()
        return if (parsed == before) DcAddressesRefresh.Same else DcAddressesRefresh.Updated.also { Log.i(TAG, "Адреса DC обновлены: $before → $parsed") }
    }

    fun refreshIfStale(context: Context): DcAddressesRefresh? = if (isStale(context)) refresh(context) else null

    private fun download(): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(SOURCE_URL).openConnection(Proxy.NO_PROXY) as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
