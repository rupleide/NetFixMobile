package com.rupleide.netfix.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.BuildConfig
import com.rupleide.netfix.core.beta.BetaExpiry
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.theme.NetFixFonts

private const val BACKUP_FILE_NAME = "netfix_backup.json"

@Composable
fun BetaExpiredScreen() {
    val context = LocalContext.current
    val exitApp = { (context as? Activity)?.finishAffinity() ?: Unit }
    BackHandler(onBack = exitApp)
    var saved by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val backupJson = { StrategyTestManager.exportAllResultsJson(context) }
    val saveFallback: () -> Unit = {
        try {
            val file = File(context.getExternalFilesDir(null), BACKUP_FILE_NAME)
            file.parentFile?.mkdirs()
            file.writeText(backupJson())
            saved = true
            notice = "Файл сохранён: ${file.absolutePath}"
        } catch (e: Exception) {
            notice = "Не получилось сохранить: ${e.message}"
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(backupJson().toByteArray(Charsets.UTF_8)) }
                saved = true
                notice = "Файл сохранён. Теперь удалите тестовую версию."
            } catch (e: Exception) {
                notice = "Не получилось сохранить: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Nf.Bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Тестовая версия закончилась",
                fontFamily = NetFixFonts.Unbounded,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = Nf.Text,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Бета NetFix ${BuildConfig.VERSION_NAME} работала до ${BetaExpiry.lastWorkingDayLabel} включительно. Спасибо, что помогли её проверить!\n\n1. Сохраните свои способы в файл.\n2. Удалите эту тестовую версию: поверх обычная не встанет.\n3. Установите обычную версию и загрузите файл: Настройки, «Импорт данных».",
                fontFamily = NetFixFonts.Manrope,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Nf.TextSub,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            NfButton(
                text = if (saved) "Способы сохранены" else "Сохранить мои способы",
                onClick = {
                    try {
                        exportLauncher.launch(BACKUP_FILE_NAME)
                    } catch (_: Exception) {
                        saveFallback()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = NfButtonStyle.Secondary
            )
            NfButton(
                text = "Удалить тестовую версию",
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = NfButtonStyle.Secondary
            )
            NfButton(
                text = "Установить обычную версию",
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(BetaExpiry.RELEASES_PAGE))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = NfButtonStyle.Secondary
            )
            notice?.let {
                Text(
                    text = it,
                    fontFamily = NetFixFonts.Manrope,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Nf.TextSub,
                    textAlign = TextAlign.Center
                )
            }
            NfButton(
                text = "Выйти",
                onClick = exitApp,
                modifier = Modifier.fillMaxWidth(),
                style = NfButtonStyle.Secondary
            )
        }
    }
}
