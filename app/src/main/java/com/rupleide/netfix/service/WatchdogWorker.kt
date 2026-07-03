package com.rupleide.netfix.service

import android.content.Context
import android.net.VpnService
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.appStatus
import com.rupleide.netfix.data.isTgProxyRunningGlobal
import java.util.concurrent.TimeUnit

class WatchdogWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val appCtx = applicationContext
        val prefs = appCtx.getSharedPreferences(appCtx.packageName + "_preferences", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("service_enabled", false)

        if (!serviceEnabled) return Result.success()

        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)

        val serviceDead = appStatus.first != AppStatus.Running
        val tgDead = !isTgProxyRunningGlobal && !TgProxyController.isPortOpen(
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
                val intent = android.content.Intent(appCtx, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                    action = com.rupleide.netfix.data.START_ACTION
                    putExtra("open_tg", false)
                }
                androidx.core.content.ContextCompat.startForegroundService(appCtx, intent)
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
            ).build()

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
