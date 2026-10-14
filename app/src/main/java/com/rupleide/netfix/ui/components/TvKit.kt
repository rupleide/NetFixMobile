@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.rupleide.netfix.ui.components

import androidx.compose.ui.focus.focusRestorer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.Image
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.rupleide.netfix.ui.main.rememberAnimationsEnabled
import kotlinx.coroutines.delay

object NfTv {
    val Safe = 48.dp
    val RailWidth = 104.dp
    val RailExpanded = 240.dp
    val RailItem = 60.dp
    val ContentStart = 136.dp
    val LeftPane = 288.dp
    val PaneGap = 24.dp
    val FocusRoom = 16.dp
    val ButtonL = 56.dp
    val ButtonM = 48.dp
    val ButtonS = 40.dp
    val Row = 72.dp
    val Input = 56.dp
    val Dialog = 720.dp
    val ImeShare = 0.45f
}

enum class NfTvSize { L, M, S }

data class NfTvRailItem(val key: Int, val label: String, val iconRes: Int, val badge: Boolean = false)

@Composable
fun NfTvScaffold(
    items: List<NfTvRailItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    contentFocus: FocusRequester,
    railFocus: FocusRequester,
    content: @Composable () -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val focusManager = LocalFocusManager.current
    var railHasFocus by remember { mutableStateOf(false) }
    var railCollapsed by remember { mutableStateOf(false) }
    val imeOpen = isImeShown()
    val sheetOpen = com.rupleide.netfix.data.isActionSheetVisibleGlobal
    BackHandler(enabled = railHasFocus && !sheetOpen) { focusManager.moveFocus(FocusDirection.Right) }
    val contentGroup = remember { FocusRequester() }
    val stripDim = if (com.rupleide.netfix.data.isTvFullScreenOverlayGlobal) 0f else com.rupleide.netfix.data.sheetScrimAlphaMax()
    LaunchedEffect(railHasFocus, sheetOpen, imeOpen) {
        if (railHasFocus) {
            railCollapsed = false
        } else if (!sheetOpen && !imeOpen) {
            delay(15_000)
            railCollapsed = true
        }
    }
    val collapse by animateFloatAsState(
        targetValue = if (railCollapsed) 1f else 0f,
        animationSpec = tween(if (animationsEnabled) 320 else 0, easing = FastOutSlowInEasing),
        label = "nfTvRailCollapse"
    )
    var contentHasFocus by remember { mutableStateOf(false) }
    val enterScope = rememberCoroutineScope()
    val enterContent: () -> Unit = {
        enterScope.launch {
            repeat(14) {
                delay(120)
                if (!contentHasFocus) contentGroup.requestFocusSafe()
            }
        }
    }
    var sheetWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(sheetOpen) {
        if (sheetOpen) {
            sheetWasOpen = true
        } else if (sheetWasOpen) {
            sheetWasOpen = false
            delay(160)
            contentGroup.requestFocusSafe()
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Nf.Bg)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(contentGroup)
                .onFocusChanged { contentHasFocus = it.hasFocus }
                .focusProperties { exit = { if (it == FocusDirection.Left) railFocus else FocusRequester.Default } }
                .focusGroup()
        ) {
            content()
        }
        if (collapse > 0f && !sheetOpen && !imeOpen) {
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
                    .width(4.dp)
                    .height(88.dp)
                    .graphicsLayer { alpha = collapse }
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = !sheetOpen && !imeOpen,
            enter = androidx.compose.animation.fadeIn(tween(if (animationsEnabled) 240 else 0)) +
                androidx.compose.animation.slideInHorizontally(
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 160f)
                ) { -it },
            exit = androidx.compose.animation.fadeOut(tween(if (animationsEnabled) 160 else 0)) +
                androidx.compose.animation.slideOutHorizontally(tween(if (animationsEnabled) 200 else 0)) { -it },
            modifier = Modifier
                .zIndex(2f)
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .graphicsLayer {
                    alpha = 1f - collapse
                    translationX = -48.dp.toPx() * collapse
                }
        ) {
            NfTvRail(
                items = items,
                selected = selected,
                onSelect = onSelect,
                onEnterContent = enterContent,
                railFocus = railFocus,
                modifier = Modifier
                    .width(88.dp)
                    .onFocusChanged { railHasFocus = it.hasFocus }
            )
        }
    }
}

@Composable
fun NfTvRail(
    items: List<NfTvRailItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onEnterContent: () -> Unit,
    railFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = rememberNetFixHaptics()
    val itemHeight = 64.dp
    val itemGap = 4.dp
    val selectedIndex = items.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    val indicator = remember { androidx.compose.animation.core.Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        if (animationsEnabled) {
            indicator.animateTo(
                selectedIndex.toFloat(),
                tween(350, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f))
            )
        } else {
            indicator.snapTo(selectedIndex.toFloat())
        }
    }
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemGap)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .offset(y = (itemHeight + itemGap) * indicator.value)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF2D2D30))
            )
            Column(verticalArrangement = Arrangement.spacedBy(itemGap)) {
                items.forEachIndexed { index, item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused by interactionSource.collectIsFocusedAsState()
                    val emphasis = (1f - kotlin.math.abs(index - indicator.value)).coerceIn(0f, 1f)
                    val tint by animateColorAsState(
                        targetValue = if (focused) Color.White else androidx.compose.ui.graphics.lerp(Color(0xFF71717A), Color(0xFFF4F4F5), emphasis),
                        animationSpec = tween(150),
                        label = "nfTvRailTint"
                    )
                    Column(
                        modifier = Modifier
                            .then(if (item.key == selected) Modifier.focusRequester(railFocus) else Modifier)
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (focused) Color(0xFF383838) else Color.Transparent)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                haptics(NetFixHaptic.Tap)
                                onSelect(item.key)
                                onEnterContent()
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            Icon(painter = painterResource(item.iconRes), contentDescription = item.label, tint = tint, modifier = Modifier.size(26.dp))
                            if (item.badge) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-2).dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Nf.Yellow)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = tint,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = if (emphasis > 0.55f) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NfTvTwoPane(
    title: String,
    modifier: Modifier = Modifier,
    rightTitle: String? = null,
    hideSectionTitles: Boolean = false,
    leftEntry: FocusRequester? = null,
    rightScroll: ScrollState = rememberScrollState(),
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit
) {
    val leftWidth = NfTv.LeftPane + NfTv.FocusRoom * 2
    val rightEntry = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = NfTv.ContentStart - NfTv.FocusRoom, end = NfTv.Safe - NfTv.FocusRoom, top = 28.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .nfEnter(0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Nf.Text,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(leftWidth).padding(start = NfTv.FocusRoom)
            )
            if (rightTitle != null) {
                Text(
                    text = rightTitle,
                    color = Nf.TextSub,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = NfTv.PaneGap - NfTv.FocusRoom + NfTv.FocusRoom)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(leftWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NfTv.FocusRoom, vertical = NfTv.FocusRoom / 2)
                    .focusProperties { exit = { if (it == FocusDirection.Right) rightEntry else FocusRequester.Default } }
                    .focusGroup()
                    .nfEnter(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = left
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = (NfTv.PaneGap - 1.dp) / 2, vertical = 4.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0x14FFFFFF))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .nfScrollFade(rightScroll, edge = 24.dp)
                    .verticalScroll(rightScroll)
                    .padding(start = NfTv.FocusRoom, end = NfTv.FocusRoom, top = NfTv.FocusRoom / 2, bottom = NfTv.FocusRoom)
                    .focusRequester(rightEntry)
                    .focusProperties {
                        exit = { if (it == FocusDirection.Left && leftEntry != null) leftEntry else FocusRequester.Default }
                    }
                    .focusGroup()
                    .nfEnter(2),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompositionLocalProvider(LocalNfHideSectionTitle provides hideSectionTitles) {
                    right()
                }
            }
        }
    }
}

@Composable
fun NfTvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NfButtonStyle = NfButtonStyle.Secondary,
    size: NfTvSize = NfTvSize.M,
    enabled: Boolean = true
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val height = when (size) {
        NfTvSize.L -> NfTv.ButtonL
        NfTvSize.M -> NfTv.ButtonM
        NfTvSize.S -> NfTv.ButtonS
    }
    val corner = when (size) {
        NfTvSize.L -> 14.dp
        NfTvSize.M -> 12.dp
        NfTvSize.S -> 10.dp
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled && animationsEnabled) 0.97f else 1f,
        animationSpec = tween(90),
        label = "nfTvButtonPress"
    )
    val fill = when {
        !enabled -> Nf.SurfaceOff
        style == NfButtonStyle.Primary -> if (pressed || focused) Color(0xFF383838) else Color(0xFF242424)
        style == NfButtonStyle.DangerSolid -> if (pressed) Color(0xFFC93A3A) else Nf.Red
        style == NfButtonStyle.Danger -> Nf.Red.copy(alpha = if (focused || pressed) 0.22f else 0.12f)
        style == NfButtonStyle.Ghost -> if (focused || pressed) Nf.SurfacePressed else Color.Transparent
        else -> if (focused || pressed) Nf.SurfacePressed else Color(0xFF242424)
    }
    val textColor = when {
        !enabled -> Nf.TextOff
        style == NfButtonStyle.Primary -> Nf.Text
        style == NfButtonStyle.DangerSolid -> Color.White
        style == NfButtonStyle.Danger -> Nf.RedText
        else -> Nf.Text
    }
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .focusProperties { canFocus = enabled }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(fill)
            .border(1.dp, if (style == NfButtonStyle.DangerSolid) Color.Transparent else if (style == NfButtonStyle.Tinted) Color(0x4DFFFFFF) else Color(0x14FFFFFF), shape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) {
                haptics(NetFixHaptic.Tap)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = when (size) {
                NfTvSize.L -> 18.sp
                NfTvSize.M -> 16.sp
                NfTvSize.S -> 15.sp
            },
            maxLines = 1
        )
    }
}

@Composable
fun NfTvSectionItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val shape = RoundedCornerShape(10.dp)
    val tint = when {
        selected || focused -> Nf.Text
        danger -> Nf.RedText
        else -> Nf.Muted
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NfTv.ButtonS)
            .clip(shape)
            .background(if (focused) Color(0xFF383838) else Color.Transparent)
            .border(1.dp, if (selected) Color(0x4DFFFFFF) else Color.Transparent, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onFocus() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, color = if (selected || focused) Nf.Text else Nf.TextSub, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun NfTvValueField(
    label: String,
    value: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    isError: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = if (isError) Nf.RedText else Nf.TextSub, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NfTv.Input)
                .nfTvFocus(corner = 14.dp, wide = true)
                .clip(shape)
                .background(Nf.Surface)
                .border(1.dp, if (isError) Nf.Red.copy(alpha = 0.65f) else Nf.Line, shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onOpen)
                .padding(start = 18.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = Nf.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (monospace) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun NfTvTextEditor(
    visible: Boolean,
    title: String,
    label: String,
    initial: String,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
    help: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false,
    onTextChange: (String) -> Unit = {},
    validate: (String) -> Boolean = { true }
) {
    if (!visible) return
    var text by remember(initial) { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val valid = validate(text)
    LaunchedEffect(text) { onTextChange(text) }
    BackHandler {
        if (valid) onDone(text) else onDismiss()
    }
    LaunchedEffect(Unit) {
        delay(60)
        focus.requestFocusSafe()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .background(Nf.Bg)
            .padding(start = NfTv.ContentStart, end = NfTv.Safe, top = 24.dp)
            .focusGroup()
            .focusProperties { exit = { FocusRequester.Cancel } },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Nf.TextSub, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(label, color = if (valid) Nf.TextSub else Nf.RedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .width(if (multiline) 752.dp else 600.dp)
                .height(if (multiline) 172.dp else 64.dp)
                .nfTvFocus(corner = 14.dp, wide = true)
                .clip(RoundedCornerShape(14.dp))
                .background(Nf.Surface)
                .border(1.dp, if (valid) Nf.Line else Nf.Red.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = if (multiline) 14.dp else 0.dp),
            contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = !multiline,
                maxLines = if (multiline) 6 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (multiline) ImeAction.Default else ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (valid) onDone(text) }),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Nf.Text,
                    fontSize = if (multiline) 16.sp else 22.sp,
                    lineHeight = if (multiline) 26.sp else 28.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (multiline || keyboardType == KeyboardType.Number) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Nf.Bright),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .nfTvKeyboardOnCenter()
            )
        }
        if (help != null) {
            Text(help, color = if (valid) Nf.TextSub else Nf.RedText, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun NfTvDialogFrame(
    visible: Boolean,
    onDismiss: () -> Unit,
    firstFocus: FocusRequester,
    returnFocus: FocusRequester?,
    header: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return
    val animationsEnabled = rememberAnimationsEnabled()
    var shown by remember { mutableStateOf(!animationsEnabled) }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(if (animationsEnabled) 260 else 0, easing = FastOutSlowInEasing),
        label = "nfTvDialog"
    )
    LaunchedEffect(Unit) {
        shown = true
        delay(if (animationsEnabled) 120 else 0)
        firstFocus.requestFocusSafe()
    }
    BackHandler {
        onDismiss()
        returnFocus?.requestFocusSafe()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(6f)
            .background(Color.Black.copy(alpha = 0.64f * progress)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(NfTv.Dialog)
                .graphicsLayer {
                    alpha = progress
                    val s = 0.94f + 0.06f * progress
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Nf.Sheet)
                .border(1.dp, Nf.Line, RoundedCornerShape(28.dp))
                .padding(28.dp)
                .focusGroup()
                .focusProperties { exit = { FocusRequester.Cancel } },
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(240.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = header
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(PaddingValues(NfTv.FocusRoom / 2)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = actions
            )
        }
    }
}

fun FocusRequester.requestFocusSafe() {
    try {
        requestFocus()
    } catch (_: Exception) {
    }
}

@Composable
fun nfRegisterScrim(alpha: Float): Float {
    val key = remember { Any() }
    androidx.compose.runtime.SideEffect { com.rupleide.netfix.data.sheetScrimAlphas[key] = alpha }
    androidx.compose.runtime.DisposableEffect(key) {
        onDispose { com.rupleide.netfix.data.sheetScrimAlphas.remove(key) }
    }
    return alpha
}
