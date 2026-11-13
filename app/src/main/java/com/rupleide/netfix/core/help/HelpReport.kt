package com.rupleide.netfix.core.help

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.rupleide.netfix.core.debug.DiagnosticReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object HelpReport {
    const val SUPPORT_TELEGRAM = "sofirka_hanabi"
    const val SUPPORT_LINK = "https://t.me/$SUPPORT_TELEGRAM"

    private val telegramPackages = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.messenger.beta",
        "org.thunderdog.challegram",
        "com.exteragram.messenger",
        "tw.nekomimi.nekogram",
        "uz.unnarsx.cherrygram",
        "org.telegram.plus",
        "com.radolyn.ayugram",
        "app.nicegram",
        "com.iMe.android"
    )

    suspend fun write(context: Context, helpSection: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, DiagnosticReport.FILE_NAME)
        file.writeText(DiagnosticReport.build(context, helpSection))
        file
    }

    fun installedTelegram(context: Context): String? =
        telegramPackages.firstOrNull { HelpProbes.isInstalled(context, it) }

    fun canShare(context: Context): Boolean =
        Intent(Intent.ACTION_SEND).setType("text/plain").resolveActivity(context.packageManager) != null

    private const val SUPPORT_DIALOG_ID = 6420309291L
    private const val EXTRA_SHORTCUT_ID = "android.intent.extra.shortcut.ID"

    fun sendToSupport(context: Context, file: File, caption: String): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(EXTRA_SHORTCUT_ID, "did$SUPPORT_DIALOG_ID")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val telegram = installedTelegram(context)
        if (telegram != null) {
            try {
                context.startActivity(Intent(send).setPackage(telegram))
                return true
            } catch (_: Exception) {
            }
        }
        return try {
            context.startActivity(Intent.createChooser(send, "Отправить в поддержку").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun send(context: Context, file: File, text: String): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "NetFix Mobile: диагностика")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val telegram = installedTelegram(context)
        if (telegram != null) {
            try {
                context.startActivity(Intent(send).setPackage(telegram))
                return true
            } catch (_: Exception) {
            }
        }
        return try {
            context.startActivity(Intent.createChooser(send, "Отправить отчёт в поддержку").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSupportChat(context: Context, text: String? = null): Boolean {
        val query = text?.let { "text=" + Uri.encode(it) }
        val attempts = listOf(
            Uri.parse("tg://resolve?domain=$SUPPORT_TELEGRAM" + (query?.let { "&$it" } ?: "")),
            Uri.parse(SUPPORT_LINK + (query?.let { "?$it" } ?: ""))
        )
        attempts.forEach { uri ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    private const val MAX_QR_LINK_LENGTH = 1500

    fun supportLinkWithText(text: String, maxLength: Int = MAX_QR_LINK_LENGTH): String {
        fun linkFor(body: String) = "$SUPPORT_LINK?text=" + Uri.encode(body)
        var lines = text.lines().filter { it.isNotBlank() }
        while (lines.isNotEmpty() && linkFor(lines.joinToString("\n")).length > maxLength) {
            lines = lines.dropLast(1)
        }
        return if (lines.isEmpty()) SUPPORT_LINK else linkFor(lines.joinToString("\n"))
    }

    fun copySupportNick(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Поддержка NetFix", "@$SUPPORT_TELEGRAM"))
    }

    fun copyText(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NetFix", text))
    }

    fun saveToDownloads(context: Context, file: File): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { target ->
                context.contentResolver.openOutputStream(target)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                "Загрузки/${file.name}"
            }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val target = File(dir, file.name)
            file.copyTo(target, overwrite = true)
            target.absolutePath
        }
    } catch (_: Exception) {
        null
    }
}
