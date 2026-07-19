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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R

private val RsSheetBackground = Color(0xFF1E1E1E)
private val RsButtonBackground = Color(0xFF262626)
private val RsCloseBackground = Color(0xFF1E1E1E)
private val RsCloseText = Color(0xFFC4C4C6)
private val RsButtonBorder = Color(0x1AFFFFFF)
private val RsTextPrimary = Color(0xFFF4F4F5)
private val RsTextSecondary = Color(0xFFA1A1AA)

private val RsDecelerateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
private val RsAccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val RsOvershootEasing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f)
private val RsIconOvershootEasing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1.0f)

private val RsPressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh
)

data class ResetAction(
    val label: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
fun NetFixResetSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<ResetAction>,
    closeLabel: String = "Отмена"
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", 0) }
    val isTv = remember(sharedPrefs) { sharedPrefs.getBoolean("is_smart_tv", false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val useTvLayout = isTv || isLandscape

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val frozenNavBarPadding = remember { navBarPaddingValues }
    val transition = updateTransition(targetState = visible, label = "ResetSheetTransition")

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
            transitionSpec = {
                if (targetState) tween(300, easing = RsDecelerateEasing) else tween(420, easing = RsAccelerateEasing)
            },
            label = "scrimAlpha"
        ) { if (it) 0.64f else 0f }

        val cardProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(360, easing = RsOvershootEasing)
                else tween(300, delayMillis = 120, easing = RsAccelerateEasing)
            },
            label = "cardProgress"
        ) { if (it) 1f else 0f }

        val innerIconScale by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(320, delayMillis = 100, easing = RsIconOvershootEasing)
                else tween(200, delayMillis = 80, easing = RsAccelerateEasing)
            },
            label = "innerIconScale"
        ) { if (it) 1f else 0f }

        val innerIconRotation by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(320, delayMillis = 100, easing = RsIconOvershootEasing)
                else tween(200, delayMillis = 80, easing = RsAccelerateEasing)
            },
            label = "innerIconRotation"
        ) { if (it) 0f else -25f }

        val headerProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, delayMillis = 60, easing = RsDecelerateEasing)
                else tween(200, delayMillis = 80, easing = RsAccelerateEasing)
            },
            label = "headerProgress"
        ) { if (it) 1f else 0f }

        val actionsProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, delayMillis = 100, easing = RsDecelerateEasing)
                else tween(200, delayMillis = 40, easing = RsAccelerateEasing)
            },
            label = "actionsProgress"
        ) { if (it) 1f else 0f }

        val closeButtonProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, delayMillis = 140, easing = RsDecelerateEasing)
                else tween(200, delayMillis = 0, easing = RsAccelerateEasing)
            },
            label = "closeButtonProgress"
        ) { if (it) 1f else 0f }

        val infiniteTransition = rememberInfiniteTransition(label = "iconFloat")
        val floatTranslation by infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatTranslation"
        )
        val swayRotation by infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
                repeatMode = RepeatMode.Reverse
            ),
            label = "swayRotation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(onDismissRequest) {
                    detectTapGestures(onTap = { onDismissRequest() })
                },
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
                        val scale = 0.88f + 0.12f * cardProgress
                        scaleX = scale
                        scaleY = scale
                        if (!useTvLayout) {
                            translationY = (1f - cardProgress) * 80f
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { })
                    }
            ) {
                if (useTvLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RsSheetBackground, RoundedCornerShape(28.dp))
                            .border(1.dp, RsButtonBorder, RoundedCornerShape(28.dp))
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
                                    .background(RsButtonBorder, CircleShape)
                                    .padding(2.dp)
                                    .background(RsSheetBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = RsTextPrimary,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = innerIconScale
                                            scaleY = innerIconScale
                                            translationY = floatTranslation * innerIconScale
                                            rotationZ = innerIconRotation + (swayRotation * innerIconScale)
                                        }
                                        .size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Сброс приложения",
                                color = RsTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Выберите вариант сброса. Приложение перезапустится.",
                                color = RsTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .graphicsLayer {
                                    alpha = actionsProgress
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            actions.forEachIndexed { index, action ->
                                ResetActionButton(
                                    label = action.label,
                                    subtitle = action.subtitle,
                                    focusRequester = focusRequesters[index],
                                    onClick = action.onClick
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            ResetCloseButton(
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
                                .background(RsSheetBackground, RoundedCornerShape(28.dp))
                                .padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .background(RsButtonBorder, CircleShape)
                                    .padding(2.dp)
                                    .background(RsSheetBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = RsTextPrimary,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = innerIconScale
                                            scaleY = innerIconScale
                                            translationY = floatTranslation * innerIconScale
                                            rotationZ = innerIconRotation + (swayRotation * innerIconScale)
                                        }
                                        .size(34.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Column(
                                modifier = Modifier.graphicsLayer { alpha = headerProgress },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Сброс приложения",
                                    color = RsTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Выберите вариант сброса. Приложение перезапустится.",
                                    color = RsTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = actionsProgress
                                        translationY = (1f - actionsProgress) * 16f
                                    },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                actions.forEachIndexed { index, action ->
                                    ResetActionButton(
                                        label = action.label,
                                        subtitle = action.subtitle,
                                        focusRequester = focusRequesters[index],
                                        onClick = action.onClick
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.graphicsLayer {
                                alpha = closeButtonProgress
                                translationY = (1f - closeButtonProgress) * 24f
                            }
                        ) {
                            ResetCloseButton(
                                label = closeLabel,
                                focusRequester = closeFocusRequester,
                                onClick = onDismissRequest
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetActionButton(
    label: String,
    subtitle: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val finalActive = isPressed || isFocused

    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = RsPressSpring,
        label = "resetPress"
    )
    val bgColor by animateColorAsState(
        targetValue = if (finalActive) Color(0xFF38383A) else RsButtonBackground,
        animationSpec = tween(150),
        label = "resetBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "resetBorder"
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
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
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
        Column {
            Text(
                text = label,
                color = RsTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = RsTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ResetCloseButton(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val finalActive = isPressed || isFocused

    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = RsPressSpring,
        label = "rClosePress"
    )
    val bgColor by animateColorAsState(
        targetValue = if (finalActive) Color(0xFF38383A) else RsButtonBackground,
        animationSpec = tween(150),
        label = "rCloseBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "rCloseBorder"
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
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .background(borderColor, RoundedCornerShape(20.dp))
            .padding(1.dp)
            .background(bgColor, RoundedCornerShape(19.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (finalActive) Color.White else RsCloseText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
