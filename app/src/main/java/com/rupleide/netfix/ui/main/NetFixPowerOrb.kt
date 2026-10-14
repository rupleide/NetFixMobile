package com.rupleide.netfix.ui.main

import com.rupleide.netfix.ui.components.nfTvMenuKey
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.performNetFixHaptic
import com.rupleide.netfix.ui.globe.GlobeStatus
import com.rupleide.netfix.ui.theme.NetFixFonts
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val OrbBorderWidth = 2.5.dp
private val GlowSpread = 16.dp
private const val ICON_SIZE_RATIO = 0.31f
private const val LINEAR_GRADIENT_DEGREES = 160.0
private const val INITIAL_SETTLE_MS = 1_500L

private val BrightStart = Color(0xFF26262B)
private val BrightMiddle = Color(0xFF1A1A1E)
private val BrightEnd = Color(0xFF111113)

private fun orbTitle(status: GlobeStatus, telegramOnly: Boolean) = when (status) {
    GlobeStatus.Off -> "Включить"
    GlobeStatus.Connecting -> "Подключаем"
    GlobeStatus.Connected -> if (telegramOnly) "Только Telegram" else "Подключено"
    GlobeStatus.Error -> "Повторить"
}

private fun orbSubtitle(status: GlobeStatus) = when (status) {
    GlobeStatus.Off -> "нажмите, чтобы начать"
    GlobeStatus.Connecting -> "подбираем стратегию"
    GlobeStatus.Connected -> ""
    GlobeStatus.Error -> "не удалось запустить"
}

private fun orbContentDescription(status: GlobeStatus) = when (status) {
    GlobeStatus.Off -> "Включить обход блокировок"
    GlobeStatus.Connecting -> "Отменить подключение"
    GlobeStatus.Connected -> "Выключить обход блокировок"
    GlobeStatus.Error -> "Повторить подключение"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetFixPowerOrb(
    status: GlobeStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    orbSize: androidx.compose.ui.unit.Dp = 196.dp,
    telegramOnly: Boolean = false
) {
    val tvMode = com.rupleide.netfix.ui.components.LocalIsTv.current
    val animationsEnabled = rememberAnimationsEnabled()
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = snappySpring(animationsEnabled),
        label = "orbPress"
    )

    val transition = updateTransition(status, label = "orb")
    val colorSpec = motionTween<Color>(animationsEnabled, MainMotion.STATE_MS, MainMotion.Standard)
    val floatSpec = motionTween<Float>(animationsEnabled, MainMotion.STATE_MS, MainMotion.Standard)

    val darkCenter by transition.animateColor({ colorSpec }, label = "darkCenter") {
        when (it) {
            GlobeStatus.Off -> Color(0xFF1C1C20)
            GlobeStatus.Connecting, GlobeStatus.Connected -> Color(0xFF1F1F24)
            GlobeStatus.Error -> Color(0xFF221A1A)
        }
    }
    val darkEdge by transition.animateColor({ colorSpec }, label = "darkEdge") {
        when (it) {
            GlobeStatus.Off -> Color(0xFF0E0E10)
            GlobeStatus.Connecting, GlobeStatus.Connected -> Color(0xFF0F0F12)
            GlobeStatus.Error -> Color(0xFF120E0E)
        }
    }
    val darkEdgeStop by transition.animateFloat({ floatSpec }, label = "darkEdgeStop") {
        if (it == GlobeStatus.Connecting || it == GlobeStatus.Connected) 0.70f else 0.68f
    }
    val brightAlpha by transition.animateFloat({ floatSpec }, label = "brightAlpha") {
        if (it == GlobeStatus.Connected) 1f else 0f
    }
    val borderColor by transition.animateColor({ colorSpec }, label = "border") {
        when (it) {
            GlobeStatus.Off -> Color.White.copy(alpha = 0.12f)
            GlobeStatus.Connecting -> Color(0xFF3B82F6).copy(alpha = 0.75f)
            GlobeStatus.Connected -> Color(0xFF22C55E).copy(alpha = 0.6f)
            GlobeStatus.Error -> Color(0xFFEF4444).copy(alpha = 0.6f)
        }
    }
    val iconColor by transition.animateColor({ colorSpec }, label = "icon") {
        when (it) {
            GlobeStatus.Off -> Color(0xFFA1A1AA)
            GlobeStatus.Connecting -> Color(0xFFF4F4F5)
            GlobeStatus.Connected -> Color(0xFF22C55E)
            GlobeStatus.Error -> Color(0xFFF87171)
        }
    }
    val subtitleColor by transition.animateColor({ colorSpec }, label = "subtitle") {
        when (it) {
            GlobeStatus.Off -> Color(0xFF8A8A90)
            GlobeStatus.Connecting -> Color(0xFFA1A1AA)
            GlobeStatus.Connected -> Color(0xFFD4D4D8)
            GlobeStatus.Error -> Color(0xFFC9A9A9)
        }
    }
    val glowColor by transition.animateColor({ colorSpec }, label = "glow") {
        when (it) {
            GlobeStatus.Off -> Color(0xFFD4D4D8)
            GlobeStatus.Connecting -> Color(0xFF3B82F6)
            GlobeStatus.Connected -> Color(0xFF22C55E)
            GlobeStatus.Error -> Color(0xFFEF4444)
        }
    }

    val iconScale = remember { Animatable(1f) }
    var previousStatus by remember { mutableStateOf(status) }
    val composedAtMs = remember { android.os.SystemClock.uptimeMillis() }
    LaunchedEffect(status) {
        val becameConnected = status == GlobeStatus.Connected && previousStatus != GlobeStatus.Connected
        previousStatus = status
        val isInitialSettle = android.os.SystemClock.uptimeMillis() - composedAtMs < INITIAL_SETTLE_MS
        if (becameConnected && !isInitialSettle) {
            view.performNetFixHaptic(NetFixHaptic.Confirm)
            if (animationsEnabled) {
                iconScale.animateTo(0.85f, tween(120, easing = MainMotion.Accelerate))
                iconScale.animateTo(1f, premiumSpring(animationsEnabled))
            }
        }
    }

    Box(
        modifier = modifier
            .size(orbSize)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                val glowRadius = size.minDimension / 2f + GlowSpread.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.4f), Color.Transparent),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )
            }
            .focusRequester(focusRequester)
            .focusProperties { if (!tvMode) down = navBarFocusRequester }
            .nfTvMenuKey(onLongClick)
            .clip(CircleShape)
            .semantics { contentDescription = orbContentDescription(status) }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .drawBehind {
                val orbRadius = size.minDimension / 2f
                val radialCenter = Offset(size.width * 0.5f, size.height * 0.3f)
                val radialRadius = size.minDimension * 0.86f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to darkCenter,
                        darkEdgeStop to darkEdge,
                        1f to darkEdge,
                        center = radialCenter,
                        radius = radialRadius
                    ),
                    radius = orbRadius
                )
                if (brightAlpha > 0.001f) {
                    val angleRadians = Math.toRadians(LINEAR_GRADIENT_DEGREES)
                    val directionX = sin(angleRadians).toFloat()
                    val directionY = -cos(angleRadians).toFloat()
                    val halfLength = (kotlin.math.abs(size.width * directionX) + kotlin.math.abs(size.height * directionY)) / 2f
                    drawCircle(
                        brush = Brush.linearGradient(
                            0f to BrightStart,
                            0.52f to BrightMiddle,
                            1f to BrightEnd,
                            start = Offset(center.x - directionX * halfLength, center.y - directionY * halfLength),
                            end = Offset(center.x + directionX * halfLength, center.y + directionY * halfLength)
                        ),
                        radius = orbRadius,
                        alpha = brightAlpha
                    )
                    val highlightInset = OrbBorderWidth.toPx() + 1.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.12f * brightAlpha),
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(highlightInset, highlightInset),
                        size = Size(size.width - highlightInset * 2f, size.height - highlightInset * 2f),
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                val borderWidthPx = OrbBorderWidth.toPx()
                drawCircle(
                    color = if (isFocused) Color.White.copy(alpha = 0.9f) else borderColor,
                    radius = orbRadius - borderWidthPx / 2f,
                    style = Stroke(width = if (isFocused) borderWidthPx * 1.6f else borderWidthPx)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.rupleide.netfix.R.drawable.ic_power),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(orbSize * ICON_SIZE_RATIO)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
            )

            Spacer(modifier = Modifier.height(orbSize * 0.04f))

            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    (fadeIn(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) +
                        slideInVertically(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) { it / 3 })
                        .togetherWith(
                            fadeOut(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) +
                                slideOutVertically(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) { -it / 3 }
                        )
                        .using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                label = "orbTitle"
            ) { shownStatus ->
                Text(
                    text = orbTitle(shownStatus, telegramOnly),
                    color = Color(0xFFF4F4F5),
                    fontFamily = NetFixFonts.Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    (fadeIn(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) +
                        slideInVertically(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) { it / 3 })
                        .togetherWith(
                            fadeOut(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) +
                                slideOutVertically(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) { -it / 3 }
                        )
                        .using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                label = "orbSubtitle"
            ) { shownStatus ->
                if (shownStatus == GlobeStatus.Connected) {
                    ConnectionTimerText(color = subtitleColor)
                } else {
                    Text(
                        text = orbSubtitle(shownStatus),
                        color = subtitleColor,
                        fontFamily = NetFixFonts.Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionTimerText(color: Color) {
    val elapsedLabel by produceState(initialValue = formatConnectionElapsed()) {
        while (true) {
            value = formatConnectionElapsed()
            delay(1_000)
        }
    }
    Text(
        text = elapsedLabel,
        color = color,
        fontFamily = NetFixFonts.JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
}

private fun formatConnectionElapsed(): String {
    val startedAt = com.rupleide.netfix.data.connectionStartTime
    val totalSeconds = if (startedAt == 0L) 0L else ((System.currentTimeMillis() - startedAt) / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
