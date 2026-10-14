package com.rupleide.netfix.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.data.isActionSheetVisibleGlobal
import com.rupleide.netfix.ui.main.MainMotion
import com.rupleide.netfix.ui.main.motionTween
import com.rupleide.netfix.ui.main.rememberAnimationsEnabled
import com.rupleide.netfix.ui.main.snappySpring

val LocalSheetViewportHeight = compositionLocalOf { 0.dp }

object Nf {
    val Bg = Color(0xFF161616)
    val Glass = Color(0xFF1E1E1E).copy(alpha = 0.85f)
    val Sheet = Color(0xFF1E1E1E)
    val Surface = Color(0xFF262626)
    val SurfaceOff = Color(0xFF1c1c1c)
    val SurfacePressed = Color(0xFF383838)
    val Line = Color(0x1AFFFFFF)
    val LineSoft = Color(0x14FFFFFF)
    val Text = Color(0xFFF4F4F5)
    val TextSub = Color(0xFFA1A1AA)
    val TextOff = Color(0xFF6B6B70)
    val Muted = Color(0xFF71717A)
    val Bright = Color(0xFFF4F4F5)
    val Blue = Color(0xFF3B82F6)
    val Green = Color(0xFF22C55E)
    val GreenText = Color(0xFF4ADE80)
    val Yellow = Color(0xFFEAB308)
    val Red = Color(0xFFEF4444)
    val RedText = Color(0xFFF87171)
    val SwitchOff = Color(0xFF3A3A3D)
    val Accent: Brush = SolidColor(Bright)
}

enum class NfButtonStyle { Primary, Accent, Secondary, Soft, Tinted, Ghost, Chosen, Danger, DangerSolid }

@Composable
fun NfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NfButtonStyle = NfButtonStyle.Primary,
    compact: Boolean = false,
    enabled: Boolean = true,
    small: Boolean = false
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = snappySpring(animationsEnabled),
        label = "nfButtonScale"
    )
    val shape = RoundedCornerShape(if (small) 10.dp else 12.dp)
    val lit = style != NfButtonStyle.Chosen && enabled
    val tvNeutral = LocalIsTv.current && enabled && (
        style == NfButtonStyle.Primary || style == NfButtonStyle.Secondary || style == NfButtonStyle.Soft ||
            style == NfButtonStyle.Tinted || style == NfButtonStyle.Ghost
        )
    val fillColor = when {
        tvNeutral -> if (pressed || focused) Color(0xFF383838) else if (style == NfButtonStyle.Ghost) Color.Transparent else Color(0xFF242424)
        style == NfButtonStyle.Chosen -> Nf.Green.copy(alpha = 0.12f)
        !enabled -> if (style == NfButtonStyle.Ghost) Color.Transparent else Nf.SurfaceOff
        style == NfButtonStyle.Danger -> Nf.Red.copy(alpha = if (pressed) 0.22f else 0.12f)
        style == NfButtonStyle.DangerSolid -> if (pressed) Color(0xFFC93A3A) else Nf.Red
        style == NfButtonStyle.Accent -> if (pressed) Color(0xFF2F6FD6) else Nf.Blue
        style == NfButtonStyle.Primary -> if (pressed) Color(0xFFD4D4D8) else Nf.Bright
        style == NfButtonStyle.Secondary -> if (pressed) Color(0xFF383838) else Color(0xFF242424)
        style == NfButtonStyle.Soft -> if (pressed) Color(0xFF383838) else Color(0xFF242424)
        style == NfButtonStyle.Tinted -> Nf.Bright.copy(alpha = if (pressed) 0.24f else 0.14f)
        else -> if (pressed) Nf.Surface else Color.Transparent
    }
    val borderColor = when {
        tvNeutral -> if (style == NfButtonStyle.Tinted) Color(0x4DFFFFFF) else Color(0x14FFFFFF)
        focused && enabled -> Color.White
        style == NfButtonStyle.Chosen -> Nf.Green.copy(alpha = 0.35f)
        !enabled -> Color(0x0DFFFFFF)
        style == NfButtonStyle.Danger -> Nf.Red.copy(alpha = 0.35f)
        style == NfButtonStyle.Soft || style == NfButtonStyle.Secondary -> Color(0x1AFFFFFF)
        style == NfButtonStyle.Tinted -> Nf.Bright.copy(alpha = 0.34f)
        else -> Color.Transparent
    }
    val textColor = when {
        tvNeutral -> if (pressed || focused) Color.White else Nf.Text
        style == NfButtonStyle.Chosen -> Nf.GreenText
        !enabled -> Nf.TextOff
        style == NfButtonStyle.Danger -> Nf.RedText
        style == NfButtonStyle.DangerSolid -> Color.White
        style == NfButtonStyle.Accent -> Color.White
        style == NfButtonStyle.Primary -> Nf.Bg
        style == NfButtonStyle.Ghost -> Nf.TextSub
        else -> Nf.Text
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (small) 36.dp else 52.dp)
            .scale(scale)
            .clip(shape)
            .background(fillColor)
            .border(if (focused && lit && !tvNeutral) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button
            ) {
                haptics(NetFixHaptic.Tap)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = if (small) 13.sp else 15.sp,
            maxLines = 1
        )
    }
}

@Composable
fun NfProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    height: Int = 6,
    indeterminate: Boolean = false
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val shown by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(if (animationsEnabled) 500 else 0, easing = FastOutSlowInEasing),
        label = "nfProgress"
    )
    val barModifier = modifier
        .fillMaxWidth()
        .height(height.dp)
    val fill = color ?: Nf.Bright
    if (indeterminate && fraction <= 0f) {
        LinearProgressIndicator(
            modifier = barModifier,
            color = fill,
            trackColor = Color(0x1AFFFFFF),
            strokeCap = StrokeCap.Round
        )
    } else {
        LinearProgressIndicator(
            progress = { shown },
            modifier = barModifier,
            color = fill,
            trackColor = Color(0x1AFFFFFF),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

@Composable
fun Modifier.nfBlink(active: Boolean, tint: Color): Modifier {
    if (!active) return this
    val infinite = rememberInfiniteTransition(label = "nfBlink")
    val pulse by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfBlinkPulse"
    )
    return this
        .drawBehind {
            drawRoundRect(
                color = tint.copy(alpha = 0.22f * pulse),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
        }
        .graphicsLayer { alpha = 0.45f + 0.55f * pulse }
}

@Composable
fun Modifier.nfFloat(): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val infinite = rememberInfiniteTransition(label = "nfFloat")
    val shift by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfFloatShift"
    )
    val sway by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfFloatSway"
    )
    return if (animationsEnabled) {
        this.graphicsLayer {
            translationY = shift
            rotationZ = sway
        }
    } else {
        this
    }
}

@Composable
fun Modifier.nfAura(color: Color): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val shown by animateColorAsState(
        targetValue = color,
        animationSpec = tween(if (animationsEnabled) 1200 else 0),
        label = "nfAura"
    )
    return this.drawBehind {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(shown, shown.copy(alpha = shown.alpha * 0.4f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

@Composable
fun NfSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val thumbWidth by animateDpAsState(
        targetValue = if (pressed) 26.dp else 20.dp,
        animationSpec = if (animationsEnabled) spring(dampingRatio = 0.8f, stiffness = 600f) else snap(),
        label = "nfSwitchThumbWidth"
    )
    val thumbX by animateDpAsState(
        targetValue = if (checked) 44.dp - 3.dp - thumbWidth else 3.dp,
        animationSpec = if (animationsEnabled) spring(dampingRatio = 0.72f, stiffness = 420f) else snap(),
        label = "nfSwitchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) Nf.Blue else Color(0xFF34343A),
        animationSpec = tween(if (animationsEnabled) 260 else 0),
        label = "nfSwitchTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color(0xFFB4B4BB),
        animationSpec = tween(if (animationsEnabled) 260 else 0),
        label = "nfSwitchThumbColor"
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(shape)
            .background(trackColor)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x14FFFFFF), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch
            ) {
                haptics(NetFixHaptic.Tap)
                onCheckedChange(!checked)
            }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX, y = 3.dp)
                .size(width = thumbWidth, height = 20.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
fun NfCheckbox(checked: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(Nf.Bright)
                else Modifier.border(2.dp, Color(0x47FFFFFF), shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Nf.Bg,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private val SheetDecelerate = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
private val SheetAccelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val SheetOvershoot = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1.0f)
private val SheetIconOvershoot = CubicBezierEasing(0.34f, 1.6f, 0.64f, 1.0f)
private val SheetButtonBackground = Color(0xFF262626)
private val SheetCloseText = Color(0xFFC4C4C6)

@Composable
fun NfSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    indicatorColor: Color = Nf.Bright,
    selectedTextColor: Color = Nf.Bg
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = rememberNetFixHaptics()
    val tvSegment = LocalIsTv.current && indicatorColor == Nf.Bright
    val baseIndicator = if (tvSegment) Color(0xFF343438) else indicatorColor
    val shownIndicatorColor = if (enabled) baseIndicator else baseIndicator.copy(alpha = 0.35f)
    val indicatorIndex = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        if (animationsEnabled) {
            indicatorIndex.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f))
            )
        } else {
            indicatorIndex.snapTo(selectedIndex.toFloat())
        }
    }
    val sources = remember(options.size) { List(options.size) { MutableInteractionSource() } }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Nf.Bg)
            .padding(4.dp)
    ) {
        val gap = 4.dp
        val itemWidth = (maxWidth - gap * (options.size - 1)) / options.size
        Box(
            modifier = Modifier
                .offset(x = (itemWidth + gap) * indicatorIndex.value)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(shownIndicatorColor)
                .then(if (tvSegment) Modifier.border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(10.dp)) else Modifier)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            options.forEachIndexed { index, label ->
                val source = sources[index]
                val focused by source.collectIsFocusedAsState()
                val hovered by source.collectIsHoveredAsState()
                val highlighted = focused || hovered
                val bgColor by animateColorAsState(
                    targetValue = if (highlighted && index != selectedIndex) Color(0x1AFFFFFF) else Color.Transparent,
                    animationSpec = tween(150),
                    label = "segmentBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .then(
                            if (focused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable(
                            interactionSource = source,
                            indication = null,
                            enabled = enabled
                        ) {
                            if (index != selectedIndex) haptics(NetFixHaptic.Tap)
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (!enabled) Nf.TextSub else if (index == selectedIndex) (if (tvSegment) Color.White else selectedTextColor) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.nfScrollFade(state: ScrollState, edge: Dp = 28.dp): Modifier {
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = edge.toPx()
            val topStrength = (state.value / fade).coerceIn(0f, 1f)
            val bottomStrength = ((state.maxValue - state.value) / fade).coerceIn(0f, 1f)
            if (topStrength > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 1f - topStrength), Color.Black),
                        startY = 0f,
                        endY = fade
                    ),
                    size = Size(size.width, fade),
                    blendMode = BlendMode.DstIn
                )
            }
            if (bottomStrength > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - bottomStrength)),
                        startY = size.height - fade,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - fade),
                    size = Size(size.width, fade),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}

@Composable
fun NfInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    monospace: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    if (LocalIsTv.current && !readOnly) {
        var editing by remember { mutableStateOf(false) }
        var draft by remember(editing) { mutableStateOf(value) }
        val shown = remember(value, visualTransformation) {
            visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(value)).text.text
        }
        NfTvValueField(
            label = placeholder,
            value = shown,
            onOpen = { editing = true },
            modifier = modifier,
            monospace = monospace,
            isError = isError
        )
        if (editing) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    onValueChange(draft)
                    editing = false
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                NfTvTextEditor(
                    visible = true,
                    title = "Редактирование",
                    label = placeholder,
                    initial = value,
                    onDone = {
                        onValueChange(it)
                        editing = false
                    },
                    onDismiss = { editing = false },
                    onTextChange = { draft = it },
                    keyboardType = keyboardType
                )
            }
        }
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Nf.Red.copy(alpha = 0.65f)
            focused -> Color(0x4DFFFFFF)
            else -> Nf.Line
        },
        animationSpec = tween(150),
        label = "inputBorder"
    )
    val focusManager = LocalFocusManager.current
    val tv = LocalIsTv.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Nf.Surface)
            .border(if (tv && focused) 2.dp else 1.dp, if (tv && focused) Color.White else borderColor, shape)
            .padding(start = 16.dp, end = if (trailing != null) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = Nf.TextOff, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                readOnly = readOnly,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (tv) focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) else focusManager.clearFocus()
                }),
                textStyle = TextStyle(
                    color = Nf.Text,
                    fontSize = if (monospace) 14.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
                ),
                cursorBrush = SolidColor(Nf.Bright),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (tv) Modifier.nfTvKeyboardOnCenter() else Modifier)
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = if (focused) Nf.Text else Nf.TextSub,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun NfBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    closeLabel: String,
    iconTint: Color? = null,
    spinIcon: Boolean = false,
    maxHeightFraction: Float = 0.6f,
    fixedHeight: Boolean = false,
    closeHighlighted: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    scrollResetKey: Any? = null,
    footer: (@Composable ColumnScope.(entry: @Composable (Int) -> Modifier) -> Unit)? = null,
    content: @Composable ColumnScope.(entry: @Composable (Int) -> Modifier) -> Unit
) {
    val configuration = LocalConfiguration.current
    val useTvLayout = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val frozenNavBarPadding = remember { navBarPaddingValues }
    val transition = updateTransition(targetState = visible, label = "NfSheetTransition")

    LaunchedEffect(visible) {
        isActionSheetVisibleGlobal = visible
    }
    DisposableEffect(Unit) {
        onDispose { isActionSheetVisibleGlobal = false }
    }

    if (!(transition.currentState || transition.targetState)) return

    BackHandler(onBack = onDismiss)

    val tv = LocalIsTv.current
    val sheetFocus = remember { FocusRequester() }
    LaunchedEffect(visible, tv) {
        if (tv && visible) {
            delay(420)
            try {
                sheetFocus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val contentScroll = rememberScrollState()
    LaunchedEffect(scrollResetKey) {
        contentScroll.scrollTo(0)
        if (tv && visible) {
            delay(380)
            try {
                sheetFocus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val scrimAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(300, easing = SheetDecelerate) else tween(460, easing = SheetAccelerate)
        },
        label = "scrimAlpha"
    ) { if (it) 0.64f else 0f }

    val scrimKey = remember { Any() }
    androidx.compose.runtime.SideEffect { com.rupleide.netfix.data.sheetScrimAlphas[scrimKey] = scrimAlpha }
    androidx.compose.runtime.DisposableEffect(scrimKey) {
        onDispose { com.rupleide.netfix.data.sheetScrimAlphas.remove(scrimKey) }
    }

    val cardProgress by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(360, easing = SheetOvershoot) else tween(300, delayMillis = 160, easing = SheetAccelerate)
        },
        label = "cardProgress"
    ) { if (it) 1f else 0f }

    val innerIconScale by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(320, delayMillis = 100, easing = SheetIconOvershoot) else tween(200, delayMillis = 120, easing = SheetAccelerate)
        },
        label = "innerIconScale"
    ) { if (it) 1f else 0f }

    val innerIconRotation by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(320, delayMillis = 100, easing = SheetIconOvershoot) else tween(200, delayMillis = 120, easing = SheetAccelerate)
        },
        label = "innerIconRotation"
    ) { if (it) 0f else -25f }

    val closeButtonProgress by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(300, delayMillis = 140, easing = SheetDecelerate) else tween(200, delayMillis = 0, easing = SheetAccelerate)
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
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "spinAngle"
    )

    val entryFor: @Composable (Int) -> Modifier = { index ->
        val entryProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 300, delayMillis = 60 + index * 40, easing = SheetDecelerate)
                } else {
                    tween(durationMillis = 200, delayMillis = (4 - index).coerceAtLeast(0) * 40, easing = SheetAccelerate)
                }
            },
            label = "entry_$index"
        ) { if (it) 1f else 0f }
        Modifier.graphicsLayer {
            alpha = entryProgress
            translationY = (1f - entryProgress) * 24f
        }
    }

    val tint = iconTint ?: Nf.Text
    val ringColor = if (iconTint != null) iconTint.copy(alpha = 0.45f) else Nf.Line
    val iconModifier = Modifier.graphicsLayer {
        scaleX = innerIconScale
        scaleY = innerIconScale
        translationY = floatTranslation * innerIconScale
        rotationZ = innerIconRotation + (if (spinIcon) spinAngle else swayRotation * innerIconScale)
    }
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val availableHeight = (configuration.screenHeightDp.dp - imeHeight - 120.dp).coerceAtLeast(180.dp)
    val maxCardHeight = minOf((configuration.screenHeightDp * (if (useTvLayout) 0.86f else maxHeightFraction)).dp, availableHeight)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = nfRegisterScrim(scrimAlpha)))
            .pointerInput(onDismiss) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = if (useTvLayout) Alignment.Center else Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .imePadding()
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
                .then(
                    if (tv) {
                        Modifier
                            .focusRequester(sheetFocus)
                            .focusProperties { exit = { FocusRequester.Cancel } }
                            .focusGroup()
                    } else {
                        Modifier
                    }
                )
        ) {
            if (useTvLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Nf.Sheet, RoundedCornerShape(28.dp))
                        .border(1.dp, Nf.Line, RoundedCornerShape(28.dp))
                        .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing), alignment = Alignment.BottomCenter)
                        .then(if (fixedHeight) Modifier.height(maxCardHeight) else Modifier.heightIn(max = maxCardHeight))
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(ringColor, CircleShape)
                                .padding(2.dp)
                                .background(Nf.Sheet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = icon,
                                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                                label = "sheetIcon"
                            ) { shownIcon ->
                                Icon(
                                    imageVector = shownIcon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = iconModifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                            label = "sheetTitle"
                        ) { shownTitle ->
                            Text(
                                text = shownTitle,
                                color = Nf.Text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (subtitle.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = subtitle,
                                color = Nf.TextSub,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f, fill = fixedHeight)
                                .fillMaxWidth()
                        ) {
                            val viewportHeight = maxHeight
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .nfScrollFade(contentScroll)
                                    .verticalScroll(contentScroll),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompositionLocalProvider(LocalSheetViewportHeight provides viewportHeight) {
                                    content(entryFor)
                                }
                            }
                        }
                        if (footer != null) footer(entryFor)
                        Spacer(Modifier.height(4.dp))
                        SheetCloseButton(
                            label = closeLabel,
                            staggerIndex = 3,
                            transition = transition,
                            highlighted = closeHighlighted,
                            onClick = onConfirm ?: onDismiss
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Nf.Sheet, RoundedCornerShape(28.dp))
                            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing), alignment = Alignment.BottomCenter)
                            .then(if (fixedHeight) Modifier.height(maxCardHeight) else Modifier.heightIn(max = maxCardHeight))
                            .padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(ringColor, CircleShape)
                                .padding(2.dp)
                                .background(Nf.Sheet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = icon,
                                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                                label = "sheetIcon"
                            ) { shownIcon ->
                                Icon(
                                    imageVector = shownIcon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = iconModifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        AnimatedContent(
                            targetState = title,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                            label = "sheetTitle"
                        ) { shownTitle ->
                            Text(
                                text = shownTitle,
                                color = Nf.Text,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (subtitle.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = subtitle,
                                color = Nf.TextSub,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }


                        Spacer(Modifier.height(20.dp))

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f, fill = fixedHeight)
                                .fillMaxWidth()
                        ) {
                            val viewportHeight = maxHeight
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .nfScrollFade(contentScroll)
                                    .verticalScroll(contentScroll),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompositionLocalProvider(LocalSheetViewportHeight provides viewportHeight) {
                                    content(entryFor)
                                }
                            }
                        }
                        if (footer != null) footer(entryFor)
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = closeButtonProgress
                            translationY = (1f - closeButtonProgress) * 24f
                        }
                    ) {
                        SheetCloseButton(
                            label = closeLabel,
                            staggerIndex = 3,
                            transition = transition,
                            highlighted = closeHighlighted,
                            onClick = onConfirm ?: onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetCloseButton(
    label: String,
    staggerIndex: Int,
    transition: Transition<Boolean>,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val active = pressed || focused

    val entryProgress by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, delayMillis = 60 + staggerIndex * 40, easing = SheetDecelerate)
            } else {
                tween(durationMillis = 200, delayMillis = 0, easing = SheetAccelerate)
            }
        },
        label = "closeEntry"
    ) { if (it) 1f else 0f }

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "closePress"
    )
    val background by animateColorAsState(
        targetValue = if (active) Nf.SurfacePressed else SheetButtonBackground,
        animationSpec = tween(150),
        label = "closeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            pressed -> Color.White
            focused -> Color(0x33FFFFFF)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "closeBorder"
    )

    val labelColor by animateColorAsState(
        targetValue = if (highlighted || active) Color.White else SheetCloseText,
        animationSpec = tween(200),
        label = "closeLabel"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryProgress
                translationY = (1f - entryProgress) * 24f
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button
            ) {
                haptics(NetFixHaptic.Tap)
                onClick()
            }
            .background(borderColor, RoundedCornerShape(20.dp))
            .padding(1.dp)
            .background(background, RoundedCornerShape(19.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
