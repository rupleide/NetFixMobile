package com.rupleide.netfix.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

@Composable
fun rememberEntranceProgress(
    play: Boolean,
    delayMillis: Long = 0L,
    dampingRatio: Float = 0.85f,
    stiffness: Float = 320f
): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (play) {
            delay(delayMillis)
            progress.animateTo(1f, spring(dampingRatio = dampingRatio, stiffness = stiffness))
        }
    }
    return progress.value
}
