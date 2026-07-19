package com.rupleide.netfix.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import com.rupleide.netfix.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

private val SheetBackground = Color(0xFF1E1E1E)
private val ButtonBackground = Color(0xFF262626)
private val CloseText = Color(0xFFC4C4C6)
private val ButtonBorder = Color(0x1AFFFFFF)
private val TextPrimary = Color(0xFFF4F4F5)
private val TextSecondary = Color(0xFFA1A1AA)

private val DecelerateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
private val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val OvershootEasing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f)
private val IconOvershootEasing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1.0f)

private val PressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh
)

@Composable
fun NetFixLogsSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    icon: Any,
    title: String,
    subtitle: String,
    systemInfo: String,
    logsList: List<String>,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    closeLabel: String = "Закрыть"
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", 0) }
    val isTv = remember(sharedPrefs) { sharedPrefs.getBoolean("is_smart_tv", false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val useTvLayout = isTv || isLandscape

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val frozenNavBarPadding = remember { navBarPaddingValues }

    val transition = updateTransition(targetState = visible, label = "LogsSheetTransition")

    val copyFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            delay(150)
            try {
                copyFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (transition.currentState || transition.targetState) {
        BackHandler(onBack = onDismissRequest)

        val scrimAlpha by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(300, easing = DecelerateEasing) else tween(460, easing = AccelerateEasing)
            },
            label = "scrimAlpha"
        ) { if (it) 0.64f else 0f }

        val cardProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(360, easing = OvershootEasing)
                } else {
                    tween(300, delayMillis = 160, easing = AccelerateEasing)
                }
            },
            label = "cardProgress"
        ) { if (it) 1f else 0f }

        val innerIconScale by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(320, delayMillis = 100, easing = IconOvershootEasing)
                } else {
                    tween(200, delayMillis = 120, easing = AccelerateEasing)
                }
            },
            label = "innerIconScale"
        ) { if (it) 1f else 0f }

        val innerIconRotation by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(320, delayMillis = 100, easing = IconOvershootEasing)
                } else {
                    tween(200, delayMillis = 120, easing = AccelerateEasing)
                }
            },
            label = "innerIconRotation"
        ) { if (it) 0f else -25f }

        val headerTextProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(300, delayMillis = 60, easing = DecelerateEasing)
                } else {
                    tween(200, delayMillis = 120, easing = AccelerateEasing)
                }
            },
            label = "headerTextProgress"
        ) { if (it) 1f else 0f }

        val logsBoxProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(300, delayMillis = 100, easing = DecelerateEasing)
                } else {
                    tween(200, delayMillis = 80, easing = AccelerateEasing)
                }
            },
            label = "logsBoxProgress"
        ) { if (it) 1f else 0f }

        val actionsRowProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(300, delayMillis = 140, easing = DecelerateEasing)
                } else {
                    tween(200, delayMillis = 40, easing = AccelerateEasing)
                }
            },
            label = "actionsRowProgress"
        ) { if (it) 1f else 0f }

        val closeButtonProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(300, delayMillis = 180, easing = DecelerateEasing)
                } else {
                    tween(200, delayMillis = 0, easing = AccelerateEasing)
                }
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
                    .widthIn(max = if (useTvLayout) 720.dp else 500.dp)
                    .fillMaxHeight(0.75f)
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
                            .background(SheetBackground, RoundedCornerShape(28.dp))
                            .border(1.dp, ButtonBorder, RoundedCornerShape(28.dp))
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(ButtonBorder, CircleShape)
                                    .padding(2.dp)
                                    .background(SheetBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                when (icon) {
                                    is ImageVector -> {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = TextPrimary,
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
                                    is Int -> {
                                        Icon(
                                            painter = painterResource(id = icon),
                                            contentDescription = null,
                                            tint = TextPrimary,
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
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LogsActionButton(
                                        label = "Копировать",
                                        focusRequester = copyFocusRequester,
                                        onClick = onCopy,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LogsActionButton(
                                        label = "Очистить",
                                        focusRequester = clearFocusRequester,
                                        onClick = onClear,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                LogsCloseButton(
                                    label = closeLabel,
                                    focusRequester = closeFocusRequester,
                                    onClick = onDismissRequest
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF121212))
                                .padding(12.dp)
                        ) {
                            if (logsList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Здесь пока что пусто)\nВключите запись логов",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    item {
                                        Text(
                                            text = systemInfo,
                                            color = Color(0xFF3B82F6),
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    items(logsList.size) { idx ->
                                        Text(
                                            text = logsList[idx],
                                            color = Color(0xFFD4D4D8),
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(SheetBackground, RoundedCornerShape(28.dp))
                                .padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .background(ButtonBorder, CircleShape)
                                    .padding(2.dp)
                                    .background(SheetBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                when (icon) {
                                    is ImageVector -> {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = TextPrimary,
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
                                    is Int -> {
                                        Icon(
                                            painter = painterResource(id = icon),
                                            contentDescription = null,
                                            tint = TextPrimary,
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
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Column(
                                modifier = Modifier.graphicsLayer { alpha = headerTextProgress },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = subtitle,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = logsBoxProgress
                                        translationY = (1f - logsBoxProgress) * 16f
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF121212))
                                    .padding(12.dp)
                            ) {
                                if (logsList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Здесь пока что пусто)\nВключите запись логов",
                                            color = TextSecondary,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = systemInfo,
                                                color = Color(0xFF3B82F6),
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                        items(logsList.size) { idx ->
                                            Text(
                                                text = logsList[idx],
                                                color = Color(0xFFD4D4D8),
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = actionsRowProgress
                                        translationY = (1f - actionsRowProgress) * 16f
                                    },
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LogsActionButton(
                                    label = "Копировать",
                                    focusRequester = copyFocusRequester,
                                    onClick = onCopy,
                                    modifier = Modifier.weight(1f)
                                )
                                LogsActionButton(
                                    label = "Очистить",
                                    focusRequester = clearFocusRequester,
                                    onClick = onClear,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = closeButtonProgress
                                    translationY = (1f - closeButtonProgress) * 24f
                                }
                        ) {
                            LogsCloseButton(
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
private fun LogsActionButton(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val finalActive = isPressed || isFocused

    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = PressSpring,
        label = "btnPress"
    )

    val currentBgColor by animateColorAsState(
        targetValue = if (finalActive) Color(0xFF38383A) else ButtonBackground,
        animationSpec = tween(150),
        label = "btnBg"
    )

    val currentBorderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color(0x1AFFFFFF)
        },
        animationSpec = tween(150),
        label = "btnBorder"
    )

    Box(
        modifier = modifier
            .height(48.dp)
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
            .background(currentBorderColor, RoundedCornerShape(12.dp))
            .padding(1.dp)
            .background(currentBgColor, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LogsCloseButton(
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
        animationSpec = PressSpring,
        label = "closePress"
    )

    val currentBgColor by animateColorAsState(
        targetValue = if (finalActive) Color(0xFF38383A) else ButtonBackground,
        animationSpec = tween(150),
        label = "closeBg"
    )
    val currentBorderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isFocused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "closeBorder"
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
            .background(currentBorderColor, RoundedCornerShape(20.dp))
            .padding(1.dp)
            .background(currentBgColor, RoundedCornerShape(19.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (finalActive) Color.White else CloseText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
