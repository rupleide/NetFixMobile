package com.rupleide.netfix.ui.components

import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

private val EditDecelerateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
private val EditAccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val EditOvershootEasing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f)
private val EditIconEasing = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1.0f)
private val EditPressSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

private val EditSheetBg = Color(0xFF1E1E1E)
private val EditSheetCard = Color(0xFF262626)
private val EditTextPrimary = Color(0xFFF4F4F5)
private val EditTextSecondary = Color(0xFFA1A1AA)

@Composable
fun NetFixEditStrategySheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    nameValue: String,
    onNameChange: (String) -> Unit,
    notesValue: String,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    commandText: String? = null,
    onCommandChange: ((String) -> Unit)? = null,
    commandError: String? = null,
    showNotes: Boolean = true,
    title: String = "Настройка стратегии",
    subtitle: String = "Измените название и заметку для этой стратегии обхода"
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", 0) }
    val isTv = remember(sharedPrefs) { sharedPrefs.getBoolean("is_smart_tv", false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val useTvLayout = isTv || isLandscape

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val frozenNavBarPadding = remember { navBarPadding }
    val transition = updateTransition(targetState = visible, label = "EditStrategyTransition")
    val nameAtOpen = remember(visible) { nameValue }
    val notesAtOpen = remember(visible) { notesValue }
    val commandAtOpen = remember(visible) { commandText }
    val hasChanges = nameValue != nameAtOpen ||
        notesValue != notesAtOpen ||
        (onCommandChange != null && commandText != commandAtOpen)
    var lastShownCommandText by remember { mutableStateOf(commandText) }
    if (commandText != null) {
        lastShownCommandText = commandText
    }

    val saveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible && isTv) {
            kotlinx.coroutines.delay(150)
            try {
                saveFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (transition.currentState || transition.targetState) {
        BackHandler(onBack = onDismissRequest)

        val scrimAlpha by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, easing = EditDecelerateEasing) else tween(420, easing = EditAccelerateEasing) },
            label = "scrim"
        ) { if (it) 0.64f else 0f }

        val cardProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(360, easing = EditOvershootEasing) else tween(300, delayMillis = 120, easing = EditAccelerateEasing) },
            label = "card"
        ) { if (it) 1f else 0f }

        val iconScale by transition.animateFloat(
            transitionSpec = { if (targetState) tween(320, delayMillis = 100, easing = EditIconEasing) else tween(200, delayMillis = 80, easing = EditAccelerateEasing) },
            label = "iconScale"
        ) { if (it) 1f else 0f }

        val iconRotation by transition.animateFloat(
            transitionSpec = { if (targetState) tween(320, delayMillis = 100, easing = EditIconEasing) else tween(200, delayMillis = 80, easing = EditAccelerateEasing) },
            label = "iconRotation"
        ) { if (it) 0f else -25f }

        val headerProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, delayMillis = 60, easing = EditDecelerateEasing) else tween(200, delayMillis = 80, easing = EditAccelerateEasing) },
            label = "header"
        ) { if (it) 1f else 0f }

        val formProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, delayMillis = 100, easing = EditDecelerateEasing) else tween(200, delayMillis = 40, easing = EditAccelerateEasing) },
            label = "form"
        ) { if (it) 1f else 0f }

        val closeProgress by transition.animateFloat(
            transitionSpec = { if (targetState) tween(300, delayMillis = 140, easing = EditDecelerateEasing) else tween(200, easing = EditAccelerateEasing) },
            label = "close"
        ) { if (it) 1f else 0f }

        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val floatY by infiniteTransition.animateFloat(
            initialValue = -3f, targetValue = 3f,
            animationSpec = infiniteRepeatable(animation = tween(1500, easing = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)), repeatMode = RepeatMode.Reverse),
            label = "floatY"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = nfRegisterScrim(scrimAlpha)))
                .pointerInput(onDismissRequest) { detectTapGestures(onTap = { onDismissRequest() }) },
            contentAlignment = if (useTvLayout) Alignment.Center else Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .imePadding()
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
                            .background(EditSheetBg, RoundedCornerShape(28.dp))
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
                                    .background(EditSheetBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_custom),
                                    contentDescription = null,
                                    tint = EditTextPrimary,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = iconScale; scaleY = iconScale
                                            translationY = floatY * iconScale
                                            rotationZ = iconRotation
                                        }
                                        .size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(text = title, color = EditTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(text = subtitle, color = EditTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .graphicsLayer { alpha = formProgress },
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NetFixTextField(
                                value = nameValue,
                                onValueChange = onNameChange,
                                label = "Название",
                                placeholder = "Например, Дом, Работа",
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (showNotes) {
                                NetFixTextField(
                                    value = notesValue,
                                    onValueChange = onNotesChange,
                                    label = "Заметка",
                                    placeholder = "Заметка/комментарий к стратегии",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            lastShownCommandText?.let { StrategyCommandField(it, onCommandChange, commandError) }

                            Spacer(Modifier.height(8.dp))

                            EditCloseButton(
                                label = "Готово",
                                focusRequester = saveFocusRequester,
                                hasChanges = hasChanges,
                                onClick = onSave
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .background(EditSheetBg, RoundedCornerShape(28.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .background(Color(0x1AFFFFFF), CircleShape)
                                    .padding(2.dp)
                                    .background(EditSheetBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_custom),
                                    contentDescription = null,
                                    tint = EditTextPrimary,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = iconScale; scaleY = iconScale
                                            translationY = floatY * iconScale
                                            rotationZ = iconRotation
                                        }
                                        .size(34.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Column(
                                modifier = Modifier.graphicsLayer { alpha = headerProgress },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = title, color = EditTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(6.dp))
                                Text(text = subtitle, color = EditTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }

                            Spacer(Modifier.height(20.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = formProgress; translationY = (1f - formProgress) * 16f },
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NetFixTextField(
                                    value = nameValue,
                                    onValueChange = onNameChange,
                                    label = "Название",
                                    placeholder = "Например, Дом, Работа",
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (showNotes) {
                                    NetFixTextField(
                                        value = notesValue,
                                        onValueChange = onNotesChange,
                                        label = "Заметка",
                                        placeholder = "Заметка/комментарий к стратегии",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                lastShownCommandText?.let { StrategyCommandField(it, onCommandChange, commandError) }

                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(modifier = Modifier.graphicsLayer { alpha = closeProgress; translationY = (1f - closeProgress) * 24f }) {
                            EditCloseButton(
                                label = "Готово",
                                focusRequester = saveFocusRequester,
                                hasChanges = hasChanges,
                                onClick = onSave
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyCommandField(
    commandText: String,
    onCommandChange: ((String) -> Unit)?,
    commandError: String?
) {
    if (onCommandChange != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NetFixTextField(
                value = commandText,
                onValueChange = onCommandChange,
                label = "Строка конфига",
                placeholder = "Вставьте конфиг из ByeByeDPI или NetFix",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            if (commandError != null) {
                Text(text = commandError, color = Color(0xFFEF4444), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        return
    }

    val context = LocalContext.current
    NetFixTextField(
        value = commandText,
        onValueChange = { },
        label = "Строка конфига",
        singleLine = false,
        readOnly = true,
        trailingIcon = {
            androidx.compose.material3.IconButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NetFix strategy", commandText))
                    android.widget.Toast.makeText(context, "Конфиг скопирован", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                    contentDescription = "Скопировать конфиг",
                    tint = EditTextSecondary
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EditCloseButton(
    label: String,
    focusRequester: FocusRequester,
    hasChanges: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val finalActive = isPressed || isFocused

    val pressScale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = EditPressSpring, label = "cPress")
    val bgColor by animateColorAsState(targetValue = if (finalActive) Color(0xFF383838) else EditSheetCard, animationSpec = tween(150), label = "cBg")
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
        val labelColor by animateColorAsState(
            targetValue = if (isPressed || hasChanges) Color.White else EditTextSecondary,
            animationSpec = tween(200),
            label = "cLabel"
        )
        Text(text = label, color = labelColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
