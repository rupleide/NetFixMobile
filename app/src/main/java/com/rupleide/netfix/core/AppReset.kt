package com.rupleide.netfix.core

import android.content.Context
import android.content.Intent
import com.rupleide.netfix.core.debug.AppDebugManager
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.core.tgproxy.TgProxyService
import com.rupleide.netfix.data.STOP_ACTION
import com.rupleide.netfix.service.WatchdogWorker

object AppReset {
    private const val HINTS_PREFS = "netfix_hints"
    private val TELEGRAM_LINK_KEYS = listOf("secret_key", "tgproxy_port", "tg_proxy_configured")

    fun resetSettingsKeepingResults(context: Context, reason: String) {
        val app = context.applicationContext
        stopEverything(app)
        AppDebugManager.log("Сброс настроек с сохранением результатов тестов: $reason")
        val prefs = app.getSharedPreferences(app.packageName + "_preferences", Context.MODE_PRIVATE)
        val isSetupComplete = prefs.getBoolean("wizard_is_setup_complete", false)
        val isSetupCompleteOld = prefs.getBoolean("is_setup_complete", false)
        val currentTestIndex = prefs.getInt("wizard_current_test_index", 0)
        val currentTestIndexOld = prefs.getInt("current_test_index", 0)
        val savedSelectedApps = prefs.getStringSet("selected_apps", null)?.toSet()
        val telegramLink = TELEGRAM_LINK_KEYS.associateWith { prefs.all[it] }
        prefs.edit().clear().commit()
        app.getSharedPreferences(HINTS_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        com.rupleide.netfix.core.help.HelpRepairStore.clear(app)
        prefs.edit().apply {
            putBoolean("wizard_is_setup_complete", isSetupComplete)
            putBoolean("is_setup_complete", isSetupCompleteOld)
            putInt("wizard_current_test_index", currentTestIndex)
            putInt("current_test_index", currentTestIndexOld)
            if (savedSelectedApps != null) putStringSet("selected_apps", savedSelectedApps)
            telegramLink.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                }
            }
        }.commit()
        restart(app)
    }

    fun resetEverything(context: Context) {
        val app = context.applicationContext
        stopEverything(app)
        AppDebugManager.log("Выполнен полный сброс настроек приложения (Сбросить всё) с перезапуском")
        app.getSharedPreferences(app.packageName + "_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences(HINTS_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        restart(app)
    }

    private fun stopEverything(app: Context) {
        ServiceManager.stop(app)
        try {
            app.startService(Intent(app, TgProxyService::class.java).setAction(STOP_ACTION))
        } catch (_: Exception) {
        }
        TgProxyController.stop()
        WatchdogWorker.cancelPeriodicWork(app)
    }

    private fun restart(app: Context) {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) app.startActivity(intent)
        System.exit(0)
    }
}
