package com.rupleide.netfix.core.debug

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppDebugManager {
    private const val MAX_LOGS = 2000
    private val logs = java.util.ArrayList<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var isEnabled = false
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var statsSampler: Thread? = null
    private const val STATS_INTERVAL_MS = 10_000L

    fun i(tag: String, msg: String) {
        log("$tag: $msg")
        android.util.Log.i(tag, msg)
    }

    fun d(tag: String, msg: String) {
        log("$tag: $msg")
        android.util.Log.d(tag, msg)
    }

    fun w(tag: String, msg: String) {
        log("$tag: $msg")
        android.util.Log.w(tag, msg)
    }

    fun w(tag: String, msg: String, tr: Throwable) {
        log("$tag: $msg\n${android.util.Log.getStackTraceString(tr)}")
        android.util.Log.w(tag, msg, tr)
    }

    fun e(tag: String, msg: String) {
        log("$tag: $msg")
        android.util.Log.e(tag, msg)
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        log("$tag: $msg\n${android.util.Log.getStackTraceString(tr)}")
        android.util.Log.e(tag, msg, tr)
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean("debug_log_enabled", false)
        if (isEnabled) startBackgroundRecording(context.applicationContext)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("debug_log_enabled", enabled).apply()
        if (!enabled) {
            stopBackgroundRecording(context.applicationContext)
            synchronized(logs) {
                logs.clear()
            }
        } else {
            log("Отладка включена")
            startBackgroundRecording(context.applicationContext)
        }
    }

    private fun describeNetwork(capabilities: android.net.NetworkCapabilities?): String {
        if (capabilities == null) return "нет сети"
        val transport = when {
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "мобильная"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "другая"
        }
        val validated = capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val internet = capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return "$transport, интернет=$internet, проверена=$validated"
    }

    private fun startBackgroundRecording(appContext: Context) {
        if (networkCallback == null) {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                private var lastSummary = ""

                override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                    val summary = describeNetwork(capabilities)
                    if (summary == lastSummary) return
                    lastSummary = summary
                    log("Сеть по умолчанию: $summary")
                }

                override fun onLost(network: android.net.Network) {
                    lastSummary = ""
                    log("Сеть по умолчанию потеряна")
                }
            }
            try {
                connectivityManager.registerDefaultNetworkCallback(callback)
                networkCallback = callback
            } catch (_: Exception) {
            }
        }
        if (statsSampler == null) {
            val sampler = Thread {
                var lastStats = ""
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        Thread.sleep(STATS_INTERVAL_MS)
                    } catch (_: InterruptedException) {
                        break
                    }
                    if (!com.rupleide.netfix.core.tgproxy.TgProxyController.isRunning) continue
                    val stats = try {
                        com.rupleide.netfix.core.tgproxy.NativeProxy.getStats() ?: continue
                    } catch (_: Throwable) {
                        continue
                    }
                    if (stats != lastStats) {
                        lastStats = stats
                        log("Статистика ядра TG: $stats")
                    }
                }
            }
            sampler.isDaemon = true
            sampler.name = "netfix-debug-sampler"
            sampler.start()
            statsSampler = sampler
        }
    }

    private fun stopBackgroundRecording(appContext: Context) {
        networkCallback?.let {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
        statsSampler?.interrupt()
        statsSampler = null
    }

    fun isLoggingEnabled(): Boolean = isEnabled

    fun log(message: String) {
        if (!isEnabled) return
        val time = dateFormat.format(Date())
        val entry = "[$time] $message"
        synchronized(logs) {
            if (logs.size >= MAX_LOGS) {
                logs.removeAt(0)
            }
            logs.add(entry)
        }
    }

    fun getLogs(): List<String> {
        return synchronized(logs) {
            logs.toList().reversed()
        }
    }

    fun clear() {
        synchronized(logs) {
            logs.clear()
        }
    }

    fun generateSystemInfoDump(context: Context): String {
        val sb = java.lang.StringBuilder()
        sb.append("=== NetFix Mobile System Info ===\n")
        sb.append("Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})\n")
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            sb.append("Версия приложения: $versionName (код $versionCode)\n")
        } catch (_: Exception) {
            sb.append("Версия приложения: ошибка\n")
        }
        try {
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val freeMem = memoryInfo.availMem / 1048576L
            val totalMem = memoryInfo.totalMem / 1048576L
            sb.append("Память: $freeMem МБ свободно из $totalMem МБ\n")
        } catch (_: Exception) {
            sb.append("Память: ошибка\n")
        }
        try {
            val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == android.os.BatteryManager.BATTERY_STATUS_FULL
            sb.append("Батарея: $pct% (${if (isCharging) "заряжается" else "разряжается"})\n")
        } catch (e: Exception) {
            sb.append("Батарея: неизвестно\n")
        }
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val networkType = when {
                capabilities == null -> "Нет сети"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Мобильная сеть"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Другая"
            }
            sb.append("Тип сети: $networkType\n")
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            val operatorName = tm.networkOperatorName
            if (!operatorName.isNullOrEmpty()) {
                sb.append("Оператор сотовой связи: $operatorName\n")
            }
            val linkProperties = cm.getLinkProperties(activeNetwork)
            val dnsList = linkProperties?.dnsServers?.map { it.hostAddress } ?: emptyList()
            if (dnsList.isNotEmpty()) {
                sb.append("Системные DNS: ${dnsList.joinToString(", ")}\n")
            }
        } catch (e: Exception) {
            sb.append("Сеть: ошибка определения\n")
        }
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            var otherVpnActive = false
            val networks = cm.allNetworks
            for (network in networks) {
                val caps = cm.getNetworkCapabilities(network)
                if (caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) {
                    otherVpnActive = true
                }
            }
            sb.append("Активен другой VPN в системе: ${if (otherVpnActive) "Да" else "Нет"}\n")
        } catch (e: Exception) {
            sb.append("Проверка других VPN: ошибка\n")
        }
        try {
            val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            val serviceEnabled = prefs.getBoolean("service_enabled", false)
            val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
            val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
            sb.append("Служба NetFix включена: ${if (serviceEnabled) "Да" else "Нет"}\n")
            sb.append("Обход YouTube включен: ${if (wantsYoutube) "Да" else "Нет"}\n")
            sb.append("Фикс Telegram включен: ${if (wantsTelegram) "Да" else "Нет"}\n")
            if (wantsYoutube) {
                val manualMode = prefs.getBoolean("strategy_manual_mode", false)
                val byedpiArgs = prefs.getString("byedpi_cmd_args", "")
                sb.append("Режим YouTube: ${if (manualMode) "Вручную" else "Авто"}\n")
                sb.append("Аргументы ByeDpi: \"$byedpiArgs\"\n")
                val allowedApps = prefs.getStringSet("selected_apps", null)
                if (allowedApps != null) {
                    sb.append("Выбранные приложения для VPN (${allowedApps.size}): ${allowedApps.joinToString(", ")}\n")
                }
            }
            if (wantsTelegram) {
                val port = com.rupleide.netfix.core.tgproxy.TgProxyController.getPort(context)
                val cfEnabled = com.rupleide.netfix.core.tgproxy.TgProxyController.isCfEnabled(context)
                val cfPriority = com.rupleide.netfix.core.tgproxy.TgProxyController.isCfPriority(context)
                val isPortOpen = com.rupleide.netfix.core.tgproxy.TgProxyController.isPortOpen(
                    com.rupleide.netfix.core.tgproxy.TgProxyController.DEFAULT_BIND_IP,
                    port,
                    300
                )
                sb.append("Порт TG-прокси: $port (CF туннелирование: $cfEnabled, Приоритет CF: $cfPriority, Открыт: $isPortOpen)\n")
            }
        } catch (e: Exception) {
            sb.append("Настройки: ошибка чтения\n")
        }
        sb.append("=================================\n")
        return sb.toString()
    }
}
