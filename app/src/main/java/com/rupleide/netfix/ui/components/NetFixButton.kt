package com.rupleide.netfix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NetFixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null,
    backgroundColor: Color = Color(0xFF242426),
    highlightedColor: Color = Color(0xFF38383A)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHighlighted = enabled && (isPressed || isHovered || isFocused)

    val currentBgColor by animateColorAsState(
        targetValue = if (enabled && isHighlighted) highlightedColor else backgroundColor,
        animationSpec = tween(150),
        label = "btnBg"
    )
    val currentBorderColor by animateColorAsState(
        targetValue = if (enabled && isHighlighted) Color.White else Color(0x1AFFFFFF),
        animationSpec = tween(150),
        label = "btnBorder"
    )
    val currentTextColor by animateColorAsState(
        targetValue = if (enabled && isHighlighted) Color.White else Color(0xFFF4F4F5),
        animationSpec = tween(150),
        label = "btnText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(currentBgColor)
            .border(1.dp, currentBorderColor, RoundedCornerShape(16.dp))
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
            }
            Text(
                text = text,
                color = currentTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}
