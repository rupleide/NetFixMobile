package com.rupleide.netfix.ui.components

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rupleide.netfix.ui.main.rememberAnimationsEnabled

val LocalIsTv = compositionLocalOf { false }

const val TvTargetWidthDp = 960f

const val TvTargetHeightDp = 540f

fun Context.hasLeanbackFeature(): Boolean =
    packageManager.hasSystemFeature("android.software.leanback")

fun SharedPreferences.isTvLayoutEnabled(context: Context, onboardingCompleted: Boolean): Boolean {
    if (contains("is_smart_tv")) return getBoolean("is_smart_tv", false)
    return !onboardingCompleted && context.hasLeanbackFeature()
}

@Composable
fun ProvideTvEnvironment(enabled: Boolean, content: @Composable () -> Unit) {
    val baseDensity = LocalDensity.current
    val baseConfiguration = LocalConfiguration.current
    val factor = if (enabled) {
        minOf(baseConfiguration.screenWidthDp / TvTargetWidthDp, baseConfiguration.screenHeightDp / TvTargetHeightDp).coerceIn(0.6f, 2f)
    } else {
        1f
    }
    val density = remember(baseDensity, factor, enabled) {
        if (enabled) {
            Density(density = baseDensity.density * factor, fontScale = baseDensity.fontScale)
        } else {
            baseDensity
        }
    }
    val configuration = remember(baseConfiguration, factor, enabled) {
        if (enabled) {
            Configuration(baseConfiguration).apply {
                screenWidthDp = (baseConfiguration.screenWidthDp / factor).toInt()
                screenHeightDp = (baseConfiguration.screenHeightDp / factor).toInt()
                smallestScreenWidthDp = (baseConfiguration.smallestScreenWidthDp / factor).toInt()
                densityDpi = (baseConfiguration.densityDpi * factor).toInt()
            }
        } else {
            baseConfiguration
        }
    }
    CompositionLocalProvider(
        LocalIsTv provides enabled,
        LocalDensity provides density,
        LocalConfiguration provides configuration,
        content = content
    )
}

@Composable
fun isImeShown(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}

@Composable
fun Modifier.nfTvFocus(
    corner: Dp = 14.dp,
    grow: Float = 1.04f,
    ring: Boolean = true,
    wide: Boolean = false,
    inside: Boolean = false,
    gapColor: Color = Nf.Bg
): Modifier {
    if (!LocalIsTv.current) return this
    val animationsEnabled = rememberAnimationsEnabled()
    var focused by remember { mutableStateOf(false) }
    val target = when {
        !focused || !animationsEnabled || inside -> 1f
        wide -> 1.02f
        else -> grow
    }
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(if (animationsEnabled) 140 else 0, easing = FastOutSlowInEasing),
        label = "nfTvFocusScale"
    )
    val glow by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (animationsEnabled) 140 else 0),
        label = "nfTvFocusGlow"
    )
    return this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (ring && glow > 0f && inside) {
                val width = 2.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.07f * glow),
                    cornerRadius = CornerRadius(corner.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = glow),
                    topLeft = Offset(width / 2f, width / 2f),
                    size = Size(size.width - width, size.height - width),
                    cornerRadius = CornerRadius(corner.toPx() - width / 2f),
                    style = Stroke(width = width)
                )
            } else if (ring && glow > 0f) {
                val width = 2.dp.toPx()
                val offset = width / 2f + 1.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = glow),
                    topLeft = Offset(-offset, -offset),
                    size = Size(size.width + offset * 2f, size.height + offset * 2f),
                    cornerRadius = CornerRadius(corner.toPx() + offset),
                    style = Stroke(width = width)
                )
            }
        }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.nfTvKeyboardOnCenter(): Modifier {
    if (!LocalIsTv.current) return this
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeShown = isImeShown()
    return this.onPreviewKeyEvent { event ->
        val isSelect = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
        if (isSelect && event.type == KeyEventType.KeyUp) {
            keyboard?.show()
        }
        val direction = when (event.key) {
            Key.DirectionUp -> FocusDirection.Up
            Key.DirectionDown -> FocusDirection.Down
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            else -> null
        }
        if (!imeShown && direction != null) {
            if (event.type == KeyEventType.KeyDown) focusManager.moveFocus(direction)
            true
        } else {
            false
        }
    }
}

@Composable
fun Modifier.nfTvMenuKey(onMenu: () -> Unit): Modifier {
    if (!LocalIsTv.current) return this
    return this.onPreviewKeyEvent { event ->
        if (event.key == Key.Menu && event.type == KeyEventType.KeyUp) {
            onMenu()
            true
        } else {
            false
        }
    }
}

@Composable
fun navBarReserve(): Dp = if (LocalIsTv.current) 0.dp else 86.dp
