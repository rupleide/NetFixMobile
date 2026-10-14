package com.rupleide.netfix.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AggressiveFirmware(val displayName: String) {
    ColorOS("ColorOS"),
    MiuiHyperOs("MIUI / HyperOS"),
    OneUi("One UI")
}

fun detectAggressiveFirmware(): AggressiveFirmware? {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> AggressiveFirmware.ColorOS
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> AggressiveFirmware.MiuiHyperOs
        manufacturer.contains("samsung") -> AggressiveFirmware.OneUi
        else -> null
    }
}

const val STABLE_PROXY_HINT_DISMISSED_KEY = "stable_proxy_hint_dismissed"

@Composable
fun StableProxyHintCard(
    firmware: AggressiveFirmware,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Nf.Text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Важно: у вас ${firmware.displayName}",
                color = Nf.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
        Text(
            text = "Эта прошивка может закрывать Telegram-прокси через 10–15 секунд после сворачивания. Если Telegram перестаёт подключаться, включите «Режим стабильного прокси». Если всё работает, включать не нужно: режим немного расходует батарею.",
            color = Nf.TextSub,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NfSoftButton(
                text = "Скрыть",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            NfSoftButton(
                text = "Включить",
                onClick = onEnable,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
