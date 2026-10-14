package com.rupleide.netfix.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.rupleide.netfix.R

fun ActivityResultLauncher<Intent>.launchVpnConsentSafely(consentIntent: Intent): Boolean =
    try {
        launch(consentIntent)
        true
    } catch (e: ActivityNotFoundException) {
        com.rupleide.netfix.data.vpnConsentUnavailableGlobal = true
        false
    }

@Composable
fun VpnConsentUnavailableSheet() {
    val context = LocalContext.current
    val grantCommand = "adb shell appops set ${context.packageName} ACTIVATE_VPN allow"

    NetFixActionSheet(
        visible = com.rupleide.netfix.data.vpnConsentUnavailableGlobal,
        onDismissRequest = { com.rupleide.netfix.data.vpnConsentUnavailableGlobal = false },
        icon = R.drawable.ic_info,
        title = "Нет окна разрешения VPN",
        subtitle = "Прошивка этого устройства не показывает системный запрос на VPN. Разрешение можно выдать один раз с компьютера командой:\n\n$grantCommand",
        options = listOf(
            NetFixSheetOption(label = "Скопировать команду") {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NetFix VPN", grantCommand))
                android.widget.Toast.makeText(context, "Команда скопирована", android.widget.Toast.LENGTH_SHORT).show()
            }
        ),
        closeLabel = "Закрыть"
    )
}
