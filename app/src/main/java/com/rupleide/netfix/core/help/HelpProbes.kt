package com.rupleide.netfix.core.help

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.compat.VersionOrder
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.ProxyPortResolver
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.StrategyTester
import com.rupleide.netfix.core.dpibypass.appListMode
import com.rupleide.netfix.core.dpibypass.currentNetworkKind
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.dpibypass.getProxyIpAndPort
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults
import com.rupleide.netfix.core.tgproxy.NativeProxy
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.core.tgproxy.TgProxyStatsSample
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.service.VendorAutoStartHelper
import com.rupleide.netfix.ui.main.activeConnection
import com.rupleide.netfix.ui.main.shouldShowNetworkAssistantHint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object HelpProbes {
    private const val DIRECT_TIMEOUT_MS = 4_000
    private const val TG_SAMPLE_GAP_MS = 3_000L
    private const val FASTER_RATIO = 0.7

    val smartTubePackages = listOf(
        "org.smarttube.stable",
        "org.smarttube.beta",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "com.teamsmart.videomanager.tv"
    )

    fun isInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getApplicationInfo(packageName, 0).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun appLabel(context: Context, packageName: String): String =
        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }

    fun defaultBrowser(context: Context): String? {
        val resolved = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        return resolved?.takeIf { it != "android" && it != context.packageName }
    }

    fun otherVpnActive(context: Context): Boolean = otherVpnCapturesApp(context)

    private val spoofCheckHosts = listOf("youtube.com", "m.youtube.com", "discord.com", "youtubei.googleapis.com")

    suspend fun networkDnsSpoofed(): Boolean = withContext(Dispatchers.IO) {
        coroutineScope {
            spoofCheckHosts.map { host ->
                async {
                    val address = HelpHostCheck.resolve(host, null) ?: return@async false
                    HelpHostCheck.answersWithForeignCertificate(host, address) == true
                }
            }.awaitAll().any { it }
        }
    }

    fun privateDnsHost(context: Context): String? {
        val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
        if (mode != "hostname") return null
        return Settings.Global.getString(context.contentResolver, "private_dns_specifier").orEmpty()
    }

    fun isTv(context: Context): Boolean {
        val prefs = context.getPreferences()
        return prefs.getBoolean("device_type_tv", false) || prefs.getBoolean("is_smart_tv", false)
    }

    val youtubeClients = listOf(
        "app.revanced.android.youtube",
        "app.rvx.android.youtube",
        "app.morphe.android.youtube",
        "com.vanced.android.youtube",
        HelpPlanner.YOUTUBE_PACKAGE
    )

    fun packagesThatCarry(context: Context, packageName: String): List<String> =
        if (packageName == CompatibilityManager.GEMINI_PACKAGE && isInstalled(context, CompatibilityManager.GOOGLE_APP_PACKAGE)) {
            listOf(packageName, CompatibilityManager.GOOGLE_APP_PACKAGE)
        } else {
            listOf(packageName)
        }

    fun carriedByNetFix(context: Context, packageName: String, selected: Set<String>): Boolean {
        val viaVpn = CompatibilityManager.vpnAppsFor(context.getPreferences(), selected)
        return packagesThatCarry(context, packageName).all { it in viaVpn }
    }

    fun preferredYoutube(context: Context): String? {
        com.rupleide.netfix.core.YoutubeClient.chosenPackage(context)?.let { return it }
        val installed = youtubeClients.filter { isInstalled(context, it) }
        val selected = context.getPreferences().selectedAppsWithDefaults()
        return installed.firstOrNull { it in selected } ?: installed.firstOrNull()
    }

    fun currentStrategy(context: Context): String? {
        val prefs = context.getPreferences()
        val kind = context.currentNetworkKind()
        return kind?.let { StrategyTestManager.selectedStrategyFor(context, it) }
            ?: prefs.getString("byedpi_cmd_args", null)?.takeIf { it.isNotBlank() && prefs.getBoolean("byedpi_enable_cmd_settings", false) }
    }

    fun nextWorkingStrategy(context: Context): String? {
        val kind = context.currentNetworkKind() ?: return null
        val current = currentStrategy(context)?.let(StrategyTester::normalizeStrategy)
        val verified = StrategyTestManager.verifyResults[kind].orEmpty()
            .filter { it.isWorking && StrategyTester.normalizeStrategy(it.strategy) != current }
            .sortedWith(compareByDescending<StrategyTestManager.RankedConfig> { it.totalOk }.thenBy { if (it.averageMs < 0) Long.MAX_VALUE else it.averageMs })
            .firstOrNull()?.strategy
        return verified ?: StrategyTestManager.configChoices(kind).firstOrNull { StrategyTester.normalizeStrategy(it) != current }
    }

    fun fasterStrategy(context: Context): String? {
        val kind = context.currentNetworkKind() ?: return null
        val current = currentStrategy(context)
        val currentMs = StrategyTestManager.resultFor(kind, current)?.averageMs ?: return null
        if (currentMs <= 0) return null
        val currentKey = current?.let(StrategyTester::normalizeStrategy)
        return StrategyTestManager.verifyResults[kind].orEmpty()
            .filter { it.isWorking && it.averageMs in 1 until (currentMs * FASTER_RATIO).toLong() && StrategyTester.normalizeStrategy(it.strategy) != currentKey }
            .minByOrNull { it.averageMs }
            ?.strategy
    }

    fun reachableDirect(host: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("https://$host").openConnection() as HttpURLConnection
            connection.connectTimeout = DIRECT_TIMEOUT_MS
            connection.readTimeout = DIRECT_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.setRequestProperty("Connection", "close")
            connection.responseCode
            connection.inputStream.use { it.read(ByteArray(2048)) }
            true
        } catch (e: java.io.IOException) {
            connection?.errorStream?.use { true } ?: false
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    fun tunnelDnsServer(context: Context): String? {
        val prefs = context.getPreferences()
        if (privateDnsHost(context) != null) return null
        return CompatibilityManager.tunnelDnsServers(prefs, false).firstOrNull { !it.contains(':') }
    }

    fun liveProxyPort(context: Context): Int? {
        if (!ServiceManager.isConnected) return null
        val port = ByeDpiVpnService.liveProxyPort
            ?: context.getPreferences().getProxyIpAndPort().second.toIntOrNull()
            ?: return null
        return port.takeIf { TgProxyController.isPortOpen(TgProxyController.DEFAULT_BIND_IP, it, 300) }
    }

    private const val LIVE_PORT_WAIT_MS = 5_000L
    private const val LIVE_PORT_POLL_MS = 250L

    private suspend fun awaitLivePortWhileStarting(context: Context): Int? {
        if (!ServiceManager.isConnected && !ByeDpiVpnService.isAlive) return null
        val deadline = System.currentTimeMillis() + LIVE_PORT_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(LIVE_PORT_POLL_MS)
            liveProxyPort(context)?.let { return it }
            if (!ServiceManager.isConnected && !ByeDpiVpnService.isAlive) return null
        }
        return null
    }

    fun otherVpnCapturesApp(context: Context): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val appNetwork = cm.activeNetwork
        appNetwork != null && cm.getNetworkCapabilities(appNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    } catch (_: Exception) {
        false
    }

    suspend fun <T> onCurrentPath(context: Context, fallback: T, block: suspend (Int) -> T): T = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val live = liveProxyPort(app) ?: awaitLivePortWhileStarting(app)
        if (live != null) return@withContext block(live)
        if (StrategyTestManager.isTesting || StrategyTestManager.isVerifying) return@withContext fallback
        val strategy = currentStrategy(app) ?: return@withContext fallback
        StrategyTester(app).withStrategyPort(strategy, fallback, block)
    }

    private const val RECHECK_DELAY_MS = 1_500L

    private suspend fun recheckFailed(
        first: Map<String, HostVerdict>,
        check: suspend (List<String>) -> Map<String, HostVerdict>
    ): Map<String, HostVerdict> {
        val failed = first.filterValues { it == HostVerdict.Blocked }.keys.toList()
        if (failed.isEmpty()) return first
        delay(RECHECK_DELAY_MS)
        return first + check(failed).filterValues { it == HostVerdict.Ok }
    }

    suspend fun checkHostsViaNetFix(context: Context, hosts: List<String>): Map<String, HostVerdict> {
        val app = context.applicationContext
        val dns = tunnelDnsServer(app)
        return onCurrentPath(app, emptyMap()) { port ->
            coroutineScope {
                hosts.map { host -> async(Dispatchers.IO) { host to HelpHostCheck.check(host, dns, port) } }.awaitAll().toMap()
            }
        }
    }

    suspend fun probeCurrent(context: Context): HelpProbe? {
        val app = context.applicationContext
        val youtube = StrategyTester.YOUTUBE_PROBE_HOSTS
        val discord = if (StrategyTester.discordCheckEnabled(app.getPreferences())) HelpMeasure.discordHosts(app) else emptyList()
        val verdicts = checkHostsViaNetFix(app, youtube + discord)
        if (verdicts.isEmpty()) return null
        return HelpProbe(
            youtubeOk = youtube.count { verdicts[it] == HostVerdict.Ok },
            youtubeTotal = youtube.size,
            discordOk = discord.count { verdicts[it] == HostVerdict.Ok },
            discordTotal = discord.size,
            averageMs = -1
        )
    }

    suspend fun checkHostsDirect(hosts: List<String>): Map<String, HostVerdict> = withContext(Dispatchers.IO) {
        coroutineScope {
            hosts.map { host -> async(Dispatchers.IO) { host to HelpHostCheck.check(host, null, null) } }.awaitAll().toMap()
        }
    }

    private fun strategyLabel(context: Context, strategy: String): String =
        "«${StrategyTestManager.getStrategyName(strategy, context)}»"

    suspend fun bestDiscordStrategy(context: Context, liveProbe: Boolean): Triple<String, String, String?>? {
        val app = context.applicationContext
        val kind = app.currentNetworkKind() ?: return null
        val current = currentStrategy(app)
        val currentKey = current?.let(StrategyTester::normalizeStrategy)
        val currentResult = StrategyTestManager.resultFor(kind, current)
        val stored = StrategyTestManager.verifyResults[kind].orEmpty()
            .filter { it.discordTotal > 0 && StrategyTester.normalizeStrategy(it.strategy) != currentKey }
            .maxWithOrNull(compareBy<StrategyTestManager.RankedConfig> { it.discordOk }.thenBy { it.youtubeOk }.thenBy { -(if (it.averageMs < 0) Long.MAX_VALUE / 2 else it.averageMs) })
        val currentDiscord = currentResult?.discordOk ?: -1
        if (stored != null && stored.discordOk > currentDiscord && stored.discordOk > 0) {
            return Triple(
                stored.strategy,
                "${strategyLabel(app, stored.strategy)}: Discord ${stored.discordOk}/${stored.discordTotal}",
                currentResult?.let { "${it.discordOk}/${it.discordTotal}" }
            )
        }
        if (!liveProbe || StrategyTestManager.isTesting || StrategyTestManager.isVerifying) return null
        val tester = StrategyTester(app)
        val currentProbe = current?.let { tester.probeStrategy(it) }
        val candidates = (StrategyTestManager.configChoices(kind) + StrategyTester.DISCORD_COMBINED_STRATEGY)
            .map { StrategyTester.normalizeStrategy(it) }
            .distinct()
            .filter { it != currentKey }
            .take(LIVE_DISCORD_CANDIDATES)
        var best: Pair<String, StrategyTester.StrategyProbeResult>? = null
        for (candidate in candidates) {
            val result = tester.probeStrategy(candidate)
            if (result.discordTotal == 0) continue
            val leader = best?.second
            if (leader == null || result.discordOk > leader.discordOk || (result.discordOk == leader.discordOk && result.youtubeOk > leader.youtubeOk)) {
                best = candidate to result
            }
            if (result.discordOk == result.discordTotal && result.youtubeOk == result.youtubeTotal) break
        }
        val winner = best ?: return null
        if (winner.second.discordOk <= (currentProbe?.discordOk ?: -1)) return null
        return Triple(
            winner.first,
            "${strategyLabel(app, winner.first)}: Discord ${winner.second.discordOk}/${winner.second.discordTotal}",
            currentProbe?.let { "${it.discordOk}/${it.discordTotal}" }
        )
    }

    private const val LIVE_DISCORD_CANDIDATES = 8

    private fun internetDirect(): Boolean =
        reachableDirect("ya.ru") || reachableDirect("connectivitycheck.gstatic.com/generate_204")

    private fun onEthernet(context: Context): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.allNetworks.any { network ->
            val caps = cm.getNetworkCapabilities(network) ?: return@any false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    } catch (_: Exception) {
        false
    }

    private suspend fun tgTrafficFlowing(): Boolean? {
        if (!TgProxyController.isRunning) return null
        val first = runCatching { NativeProxy.getStats()?.let(TgProxyStatsSample::parse) }.getOrNull() ?: return null
        delay(TG_SAMPLE_GAP_MS)
        val second = runCatching { NativeProxy.getStats()?.let(TgProxyStatsSample::parse) }.getOrNull() ?: return null
        val moved = second.downloaded != first.downloaded || second.routedConnections > first.routedConnections
        return when {
            moved -> true
            second.totalConnections > first.totalConnections -> false
            else -> null
        }
    }

    private fun previousVersion(current: String): String? =
        CompatibilityManager.behaviorChoices.filter { VersionOrder.compare(it, current) < 0 }.maxWithOrNull(VersionOrder)
            ?: CompatibilityManager.behaviorChoices.filter { VersionOrder.compare(it, CompatibilityManager.latestVersion) < 0 }.maxWithOrNull(VersionOrder)

    suspend fun collect(context: Context, symptom: HelpSymptom, pickedPackage: String?): HelpFacts = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val prefs = app.getPreferences()
        val selected = prefs.selectedAppsWithDefaults()
        val mode = prefs.appListMode()
        val kind = app.currentNetworkKind()
        val smartTube = smartTubePackages.firstOrNull { isInstalled(app, it) }
        val target = pickedPackage
            ?: if (symptom == HelpSymptom.Tv) smartTube
            else if (HelpPlanner.appForSymptom(symptom) == HelpPlanner.YOUTUBE_PACKAGE) preferredYoutube(app)
            else HelpPlanner.appForSymptom(symptom)?.takeIf { isInstalled(app, it) }
        val browser = if (symptom == HelpSymptom.YoutubeBrowser) defaultBrowser(app) else null
        val strategy = currentStrategy(app)
        val busy = StrategyTestManager.isTesting || StrategyTestManager.isVerifying
        val bypassRunning = com.rupleide.netfix.data.appStatus.first == AppStatus.Running || ServiceManager.isConnected
        val behavior = CompatibilityManager.behaviorVersion(prefs)
        val privateDns = privateDnsHost(app)
        val tgSymptom = symptom == HelpSymptom.TelegramNoConnect || symptom == HelpSymptom.TelegramDrops

        coroutineScope {
            val internet = async { internetDirect() }
            val probe = async {
                if (symptom in HelpPlanner.probesStrategy && strategy != null && !busy) probeCurrent(app) else null
            }
            val hosts = target?.let { HelpPlanner.knownAppHosts[it] }.orEmpty()
            val viaBypass = async {
                if (hosts.isNotEmpty() && symptom != HelpSymptom.Discord) checkHostsViaNetFix(app, hosts) else emptyMap()
            }
            val direct = async {
                if (hosts.isNotEmpty() && symptom != HelpSymptom.Discord) checkHostsDirect(hosts) else emptyMap()
            }
            val discordBest = async { if (symptom == HelpSymptom.Discord) bestDiscordStrategy(app, liveProbe = false) else null }
            val tgFlow = async { if (tgSymptom) tgTrafficFlowing() else null }
            val dnsSpoofed = async { if (symptom in HelpPlanner.spoofSensitive) networkDnsSpoofed() else false }
            val viaVerdicts = recheckFailed(viaBypass.await()) { failed -> checkHostsViaNetFix(app, failed) }
            val directVerdicts = direct.await()
            val discord = discordBest.await()

            HelpFacts(
                bypassWanted = prefs.getBoolean("wants_youtube_bypass", true),
                bypassRunning = bypassRunning,
                tgWanted = prefs.getBoolean("telegram_proxy_enabled_by_user", true),
                tgProxyRunning = TgProxyController.isRunning,
                tgPortOpen = TgProxyController.isRunning &&
                    TgProxyController.isPortOpen(TgProxyController.DEFAULT_BIND_IP, TgProxyController.getPort(app), 500),
                tgTrafficFlowing = tgFlow.await(),
                tgPortMovedFrom = prefs.getInt(TgProxyController.KEY_PORT_MOVED_FROM, -1).takeIf { it > 0 },
                tgCfPriority = TgProxyController.isCfPriority(app),
                tgStable = prefs.getBoolean("tg_proxy_wakelock_enabled", false),
                vpnPermissionMissing = !ServiceManager.rootMode(app) && runCatching { ServiceManager.prepare(app) != null }.getOrDefault(false),
                otherVpnActive = otherVpnActive(app),
                privateDnsActive = privateDns != null,
                privateDnsHost = privateDns?.takeIf { it.isNotBlank() },
                networkDnsSpoofed = dnsSpoofed.await(),
                internetDirect = internet.await(),
                networkKind = kind,
                onEthernet = onEthernet(app),
                isTv = isTv(app),
                hasScanForNetwork = kind != null && (StrategyTestManager.verifyResults[kind].orEmpty().isNotEmpty() || StrategyTestManager.hasScanResults()),
                nextWorkingStrategy = nextWorkingStrategy(app),
                fasterStrategy = fasterStrategy(app),
                currentProbe = probe.await(),
                appListMode = mode,
                youtubeInBypass = preferredYoutube(app)?.let { it in selected } ?: true,
                targetPackage = target,
                targetLabel = target?.let { appLabel(app, it) },
                targetInBypass = target != null && carriedByNetFix(app, target, selected),
                targetHostsViaBypass = viaVerdicts.mapValues { it.value == HostVerdict.Ok },
                targetHostsDirect = directVerdicts.mapValues { it.value == HostVerdict.Ok },
                targetRegionBlocked = (viaVerdicts.values + directVerdicts.values).any { it == HostVerdict.RegionBlocked },
                systemDnsWorks = runCatching { java.net.InetAddress.getByName("www.youtube.com"); true }.getOrDefault(false),
                discordBestStrategy = discord?.first,
                discordBestLabel = discord?.second,
                discordCurrentLabel = discord?.third,
                browserPackage = browser,
                browserInBypass = browser != null && browser in selected,
                smartTubeInstalled = smartTube != null,
                smartTubeInBypass = smartTube != null && smartTube in selected,
                oldAndroid = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P,
                tunnelDns = CompatibilityManager.tunnelDnsMode(prefs),
                tunnelDnsName = CompatibilityManager.customTunnelDnsName(prefs).takeIf { CompatibilityManager.tunnelDnsMode(prefs) == TunnelDnsMode.Custom },
                behaviorVersion = behavior,
                behaviorLatest = CompatibilityManager.isLatestBehavior(prefs),
                previousVersion = previousVersion(behavior),
                freePortOn = CompatibilityManager.freeProxyPortEnabled(prefs),
                watchdogOn = CompatibilityManager.tgProxyWatchdogEnabled(prefs),
                streamProbeOn = CompatibilityManager.streamProbeEnabled(prefs),
                smartTubeHostsOn = CompatibilityManager.featureEnabled(prefs, CompatFeature.SmartTubeHosts),
                discordCheckOn = StrategyTester.discordCheckEnabled(prefs),
                batteryRestricted = !VendorAutoStartHelper.isBatteryOptimizationIgnored(app),
                networkAssistantRisk = shouldShowNetworkAssistantHint(Build.MANUFACTURER, app.activeConnection(), false),
                dpiPortBusy = !bypassRunning && !ProxyPortResolver.isPortFree("127.0.0.1", ProxyPortResolver.DEFAULT_PROXY_PORT),
                betaExpired = com.rupleide.netfix.core.beta.BetaExpiry.isExpiredNow(app),
                secureDnsOn = CompatibilityManager.secureDnsEnabled(prefs),
                tgCfEnabled = TgProxyController.isCfEnabled(app),
                tgCfPinned = TgProxyController.pinnedCfDomain(app),
                cloudflareFreezeRuledOut = com.rupleide.netfix.core.dpibypass.CloudflareUnfreeze.isEnabled(app) &&
                    com.rupleide.netfix.core.dpibypass.CloudflareUnfreeze.recentlyNotNeeded(app)
            )
        }
    }

    suspend fun measure(context: Context): HelpProbe? = withContext(Dispatchers.IO) {
        probeCurrent(context)
    }
}
