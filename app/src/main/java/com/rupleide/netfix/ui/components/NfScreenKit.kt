package com.rupleide.netfix.ui.components

import androidx.compose.ui.focus.focusProperties
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.ui.main.rememberAnimationsEnabled

private val EnterEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)

@Composable
fun Modifier.nfEnter(index: Int): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = if (index > 5) 0 else 60 + index * 40,
                    easing = EnterEasing
                )
            )
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24f * density
    }
}

@Composable
fun BoxScope.NfCornerAura(color: Color) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 160.dp, y = (-160).dp)
            .size(360.dp)
            .nfAura(color)
    )
}

@Composable
fun NfScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Nf.Text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 4.dp, end = 4.dp)
    )
}

val LocalNfHideSectionTitle = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun NfSection(
    title: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = Nf.TextSub,
    borderColor: Color = Nf.Line,
    note: String? = null,
    cardPadding: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null && !LocalNfHideSectionTitle.current) {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .height(20.dp)
                    .padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Nf.Glass)
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(cardPadding),
            content = content
        )
        if (note != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note,
                color = Nf.TextSub,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun NfDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 10.dp)
            .height(1.dp)
            .background(Nf.LineSoft)
    )
}

@Composable
fun NfRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    iconTint: Color = Nf.Text,
    iconBackground: Color = Color(0x0FFFFFFF),
    titleColor: Color = Nf.Text,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    iconModifier: Modifier = Modifier
) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val background by animateColorAsState(
        targetValue = if (onClick != null && enabled && (pressed || focused)) Color(0x14FFFFFF) else Color.Transparent,
        animationSpec = tween(150),
        label = "nfRowBg"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (LocalIsTv.current) 56.dp else 64.dp)
            .then(if (onClick != null && enabled) Modifier.nfTvFocus(corner = 14.dp, inside = true) else Modifier)
            .clip(shape)
            .background(background)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) {
                    haptics(NetFixHaptic.Tap)
                    onClick()
                } else Modifier
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) leading()
        if (icon != null || iconPainter != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = iconModifier.size(20.dp))
                } else if (iconPainter != null) {
                    Icon(painter = iconPainter, contentDescription = null, tint = iconTint, modifier = iconModifier.size(20.dp))
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Nf.TextSub,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun NfSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    switchModifier: Modifier = Modifier
) {
    NfRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconPainter = iconPainter,
        enabled = enabled,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            NfSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (LocalIsTv.current) switchModifier.focusProperties { canFocus = false } else switchModifier
            )
        }
    )
}

@Composable
fun NfValueTrailing(value: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (value != null) {
            Text(
                text = value,
                color = Nf.TextSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 130.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Nf.TextSub,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun NfMiniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NfButtonStyle = NfButtonStyle.Secondary,
    enabled: Boolean = true,
    width: Dp = 96.dp
) {
    NfButton(
        text = text,
        onClick = onClick,
        style = style,
        enabled = enabled,
        small = true,
        modifier = modifier.width(width)
    )
}

@Composable
fun NfSoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NfButtonStyle = NfButtonStyle.Soft,
    enabled: Boolean = true
) {
    NfButton(text = text, onClick = onClick, modifier = modifier, style = style, enabled = enabled)
}

@Composable
fun NfStatusDot(color: Color, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun NfStatusCard(
    dotColor: Color,
    title: String,
    rightText: String?,
    rightColor: Color,
    line: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Nf.Line,
    slot: @Composable () -> Unit,
    buttons: @Composable () -> Unit
) {
    val animatedBorder by animateColorAsState(targetValue = borderColor, animationSpec = tween(300), label = "nfStatusBorder")
    val animatedDot by animateColorAsState(targetValue = dotColor, animationSpec = tween(300), label = "nfStatusDot")
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, animatedBorder, shape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NfStatusDot(animatedDot)
            Text(
                text = title,
                color = Nf.Text,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (rightText != null) {
                Text(
                    text = rightText,
                    color = rightColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = line,
                color = Nf.Text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(modifier = Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.CenterStart) {
            slot()
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
            buttons()
        }
    }
}

@Composable
fun NfSlotText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun NfFieldBlock(
    label: String,
    modifier: Modifier = Modifier,
    help: String? = null,
    isError: Boolean = false,
    field: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = if (isError) Nf.RedText else Nf.TextSub,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        field()
        if (help != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = help,
                color = if (isError) Nf.RedText else Nf.TextSub,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun Modifier.nfHeartbeat(): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    if (!animationsEnabled) return this
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "nfHeartbeat")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 1800
                1f at 0
                1.12f at 150
                1f at 300
                1.08f at 440
                1f at 600
                1f at 1800
            }
        ),
        label = "nfHeartbeatScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberStagedLevel(levels: Int): Int {
    var level by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        for (step in 1..levels) {
            androidx.compose.runtime.withFrameNanos { }
            androidx.compose.runtime.withFrameNanos { }
            level = step
        }
    }
    return level
}
