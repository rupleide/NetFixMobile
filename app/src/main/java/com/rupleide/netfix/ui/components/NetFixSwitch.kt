package com.rupleide.netfix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun NetFixSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHighlighted = enabled && (isPressed || isHovered || isFocused)

    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            if (enabled) Color(0xFF3B82F6) else Color(0xFF1D3E75)
        } else {
            if (enabled) Color(0xFF2E2E2E) else Color(0xFF1E1E1E)
        },
        animationSpec = tween(220),
        label = "track"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) Color.White else Color.Transparent,
        animationSpec = tween(150),
        label = "switchBorder"
    )

    val density = LocalDensity.current
    val maxTranslationPx = remember(density) { with(density) { 22.dp.toPx() } }

    val translationFactor by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(220),
        label = "thumb"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(13.dp)
            )
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onCheckedChange(!checked) }
                else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .graphicsLayer {
                    translationX = translationFactor * maxTranslationPx
                }
                .size(18.dp)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color(0xFF888888))
        )
    }
}
