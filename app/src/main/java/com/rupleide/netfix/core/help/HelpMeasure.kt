package com.rupleide.netfix.core.help

import android.content.Context
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.StrategyTester
import com.rupleide.netfix.core.dpibypass.StreamChunkProbe
import com.rupleide.netfix.core.dpibypass.appListMode
import com.rupleide.netfix.core.dpibypass.currentNetworkKind
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults
import com.rupleide.netfix.core.tgproxy.NativeProxy
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.core.tgproxy.TgProxyStatsSample
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.appStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy

enum class HelpMeasureKind { YoutubeHosts, VideoSpeed, DiscordHosts, AppHosts, TelegramTraffic, NetFixRunning, ScanFound, None }

data class HelpScore(
    val value: Double,
    val success: Boolean,
    val text: String,
    val short: String = "",
    val caption: String = "",
    val partial: Boolean = false
)

object HelpMeasure {
    private const val VIDEO_TIMEOUT_MS = 8_000
    private const val RECHECK_DELAY_MS = 1_500L
    const val GOOD_VIDEO_KBPS = 700L
    private const val QHD_VIDEO_KBPS = 1_500L
    private const val HD_VIDEO_KBPS = 350L

    fun videoQualityFor(kbps: Long): String = when {
        kbps >= QHD_VIDEO_KBPS -> "этого хватает даже для 1440p"
        kbps >= GOOD_VIDEO_KBPS -> "этого хватает для 1080p"
        kbps >= HD_VIDEO_KBPS -> "этого хватает только для 720p"
        else -> "этого мало даже для 720p"
    }

    fun kindFor(symptom: HelpSymptom, facts: HelpFacts?): HelpMeasureKind = when (symptom) {
        HelpSymptom.YoutubeDown, HelpSymptom.YoutubeBrowser, HelpSymptom.Tv -> HelpMeasureKind.YoutubeHosts
        HelpSymptom.YoutubeSlow -> HelpMeasureKind.VideoSpeed
        HelpSymptom.Discord -> HelpMeasureKind.DiscordHosts
        HelpSymptom.OtherApp, HelpSymptom.BrokeApp ->
            if (facts?.targetPackage?.let { HelpPlanner.knownAppHosts[it] } != null) HelpMeasureKind.AppHosts else HelpMeasureKind.None
        HelpSymptom.TelegramNoConnect -> HelpMeasureKind.TelegramTraffic
        HelpSymptom.WontStart -> HelpMeasureKind.NetFixRunning
        HelpSymptom.ScanZero -> HelpMeasureKind.ScanFound
        else -> HelpMeasureKind.None
    }

    fun telegramSample(): TgProxyStatsSample? =
        if (!TgProxyController.isRunning) null else runCatching { NativeProxy.getStats()?.let(TgProxyStatsSample::parse) }.getOrNull()

    private val optionalHost = Regex("""^rr\d+---""")

    fun discordHosts(context: Context): List<String> =
        StrategyTester.DISCORD_PROBE_HOSTS +
            if (com.rupleide.netfix.core.compat.CompatibilityManager.discordEchCheckEnabled(context.getPreferences())) listOf(StrategyTester.ECH_PROBE_HOST) else emptyList()

    private const val MAX_ALWAYS_FAILING_HOSTS = 1

    fun youtubeFailuresInBestScan(context: Context): Int? {
        val kind = context.currentNetworkKind() ?: return null
        val best = StrategyTestManager.verifyResults[kind].orEmpty().filter { it.youtubeTotal > 0 }.maxByOrNull { it.youtubeOk } ?: return null
        return (best.youtubeTotal - best.youtubeOk).takeIf { best.youtubeOk > 0 && it in 1..MAX_ALWAYS_FAILING_HOSTS }
    }

    private suspend fun hostsScore(
        context: Context,
        label: String,
        hosts: List<String>,
        viaNetFix: Boolean,
        failuresAlwaysSeen: Int? = null
    ): HelpScore? {
        suspend fun check(list: List<String>) =
            if (viaNetFix) HelpProbes.checkHostsViaNetFix(context, list) else HelpProbes.checkHostsDirect(list)
        val first = check(hosts)
        if (first.isEmpty()) return null
        val failed = first.filterValues { it != HostVerdict.Ok && it != HostVerdict.RegionBlocked }.keys.toList()
        val verdicts = if (failed.isEmpty()) first else {
            kotlinx.coroutines.delay(RECHECK_DELAY_MS)
            val second = check(failed)
            com.rupleide.netfix.core.debug.AppDebugManager.i("Help", "Перепроверка ${failed.joinToString()}: ${second.entries.joinToString { "${it.key}=${it.value}" }}")
            first + second.filterValues { it == HostVerdict.Ok }
        }
        val ok = verdicts.values.count { it == HostVerdict.Ok }
        val required = verdicts.filterKeys { !optionalHost.containsMatchIn(it) }
        val requiredOk = required.values.all { it == HostVerdict.Ok }
        val asGoodAsBestScan = failuresAlwaysSeen != null && ok > 0 && verdicts.size - ok <= failuresAlwaysSeen
        val path = if (viaNetFix) "Через NetFix" else "Без NetFix"
        val onlyEchFailed = verdicts[StrategyTester.ECH_PROBE_HOST]?.let { it != HostVerdict.Ok } == true &&
            verdicts.filterKeys { it != StrategyTester.ECH_PROBE_HOST }.values.all { it == HostVerdict.Ok }
        val text = when {
            verdicts.values.any { it == HostVerdict.RegionBlocked } -> "$label не пускает пользователей из России"
            onlyEchFailed -> "Сервера $label открываются, но сеть режет ECH ($ok из ${verdicts.size}), из-за этого $label не грузится"
            ok == verdicts.size || (requiredOk && required.isNotEmpty()) -> "$path $label открывается"
            asGoodAsBestScan -> "$path $label открывается, $ok из ${verdicts.size}, как у лучшего способа в проверке"
            ok == 0 -> "$path $label не открывается"
            else -> "$path $label открывается частично, $ok из ${verdicts.size}"
        }
        return HelpScore(
            value = ok.toDouble(),
            success = (requiredOk && required.isNotEmpty()) || asGoodAsBestScan,
            text = text,
            short = "$ok/${verdicts.size}",
            caption = "серверов",
            partial = ok in 1 until verdicts.size
        )
    }

    suspend fun measure(context: Context, symptom: HelpSymptom, facts: HelpFacts?, telegramBaseline: TgProxyStatsSample?): HelpScore? {
        val app = context.applicationContext
        return when (kindFor(symptom, facts)) {
            HelpMeasureKind.YoutubeHosts -> {
                val hosts = StrategyTester.YOUTUBE_PROBE_HOSTS +
                    if (symptom == HelpSymptom.Tv && facts?.smartTubeInstalled == true) StrategyTester.SMARTTUBE_HOSTS else emptyList()
                hostsScore(app, "YouTube", hosts, viaNetFix = true, failuresAlwaysSeen = youtubeFailuresInBestScan(app))
            }
            HelpMeasureKind.DiscordHosts -> hostsScore(app, "Discord", discordHosts(app), viaNetFix = true)
            HelpMeasureKind.AppHosts -> {
                val target = facts?.targetPackage ?: return null
                val hosts = HelpPlanner.knownAppHosts[target] ?: return null
                val prefs = app.getPreferences()
                val viaNetFix = prefs.appListMode() == AppListMode.AllTraffic || target in prefs.selectedAppsWithDefaults()
                hostsScore(app, facts.targetLabel ?: "Приложение", hosts, viaNetFix)
            }
            HelpMeasureKind.VideoSpeed -> {
                val result = videoSpeedCurrent(app) ?: return null
                if (!result.loaded) {
                    com.rupleide.netfix.core.debug.AppDebugManager.i("Help", "Замер видео не удался: ${result.failure}")
                    null
                } else {
                    val megabytes = result.speedKbps >= 1000
                    val speed = if (megabytes) String.format(java.util.Locale("ru"), "%.1f МБ/с", result.speedKbps / 1000.0) else "${result.speedKbps} КБ/с"
                    HelpScore(
                        value = result.speedKbps.toDouble(),
                        success = result.speedKbps >= GOOD_VIDEO_KBPS,
                        text = "Видео грузится со скоростью $speed, ${videoQualityFor(result.speedKbps)}",
                        short = if (megabytes) String.format(java.util.Locale.US, "%.1f", result.speedKbps / 1000.0) else "${result.speedKbps}",
                        caption = if (megabytes) "МБ/с" else "КБ/с",
                        partial = result.speedKbps < GOOD_VIDEO_KBPS
                    )
                }
            }
            HelpMeasureKind.TelegramTraffic -> {
                val now = telegramSample() ?: return HelpScore(0.0, false, "Прокси Telegram не работает")
                val before = telegramBaseline ?: return null
                val flowing = now.routedConnections > before.routedConnections || now.downloaded != before.downloaded
                if (flowing) HelpScore(1.0, true, "Telegram идёт через прокси", "да", "трафик")
                else HelpScore(0.0, false, "Telegram не идёт через прокси", "нет", "трафика")
            }
            HelpMeasureKind.NetFixRunning -> {
                val running = appStatus.first == AppStatus.Running || ServiceManager.isConnected
                HelpScore(if (running) 1.0 else 0.0, running, if (running) "NetFix работает" else "NetFix не включается", if (running) "вкл" else "выкл", "NetFix")
            }
            HelpMeasureKind.ScanFound -> {
                val found = StrategyTestManager.testResults.count { StrategyTester.isSuccessfulStatus(it.third) }
                HelpScore(found.toDouble(), found > 0, if (found > 0) "Рабочих способов в подборе: $found" else "В подборе нет рабочих способов", "$found", "способов")
            }
            HelpMeasureKind.None -> null
        }
    }

    suspend fun videoSpeedCurrent(context: Context): com.rupleide.netfix.core.dpibypass.StreamProbeResult? =
        HelpProbes.onCurrentPath(context, null) { port ->
            StreamChunkProbe.probe(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)), VIDEO_TIMEOUT_MS, {}, {})
        }

    suspend fun videoSpeed(context: Context, strategy: String): com.rupleide.netfix.core.dpibypass.StreamProbeResult? = withContext(Dispatchers.IO) {
        StrategyTester(context.applicationContext).withStrategyPort(strategy, null) { port ->
            StreamChunkProbe.probe(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)), VIDEO_TIMEOUT_MS, {}, {})
        }
    }
}
