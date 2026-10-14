package com.rupleide.netfix.ui.main

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rupleide.netfix.ui.globe.GlobeStatus
import kotlinx.coroutines.delay

object MainMotion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val SymmetricSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
    const val STATE_MS = 350
    const val ENTER_MS = 250
    const val EXIT_MS = 200
    const val CARD_ENTER_MS = 300
    const val PULSE_MS = 1200
    const val STAGGER_MS = 50L
    const val STATUS_DEBOUNCE_MS = 180L
}

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val systemAnimationsEnabled = remember(configuration) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    return systemAnimationsEnabled && !com.rupleide.netfix.data.performanceModeGlobal
}

fun <T> motionTween(animationsEnabled: Boolean, durationMs: Int, easing: Easing): FiniteAnimationSpec<T> =
    if (animationsEnabled) tween(durationMs, easing = easing) else snap()

fun <T> snappySpring(animationsEnabled: Boolean): FiniteAnimationSpec<T> =
    if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh) else snap()

fun <T> premiumSpring(animationsEnabled: Boolean): FiniteAnimationSpec<T> =
    if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else snap()

private const val LOW_END_MEMORY_CLASS_MB = 192
private const val LOW_END_CPU_CORES = 4

fun android.content.Context.isLowEndDeviceNow(): Boolean {
    val activityManager = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    return activityManager.isLowRamDevice ||
        activityManager.memoryClass <= LOW_END_MEMORY_CLASS_MB ||
        Runtime.getRuntime().availableProcessors() <= LOW_END_CPU_CORES
}

const val LITE_ANIMATIONS_KEY = "lite_animations"

fun android.content.SharedPreferences.liteAnimationsEnabled(context: android.content.Context): Boolean =
    if (contains(LITE_ANIMATIONS_KEY)) getBoolean(LITE_ANIMATIONS_KEY, false) else context.isLowEndDeviceNow()

@Composable
fun rememberIsLowEndDevice(): Boolean {
    val context = LocalContext.current
    return remember { context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE).liteAnimationsEnabled(context) }
}

@Composable
fun rememberDebouncedGlobeStatus(status: GlobeStatus): GlobeStatus {
    var shownStatus by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (status != shownStatus) {
            delay(MainMotion.STATUS_DEBOUNCE_MS)
            shownStatus = status
        }
    }
    return shownStatus
}

@Composable
fun rememberStaggeredEntrance(delayMs: Long, animationsEnabled: Boolean): State<Float> {
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            delay(delayMs)
            progress.animateTo(1f, tween(MainMotion.CARD_ENTER_MS, easing = MainMotion.Decelerate))
        }
    }
    return progress.asState()
}

@Composable
fun rememberIsUiActive(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return resumed && LocalWindowInfo.current.isWindowFocused
}
