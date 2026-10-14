package com.rupleide.netfix.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class NetFixHaptic { Tap, ToggleOn, ToggleOff, Confirm, Reject, LongPress }

fun View.performNetFixHaptic(haptic: NetFixHaptic) {
    val feedbackConstant = when (haptic) {
        NetFixHaptic.Tap -> HapticFeedbackConstants.VIRTUAL_KEY
        NetFixHaptic.ToggleOn ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_ON
            else HapticFeedbackConstants.CLOCK_TICK
        NetFixHaptic.ToggleOff ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_OFF
            else HapticFeedbackConstants.CLOCK_TICK
        NetFixHaptic.Confirm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
        NetFixHaptic.Reject ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
        NetFixHaptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(feedbackConstant)
}

@Composable
fun rememberNetFixHaptics(): (NetFixHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) { { haptic: NetFixHaptic -> view.performNetFixHaptic(haptic) } }
}
