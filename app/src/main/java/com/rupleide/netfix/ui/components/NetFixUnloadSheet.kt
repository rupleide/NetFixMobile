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
import androidx.compose.ui.draw.clip
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

private val UlSheetBackground = Color(0xFF1E1E1E)
private val UlButtonBackground = Color(0xFF262626)
private val UlCloseText = Color(0xFFC4C4C6)
private val UlButtonBorder = Color(0x1AFFFFFF)
private val UlTextPrimary = Color(0xFFF4F4F5)
private val UlTextSecondary = Color(0xFFA1A1AA)

private val UlDecelerateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
private val UlAccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val UlOvershootEasing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f)
private val UlIconOvershootEasing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1.0f)

private val UlPressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh
)

data class UnloadOption(
    val key: String,
    val label: String,
    val subtitle: String
)

@Composable
fun NetFixUnloadSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    options: List<UnloadOption>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
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
    val transition = updateTransition(targetState = visible, label = "UnloadSheetTransition")

    val focusRequesters = remember(options) { List(options.size) { FocusRequester() } }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(150)
            try {
                val selectedIndex = options.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
                focusRequesters.getOrNull(selectedIndex)?.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (transition.currentState || transition.targetState) {
        BackHandler(onBack = onDismissRequest)

        val scrimAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, easing = UlDecelerateEasing) else tween(420, easing = UlAccelerateEasing)
            },
            label = "scrimAlpha"
        ) { if (it) 0.64f else 0f }

        val cardProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(360, easing = UlOvershootEasing)
                else tween(300, delayMillis = 120, easing = UlAccelerateEasing)
            },
            label = "cardProgress"
        ) { if (it) 1f else 0f }

        val innerIconScale by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(320, delayMillis = 100, easing = UlIconOvershootEasing)
                else tween(200, delayMillis = 80, easing = UlAccelerateEasing)
            },
            label = "innerIconScale"
        ) { if (it) 1f else 0f }

        val innerIconRotation by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(320, delayMillis = 100, easing = UlIconOvershootEasing)
                else tween(200, delayMillis = 80, easing = UlAccelerateEasing)
            },
            label = "innerIconRotation"
        ) { if (it) 0f else -25f }

        val headerProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, delayMillis = 60, easing = UlDecelerateEasing)
                else tween(200, delayMillis = 80, easing = UlAccelerateEasing)
            },
            label = "headerProgress"
        ) { if (it) 1f else 0f }

        val optionsProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, delayMillis = 100, easing = UlDecelerateEasing)
                else tween(200, delayMillis = 40, easing = UlAccelerateEasing)
            },
            label = "optionsProgress"
        ) { if (it) 1f else 0f }

        val closeButtonProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, delayMillis = 140, easing = UlDecelerateEasing)
                else tween(200, delayMillis = 0, easing = UlAccelerateEasing)
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
                            .background(UlSheetBackground, RoundedCornerShape(28.dp))
                            .border(1.dp, UlButtonBorder, RoundedCornerShape(28.dp))
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
                                    .background(UlButtonBorder, CircleShape)
                                    .padding(2.dp)
                                    .background(UlSheetBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_bolt),
                                    contentDescription = null,
                                    tint = UlTextPrimary,
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
                                text = "Запуск в фоне и выход",
                                color = UlTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Выберите, какие модули запустить перед выходом приложения.",
                                color = UlTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .graphicsLayer {
                                    alpha = optionsProgress
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEachIndexed { index, option ->
                                UnloadActionButton(
                                    option = option,
                                    selected = option.key == selectedKey,
                                    focusRequester = focusRequesters[index],
                                    onClick = { onOptionSelected(option.key) }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            UnloadCloseButton(
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
                                .background(UlSheetBackground, RoundedCornerShape(28.dp))
                                .padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .background(UlButtonBorder, CircleShape)
                                    .padding(2.dp)
                                    .background(UlSheetBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_bolt),
                                    contentDescription = null,
                                    tint = UlTextPrimary,
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
                                    text = "Запуск в фоне и выход",
                                    color = UlTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Выберите, какие модули запустить перед выходом приложения.",
                                    color = UlTextSecondary,
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
                                        alpha = optionsProgress
                                        translationY = (1f - optionsProgress) * 16f
                                    },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.forEachIndexed { index, option ->
                                    UnloadActionButton(
                                        option = option,
                                        selected = option.key == selectedKey,
                                        focusRequester = focusRequesters[index],
                                        onClick = { onOptionSelected(option.key) }
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
                            UnloadCloseButton(
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
private fun UnloadActionButton(
    option: UnloadOption,
    selected: Boolean,
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
        animationSpec = UlPressSpring,
        label = "unloadPress"
    )
    val bgColor by animateColorAsState(
        targetValue = if (finalActive) Color(0xFF38383A) else UlButtonBackground,
        animationSpec = tween(150),
        label = "unloadBg"
    )
    val baseBorder = if (selected) Color(0x4DFFFFFF) else Color.Transparent
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> baseBorder
        },
        animationSpec = tween(150),
        label = "unloadBorder"
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    color = UlTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (option.subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = option.subtitle,
                        color = UlTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnloadCloseButton(
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
        animationSpec = UlPressSpring,
        label = "uClosePress"
    )
    val bgColor by animateColorAsState(
        targetValue = if (finalActive) Color(0xFF38383A) else UlButtonBackground,
        animationSpec = tween(150),
        label = "uCloseBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "uCloseBorder"
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
            color = if (finalActive) Color.White else UlCloseText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
