package com.rupleide.netfix.service

import android.content.Context
import android.net.VpnService
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.data.Mode
import java.util.concurrent.TimeUnit

class WatchdogWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val appCtx = applicationContext
        val prefs = appCtx.getSharedPreferences(appCtx.packageName + "_preferences", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("service_enabled", false)

        val autostartEnabled = prefs.getBoolean("autostart", false)
        if (!serviceEnabled || !autostartEnabled) return Result.success()

        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)

        val serviceDead = !ByeDpiVpnService.isConnected
        val tgDead = !TgProxyController.isRunning && !TgProxyController.isPortOpen(
            TgProxyController.DEFAULT_BIND_IP,
            TgProxyController.getPort(appCtx),
            500
        )

        if (wantsYoutube && serviceDead) {
            if (VpnService.prepare(appCtx) == null) {
                ServiceManager.start(appCtx, Mode.VPN)
            }
        }

        if (wantsTelegram && tgDead) {
            if (wantsYoutube) {
                TgProxyController.startAsync(appCtx, {}, {})
            } else {
                com.rupleide.netfix.core.tgproxy.TgProxyService.startInForeground(appCtx, openTelegram = false)
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "com.rupleide.netfix.work.WATCHDOG"

        fun schedulePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(
                30, TimeUnit.MINUTES,
                10, TimeUnit.MINUTES
            ).setInitialDelay(30, TimeUnit.MINUTES).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
