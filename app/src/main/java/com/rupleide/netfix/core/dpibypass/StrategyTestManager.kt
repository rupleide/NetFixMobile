package com.rupleide.netfix.core.dpibypass

import android.content.Context
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.io.File

object StrategyTestManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isTesting by mutableStateOf(false)
        private set

    @Volatile
    private var finishEarlyRequested = false

    var canFinishEarly by mutableStateOf(false)
        private set

    var finishRequested by mutableStateOf(false)
        private set

    var scanFinalTotal by mutableStateOf(0)
        private set

    var scanFinalDone by mutableStateOf(0)
        private set

    fun finishScanEarly() {
        if (isTesting && currentTestIndex >= MIN_CHECKED_BEFORE_EARLY_FINISH) {
            finishEarlyRequested = true
            finishRequested = true
            canFinishEarly = false
            currentProgress = "Завершаем досрочно…"
        }
    }

    private const val MIN_CHECKED_BEFORE_EARLY_FINISH = 5

    var currentProgress by mutableStateOf("")
        private set

    var hasConnectionError by mutableStateOf(false)

    var currentTestIndex by mutableStateOf(0)
        private set

    val testResults = mutableStateListOf<Triple<Int, String, String>>()

    var bestStrategyResult by mutableStateOf<String?>(null)
        private set

    var appliedStrategy by mutableStateOf<String?>(null)

    val totalStrategiesCount: Int
        get() = StrategyTester.defaultStrategies.size + activeCustomStrategies().size

    var checkingCustomStrategy by mutableStateOf<String?>(null)
        private set

    const val CHECKING_STATUS = "проверка…"

    private fun activeCustomStrategies(): List<String> =
        customStrategies.filter { !deletedStrategies.containsKey(it) }

    private fun isVisibleResult(result: Triple<Int, String, String>): Boolean =
        !deletedStrategies.containsKey(result.second) &&
            (!result.third.contains("тайм-аут") || result.second in customStrategies)

    val pinnedStrategies = mutableStateMapOf<String, Boolean>()
    val customNames = mutableStateMapOf<String, String>()
    val strategyNotes = mutableStateMapOf<String, String>()
    val deletedStrategies = mutableStateMapOf<String, Boolean>()
    private val customStrategies = mutableListOf<String>()

    const val CUSTOM_STRATEGY_STATUS = "Своя"

    private fun loadCustomizations(context: Context) {
        val file = File(context.filesDir, "strategy_customizations.json")
        if (!file.exists()) return
        try {
            val json = org.json.JSONObject(file.readText())
            val pinnedArr = json.optJSONArray("pinned")
            if (pinnedArr != null) {
                pinnedStrategies.clear()
                for (i in 0 until pinnedArr.length()) {
                    pinnedStrategies[pinnedArr.getString(i)] = true
                }
            }
            val deletedArr = json.optJSONArray("deleted")
            if (deletedArr != null) {
                deletedStrategies.clear()
                for (i in 0 until deletedArr.length()) {
                    deletedStrategies[deletedArr.getString(i)] = true
                }
            }
            val namesObj = json.optJSONObject("names")
            if (namesObj != null) {
                customNames.clear()
                val keys = namesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    customNames[key] = namesObj.getString(key)
                }
            }
            val customArr = json.optJSONArray("custom")
            if (customArr != null) {
                customStrategies.clear()
                for (i in 0 until customArr.length()) {
                    customStrategies.add(customArr.getString(i))
                }
            }
            val notesObj = json.optJSONObject("notes")
            if (notesObj != null) {
                strategyNotes.clear()
                val keys = notesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    strategyNotes[key] = notesObj.getString(key)
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveCustomizations(context: Context) {
        try {
            val file = File(context.filesDir, "strategy_customizations.json")
            val json = org.json.JSONObject()
            
            val pinnedArr = org.json.JSONArray()
            pinnedStrategies.forEach { (strategy, pinned) ->
                if (pinned) pinnedArr.put(strategy)
            }
            json.put("pinned", pinnedArr)

            val deletedArr = org.json.JSONArray()
            deletedStrategies.forEach { (strategy, deleted) ->
                if (deleted) deletedArr.put(strategy)
            }
            json.put("deleted", deletedArr)

            val namesObj = org.json.JSONObject()
            customNames.forEach { (strategy, name) ->
                namesObj.put(strategy, name)
            }
            json.put("names", namesObj)

            val notesObj = org.json.JSONObject()
            strategyNotes.forEach { (strategy, notes) ->
                notesObj.put(strategy, notes)
            }
            json.put("notes", notesObj)

            json.put("custom", org.json.JSONArray(customStrategies))

            file.writeText(json.toString())
        } catch (_: Exception) {}
    }

    enum class VerifyScope { Wifi, Mobile, Both }

    data class RankedConfig(
        val strategy: String,
        val youtubeOk: Int,
        val youtubeTotal: Int,
        val discordOk: Int,
        val discordTotal: Int,
        val averageMs: Long
    ) {
        val totalOk: Int get() = youtubeOk + discordOk
        val total: Int get() = youtubeTotal + discordTotal
        val isWorking: Boolean get() = total > 0 && totalOk * 4 >= total * 3
    }

    val verifyResults = mutableStateMapOf<NetworkKind, List<RankedConfig>>()

    var isVerifying by mutableStateOf(false)
        private set

    var selectionVersion by mutableStateOf(0)
        private set

    var verifyMessage by mutableStateOf("")
        private set

    var verifyIsError by mutableStateOf(false)
        private set

    var verifyNeeds by mutableStateOf<NetworkKind?>(null)
        private set

    var verifyDone by mutableStateOf(0)
        private set

    var verifyTotal by mutableStateOf(0)
        private set

    var verifyKinds by mutableStateOf<List<NetworkKind>>(emptyList())
        private set

    var verifyStopping by mutableStateOf(false)
        private set

    var verifyCancelled by mutableStateOf(false)
        private set

    var verifyFinishedAt by mutableStateOf(0L)
        private set

    private var verifyTester: StrategyTester? = null

    var verifySingle by mutableStateOf<String?>(null)
        private set

    var universalStrategy by mutableStateOf<String?>(null)
        private set

    var lastScanNetwork by mutableStateOf<NetworkKind?>(null)
        private set

    private var verifyJob: Job? = null

    private const val LAST_SCAN_NETWORK_KEY = "last_scan_network_kind"
    private const val VERIFY_CANDIDATES = 10
    private const val NETWORK_QUICK_TIMEOUT_MS = 6_000L
    private const val NETWORK_USER_WAIT_MS = 90_000L
    private const val VERIFY_FILE = "verify_results.json"
    private const val UNIVERSAL_KEY = "universal_strategy"

    private fun loadLastScanNetwork(context: Context) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val suffix = prefs.getString(LAST_SCAN_NETWORK_KEY, null)
        lastScanNetwork = NetworkKind.values().firstOrNull { it.preferenceSuffix == suffix }
        universalStrategy = prefs.getString(UNIVERSAL_KEY, null)
    }

    private fun rememberLastScanNetwork(context: Context, kind: NetworkKind?) {
        lastScanNetwork = kind
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString(LAST_SCAN_NETWORK_KEY, kind?.preferenceSuffix).apply()
    }

    fun hasScanResults(): Boolean =
        testResults.any { StrategyTester.isSuccessfulStatus(it.third) } ||
            pinnedStrategies.keys.any { !deletedStrategies.containsKey(it) }

    fun selectedStrategyFor(context: Context, kind: NetworkKind): String? {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.strategyForNetwork(kind)?.let { return it }
        if (context.currentNetworkKind() != kind) return null
        return appliedStrategy
            ?: prefs.getString("byedpi_cmd_args", null)
                ?.takeIf { it.isNotBlank() && prefs.getBoolean("byedpi_enable_cmd_settings", false) }
    }

    fun openConnectivitySettings(context: Context) {
        val primary = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.content.Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
        }
        primary.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(primary)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun saveVerifyResults(context: Context) {
        try {
            val root = org.json.JSONObject()
            verifyResults.forEach { (kind, list) ->
                val array = org.json.JSONArray()
                list.forEach { item ->
                    array.put(
                        org.json.JSONObject()
                            .put("strategy", item.strategy)
                            .put("yt", item.youtubeOk).put("ytTotal", item.youtubeTotal)
                            .put("dc", item.discordOk).put("dcTotal", item.discordTotal)
                            .put("ms", item.averageMs)
                    )
                }
                root.put(kind.preferenceSuffix, array)
            }
            File(context.filesDir, VERIFY_FILE).writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    private fun loadVerifyResults(context: Context) {
        val file = File(context.filesDir, VERIFY_FILE)
        if (!file.exists()) return
        try {
            val root = org.json.JSONObject(file.readText())
            NetworkKind.values().forEach { kind ->
                val array = root.optJSONArray(kind.preferenceSuffix) ?: return@forEach
                val list = (0 until array.length()).map { index ->
                    val obj = array.getJSONObject(index)
                    RankedConfig(
                        strategy = obj.getString("strategy"),
                        youtubeOk = obj.getInt("yt"),
                        youtubeTotal = obj.getInt("ytTotal"),
                        discordOk = obj.getInt("dc"),
                        discordTotal = obj.getInt("dcTotal"),
                        averageMs = obj.optLong("ms", -1L)
                    )
                }
                verifyResults[kind] = list
            }
        } catch (_: Exception) {
        }
    }

    private fun verifyCandidates(context: Context): List<String> {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val ordered = LinkedHashSet<String>()
        testResults
            .filter { StrategyTester.isSuccessfulStatus(it.third) && !deletedStrategies.containsKey(it.second) }
            .sortedByDescending { StrategyTester.successScore(it.third) ?: 0.0 }
            .take(VERIFY_CANDIDATES)
            .forEach { ordered.add(it.second) }
        pinnedStrategies.keys
            .filter { !deletedStrategies.containsKey(it) }
            .forEach { ordered.add(it) }
        NetworkKind.values().forEach { kind -> prefs.strategyForNetwork(kind)?.let { ordered.add(it) } }
        return ordered.toList()
    }

    private class HeldNetwork(
        val network: android.net.Network,
        val callback: android.net.ConnectivityManager.NetworkCallback
    )

    private suspend fun requestNetworkOnce(
        connectivityManager: android.net.ConnectivityManager,
        kind: NetworkKind,
        timeoutMs: Long
    ): HeldNetwork? {
        val transport = when (kind) {
            NetworkKind.Wifi -> android.net.NetworkCapabilities.TRANSPORT_WIFI
            NetworkKind.Mobile -> android.net.NetworkCapabilities.TRANSPORT_CELLULAR
        }
        val request = android.net.NetworkRequest.Builder()
            .addTransportType(transport)
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        var registeredCallback: android.net.ConnectivityManager.NetworkCallback? = null
        fun unregister() {
            registeredCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                } catch (_: Exception) {
                }
            }
        }
        val held = try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<HeldNetwork?> { continuation ->
                    val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: android.net.Network) {
                            if (continuation.isActive) continuation.resume(HeldNetwork(network, this))
                        }
                    }
                    registeredCallback = callback
                    try {
                        connectivityManager.requestNetwork(request, callback)
                    } catch (e: Exception) {
                        Log.w("StrategyTestManager", "Не удалось запросить сеть ${kind.displayName}", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            unregister()
            throw e
        }
        if (held == null) unregister()
        return held
    }

    private suspend fun acquireNetwork(
        connectivityManager: android.net.ConnectivityManager,
        kind: NetworkKind
    ): HeldNetwork? {
        requestNetworkOnce(connectivityManager, kind, NETWORK_QUICK_TIMEOUT_MS)?.let { return it }
        verifyNeeds = kind
        try {
            return requestNetworkOnce(connectivityManager, kind, NETWORK_USER_WAIT_MS)
        } finally {
            verifyNeeds = null
        }
    }

    private fun applyStrategyToActive(context: Context, strategy: String) {
        val normalized = StrategyTester.normalizeStrategy(strategy)
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE).edit()
            .putString(ACTIVE_STRATEGY_KEY, normalized)
            .putBoolean("byedpi_enable_cmd_settings", true)
            .apply()
        appliedStrategy = normalized
        bestStrategyResult = normalized
    }

    private fun setUniversal(context: Context, strategy: String?) {
        universalStrategy = strategy
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE).edit()
            .putString(UNIVERSAL_KEY, strategy)
            .apply()
    }

    private fun applyUniversalIfPossible(context: Context): Boolean {
        val wifi = verifyResults[NetworkKind.Wifi].orEmpty()
        val mobile = verifyResults[NetworkKind.Mobile].orEmpty()
        if (wifi.isEmpty() || mobile.isEmpty()) {
            setUniversal(context, null)
            return false
        }
        val wifiBest = wifi.maxOf { it.totalOk }
        val mobileBest = mobile.maxOf { it.totalOk }
        val mobileByKey = mobile.associateBy { StrategyTester.normalizeStrategy(it.strategy) }
        val universal = wifi
            .mapNotNull { w -> mobileByKey[StrategyTester.normalizeStrategy(w.strategy)]?.let { w to it } }
            .filter { (w, m) -> minOf(w.totalOk, m.totalOk) > 0 && w.totalOk >= wifiBest - 1 && m.totalOk >= mobileBest - 1 }
            .maxWithOrNull(
                compareBy<Pair<RankedConfig, RankedConfig>> { (w, m) -> minOf(w.totalOk, m.totalOk) }
                    .thenBy { (w, m) -> w.totalOk + m.totalOk }
                    .thenBy { (w, m) -> -(maxOf(w.averageMs, m.averageMs)) }
            )
        if (universal == null) {
            setUniversal(context, null)
            return false
        }
        val strategy = StrategyTester.normalizeStrategy(universal.first.strategy)
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        NetworkKind.values().forEach { prefs.rememberStrategyForNetwork(it, strategy) }
        if (context.currentNetworkKind() != null) applyStrategyToActive(context, strategy)
        setUniversal(context, strategy)
        return true
    }

    fun chooseConfigFor(context: Context, kind: NetworkKind, strategy: String) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(appContext.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.rememberStrategyForNetwork(kind, StrategyTester.normalizeStrategy(strategy))
        if (universalStrategy != null && universalStrategy != StrategyTester.normalizeStrategy(strategy)) {
            setUniversal(appContext, null)
        }
        if (appContext.currentNetworkKind() == kind) {
            applyStrategyToActive(appContext, strategy)
            if (com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running) {
                ServiceManager.restart(appContext, com.rupleide.netfix.data.Mode.VPN)
            }
        }
        selectionVersion += 1
        android.widget.Toast.makeText(
            appContext,
            "${kind.displayName}: выбран «${getStrategyName(strategy, appContext)}»",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun resultFor(kind: NetworkKind, strategy: String?): RankedConfig? {
        if (strategy == null) return null
        val key = StrategyTester.normalizeStrategy(strategy)
        return verifyResults[kind]?.firstOrNull { StrategyTester.normalizeStrategy(it.strategy) == key }
    }

    fun configChoices(kind: NetworkKind): List<String> {
        val ordered = LinkedHashMap<String, String>()
        customStrategies
            .filter { !deletedStrategies.containsKey(it) }
            .forEach { ordered.putIfAbsent(StrategyTester.normalizeStrategy(it), it) }
        verifyResults[kind].orEmpty()
            .filter { !deletedStrategies.containsKey(it.strategy) }
            .forEach { ordered.putIfAbsent(StrategyTester.normalizeStrategy(it.strategy), it.strategy) }
        testResults
            .filter { StrategyTester.isSuccessfulStatus(it.third) && !deletedStrategies.containsKey(it.second) }
            .sortedByDescending { StrategyTester.successScore(it.third) ?: 0.0 }
            .forEach { ordered.putIfAbsent(StrategyTester.normalizeStrategy(it.second), it.second) }
        pinnedStrategies.keys
            .filter { !deletedStrategies.containsKey(it) }
            .forEach { ordered.putIfAbsent(StrategyTester.normalizeStrategy(it), it) }
        return ordered.values.take(80)
    }

    fun scanStatusFor(strategy: String): String? {
        val key = StrategyTester.normalizeStrategy(strategy)
        return testResults.firstOrNull { StrategyTester.normalizeStrategy(it.second) == key }?.third?.replace(" (Успешно)", "")
    }

    fun nameForNetwork(context: Context, kind: NetworkKind?): String {
        val version = selectionVersion
        val applied = appliedStrategy
        val saved = if (kind != null && version >= 0) selectedStrategyFor(context, kind) else null
        val chosen = saved ?: applied ?: return "По умолчанию"
        return getStrategyName(chosen, context)
    }

    fun stopVerification() {
        if (!isVerifying || verifyStopping) return
        verifyStopping = true
        verifyMessage = "Останавливаем…"
        verifyJob?.cancel()
        val tester = verifyTester
        this.scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            tester?.abortActiveChecks()
        }
    }

    fun startVerification(context: Context, scope: VerifyScope, only: String? = null): Job? {
        if (isVerifying || isTesting) return null
        val appContext = context.applicationContext
        val candidates = only?.let { listOf(it) } ?: verifyCandidates(appContext)
        if (candidates.isEmpty()) {
            verifyIsError = true
            verifyMessage = "Сначала просканируйте сеть"
            return null
        }
        val kinds = when (scope) {
            VerifyScope.Wifi -> listOf(NetworkKind.Wifi)
            VerifyScope.Mobile -> listOf(NetworkKind.Mobile)
            VerifyScope.Both -> listOf(NetworkKind.Wifi, NetworkKind.Mobile)
        }

        verifyKinds = kinds
        verifySingle = only
        verifyStopping = false
        verifyCancelled = false
        isVerifying = true
        verifyIsError = false
        verifyDone = 0
        verifyTotal = candidates.size * kinds.size
        verifyMessage = "Подготовка…"

        val job = this.scope.launch {
            val tester = StrategyTester(appContext)
            verifyTester = tester
            val wasRunning = tester.stopRunningVpnForTests()
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val failures = ArrayList<String>()
            try {
                for (kind in kinds) {
                    verifyMessage = "${kind.displayName}: подключаемся…"
                    val held = acquireNetwork(connectivityManager, kind)
                    if (held == null) {
                        failures += when (kind) {
                            NetworkKind.Mobile -> "Мобильная сеть недоступна"
                            NetworkKind.Wifi -> "Wi-Fi недоступен"
                        }
                        verifyDone += candidates.size
                        continue
                    }
                    val ranked = ArrayList<RankedConfig>()
                    try {
                        connectivityManager.bindProcessToNetwork(held.network)
                        candidates.forEachIndexed { index, strategy ->
                            verifyMessage = "${kind.displayName}: проверено $index из ${candidates.size}"
                            val probe = tester.probeStrategy(strategy)
                            ranked += RankedConfig(strategy, probe.youtubeOk, probe.youtubeTotal, probe.discordOk, probe.discordTotal, probe.averageMs)
                            verifyDone += 1
                        }
                    } finally {
                        connectivityManager.bindProcessToNetwork(null)
                        try {
                            connectivityManager.unregisterNetworkCallback(held.callback)
                        } catch (_: Exception) {
                        }
                    }
                    val merged = if (only != null) {
                        verifyResults[kind].orEmpty().filter { old ->
                            ranked.none { StrategyTester.normalizeStrategy(it.strategy) == StrategyTester.normalizeStrategy(old.strategy) }
                        } + ranked
                    } else {
                        ranked
                    }
                    val sorted = merged.sortedWith(
                        compareByDescending<RankedConfig> { it.totalOk }
                            .thenBy { if (it.averageMs < 0) Long.MAX_VALUE else it.averageMs }
                    )
                    verifyResults[kind] = sorted
                    val best = sorted.firstOrNull()
                    if (only == null && best != null && best.totalOk > 0) {
                        val prefs = appContext.getSharedPreferences(appContext.packageName + "_preferences", Context.MODE_PRIVATE)
                        prefs.rememberStrategyForNetwork(kind, StrategyTester.normalizeStrategy(best.strategy))
                        if (appContext.currentNetworkKind() == kind) applyStrategyToActive(appContext, best.strategy)
                    }
                    selectionVersion += 1
                    saveVerifyResults(appContext)
                }
                if (scope == VerifyScope.Both && only == null) applyUniversalIfPossible(appContext)
                selectionVersion += 1
                verifyIsError = failures.isNotEmpty()
                verifyMessage = failures.joinToString("\n")
                verifyFinishedAt = System.currentTimeMillis()
            } catch (e: kotlinx.coroutines.CancellationException) {
                verifyCancelled = true
                verifyIsError = false
                verifyMessage = ""
                throw e
            } finally {
                isVerifying = false
                verifyStopping = false
                verifyTester = null
                verifyNeeds = null
                if (wasRunning) {
                    ServiceManager.start(appContext, com.rupleide.netfix.data.Mode.VPN)
                }
            }
        }
        verifyJob = job
        return job
    }

    private var activeStrategyRecorder: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    private fun startRecordingStrategiesPerNetwork(context: Context) {
        if (activeStrategyRecorder != null) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(appContext.packageName + "_preferences", Context.MODE_PRIVATE)
        val recorder = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key != ACTIVE_STRATEGY_KEY) return@OnSharedPreferenceChangeListener
            val activeStrategy = sharedPreferences.getString(ACTIVE_STRATEGY_KEY, null) ?: return@OnSharedPreferenceChangeListener
            val kind = appContext.currentNetworkKind() ?: return@OnSharedPreferenceChangeListener
            sharedPreferences.rememberStrategyForNetwork(kind, activeStrategy)
        }
        prefs.registerOnSharedPreferenceChangeListener(recorder)
        activeStrategyRecorder = recorder
        val activeStrategy = prefs.getString(ACTIVE_STRATEGY_KEY, null)
        val kind = appContext.currentNetworkKind()
        if (activeStrategy != null && kind != null && prefs.strategyForNetwork(kind) == null) {
            prefs.rememberStrategyForNetwork(kind, activeStrategy)
        }
    }

    fun init(context: Context) {
        startRecordingStrategiesPerNetwork(context)
        loadLastScanNetwork(context)
        loadVerifyResults(context)
        loadCustomizations(context)
        val file = File(context.filesDir, "proxy_test_results.txt")
        if (file.exists()) {
            try {
                val lines = file.readLines()
                val loaded = mutableListOf<Triple<Int, String, String>>()
                for (i in lines.indices step 3) {
                    if (i + 2 < lines.size) {
                        val index = lines[i].toIntOrNull() ?: continue
                        val strategy = lines[i + 1]
                        val status = lines[i + 2].replace(" (Успешно)", "")
                        loaded.add(Triple(index, strategy, status))
                    }
                }

                val prefs = context.getSharedPreferences(
                    context.packageName + "_preferences",
                    Context.MODE_PRIVATE
                )
                val activeCmd = prefs.getString("byedpi_cmd_args", null)
                if (activeCmd != null && prefs.getBoolean("byedpi_enable_cmd_settings", false)) {
                    bestStrategyResult = activeCmd
                    appliedStrategy = activeCmd
                }

                appendMissingCustomStrategies(loaded)

                if (loaded.isNotEmpty()) {
                    testResults.clear()
                    val activeApplied = appliedStrategy
                    val filtered = loaded.filter { isVisibleResult(it) }
                    val sortedLoaded = filtered.sortedWith(compareBy<Triple<Int, String, String>> {
                        if (pinnedStrategies.containsKey(it.second)) 0 else 1
                    }.thenBy {
                        if (activeApplied != null && StrategyTester.normalizeStrategy(it.second) == activeApplied) 0 else 1
                    })
                    testResults.addAll(sortedLoaded)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun checkInternetConnection(): Boolean {
        return try {
            val url = java.net.URL("https://ya.ru")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..399
        } catch (_: Exception) {
            try {
                val url = java.net.URL("https://connectivitycheck.gstatic.com/generate_204")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 204
            } catch (_: Exception) {
                false
            }
        }
    }

    fun startTesting(context: Context): Job? {
        if (isTesting) return null

        return scope.launch {
            try {
                Log.i("StrategyTestManager", "Запуск автоподбора стратегий")
                finishEarlyRequested = false
                finishRequested = false
                scanFinalTotal = 0
                scanFinalDone = 0
                isTesting = true
                hasConnectionError = false
                bestStrategyResult = null
                testResults.clear()
                currentTestIndex = 0
                currentProgress = "Готовимся к тестированию..."

                if (!checkInternetConnection()) {
                    Log.e("StrategyTestManager", "Ошибка автоподбора: отсутствует интернет-соединение")
                    hasConnectionError = true
                    currentProgress = "Ошибка: отсутствует интернет-соединение."
                    isTesting = false
                    return@launch
                }

                val tester = StrategyTester(context)
                val wasRunning = com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running
                val best = tester.runTests(
                    StrategyTester.defaultStrategies + activeCustomStrategies(),
                    { finishEarlyRequested },
                    onFinalPhase = { count ->
                        scanFinalTotal = count
                        scanFinalDone = 0
                        canFinishEarly = !finishEarlyRequested
                        currentProgress = "Уточняем лучшие стратегии…"
                    }
                ) { index, strategy, status, isFinalCheck ->
                    canFinishEarly = currentTestIndex >= MIN_CHECKED_BEFORE_EARLY_FINISH && !finishEarlyRequested
                    if (isFinalCheck) {
                        currentProgress = "Уточняем лучшие стратегии…"
                        scanFinalDone += 1
                        replaceResultStatus(strategy, status)
                    } else {
                        currentTestIndex = index + 1
                        currentProgress = "Проверяем стратегию ${index + 1} из $totalStrategiesCount"
                        if (!deletedStrategies.containsKey(strategy)) {
                            testResults.add(0, Triple(index + 1, strategy, status))
                        }
                    }
                }

                Log.i("StrategyTestManager", "Автоподбор завершен. Лучшая стратегия: \"$best\"")

                if (best != null) {
                    val formattedBest = StrategyTester.normalizeStrategy(best)
                    bestStrategyResult = formattedBest
                    appliedStrategy = formattedBest
                    val prefs = context.getSharedPreferences(
                        context.packageName + "_preferences",
                        Context.MODE_PRIVATE
                    )
                    prefs.edit()
                        .putString("byedpi_cmd_args", formattedBest)
                        .putBoolean("byedpi_enable_cmd_settings", true)
                        .apply()
                }

                val activeApplied = appliedStrategy
                val filtered = testResults.filter { isVisibleResult(it) }
                val sorted = filtered.sortedWith(compareBy<Triple<Int, String, String>> {
                    if (pinnedStrategies.containsKey(it.second)) 0 else 1
                }.thenBy {
                    if (activeApplied != null && StrategyTester.normalizeStrategy(it.second) == activeApplied) 0 else 1
                }.thenByDescending {
                    StrategyTester.successScore(it.third) ?: -1.0
                })
                testResults.clear()
                testResults.addAll(sorted)
                appendMissingCustomStrategies(testResults)

                saveResults(context)
                rememberLastScanNetwork(context, context.currentNetworkKind())
                showNotification(context)

                if (wasRunning) {
                    com.rupleide.netfix.core.dpibypass.ServiceManager.start(
                        context,
                        com.rupleide.netfix.data.Mode.VPN
                    )
                }
            } finally {
                isTesting = false
                canFinishEarly = false
                finishEarlyRequested = false
                finishRequested = false
                scanFinalTotal = 0
                scanFinalDone = 0
                currentProgress = "Тестирование завершено"
                Log.i("StrategyTestManager", "Тестирование полностью завершено")
            }
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "NetFixNotifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel(
                channelId,
                "Уведомления NetFix",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.rupleide.netfix.R.drawable.ic_notification)
            .setContentTitle("NetFix Mobile")
            .setContentText("Сканирование стратегий успешно завершено")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(1001, builder.build())
    }

    private fun saveResults(context: Context) {
        try {
            val file = File(context.filesDir, "proxy_test_results.txt")
            val sb = StringBuilder()
            testResults.forEach {
                sb.append(it.first).append("\n")
                sb.append(it.second).append("\n")
                sb.append(it.third).append("\n")
            }
            file.writeText(sb.toString())
        } catch (_: Exception) {
        }
    }

    fun applyStrategy(context: Context, originalIndex: Int, strategy: String) {
        Log.i("StrategyTestManager", "Вручную применена стратегия №$originalIndex: \"$strategy\"")
        val prefs = context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
        prefs.edit()
            .putString("byedpi_cmd_args", strategy)
            .putBoolean("byedpi_enable_cmd_settings", true)
            .apply()

        appliedStrategy = strategy

        resortResults(context)

        android.widget.Toast.makeText(
            context,
            "Применена стратегия: $originalIndex",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        if (com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running) {
            com.rupleide.netfix.core.dpibypass.ServiceManager.restart(
                context,
                com.rupleide.netfix.data.Mode.VPN
            )
        }
    }

    fun togglePin(context: Context, strategy: String) {
        val current = pinnedStrategies[strategy] ?: false
        if (current) {
            pinnedStrategies.remove(strategy)
        } else {
            pinnedStrategies[strategy] = true
        }
        saveCustomizations(context)
        resortResults(context)
    }

    fun renameStrategy(context: Context, strategy: String, newName: String?) {
        if (newName.isNullOrBlank()) {
            customNames.remove(strategy)
        } else {
            customNames[strategy] = newName
        }
        saveCustomizations(context)
    }

    fun updateNotes(context: Context, strategy: String, notes: String?) {
        if (notes.isNullOrBlank()) {
            strategyNotes.remove(strategy)
        } else {
            strategyNotes[strategy] = notes
        }
        saveCustomizations(context)
    }

    sealed interface AddCustomStrategyResult {
        data class Added(val command: String) : AddCustomStrategyResult
        data object Invalid : AddCustomStrategyResult
        data class AlreadyExists(val existingName: String) : AddCustomStrategyResult
    }

    fun addCustomStrategy(context: Context, command: String, name: String?, notes: String?): AddCustomStrategyResult {
        val trimmedCommand = StrategyTester.collapseFakeSniDomains(sanitizePastedByeDpiCommand(command))
        if (!isValidStrategyCommand(trimmedCommand)) return AddCustomStrategyResult.Invalid

        val normalizedNewCommand = StrategyTester.normalizeStrategy(trimmedCommand)
        val existingStrategy = testResults.firstOrNull {
            StrategyTester.normalizeStrategy(StrategyTester.collapseFakeSniDomains(it.second)) == normalizedNewCommand
        }
        if (existingStrategy != null) {
            return AddCustomStrategyResult.AlreadyExists(getStrategyName(existingStrategy.second))
        }

        deletedStrategies.remove(trimmedCommand)
        if (trimmedCommand !in customStrategies) {
            customStrategies.add(trimmedCommand)
        }
        renameStrategy(context, trimmedCommand, name)
        updateNotes(context, trimmedCommand, notes)
        if (testResults.none { it.second == trimmedCommand }) {
            testResults.add(0, Triple(0, trimmedCommand, CUSTOM_STRATEGY_STATUS))
        }
        saveCustomizations(context)
        saveResults(context)
        return AddCustomStrategyResult.Added(trimmedCommand)
    }

    private fun replaceResultStatus(strategy: String, status: String) {
        val index = testResults.indexOfFirst { it.second == strategy }
        if (index >= 0) {
            val existing = testResults[index]
            testResults[index] = Triple(existing.first, existing.second, status)
        }
    }

    fun checkCustomStrategy(context: Context, strategy: String): Job? {
        if (isTesting || checkingCustomStrategy != null) return null
        return scope.launch {
            checkingCustomStrategy = strategy
            replaceResultStatus(strategy, CHECKING_STATUS)
            val wasRunning = com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running
            try {
                StrategyTester(context).runTests(listOf(strategy)) { _, _, status, _ ->
                    replaceResultStatus(strategy, status)
                }
                saveResults(context)
            } finally {
                if (testResults.any { it.second == strategy && it.third == CHECKING_STATUS }) {
                    replaceResultStatus(strategy, CUSTOM_STRATEGY_STATUS)
                }
                checkingCustomStrategy = null
                if (wasRunning) {
                    ServiceManager.start(context, com.rupleide.netfix.data.Mode.VPN)
                }
            }
        }
    }

    fun isCustomStrategy(strategy: String): Boolean = strategy in customStrategies

    fun isValidStrategyCommand(command: String): Boolean {
        val args = shellSplit(command.trim())
        return args.isNotEmpty() && args.first().startsWith("-")
    }

    private fun appendMissingCustomStrategies(results: MutableList<Triple<Int, String, String>>) {
        customStrategies
            .filter { custom -> !deletedStrategies.containsKey(custom) && results.none { it.second == custom } }
            .forEach { custom -> results.add(Triple(0, custom, CUSTOM_STRATEGY_STATUS)) }
    }

    fun deleteStrategy(context: Context, strategy: String) {
        val key = StrategyTester.normalizeStrategy(strategy)
        val variants = LinkedHashSet<String>()
        variants.add(strategy)
        variants.add(key)
        testResults.forEach { if (StrategyTester.normalizeStrategy(it.second) == key) variants.add(it.second) }
        verifyResults.values.forEach { list ->
            list.forEach { if (StrategyTester.normalizeStrategy(it.strategy) == key) variants.add(it.strategy) }
        }
        customStrategies.toList().forEach { if (StrategyTester.normalizeStrategy(it) == key) variants.add(it) }
        pinnedStrategies.keys.toList().forEach { if (StrategyTester.normalizeStrategy(it) == key) variants.add(it) }
        variants.forEach { variant ->
            customStrategies.remove(variant)
            pinnedStrategies.remove(variant)
            deletedStrategies[variant] = true
        }
        saveCustomizations(context)
        testResults.removeAll { StrategyTester.normalizeStrategy(it.second) == key }
        saveResults(context)
        verifyResults.keys.toList().forEach { kind ->
            verifyResults[kind] = verifyResults[kind].orEmpty().filter { StrategyTester.normalizeStrategy(it.strategy) != key }
        }
        saveVerifyResults(context)
        selectionVersion += 1
    }

    private fun resortResults(context: Context) {
        val activeApplied = appliedStrategy
        val current = testResults.filter { isVisibleResult(it) }
        val sorted = current.sortedWith(compareBy<Triple<Int, String, String>> {
            if (pinnedStrategies.containsKey(it.second)) 0 else 1
        }.thenBy {
            if (activeApplied != null && StrategyTester.normalizeStrategy(it.second) == activeApplied) 0 else 1
        }.thenByDescending {
            StrategyTester.successScore(it.third) ?: -1.0
        })
        testResults.clear()
        testResults.addAll(sorted)
        saveResults(context)
    }

    fun recommendedStrategies(): List<String> =
        testResults
            .filter { StrategyTester.isSuccessfulStatus(it.third) }
            .sortedByDescending { StrategyTester.successScore(it.third) ?: 0.0 }
            .map { it.second }

    fun getStrategyName(strategy: String, context: Context? = null): String {
        val clean = StrategyTester.normalizeStrategy(strategy)
        val custom = customNames[clean] ?: customNames[strategy]
        if (custom != null) return custom
        
        val index = StrategyTester.defaultStrategies.indexOfFirst {
            StrategyTester.normalizeStrategy(it) == clean
        }
        return if (index >= 0) "Способ ${index + 1}" else "Кастомный способ"
    }

    fun getActiveStrategyName(context: Context? = null): String {
        val active = appliedStrategy
        if (active == null) return "По умолчанию"
        return getStrategyName(active, context)
    }

    fun exportWorkingStrategiesJson(context: Context): String {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val root = org.json.JSONObject()
        root.put("version", 1)
        root.put("app", "NetFix")
        val array = org.json.JSONArray()

        val exportedSet = LinkedHashSet<String>()

        pinnedStrategies.forEach { (strategy, pinned) ->
            if (pinned && !deletedStrategies.containsKey(strategy)) {
                exportedSet.add(strategy)
            }
        }

        testResults.forEach { item ->
            val strategy = item.second
            val normalized = StrategyTester.normalizeStrategy(strategy)
            val manualStatus = prefs.getString("manual_status_$normalized", null)
            val isWorking = manualStatus == "working"
            if (isWorking && !deletedStrategies.containsKey(strategy)) {
                exportedSet.add(strategy)
            }
        }

        exportedSet.forEach { strategy ->
            val obj = org.json.JSONObject()
            obj.put("strategy", strategy)
            val name = getStrategyName(strategy, context)
            obj.put("name", name)
            val notes = strategyNotes[strategy]
            if (!notes.isNullOrBlank()) {
                obj.put("notes", notes)
            }
            array.put(obj)
        }

        root.put("strategies", array)
        return root.toString(2)
    }

    fun exportAllResultsJson(context: Context): String {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val root = org.json.JSONObject()
        root.put("version", 3)
        root.put("app", "NetFix Mobile")
        root.put("kind", "scan_results")

        val exported = LinkedHashMap<String, String>()
        fun include(strategy: String?) {
            if (strategy.isNullOrBlank() || deletedStrategies.containsKey(strategy)) return
            exported.putIfAbsent(StrategyTester.normalizeStrategy(strategy), strategy)
        }
        testResults
            .filter { StrategyTester.isSuccessfulStatus(it.third) }
            .sortedByDescending { StrategyTester.successScore(it.third) ?: 0.0 }
            .forEach { include(it.second) }
        customStrategies.forEach { include(it) }
        pinnedStrategies.filter { it.value }.keys.forEach { include(it) }
        NetworkKind.values().forEach { kind ->
            verifyResults[kind].orEmpty().forEach { include(it.strategy) }
            include(prefs.strategyForNetwork(kind))
        }

        val array = org.json.JSONArray()
        exported.forEach { (normalized, strategy) ->
            val obj = org.json.JSONObject()
            obj.put("strategy", strategy)
            obj.put("name", getStrategyName(strategy, context))
            val status = testResults.firstOrNull { StrategyTester.normalizeStrategy(it.second) == normalized }?.third
            obj.put("status", status ?: "Импортировано (Успешно)")
            obj.put("pinned", pinnedStrategies[strategy] == true)
            obj.put("custom", customStrategies.any { StrategyTester.normalizeStrategy(it) == normalized })
            strategyNotes[strategy]?.takeIf { it.isNotBlank() }?.let { obj.put("notes", it) }
            array.put(obj)
        }
        root.put("strategies", array)

        val networks = org.json.JSONObject()
        val networkNames = org.json.JSONObject()
        NetworkKind.values().forEach { kind ->
            prefs.strategyForNetwork(kind)?.let {
                networks.put(kind.preferenceSuffix, it)
                networkNames.put(kind.preferenceSuffix, getStrategyName(it, context))
            }
        }
        root.put("networks", networks)
        root.put("networkNames", networkNames)

        val verify = org.json.JSONObject()
        NetworkKind.values().forEach { kind ->
            val list = org.json.JSONArray()
            verifyResults[kind].orEmpty().filter { !deletedStrategies.containsKey(it.strategy) }.forEach { item ->
                list.put(
                    org.json.JSONObject()
                        .put("strategy", item.strategy)
                        .put("name", getStrategyName(item.strategy, context))
                        .put("yt", item.youtubeOk).put("ytTotal", item.youtubeTotal)
                        .put("dc", item.discordOk).put("dcTotal", item.discordTotal)
                        .put("ms", item.averageMs)
                )
            }
            verify.put(kind.preferenceSuffix, list)
        }
        root.put("verify", verify)
        return root.toString(2)
    }

    private fun importScanResults(context: Context, root: org.json.JSONObject): Int {
        val array = root.optJSONArray("strategies") ?: return 0
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val strategy = obj.optString("strategy", "")
            if (strategy.isBlank()) continue
            val status = obj.optString("status", "Импортировано (Успешно)").ifBlank { "Импортировано (Успешно)" }
            deletedStrategies.remove(strategy)
            val existing = testResults.indexOfFirst { it.second == strategy }
            if (existing >= 0) {
                testResults[existing] = Triple(testResults[existing].first, strategy, status)
            } else {
                testResults.add(Triple(testResults.size + 1, strategy, status))
                if (strategy !in customStrategies && StrategyTester.defaultStrategies.none { StrategyTester.normalizeStrategy(it) == StrategyTester.normalizeStrategy(strategy) }) {
                    customStrategies.add(strategy)
                }
            }
            if (obj.optBoolean("pinned", false)) pinnedStrategies[strategy] = true
            obj.optString("notes", "").takeIf { it.isNotBlank() }?.let { strategyNotes[strategy] = it }
            obj.optString("name", "").takeIf { it.isNotBlank() && !it.matches(Regex("Способ \\d+")) && it != "Кастомный способ" }
                ?.let { customNames[strategy] = it }
            if (obj.optBoolean("custom", false) && strategy !in customStrategies) customStrategies.add(strategy)
            count++
        }
        val verify = root.optJSONObject("verify")
        if (verify != null) {
            NetworkKind.values().forEach { kind ->
                val items = verify.optJSONArray(kind.preferenceSuffix) ?: return@forEach
                val list = (0 until items.length()).mapNotNull { index ->
                    val item = items.optJSONObject(index) ?: return@mapNotNull null
                    val itemStrategy = item.optString("strategy", "")
                    if (itemStrategy.isBlank()) return@mapNotNull null
                    RankedConfig(
                        strategy = itemStrategy,
                        youtubeOk = item.optInt("yt"),
                        youtubeTotal = item.optInt("ytTotal"),
                        discordOk = item.optInt("dc"),
                        discordTotal = item.optInt("dcTotal"),
                        averageMs = item.optLong("ms", -1L)
                    )
                }
                if (list.isNotEmpty()) verifyResults[kind] = list
            }
            saveVerifyResults(context)
        }
        val networks = root.optJSONObject("networks")
        if (networks != null) {
            NetworkKind.values().forEach { kind ->
                networks.optString(kind.preferenceSuffix, "").takeIf { it.isNotBlank() }
                    ?.let { prefs.rememberStrategyForNetwork(kind, it) }
            }
        }
        if (count > 0) {
            resortResults(context)
            saveCustomizations(context)
            saveResults(context)
            selectionVersion += 1
        }
        return count
    }

    fun importStrategiesFromJson(context: Context, jsonString: String): Int {
        try {
            val root = org.json.JSONObject(jsonString)
            if (root.optString("kind") == "scan_results") return importScanResults(context, root)
            val array = root.optJSONArray("strategies") ?: return 0
            var importedCount = 0

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val strategy = obj.optString("strategy", "")
                if (strategy.isBlank()) continue

                val rawName = obj.optString("name", "Способ")
                val cleanName = if (rawName.startsWith("EX: ")) rawName else "EX: $rawName"
                val notes = obj.optString("notes", "")

                customNames[strategy] = cleanName
                pinnedStrategies[strategy] = true
                if (notes.isNotBlank()) {
                    strategyNotes[strategy] = notes
                }

                val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                val normalized = StrategyTester.normalizeStrategy(strategy)
                prefs.edit().putString("manual_status_$normalized", "working").apply()

                val existingIndex = testResults.indexOfFirst { it.second == strategy }
                if (existingIndex >= 0) {
                    testResults[existingIndex] = Triple(testResults[existingIndex].first, strategy, "Импортировано (Успешно)")
                } else {
                    val newIdx = testResults.size + 1
                    testResults.add(0, Triple(newIdx, strategy, "Импортировано (Успешно)"))
                }

                importedCount++
            }

            if (importedCount > 0) {
                saveCustomizations(context)
                saveResults(context)
            }
            return importedCount
        } catch (e: Exception) {
            Log.e("StrategyTestManager", "Ошибка импорта JSON", e)
            return 0
        }
    }
}
