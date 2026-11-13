package com.rupleide.netfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val NETWORK_ASSISTANT_HINT_DISMISSED_KEY = "network_assistant_hint_dismissed"

@Composable
fun NetworkAssistantHintStrip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val haptics = rememberNetFixHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                haptics(NetFixHaptic.Tap)
                onClick()
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Nf.Yellow,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Нет интернета по Wi-Fi? Сетевой ассистент",
            color = Nf.TextSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Nf.TextSub,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun NetworkAssistantSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onHideForever: () -> Unit
) {
    NetFixActionSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = Icons.Default.Info,
        title = "Сетевой ассистент",
        subtitle = "На Xiaomi, Poco и Realme функция «Использовать мобильный интернет для ускорения» может мешать обходу, пока включён Wi-Fi. Выключите её: Настройки Android → Wi-Fi → Сетевой ассистент.",
        options = listOf(
            NetFixSheetOption(label = "Открыть настройки Wi-Fi") { onOpenWifiSettings() },
            NetFixSheetOption(label = "Больше не показывать") { onHideForever() }
        ),
        footnote = "Мы не можем проверить, включена ли эта функция на вашем телефоне"
    )
}
