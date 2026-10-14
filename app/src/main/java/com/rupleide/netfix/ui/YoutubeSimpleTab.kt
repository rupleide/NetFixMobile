package com.rupleide.netfix.ui

import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.NfTvButton
import com.rupleide.netfix.ui.components.NfTvSize
import com.rupleide.netfix.ui.components.NfTvTwoPane
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.nfTvKeyboardOnCenter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.core.dpibypass.NetworkKind
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager.RankedConfig
import com.rupleide.netfix.core.dpibypass.sanitizePastedByeDpiCommand
import com.rupleide.netfix.core.dpibypass.StrategyTestManager.VerifyScope
import com.rupleide.netfix.core.dpibypass.StrategyTester
import com.rupleide.netfix.ui.components.LocalSheetViewportHeight
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfInputField
import com.rupleide.netfix.ui.components.NfBottomSheet
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.NfSegmented
import com.rupleide.netfix.ui.components.nfAura
import com.rupleide.netfix.ui.components.nfBlink
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.nfFloat
import com.rupleide.netfix.ui.components.rememberNetFixHaptics
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import com.rupleide.netfix.ui.main.MainMotion
import com.rupleide.netfix.ui.main.motionTween
import com.rupleide.netfix.ui.main.rememberAnimationsEnabled

private enum class TabScreen { NoScan, Scanning, Ready }

private data class Verdict(val label: String, val textColor: Color, val dotColor: Color)

private data class VerifyRow(
    val kind: NetworkKind,
    val strategyName: String,
    val verdict: Verdict,
    val totalOk: Int,
    val total: Int
)

private fun sameStrategy(a: String?, b: String?): Boolean =
    a != null && b != null && StrategyTester.normalizeStrategy(a) == StrategyTester.normalizeStrategy(b)

private fun verdictOf(result: RankedConfig?): Verdict = when {
    result == null -> Verdict("Не проверен", Nf.TextSub, Nf.Muted)
    result.isWorking -> Verdict("Работает", Nf.Green, Nf.Green)
    result.totalOk > 0 -> Verdict("Работает частично", Nf.Yellow, Nf.Yellow)
    else -> Verdict("Не работает", Nf.RedText, Nf.Red)
}

private fun kindIcon(kind: NetworkKind): ImageVector =
    if (kind == NetworkKind.Wifi) Icons.Default.Wifi else Icons.Default.SignalCellularAlt

@Composable
fun YoutubeTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var advanced by remember { mutableStateOf(false) }
    var showAdvancedNotice by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = advanced) { advanced = false }
    if (advanced) {
        Column(modifier = modifier.fillMaxSize().padding(start = if (com.rupleide.netfix.ui.components.LocalIsTv.current) com.rupleide.netfix.ui.components.NfTv.RailWidth else 0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Nf.Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "← Назад к проверке",
                    color = Nf.Bright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .nfTvFocus(corner = 8.dp, grow = 1.03f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { advanced = false }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            Box(modifier = Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) {
                YoutubeAdvancedTab(
                    focusRequester = focusRequester,
                    navBarFocusRequester = navBarFocusRequester,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val animationsEnabled = rememberAnimationsEnabled()
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()

    var scope by remember { mutableStateOf(VerifyScope.Both) }
    var changeKind by remember { mutableStateOf<NetworkKind?>(null) }
    var verifySheetVisible by remember { mutableStateOf(false) }
    val currentKind = com.rupleide.netfix.ui.main.rememberCurrentNetworkKind()
    val verifying = StrategyTestManager.isVerifying
    val selectionVersion = StrategyTestManager.selectionVersion

    val startCheck: (VerifyScope, String?) -> Unit = { target, only ->
        val job = StrategyTestManager.startVerification(context, target, only)
        if (job == null) {
            val message = when {
                StrategyTestManager.isTesting -> "Сейчас идёт сканирование сети"
                StrategyTestManager.isVerifying -> "Проверка уже идёт"
                else -> StrategyTestManager.verifyMessage.ifBlank { "Сначала просканируйте сеть" }
            }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        } else if (changeKind == null) {
            verifySheetVisible = true
        }
    }

    var toast by remember { mutableStateOf<ResultToast?>(null) }
    val finishedAt = StrategyTestManager.verifyFinishedAt
    LaunchedEffect(finishedAt) {
        if (finishedAt > 0L && System.currentTimeMillis() - finishedAt < 3000L) {
            val rows = verifyRows(context)
            if (rows.isNotEmpty()) {
                toast = toastFor(rows)
                kotlinx.coroutines.delay(5000L)
                toast = null
            }
        }
    }

    val screen = when {
        StrategyTestManager.isTesting -> TabScreen.Scanning
        !StrategyTestManager.hasScanResults() -> TabScreen.NoScan
        else -> TabScreen.Ready
    }

    val allWorking = selectionVersion >= 0 && NetworkKind.values().all { kind ->
        StrategyTestManager.resultFor(kind, StrategyTestManager.selectedStrategyFor(context, kind))?.isWorking == true
    }
    val auraTarget = when {
        screen == TabScreen.Scanning -> Nf.Bright.copy(alpha = 0.14f)
        screen == TabScreen.NoScan -> Nf.Bright.copy(alpha = 0.10f)
        verifying -> Nf.Bright.copy(alpha = 0.10f)
        StrategyTestManager.verifyIsError -> Nf.Red.copy(alpha = 0.14f)
        allWorking -> Nf.Green.copy(alpha = 0.16f)
        else -> Nf.Bright.copy(alpha = 0.08f)
    }

    ProvideTextStyle(TextStyle.Default) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Nf.Bg),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 160.dp, y = (-160).dp)
                    .size(360.dp)
                    .nfAura(auraTarget.copy(alpha = auraTarget.alpha * 0.7f))
            )

            AnimatedContent(
                targetState = screen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) +
                        slideInVertically(motionTween(animationsEnabled, MainMotion.ENTER_MS, MainMotion.Decelerate)) { it / 14 })
                        .togetherWith(fadeOut(motionTween(animationsEnabled, MainMotion.EXIT_MS, MainMotion.Accelerate)))
                        .using(SizeTransform(clip = false))
                },
                label = "youtubeScreen"
            ) { shown ->
                when (shown) {
                    TabScreen.Ready -> ReadyScreen(
                        scope = scope,
                        onScopeChange = { scope = it },
                        currentKind = currentKind,
                        onChange = { changeKind = it },
                        onAdvanced = { showAdvancedNotice = true },
                        onVerify = startCheck,
                        bottomReserve = navOverlayReserve,
                        focusRequester = focusRequester,
                        navBarFocusRequester = navBarFocusRequester
                    )
                    else -> ScanStage(
                        scanning = shown == TabScreen.Scanning,
                        bottomReserve = navOverlayReserve,
                        focusRequester = focusRequester,
                        navBarFocusRequester = navBarFocusRequester
                    )
                }
            }

            ChangeSheet(kind = changeKind, onClose = { changeKind = null }, onVerify = startCheck)
            VerifySheet(visible = verifySheetVisible, onClose = { verifySheetVisible = false })
            NfBottomSheet(
                visible = showAdvancedNotice,
                onDismiss = { showAdvancedNotice = false },
                icon = Icons.Default.Warning,
                iconTint = Nf.Yellow,
                title = "Расширенный режим",
                subtitle = "Устаревшая вкладка",
                closeLabel = "Закрыть"
            ) { entry ->
                Text(
                    text = "Это старая вкладка. Она больше не поддерживается и обновляться не будет. Используйте её на своё усмотрение.",
                    color = Nf.TextSub,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = entry(0).fillMaxWidth()
                )
                NfButton(
                    text = "Всё равно открыть",
                    style = NfButtonStyle.Soft,
                    modifier = entry(1),
                    onClick = {
                        showAdvancedNotice = false
                        advanced = true
                    }
                )
            }
            ResultToastHost(toast = toast, onDismiss = { toast = null }, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun NfCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Nf.Line,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, borderColor, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun StatusDot(color: Color, size: Dp = 8.dp) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
private fun StatusLine(text: String, color: Color, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(dotColor)
        Text(text = text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun Tag(
    text: String,
    background: Color,
    color: Color,
    height: Dp = 24.dp,
    size: Int = 12,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = if (height == 24.dp) 10.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.Bold, fontSize = size.sp, maxLines = 1)
    }
}

@Composable
private fun ReadyScreen(
    scope: VerifyScope,
    onScopeChange: (VerifyScope) -> Unit,
    currentKind: NetworkKind?,
    onChange: (NetworkKind) -> Unit,
    onAdvanced: () -> Unit,
    onVerify: (VerifyScope, String?) -> Unit,
    bottomReserve: Dp,
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester
) {
    val context = LocalContext.current
    val ordered = NetworkKind.values().sortedByDescending { it == currentKind }
    var rescanSheetVisible by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (LocalIsTv.current) {
            NfTvTwoPane(
                title = "YouTube и Discord",
                rightTitle = "Wi-Fi",
                left = {
                    CheckCard(VerifyScope.Wifi, onScopeChange, onVerify, focusRequester, navBarFocusRequester)
                    NfTvButton(
                        text = "Сканировать сеть заново",
                        style = NfButtonStyle.Ghost,
                        size = NfTvSize.S,
                        onClick = { rescanSheetVisible = true }
                    )
                    NfTvButton(
                        text = "Расширенный режим",
                        style = NfButtonStyle.Ghost,
                        size = NfTvSize.S,
                        onClick = onAdvanced
                    )
                },
                right = {
                    NetworkCard(
                        kind = NetworkKind.Wifi,
                        isCurrent = currentKind == NetworkKind.Wifi,
                        dimmed = false,
                        onChange = { onChange(NetworkKind.Wifi) },
                        onVerify = onVerify
                    )
                }
            )
        } else LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = bottomReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                Text(
                    text = "YouTube и Discord",
                    color = Nf.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    lineHeight = 29.sp,
                    modifier = Modifier.nfEnter(0).padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                )
            }

            items(ordered, key = { it.name }) { kind ->
                NetworkCard(
                    kind = kind,
                    isCurrent = currentKind == kind,
                    dimmed = currentKind != null && currentKind != kind,
                    onChange = { onChange(kind) },
                    onVerify = onVerify,
                    modifier = Modifier.nfEnter(ordered.indexOf(kind) + 1).animateItem()
                )
            }

            item(key = "check") {
                Box(modifier = Modifier.nfEnter(3)) {
                    CheckCard(scope, onScopeChange, onVerify, focusRequester, navBarFocusRequester)
                }
            }

            item(key = "links") {
                Row(
                    modifier = Modifier.nfEnter(4).fillMaxWidth().height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LinkText("Сканировать сеть заново") { rescanSheetVisible = true }
                    LinkText("Расширенный режим", onClick = onAdvanced)
                }
            }
        }
        NfBottomSheet(
            visible = rescanSheetVisible,
            onDismiss = { rescanSheetVisible = false },
            icon = Icons.Default.Search,
            title = "Сканировать заново?",
            subtitle = "Заново проверим способы для вашей сети. Это займёт пару минут.",
            closeLabel = "Отмена"
        ) { entry ->
            NfButton("Сканировать сеть", style = NfButtonStyle.Secondary, modifier = entry(0), onClick = {
                rescanSheetVisible = false
                StrategyTestManager.startTesting(context)
            })
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Nf.TextSub,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier
            .nfTvFocus(corner = 8.dp, grow = 1.03f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp)
    )
}

@Composable
private fun NetworkCard(
    kind: NetworkKind,
    isCurrent: Boolean,
    dimmed: Boolean,
    onChange: () -> Unit,
    onVerify: (VerifyScope, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectionVersion = StrategyTestManager.selectionVersion
    val selected = if (selectionVersion >= 0) StrategyTestManager.selectedStrategyFor(context, kind) else null
    val result = StrategyTestManager.resultFor(kind, selected)
    val busy = StrategyTestManager.isVerifying

    val kinds = StrategyTestManager.verifyKinds
    val index = kinds.indexOf(kind)
    val perKind = if (kinds.isEmpty()) 1 else (StrategyTestManager.verifyTotal / kinds.size).coerceAtLeast(1)
    val doneHere = (StrategyTestManager.verifyDone - index * perKind).coerceIn(0, perKind)
    val inRun = busy && index >= 0
    val waiting = inRun && StrategyTestManager.verifyDone < index * perKind
    val checking = inRun && !waiting && doneHere < perKind
    val needsNetwork = busy && StrategyTestManager.verifyNeeds == kind
    val showBar = checking && !needsNetwork

    val verdict = verdictOf(result)
    val statusText: String
    val statusColor: Color
    val dotColor: Color
    when {
        needsNetwork -> {
            statusText = if (kind == NetworkKind.Wifi) "Включите Wi-Fi" else "Включите мобильные данные"
            statusColor = Nf.Yellow
            dotColor = Nf.Yellow
        }
        waiting -> {
            statusText = "Не проверен · проверим следующей"
            statusColor = Nf.TextSub
            dotColor = Nf.Muted
        }
        checking -> {
            statusText = if (perKind > 1) "Проверяем $doneHere из $perKind" else "Проверяем…"
            statusColor = Nf.Bright
            dotColor = Nf.Bright
        }
        selected == null -> {
            statusText = "Не проверен"
            statusColor = Nf.TextSub
            dotColor = Nf.Muted
        }
        else -> {
            statusText = verdict.label + if (result != null) " · открыто ${result.totalOk} из ${result.total} адресов" else ""
            statusColor = verdict.textColor
            dotColor = verdict.dotColor
        }
    }

    val best = StrategyTestManager.verifyResults[kind]?.firstOrNull()?.strategy
    val recommended = result != null && sameStrategy(best, selected)
    val failing = result != null && result.totalOk == 0 && !busy
    val borderColor = when {
        checking || needsNetwork -> Nf.Bright.copy(alpha = 0.35f)
        failing && !dimmed -> Nf.Red.copy(alpha = 0.40f)
        isCurrent -> Nf.Green.copy(alpha = 0.45f)
        else -> Nf.Line
    }
    val inactive = dimmed && !checking && !needsNetwork
    val shownStatusColor = if (inactive) Nf.TextSub else statusColor
    val shownDotColor = if (inactive) Nf.Muted else dotColor
    val cardAlpha by animateFloatAsState(
        targetValue = if (dimmed) 0.72f else 1f,
        animationSpec = tween(400),
        label = "networkCardAlpha"
    )

    val animatedBorder by animateColorAsState(borderColor, tween(450), label = "networkBorder")
    val animatedDot by animateColorAsState(shownDotColor, tween(450), label = "networkDot")
    val animatedStatus by animateColorAsState(shownStatusColor, tween(450), label = "networkStatus")

    NfCard(modifier = modifier.height(176.dp).graphicsLayer { alpha = cardAlpha }, borderColor = animatedBorder) {
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusDot(animatedDot, 10.dp)
            Text(
                text = kind.displayName,
                color = Nf.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = isCurrent,
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(250))
            ) {
                Tag("Вы здесь", Nf.Green.copy(alpha = 0.12f), Nf.GreenText, borderColor = Nf.Green.copy(alpha = 0.65f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selected != null) StrategyTestManager.getStrategyName(selected, context) else "Способ не выбран",
                    color = Nf.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (recommended) Tag("Рекомендуем", Color(0x12FFFFFF), Nf.TextSub, height = 22.dp, size = 11)
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showBar) {
                    NfProgress(
                        fraction = if (perKind > 1) doneHere / perKind.toFloat() else 0f,
                        modifier = Modifier.weight(1f),
                        indeterminate = true
                    )
                    Text(
                        text = statusText,
                        color = animatedStatus,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = statusText,
                        color = animatedStatus,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NfButton(
                text = "Проверить",
                style = NfButtonStyle.Secondary,
                enabled = !busy && selected != null && !dimmed,
                modifier = Modifier.weight(1f),
                onClick = {
                    val target = if (kind == NetworkKind.Wifi) VerifyScope.Wifi else VerifyScope.Mobile
                    onVerify(target, selected)
                }
            )
            NfButton(
                text = "Сменить",
                style = NfButtonStyle.Secondary,
                enabled = !busy && !StrategyTestManager.isTesting && !dimmed,
                modifier = Modifier.weight(1f),
                onClick = onChange
            )
        }
    }
}

@Composable
private fun CheckCard(
    scope: VerifyScope,
    onScopeChange: (VerifyScope) -> Unit,
    onVerify: (VerifyScope, String?) -> Unit,
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester
) {
    val busy = StrategyTestManager.isVerifying
    val failure = !busy && StrategyTestManager.verifyIsError && StrategyTestManager.verifyMessage.isNotBlank()
    val tvCard = LocalIsTv.current
    NfCard(modifier = if (tvCard) Modifier else Modifier.height(214.dp)) {
        Text(
            "Проверка конфигов",
            color = Nf.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            maxLines = 1,
            modifier = Modifier.height(22.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (failure) {
                StatusDot(Nf.Red)
                Text(
                    "Проверка прервалась: ${StrategyTestManager.verifyMessage.replace('\n', ' ')}",
                    color = Nf.RedText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "Найдём самый надёжный способ.",
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1
                )
            }
        }

        if (!tvCard) {
            NfSegmented(
                options = listOf("Wi-Fi", "Мобильная", "Обе сети"),
                selectedIndex = scope.ordinal,
                enabled = !busy,
                indicatorColor = Nf.Blue,
                selectedTextColor = Color.White,
                onSelect = { onScopeChange(VerifyScope.values()[it]) }
            )
        }

        NfButton(
            text = when {
                busy -> "Идёт проверка…"
                failure -> "Проверить ещё раз"
                else -> "Проверить"
            },
            style = NfButtonStyle.Soft,
            enabled = !busy,
            onClick = { onVerify(scope, null) },
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusProperties { down = navBarFocusRequester }
        )
    }
}

@Composable
private fun StageIllustration(icon: ImageVector, tint: Color, aura: Color, iconBackground: Color, iconBorder: Color, spinning: Boolean) {
    val animationsEnabled = rememberAnimationsEnabled()
    val compact = LocalConfiguration.current.screenHeightDp < 700
    val height = if (compact) 170.dp else 250.dp
    val spin = rememberInfiniteTransition(label = "stageSpin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "stageAngle"
    )
    Box(modifier = Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(height).nfAura(aura))
        Canvas(modifier = Modifier.size(height * 0.86f)) {
            val stroke = Stroke(width = 1.dp.toPx())
            drawCircle(Color(0x14FFFFFF), radius = size.minDimension / 2f, style = stroke)
            drawCircle(Color(0x14FFFFFF), radius = size.minDimension / 2f * 0.72f, style = stroke)
            drawCircle(Color(0x0DFFFFFF), radius = size.minDimension / 2f * 0.44f, style = stroke)
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(iconBackground)
                .border(1.dp, iconBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(32.dp)
                    .then(if (spinning) Modifier.rotate(if (animationsEnabled) angle else 0f) else Modifier.nfFloat())
            )
        }
    }
}

@Composable
private fun ScanStage(
    scanning: Boolean,
    bottomReserve: Dp,
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = bottomReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "NetFix",
                    color = Nf.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }

            StageIllustration(
                icon = if (scanning) Icons.Default.Refresh else Icons.Default.Search,
                tint = Nf.Text,
                aura = if (scanning) Nf.Bright.copy(alpha = 0.16f) else Nf.Bright.copy(alpha = 0.10f),
                iconBackground = Nf.Glass,
                iconBorder = if (scanning) Nf.Bright.copy(alpha = 0.5f) else Color(0x24FFFFFF),
                spinning = scanning
            )

            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (scanning && StrategyTestManager.scanFinalTotal > 0) "Уточняем результат…" else if (scanning) "Ищем способ…" else "Подберите способ обхода",
                    color = Nf.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    lineHeight = 31.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (scanning && StrategyTestManager.scanFinalTotal > 0) "Все способы проверены. Сейчас подробно проверяем лучшие. Это займёт около минуты." else if (scanning) "Можно свернуть приложение, проверка продолжится." else "NetFix сам найдёт способ, который работает у вашего интернета.",
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (scanning) {
                val total = StrategyTestManager.totalStrategiesCount
                val finalTotal = StrategyTestManager.scanFinalTotal
                val inFinal = finalTotal > 0
                val done = if (inFinal) StrategyTestManager.scanFinalDone else StrategyTestManager.currentTestIndex
                val shownTotal = if (inFinal) finalTotal else total
                val fraction = if (shownTotal > 0) done / shownTotal.toFloat() else 0f
                val found = StrategyTestManager.testResults.count { StrategyTester.isSuccessfulStatus(it.third) }
                NfCard(modifier = Modifier.height(104.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (inFinal) "Уточняем лучшие: $done из $finalTotal" else "Проверено $done из $total",
                            color = Nf.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            maxLines = 1
                        )
                        Text(
                            "${(fraction * 100).toInt()}%",
                            color = Nf.Bright,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    NfProgress(fraction, height = 8, indeterminate = inFinal && done == 0)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (inFinal) {
                            Text(
                                "Первый этап ($total из $total) завершён",
                                color = Nf.TextSub,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        } else {
                            StatusDot(if (found > 0) Nf.Green else Nf.Muted)
                            Text(
                                if (found > 0) "Уже нашли $found рабочих" else "Рабочих пока нет",
                                color = if (found > 0) Nf.Green else Nf.TextSub,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (scanning) {
                var confirmFinish by remember { mutableStateOf(false) }
                LaunchedEffect(confirmFinish) {
                    if (confirmFinish) {
                        kotlinx.coroutines.delay(4000L)
                        confirmFinish = false
                    }
                }
                if (StrategyTestManager.finishRequested) {
                    NfButton(
                        text = "Завершаем…",
                        style = NfButtonStyle.Secondary,
                        enabled = false,
                        onClick = {}
                    )
                } else if (StrategyTestManager.canFinishEarly) {
                    NfButton(
                        text = if (confirmFinish) "Точно завершить?" else "Завершить досрочно",
                        style = if (confirmFinish) NfButtonStyle.DangerSolid else NfButtonStyle.Secondary,
                        onClick = {
                            if (confirmFinish) {
                                confirmFinish = false
                                StrategyTestManager.finishScanEarly()
                            } else {
                                confirmFinish = true
                            }
                        }
                    )
                }
            } else {
                Text(
                    text = "Займёт 2–3 минуты",
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                NfButton(
                    text = "Сканировать сеть",
                    onClick = { StrategyTestManager.startTesting(context) },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusProperties { down = navBarFocusRequester }
                )
            }
        }
    }
}

@Composable
private fun SheetProgress(
    fraction: Float,
    color: Color?,
    caption: String,
    captionColor: Color,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NfProgress(fraction, color = color, height = 8, indeterminate = indeterminate)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(caption, color = Nf.TextSub, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                "${(fraction * 100).toInt()}%",
                color = captionColor,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

private fun verifyRows(context: android.content.Context): List<VerifyRow> {
    val single = StrategyTestManager.verifySingle
    return StrategyTestManager.verifyKinds.mapNotNull { kind ->
        val result = (if (single != null) {
            StrategyTestManager.resultFor(kind, single)
        } else {
            StrategyTestManager.verifyResults[kind]?.firstOrNull()
        }) ?: return@mapNotNull null
        VerifyRow(
            kind = kind,
            strategyName = StrategyTestManager.getStrategyName(result.strategy, context),
            verdict = verdictOf(result),
            totalOk = result.totalOk,
            total = result.total
        )
    }
}

@Composable
private fun ResultRow(row: VerifyRow, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(Nf.Surface)
            .border(1.dp, Nf.LineSoft, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = kindIcon(row.kind), contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.kind.displayName, color = Nf.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(row.strategyName, color = Nf.TextSub, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(row.verdict.dotColor)
                Text(if (row.verdict.label == "Работает частично") "Частично" else row.verdict.label, color = row.verdict.textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Text("${row.totalOk} из ${row.total} адресов", color = Nf.TextSub, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

private data class SheetHead(
    val icon: ImageVector,
    val tint: Color?,
    val spin: Boolean,
    val title: String,
    val subtitle: String,
    val close: String
)

@Composable
private fun VerifySheet(visible: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val verifying = StrategyTestManager.isVerifying
    val stopping = verifying && StrategyTestManager.verifyStopping
    val needs = StrategyTestManager.verifyNeeds
    val selectionVersion = StrategyTestManager.selectionVersion
    val done = StrategyTestManager.verifyDone
    val total = StrategyTestManager.verifyTotal
    val fraction = if (total > 0) done / total.toFloat() else 0f
    val cancelled = !verifying && StrategyTestManager.verifyCancelled
    val rows = if (verifying || cancelled || selectionVersion < 0) emptyList() else verifyRows(context)
    val failure = if (!verifying && StrategyTestManager.verifyIsError) StrategyTestManager.verifyMessage else ""
    val allFailed = rows.isEmpty() && failure.isNotBlank()

    val head = when {
        stopping -> SheetHead(Icons.Default.Refresh, Nf.Bright, true, "Останавливаем…", "Подождите пару секунд", "Свернуть")
        verifying && needs != null -> SheetHead(
            Icons.Default.WifiOff,
            Nf.Yellow,
            false,
            if (needs == NetworkKind.Wifi) "Включите Wi-Fi" else "Включите мобильные данные",
            "Проверка продолжится сама, как только сеть появится.",
            "Свернуть"
        )
        verifying -> SheetHead(
            Icons.Default.Refresh,
            Nf.Bright,
            true,
            "Идёт проверка",
            StrategyTestManager.verifyMessage.ifBlank { "Проверяем адреса YouTube и Discord" },
            "Свернуть"
        )
        rows.isNotEmpty() || allFailed -> SheetHead(
            if (allFailed) Icons.Default.Warning else Icons.Default.Check,
            if (allFailed) Nf.Yellow else Nf.Green,
            false,
            if (allFailed) "Проверка не удалась" else "Проверка завершена",
            when {
                allFailed -> failure.replace('\n', ' ')
                StrategyTestManager.verifySingle != null -> "Результат проверки выбранного способа."
                else -> "Лучшие способы уже выбраны."
            },
            "Готово"
        )
        else -> SheetHead(Icons.Default.Close, null, false, "Проверка остановлена", "Результаты не изменились.", "Закрыть")
    }

    NfBottomSheet(
        visible = visible,
        onDismiss = onClose,
        icon = head.icon,
        title = head.title,
        subtitle = head.subtitle,
        closeLabel = head.close,
        iconTint = head.tint,
        spinIcon = head.spin
    ) { entry ->
        when {
            stopping -> {
                SheetProgress(fraction, null, "Останавливаем проверку", Nf.Bright, entry(0), indeterminate = true)
            }
            verifying && needs != null -> {
                SheetProgress(fraction, Nf.Yellow, "На паузе · готово $done из $total", Nf.Yellow, entry(0))
                NfButton("Открыть настройки", style = NfButtonStyle.Secondary, modifier = entry(1), onClick = { StrategyTestManager.openConnectivitySettings(context) })
                NfButton("Остановить", style = NfButtonStyle.Secondary, modifier = entry(2), onClick = { StrategyTestManager.stopVerification() })
            }
            verifying -> {
                SheetProgress(fraction, null, "Готово $done из $total", Nf.Bright, entry(0), indeterminate = true)
                NfButton("Остановить", style = NfButtonStyle.Secondary, modifier = entry(1), onClick = { StrategyTestManager.stopVerification() })
            }
            rows.isNotEmpty() -> {
                rows.forEachIndexed { index, row -> ResultRow(row, entry(minOf(index, 3))) }
                if (failure.isNotBlank()) StatusLine(failure.replace('\n', ' '), Nf.RedText, Nf.Red)
            }
            else -> Unit
        }
    }
}

@Composable
private fun CheckProgressBanner(name: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(Nf.Bright.copy(alpha = 0.08f))
            .border(1.dp, Nf.Bright.copy(alpha = 0.35f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text("Проверяем «$name»…", color = Nf.Bright, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        NfProgress(0f, indeterminate = true)
        Text("Открываем адреса YouTube и Discord", color = Nf.TextSub, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

@Composable
private fun CheckResultBanner(name: String, result: RankedConfig, modifier: Modifier = Modifier) {
    val verdict = verdictOf(result)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(verdict.dotColor.copy(alpha = 0.10f))
            .border(1.dp, verdict.dotColor.copy(alpha = 0.35f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(verdict.dotColor)
            Text("$name · ${verdict.label}", color = verdict.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(
            "YouTube ${result.youtubeOk}/${result.youtubeTotal} · Discord ${result.discordOk}/${result.discordTotal} · открыто ${result.totalOk} из ${result.total}",
            color = Nf.TextSub,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

private const val ADD_FORM_KEY = "\u0000add"

@Composable
private fun ChangeSheet(kind: NetworkKind?, onClose: () -> Unit, onVerify: (VerifyScope, String?) -> Unit) {
    val context = LocalContext.current
    var shownKind by remember { mutableStateOf(kind ?: NetworkKind.Wifi) }
    var order by remember { mutableStateOf<List<String>>(emptyList()) }
    var checked by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var addName by remember { mutableStateOf("") }
    var addCommand by remember { mutableStateOf("") }
    LaunchedEffect(kind) {
        if (kind != null) {
            shownKind = kind
            val selectedNow = StrategyTestManager.selectedStrategyFor(context, kind)
            order = StrategyTestManager.configChoices(kind).sortedByDescending { sameStrategy(it, selectedNow) }
            checked = null
            editing = null
            adding = false
            addName = ""
            addCommand = ""
        }
    }
    val selectionVersion = StrategyTestManager.selectionVersion
    val busy = StrategyTestManager.isVerifying
    val selected = if (selectionVersion >= 0) StrategyTestManager.selectedStrategyFor(context, shownKind) else null
    val liveChoices = StrategyTestManager.configChoices(shownKind).sortedByDescending { sameStrategy(it, selected) }
    val choices = if (order.isEmpty()) liveChoices else order
    val failure = !busy && StrategyTestManager.verifyIsError && StrategyTestManager.verifyMessage.isNotBlank()
    val empty = choices.isEmpty() && !adding
    val target = checked
    val targetName = target?.let { StrategyTestManager.getStrategyName(it, context) }
    val targetResult = if (target != null && selectionVersion >= 0) StrategyTestManager.resultFor(shownKind, target) else null

    val editingStrategy = if (empty || adding) null else editing
    val showDetail = editingStrategy != null
    val currentName = editingStrategy?.let { StrategyTestManager.getStrategyName(it, context) }.orEmpty()
    val nameChanged = showDetail && nameDraft.trim() != currentName

    val addReady = adding && addName.isNotBlank() && addCommand.isNotBlank()
    val saveAdd: () -> Unit = {
        when (val result = StrategyTestManager.addCustomStrategy(context, addCommand, addName.trim().ifBlank { null }, null)) {
            is StrategyTestManager.AddCustomStrategyResult.Added -> {
                android.widget.Toast.makeText(context, "Способ сохранён", android.widget.Toast.LENGTH_SHORT).show()
                val selectedNow = StrategyTestManager.selectedStrategyFor(context, shownKind)
                order = StrategyTestManager.configChoices(shownKind).sortedByDescending { sameStrategy(it, selectedNow) }
                adding = false
                addName = ""
                addCommand = ""
            }
            StrategyTestManager.AddCustomStrategyResult.Invalid ->
                android.widget.Toast.makeText(context, "Адрес не распознан", android.widget.Toast.LENGTH_SHORT).show()
            is StrategyTestManager.AddCustomStrategyResult.AlreadyExists ->
                android.widget.Toast.makeText(context, "Такой способ уже есть: ${result.existingName}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val commitAndBack: () -> Unit = {
        if (editingStrategy != null && nameChanged) {
            StrategyTestManager.renameStrategy(context, editingStrategy, nameDraft.trim().ifBlank { null })
        }
        editing = null
    }

    NfBottomSheet(
        visible = kind != null,
        onDismiss = when {
            adding -> ({ adding = false })
            showDetail -> commitAndBack
            else -> onClose
        },
        icon = when {
            adding -> Icons.Default.Add
            empty -> Icons.Default.Search
            showDetail -> Icons.Default.Settings
            else -> kindIcon(shownKind)
        },
        title = when {
            adding -> "Свой способ"
            empty -> "Пока нечего выбрать"
            showDetail -> currentName
            shownKind == NetworkKind.Wifi -> "Способ для Wi-Fi"
            else -> "Способ для мобильной сети"
        },
        subtitle = when {
            adding -> "Название и командная строка способа."
            empty -> "Просканируйте сеть, и способы появятся здесь."
            showDetail -> "Название, проверка и удаление способа."
            else -> "Нажмите, чтобы выбрать способ"
        },
        closeLabel = when {
            adding && addReady -> "Готово"
            adding -> "Назад"
            empty -> "Закрыть"
            showDetail && !nameChanged -> "Назад"
            else -> "Готово"
        },
        maxHeightFraction = 0.66f,
        fixedHeight = !empty && !showDetail && !adding,
        closeHighlighted = nameChanged || addReady,
        onConfirm = if (addReady) saveAdd else null,
        scrollResetKey = if (adding) ADD_FORM_KEY else editingStrategy,
        footer = if (!empty) {
            { entry ->
                AnimatedVisibility(
                    visible = !showDetail && !adding,
                    enter = fadeIn(tween(220)),
                    exit = ExitTransition.None
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        AddStrategyRow(modifier = entry(5), enabled = !busy, onClick = { adding = true })
                    }
                }
            }
        } else {
            null
        }
    ) { entry ->
        if (empty) {
            NfButton("Сканировать сеть", style = NfButtonStyle.Secondary, modifier = entry(0), onClick = {
                onClose()
                StrategyTestManager.startTesting(context)
            })
        } else {
            AnimatedContent(
                targetState = if (adding) ADD_FORM_KEY else editingStrategy,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    val forward = targetState != null
                    (fadeIn(tween(260, delayMillis = 60)) +
                        slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { if (forward) it / 5 else -it / 5 })
                        .togetherWith(
                            fadeOut(tween(160)) +
                                slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) -it / 5 else it / 5 }
                        )
                        .using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                label = "sheetBody"
            ) { screenKey ->
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (screenKey == ADD_FORM_KEY) {
                        AddStrategyForm(
                            name = addName,
                            command = addCommand,
                            onNameChange = { addName = it.take(40) },
                            onCommandChange = { addCommand = it },
                            entry = entry
                        )
                    } else if (screenKey != null) {
                        ConfigDetail(
                            kind = shownKind,
                            strategy = screenKey,
                            busy = busy,
                            checking = busy && sameStrategy(screenKey, target),
                            nameText = nameDraft,
                            onNameChange = { nameDraft = it.take(40) },
                            entry = entry,
                            onVerify = { scope, only ->
                                checked = only
                                onVerify(scope, only)
                            },
                            onSelected = onClose,
                            onDeleted = {
                                order = order.filter { !sameStrategy(it, screenKey) }
                                editing = null
                            }
                        )
                    } else {
                        if (targetName != null && busy) {
                            CheckProgressBanner(targetName, entry(0))
                        } else if (targetName != null && targetResult != null) {
                            CheckResultBanner(targetName, targetResult, entry(0))
                        }
                        if (failure) {
                            val shape = RoundedCornerShape(14.dp)
                            Row(
                                modifier = entry(0)
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(shape)
                                    .background(Nf.Red.copy(alpha = 0.10f))
                                    .border(1.dp, Nf.Red.copy(alpha = 0.30f), shape)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Nf.RedText, modifier = Modifier.size(20.dp))
                                Text(
                                    "Не удалось проверить: ${StrategyTestManager.verifyMessage.replace('\n', ' ')}",
                                    color = Nf.RedText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        choices.forEachIndexed { index, strategy ->
                            ConfigRow(
                                kind = shownKind,
                                strategy = strategy,
                                isSelected = sameStrategy(strategy, selected),
                                busy = busy,
                                checking = busy && sameStrategy(strategy, target),
                                modifier = entry(minOf(index + 1, 4)),
                                onSelected = onClose,
                                onOpen = {
                                    nameDraft = StrategyTestManager.getStrategyName(strategy, context)
                                    editing = strategy
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStrategyRow(modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val dashColor = if (focused) Color.White else Color(0x38FFFFFF)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .nfTvFocus(corner = 14.dp, wide = true)
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
                    )
                )
            }
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) {
                haptics(NetFixHaptic.Tap)
                onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .drawBehind {
                    drawCircle(
                        color = Color(0x47FFFFFF),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Nf.Text, modifier = Modifier.size(14.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Добавить свой способ",
                color = Nf.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Своя командная строка",
                color = Nf.TextSub,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AddStrategyForm(
    name: String,
    command: String,
    onNameChange: (String) -> Unit,
    onCommandChange: (String) -> Unit,
    entry: @Composable (Int) -> Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NfInputField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Название способа",
            modifier = entry(0)
        )
        Box(
            modifier = entry(1)
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .nfTvFocus(corner = 12.dp, grow = 1f)
                .clip(shape)
                .background(Nf.Surface)
                .border(1.dp, Nf.Line, shape)
                .padding(14.dp)
        ) {
            if (command.isEmpty()) {
                Text("Командная строка", color = Nf.TextOff, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            BasicTextField(
                value = command,
                onValueChange = onCommandChange,
                textStyle = TextStyle(color = Nf.Text, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Nf.Bright),
                modifier = Modifier
                    .fillMaxWidth()
                    .nfTvKeyboardOnCenter()
            )
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(Nf.Green)
                else Modifier.border(2.dp, Color(0x47FFFFFF), CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun GearButton(enabled: Boolean, onClick: () -> Unit) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(shape)
            .background(Nf.SurfacePressed)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Nf.Line, shape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) {
                haptics(NetFixHaptic.Tap)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Settings, contentDescription = "Настроить способ", tint = Color(0xFFA1A1A1), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ConfigRow(
    kind: NetworkKind,
    strategy: String,
    isSelected: Boolean,
    busy: Boolean,
    checking: Boolean,
    modifier: Modifier,
    onSelected: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val result = StrategyTestManager.resultFor(kind, strategy)
    val verdict = verdictOf(result)
    val failing = result != null && result.totalOk == 0 && !checking
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        isSelected -> Nf.Green.copy(alpha = 0.8f)
        checking -> Nf.Bright.copy(alpha = 0.45f)
        failing -> Nf.Red.copy(alpha = 0.40f)
        else -> Color(0x14FFFFFF)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer { alpha = if (busy && !checking) 0.6f else 1f }
            .nfTvFocus(corner = 14.dp, inside = true)
            .clip(shape)
            .background(if (isSelected) Nf.Green.copy(alpha = 0.07f) else Nf.Surface)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !busy
                ) {
                    if (!isSelected) StrategyTestManager.chooseConfigFor(context, kind, strategy)
                    onSelected()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        RadioMark(isSelected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = StrategyTestManager.getStrategyName(strategy, context),
                color = Nf.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (checking) {
                    NfProgress(0f, modifier = Modifier.width(56.dp), height = 4, indeterminate = true)
                }
                Text(
                    text = when {
                        checking -> "Проверяем…"
                        result == null -> "Не проверен"
                        else -> "YouTube ${result.youtubeOk}/${result.youtubeTotal} · Discord ${result.discordOk}/${result.discordTotal}"
                    },
                    color = if (checking) Nf.Bright else verdict.textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        }
        GearButton(enabled = !busy, onClick = onOpen)
    }
}

@Composable
private fun ConfigDetail(
    kind: NetworkKind,
    strategy: String,
    busy: Boolean,
    checking: Boolean,
    nameText: String,
    onNameChange: (String) -> Unit,
    entry: @Composable (Int) -> Modifier,
    onVerify: (VerifyScope, String?) -> Unit,
    onSelected: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val selectionVersion = StrategyTestManager.selectionVersion
    val result = StrategyTestManager.resultFor(kind, strategy)
    val verdict = verdictOf(result)
    val isSelected = sameStrategy(StrategyTestManager.selectedStrategyFor(context, kind), strategy)
    val usedByAnyNetwork = selectionVersion >= 0 && NetworkKind.values().any {
        sameStrategy(StrategyTestManager.selectedStrategyFor(context, it), strategy)
    }
    val canDelete = !usedByAnyNetwork && !busy
    var confirmDelete by remember(strategy) { mutableStateOf(false) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            kotlinx.coroutines.delay(4000L)
            confirmDelete = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Crossfade(targetState = checking, animationSpec = tween(220), label = "plate") { isChecking ->
    if (isChecking) {
        CheckProgressBanner(StrategyTestManager.getStrategyName(strategy, context), entry(0))
    } else {
        val shape = RoundedCornerShape(14.dp)
        Column(
            modifier = entry(0)
                .fillMaxWidth()
                .height(88.dp)
                .clip(shape)
                .background(verdict.dotColor.copy(alpha = if (result == null) 0.06f else 0.10f))
                .border(1.dp, verdict.dotColor.copy(alpha = if (result == null) 0.20f else 0.35f), shape)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            StatusLine(
                text = verdict.label + if (result != null) " · открыто ${result.totalOk} из ${result.total}" else "",
                color = verdict.textColor,
                dotColor = verdict.dotColor
            )
            Text(
                text = if (result != null) {
                    "${kind.displayName} · YouTube ${result.youtubeOk}/${result.youtubeTotal} · Discord ${result.discordOk}/${result.discordTotal}"
                } else {
                    "${kind.displayName} · нажмите «Проверить»"
                },
                color = Nf.TextSub,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
    }

    NfInputField(
        value = nameText,
        onValueChange = onNameChange,
        placeholder = "Название способа",
        modifier = entry(1)
    )

    Row(modifier = entry(2).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NfButton(
            text = "Проверить",
            style = NfButtonStyle.Soft,
            enabled = !busy,
            modifier = Modifier.weight(1f),
            onClick = {
                val scope = if (kind == NetworkKind.Wifi) VerifyScope.Wifi else VerifyScope.Mobile
                onVerify(scope, strategy)
            }
        )
        NfButton(
            text = if (isSelected) "Выбран" else "Выбрать",
            style = if (isSelected) NfButtonStyle.Chosen else NfButtonStyle.Soft,
            enabled = !isSelected && !busy,
            modifier = Modifier.weight(1f),
            onClick = {
                StrategyTestManager.chooseConfigFor(context, kind, strategy)
                onSelected()
            }
        )
    }
    }

    NfButton(
        text = if (canDelete && confirmDelete) "Точно удалить?" else "Удалить способ",
        style = when {
            !canDelete -> NfButtonStyle.Secondary
            confirmDelete -> NfButtonStyle.DangerSolid
            else -> NfButtonStyle.Danger
        },
        enabled = canDelete,
        modifier = entry(3),
        onClick = {
            if (confirmDelete) {
                StrategyTestManager.deleteStrategy(context, strategy)
                onDeleted()
            } else {
                confirmDelete = true
            }
        }
    )
    }
}

private data class ResultToast(
    val title: String,
    val lines: List<String>,
    val tint: Color,
    val icon: ImageVector
)

private fun toastFor(rows: List<VerifyRow>): ResultToast {
    val allWorking = rows.all { it.verdict.label == "Работает" }
    val noneWorking = rows.all { it.totalOk == 0 }
    return ResultToast(
        title = when {
            allWorking -> "Проверка прошла успешно"
            noneWorking -> "Способ не работает"
            else -> "Проверка завершена"
        },
        lines = rows.map { "${if (it.kind == NetworkKind.Wifi) "Wi-Fi" else "Мобильная"} · ${it.strategyName}: открыто ${it.totalOk} из ${it.total}" },
        tint = when {
            allWorking -> Nf.Green
            noneWorking -> Nf.Red
            else -> Nf.Yellow
        },
        icon = if (noneWorking) Icons.Default.Warning else Icons.Default.Check
    )
}

@Composable
private fun ResultToastHost(toast: ResultToast?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var lastShown by remember { mutableStateOf<ResultToast?>(null) }
    if (toast != null) lastShown = toast
    val shown = lastShown
    AnimatedVisibility(
        visible = toast != null,
        modifier = modifier,
        enter = fadeIn(tween(220)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { -it },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it }
    ) {
        if (shown != null) {
            val shape = RoundedCornerShape(20.dp)
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Nf.Sheet)
                    .border(1.dp, shown.tint.copy(alpha = 0.55f), shape)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(shown.tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(shown.icon, contentDescription = null, tint = shown.tint, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(shown.title, color = Nf.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    shown.lines.forEach { line ->
                        Text(line, color = Nf.TextSub, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
