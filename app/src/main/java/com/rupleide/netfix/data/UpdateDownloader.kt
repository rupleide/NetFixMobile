package com.rupleide.netfix.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rupleide.netfix.R
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateDownloader {
    private const val CHANNEL_ID = "update_channel"
    private const val NOTIFICATION_ID = 9999

    fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: ((Float) -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        Log.e("NetFixDebug", "downloadAndInstall called with URL: $downloadUrl")
        android.widget.Toast.makeText(appContext, "Загрузка обновления...", android.widget.Toast.LENGTH_SHORT).show()
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновления",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Скачивание обновления")
            .setContentText("Подготовка...")
            .setProgress(100, 0, true)
            .setOngoing(true)
            .setSilent(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = android.net.Uri.parse("package:${appContext.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            appContext.startActivity(intent)
                        }
                        throw Exception("Необходимо разрешение на установку неизвестных приложений")
                    }
                }
                var currentUrl = downloadUrl
                var connection: HttpURLConnection? = null
                var redirectCount = 0
                while (redirectCount < 5) {
                    connection = URL(currentUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.instanceFollowRedirects = false
                    val code = connection.responseCode
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        val newUrl = connection.getHeaderField("Location")
                        if (newUrl.isNullOrEmpty()) break
                        currentUrl = newUrl
                        redirectCount++
                    } else {
                        break
                    }
                }

                if (connection == null || connection.responseCode != 200) {
                    throw Exception("Не удалось загрузить файл: ${connection?.responseCode}")
                }

                val totalLength = connection.contentLength
                val destinationFile = File(appContext.externalCacheDir ?: appContext.cacheDir, "update.apk")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                connection.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastProgress = 0
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalLength > 0) {
                                val progress = ((totalRead * 100) / totalLength).toInt()
                                if (progress > lastProgress) {
                                    lastProgress = progress
                                    builder.setProgress(100, progress, false)
                                        .setContentText("Загружено $progress%")
                                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                                    
                                    CoroutineScope(Dispatchers.Main).launch {
                                        onProgress?.invoke(progress / 100f)
                                    }
                                }
                            }
                        }
                    }
                }

                Log.e("NetFixDebug", "Download finished, file size: ${destinationFile.length()}")
                builder.setContentTitle("Установка обновления")
                    .setContentText("Инициализация установки...")
                    .setProgress(0, 0, true)
                notificationManager.notify(NOTIFICATION_ID, builder.build())

                CoroutineScope(Dispatchers.Main).launch {
                    onFinished?.invoke()
                }

                installPackage(appContext, destinationFile)
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.e("NetFixDebug", "Update error", e)
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(appContext, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    onError?.invoke(e.message ?: "Не удалось скачать файл")
                }
                builder.setContentTitle("Ошибка обновления")
                    .setContentText(e.message ?: "Не удалось скачать файл")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    private fun installPackage(context: Context, file: File) {
        Log.e("NetFixDebug", "installPackage started, size: ${file.length()}")
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
