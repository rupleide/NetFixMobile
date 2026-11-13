package com.rupleide.netfix.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.rememberNetFixHaptics

@Composable
fun HelpFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "helpFabScale")
    val sizeSpring = spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    Row(
        modifier = modifier
            .scale(scale)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(if (pressed) Color(0xFF383838) else Color(0xFF242424))
            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptics(NetFixHaptic.Tap)
                onClick()
            }
            .semantics { contentDescription = "Помощь" }
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.QuestionMark, contentDescription = null, tint = Color(0xFFF4F4F5), modifier = Modifier.size(24.dp))
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(sizeSpring, expandFrom = Alignment.Start) + fadeIn(tween(220, delayMillis = 60)),
            exit = shrinkHorizontally(sizeSpring, shrinkTowards = Alignment.Start) + fadeOut(tween(120))
        ) {
            Text(
                text = "Помощь",
                color = Color(0xFFF4F4F5),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(end = 20.dp)
            )
        }
    }
}
