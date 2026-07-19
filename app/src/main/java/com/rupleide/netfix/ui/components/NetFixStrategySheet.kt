package com.rupleide.netfix.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

private val YtDecelerateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
private val YtAccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val YtOvershootEasing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f)
private val YtIconEasing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1.0f)
private val YtPressSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

private val SheetBg = Color(0xFF1E1E1E)
private val SheetCard = Color(0xFF262626)
private val SheetText = Color(0xFFF4F4F5)
private val SheetSubtext = Color(0xFFA1A1AA)
private val SheetClose = Color(0xFFC4C4C6)
private val SheetDestructive = Color(0xFFEF4444)

data class StrategyAction(
    val label: String,
    val subtitle: String = "",
    val destructive: Boolean = false,
    val bold: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun NetFixStrategySheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String = "",
    iconRes: Int,
    actions: List<StrategyAction>,
    closeLabel: String = "Отмена"
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", 0) }
    val isTv = remember(sharedPrefs) { sharedPrefs.getBoolean("is_smart_tv", false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val useTvLayout = isTv || isLandscape

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val frozenNavBarPadding = remember { navBarPadding }
    val transition = updateTransition(targetState = visible, label = "StrategySheetTransition")

    val focusRequesters = remember(actions) { List(actions.size) { FocusRequester() } }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(150)
            try {
                focusRequesters.firstOrNull()?.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (transition.currentState || transition.targetState) {
        BackHandler(onBack = onDismissRequest)

        val scrimAlpha by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, easing = YtDecelerateEasing) else tween(420, easing = YtAccelerateEasing) },
            label = "scrim"
        ) { if (it) 0.64f else 0f }

        val cardProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(360, easing = YtOvershootEasing) else tween(300, delayMillis = 120, easing = YtAccelerateEasing) },
            label = "card"
        ) { if (it) 1f else 0f }

        val iconScale by transition.animateFloat(
            transitionSpec = { if (targetState) tween(320, delayMillis = 100, easing = YtIconEasing) else tween(200, delayMillis = 80, easing = YtAccelerateEasing) },
            label = "iconScale"
        ) { if (it) 1f else 0f }

        val iconRotation by transition.animateFloat(
            transitionSpec = { if (targetState) tween(320, delayMillis = 100, easing = YtIconEasing) else tween(200, delayMillis = 80, easing = YtAccelerateEasing) },
            label = "iconRotation"
        ) { if (it) 0f else -25f }

        val headerProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, delayMillis = 60, easing = YtDecelerateEasing) else tween(200, delayMillis = 80, easing = YtAccelerateEasing) },
            label = "header"
        ) { if (it) 1f else 0f }

        val actionsProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, delayMillis = 100, easing = YtDecelerateEasing) else tween(200, delayMillis = 40, easing = YtAccelerateEasing) },
            label = "actions"
        ) { if (it) 1f else 0f }

        val closeProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, delayMillis = 140, easing = YtDecelerateEasing) else tween(200, easing = YtAccelerateEasing) },
            label = "close"
        ) { if (it) 1f else 0f }

        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val floatY by infiniteTransition.animateFloat(
            initialValue = -3f, targetValue = 3f,
            animationSpec = infiniteRepeatable(animation = tween(1500, easing = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)), repeatMode = RepeatMode.Reverse),
            label = "floatY"
        )
        val sway by infiniteTransition.animateFloat(
            initialValue = -6f, targetValue = 6f,
            animationSpec = infiniteRepeatable(animation = tween(2000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), repeatMode = RepeatMode.Reverse),
            label = "sway"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(onDismissRequest) { detectTapGestures(onTap = { onDismissRequest() }) },
            contentAlignment = if (useTvLayout) Alignment.Center else Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(frozenNavBarPadding)
                    .padding(bottom = if (useTvLayout) 0.dp else 16.dp)
                    .widthIn(max = if (useTvLayout) 640.dp else 500.dp)
                    .graphicsLayer {
                        alpha = cardProgress
                        val s = 0.88f + 0.12f * cardProgress
                        scaleX = s; scaleY = s
                        if (!useTvLayout) {
                            translationY = (1f - cardProgress) * 80f
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures { } }
            ) {
                if (useTvLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SheetBg, RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { alpha = headerProgress },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0x1AFFFFFF), CircleShape)
                                    .padding(2.dp)
                                    .background(SheetBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = SheetText,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = iconScale; scaleY = iconScale
                                            translationY = floatY * iconScale
                                            rotationZ = iconRotation + (sway * iconScale)
                                        }
                                        .size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(text = title, color = SheetText, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            if (subtitle.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(text = subtitle, color = SheetSubtext, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .graphicsLayer { alpha = actionsProgress },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            actions.forEachIndexed { index, action ->
                                StrategyActionButton(
                                    label = action.label,
                                    subtitle = action.subtitle,
                                    destructive = action.destructive,
                                    bold = action.bold,
                                    focusRequester = focusRequesters[index],
                                    onClick = action.onClick
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            StrategyCloseButton(
                                label = closeLabel,
                                focusRequester = closeFocusRequester,
                                onClick = onDismissRequest
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SheetBg, RoundedCornerShape(28.dp))
                                .padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .background(Color(0x1AFFFFFF), CircleShape)
                                    .padding(2.dp)
                                    .background(SheetBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = SheetText,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = iconScale; scaleY = iconScale
                                            translationY = floatY * iconScale
                                            rotationZ = iconRotation + (sway * iconScale)
                                        }
                                        .size(34.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Column(
                                modifier = Modifier.graphicsLayer { alpha = headerProgress },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = title, color = SheetText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                if (subtitle.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(text = subtitle, color = SheetSubtext, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = actionsProgress; translationY = (1f - actionsProgress) * 16f },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                actions.forEachIndexed { index, action ->
                                    StrategyActionButton(
                                        label = action.label,
                                        subtitle = action.subtitle,
                                        destructive = action.destructive,
                                        bold = action.bold,
                                        focusRequester = focusRequesters[index],
                                        onClick = action.onClick
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(modifier = Modifier.graphicsLayer { alpha = closeProgress; translationY = (1f - closeProgress) * 24f }) {
                            StrategyCloseButton(label = closeLabel, focusRequester = closeFocusRequester, onClick = onDismissRequest)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyActionButton(
    label: String,
    subtitle: String,
    destructive: Boolean,
    bold: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val finalActive = isPressed || isFocused

    val pressScale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = YtPressSpring, label = "press")
    val bgColor by animateColorAsState(targetValue = if (finalActive) Color(0xFF38383A) else SheetCard, animationSpec = tween(150), label = "bg")
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "border"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                val isDpadClick = keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter
                if (isDpadClick && keyEvent.type == KeyEventType.KeyUp) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                    true
                } else {
                    false
                }
            }
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .background(borderColor, RoundedCornerShape(12.dp))
            .padding(1.dp)
            .background(bgColor, RoundedCornerShape(11.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (destructive) SheetDestructive else SheetText,
                fontSize = 14.sp,
                fontWeight = if (bold || finalActive) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SheetSubtext,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StrategyCloseButton(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val finalActive = isPressed || isFocused

    val pressScale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = YtPressSpring, label = "cPress")
    val bgColor by animateColorAsState(targetValue = if (finalActive) Color(0xFF38383A) else SheetCard, animationSpec = tween(150), label = "cBg")
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "cBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                val isDpadClick = keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter
                if (isDpadClick && keyEvent.type == KeyEventType.KeyUp) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                    true
                } else {
                    false
                }
            }
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            })
            .background(borderColor, RoundedCornerShape(20.dp))
            .padding(1.dp)
            .background(bgColor, RoundedCornerShape(19.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (finalActive) Color.White else SheetClose, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
