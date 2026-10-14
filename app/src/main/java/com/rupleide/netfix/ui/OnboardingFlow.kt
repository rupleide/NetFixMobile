package com.rupleide.netfix.ui

import com.rupleide.netfix.ui.components.hasLeanbackFeature
import androidx.compose.ui.focus.focusProperties
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.nfTvFocus
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.StrategyTester
import com.rupleide.netfix.core.dpibypass.currentNetworkKind
import com.rupleide.netfix.data.OnboardingStep
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfCornerAura
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.nfAura
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.nfFloat
import com.rupleide.netfix.ui.components.nfHeartbeat
import com.rupleide.netfix.ui.main.rememberAnimationsEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val LocalOnbCompact = compositionLocalOf { false }

private val LocalOnbTv = compositionLocalOf { false }

private enum class OnbMood { Neutral, Success, Warn, Error }

private enum class ScanPhase { Start, Search, Refine, Verify, Stop }

private enum class OnbMark { None, Ok, Off, Warn, Bad }

private class OnbRowData(
    val icon: ImageVector?,
    val title: String,
    val sub: String?,
    val mark: OnbMark = OnbMark.None,
    val dimmed: Boolean = false,
    val iconRes: Int? = null,
    val iconColor: Color = Nf.Text,
    val iconBg: Color = Color(0xFF242424)
)

private fun OnbMood.color(): Color = when (this) {
    OnbMood.Neutral -> Nf.Bright
    OnbMood.Success -> Nf.Green
    OnbMood.Warn -> Nf.Yellow
    OnbMood.Error -> Nf.Red
}

private fun stepFromName(name: String): OnboardingStep = when (name) {
    "Welcome" -> OnboardingStep.Welcome
    "DeviceType" -> OnboardingStep.DeviceType
    "TelegramProxyQuestion" -> OnboardingStep.TelegramProxyQuestion
    "YoutubeBypassQuestion" -> OnboardingStep.YoutubeBypassQuestion
    "StrategyTestingExisting" -> OnboardingStep.StrategyTestingExisting
    "StrategyTestingRunning" -> OnboardingStep.StrategyTestingRunning
    "StrategyTestingResult" -> OnboardingStep.StrategyTestingResult
    "AboutAndSupport" -> OnboardingStep.AboutAndSupport
    "Summary" -> OnboardingStep.Summary
    "FinalGreeting" -> OnboardingStep.FinalGreeting
    else -> OnboardingStep.Welcome
}

private fun segmentOf(step: OnboardingStep): Int = when (step) {
    OnboardingStep.DeviceType -> 0
    OnboardingStep.TelegramProxyQuestion -> 1
    OnboardingStep.YoutubeBypassQuestion -> 2
    OnboardingStep.StrategyTestingExisting,
    OnboardingStep.StrategyTestingRunning,
    OnboardingStep.StrategyTestingResult -> 3
    OnboardingStep.AboutAndSupport -> 4
    OnboardingStep.Summary -> 5
    else -> -1
}

private fun prefsOf(context: Context) =
    context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

@Composable
fun OnboardingFlow(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsEnabled = rememberAnimationsEnabled()
    val density = LocalDensity.current
    val tv = LocalIsTv.current
    val compact = !tv && LocalConfiguration.current.screenHeightDp < 700

    val onboardingStepSaver = Saver<OnboardingStep, String>(
        save = { it::class.java.simpleName },
        restore = { stepFromName(it) }
    )
    val stepsHistorySaver = Saver<SnapshotStateList<OnboardingStep>, List<String>>(
        save = { list -> list.map { it::class.java.simpleName } },
        restore = { names ->
            val list = mutableStateListOf<OnboardingStep>()
            names.forEach { list.add(stepFromName(it)) }
            list
        }
    )
    val nullableBooleanSaver = Saver<Boolean?, Any>(
        save = { it ?: -1 },
        restore = { saved ->
            when (saved) {
                true -> true
                false -> false
                else -> null
            }
        }
    )

    var currentStep by rememberSaveable(stateSaver = onboardingStepSaver) { mutableStateOf<OnboardingStep>(OnboardingStep.Welcome) }
    var wantsTelegramProxy by rememberSaveable(stateSaver = nullableBooleanSaver) { mutableStateOf<Boolean?>(null) }
    var wantsYoutubeBypass by rememberSaveable(stateSaver = nullableBooleanSaver) { mutableStateOf<Boolean?>(null) }
    var deviceIsAndroid by rememberSaveable(stateSaver = nullableBooleanSaver) { mutableStateOf<Boolean?>(null) }
    val stepsHistory = rememberSaveable(saver = stepsHistorySaver) { mutableStateListOf<OnboardingStep>() }
    var forward by remember { mutableStateOf(true) }
    var auraLit by remember { mutableStateOf(false) }

    fun navigateTo(step: OnboardingStep) {
        forward = true
        stepsHistory.add(currentStep)
        currentStep = step
    }

    fun navigateBack() {
        if (stepsHistory.isEmpty()) return
        forward = false
        var target = stepsHistory.removeAt(stepsHistory.size - 1)
        while (target == OnboardingStep.StrategyTestingRunning && stepsHistory.isNotEmpty()) {
            target = stepsHistory.removeAt(stepsHistory.size - 1)
        }
        currentStep = target
    }

    LaunchedEffect(currentStep) {
        auraLit = false
        if (currentStep == OnboardingStep.Welcome) {
            delay(700)
            auraLit = true
        }
    }

    val hasSuccess = StrategyTestManager.testResults.any { StrategyTester.isSuccessfulStatus(it.third) }
    val mood = when (currentStep) {
        OnboardingStep.Welcome -> if (auraLit) OnbMood.Success else OnbMood.Neutral
        OnboardingStep.StrategyTestingExisting,
        OnboardingStep.Summary,
        OnboardingStep.FinalGreeting -> OnbMood.Success
        OnboardingStep.StrategyTestingResult -> when {
            StrategyTestManager.hasConnectionError -> OnbMood.Error
            hasSuccess -> if (resultIsPartial(context)) OnbMood.Warn else OnbMood.Success
            else -> OnbMood.Neutral
        }
        else -> OnbMood.Neutral
    }
    val auraColor = mood.color().copy(alpha = if (mood == OnbMood.Neutral) 0.05f else 0.10f)

    val segment = segmentOf(currentStep)
    val showBar = segment >= 0
    val showBack = showBar && currentStep != OnboardingStep.StrategyTestingRunning

    val slideIn = with(density) { 72.dp.roundToPx() }
    val slideOut = with(density) { 56.dp.roundToPx() }

    androidx.activity.compose.BackHandler(enabled = tv && showBack) { navigateBack() }

    CompositionLocalProvider(LocalOnbCompact provides compact, LocalOnbTv provides tv) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Nf.Bg)
        ) {
            NfCornerAura(auraColor)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = if (tv) 1100.dp else 440.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                if (showBar) {
                    OnbTopBar(
                        segment = segment,
                        checkSkipped = wantsYoutubeBypass == false,
                        showBack = showBack,
                        onBack = ::navigateBack
                    )
                }
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (!animationsEnabled) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val dir = if (forward) 1 else -1
                            (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { slideIn * dir } +
                                fadeIn(tween(300, easing = FastOutSlowInEasing))) togetherWith
                                (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -slideOut * dir } +
                                    fadeOut(tween(200)))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "onboardingTransition"
                ) { step ->
                    when (step) {
                        OnboardingStep.Welcome -> WelcomeStep(onNavigate = ::navigateTo)
                        OnboardingStep.DeviceType -> DeviceTypeStep(
                            onNavigate = ::navigateTo,
                            onSetDeviceIsAndroid = { deviceIsAndroid = it }
                        )
                        OnboardingStep.TelegramProxyQuestion -> TelegramProxyStep(
                            onNavigate = ::navigateTo,
                            deviceIsAndroid = deviceIsAndroid ?: true,
                            onSetTelegramProxy = { wantsTelegramProxy = it }
                        )
                        OnboardingStep.YoutubeBypassQuestion -> YoutubeBypassStep(
                            onNavigate = ::navigateTo,
                            onSetYoutubeBypass = { wantsYoutubeBypass = it },
                            isSmartTv = prefsOf(context).getBoolean("is_smart_tv", false)
                        )
                        OnboardingStep.StrategyTestingExisting -> StrategyTestingExistingStep(onNavigate = ::navigateTo)
                        OnboardingStep.StrategyTestingRunning -> StrategyTestingRunningStep(
                            context = context,
                            onNavigate = ::navigateTo
                        )
                        OnboardingStep.StrategyTestingResult -> StrategyTestingResultStep(onNavigate = ::navigateTo)
                        OnboardingStep.AboutAndSupport -> AboutAndSupportStep(onNavigate = ::navigateTo)
                        OnboardingStep.Summary -> SummaryStep(
                            wantsTelegramProxy = wantsTelegramProxy,
                            wantsYoutubeBypass = wantsYoutubeBypass,
                            deviceIsAndroid = deviceIsAndroid ?: true,
                            onNavigate = ::navigateTo
                        )
                        OnboardingStep.FinalGreeting -> FinalGreetingScreen(onCompleted = onCompleted)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnbTopBar(
    segment: Int,
    checkSkipped: Boolean,
    showBack: Boolean,
    onBack: () -> Unit
) {
    val compact = LocalOnbCompact.current
    val tv = LocalOnbTv.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (tv) 56.dp else if (compact) 40.dp else 44.dp)
            .padding(horizontal = if (tv) 56.dp else if (compact) 16.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            OnbBackButton(onBack)
            Spacer(modifier = Modifier.size(8.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            repeat(6) { index ->
                OnbSegment(
                    index = index,
                    current = segment,
                    collapsed = index == 3 && checkSkipped
                )
            }
        }
    }
}

@Composable
private fun RowScope.OnbSegment(index: Int, current: Int, collapsed: Boolean) {
    val animationsEnabled = rememberAnimationsEnabled()
    val width by animateFloatAsState(
        targetValue = if (collapsed) 0f else 1f,
        animationSpec = tween(if (animationsEnabled) 300 else 0, easing = FastOutSlowInEasing),
        label = "onbSegmentWidth"
    )
    val color by animateColorAsState(
        targetValue = when {
            index < current -> Nf.Bright.copy(alpha = 0.5f)
            index == current -> Nf.Bright
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(if (animationsEnabled) 250 else 0),
        label = "onbSegmentColor"
    )
    Box(
        modifier = Modifier
            .weight(width.coerceAtLeast(0.001f))
            .padding(end = 4.dp * width)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
private fun OnbBackButton(onBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val color by animateColorAsState(
        targetValue = if (pressed || hovered || focused) Color.White else Nf.TextSub,
        animationSpec = tween(150),
        label = "onbBackColor"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(44.dp)
            .nfTvFocus(corner = 12.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onBack)
            .padding(end = 6.dp)
    ) {
        Icon(
            imageVector = OnbIcons.ChevronLeft,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "Назад",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Modifier.onbReveal(visible: Boolean, index: Int = 0): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (animationsEnabled) 300 else 0,
            delayMillis = if (animationsEnabled && visible) index * 40 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "onbReveal"
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 12.dp.toPx()
    }
}

@Composable
private fun rememberEnterGate(): Boolean {
    val animationsEnabled = rememberAnimationsEnabled()
    var ready by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) {
        if (!ready) {
            delay(220)
            ready = true
        }
    }
    return ready
}

@Composable
private fun rememberPrimaryFocus(ready: Boolean): FocusRequester {
    val context = LocalContext.current
    val requester = remember { FocusRequester() }
    val remoteControlled = remember {
        context.packageManager.hasSystemFeature("android.software.leanback") ||
            prefsOf(context).getBoolean("is_smart_tv", false)
    }
    LaunchedEffect(ready) {
        if (ready && remoteControlled) {
            try {
                requester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    return requester
}

@Composable
private fun OnbTitle(text: String, color: Color = Nf.Text, modifier: Modifier = Modifier) {
    val compact = LocalOnbCompact.current
    val tv = LocalOnbTv.current
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = if (tv) 30.sp else if (compact) 22.sp else 24.sp,
        lineHeight = if (tv) 38.sp else if (compact) 28.sp else 31.sp,
        textAlign = if (tv) TextAlign.Start else TextAlign.Center,
        maxLines = 2,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}

@Composable
private fun OnbDescription(text: String, modifier: Modifier = Modifier) {
    val tv = LocalOnbTv.current
    Text(
        text = text,
        color = Nf.TextSub,
        fontWeight = FontWeight.Medium,
        fontSize = if (tv) 16.sp else 14.sp,
        lineHeight = if (tv) 24.sp else 20.sp,
        textAlign = if (tv) TextAlign.Start else TextAlign.Center,
        minLines = 2,
        modifier = modifier.widthIn(max = if (tv) 460.dp else 300.dp)
    )
}

@Composable
private fun OnbPageSlots(
    illustration: @Composable () -> Unit,
    titleSlot: @Composable () -> Unit,
    descriptionSlot: @Composable () -> Unit,
    textVisible: Boolean = true,
    middleVisible: Boolean = true,
    buttonsVisible: Boolean = true,
    middle: @Composable ColumnScope.() -> Unit = {},
    buttons: @Composable ColumnScope.() -> Unit
) {
    val compact = LocalOnbCompact.current
    if (LocalOnbTv.current) {
        val tvScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp, end = 56.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .nfEnter(0),
                contentAlignment = Alignment.Center
            ) {
                illustration()
            }
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(tvScroll)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(modifier = Modifier.nfEnter(1).onbReveal(textVisible, 1)) { titleSlot() }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.nfEnter(2).onbReveal(textVisible, 2)) { descriptionSlot() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .nfEnter(3)
                            .onbReveal(middleVisible, 0),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        middle()
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nfEnter(4)
                            .onbReveal(buttonsVisible, 0),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        buttons()
                    }
                }
            }
        }
        return
    }
    val side = if (compact) 16.dp else 24.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = side, end = side, bottom = if (compact) 20.dp else 32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 24.dp))
            Box(modifier = Modifier.nfEnter(0)) { illustration() }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.nfEnter(1).onbReveal(textVisible, 1)) { titleSlot() }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.nfEnter(2).onbReveal(textVisible, 2), contentAlignment = Alignment.Center) {
                descriptionSlot()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .nfEnter(3)
                    .onbReveal(middleVisible, 0),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                middle()
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nfEnter(4)
                .onbReveal(buttonsVisible, 0),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            buttons()
        }
    }
}

@Composable
private fun OnbPage(
    illustration: @Composable () -> Unit,
    title: String,
    description: String,
    titleColor: Color = Nf.Text,
    textVisible: Boolean = true,
    middleVisible: Boolean = true,
    buttonsVisible: Boolean = true,
    middle: @Composable ColumnScope.() -> Unit = {},
    buttons: @Composable ColumnScope.() -> Unit
) {
    OnbPageSlots(
        illustration = illustration,
        titleSlot = { OnbTitle(title, titleColor) },
        descriptionSlot = { OnbDescription(description) },
        textVisible = textVisible,
        middleVisible = middleVisible,
        buttonsVisible = buttonsVisible,
        middle = middle,
        buttons = buttons
    )
}

private class OnbDot(val angle: Float, val radius: Float, val size: Float, val alpha: Float)

private val DefaultOnbDots = listOf(
    OnbDot(-52f, 0.93f, 3f, 0.5f),
    OnbDot(160f, 0.86f, 2.5f, 0.35f)
)

@Composable
private fun OnbRings(
    size: Dp,
    mood: OnbMood,
    dashedOuter: Boolean = false,
    arcSweep: Float = 0f,
    spinning: Boolean = false,
    dots: List<OnbDot> = DefaultOnbDots
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val infinite = rememberInfiniteTransition(label = "onbRings")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "onbSpin"
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "onbOrbit"
    )
    val sweep by animateFloatAsState(
        targetValue = arcSweep,
        animationSpec = tween(if (animationsEnabled) 600 else 0, easing = FastOutSlowInEasing),
        label = "onbSweep"
    )
    val tint = mood.color()
    val alphas = when (mood) {
        OnbMood.Neutral -> Triple(0.08f, 0.09f, 0.05f)
        OnbMood.Success -> Triple(0.14f, 0.45f, 0.12f)
        OnbMood.Warn -> Triple(0.08f, 0.25f, 0.05f)
        OnbMood.Error -> Triple(0.10f, 0.30f, 0.10f)
    }
    val base = if (mood == OnbMood.Neutral) Color.White else tint
    Canvas(modifier = Modifier.size(size)) {
        val half = this.size.minDimension / 2f
        val stroke = Stroke(width = 1.dp.toPx())
        val dashed = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 8.dp.toPx()))
        )
        drawCircle(base.copy(alpha = alphas.first), radius = half * 0.995f, style = if (dashedOuter) dashed else stroke)
        drawCircle(
            base.copy(alpha = alphas.second),
            radius = half * 0.716f,
            style = if (mood == OnbMood.Success) Stroke(width = 2.dp.toPx()) else stroke
        )
        drawCircle(base.copy(alpha = alphas.third), radius = half * 0.437f, style = stroke)
        if (sweep > 0f) {
            val ringRadius = half * 0.716f
            val startAngle = -90f + if (spinning && animationsEnabled) spin else 0f
            drawArc(
                color = Nf.Bright,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            val endAngle = (startAngle + sweep) * PI.toFloat() / 180f
            drawCircle(
                Nf.Bright,
                radius = 3.5.dp.toPx(),
                center = Offset(center.x + cos(endAngle) * ringRadius, center.y + sin(endAngle) * ringRadius)
            )
        }
    }
}

@Composable
private fun OnbIconCircle(
    icon: ImageVector,
    mood: OnbMood,
    tint: Color = mood.color(),
    glyph: Dp = 32.dp,
    contentAlpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    val compact = LocalOnbCompact.current
    val diameter = if (compact) 60.dp else 72.dp
    val iconSize = if (compact) 26.dp else glyph
    Box(
        modifier = modifier
            .size(diameter)
            .graphicsLayer { alpha = contentAlpha }
            .clip(CircleShape)
            .background(if (mood == OnbMood.Success) Nf.Green.copy(alpha = 0.12f) else Nf.Glass)
            .border(
                1.dp,
                if (mood == OnbMood.Neutral) Color.White.copy(alpha = 0.14f) else mood.color().copy(alpha = 0.5f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .nfFloat()
        )
    }
}

private val SparklePoints = listOf(
    Triple(0.12f, 0.20f, 0.00f),
    Triple(0.88f, 0.14f, 0.30f),
    Triple(0.94f, 0.62f, 0.60f),
    Triple(0.06f, 0.78f, 0.80f),
    Triple(0.64f, 0.95f, 0.15f),
    Triple(0.34f, 0.04f, 0.50f),
    Triple(0.80f, 0.86f, 0.90f),
    Triple(0.18f, 0.50f, 0.40f)
)

@Composable
private fun OnbSparkles(mood: OnbMood) {
    val animationsEnabled = rememberAnimationsEnabled()
    val infinite = rememberInfiniteTransition(label = "onbSparkles")
    val flow = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "onbSparklesFlow"
    )
    val tint = if (mood == OnbMood.Neutral) Color.White else mood.color()
    Canvas(modifier = Modifier.fillMaxSize()) {
        SparklePoints.forEach { point ->
            val strength = if (animationsEnabled) sin(((flow.value + point.third) % 1f) * PI.toFloat()) else 0.5f
            drawCircle(
                color = tint.copy(alpha = 0.45f * strength),
                radius = 1.8.dp.toPx(),
                center = Offset(point.first * size.width, point.second * size.height)
            )
        }
    }
}

@Composable
private fun OnbIllustration(
    icon: ImageVector,
    mood: OnbMood = OnbMood.Neutral,
    iconTint: Color = if (mood == OnbMood.Neutral) Nf.Text else mood.color(),
    dashedOuter: Boolean = false,
    arcSweep: Float = 0f,
    spinning: Boolean = false,
    dim: Boolean = false,
    dots: List<OnbDot> = DefaultOnbDots
) {
    val compact = LocalOnbCompact.current
    val height = if (compact) 176.dp else 240.dp
    val ringSize = if (compact) 170.dp else 215.dp
    val auraAlpha = when (mood) {
        OnbMood.Neutral -> 0.10f
        OnbMood.Success -> 0.16f
        OnbMood.Warn -> 0.14f
        OnbMood.Error -> 0.14f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(height).nfAura(mood.color().copy(alpha = auraAlpha)))
        OnbSparkles(mood)
        OnbRings(ringSize, mood, dashedOuter, arcSweep, spinning, dots)
        OnbIconCircle(icon = icon, mood = mood, tint = iconTint, contentAlpha = if (dim) 0.7f else 1f)
    }
}

@Composable
private fun OnbRowCard(
    rows: List<OnbRowData>,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    borderColor: Color = Nf.Line,
    rowHeight: Dp = 64.dp
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, borderColor, shape)
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.06f))
                )
            }
            OnbRow(row, Modifier.onbReveal(visible, index), rowHeight)
        }
    }
}

@Composable
private fun OnbRow(row: OnbRowData, modifier: Modifier = Modifier, rowHeight: Dp = 64.dp) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (row.dimmed) Color(0xFF1C1C1C) else row.iconBg),
            contentAlignment = Alignment.Center
        ) {
            val tint = if (row.dimmed) Nf.Muted else row.iconColor
            if (row.icon != null) {
                Icon(row.icon, null, tint = tint, modifier = Modifier.size(18.dp))
            } else if (row.iconRes != null) {
                Icon(painterResource(row.iconRes), null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = row.title,
                color = if (row.dimmed) Nf.TextSub else Nf.Text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (row.sub != null) {
                Text(
                    text = row.sub,
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        OnbMarkBadge(row.mark)
    }
}

@Composable
private fun OnbMarkBadge(mark: OnbMark) {
    if (mark == OnbMark.None) return
    val color = when (mark) {
        OnbMark.Ok -> Nf.Green
        OnbMark.Warn -> Nf.Yellow
        OnbMark.Bad -> Nf.Red
        else -> Nf.Muted
    }
    val icon = when (mark) {
        OnbMark.Ok -> OnbIcons.CheckSmall
        OnbMark.Warn -> OnbIcons.Exclaim
        OnbMark.Bad -> OnbIcons.Close
        else -> OnbIcons.Minus
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (mark == OnbMark.Off) 0.2f else 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun OnbNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = OnbIcons.Info,
            contentDescription = null,
            tint = Nf.Muted,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Text(
            text = text,
            color = Nf.TextSub,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

private val FlowEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val SatelliteBaseAngles = floatArrayOf(-150f, -30f, 90f)
private const val SatelliteOrbit = 99f

private fun satelliteCenter(index: Int, rotation: Float): Offset {
    val rad = (SatelliteBaseAngles[index] + rotation) * PI.toFloat() / 180f
    return Offset(171f + SatelliteOrbit * cos(rad), 150f + SatelliteOrbit * sin(rad))
}

private val TwinkleDots = listOf(
    Triple(40f, 60f, 0.0f),
    Triple(300f, 40f, 0.3f),
    Triple(318f, 190f, 0.6f),
    Triple(24f, 230f, 0.8f),
    Triple(220f, 280f, 0.15f),
    Triple(120f, 20f, 0.5f)
)

@Composable
private fun WelcomeIllustration(stage: Int, k: Float) {
    val animationsEnabled = rememberAnimationsEnabled()
    val duration = if (animationsEnabled) 450 else 0
    val orbScale by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0.62f,
        animationSpec = tween(duration, easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.15f)),
        label = "welcomeOrbScale"
    )
    val orbAlpha by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0.45f,
        animationSpec = tween(duration),
        label = "welcomeOrbAlpha"
    )
    val lit = stage >= 2
    val tint by animateColorAsState(
        targetValue = if (lit) Nf.Green else if (stage >= 1) Nf.Bright else Nf.TextSub,
        animationSpec = tween(duration),
        label = "welcomeTint"
    )
    val orbTop by animateColorAsState(
        targetValue = if (lit) Color(0xFF1F3A2A) else Color(0xFF2C2C30),
        animationSpec = tween(duration),
        label = "welcomeOrbTop"
    )
    val orbBottom by animateColorAsState(
        targetValue = if (lit) Color(0xFF14221A) else Color(0xFF1A1A1C),
        animationSpec = tween(duration),
        label = "welcomeOrbBottom"
    )
    val orbBorder by animateColorAsState(
        targetValue = if (lit) Nf.Green.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
        animationSpec = tween(duration),
        label = "welcomeOrbBorder"
    )
    val linesAlpha by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(if (animationsEnabled) 600 else 0),
        label = "welcomeLines"
    )
    val glowColor = when {
        stage >= 3 -> Nf.Green.copy(alpha = 0.18f)
        stage >= 2 -> Nf.Green.copy(alpha = 0.16f)
        stage >= 1 -> Nf.Bright.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val infinite = rememberInfiniteTransition(label = "welcomeLoop")
    val flow = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "welcomeFlow"
    )
    val ripple = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "welcomeRipple"
    )
    val rotation = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)),
        label = "welcomeRotation"
    )
    val breathe = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "welcomeBreathe"
    )
    val satellites = listOf(OnbIcons.Plane, OnbIcons.Play, OnbIcons.Gamepad)
    Box(
        modifier = Modifier.size((342 * k).dp, (300 * k).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size((260 * k).dp).nfAura(glowColor))
        Canvas(modifier = Modifier.fillMaxSize()) {
            val unit = size.width / 342f
            val origin = Offset(171f * unit, 150f * unit)
            val stroke = Stroke(width = 1.dp.toPx())
            val ringAlphas = if (lit) listOf(0.07f, 0.08f, 0.06f) else listOf(0.04f, 0.06f, 0.06f)
            listOf(129f, 99f, 69f).forEachIndexed { i, r ->
                drawCircle(Color.White.copy(alpha = ringAlphas[i]), radius = r * unit, center = origin, style = stroke)
            }
            if (stage >= 1) {
                TwinkleDots.forEach { dot ->
                    val phase = (flow.value + dot.third) % 1f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f * sin(phase * PI.toFloat())),
                        radius = 1.8.dp.toPx(),
                        center = Offset(dot.first * unit, dot.second * unit)
                    )
                }
            }
            if (linesAlpha > 0f) {
                for (j in 0..0) {
                    val p = (ripple.value + j * 0.5f) % 1f
                    drawCircle(
                        color = Nf.Green.copy(alpha = (1f - p) * 0.35f * linesAlpha),
                        radius = (68f + 60f * p) * unit,
                        center = origin,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size((132 * k).dp)
                .graphicsLayer {
                    val pulse = if (lit) 1f + 0.035f * breathe.value else 1f
                    scaleX = orbScale * pulse
                    scaleY = orbScale * pulse
                    alpha = orbAlpha
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(orbTop, orbBottom)))
                .border(1.dp, orbBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = OnbIcons.Power,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size((48 * k).dp)
            )
        }
        satellites.forEachIndexed { i, icon ->
            WelcomeSatellite(
                icon = icon,
                k = k,
                rotation = rotation,
                visible = stage >= 2,
                lit = stage >= 3,
                index = i
            )
        }
    }
}

@Composable
private fun BoxScope.WelcomeSatellite(
    icon: ImageVector,
    k: Float,
    rotation: androidx.compose.runtime.State<Float>,
    visible: Boolean,
    lit: Boolean,
    index: Int
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (animationsEnabled) 450 else 0,
            delayMillis = if (animationsEnabled) index * 90 else 0,
            easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.15f)
        ),
        label = "welcomeSatScale"
    )
    val border by animateColorAsState(
        targetValue = if (lit) Nf.Green.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.16f),
        animationSpec = tween(if (animationsEnabled) 400 else 0),
        label = "welcomeSatBorder"
    )
    val diameter = 40 * k
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                val center = satelliteCenter(index, rotation.value)
                val half = with(density) { (diameter / 2f).dp.toPx() }
                val unit = with(density) { k.dp.toPx() }
                androidx.compose.ui.unit.IntOffset(
                    (center.x * unit - half).toInt(),
                    (center.y * unit - half).toInt()
                )
            }
            .size(diameter.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .background(Color(0xFF1E1E1E))
            .border(1.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Nf.Text,
            modifier = Modifier
                .size((diameter / 2f).dp)
                .nfFloat()
        )
    }
}

private val WelcomeBenefits = listOf(
    Triple(OnbIcons.Bolt, "Одна кнопка", "Включили и пользуетесь"),
    Triple(OnbIcons.Refresh, "Работает само", "Подберём способ сами"),
    Triple(OnbIcons.Code, "Открытый код", "Без сбора данных")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnbBenefitCarousel(visible: Boolean, modifier: Modifier = Modifier, edge: Dp = 24.dp) {
    val anim = rememberAnimationsEnabled()
    val count = WelcomeBenefits.size
    val start = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % count
    val pager = rememberPagerState(initialPage = start) { Int.MAX_VALUE }
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val timer = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    LaunchedEffect(pager.targetPage, dragged, visible, anim) {
        timer.snapTo(0f)
        if (!visible || !anim || dragged) return@LaunchedEffect
        timer.animateTo(1f, tween(3600, easing = LinearEasing))
        val next = pager.targetPage + 1
        scope.launch { pager.animateScrollToPage(next, animationSpec = tween(520, easing = ease)) }
    }

    Column(modifier.fillMaxWidth().onbReveal(visible)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = edge + 16.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth().height(88.dp)
        ) { page ->
            val i = page % count
            val (icon, title, sub) = WelcomeBenefits[i]
            val off = (pager.currentPage - page) + pager.currentPageOffsetFraction
            val a = off.absoluteValue.coerceIn(0f, 1f)
            val active = pager.targetPage == page

            val kick = remember { Animatable(1f) }
            LaunchedEffect(active) {
                if (active && anim && visible) {
                    kick.snapTo(0f)
                    kick.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
                }
            }
            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = lerp(1f, 0.92f, a)
                        scaleX = s
                        scaleY = s
                    }
                    .clip(shape)
                    .background(lerpColor(Color(0xFF1F1F1F), Color(0xFF1A1A1A), a))
                    .border(1.dp, lerpColor(Nf.Green.copy(alpha = 0.32f), Color.White.copy(alpha = 0.06f), a), shape)
                    .drawBehind {
                        val t = kick.value
                        if (active && t < 1f) {
                            val band = 90.dp.toPx()
                            val x = -band + (size.width + band * 2) * t
                            drawRect(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Nf.Green.copy(alpha = 0.10f), Color.Transparent),
                                    start = Offset(x, 0f),
                                    end = Offset(x + band, size.height * 0.33f)
                                )
                            )
                        }
                    }
                    .focusProperties { canFocus = false }
                    .clickable(remember { MutableInteractionSource() }, null) {
                        scope.launch {
                            val target = if (page == pager.currentPage) page + 1 else page
                            if (anim) {
                                pager.animateScrollToPage(target, animationSpec = tween(520, easing = ease))
                            } else {
                                pager.scrollToPage(target)
                            }
                        }
                    }
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                        .graphicsLayer {
                            alpha = lerp(1f, 0.5f, a)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BenefitIcon(icon, i, kick.value, active, a)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = title,
                            color = Nf.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            maxLines = 1
                        )
                        Text(
                            text = sub,
                            color = Nf.TextSub,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(count) { k ->
                val on = pager.targetPage % count == k
                val w by animateDpAsState(if (on) 24.dp else 6.dp, tween(if (anim) 300 else 0), label = "pill")
                Box(
                    Modifier
                        .size(w, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = if (on) 0.12f else 0.18f))
                ) {
                    val fillAlpha by animateFloatAsState(
                        targetValue = if (on) 1f else 0f,
                        animationSpec = tween(if (anim) 220 else 0),
                        label = "pillFill"
                    )
                    if (fillAlpha > 0.01f) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (!on) 1f else if (anim) timer.value else 1f)
                                .graphicsLayer { alpha = fillAlpha }
                                .background(Nf.Text)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitIcon(icon: ImageVector, i: Int, t: Float, active: Boolean, a: Float) {
    val tint = lerpColor(Nf.Green, Color(0xFF71717A), a)
    val pop = animateFloatAsState(
        targetValue = if (active) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f),
        label = "benefitIconPop"
    )
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (active && t < 1f) {
                val base = 22.dp.toPx()
                val room = size.minDimension / 2f - base - 1.dp.toPx()
                drawCircle(
                    color = Nf.Green.copy(alpha = (1f - t) * 0.5f),
                    radius = base + room * t,
                    style = Stroke(1.5.dp.toPx() * (1f - t * 0.6f))
                )
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(lerpColor(Color(0xFF1F3A2A), Color(0xFF242424), a)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        if (!active || t >= 1f) return@graphicsLayer
                        when (i) {
                            0 -> alpha = if ((t * 10).toInt() % 2 == 0) 1f else 0.4f + 0.6f * t
                            1 -> rotationZ = 360f * FastOutSlowInEasing.transform(t)
                            else -> translationX = sin(t * PI.toFloat() * 4f) * 2.dp.toPx() * (1f - t)
                        }
                    }
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNavigate: (OnboardingStep) -> Unit) {
    val animationsEnabled = rememberAnimationsEnabled()
    var stage by remember { mutableIntStateOf(if (animationsEnabled) 0 else 3) }
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready && stage >= 3)

    LaunchedEffect(Unit) {
        if (stage < 3) {
            delay(300)
            stage = 1
            delay(400)
            stage = 2
            delay(500)
            stage = 3
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (LocalOnbTv.current) {
            val tvK = ((maxHeight - 56.dp) / 300.dp).coerceIn(0.6f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 56.dp, end = 56.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .nfEnter(0),
                    contentAlignment = Alignment.Center
                ) {
                    WelcomeIllustration(stage, tvK)
                }
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(modifier = Modifier.nfEnter(1).onbReveal(stage >= 1, 1)) {
                        OnbTitle("Одна кнопка, и всё работает")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.nfEnter(2).onbReveal(stage >= 1, 2)) {
                        OnbDescription("Telegram, YouTube и Discord снова грузятся быстро и без сбоев.")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    OnbBenefitCarousel(
                        visible = stage >= 2,
                        edge = 0.dp,
                        modifier = Modifier.nfEnter(3)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    NfButton(
                        text = "Начать",
                        enabled = ready && stage >= 3,
                        modifier = Modifier
                            .nfEnter(4)
                            .onbReveal(stage >= 3, 0)
                            .focusRequester(focusRequester),
                        onClick = { onNavigate(OnboardingStep.DeviceType) }
                    )
                }
            }
            return@BoxWithConstraints
        }
        val fixed = 338.dp
        val side = if (maxWidth < 360.dp) 16.dp else 24.dp
        val k = minOf(((maxHeight - fixed) / 300.dp).coerceIn(0.55f, 1f), (maxWidth - side * 2) / 342.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = side, end = side, bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.nfEnter(0)) { WelcomeIllustration(stage, k) }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.nfEnter(1).onbReveal(stage >= 1, 1)) {
                    OnbTitle("Одна кнопка, и всё работает")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.nfEnter(2).onbReveal(stage >= 1, 2)) {
                    OnbDescription("Telegram, YouTube и Discord снова грузятся быстро и без сбоев.")
                }
                Spacer(modifier = Modifier.height(16.dp))
                val bleed = side
                OnbBenefitCarousel(
                    visible = stage >= 2,
                    edge = bleed,
                    modifier = Modifier
                        .nfEnter(3)
                        .layout { measurable, constraints ->
                            val extra = (bleed * 2).roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra)
                            )
                            layout(constraints.maxWidth, placeable.height) {
                                placeable.place(-bleed.roundToPx(), 0)
                            }
                        }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            NfButton(
                text = "Начать",
                enabled = ready && stage >= 3,
                modifier = Modifier
                    .nfEnter(4)
                    .onbReveal(stage >= 3, 0)
                    .focusRequester(focusRequester),
                onClick = { onNavigate(OnboardingStep.DeviceType) }
            )
        }
    }
}

@Composable
private fun DeviceTypeStep(
    onNavigate: (OnboardingStep) -> Unit,
    onSetDeviceIsAndroid: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)

    fun choose(isPhone: Boolean) {
        onSetDeviceIsAndroid(isPhone)
        prefsOf(context).edit()
            .putBoolean("device_type_tv", !isPhone)
            .putBoolean("is_smart_tv", !isPhone)
            .apply()
        onNavigate(OnboardingStep.TelegramProxyQuestion)
    }

    val onTvBox = remember { context.hasLeanbackFeature() }

    OnbPage(
        illustration = { OnbIllustration(if (onTvBox) OnbIcons.Tv else OnbIcons.Phone) },
        title = "Где будет работать NetFix?",
        description = "Подстроим экран и управление под ваше устройство."
    ) {
        if (onTvBox) {
            NfButton(
                text = "На Smart TV",
                enabled = ready,
                modifier = Modifier.focusRequester(focusRequester),
                onClick = { choose(false) }
            )
            NfButton(
                text = "На этом телефоне",
                style = NfButtonStyle.Secondary,
                enabled = ready,
                onClick = { choose(true) }
            )
        } else {
            NfButton(
                text = "На этом телефоне",
                enabled = ready,
                modifier = Modifier.focusRequester(focusRequester),
                onClick = { choose(true) }
            )
            NfButton(
                text = "На Smart TV",
                style = NfButtonStyle.Secondary,
                enabled = ready,
                onClick = { choose(false) }
            )
        }
    }
}

@Composable
private fun TelegramProxyStep(
    onNavigate: (OnboardingStep) -> Unit,
    deviceIsAndroid: Boolean,
    onSetTelegramProxy: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)
    var busy by remember { mutableStateOf(false) }

    OnbPage(
        illustration = { OnbIllustration(OnbIcons.Plane) },
        title = if (deviceIsAndroid) "Починить Telegram?" else "Пользуетесь Telegram на ТВ?",
        description = if (deviceIsAndroid) {
            "Настроим прокси сами. В Telegram останется нажать «Подключить»."
        } else {
            "Если нет, пропустите, настроить можно позже."
        }
    ) {
        val tvOrder = LocalOnbTv.current
        val setupButton: @Composable (Modifier, NfButtonStyle) -> Unit = { buttonModifier, buttonStyle ->
            NfButton(
                text = if (busy) "Настраиваем…" else "Да, настроить",
                style = buttonStyle,
                enabled = ready && !busy,
                modifier = buttonModifier,
                onClick = {
                    busy = true
                    onSetTelegramProxy(true)
                    scope.launch {
                        delay(450)
                        onNavigate(OnboardingStep.YoutubeBypassQuestion)
                    }
                }
            )
        }
        val skipButton: @Composable (Modifier, NfButtonStyle) -> Unit = { buttonModifier, buttonStyle ->
            NfButton(
                text = "Пропустить",
                style = buttonStyle,
                enabled = ready && !busy,
                modifier = buttonModifier,
                onClick = {
                    onSetTelegramProxy(false)
                    onNavigate(OnboardingStep.YoutubeBypassQuestion)
                }
            )
        }
        if (tvOrder) {
            skipButton(Modifier.focusRequester(focusRequester), NfButtonStyle.Primary)
            setupButton(Modifier, NfButtonStyle.Secondary)
        } else {
            setupButton(Modifier.focusRequester(focusRequester), NfButtonStyle.Primary)
            skipButton(Modifier, NfButtonStyle.Secondary)
        }
    }
}

@Composable
private fun TvYoutubeCardButton(
    title: String,
    subtitle: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val fill by animateColorAsState(
        targetValue = if (pressed) Nf.SurfacePressed else Color(0xFF242424),
        animationSpec = tween(150),
        label = "tvCardFill"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Nf.Line, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = Nf.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(subtitle, color = Nf.TextSub, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun YoutubeBypassStep(
    onNavigate: (OnboardingStep) -> Unit,
    onSetYoutubeBypass: (Boolean) -> Unit,
    isSmartTv: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)
    var busy by remember { mutableStateOf(false) }

    fun continueToScan() {
        if (StrategyTestManager.testResults.isNotEmpty()) {
            onNavigate(OnboardingStep.StrategyTestingExisting)
        } else {
            onNavigate(OnboardingStep.StrategyTestingRunning)
        }
    }

    OnbPage(
        illustration = { OnbIllustration(OnbIcons.Play) },
        title = if (isSmartTv) "Разблокировать YouTube?" else "Починить YouTube и Discord?",
        description = if (isSmartTv) {
            "YouTube в России заблокирован. Рекомендуем SmartTube, бесплатный плеер без рекламы с поддержкой обхода, NetFix установит его сам."
        } else {
            "Без этого видео может не грузиться или зависать."
        }
    ) {
        if (isSmartTv) {
            TvYoutubeCardButton(
                title = "SmartTube (Рекомендуется)",
                subtitle = "Простой и удобный аналог YouTube для ТВ без рекламы. NetFix автоматически скачает, установит и настроит его для плавной работы.",
                enabled = ready,
                modifier = Modifier.focusRequester(focusRequester),
                onClick = {
                    prefsOf(context).edit()
                        .putString("work_mode", "Proxy")
                        .putString("youtube_mode", "smarttube")
                        .putString("byedpi_proxy_port", "1080")
                        .putBoolean("is_smart_tv", true)
                        .putBoolean("wants_youtube_bypass", true)
                        .apply()
                    onSetYoutubeBypass(true)
                    scope.launch {
                        try {
                            val url = com.rupleide.netfix.core.update.UpdateManager.getSmartTubeLatestUrl()
                            com.rupleide.netfix.core.update.UpdateManager.downloadAndInstallApk(
                                context = context,
                                downloadUrl = url,
                                fileName = "smarttube.apk",
                                onProgress = {},
                                onError = {}
                            )
                        } catch (_: Exception) {
                        }
                    }
                    continueToScan()
                }
            )
            TvYoutubeCardButton(
                title = "Официальный YouTube",
                subtitle = "Скорее всего, не запустится или будет постоянно вылетать в фоне из-за нехватки оперативной памяти ТВ.",
                enabled = ready,
                onClick = {
                    prefsOf(context).edit()
                        .putString("work_mode", "VPN")
                        .putString("youtube_mode", "official")
                        .putBoolean("is_smart_tv", true)
                        .putBoolean("wants_youtube_bypass", true)
                        .apply()
                    onSetYoutubeBypass(true)
                    continueToScan()
                }
            )
        } else {
            NfButton(
                text = if (busy) "Включаем…" else "Да, включить",
                enabled = ready && !busy,
                modifier = Modifier.focusRequester(focusRequester),
                onClick = {
                    busy = true
                    prefsOf(context).edit().putBoolean("wants_youtube_bypass", true).apply()
                    onSetYoutubeBypass(true)
                    scope.launch {
                        delay(450)
                        continueToScan()
                    }
                }
            )
            NfButton(
                text = "Пропустить",
                style = NfButtonStyle.Secondary,
                enabled = ready && !busy,
                onClick = {
                    prefsOf(context).edit().putBoolean("wants_youtube_bypass", false).apply()
                    onSetYoutubeBypass(false)
                    onNavigate(OnboardingStep.AboutAndSupport)
                }
            )
        }
    }
}

private val passedSitesRegex = Regex("""^(\d+)/(\d+)$""")

private fun appliedResultStatus(): String? {
    val applied = StrategyTestManager.appliedStrategy ?: return null
    val key = StrategyTester.normalizeStrategy(applied)
    return StrategyTestManager.testResults
        .firstOrNull { StrategyTester.normalizeStrategy(it.second) == key }
        ?.third
}

private fun describeStatus(status: String?, prefix: String): String {
    if (status == null) return prefix
    val match = passedSitesRegex.find(status.trim())
    return when {
        match != null -> "$prefix · открыто ${match.groupValues[1]} из ${match.groupValues[2]}"
        status.contains("мс") -> "$prefix · ${status.trim()}"
        else -> prefix
    }
}

private fun resultIsPartial(context: Context): Boolean {
    val applied = StrategyTestManager.appliedStrategy ?: return false
    val kind = context.currentNetworkKind() ?: return false
    val key = StrategyTester.normalizeStrategy(applied)
    val ranked = StrategyTestManager.verifyResults[kind]
        ?.firstOrNull { StrategyTester.normalizeStrategy(it.strategy) == key }
        ?: return false
    return ranked.total > 0 && !ranked.isWorking
}

private fun verifiedLine(context: Context, strategy: String?): String? {
    if (strategy == null) return null
    val kind = context.currentNetworkKind() ?: return null
    val key = StrategyTester.normalizeStrategy(strategy)
    val ranked = StrategyTestManager.verifyResults[kind]
        ?.firstOrNull { StrategyTester.normalizeStrategy(it.strategy) == key }
        ?: return null
    if (ranked.total <= 0) return null
    val base = "Открыто ${ranked.totalOk} из ${ranked.total}"
    return if (ranked.averageMs > 0) "$base · ${ranked.averageMs} мс" else base
}

private fun strategyTitle(context: Context, name: String): String {
    val network = context.currentNetworkKind()?.displayName
    return if (network != null) "$name · $network" else name
}

@Composable
private fun StrategyTestingExistingStep(onNavigate: (OnboardingStep) -> Unit) {
    val context = LocalContext.current
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)
    val title = strategyTitle(context, StrategyTestManager.getActiveStrategyName(context))
    val sub = verifiedLine(context, StrategyTestManager.appliedStrategy)?.let { "Работает · ${it.replaceFirstChar { c -> c.lowercase() }}" }
        ?: describeStatus(appliedResultStatus(), "Работает")

    OnbPage(
        illustration = {
            OnbIllustration(
                icon = OnbIcons.Check,
                mood = OnbMood.Success,
                dots = listOf(OnbDot(-35f, 0.9f, 3f, 1f), OnbDot(150f, 0.85f, 2.5f, 0.5f))
            )
        },
        title = "Способ уже подобран",
        description = "Нашли его при прошлой проверке. Оставим?",
        middle = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Nf.Green.copy(alpha = 0.07f))
                    .border(2.dp, Nf.Green.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Nf.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(OnbIcons.CheckSmall, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        color = Nf.Text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sub,
                        color = Nf.Green,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) {
        NfButton(
            text = "Оставить как есть",
            enabled = ready,
            modifier = Modifier.focusRequester(focusRequester),
            onClick = { onNavigate(OnboardingStep.AboutAndSupport) }
        )
        NfButton(
            text = "Проверить заново",
            style = NfButtonStyle.Secondary,
            enabled = ready,
            onClick = { onNavigate(OnboardingStep.StrategyTestingRunning) }
        )
    }
}

private val ScanTips = listOf(
    "Можно свернуть приложение, проверка продолжится.",
    "Результат сохранится, повторять не нужно.",
    "Способ можно сменить в любой момент.",
    "Проверка занимает 2-3 минуты.",
    "Для Wi-Fi и мобильной сети способы свои."
)

private fun waysPhrase(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    val verb = if (last == 1 && lastTwo != 11) "Подходит" else "Подходят"
    val noun = when {
        lastTwo in 11..14 -> "способов"
        last == 1 -> "способ"
        last in 2..4 -> "способа"
        else -> "способов"
    }
    return "$verb пока $count $noun"
}

@Composable
private fun StrategyTestingRunningStep(
    context: Context,
    onNavigate: (OnboardingStep) -> Unit
) {
    var verifying by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val job = StrategyTestManager.startTesting(context)
        if (job != null) {
            job.join()
        } else {
            while (StrategyTestManager.isTesting) {
                delay(200)
            }
        }
        val kind = context.currentNetworkKind()
        if (!StrategyTestManager.hasConnectionError && StrategyTestManager.hasScanResults() && kind != null) {
            verifying = true
            val verifyScope = if (kind == com.rupleide.netfix.core.dpibypass.NetworkKind.Wifi) {
                StrategyTestManager.VerifyScope.Wifi
            } else {
                StrategyTestManager.VerifyScope.Mobile
            }
            val verifyJob = StrategyTestManager.startVerification(context, verifyScope)
            if (verifyJob != null) {
                verifyJob.join()
            } else {
                while (StrategyTestManager.isVerifying) {
                    delay(200)
                }
            }
        }
        onNavigate(OnboardingStep.StrategyTestingResult)
    }

    val total = StrategyTestManager.totalStrategiesCount
    val checked = StrategyTestManager.currentTestIndex
    val finalTotal = StrategyTestManager.scanFinalTotal
    val finalDone = StrategyTestManager.scanFinalDone
    val stopping = StrategyTestManager.finishRequested
    val canFinish = StrategyTestManager.canFinishEarly
    val inFinal = finalTotal > 0
    val found = StrategyTestManager.testResults.count { StrategyTester.isSuccessfulStatus(it.third) }
    val phase = when {
        verifying || StrategyTestManager.isVerifying -> ScanPhase.Verify
        stopping -> ScanPhase.Stop
        inFinal -> ScanPhase.Refine
        checked == 0 -> ScanPhase.Start
        else -> ScanPhase.Search
    }
    val refineLike = phase == ScanPhase.Refine || phase == ScanPhase.Stop

    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(confirm) {
        if (confirm) {
            delay(3000)
            confirm = false
        }
    }
    LaunchedEffect(canFinish, stopping) {
        if (!canFinish || stopping) confirm = false
    }

    val ready = rememberEnterGate()
    val finishFocus = rememberPrimaryFocus(ready && canFinish && !stopping)

    OnbPageSlots(
        illustration = {
            OnbIllustration(
                icon = when (phase) {
                    ScanPhase.Start, ScanPhase.Search -> OnbIcons.Search
                    ScanPhase.Refine, ScanPhase.Verify -> OnbIcons.Target
                    ScanPhase.Stop -> OnbIcons.CheckCircle
                },
                dashedOuter = true,
                arcSweep = if (refineLike) 260f else 75f,
                spinning = !stopping,
                dim = stopping,
                dots = if (refineLike) {
                    listOf(OnbDot(-30f, 0.78f, 3f, 0.7f), OnbDot(120f, 0.78f, 3f, 0.5f), OnbDot(40f, 0.9f, 3f, 1f))
                } else {
                    DefaultOnbDots
                }
            )
        },
        titleSlot = {
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "scanTitle"
            ) { value ->
                OnbTitle(
                    when (value) {
                        ScanPhase.Start -> "Готовимся к проверке"
                        ScanPhase.Search -> "Ищем рабочий способ"
                        ScanPhase.Refine -> "Уточняем лучшие: $finalDone из $finalTotal"
                        ScanPhase.Verify -> "Проверяем по адресам"
                        ScanPhase.Stop -> "Сохраняем результат"
                    }
                )
            }
        },
        descriptionSlot = {
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "scanDescription"
            ) { value ->
                OnbDescription(
                    when (value) {
                        ScanPhase.Start -> "Соберём список способов для вашей сети."
                        ScanPhase.Search -> "Пробуем способы по очереди и меряем скорость."
                        ScanPhase.Refine -> "Перепроверяем самые быстрые, чтобы выбрать точно."
                        ScanPhase.Verify -> "Смотрим, как лучшие способы открывают YouTube и Discord."
                        ScanPhase.Stop -> "Включаем самый быстрый из найденных."
                    }
                )
            }
        },
        middle = {
            ScanCard(
                phase = phase,
                checked = checked,
                total = total,
                finalDone = finalDone,
                finalTotal = finalTotal,
                found = found,
                verifyDone = StrategyTestManager.verifyDone,
                verifyTotal = StrategyTestManager.verifyTotal
            )
            ScanTipArea(
                confirm = confirm,
                stopping = stopping
            )
        }
    ) {
        when {
            phase == ScanPhase.Verify -> NfButton(
                text = "Пропустить проверку",
                style = NfButtonStyle.Secondary,
                enabled = ready && StrategyTestManager.isVerifying,
                onClick = { StrategyTestManager.stopVerification() }
            )
            stopping -> NfButton(
                text = "Завершаем…",
                style = NfButtonStyle.Secondary,
                enabled = false,
                onClick = {}
            )
            confirm -> NfButton(
                text = "Точно завершить?",
                style = NfButtonStyle.DangerSolid,
                enabled = ready,
                onClick = {
                    confirm = false
                    StrategyTestManager.finishScanEarly()
                }
            )
            else -> NfButton(
                text = "Завершить досрочно",
                style = NfButtonStyle.Secondary,
                enabled = ready && canFinish,
                modifier = Modifier.focusRequester(finishFocus),
                onClick = { confirm = true }
            )
        }
    }
}

@Composable
private fun ScanCard(
    phase: ScanPhase,
    checked: Int,
    total: Int,
    finalDone: Int,
    finalTotal: Int,
    found: Int,
    verifyDone: Int,
    verifyTotal: Int
) {
    val refineLike = phase == ScanPhase.Refine || phase == ScanPhase.Stop
    val isVerify = phase == ScanPhase.Verify
    val fraction = when {
        isVerify -> if (verifyTotal > 0) verifyDone / verifyTotal.toFloat() else 0f
        total > 0 -> checked / total.toFloat()
        else -> 0f
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .graphicsLayer { alpha = if (phase == ScanPhase.Stop) 0.6f else 1f }
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isVerify -> "Проверено $verifyDone из $verifyTotal"
                    refineLike -> "Лучшие $finalTotal из $total"
                    else -> "Проверено $checked из $total"
                },
                color = Nf.Text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1
            )
            Text(
                text = when (phase) {
                    ScanPhase.Refine -> "$finalDone готово"
                    ScanPhase.Stop -> "остановлено"
                    else -> "${(fraction * 100).toInt()}%"
                },
                color = Nf.TextSub,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1
            )
        }
        if (refineLike) {
            RefineSegments(count = finalTotal, done = finalDone, stopped = phase == ScanPhase.Stop)
        } else {
            NfProgress(fraction, height = 6)
        }
        Text(
            text = when (phase) {
                ScanPhase.Start -> "Подключаемся…"
                ScanPhase.Search -> if (found > 0) waysPhrase(found) else "Рабочих пока нет"
                ScanPhase.Verify -> "Выбираем лучший для сети"
                else -> "Способ ${min(finalDone + 1, finalTotal)} из $finalTotal"
            },
            color = Nf.TextSub,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun RefineSegments(count: Int, done: Int, stopped: Boolean) {
    val animationsEnabled = rememberAnimationsEnabled()
    val infinite = rememberInfiniteTransition(label = "refineShimmer")
    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "refineShimmerValue"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) { index ->
            val isDone = index < done
            val isCurrent = index == done && !stopped
            val fill by animateColorAsState(
                targetValue = if (isDone) Nf.Bright else Color.White.copy(alpha = 0.08f),
                animationSpec = tween(250),
                label = "refineSegmentFill"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(fill)
                    .drawBehind {
                        if (isCurrent && animationsEnabled) {
                            val band = 24.dp.toPx()
                            val x = -band + (size.width + band) * shimmer
                            drawRect(
                                color = Color.White.copy(alpha = 0.35f),
                                topLeft = Offset(x, 0f),
                                size = Size(band, size.height)
                            )
                        }
                    }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScanTipArea(confirm: Boolean, stopping: Boolean) {
    val anim = rememberAnimationsEnabled()
    val count = ScanTips.size
    val override = confirm || stopping
    val start = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % count
    val pager = rememberPagerState(initialPage = start) { Int.MAX_VALUE }
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val timer = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    LaunchedEffect(pager.targetPage, dragged, override, anim) {
        timer.snapTo(0f)
        if (override || !anim || dragged) return@LaunchedEffect
        timer.animateTo(1f, tween(6000, easing = LinearEasing))
        val next = pager.targetPage + 1
        scope.launch { pager.animateScrollToPage(next, animationSpec = tween(520, easing = ease)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                stopping -> {}
                confirm -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(OnbIcons.Alert, null, tint = Nf.Red, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Возьмём лучший из уже найденных.",
                        color = Nf.Text,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) { page ->
                    val off = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    val a = off.absoluteValue.coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = lerp(1f, 0.25f, a)
                                val s = lerp(1f, 0.94f, a)
                                scaleX = s
                                scaleY = s
                            }
                            .clickable(remember { MutableInteractionSource() }, null) {
                                scope.launch {
                                    val target = pager.targetPage + 1
                                    if (anim) {
                                        pager.animateScrollToPage(target, animationSpec = tween(520, easing = ease))
                                    } else {
                                        pager.scrollToPage(target)
                                    }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = ScanTips[page % count],
                            color = Nf.TextSub,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (override) 0f else 1f },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            repeat(count) { k ->
                val on = pager.targetPage % count == k
                val width by animateDpAsState(if (on) 18.dp else 5.dp, tween(if (anim) 300 else 0), label = "tipPill")
                val fillAlpha by animateFloatAsState(
                    targetValue = if (on) 1f else 0f,
                    animationSpec = tween(if (anim) 220 else 0),
                    label = "tipPillFill"
                )
                Box(
                    Modifier
                        .size(width, 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = if (on) 0.14f else 0.16f))
                ) {
                    if (fillAlpha > 0.01f) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (!on) 1f else if (anim) timer.value else 1f)
                                .graphicsLayer { alpha = fillAlpha }
                                .background(Nf.TextSub)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyTestingResultStep(onNavigate: (OnboardingStep) -> Unit) {
    val context = LocalContext.current
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)
    val isError = StrategyTestManager.hasConnectionError
    val results = StrategyTestManager.testResults
    val bestSuccess = results.firstOrNull { StrategyTester.isSuccessfulStatus(it.third) }
    val hasSuccess = bestSuccess != null

    when {
        isError -> OnbPage(
            illustration = { OnbIllustration(OnbIcons.WifiOff, OnbMood.Error, dashedOuter = true, dots = emptyList()) },
            title = "Нет интернета",
            description = "Проверьте сеть и отключите другие VPN.",
            middle = {
                OnbRowCard(
                    rows = listOf(
                        OnbRowData(
                            icon = OnbIcons.Close,
                            title = "Сеть недоступна",
                            sub = "Проверено ${StrategyTestManager.currentTestIndex} из ${StrategyTestManager.totalStrategiesCount}",
                            iconColor = Nf.Red,
                            iconBg = Nf.Red.copy(alpha = 0.14f)
                        )
                    ),
                    borderColor = Nf.Red.copy(alpha = 0.25f)
                )
            }
        ) {
            NfButton(
                text = "Повторить",
                enabled = ready,
                modifier = Modifier.focusRequester(focusRequester),
                onClick = { onNavigate(OnboardingStep.StrategyTestingRunning) }
            )
            NfButton(
                text = "Продолжить без настройки",
                style = NfButtonStyle.Secondary,
                enabled = ready,
                onClick = { onNavigate(OnboardingStep.AboutAndSupport) }
            )
        }
        hasSuccess -> {
            val strategy = StrategyTestManager.appliedStrategy ?: bestSuccess!!.second
            val status = appliedResultStatus() ?: bestSuccess!!.third
            val statusMatch = passedSitesRegex.find(status.trim())
            val partial = resultIsPartial(context)
            val sub = verifiedLine(context, strategy) ?: when {
                statusMatch != null -> "Открыто ${statusMatch.groupValues[1]} из ${statusMatch.groupValues[2]}"
                status.contains("мс") -> status.trim()
                else -> "Работает"
            }
            OnbPage(
                illustration = {
                    if (partial) OnbIllustration(OnbIcons.ShieldAlert, OnbMood.Warn) else OnbIllustration(OnbIcons.Check, OnbMood.Success)
                },
                title = if (partial) "Работает не полностью" else "Способ найден",
                description = if (partial) {
                    "Часть адресов не открылась. Проверить заново можно в настройках."
                } else {
                    "Подобрали лучший для вашей сети. Сменить можно в настройках."
                },
                middle = {
                    OnbRowCard(
                        rows = listOf(
                            OnbRowData(
                                icon = OnbIcons.Bolt,
                                title = strategyTitle(context, StrategyTestManager.getStrategyName(strategy, context)),
                                sub = sub,
                                iconColor = if (partial) Nf.Yellow else Nf.Green,
                                iconBg = (if (partial) Nf.Yellow else Nf.Green).copy(alpha = 0.14f)
                            )
                        ),
                        borderColor = (if (partial) Nf.Yellow else Nf.Green).copy(alpha = 0.30f)
                    )
                }
            ) {
                NfButton(
                    text = "Продолжить",
                    enabled = ready,
                    modifier = Modifier.focusRequester(focusRequester),
                    onClick = { onNavigate(OnboardingStep.AboutAndSupport) }
                )
                if (partial) {
                    NfButton(
                        text = "Проверить ещё раз",
                        style = NfButtonStyle.Secondary,
                        enabled = ready,
                        onClick = { onNavigate(OnboardingStep.StrategyTestingRunning) }
                    )
                }
            }
        }
        else -> OnbPage(
            illustration = { OnbIllustration(OnbIcons.Shield, OnbMood.Neutral) },
            title = "Включили стандартный способ",
            description = "Скорость не измерили. Проверить заново можно в настройках.",
            middle = {
                OnbRowCard(
                    rows = listOf(
                        OnbRowData(
                            icon = OnbIcons.Bolt,
                            title = "Способ 1 · стандартный",
                            sub = "Подходит большинству сетей"
                        )
                    )
                )
            }
        ) {
            NfButton(
                text = "Продолжить",
                enabled = ready,
                modifier = Modifier.focusRequester(focusRequester),
                onClick = { onNavigate(OnboardingStep.AboutAndSupport) }
            )
            NfButton(
                text = "Проверить ещё раз",
                style = NfButtonStyle.Secondary,
                enabled = ready,
                onClick = { onNavigate(OnboardingStep.StrategyTestingRunning) }
            )
        }
    }
}

@Composable
private fun AboutAndSupportStep(onNavigate: (OnboardingStep) -> Unit) {
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)

    OnbPage(
        illustration = { OnbIllustration(OnbIcons.Shield) },
        title = "Честно и безопасно",
        description = "NetFix не видит ваши данные и ничего не продаёт.",
        middle = {
            OnbRowCard(
                rows = listOf(
                    OnbRowData(OnbIcons.Code, "Открытый код", "Проверить может любой"),
                    OnbRowData(OnbIcons.Lock, "Без сбора данных", "Ничего не отправляем"),
                    OnbRowData(OnbIcons.Help, "Помощь рядом", "Вкладка «Информация»")
                )
            )
        }
    ) {
        NfButton(
            text = "Понятно",
            enabled = ready,
            modifier = Modifier.focusRequester(focusRequester),
            onClick = { onNavigate(OnboardingStep.Summary) }
        )
    }
}

@Composable
private fun SummaryStep(
    wantsTelegramProxy: Boolean?,
    wantsYoutubeBypass: Boolean?,
    deviceIsAndroid: Boolean,
    onNavigate: (OnboardingStep) -> Unit
) {
    val context = LocalContext.current
    val ready = rememberEnterGate()
    val focusRequester = rememberPrimaryFocus(ready)
    val telegramOn = wantsTelegramProxy == true
    val youtubeOn = wantsYoutubeBypass == true
    val strategyName = if (youtubeOn) StrategyTestManager.getActiveStrategyName(context) else null

    OnbPage(
        illustration = {
            OnbIllustration(
                icon = OnbIcons.Check,
                mood = OnbMood.Success,
                dots = listOf(OnbDot(0f, 0.93f, 3f, 0.7f))
            )
        },
        title = "Всё готово!",
        description = "Вот что мы настроили.",
        middle = {
            OnbRowCard(
                rows = listOf(
                    OnbRowData(
                        icon = OnbIcons.Plane,
                        title = "Telegram",
                        sub = if (telegramOn) "Прокси настроен" else "Не используется",
                        mark = if (telegramOn) OnbMark.Ok else OnbMark.Off,
                        dimmed = !telegramOn
                    ),
                    OnbRowData(
                        icon = OnbIcons.Play,
                        title = "YouTube и Discord",
                        sub = if (strategyName != null) "$strategyName, выбран автоматически" else "Обход выключен",
                        mark = if (youtubeOn) OnbMark.Ok else OnbMark.Off,
                        dimmed = !youtubeOn
                    ),
                    if (deviceIsAndroid) {
                        OnbRowData(OnbIcons.PhoneSmall, "Устройство", "Телефон", OnbMark.Ok)
                    } else {
                        OnbRowData(null, "Устройство", "Smart TV", OnbMark.Ok, iconRes = R.drawable.ic_smart_tv)
                    }
                )
            )
            if (!youtubeOn) {
                OnbNote("Включить пропущенное можно на вкладке «YouTube и Discord».")
            }
        }
    ) {
        NfButton(
            text = "Начать пользоваться",
            enabled = ready,
            modifier = Modifier.focusRequester(focusRequester),
            onClick = {
                prefsOf(context).edit()
                    .putBoolean("telegram_proxy_enabled_by_user", wantsTelegramProxy ?: true)
                    .putBoolean("ui_show_telegram", wantsTelegramProxy ?: true)
                    .putBoolean("ui_show_youtube", wantsYoutubeBypass ?: true)
                    .apply()
                onNavigate(OnboardingStep.FinalGreeting)
            }
        )
    }
}

private class ConfettiDot(val angle: Float, val distance: Float, val radius: Float, val color: Color, val alpha: Float)

private val FinalConfetti = listOf(
    ConfettiDot(-120f, 120f, 3f, Nf.Green, 1f),
    ConfettiDot(-85f, 150f, 2f, Nf.Bright, 0.7f),
    ConfettiDot(-40f, 140f, 3f, Nf.Yellow, 1f),
    ConfettiDot(-10f, 165f, 2.5f, Nf.Bright, 1f),
    ConfettiDot(30f, 175f, 3f, Nf.Green, 0.8f),
    ConfettiDot(70f, 150f, 2.5f, Nf.Bright, 0.6f),
    ConfettiDot(110f, 170f, 3f, Nf.Yellow, 0.8f),
    ConfettiDot(150f, 155f, 2f, Nf.Green, 1f),
    ConfettiDot(190f, 125f, 2f, Nf.Green, 0.6f),
    ConfettiDot(-155f, 145f, 2.5f, Nf.Bright, 0.5f),
    ConfettiDot(55f, 205f, 2f, Nf.Yellow, 0.9f),
    ConfettiDot(-65f, 195f, 2.5f, Nf.Green, 0.9f)
)

@Composable
private fun FinalGreetingScreen(onCompleted: () -> Unit) {
    val animationsEnabled = rememberAnimationsEnabled()
    val enter = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    val wave = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val hint = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!animationsEnabled) {
            delay(1200)
            onCompleted()
            return@LaunchedEffect
        }
        launch { hint.animateTo(1f, tween(1750, easing = LinearEasing)) }
        launch { enter.animateTo(1f, tween(550, easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.15f))) }
        delay(350)
        launch { wave.animateTo(1f, tween(800, easing = LinearOutSlowInEasing)) }
        launch { burst.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
        delay(1000)
        exit.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        delay(50)
        onCompleted()
    }

    val textProgress = ((enter.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                scaleX = 1f + 0.04f * exit.value
                scaleY = 1f + 0.04f * exit.value
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }
                        .nfAura(Nf.Green.copy(alpha = 0.18f))
                )
                Canvas(modifier = Modifier.size(300.dp)) {
                    val stroke = Stroke(width = 1.dp.toPx())
                    val fade = enter.value.coerceIn(0f, 1f)
                    drawCircle(Nf.Green.copy(alpha = 0.10f * fade), radius = size.minDimension / 2f * 0.987f, style = stroke)
                    drawCircle(Nf.Green.copy(alpha = 0.22f * fade), radius = size.minDimension / 2f * 0.747f, style = Stroke(1.5.dp.toPx()))
                    drawCircle(Nf.Green.copy(alpha = 0.14f * fade), radius = size.minDimension / 2f * 0.507f, style = stroke)
                    if (wave.value in 0.001f..0.999f) {
                        drawCircle(
                            color = Nf.Green.copy(alpha = (1f - wave.value) * 0.5f),
                            radius = (48f + (150f - 48f) * wave.value).dp.toPx(),
                            style = Stroke(2.dp.toPx())
                        )
                    }
                    FinalConfetti.forEach { dot ->
                        val t = burst.value
                        if (t > 0f) {
                            val rad = dot.angle * PI.toFloat() / 180f
                            val distance = dot.distance.dp.toPx() * t
                            drawCircle(
                                color = dot.color.copy(alpha = dot.alpha * (t * 3f).coerceIn(0f, 1f) * (1f - 0.3f * t)),
                                radius = dot.radius.dp.toPx(),
                                center = Offset(
                                    center.x + cos(rad) * distance,
                                    center.y + sin(rad) * distance + 20.dp.toPx() * t * t
                                )
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.6f + 0.4f * enter.value
                            scaleY = 0.6f + 0.4f * enter.value
                            alpha = enter.value.coerceIn(0f, 1f)
                        }
                        .nfHeartbeat()
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Nf.Glass)
                        .border(1.5.dp, Nf.Green.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val checkProgress = ((enter.value - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    Icon(
                        imageVector = OnbIcons.Check,
                        contentDescription = null,
                        tint = Nf.Green,
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                scaleX = checkProgress
                                scaleY = checkProgress
                                alpha = checkProgress
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = textProgress
                        translationY = (1f - textProgress) * 24.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Приятного пользования!",
                    color = Nf.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "NetFix уже работает. Открываем главный экран.",
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .size(40.dp, 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Nf.TextSub, size = Size(size.width * hint.value, size.height))
                    }
            )
        }
    }
}
