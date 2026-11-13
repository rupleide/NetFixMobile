package com.rupleide.netfix.core.help

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rupleide.netfix.MainActivity
import com.rupleide.netfix.R
import java.util.concurrent.TimeUnit

class HelpReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val symptomName = inputData.getString(KEY_SYMPTOM) ?: return Result.success()
        val symptom = HelpSymptom.entries.firstOrNull { it.name == symptomName } ?: return Result.success()
        val app = applicationContext
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Уведомления NetFix", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = Intent(app, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_HELP, symptom.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending = PendingIntent.getActivity(app, NOTIFICATION_ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${symptom.title.removeSuffix(" отваливается")}: всё ещё работает?")
            .setContentText("Помощник NetFix: нажмите и ответьте, помогло ли исправление")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
        return Result.success()
    }

    companion object {
        const val EXTRA_OPEN_HELP = "netfix_open_help"
        private const val KEY_SYMPTOM = "symptom"
        private const val CHANNEL_ID = "NetFixNotifications"
        private const val NOTIFICATION_ID = 1012
        private const val WORK_NAME = "netfix_help_reminder"
        const val DELAY_MINUTES = 30L

        fun schedule(context: Context, symptom: HelpSymptom) {
            val request = OneTimeWorkRequestBuilder<HelpReminderWorker>()
                .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_SYMPTOM to symptom.name))
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
        }
    }
}
