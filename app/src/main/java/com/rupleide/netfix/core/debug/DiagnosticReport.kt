package com.rupleide.netfix.core.debug

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.FileProvider
import com.rupleide.netfix.BuildConfig
import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.NetworkKind
import com.rupleide.netfix.core.dpibypass.ProxyPortResolver
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.tgproxy.NativeProxy
import com.rupleide.netfix.core.tgproxy.TgProxyController
import java.io.File

object DiagnosticReport {
    const val FILE_NAME = "NetFix_Diagnostic_Report.txt"
    private const val MAX_ERROR_LINES = 100
    private const val MAX_SCAN_ROWS = 5
    private const val PORT_PROBE_TIMEOUT_MS = 300
    private val extraPorts = listOf(1082)
    private val errorMarkers = listOf("Exception", "Error", "ERROR", "Ошибка", "Не удалось", "failed", "Failed", "code -")

    fun build(context: Context, helpSection: String? = null): String {
        val prefs = context.getPreferences()
        val sb = StringBuilder()
        sb.append("=== NetFix Mobile: диагностический отчёт ===\n")
        sb.append("Версия: ${BuildConfig.VERSION_NAME} (код ${BuildConfig.VERSION_CODE})\n\n")

        if (helpSection != null) {
            sb.append("--- Помощник ---\n")
            sb.append(helpSection)
            sb.append('\n')
        }

        sb.append(AppDebugManager.generateSystemInfoDump(context))
        sb.append('\n')

        sb.append("--- Сеть ---\n")
        appendNetwork(context, sb)

        sb.append("\n--- Туннель и порты ---\n")
        sb.append("VPN-туннель NetFix: ${if (ByeDpiVpnService.isConnected) "активен" else "не активен"}\n")
        val ports = listOf(ProxyPortResolver.DEFAULT_PROXY_PORT) + extraPorts + TgProxyController.getPort(context)
        ports.distinct().forEach { port ->
            val listening = TgProxyController.isPortOpen(TgProxyController.DEFAULT_BIND_IP, port, PORT_PROBE_TIMEOUT_MS)
            sb.append("Порт $port: ${if (listening) "слушает" else "свободен"}\n")
        }
        sb.append("TG-ядро: ")
        if (TgProxyController.isRunning) {
            sb.append(runCatching { NativeProxy.getStats() }.getOrNull() ?: "статистика недоступна")
        } else {
            sb.append("не запущено")
        }
        sb.append('\n')

        sb.append("\n--- Совместимость ---\n")
        sb.append("Сеть как в: v${CompatibilityManager.behaviorVersion(prefs)}${if (CompatibilityManager.isCustomized(prefs)) " (тонкая настройка)" else ""}\n")
        sb.append("DNS в туннеле: ${CompatibilityManager.tunnelDnsMode(prefs).storageValue}\n")
        CompatFeature.entries.forEach {
            sb.append("${it.name} (с v${it.sinceVersion}): ${if (CompatibilityManager.featureEnabled(prefs, it)) "вкл" else "выкл"}\n")
        }

        sb.append("\n--- Последний скан ---\n")
        appendScan(sb)

        sb.append("\n--- Последние ошибки в логе ---\n")
        appendErrors(sb)

        sb.append("=================================\n")
        return DiagnosticMasker.mask(sb.toString())
    }

    private fun appendNetwork(context: Context, sb: StringBuilder) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val properties = cm.getLinkProperties(cm.activeNetwork)
            if (properties == null) {
                sb.append("Активная сеть: нет\n")
                return
            }
            sb.append("Интерфейс: ${properties.interfaceName ?: "неизвестно"}\n")
            val gateway = properties.routes.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway?.hostAddress
            sb.append("Шлюз: ${gateway ?: "неизвестно"}\n")
            sb.append("Private DNS: ${if (properties.isPrivateDnsActive) properties.privateDnsServerName ?: "авто" else "выкл"}\n")
        } catch (e: Exception) {
            sb.append("Сеть: ошибка определения\n")
        }
    }

    private fun appendScan(sb: StringBuilder) {
        var any = false
        NetworkKind.values().forEach { kind ->
            val rows = StrategyTestManager.verifyResults[kind].orEmpty()
                .sortedWith(compareByDescending<StrategyTestManager.RankedConfig> { it.totalOk }.thenBy { it.averageMs })
                .take(MAX_SCAN_ROWS)
            if (rows.isEmpty()) return@forEach
            any = true
            sb.append("${kind.displayName}:\n")
            rows.forEach { row ->
                sb.append("  ${row.totalOk}/${row.total}, ${row.averageMs} мс: ${row.strategy}\n")
            }
        }
        if (!any) sb.append("Результатов скана нет\n")
        val failures = AppDebugManager.getLogs().filter { it.contains("[SCANNER] [ERROR]") }.take(MAX_SCAN_ROWS * 2)
        if (failures.isNotEmpty()) {
            sb.append("Причины отказов хостов:\n")
            failures.forEach { sb.append("  $it\n") }
        }
    }

    private fun appendErrors(sb: StringBuilder) {
        if (!AppDebugManager.isLoggingEnabled()) {
            sb.append("Запись логов выключена. Включите её в Настройках → Отладка и повторите проблему\n")
            return
        }
        val lines = AppDebugManager.getLogs()
            .filter { line -> errorMarkers.any { line.contains(it) } && !line.contains("[SCANNER] [ERROR]") }
            .take(MAX_ERROR_LINES)
            .reversed()
        if (lines.isEmpty()) sb.append("Ошибок нет\n") else lines.forEach { sb.append(it).append('\n') }
    }

    fun shareOrSave(context: Context): String? {
        val file = File(context.cacheDir, FILE_NAME)
        file.writeText(build(context))
        val canShare = com.rupleide.netfix.core.help.HelpReport.canShare(context)
        if (canShare) {
            try {
                startShare(context, file)
                return null
            } catch (_: Exception) {
            }
        }
        val saved = com.rupleide.netfix.core.help.HelpReport.saveToDownloads(context, file)
        return if (saved != null) "Файл сохранён: $saved" else "Не получилось ни отправить, ни сохранить файл"
    }

    private fun startShare(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "NetFix Mobile: диагностика")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Поделиться диагностикой").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
