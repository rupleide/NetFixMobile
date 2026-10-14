package com.rupleide.netfix.ui.main

import com.rupleide.netfix.ui.components.nfTvFocus
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.ui.globe.GlobeStatus
import com.rupleide.netfix.ui.theme.NetFixFonts
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val GlassFillColor = Color(0xFF1E1E1E).copy(alpha = 0.85f)
val GlassBorderColor = Color(0x1AFFFFFF)
val GlassCornerShape = RoundedCornerShape(20.dp)
private val ScreenBackground = Color(0xFF161616)
private val PrimaryText = Color(0xFFF4F4F5)
private val SecondaryText = Color(0xFFA1A1AA)
private val MeasuringColor = Color(0xFF3B82F6)

fun Modifier.netFixGlass(hazeState: HazeState?, shape: Shape = GlassCornerShape): Modifier {
    val clipped = this.clip(shape)
    val filled = if (hazeState != null) {
        clipped.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = ScreenBackground,
                tint = HazeTint(GlassFillColor),
                blurRadius = 24.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(GlassFillColor)
            )
        )
    } else {
        clipped.background(GlassFillColor)
    }
    return filled.border(1.dp, GlassBorderColor, shape)
}

private data class LatencyReading(val text: String, val numericMs: Int?)

private fun latencyReading(status: GlobeStatus, latencyMs: Int): LatencyReading = when (status) {
    GlobeStatus.Off, GlobeStatus.Error -> LatencyReading("- мс", null)
    GlobeStatus.Connecting -> LatencyReading("… мс", null)
    GlobeStatus.Connected -> if (latencyMs >= 0) LatencyReading("$latencyMs мс", latencyMs) else LatencyReading("… мс", null)
}

private fun latencyValueColor(status: GlobeStatus, latencyMs: Int): Color = when (status) {
    GlobeStatus.Off, GlobeStatus.Error -> Color(0xFF6B6B70)
    GlobeStatus.Connecting -> MeasuringColor
    GlobeStatus.Connected -> if (latencyMs >= 0) PrimaryText else MeasuringColor
}

private fun latencyQuality(status: GlobeStatus, latencyMs: Int): Pair<String, Color> = when (status) {
    GlobeStatus.Off, GlobeStatus.Error -> "Нет соединения" to Color(0xFFEF4444)
    GlobeStatus.Connecting -> "Измеряем" to MeasuringColor
    GlobeStatus.Connected -> when {
        latencyMs < 0 -> "Измеряем" to MeasuringColor
        latencyMs < 80 -> "Отличная" to Color(0xFF22C55E)
        latencyMs <= 200 -> "Нормальная" to Color(0xFFEAB308)
        else -> "Высокая" to Color(0xFFEF4444)
    }
}

@Composable
fun LatencyStrategyCard(
    status: GlobeStatus,
    latencyMs: Int,
    strategyName: String,
    onStrategyClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    networkName: String? = null
) {
    val animationsEnabled = rememberAnimationsEnabled()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .netFixGlass(hazeState)
    ) {
        LatencyColumn(
            status = status,
            latencyMs = latencyMs,
            animationsEnabled = animationsEnabled,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(GlassBorderColor)
        )
        StrategyColumn(
            strategyName = strategyName,
            networkName = networkName,
            onClick = onStrategyClick,
            animationsEnabled = animationsEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = SecondaryText,
        fontFamily = NetFixFonts.Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp
    )
}

@Composable
private fun LatencyColumn(
    status: GlobeStatus,
    latencyMs: Int,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val reading = latencyReading(status, latencyMs)
    val valueColor by animateColorAsState(
        targetValue = latencyValueColor(status, latencyMs),
        animationSpec = motionTween(animationsEnabled, MainMotion.STATE_MS, MainMotion.Standard),
        label = "latencyValueColor"
    )
    val (qualityText, qualityTarget) = latencyQuality(status, latencyMs)
    val qualityColor by animateColorAsState(
        targetValue = qualityTarget,
        animationSpec = motionTween(animationsEnabled, MainMotion.STATE_MS, MainMotion.Standard),
        label = "latencyQualityColor"
    )

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CardLabel("Задержка")
        AnimatedContent(
            targetState = reading,
            transitionSpec = {
                val previousMs = initialState.numericMs
                val nextMs = targetState.numericMs
                val pingIncreased = previousMs != null && nextMs != null && nextMs > previousMs
                val direction = if (pingIncreased) -1 else 1
                (fadeIn(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) +
                    slideInVertically(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) { -direction * it / 2 })
                    .togetherWith(
                        fadeOut(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) +
                            slideOutVertically(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) { direction * it / 2 }
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "latencyValue"
        ) { shownReading ->
            Text(
                text = shownReading.text,
                color = valueColor,
                fontFamily = NetFixFonts.JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(qualityColor)
            )
            Text(
                text = qualityText,
                color = qualityColor,
                fontFamily = NetFixFonts.Manrope,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StrategyColumn(
    strategyName: String,
    networkName: String?,
    onClick: () -> Unit,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptics = com.rupleide.netfix.ui.components.rememberNetFixHaptics()
    val rightHalfShape = remember { RoundedCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 0.dp) }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .nfTvFocus(corner = 16.dp, grow = 1.03f)
            .clip(rightHalfShape)
            .clickable(
                interactionSource = null,
                indication = ripple(),
                onClickLabel = "Сменить профиль",
                onClick = {
                    haptics(com.rupleide.netfix.ui.components.NetFixHaptic.Tap)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CardLabel("Стратегия")
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = strategyName,
                transitionSpec = {
                    (fadeIn(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) +
                        slideInHorizontally(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) { it / 4 })
                        .togetherWith(
                            fadeOut(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) +
                                slideOutHorizontally(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)) { -it / 4 }
                        )
                },
                modifier = Modifier.weight(1f),
                label = "strategyName"
            ) { shownName ->
                Text(
                    text = shownName,
                    color = PrimaryText,
                    fontFamily = NetFixFonts.Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        var hintSize by remember(networkName) { mutableStateOf(13f) }
        Text(
            text = if (networkName != null) "$networkName · Сменить" else "Сменить профиль",
            color = SecondaryText,
            fontFamily = NetFixFonts.Manrope,
            fontWeight = FontWeight.Medium,
            fontSize = hintSize.sp,
            maxLines = 1,
            softWrap = false,
            onTextLayout = { layout ->
                if (layout.didOverflowWidth && hintSize > 9f) hintSize -= 0.5f
            }
        )
    }
}
