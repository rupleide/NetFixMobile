package com.rupleide.netfix.core.help

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import com.rupleide.netfix.core.dnscheck.DnsCheckResult
import com.rupleide.netfix.core.dnscheck.TunnelDnsCatalog
import com.rupleide.netfix.core.dnscheck.TunnelDnsCheck
import com.rupleide.netfix.core.dpibypass.ACTIVE_STRATEGY_KEY
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.CloudflareUnfreeze
import com.rupleide.netfix.core.dpibypass.CloudflareUnfreezeResult
import com.rupleide.netfix.core.dpibypass.NetworkKind
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.StrategyTester
import com.rupleide.netfix.core.dpibypass.currentNetworkKind
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.dpibypass.launchIntentWithLeanback
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults
import com.rupleide.netfix.core.dpibypass.setAppListMode
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.core.tgproxy.TgProxyService
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.appStatus
import com.rupleide.netfix.service.VendorAutoStartHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class HelpApplyResult {
    data object Done : HelpApplyResult()
    data class NeedsVpnConsent(val intent: Intent) : HelpApplyResult()
    data class Failed(val reason: String) : HelpApplyResult()
}

object HelpActions {
    private const val TAG = "Help"
    private const val START_WAIT_MS = 8_000L
    private const val RESTART_WAIT_MS = 5_000L
    private const val VIDEO_CANDIDATES = 4
    private const val SETTLE_WAIT_MS = 10_000L
    private const val PORT_RELEASE_WAIT_MS = 1_500L
    private const val CLOUDFLARE_DNS = "1.1.1.1"
    private const val FASTER_VIDEO_RATIO = 1.2

    private val strategyKeys = setOf(
        ACTIVE_STRATEGY_KEY,
        "${ACTIVE_STRATEGY_KEY}_wifi",
        "${ACTIVE_STRATEGY_KEY}_mobile",
        "byedpi_enable_cmd_settings",
        "universal_strategy"
    )

    private val tunnelKeys = setOf(
        "selected_apps",
        "applist_type",
        CompatibilityManager.KEY_TUNNEL_DNS_MODE,
        CompatibilityManager.KEY_TUNNEL_DNS_CUSTOM,
        CompatibilityManager.KEY_BEHAVIOR_VERSION,
        "compat_profile",
        CompatFeature.SystemTunnelDns.storageKey,
        CompatFeature.FreeProxyPort.storageKey,
        CompatFeature.CloudflareUnfreeze.storageKey
    ) + CloudflareUnfreeze.KEY_RECORDS

    private val tgKeys = HelpSnapshot.TG_PROXY_KEYS + CompatibilityManager.KEY_BEHAVIOR_VERSION

    private fun isBypassRunning(): Boolean = appStatus.first == AppStatus.Running || ServiceManager.isConnected

    private suspend fun restartBypass(app: Context) {
        ServiceManager.stop(app)
        val stopDeadline = System.currentTimeMillis() + RESTART_WAIT_MS
        while (System.currentTimeMillis() < stopDeadline && (ServiceManager.isAlive || appStatus.first != AppStatus.Halted)) delay(100)
        app.getPreferences().edit().putBoolean("service_enabled", true).apply()
        ServiceManager.start(app, Mode.VPN)
        val startDeadline = System.currentTimeMillis() + RESTART_WAIT_MS
        while (System.currentTimeMillis() < startDeadline && appStatus.first != AppStatus.Running) delay(100)
        Log.i(TAG, "Обход перезапущен: ${if (appStatus.first == AppStatus.Running) "работает" else "не поднялся"}")
    }

    suspend fun restartAffected(context: Context, changedKeys: Set<String>, strategyAlreadyApplied: Boolean, allowStart: Boolean = true) {
        val app = context.applicationContext
        val vpnKeys = if (strategyAlreadyApplied) tunnelKeys else tunnelKeys + strategyKeys
        val tunnelChanged = changedKeys.any { it in vpnKeys }
        val wantsBypass = app.getPreferences().getBoolean("wants_youtube_bypass", true)
        when {
            tunnelChanged && isBypassRunning() -> {
                Log.i(TAG, "Перезапускаю обход: изменено ${changedKeys.filter { it in vpnKeys }}")
                restartBypass(app)
            }
            allowStart && changedKeys.any { it in tunnelKeys } && !isBypassRunning() && wantsBypass && ServiceManager.prepare(app) == null -> {
                Log.i(TAG, "NetFix был выключен, включаю, чтобы изменение заработало")
                startNetFix(app)
            }
        }
        if (TgProxyController.isRunning && changedKeys.any { it in tgKeys }) {
            Log.i(TAG, "Перезапускаю прокси Telegram: изменено ${changedKeys.filter { it in tgKeys }}")
            TgProxyController.restartAsync(app)
        }
    }

    fun bypassUp(): Boolean = isBypassRunning()

    suspend fun keepBypassUp(context: Context, wasUp: Boolean) {
        if (!wasUp || isBypassRunning()) return
        Log.i(TAG, "NetFix выключился во время шага, включаю обратно")
        delay(PORT_RELEASE_WAIT_MS)
        startNetFix(context.applicationContext)
        awaitBypassSettled(context)
    }

    suspend fun startNetFix(context: Context): HelpApplyResult {
        val app = context.applicationContext
        val prefs = app.getPreferences()
        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
        if (wantsYoutube) {
            ServiceManager.prepare(app)?.let { return HelpApplyResult.NeedsVpnConsent(it) }
        }
        prefs.edit().putBoolean("service_enabled", true).apply()
        if (wantsYoutube && !isBypassRunning()) ServiceManager.start(app, Mode.VPN)
        if (wantsTelegram && !TgProxyController.isRunning) {
            if (wantsYoutube) {
                TgProxyController.startAsync(app, onSuccess = {}, onError = {})
            } else {
                TgProxyService.startInForeground(app, openTelegram = false)
            }
        }
        val deadline = System.currentTimeMillis() + START_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            val vpnReady = !wantsYoutube || isBypassRunning()
            val tgReady = !wantsTelegram || TgProxyController.isRunning
            if (vpnReady && tgReady) return HelpApplyResult.Done
            delay(200)
        }
        return if (wantsYoutube && !isBypassRunning()) {
            HelpApplyResult.Failed("Обход не запустился. Посмотрите, не занят ли VPN другим приложением.")
        } else {
            HelpApplyResult.Done
        }
    }

    private suspend fun ensureTgProxyRunning(app: Context): HelpApplyResult {
        if (TgProxyController.isRunning) return HelpApplyResult.Done
        progressText = "Включаю прокси Telegram"
        return try {
            if (isBypassRunning()) {
                TgProxyController.startAsync(app, onSuccess = {}, onError = {})
            } else {
                TgProxyService.startInForeground(app, openTelegram = false)
            }
            val deadline = System.currentTimeMillis() + START_WAIT_MS
            while (!TgProxyController.isRunning && System.currentTimeMillis() < deadline) delay(200)
            if (TgProxyController.isRunning) HelpApplyResult.Done else HelpApplyResult.Failed("Прокси Telegram не включился")
        } finally {
            progressText = null
        }
    }

    suspend fun awaitBypassSettled(context: Context) {
        val app = context.applicationContext
        if (!app.getPreferences().getBoolean("service_enabled", false)) return
        if (!app.getPreferences().getBoolean("wants_youtube_bypass", true)) return
        awaitLivePort(app)
        delay(500)
    }

    private suspend fun awaitLivePort(app: Context): Int? = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + SETTLE_WAIT_MS
        var port = HelpProbes.liveProxyPort(app)
        while (port == null && System.currentTimeMillis() < deadline) {
            delay(250)
            port = HelpProbes.liveProxyPort(app)
        }
        port
    }

    fun undoStart(context: Context, facts: HelpFacts) {
        val app = context.applicationContext
        if (!facts.bypassRunning && isBypassRunning()) {
            Log.i(TAG, "Откат: выключаю обход, который включил помощник")
            app.getPreferences().edit().putBoolean("service_enabled", false).apply()
            ServiceManager.stop(app)
        }
        if (!facts.tgProxyRunning && TgProxyController.isRunning) {
            Log.i(TAG, "Откат: выключаю прокси Telegram, который включил помощник")
            try {
                app.startService(Intent(app, TgProxyService::class.java).setAction(com.rupleide.netfix.data.STOP_ACTION))
            } catch (_: Exception) {
            }
            TgProxyController.stop()
        }
    }

    private fun setBypassApp(context: Context, packageName: String?, include: Boolean): HelpApplyResult {
        if (packageName == null) return HelpApplyResult.Failed("Не понял, какое это приложение")
        val prefs = context.getPreferences()
        val selected = prefs.selectedAppsWithDefaults()
        val packages = HelpProbes.packagesThatCarry(context, packageName)
        val updated = if (include) selected + packages else selected - packageName
        prefs.edit()
            .putStringSet("selected_apps", updated)
            .remove("selected_apps_installed_count")
            .apply()
        return HelpApplyResult.Done
    }

    private suspend fun chooseStrategy(context: Context, strategy: String?): HelpApplyResult {
        if (strategy == null) return HelpApplyResult.Failed("Других рабочих способов в прошлом подборе нет")
        val app = context.applicationContext
        val wasRunning = appStatus.first == AppStatus.Running
        val kind = app.currentNetworkKind()
        if (kind != null) {
            StrategyTestManager.chooseConfigFor(app, kind, strategy)
        } else {
            app.getPreferences().edit()
                .putString(ACTIVE_STRATEGY_KEY, StrategyTester.normalizeStrategy(strategy))
                .putBoolean("byedpi_enable_cmd_settings", true)
                .apply()
            if (wasRunning) ServiceManager.restart(app, Mode.VPN)
        }
        if (wasRunning) {
            awaitRestart(app)
            if (!isBypassRunning() || withContext(Dispatchers.IO) { HelpProbes.liveProxyPort(app) } == null) {
                Log.i(TAG, "Обход не поднялся после смены способа (статус ${appStatus.first}, VPN ${ServiceManager.isConnected}, порт ${ByeDpiVpnService.liveProxyPort}), запускаю ещё раз")
                delay(PORT_RELEASE_WAIT_MS)
                restartBypass(app)
                awaitBypassSettled(app)
            }
            if (!isBypassRunning()) return HelpApplyResult.Failed("NetFix не запустился на этом способе")
        }
        return HelpApplyResult.Done
    }

    private suspend fun awaitRestart(app: Context) {
        val downDeadline = System.currentTimeMillis() + RESTART_WAIT_MS
        while (System.currentTimeMillis() < downDeadline && appStatus.first == AppStatus.Running) delay(100)
        val upDeadline = System.currentTimeMillis() + SETTLE_WAIT_MS
        while (System.currentTimeMillis() < upDeadline && (appStatus.first != AppStatus.Running || withContext(Dispatchers.IO) { HelpProbes.liveProxyPort(app) } == null)) delay(200)
        delay(800)
    }

    var lastChoice: String? = null
        private set

    private const val FRESH_VERIFY_MS = 10L * 60 * 1000
    private const val RECENT_CHECK_MS = 3L * 24 * 60 * 60 * 1000

    private val checkBasedFixes = setOf(
        HelpFix.UseNextStrategy,
        HelpFix.UseFasterVideoStrategy,
        HelpFix.UseDiscordStrategy,
        HelpFix.UseAnotherDiscordStrategy,
        HelpFix.TunnelDnsBest
    )

    private fun strategyCheckAt(app: Context): Long? {
        val kind = app.currentNetworkKind() ?: return null
        if (StrategyTestManager.verifyResults[kind].isNullOrEmpty()) return null
        return StrategyTestManager.verifiedAt[kind]
    }

    private fun dnsCheckAt(app: Context): Long? {
        TunnelDnsCheck.load(app)
        val network = TunnelDnsCheck.networkKey(app)
        val options = dnsOptions(app.getPreferences())
        val stored = options.mapNotNull { TunnelDnsCheck.resultFor(network, it.key) }.filter { it.checkedAt > 0 }
        if (stored.size * 2 < options.size) return null
        return stored.minOf { it.checkedAt }
    }

    private fun checkAt(context: Context, fix: HelpFix): Long? {
        if (fix !in checkBasedFixes) return null
        val app = context.applicationContext
        return if (fix == HelpFix.TunnelDnsBest) dnsCheckAt(app) else strategyCheckAt(app)
    }

    fun savedCheckAt(context: Context, fix: HelpFix): Long? {
        val at = checkAt(context, fix) ?: return null
        val age = System.currentTimeMillis() - at
        return at.takeIf { age in FRESH_VERIFY_MS..RECENT_CHECK_MS }
    }

    fun hasFreshCheck(context: Context, fix: HelpFix): Boolean = isFresh(checkAt(context, fix))

    private fun isFresh(at: Long?): Boolean = at != null && System.currentTimeMillis() - at in 0 until FRESH_VERIFY_MS

    private suspend fun ensureVerified(app: Context, useSaved: Boolean): List<StrategyTestManager.RankedConfig> {
        val kind = app.currentNetworkKind() ?: return emptyList()
        val saved = StrategyTestManager.verifyResults[kind].orEmpty()
        if (saved.isNotEmpty() && (useSaved || isFresh(StrategyTestManager.verifiedAt[kind]))) {
            Log.i(TAG, "Беру прошлую проверку способов на сети ${kind.displayName}")
            return saved
        }
        val scope = if (kind == NetworkKind.Mobile) StrategyTestManager.VerifyScope.Mobile else StrategyTestManager.VerifyScope.Wifi
        Log.i(TAG, "Проверяю способы на сети ${kind.displayName}")
        var job = StrategyTestManager.startVerification(app, scope)
        if (job == null && !StrategyTestManager.isVerifying && !StrategyTestManager.isTesting) {
            Log.i(TAG, "Подбора ещё не было, сначала подбираю способы")
            StrategyTestManager.startTesting(app)?.join()
            job = StrategyTestManager.startVerification(app, scope)
        }
        job?.join()
        awaitBypassSettled(app)
        return StrategyTestManager.verifyResults[kind].orEmpty()
    }

    fun pickBest(
        results: List<StrategyTestManager.RankedConfig>,
        current: String?,
        forDiscord: Boolean,
        exclude: Set<String> = emptySet(),
        allowEqual: Boolean = false
    ): StrategyTestManager.RankedConfig? {
        val currentKey = current?.let(StrategyTester::normalizeStrategy)
        val excluded = exclude.map(StrategyTester::normalizeStrategy).toSet()
        val score: (StrategyTestManager.RankedConfig) -> Int = {
            if (forDiscord) it.discordOk * 100 + it.youtubeOk else it.youtubeOk * 100 + it.discordOk
        }
        val currentScore = results.firstOrNull { StrategyTester.normalizeStrategy(it.strategy) == currentKey }?.let(score) ?: -1
        val best = results
            .filter { StrategyTester.normalizeStrategy(it.strategy).let { key -> key != currentKey && key !in excluded } }
            .maxWithOrNull(compareBy(score).thenBy { -(if (it.averageMs < 0) Long.MAX_VALUE / 2 else it.averageMs) })
            ?: return null
        val works = if (forDiscord) best.discordOk > 0 else best.youtubeOk > 0
        val beatsCurrent = score(best) > currentScore || (allowEqual && score(best) == currentScore)
        return best.takeIf { works && beatsCurrent }
    }

    private suspend fun chooseFasterVideo(app: Context, useSaved: Boolean): HelpApplyResult {
        val original = HelpProbes.currentStrategy(app)
        val results = ensureVerified(app, useSaved)
        if (results.isEmpty()) return HelpApplyResult.Failed("Не получилось проверить способы на этой сети")
        val originalKey = original?.let(StrategyTester::normalizeStrategy)
        val currentSpeed = HelpMeasure.videoSpeedCurrent(app)?.takeIf { it.loaded }?.speedKbps ?: 0L
        val candidates = results
            .filter { it.youtubeOk > 0 && StrategyTester.normalizeStrategy(it.strategy) != originalKey }
            .sortedByDescending { it.youtubeOk }
            .take(VIDEO_CANDIDATES)
        val wasRunning = StrategyTester(app).stopRunningVpnForTests()
        var best: Pair<String, Long>? = null
        try {
            for (candidate in candidates) {
                val speed = HelpMeasure.videoSpeed(app, candidate.strategy)?.takeIf { it.loaded }?.speedKbps ?: continue
                Log.i(TAG, "Скорость видео ${candidate.hostBreakdown}: $speed КБ/с")
                if (best == null || speed > best.second) best = candidate.strategy to speed
            }
        } finally {
            if (wasRunning) {
                app.getPreferences().edit().putBoolean("service_enabled", true).apply()
                ServiceManager.start(app, Mode.VPN)
            }
        }
        if (wasRunning) {
            val deadline = System.currentTimeMillis() + SETTLE_WAIT_MS
            while (System.currentTimeMillis() < deadline && appStatus.first != AppStatus.Running) delay(100)
            awaitBypassSettled(app)
        }
        val winner = best ?: return HelpApplyResult.Failed("Ни на одном способе видео не загрузилось")
        if (winner.second <= currentSpeed * FASTER_VIDEO_RATIO) {
            return HelpApplyResult.Failed("Быстрее текущего способа нет, сейчас видео $currentSpeed КБ/с")
        }
        lastChoice = "Включил «${StrategyTestManager.getStrategyName(winner.first, app)}»: видео ${winner.second} КБ/с, было $currentSpeed КБ/с"
        return chooseStrategy(app, winner.first)
    }

    private suspend fun chooseBestVerified(app: Context, forDiscord: Boolean, tried: Set<String>, useSaved: Boolean): HelpApplyResult {
        val original = HelpProbes.currentStrategy(app)
        val results = ensureVerified(app, useSaved)
        if (results.isEmpty()) return HelpApplyResult.Failed("Не получилось проверить способы на этой сети")
        Log.i(TAG, "Проверка способов: ${results.size} шт., у текущего ${results.firstOrNull { StrategyTester.normalizeStrategy(it.strategy) == original?.let(StrategyTester::normalizeStrategy) }?.hostBreakdown ?: "нет в списке"}")
        val best = pickBest(results, original, forDiscord, tried, allowEqual = true)
            ?: return HelpApplyResult.Failed(noBetterReason(results, original, forDiscord))
        Log.i(TAG, "Лучший способ по проверке: ${best.hostBreakdown}")
        lastChoice = "Включил «${StrategyTestManager.getStrategyName(best.strategy, app)}»: ${best.hostBreakdown}"
        return chooseStrategy(app, best.strategy)
    }

    private fun noBetterReason(results: List<StrategyTestManager.RankedConfig>, original: String?, forDiscord: Boolean): String {
        val service = if (forDiscord) "Discord" else "YouTube"
        val workingCount = results.count { if (forDiscord) it.discordOk > 0 else it.youtubeOk > 0 }
        val current = results.firstOrNull { StrategyTester.normalizeStrategy(it.strategy) == original?.let(StrategyTester::normalizeStrategy) }
        return when {
            workingCount == 0 -> "Проверил ${results.size} способов, $service не открылся ни на одном"
            current != null -> {
                val ok = if (forDiscord) current.discordOk else current.youtubeOk
                val total = if (forDiscord) current.discordTotal else current.youtubeTotal
                "Проверил ${results.size} способов, рабочих для $service: $workingCount. Ваш и так лучший, $service $ok из $total"
            }
            else -> "Проверил ${results.size} способов, рабочих для $service: $workingCount, но лучше текущего нет"
        }
    }

    private suspend fun rescan(context: Context): HelpApplyResult {
        val app = context.applicationContext
        val job = StrategyTestManager.startTesting(app) ?: return HelpApplyResult.Failed("Подбор уже идёт, дождитесь его окончания")
        job.join()
        if (StrategyTestManager.hasConnectionError) return HelpApplyResult.Failed("Нет интернета, подбор не запустился")
        awaitBypassSettled(app)
        val best = StrategyTestManager.bestStrategyResult ?: return HelpApplyResult.Failed("Подбор не нашёл рабочих способов")
        lastChoice = "Включил «${StrategyTestManager.getStrategyName(best, app)}»"
        return chooseStrategy(app, best)
    }

    private fun open(context: Context, intent: Intent, fallback: Intent? = null): HelpApplyResult {
        val app = context.applicationContext
        return try {
            app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            HelpApplyResult.Done
        } catch (_: Exception) {
            if (fallback != null) open(app, fallback) else HelpApplyResult.Failed("Не получилось открыть настройки, найдите их вручную")
        }
    }

    private fun appDetails(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    fun openTelegramProxy(context: Context): HelpApplyResult {
        val app = context.applicationContext
        val url = TgProxyController.getTgProxyUrl(
            TgProxyController.DEFAULT_BIND_IP,
            TgProxyController.getPort(app),
            TgProxyController.getOrGenerateSecret(app)
        )
        return try {
            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            app.getPreferences().edit().putBoolean("tg_proxy_configured", true).apply()
            TgProxyController.dismissPortMovedHint(app)
            HelpApplyResult.Done
        } catch (_: Exception) {
            HelpApplyResult.Failed("Telegram не установлен")
        }
    }

    var progressText by mutableStateOf<String?>(null)
        private set
    private var dnsRanking: List<HelpPlanner.DnsRank> = emptyList()
    private val triedDns = mutableSetOf<String>()

    private var dnsChecked = false

    val dnsCannotHelp: Boolean get() = dnsChecked && dnsRanking.none { it.targetOk && it.key !in triedDns }

    fun resetDnsChoice() {
        dnsRanking = emptyList()
        triedDns.clear()
        dnsChecked = false
    }

    private class DnsOption(val key: String, val systemDns: Boolean, val servers: suspend () -> List<String>)

    private fun dnsOptions(prefs: android.content.SharedPreferences): List<DnsOption> = buildList {
        add(DnsOption(TunnelDnsCatalog.SYSTEM, true) { emptyList() })
        add(DnsOption(TunnelDnsCatalog.CLOUDFLARE, false) { listOf(CLOUDFLARE_DNS) })
        TunnelDnsCatalog.presets.forEach { preset ->
            add(DnsOption(preset.name, false) { TunnelDnsCheck.resolveServers(preset.address) })
        }
        if (currentDnsKey(prefs) == TunnelDnsCatalog.CUSTOM) {
            val own = CompatibilityManager.parseDnsServers(CompatibilityManager.customTunnelDns(prefs)).filter { !it.contains(':') }
            if (own.isNotEmpty()) add(DnsOption(TunnelDnsCatalog.CUSTOM, false) { own })
        }
    }

    private fun currentDnsKey(prefs: android.content.SharedPreferences): String = when (CompatibilityManager.tunnelDnsMode(prefs)) {
        TunnelDnsMode.System -> TunnelDnsCatalog.SYSTEM
        TunnelDnsMode.Cloudflare -> TunnelDnsCatalog.CLOUDFLARE
        TunnelDnsMode.Custom -> CompatibilityManager.customTunnelDnsName(prefs)?.takeIf { TunnelDnsCatalog.preset(it) != null } ?: TunnelDnsCatalog.CUSTOM
    }

    private suspend fun chooseTunnelDns(app: Context, target: String?, next: Boolean, useSaved: Boolean): HelpApplyResult {
        val prefs = app.getPreferences()
        val currentKey = currentDnsKey(prefs)
        if (!next || dnsRanking.isEmpty()) {
            val options = dnsOptions(prefs)
            val results = linkedMapOf<String, DnsCheckResult>()
            val savedAt = dnsCheckAt(app)
            if (savedAt != null && (useSaved || isFresh(savedAt))) {
                val network = TunnelDnsCheck.networkKey(app)
                options.forEach { option -> TunnelDnsCheck.resultFor(network, option.key)?.let { results[option.key] = it } }
                Log.i(TAG, "Беру прошлую проверку DNS: ${results.size} из ${options.size}")
            } else try {
                options.forEachIndexed { i, option ->
                    progressText = "Проверяю DNS ${option.key}, ${i + 1} из ${options.size}"
                    results[option.key] = TunnelDnsCheck.checkNow(app, option.key, option.servers, option.systemDns)
                }
            } finally {
                progressText = null
            }
            dnsRanking = HelpPlanner.rankDns(results, target)
            dnsChecked = true
            Log.i(TAG, "Проверка DNS для ${target ?: "всего"}: ${dnsRanking.joinToString { "${it.key} ${if (it.targetOk) "да" else "нет"} ${it.okTargets}/${TunnelDnsCheck.targets.size}" }}, сейчас $currentKey")
        }
        triedDns.add(currentKey)
        val service = target ?: "сервисы"
        val pick = HelpPlanner.pickDns(dnsRanking, currentKey, triedDns, atLeastAsGoodAsCurrent = !next)
            ?: return HelpApplyResult.Failed(
                when {
                    dnsRanking.isEmpty() -> "Ни один DNS не ответил через NetFix"
                    dnsRanking.none { it.targetOk } -> "Проверил все DNS, $service не открывается ни с одним"
                    dnsRanking.first().key == currentKey -> "Лучший DNS уже включён: $currentKey"
                    else -> "Других DNS, где $service открывается, нет"
                }
            )
        triedDns.add(pick.key)
        val applied = when (pick.key) {
            TunnelDnsCatalog.SYSTEM -> CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.System).let { true }
            TunnelDnsCatalog.CLOUDFLARE -> CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.Cloudflare).let { true }
            TunnelDnsCatalog.CUSTOM -> CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.Custom).let { true }
            else -> {
                val preset = TunnelDnsCatalog.preset(pick.key)
                val servers = preset?.let { TunnelDnsCheck.resolveServers(it.address) }.orEmpty()
                servers.isNotEmpty() && CompatibilityManager.setCustomTunnelDns(prefs, servers.joinToString(", "), pick.key)
            }
        }
        if (!applied) return HelpApplyResult.Failed("Не получилось поставить DNS ${pick.key}")
        lastChoice = "Включил DNS ${pick.key}: по проверке открывается ${pick.okTargets} из ${TunnelDnsCheck.targets.size} сервисов"
        return HelpApplyResult.Done
    }

    private suspend fun unfreezeCloudflare(app: Context): HelpApplyResult {
        if (!isBypassRunning()) {
            progressText = "Включаю NetFix"
            val started = try {
                startNetFix(app)
            } finally {
                progressText = null
            }
            if (started != HelpApplyResult.Done) return started
            awaitBypassSettled(app)
        }
        val prefs = app.getPreferences()
        if (!CloudflareUnfreeze.isEnabled(app)) CompatibilityManager.setFeature(prefs, CompatFeature.CloudflareUnfreeze, true)
        if (CloudflareUnfreeze.savedIp(app) != null) {
            CloudflareUnfreeze.forget(app)
            restartBypass(app)
            awaitBypassSettled(app)
        }
        val port = awaitLivePort(app) ?: return HelpApplyResult.Failed("NetFix не отвечает, попробуйте ещё раз")
        progressText = "Ищу адрес Cloudflare, который не морозится"
        val detection = try {
            CloudflareUnfreeze.detect(app, port)
        } finally {
            progressText = null
        }
        return when (detection.result) {
            CloudflareUnfreezeResult.Found -> {
                restartBypass(app)
                awaitBypassSettled(app)
                lastChoice = "Нашёл рабочий адрес Cloudflare ${detection.ip}, пустил через него"
                HelpApplyResult.Done
            }
            CloudflareUnfreezeResult.NotNeeded -> HelpApplyResult.Failed("Эта сеть не морозит Cloudflare, дело не в этом")
            CloudflareUnfreezeResult.NothingWorks -> HelpApplyResult.Failed("Проверил адреса Cloudflare, на этой сети морозятся все")
            CloudflareUnfreezeResult.Unavailable -> HelpApplyResult.Failed("Не понял, какая сейчас сеть")
        }
    }

    suspend fun apply(context: Context, fix: HelpFix, facts: HelpFacts, tried: Set<String> = emptySet(), dnsTarget: String? = null, useSaved: Boolean = false): HelpApplyResult = withContext(Dispatchers.Main) {
        val app = context.applicationContext
        val prefs = app.getPreferences()
        lastChoice = null
        Log.i(TAG, "Применяю: ${fix.title}")
        when (fix) {
            HelpFix.StartBypass, HelpFix.StartTgProxy -> startNetFix(app)
            HelpFix.RequestVpnPermission ->
                ServiceManager.prepare(app)?.let { HelpApplyResult.NeedsVpnConsent(it) } ?: startNetFix(app)
            HelpFix.AddAppToBypass -> setBypassApp(app, facts.targetPackage, include = true)
            HelpFix.AddBrowserToBypass -> setBypassApp(app, facts.browserPackage, include = true)
            HelpFix.RemoveAppFromBypass -> setBypassApp(app, facts.targetPackage, include = false)
            HelpFix.UseNextStrategy -> chooseBestVerified(app, forDiscord = false, tried = tried, useSaved = useSaved)
            HelpFix.UseFasterVideoStrategy -> chooseFasterVideo(app, useSaved)
            HelpFix.UseDiscordStrategy, HelpFix.UseAnotherDiscordStrategy -> chooseBestVerified(app, forDiscord = true, tried = tried, useSaved = useSaved)
            HelpFix.CloudflareUnfreeze -> unfreezeCloudflare(app)
            HelpFix.ResetSettings -> {
                com.rupleide.netfix.core.AppReset.resetSettingsKeepingResults(app, "из помощника")
                HelpApplyResult.Done
            }
            HelpFix.TunnelDnsBest -> chooseTunnelDns(app, dnsTarget, next = false, useSaved = useSaved)
            HelpFix.TunnelDnsNext -> chooseTunnelDns(app, dnsTarget, next = true, useSaved = useSaved)
            HelpFix.RescanNetwork -> rescan(app)
            HelpFix.EnableDiscordCheck -> {
                prefs.edit().putBoolean(StrategyTester.KEY_SCAN_CHECK_DISCORD, true).apply()
                CompatibilityManager.setDiscordEchCheck(prefs, true)
                HelpApplyResult.Done
            }
            HelpFix.EnableSecureDns -> {
                CompatibilityManager.setFeature(prefs, CompatFeature.SecureDns, true)
                HelpApplyResult.Done
            }
            HelpFix.CheckCfServers -> {
                val proxyUp = ensureTgProxyRunning(app)
                if (proxyUp != HelpApplyResult.Done) return@withContext proxyUp
                progressText = "Проверяю серверы Cloudflare для Telegram"
                val results = try {
                    withContext(Dispatchers.IO) { com.rupleide.netfix.core.tgproxy.CfProxyChecker.checkAll(app) { } }
                } finally {
                    progressText = null
                }
                val working = results.filter { it.state == com.rupleide.netfix.core.tgproxy.CfServerState.Works }
                val pinned = TgProxyController.pinnedCfDomain(app)
                Log.i(TAG, "Серверы Cloudflare: работают ${working.size} из ${results.size}, закреплён ${pinned ?: "нет"}")
                when {
                    !TgProxyController.isRunning && working.isNotEmpty() ->
                        HelpApplyResult.Failed("Серверы Cloudflare работают (${working.size} из ${results.size}), но прокси Telegram выключен. Сначала включите его")
                    working.isEmpty() -> HelpApplyResult.Failed("Ни один сервер Cloudflare сейчас не отвечает, Telegram пойдёт напрямую")
                    pinned != null && working.none { it.domain == pinned } -> {
                        TgProxyController.pinCfDomain(app, null)
                        HelpApplyResult.Done
                    }
                    else -> {
                        if (TgProxyController.isRunning) withContext(Dispatchers.IO) { com.rupleide.netfix.core.tgproxy.NativeProxy.flushPools() }
                        HelpApplyResult.Done
                    }
                }
            }
            HelpFix.EnableStreamProbe -> {
                CompatibilityManager.setFeature(prefs, CompatFeature.StreamProbe, true)
                HelpApplyResult.Done
            }
            HelpFix.EnableSmartTubeHosts -> {
                CompatibilityManager.setFeature(prefs, CompatFeature.SmartTubeHosts, true)
                HelpApplyResult.Done
            }
            HelpFix.DnsSystem -> {
                CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.System)
                HelpApplyResult.Done
            }
            HelpFix.AllAppsMode -> {
                prefs.setAppListMode(AppListMode.AllTraffic)
                HelpApplyResult.Done
            }
            HelpFix.SelectedAppsMode -> {
                prefs.setAppListMode(AppListMode.SelectedAppsOnly)
                HelpApplyResult.Done
            }
            HelpFix.NetworkLatest -> {
                CompatibilityManager.enableEverythingNew(prefs)
                HelpApplyResult.Done
            }
            HelpFix.NetworkPrevious -> {
                val previous = facts.previousVersion ?: return@withContext HelpApplyResult.Failed("Прошлой версии работы сети нет")
                CompatibilityManager.setBehaviorVersion(prefs, previous)
                HelpApplyResult.Done
            }
            HelpFix.FreePortOn -> {
                CompatibilityManager.setFeature(prefs, CompatFeature.FreeProxyPort, true)
                HelpApplyResult.Done
            }
            HelpFix.TgWatchdogOn -> {
                CompatibilityManager.setFeature(prefs, CompatFeature.TgProxyWatchdog, true)
                HelpApplyResult.Done
            }
            HelpFix.TgCfPriorityOff -> {
                TgProxyController.setCfPriority(app, false)
                HelpApplyResult.Done
            }
            HelpFix.TgStableOn -> {
                prefs.edit().putBoolean("tg_proxy_wakelock_enabled", true).apply()
                if (TgProxyController.isRunning && !isBypassRunning()) TgProxyService.startInForeground(app, openTelegram = false)
                HelpApplyResult.Done
            }
            HelpFix.TgReconnect -> {
                val proxyUp = ensureTgProxyRunning(app)
                if (proxyUp == HelpApplyResult.Done) openTelegramProxy(app) else proxyUp
            }
            HelpFix.DisableOtherVpn -> open(app, Intent(Settings.ACTION_VPN_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
            HelpFix.DisablePrivateDns -> open(app, Intent(Settings.ACTION_WIRELESS_SETTINGS), Intent(Settings.ACTION_SETTINGS))
            HelpFix.ReconnectNetwork -> open(app, Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
            HelpFix.BatteryUnrestricted -> {
                VendorAutoStartHelper.requestIgnoreBatteryOptimization(app)
                HelpApplyResult.Done
            }
            HelpFix.VendorAutostart ->
                if (VendorAutoStartHelper.openVendorAutoStartSettings(app)) HelpApplyResult.Done else open(app, appDetails(app.packageName))
            HelpFix.NetworkAssistant -> open(app, Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
            HelpFix.RestartYoutube -> open(app, appDetails(facts.targetPackage ?: HelpProbes.preferredYoutube(app) ?: HelpPlanner.YOUTUBE_PACKAGE))
            HelpFix.SmartTubeConscrypt -> {
                val smartTubeLaunch = listOf("org.smarttube.stable", "org.smarttube.beta", "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta", "com.liskovsoft.smarttubetv")
                    .firstNotNullOfOrNull { app.packageManager.launchIntentWithLeanback(it) }
                if (smartTubeLaunch != null) open(app, smartTubeLaunch) else HelpApplyResult.Failed("SmartTube не найден, откройте его вручную")
            }
            HelpFix.AdviceVpn, HelpFix.AdviceSmartTube, HelpFix.AdviceVpnDetected, HelpFix.AdviceNoInternet -> HelpApplyResult.Done
        }
    }
}
