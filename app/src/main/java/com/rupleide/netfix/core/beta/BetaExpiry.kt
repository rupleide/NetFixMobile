package com.rupleide.netfix.core.beta

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rupleide.netfix.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object BetaExpiry {
    const val RELEASES_PAGE = "https://github.com/rupleide/NetFixMobile/releases/latest"

    private const val PREFS_NAME = "netfix_beta_clock"
    private const val KEY_HIGH_WATER = "high_water_ms"
    private const val KEY_TRUSTED_NOW = "trusted_now_ms"
    private const val KEY_TRUSTED_ELAPSED = "trusted_elapsed_ms"
    private const val KEY_TRUSTED_BOOT = "trusted_boot_count"
    private const val NETWORK_TIMEOUT_MS = 4000

    private val networkTimeSources = listOf(
        "https://ya.ru",
        "https://www.google.com",
        "https://github.com",
        "https://www.cloudflare.com",
        "http://ya.ru"
    )

    val isBetaBuild: Boolean get() = BuildConfig.BETA_EXPIRES_AT_MS > 0L

    var expired by mutableStateOf(false)
        private set

    val lastWorkingDayLabel: String
        get() = SimpleDateFormat("dd.MM.yyyy", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
            .format(BuildConfig.BETA_EXPIRES_AT_MS - 1L)

    fun isExpiredNow(context: Context): Boolean {
        if (!isBetaBuild) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val state = readState(context)
        val now = BetaClock.effectiveNowMs(
            deviceNowMs = System.currentTimeMillis(),
            elapsedSinceBootMs = SystemClock.elapsedRealtime(),
            bootCount = bootCount(context),
            state = state,
            builtAtMs = BuildConfig.BETA_BUILT_AT_MS
        )
        if (now > state.highWaterMs) prefs.edit().putLong(KEY_HIGH_WATER, now).apply()
        val result = BetaClock.isExpired(now, BuildConfig.BETA_EXPIRES_AT_MS)
        expired = result
        return result
    }

    suspend fun refreshFromNetwork(context: Context): Boolean {
        if (!isBetaBuild) return false
        val networkNow = withContext(Dispatchers.IO) { fetchNetworkTimeMs() }
        if (networkNow != null) {
            val updated = BetaClock.withNetworkSample(
                state = readState(context),
                networkNowMs = networkNow,
                elapsedSinceBootMs = SystemClock.elapsedRealtime(),
                bootCount = bootCount(context)
            )
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(KEY_HIGH_WATER, updated.highWaterMs)
                .putLong(KEY_TRUSTED_NOW, updated.trustedNowMs)
                .putLong(KEY_TRUSTED_ELAPSED, updated.trustedElapsedMs)
                .putInt(KEY_TRUSTED_BOOT, updated.trustedBootCount)
                .apply()
        }
        return isExpiredNow(context)
    }

    fun shutDownServices(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(appContext.packageName + "_preferences", Context.MODE_PRIVATE)
            .edit().putBoolean("service_enabled", false).apply()
        try {
            com.rupleide.netfix.core.dpibypass.ServiceManager.stop(appContext)
        } catch (_: Exception) {
        }
        try {
            appContext.startService(
                android.content.Intent(appContext, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java)
                    .setAction(com.rupleide.netfix.data.STOP_ACTION)
            )
        } catch (_: Exception) {
        }
    }

    private fun readState(context: Context): TrustedClockState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TrustedClockState(
            highWaterMs = prefs.getLong(KEY_HIGH_WATER, 0L),
            trustedNowMs = prefs.getLong(KEY_TRUSTED_NOW, 0L),
            trustedElapsedMs = prefs.getLong(KEY_TRUSTED_ELAPSED, 0L),
            trustedBootCount = prefs.getInt(KEY_TRUSTED_BOOT, -1)
        )
    }

    private fun bootCount(context: Context): Int =
        try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        } catch (_: Exception) {
            -1
        }

    private fun fetchNetworkTimeMs(): Long? {
        for (source in networkTimeSources) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(source).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = NETWORK_TIMEOUT_MS
                connection.readTimeout = NETWORK_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.connect()
                val serverDate = connection.date
                if (serverDate >= BetaClock.MIN_PLAUSIBLE_NETWORK_TIME_MS) return serverDate
            } catch (_: Exception) {
            } finally {
                connection?.disconnect()
            }
        }
        return null
    }
}
