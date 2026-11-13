package com.rupleide.netfix.core.help

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rupleide.netfix.BuildConfig
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import com.rupleide.netfix.core.dpibypass.appListMode
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults
import com.rupleide.netfix.core.tgproxy.TgProxyStatsSample
import com.rupleide.netfix.service.VendorAutoStartHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HelpStage { Pick, PickApp, Checking, Fix, Applying, Verify, Done, Report }

enum class HelpReportStep { Consent, Building, Ready }

data class HelpAutoCheck(val passed: Boolean, val text: String)

data class HelpAttempt(val fix: HelpFix, val answer: HelpAnswer, val note: String? = null) {
    val reportLine: String get() = describeAttempt(fix, answer, note)
}

fun describeAttempt(fix: HelpFix, answer: HelpAnswer, note: String?): String = when {
    answer == HelpAnswer.Skipped && fix.role == HelpFixRole.Advice -> "Показан совет"
    answer == HelpAnswer.Skipped && note != null -> "Не получилось ($note)"
    else -> answer.label + (note?.let { " ($it)" } ?: "")
}

object HelpSession {
    private const val TAG = "Help"
    private const val MAX_CHAT_TEXT = 3500
    private const val MAX_CAPTION = 1000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

    var stage by mutableStateOf(HelpStage.Pick)
        private set
    var symptom by mutableStateOf<HelpSymptom?>(null)
        private set
    var pickedPackage by mutableStateOf<String?>(null)
        private set
    var facts by mutableStateOf<HelpFacts?>(null)
        private set
    var findings by mutableStateOf<List<HelpFinding>>(emptyList())
        private set
    var queue by mutableStateOf<List<HelpFix>>(emptyList())
        private set
    var index by mutableStateOf(0)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    var baseline by mutableStateOf<HelpScore?>(null)
        private set
    var pendingConsent by mutableStateOf<Intent?>(null)
        private set
    var solved by mutableStateOf(false)
        private set
    var reportStep by mutableStateOf(HelpReportStep.Consent)
        private set
    var reportFile by mutableStateOf<java.io.File?>(null)
        private set
    var autoCheck by mutableStateOf<HelpAutoCheck?>(null)
        private set
    var checking by mutableStateOf(false)
        private set
    var waitingForUser by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
    var appliedNote by mutableStateOf<String?>(null)
        private set
    var busyText by mutableStateOf<String?>(null)
        private set
    var userNote by mutableStateOf("")
    val attempts = mutableStateListOf<HelpAttempt>()

    private var fixSnapshot: Map<String, Any?>? = null
    var scoreBeforeFix by mutableStateOf<HelpScore?>(null)
        private set
    var scoreAfterFix by mutableStateOf<HelpScore?>(null)
        private set
    private var telegramBaseline: TgProxyStatsSample? = null
    private val triedStrategies = mutableSetOf<String>()
    private var loggingWasOff = false
    private var loggingOwned = false
    private var job: Job? = null

    val currentFix: HelpFix? get() = queue.getOrNull(index)

    val isBusy: Boolean get() = stage == HelpStage.Checking || stage == HelpStage.Applying || reportStep == HelpReportStep.Building

    fun reset() {
        if (isBusy) return
        stage = HelpStage.Pick
        symptom = null
        pickedPackage = null
        facts = null
        findings = emptyList()
        queue = emptyList()
        index = 0
        errorText = null
        baseline = null
        pendingConsent = null
        solved = false
        reportStep = HelpReportStep.Consent
        reportFile = null
        userNote = ""
        autoCheck = null
        waitingForUser = false
        notice = null
        appliedNote = null
        attempts.clear()
        fixSnapshot = null
        scoreBeforeFix = null
        scoreAfterFix = null
        telegramBaseline = null
        triedStrategies.clear()
        HelpActions.resetDnsChoice()
    }

    fun choose(context: Context, chosen: HelpSymptom) {
        reset()
        symptom = chosen
        Log.i(TAG, "Помощник: выбрана проблема «${chosen.title}»")
        when {
            chosen == HelpSymptom.Other -> openReport()
            chosen.needsAppPick -> stage = HelpStage.PickApp
            else -> runChecks(context)
        }
    }

    fun pickApp(context: Context, packageName: String) {
        pickedPackage = packageName
        runChecks(context)
    }

    fun back() {
        if (isBusy) return
        when (stage) {
            HelpStage.PickApp, HelpStage.Done -> reset()
            HelpStage.Report -> if (reportStep == HelpReportStep.Consent) {
                if (queue.isEmpty()) reset() else stage = HelpStage.Done
            } else {
                stage = HelpStage.Done
            }
            HelpStage.Fix, HelpStage.Verify -> reset()
            else -> Unit
        }
    }

    fun runChecks(context: Context) {
        val chosen = symptom ?: return
        stage = HelpStage.Checking
        errorText = null
        val app = context.applicationContext
        job = scope.launch {
            val collected = try {
                HelpProbes.collect(app, chosen, pickedPackage)
            } catch (e: Exception) {
                Log.e(TAG, "Помощник: проверка упала", e)
                HelpFacts()
            }
            appContext = app
            telegramBaseline = HelpMeasure.telegramSample()
            baseline = if (HelpMeasure.kindFor(chosen, collected) == HelpMeasureKind.TelegramTraffic) null else try {
                HelpMeasure.measure(app, chosen, collected, telegramBaseline)
            } catch (e: Exception) {
                null
            }
            facts = if (baseline != null) collected.copy(currentProbe = null) else collected
            rebuildFindings()
            queue = if (chosen == HelpSymptom.ScanZero && baseline?.success == true) emptyList() else HelpPlanner.fixes(chosen, collected)
            index = 0
            Log.i(TAG, "Помощник: замер ${baseline?.text}; найдено ${findings.joinToString(" | ") { it.text }}; план: ${queue.joinToString { it.name }}")
            stage = if (queue.isEmpty()) HelpStage.Done else HelpStage.Fix
        }
    }

    private var appContext: Context? = null

    private fun rebuildFindings() {
        val chosen = symptom ?: return
        val current = facts ?: return
        val list = HelpPlanner.findings(chosen, current).toMutableList()
        baseline?.let { list.add(0, HelpFinding(if (it.success) HelpLevel.Ok else HelpLevel.Bad, it.text)) }
        findings = list.sortedBy { if (it.level == HelpLevel.Bad) 0 else if (it.level == HelpLevel.Warn) 1 else 2 }
    }

    private fun refreshFacts() {
        val app = appContext ?: return
        val current = facts ?: return
        val prefs = app.getPreferences()
        val selected = prefs.selectedAppsWithDefaults()
        val privateDns = HelpProbes.privateDnsHost(app)
        val tgRunningNow = com.rupleide.netfix.core.tgproxy.TgProxyController.isRunning
        facts = current.copy(
            bypassRunning = com.rupleide.netfix.core.dpibypass.ServiceManager.isConnected ||
                com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running,
            tgProxyRunning = tgRunningNow,
            tgPortOpen = when {
                !tgRunningNow -> false
                !current.tgProxyRunning -> true
                else -> current.tgPortOpen
            },
            secureDnsOn = com.rupleide.netfix.core.compat.CompatibilityManager.secureDnsEnabled(prefs),
            freePortOn = com.rupleide.netfix.core.compat.CompatibilityManager.freeProxyPortEnabled(prefs),
            tgCfPinned = com.rupleide.netfix.core.tgproxy.TgProxyController.pinnedCfDomain(app),
            youtubeInBypass = HelpProbes.preferredYoutube(app)?.let { it in selected } ?: true,
            targetInBypass = current.targetPackage?.let { HelpProbes.carriedByNetFix(app, it, selected) } ?: false,
            browserInBypass = current.browserPackage != null && current.browserPackage in selected,
            smartTubeInBypass = HelpProbes.smartTubePackages.any { it in selected },
            appListMode = prefs.appListMode(),
            tunnelDns = com.rupleide.netfix.core.compat.CompatibilityManager.tunnelDnsMode(prefs),
            tunnelDnsName = com.rupleide.netfix.core.compat.CompatibilityManager.customTunnelDnsName(prefs)
                .takeIf { com.rupleide.netfix.core.compat.CompatibilityManager.tunnelDnsMode(prefs) == com.rupleide.netfix.core.compat.TunnelDnsMode.Custom },
            privateDnsActive = privateDns != null,
            privateDnsHost = privateDns?.takeIf { it.isNotBlank() },
            otherVpnActive = HelpProbes.otherVpnActive(app),
            behaviorVersion = com.rupleide.netfix.core.compat.CompatibilityManager.behaviorVersion(prefs),
            behaviorLatest = com.rupleide.netfix.core.compat.CompatibilityManager.isLatestBehavior(prefs),
            tgCfPriority = com.rupleide.netfix.core.tgproxy.TgProxyController.isCfPriority(app),
            watchdogOn = com.rupleide.netfix.core.compat.CompatibilityManager.tgProxyWatchdogEnabled(prefs),
            tgStable = prefs.getBoolean("tg_proxy_wakelock_enabled", false),
            batteryRestricted = !VendorAutoStartHelper.isBatteryOptimizationIgnored(app),
            currentProbe = null
        )
        rebuildFindings()
    }

    private fun record(context: Context, fix: HelpFix, answer: HelpAnswer, note: String? = null) {
        attempts.add(HelpAttempt(fix, answer, note))
        val line = "${timeFormat.format(Date())} ${symptom?.title ?: ""}: ${fix.titleFor(facts?.targetLabel)}: ${describeAttempt(fix, answer, note)}"
        HelpRepairStore.appendJournal(context, line)
        Log.i(TAG, "Помощник: $line")
    }

    private fun fixName(fix: HelpFix): String = "«${fix.titleFor(facts?.targetLabel)}»"

    fun apply(context: Context, useSaved: Boolean = false) {
        val fix = currentFix ?: return
        val currentFacts = facts ?: HelpFacts()
        errorText = null
        notice = null
        autoCheck = null
        waitingForUser = false
        if (fix.role == HelpFixRole.Advice) {
            record(context, fix, HelpAnswer.Skipped, "показан совет")
            next()
            return
        }
        stage = HelpStage.Applying
        val app = context.applicationContext
        job = scope.launch {
            val prefs = app.getPreferences()
            if (fix.kind == HelpFixKind.Auto) {
                HelpRepairStore.beginIfNeeded(app, symptom ?: HelpSymptom.Other)
                fixSnapshot = HelpSnapshot.capture(prefs)
            } else {
                fixSnapshot = null
            }
            scoreBeforeFix = baseline
            bypassUpBeforeFix = HelpActions.bypassUp()
            tgUpBeforeFix = com.rupleide.netfix.core.tgproxy.TgProxyController.isRunning
            val result = try {
                HelpActions.apply(app, fix, currentFacts, triedStrategies, HelpPlanner.dnsTargetFor(symptom ?: HelpSymptom.Other, currentFacts.targetPackage), useSaved)
            } catch (e: Exception) {
                Log.e(TAG, "Помощник: исправление упало", e)
                HelpApplyResult.Failed("Не получилось: ${e.message ?: e.javaClass.simpleName}")
            }
            when (result) {
                is HelpApplyResult.NeedsVpnConsent -> {
                    pendingConsent = result.intent
                    stage = HelpStage.Fix
                }
                is HelpApplyResult.Failed -> {
                    record(app, fix, HelpAnswer.Skipped, result.reason)
                    val freshCloudflareRecords = prefs.getString(com.rupleide.netfix.core.dpibypass.CloudflareUnfreeze.KEY_RECORDS, null)
                    fixSnapshot?.let { HelpSnapshot.restore(prefs, it) }
                    if (fix == HelpFix.CloudflareUnfreeze && freshCloudflareRecords != null) {
                        prefs.edit().putString(com.rupleide.netfix.core.dpibypass.CloudflareUnfreeze.KEY_RECORDS, freshCloudflareRecords).apply()
                    }
                    fixSnapshot = null
                    if (fix !in setOf(HelpFix.StartBypass, HelpFix.StartTgProxy, HelpFix.RequestVpnPermission)) {
                        HelpActions.undoStart(app, HelpFacts(bypassRunning = bypassUpBeforeFix, tgProxyRunning = tgUpBeforeFix))
                    }
                    notice = "${fixName(fix)} не получилось. ${result.reason}."
                    next()
                }
                HelpApplyResult.Done -> {
                    appliedNote = HelpActions.lastChoice
                    fixSnapshot?.let { snapshot ->
                        HelpActions.restartAffected(app, HelpSnapshot.changedKeys(prefs, snapshot), strategyAlreadyApplied = fix.changesStrategy)
                    }
                    if (fix.changesStrategy) HelpProbes.currentStrategy(app)?.let { triedStrategies.add(it) }
                    HelpActions.keepBypassUp(app, bypassUpBeforeFix)
                    if (fix.role == HelpFixRole.Manual) {
                        stage = HelpStage.Verify
                        waitingForUser = true
                    } else {
                        evaluate(app, fix)
                    }
                }
            }
        }
    }

    private fun manualStateDone(app: Context, fix: HelpFix): Boolean? = when (fix) {
        HelpFix.DisableOtherVpn -> !HelpProbes.otherVpnActive(app)
        HelpFix.DisablePrivateDns -> HelpProbes.privateDnsHost(app) == null
        HelpFix.BatteryUnrestricted -> VendorAutoStartHelper.isBatteryOptimizationIgnored(app)
        else -> null
    }

    private fun manualStateHint(fix: HelpFix): String = when {
        fix == HelpFix.DisableOtherVpn -> "Другой VPN всё ещё включён."
        fix == HelpFix.DisablePrivateDns -> "«Частный DNS» всё ещё включён."
        fix == HelpFix.BatteryUnrestricted -> "Разрешение на работу в фоне ещё не выдано."
        else -> "Пока не вижу изменения."
    }

    private suspend fun evaluate(app: Context, fix: HelpFix) {
        val currentSymptom = symptom ?: return
        if (currentSymptom == HelpSymptom.WontStart && fix != HelpFix.StartBypass) HelpActions.startNetFix(app)
        if (HelpMeasure.kindFor(currentSymptom, facts) == HelpMeasureKind.TelegramTraffic) {
            telegramBaseline = HelpMeasure.telegramSample()
            stage = HelpStage.Verify
            waitingForUser = true
            return
        }
        HelpActions.awaitBypassSettled(app)
        val score = try {
            HelpMeasure.measure(app, currentSymptom, facts, telegramBaseline)
        } catch (e: Exception) {
            null
        }
        decide(app, fix, score)
    }

    private suspend fun decide(app: Context, fix: HelpFix, score: HelpScore?) {
        val currentSymptom = symptom ?: return
        scoreAfterFix = score
        waitingForUser = false
        val before = scoreBeforeFix
        if (score == null) {
            stage = HelpStage.Verify
            return
        }
        autoCheck = HelpAutoCheck(score.success, score.text)
        val improved = before == null || score.value > before.value
        val worse = before != null && score.value < before.value
        val askPersonOnTv = facts?.isTv == true && fix.role == HelpFixRole.Change && !worse
        when {
            askPersonOnTv && !(score.success && currentSymptom in HelpPlanner.doneWithoutQuestion) -> {
                if (score.success || improved) baseline = score
                stage = HelpStage.Verify
            }
            score.success -> {
                baseline = score
                if (currentSymptom in HelpPlanner.doneWithoutQuestion) {
                    record(app, fix, HelpAnswer.Worked, "проверил сам: ${score.text}")
                    fixSnapshot = null
                    solved = true
                    stage = HelpStage.Done
                } else {
                    stage = HelpStage.Verify
                }
            }
            fix.role == HelpFixRole.Change && !improved -> {
                record(app, fix, HelpAnswer.Same, listOfNotNull(appliedNote, "проверил сам: ${score.text}").joinToString(", "))
                revertFixNow(app)
                notice = "${fixName(fix)} не помогло, вернул как было. ${score.text}."
                next()
            }
            else -> {
                baseline = score
                record(app, fix, HelpAnswer.Applied, listOfNotNull(appliedNote, "проверил сам: ${score.text}").joinToString(", "))
                fixSnapshot = null
                notice = if (improved && before != null) {
                    "${fixName(fix)}: стало лучше, но не до конца. ${score.text}."
                } else {
                    "${fixName(fix)}: сделал. ${score.text}."
                }
                next()
            }
        }
    }

    fun checkNow(context: Context) {
        val fix = currentFix ?: return
        if (checking || stage != HelpStage.Verify) return
        val app = context.applicationContext
        checking = true
        scope.launch {
            try {
                val currentSymptom = symptom ?: return@launch
                if (fix.role == HelpFixRole.Manual) {
                    val state = manualStateDone(app, fix)
                    if (state == false) {
                        autoCheck = HelpAutoCheck(false, manualStateHint(fix))
                        return@launch
                    }
                    if (state == null) {
                        waitingForUser = false
                        return@launch
                    }
                    if (currentSymptom == HelpSymptom.WontStart) HelpActions.startNetFix(app)
                }
                val kind = HelpMeasure.kindFor(currentSymptom, facts)
                HelpActions.awaitBypassSettled(app)
                val score = HelpMeasure.measure(app, currentSymptom, facts, telegramBaseline)
                if (kind == HelpMeasureKind.TelegramTraffic && score != null && !score.success) {
                    autoCheck = HelpAutoCheck(false, "${score.text}. Откройте Telegram, подождите 10 секунд и вернитесь.")
                    return@launch
                }
                decide(app, fix, score)
            } finally {
                checking = false
            }
        }
    }

    fun consentHandled(context: Context, granted: Boolean) {
        pendingConsent = null
        if (granted) {
            apply(context)
        } else {
            errorText = "Без разрешения NetFix не запустится"
        }
    }

    fun skip(context: Context) {
        val fix = currentFix ?: return
        record(context, fix, HelpAnswer.Skipped)
        next()
    }

    fun answer(context: Context, answer: HelpAnswer) {
        val fix = currentFix ?: return
        val recorded = if (fix.role == HelpFixRole.Manual && answer == HelpAnswer.Same) HelpAnswer.Manual else answer
        record(context, fix, recorded, scoreAfterFix?.text?.let { "проверка: $it" })
        if (answer == HelpAnswer.Worked) {
            fixSnapshot = null
            solved = true
            stage = HelpStage.Done
            return
        }
        val after = scoreAfterFix
        val keep = fix.role == HelpFixRole.Precondition
        if (answer == HelpAnswer.Worse && fix in setOf(HelpFix.StartBypass, HelpFix.StartTgProxy, HelpFix.RequestVpnPermission)) {
            HelpActions.undoStart(context, facts ?: HelpFacts())
        }
        if (keep && after != null) baseline = after
        if (fix.role == HelpFixRole.Change && !keep) {
            val app = context.applicationContext
            busyText = "Возвращаю как было"
            stage = HelpStage.Applying
            job = scope.launch {
                revertFixNow(app)
                HelpActions.awaitBypassSettled(app)
                busyText = null
                next()
            }
        } else {
            next()
        }
    }

    private suspend fun revertFixNow(app: Context) {
        val snapshot = fixSnapshot ?: return
        fixSnapshot = null
        val prefs = app.getPreferences()
        val changed = HelpSnapshot.changedKeys(prefs, snapshot)
        HelpSnapshot.restore(prefs, snapshot)
        if (changed.any { it.startsWith("byedpi_cmd_args") || it == "universal_strategy" }) {
            com.rupleide.netfix.core.dpibypass.StrategyTestManager.reloadSavedState(app)
        }
        HelpActions.restartAffected(app, changed, strategyAlreadyApplied = false, allowStart = false)
        HelpActions.undoStart(app, HelpFacts(bypassRunning = bypassUpBeforeFix, tgProxyRunning = tgUpBeforeFix))
        HelpActions.keepBypassUp(app, bypassUpBeforeFix)
        Log.i(TAG, "Помощник: откат исправления, вернул ${changed.joinToString()}")
    }

    private var bypassUpBeforeFix = false
    private var tgUpBeforeFix = false

    private fun next() {
        refreshFacts()
        autoCheck = null
        appliedNote = null
        scoreAfterFix = null
        waitingForUser = false
        index += 1
        val privateDnsStillOn = facts?.privateDnsActive == true
        while (privateDnsStillOn && index < queue.size && HelpPlanner.changesTunnelDns(queue[index])) index += 1
        if (HelpActions.dnsCannotHelp && queue.getOrNull(index) == HelpFix.TunnelDnsNext) index += 1
        if (baseline?.success == true && queue.getOrNull(index) == HelpFix.AdviceVpn) index += 1
        if (index >= queue.size) finishUnsolved() else stage = HelpStage.Fix
    }

    private fun finishUnsolved() {
        solved = false
        stage = HelpStage.Done
    }

    fun revertAll(context: Context): Boolean {
        if (isBusy) return false
        val app = context.applicationContext
        val changed = HelpRepairStore.restoreBefore(app)
        scope.launch { HelpActions.restartAffected(app, changed, strategyAlreadyApplied = false, allowStart = false) }
        Log.i(TAG, "Помощник: всё возвращено как было до ремонта (${changed.size} настроек)")
        fixSnapshot = null
        return true
    }

    fun keepChanges(context: Context) {
        HelpRepairStore.clear(context)
    }

    fun resumeVerify(chosen: HelpSymptom) {
        if (symptom == chosen && currentFix != null && (stage == HelpStage.Verify || stage == HelpStage.Fix)) {
            stage = HelpStage.Verify
        }
    }

    fun openReport() {
        if (isBusy) return
        stage = HelpStage.Report
        reportStep = HelpReportStep.Consent
        reportFile = null
    }

    fun onOpened(context: Context) {
        if (loggingOwned) return
        val app = context.applicationContext
        val ownership = app.getSharedPreferences(LOGGING_PREFS, Context.MODE_PRIVATE)
        loggingOwned = true
        loggingWasOff = ownership.getBoolean(KEY_HELP_TURNED_LOGGING_ON, false) ||
            !com.rupleide.netfix.core.debug.AppDebugManager.isLoggingEnabled()
        if (loggingWasOff) {
            ownership.edit().putBoolean(KEY_HELP_TURNED_LOGGING_ON, true).apply()
            if (!com.rupleide.netfix.core.debug.AppDebugManager.isLoggingEnabled()) {
                com.rupleide.netfix.core.debug.AppDebugManager.setEnabled(app, true)
            }
        }
        if (symptom == null) logMark = com.rupleide.netfix.core.debug.AppDebugManager.loggedTotal
        Log.i(TAG, "Помощник открыт, журнал пишется для отчёта")
    }

    fun onClosed(context: Context) {
        if (isBusy || symptom != null) return
        val app = context.applicationContext
        if (loggingOwned && loggingWasOff) {
            com.rupleide.netfix.core.debug.AppDebugManager.setEnabled(app, false)
            app.getSharedPreferences(LOGGING_PREFS, Context.MODE_PRIVATE).edit().remove(KEY_HELP_TURNED_LOGGING_ON).apply()
        }
        loggingOwned = false
        loggingWasOff = false
    }

    private const val LOGGING_PREFS = "netfix_help_logging"
    private const val KEY_HELP_TURNED_LOGGING_ON = "help_turned_logging_on"
    private var logMark = 0L

    fun buildReport(context: Context) {
        reportStep = HelpReportStep.Building
        val app = context.applicationContext
        job = scope.launch {
            symptom?.takeIf { it != HelpSymptom.Other }?.let { chosen ->
                Log.i(TAG, "Помощник: повторная проверка для отчёта")
                val fresh = try {
                    HelpProbes.collect(app, chosen, pickedPackage)
                } catch (e: Exception) {
                    Log.e(TAG, "Помощник: повторная проверка упала", e)
                    null
                }
                fresh?.let { recheck ->
                    Log.i(TAG, "Помощник: сейчас ${HelpPlanner.findings(chosen, recheck).joinToString(" | ") { it.text }}")
                    if (facts == null) {
                        facts = recheck
                        findings = HelpPlanner.findings(chosen, recheck)
                    }
                }
            }
            val file = HelpReport.write(app, helpSection(app))
            reportFile = file
            reportStep = HelpReportStep.Ready
        }
    }

    fun finishReport() {
        reset()
    }

    fun helpSection(context: Context): String = buildString {
        append("Версия: ${BuildConfig.VERSION_NAME}\n")
        append("Проблема: ${symptom?.title ?: "не выбрана"}\n")
        facts?.targetLabel?.let { append("Приложение: $it (${facts?.targetPackage})\n") }
        if (userNote.isNotBlank()) append("Словами пользователя: ${userNote.trim()}\n")
        if (findings.isNotEmpty()) {
            append("Проверки:\n")
            findings.forEach { append("  [${it.level.name}] ${it.text}\n") }
        }
        val journal = HelpRepairStore.journal(context)
        if (journal.isNotEmpty()) {
            append("Что пробовали:\n")
            journal.forEach { append("  $it\n") }
        } else if (attempts.isNotEmpty()) {
            append("Что пробовали:\n")
            attempts.forEach { append("  ${it.fix.titleFor(facts?.targetLabel)}: ${it.reportLine}\n") }
        } else {
            append("Исправления не пробовали\n")
        }
        append("Снимок «до ремонта»: ${HelpRepairStore.takenAt(context)?.let { timeFormat.format(Date(it)) } ?: "нет"}\n")
    }

    fun shareText(): String =
        "NetFix Mobile ${BuildConfig.VERSION_NAME}, подробный отчёт помощника: ${symptom?.title ?: "другая проблема"}"

    fun chatText(): String {
        val text = buildString {
            append("NetFix Mobile ${BuildConfig.VERSION_NAME}, помощник\n")
            append("Проблема: ${symptom?.title ?: "другая"}")
            facts?.targetLabel?.let { append(" ($it)") }
            append('\n')
            append("${if (facts?.isTv == true) "Приставка" else "Телефон"}: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}\n")
            val problems = findings.filter { it.level != HelpLevel.Ok }
            if (problems.isNotEmpty()) {
                append("Нашёл:\n")
                problems.forEach { append("• ${it.text}\n") }
            }
            val tried = attempts.filter { it.answer != HelpAnswer.Skipped || it.note != null }
            if (tried.isNotEmpty()) {
                append("Пробовали:\n")
                tried.forEach { append("• ${it.fix.titleFor(facts?.targetLabel)}: ${it.reportLine}\n") }
            }
        }
        return com.rupleide.netfix.core.debug.DiagnosticMasker.mask(text).take(MAX_CHAT_TEXT)
    }

    private val errorMarkers = listOf("Exception", "Error", "ERROR", "Ошибка", "Не удалось", "failed", "Failed", "code -")

    fun supportText(context: Context): String {
        val app = context.applicationContext
        val f = facts
        val text = buildString {
            userNote.trim().takeIf { it.isNotEmpty() }?.let { append(it).append("\n\n") }
            append("NetFix Mobile ${BuildConfig.VERSION_NAME}, помощник\n")
            append("Проблема: ${symptom?.title ?: "другая"}")
            f?.targetLabel?.let { append(" ($it)") }
            append('\n')
            append("${if (facts?.isTv == true) "Приставка" else "Телефон"}: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}\n")
            if (f != null) {
                val network = when {
                    f.onEthernet -> "кабель"
                    f.networkKind == com.rupleide.netfix.core.dpibypass.NetworkKind.Mobile -> "мобильная"
                    else -> "Wi-Fi"
                }
                append("Сеть: $network, работа сети как в ${f.behaviorVersion}, DNS ${HelpPlanner.dnsLabel(f.tunnelDns)}")
                f.privateDnsHost?.let { append(", частный DNS $it") }
                append('\n')
            }
            append("Способ: «${com.rupleide.netfix.core.dpibypass.StrategyTestManager.getActiveStrategyName(app)}»\n")
            append("NetFix: ${if (com.rupleide.netfix.core.dpibypass.ServiceManager.isConnected) "включён" else "выключен"}, прокси Telegram: ${if (com.rupleide.netfix.core.tgproxy.TgProxyController.isRunning) "включён" else "выключен"}\n")
            val problems = findings.filter { it.level != HelpLevel.Ok }
            if (problems.isNotEmpty()) {
                append("\nНашёл:\n")
                problems.forEach { append("• ${it.text}\n") }
            }
            val tried = attempts.filter { it.answer != HelpAnswer.Skipped || it.note != null }
            if (tried.isNotEmpty()) {
                append("\nПробовали:\n")
                tried.forEach { append("• ${it.fix.titleFor(f?.targetLabel)}: ${it.reportLine}\n") }
            }
        }
        val masked = com.rupleide.netfix.core.debug.DiagnosticMasker.mask(text)
        val journalBudget = MAX_SUPPORT_TEXT - masked.length - JOURNAL_HEADER.length
        val journal = journalLines(journalBudget)
        val full = if (journal.isEmpty()) masked else masked + JOURNAL_HEADER + journal.joinToString("\n")
        return full.take(MAX_SUPPORT_TEXT)
    }

    private fun journalLines(budget: Int): List<String> {
        if (budget <= 0) return emptyList()
        val lines = com.rupleide.netfix.core.debug.AppDebugManager.logsSince(logMark)
            .map { com.rupleide.netfix.core.debug.DiagnosticMasker.mask(it.take(MAX_LOG_LINE_LENGTH)) }
        val important = lines.indices.filter { i -> lines[i].contains("$TAG:") || errorMarkers.any { lines[i].contains(it) } }
        val chosen = sortedSetOf<Int>()
        var left = budget
        for (i in important.asReversed()) {
            val cost = lines[i].length + 1
            if (cost > left) break
            chosen.add(i)
            left -= cost
        }
        for (i in lines.indices.reversed()) {
            if (i in chosen) continue
            val cost = lines[i].length + 1
            if (cost > left) break
            chosen.add(i)
            left -= cost
        }
        return chosen.map { lines[it] }
    }

    private const val JOURNAL_HEADER = "\nЖурнал:\n"
    private const val MAX_LOG_LINE_LENGTH = 220
    private const val MAX_SUPPORT_TEXT = 4000

    fun caption(): String {
        val own = userNote.trim()
        val auto = chatText()
        val combined = if (own.isNotEmpty()) "$own\n\n$auto" else auto
        return if (combined.length <= MAX_CAPTION) combined else combined.take(MAX_CAPTION - 1) + "…"
    }
}
