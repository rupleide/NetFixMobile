package com.rupleide.netfix.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R
import com.rupleide.netfix.core.whatsnew.WhatsNew
import com.rupleide.netfix.core.whatsnew.WhatsNewIds
import com.rupleide.netfix.core.whatsnew.WhatsNewItem
import com.rupleide.netfix.core.whatsnew.WhatsNewRelease
import com.rupleide.netfix.core.whatsnew.WhatsNewTarget
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfDivider
import com.rupleide.netfix.ui.components.currentVersionName
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.rememberNetFixHaptics
import kotlinx.coroutines.delay

private const val HIGHLIGHT_START_DELAY_MS = 500L
private const val HIGHLIGHT_BLINKS = 5
private const val HIGHLIGHT_HALF_BLINK_MS = 300

@Composable
private fun WhatsNewIcon(id: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0FFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        if (id == WhatsNewIds.TELEGRAM || id == WhatsNewIds.YOUTUBE_APP) {
            val drawable = if (id == WhatsNewIds.TELEGRAM) R.drawable.ic_telegram else R.drawable.ic_youtube
            Icon(painterResource(drawable), contentDescription = null, tint = Nf.Text, modifier = Modifier.size(20.dp))
        } else {
            val icon = when (id) {
                WhatsNewIds.HELP -> Icons.Default.QuestionMark
                WhatsNewIds.AI_APPS -> Icons.Default.SmartToy
                WhatsNewIds.SCAN -> Icons.Default.Speed
                WhatsNewIds.TV -> Icons.Default.Tv
                WhatsNewIds.TUNNEL_DNS -> Icons.Default.Dns
                WhatsNewIds.SECURE_DNS -> Icons.Default.Security
                WhatsNewIds.CF_SERVERS -> Icons.Default.Cloud
                WhatsNewIds.CLOUDFLARE -> Icons.Default.Cloud
                WhatsNewIds.SORTING -> Icons.AutoMirrored.Filled.Sort
                WhatsNewIds.COMPAT -> Icons.Default.History
                WhatsNewIds.DISCORD -> Icons.Default.Forum
                WhatsNewIds.ROOT -> Icons.Default.Security
                WhatsNewIds.BACKUP -> Icons.Default.SaveAlt
                WhatsNewIds.POLISH -> Icons.Default.AutoAwesome
                WhatsNewIds.BATTERY -> Icons.Default.BatteryChargingFull
                WhatsNewIds.VIDEO_START -> Icons.Default.PlayCircle
                else -> Icons.Default.Home
            }
            Icon(icon, contentDescription = null, tint = Nf.Text, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun WhatsNewContent(
    highlightedId: String?,
    onOpen: (WhatsNewItem) -> Unit,
    isTargetAvailable: (WhatsNewTarget) -> Boolean = { true }
) {
    val current = currentVersionName()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WhatsNew.releases.forEachIndexed { index, release ->
            ReleaseBlock(
                release = release,
                isCurrent = release.version == current || (index == 0 && WhatsNew.releaseFor(current) == null),
                highlightedId = highlightedId,
                onOpen = onOpen,
                isTargetAvailable = isTargetAvailable
            )
        }
    }
}

@Composable
private fun ReleaseBlock(
    release: WhatsNewRelease,
    isCurrent: Boolean,
    highlightedId: String?,
    onOpen: (WhatsNewItem) -> Unit,
    isTargetAvailable: (WhatsNewTarget) -> Boolean
) {
    val haptics = rememberNetFixHaptics()
    val containsHighlight = release.items.any { it.id == highlightedId }
    var expanded by remember(release.version) { mutableStateOf(isCurrent) }
    LaunchedEffect(containsHighlight) {
        if (containsHighlight) expanded = true
    }
    val chevronRotation by animateFloatAsState(if (expanded) 90f else 0f, tween(220), label = "releaseChevron")
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x08FFFFFF))
            .border(1.dp, Nf.Line, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .nfTvFocus(corner = 18.dp, inside = true)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    haptics(NetFixHaptic.Tap)
                    expanded = !expanded
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Версия ${release.version}", color = Nf.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Nf.Surface)
                        .border(1.dp, Nf.Line, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("текущая", color = Nf.TextSub, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(if (expanded) "Свернуть" else "${release.items.size} новых", color = Nf.TextSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = Nf.TextSub,
                modifier = Modifier.size(18.dp).rotate(chevronRotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(240)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                NfDivider()
                release.items.forEachIndexed { index, item ->
                    if (index > 0) NfDivider()
                    WhatsNewRow(
                        item = item,
                        highlighted = item.id == highlightedId,
                        canOpen = item.target != WhatsNewTarget.None && isTargetAvailable(item.target),
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WhatsNewRow(item: WhatsNewItem, highlighted: Boolean, canOpen: Boolean, onOpen: (WhatsNewItem) -> Unit) {
    val haptics = rememberNetFixHaptics()
    val shape = RoundedCornerShape(14.dp)
    val bringIntoView = remember { BringIntoViewRequester() }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(highlighted) {
        if (highlighted) {
            delay(HIGHLIGHT_START_DELAY_MS)
            bringIntoView.bringIntoView()
            repeat(HIGHLIGHT_BLINKS) {
                glow.animateTo(1f, tween(HIGHLIGHT_HALF_BLINK_MS))
                glow.animateTo(0.15f, tween(HIGHLIGHT_HALF_BLINK_MS))
            }
            glow.animateTo(0f, tween(HIGHLIGHT_HALF_BLINK_MS))
        } else {
            glow.animateTo(0f, tween(500))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f * glow.value))
            .border(1.5.dp, Color.White.copy(alpha = 0.85f * glow.value), shape)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WhatsNewIcon(item.id)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, color = Nf.Text, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(item.text, color = Nf.TextSub, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
            if (canOpen) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(32.dp)
                        .nfTvFocus(corner = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            haptics(NetFixHaptic.Tap)
                            onOpen(item)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(item.openLabel, color = Nf.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
