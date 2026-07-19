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
import java.io.File

object StrategyTestManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isTesting by mutableStateOf(false)
        private set

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
        get() = StrategyTester.defaultStrategies.size

    val pinnedStrategies = mutableStateMapOf<String, Boolean>()
    val customNames = mutableStateMapOf<String, String>()
    val strategyNotes = mutableStateMapOf<String, String>()
    val deletedStrategies = mutableStateMapOf<String, Boolean>()

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

            file.writeText(json.toString())
        } catch (_: Exception) {}
    }

    fun init(context: Context) {
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

                if (loaded.isNotEmpty()) {
                    testResults.clear()
                    val activeApplied = appliedStrategy
                    val filtered = loaded.filter { !deletedStrategies.containsKey(it.second) && !it.third.contains("тайм-аут") }
                    val sortedLoaded = filtered.sortedWith(compareBy<Triple<Int, String, String>> {
                        if (pinnedStrategies.containsKey(it.second)) 0 else 1
                    }.thenBy {
                        if (activeApplied != null && it.second.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == activeApplied) 0 else 1
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
                val best = tester.runTests { index, strategy, status ->
                    currentTestIndex = index + 1
                    currentProgress = "Проверяем стратегию ${index + 1} из $totalStrategiesCount"
                    if (!deletedStrategies.containsKey(strategy)) {
                        testResults.add(0, Triple(index + 1, strategy, status))
                    }
                }

                Log.i("StrategyTestManager", "Автоподбор завершен. Лучшая стратегия: \"$best\"")

                if (best != null) {
                    val formattedBest = best.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
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
                val filtered = testResults.filter { !deletedStrategies.containsKey(it.second) && !it.third.contains("тайм-аут") }
                val sorted = filtered.sortedWith(compareBy<Triple<Int, String, String>> {
                    if (pinnedStrategies.containsKey(it.second)) 0 else 1
                }.thenBy {
                    if (activeApplied != null && it.second.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == activeApplied) 0 else 1
                }.thenBy {
                    if (it.third.contains("мс")) 0 else 1
                }.thenBy {
                    if (it.third.contains("мс")) {
                        it.third.substringBefore(" мс").toLongOrNull() ?: Long.MAX_VALUE
                    } else {
                        Long.MAX_VALUE
                    }
                })
                testResults.clear()
                testResults.addAll(sorted)

                saveResults(context)
                showNotification(context)

                if (wasRunning) {
                    com.rupleide.netfix.core.dpibypass.ServiceManager.start(
                        context,
                        com.rupleide.netfix.data.Mode.VPN
                    )
                }
            } finally {
                isTesting = false
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

    fun deleteStrategy(context: Context, strategy: String) {
        deletedStrategies[strategy] = true
        saveCustomizations(context)
        testResults.removeAll { it.second == strategy }
        saveResults(context)
    }

    private fun resortResults(context: Context) {
        val activeApplied = appliedStrategy
        val current = testResults.filter { !it.third.contains("тайм-аут") }
        val sorted = current.sortedWith(compareBy<Triple<Int, String, String>> {
            if (pinnedStrategies.containsKey(it.second)) 0 else 1
        }.thenBy {
            if (activeApplied != null && it.second.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == activeApplied) 0 else 1
        }.thenBy {
            if (it.third.contains("мс")) 0 else 1
        }.thenBy {
            if (it.third.contains("мс")) {
                it.third.substringBefore(" мс").toLongOrNull() ?: Long.MAX_VALUE
            } else {
                Long.MAX_VALUE
            }
        })
        testResults.clear()
        testResults.addAll(sorted)
        saveResults(context)
    }

    fun getStrategyName(strategy: String, context: Context? = null): String {
        val clean = strategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
        val custom = customNames[clean] ?: customNames[strategy]
        if (custom != null) return custom
        
        val index = StrategyTester.defaultStrategies.indexOfFirst {
            it.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == clean
        }
        return if (index >= 0) "Способ ${index + 1}" else "Кастомный способ"
    }

    fun getActiveStrategyName(context: Context? = null): String {
        val active = appliedStrategy
        if (active == null) return "Способ по умолчанию"
        return getStrategyName(active, context)
    }
}
