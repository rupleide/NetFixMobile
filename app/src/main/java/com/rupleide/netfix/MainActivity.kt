package com.rupleide.netfix

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.Canvas
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import com.rupleide.netfix.data.performanceModeGlobal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.rupleide.netfix.ui.MainTab
import com.rupleide.netfix.ui.TgProxyTab
import com.rupleide.netfix.ui.SettingsTab
import com.rupleide.netfix.ui.InfoTab
import com.rupleide.netfix.ui.YoutubeTab
import com.rupleide.netfix.ui.OnboardingFlow
import com.rupleide.netfix.ui.theme.NetFixMobileTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.graphics.Paint
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.exp
import kotlin.random.Random

import com.rupleide.netfix.ui.rememberEntranceProgress
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.drawscope.Stroke

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        com.rupleide.netfix.core.debug.AppDebugManager.init(this)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            var showSplash by remember { mutableStateOf(splashNotShownYet) }
            var mainContentVisible by remember { mutableStateOf(false) }
            val prefs = remember { context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE) }
            val storedVal = prefs.getBoolean("onboarding_completed", false)
            Log.e("NetFixDebug", "MainActivity: onboarding_completed read as: $storedVal")
            var onboardingCompleted by remember { mutableStateOf(storedVal) }
            val isSmartTv = prefs.getBoolean("is_smart_tv", false)
            var setupDone by remember { mutableStateOf(prefs.getBoolean("wizard_is_setup_complete", false)) }
            var wantsYoutubeBypass by remember { mutableStateOf(prefs.getBoolean("wants_youtube_bypass", true)) }
            var telegramProxyEnabledByUser by remember { mutableStateOf(prefs.getBoolean("telegram_proxy_enabled_by_user", true)) }
            var selectedTab by remember { mutableIntStateOf(0) }

            androidx.compose.runtime.DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == "wizard_is_setup_complete") {
                        setupDone = sharedPreferences.getBoolean("wizard_is_setup_complete", false)
                    }
                    if (key == "wants_youtube_bypass") {
                        wantsYoutubeBypass = sharedPreferences.getBoolean("wants_youtube_bypass", true)
                        if (!wantsYoutubeBypass && selectedTab == 2) {
                            selectedTab = 0
                        }
                    }
                    if (key == "telegram_proxy_enabled_by_user") {
                        telegramProxyEnabledByUser = sharedPreferences.getBoolean("telegram_proxy_enabled_by_user", true)
                        if (!telegramProxyEnabledByUser && selectedTab == 1) {
                            selectedTab = 0
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val tabFocusRequesters = remember { List(5) { FocusRequester() } }
            val navBarFocusRequester = remember { FocusRequester() }
            val allNavItems = listOf(
                0 to NavItem("Главная", R.drawable.ic_power),
                1 to NavItem("Прокси", R.drawable.ic_telegram),
                2 to NavItem("Ютуб", R.drawable.ic_youtube),
                3 to NavItem("Настройки", R.drawable.ic_settings),
                4 to NavItem("Информация", R.drawable.ic_info)
            )
            val activeNavItems = remember(telegramProxyEnabledByUser, wantsYoutubeBypass) {
                allNavItems.filter { (idx, _) ->
                    when (idx) {
                        1 -> telegramProxyEnabledByUser
                        2 -> wantsYoutubeBypass
                        else -> true
                    }
                }
            }

            LaunchedEffect(selectedTab) {
                com.rupleide.netfix.data.onNavigateToTab = { index ->
                    val isVpnRunning = com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running
                    if (isVpnRunning && index in listOf(1, 3)) {
                        android.widget.Toast.makeText(
                            context,
                            "Выключите обход, чтобы изменять настройки",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val tabName = when (index) {
                            0 -> "Главная"
                            1 -> "Telegram"
                            2 -> "YouTube"
                            3 -> "Настройки"
                            4 -> "О программе"
                            else -> "Вкладка $index"
                        }
                        com.rupleide.netfix.core.debug.AppDebugManager.log("Переход на вкладку: $tabName")
                        selectedTab = index
                    }
                }
            }

            LaunchedEffect(Unit) {
                val activity = context as? android.app.Activity
                val showMsg = activity?.intent?.getBooleanExtra("showUpdateInstalledMessage", false) ?: false
                if (showMsg) {
                    android.widget.Toast.makeText(context, "Обновление установлено", android.widget.Toast.LENGTH_SHORT).show()
                    activity?.intent?.removeExtra("showUpdateInstalledMessage")
                }
                com.rupleide.netfix.core.dpibypass.StrategyTestManager.init(context)
                val info = com.rupleide.netfix.core.update.UpdateManager.checkUpdate(context)
                if (info != null) {
                    com.rupleide.netfix.data.updateInfoGlobal = info
                }
                val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                com.rupleide.netfix.data.performanceModeGlobal = prefs.getBoolean("performance_mode", false)
                if (prefs.getBoolean("service_enabled", false)) {
                    prefs.edit().putBoolean("econom_mode", false).apply()
                    com.rupleide.netfix.service.WatchdogWorker.schedulePeriodicWork(context)
                    com.rupleide.netfix.service.WatchdogReceiver.scheduleWatchdogAlarm(context)
                }
                val autoConnect = prefs.getBoolean("auto_connect_on_start", false)
                if (autoConnect) {
                    val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
                    val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
                    prefs.edit().putBoolean("service_enabled", true).apply()
                    if (wantsYoutube) {
                        if (android.net.VpnService.prepare(context) == null) {
                            com.rupleide.netfix.core.dpibypass.ServiceManager.start(context, com.rupleide.netfix.data.Mode.VPN)
                        }
                    }
                    if (wantsTelegram) {
                        val openTg = prefs.getBoolean("open_tg_on_connect", true)
                        if (wantsYoutube) {
                            val alreadyConfigured = prefs.getBoolean("tg_proxy_configured", false)
                            if (openTg && !alreadyConfigured) {
                                com.rupleide.netfix.core.tgproxy.TgProxyController.startAsync(
                                    context,
                                    onSuccess = {
                                        val port = com.rupleide.netfix.core.tgproxy.TgProxyController.getPort(context)
                                        val secret = com.rupleide.netfix.core.tgproxy.TgProxyController.getOrGenerateSecret(context)
                                        val url = com.rupleide.netfix.core.tgproxy.TgProxyController.getTgProxyUrl(
                                            com.rupleide.netfix.core.tgproxy.TgProxyController.DEFAULT_BIND_IP,
                                            port,
                                            secret
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                            prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                                        } catch (_: Exception) {}
                                    },
                                    onError = {}
                                )
                            } else {
                                com.rupleide.netfix.core.tgproxy.TgProxyController.startAsync(context, {}, {})
                            }
                        } else {
                            val intent = Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                                action = com.rupleide.netfix.data.START_ACTION
                                putExtra("open_tg", openTg)
                            }
                            androidx.core.content.ContextCompat.startForegroundService(context, intent)
                        }
                    }
                }
            }

            NetFixMobileTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!onboardingCompleted) {
                        OnboardingFlow(
                            onCompleted = {
                                Log.e("NetFixDebug", "MainActivity: Setting onboarding_completed to true (calling commit)")
                                prefs.edit().putBoolean("onboarding_completed", true).commit()
                                Log.e("NetFixDebug", "MainActivity: onboarding_completed applied, memory cache is: " + prefs.getBoolean("onboarding_completed", false))
                                onboardingCompleted = true
                                wantsYoutubeBypass = prefs.getBoolean("wants_youtube_bypass", true)
                                telegramProxyEnabledByUser = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
                                selectedTab = 0
                            }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color(0xFF161616)
                        ) { innerPadding ->
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
                            val navOverlayReserve = safeBottomInset + 86.dp

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    transitionSpec = {
                                        if (com.rupleide.netfix.data.performanceModeGlobal) {
                                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                        } else {
                                            val forward = targetState > initialState
                                            (fadeIn(tween(220)) + scaleIn(
                                                tween(220),
                                                initialScale = if (forward) 0.96f else 1.04f
                                            )) togetherWith (fadeOut(tween(180)) + scaleOut(
                                                tween(180),
                                                targetScale = if (forward) 1.04f else 0.96f
                                            ))
                                        }
                                    },
                                    label = "tabTransition"
                                ) { tab ->
                                    when (tab) {
                                        0 -> MainTab(
                                            focusRequester = tabFocusRequesters[0],
                                            navBarFocusRequester = navBarFocusRequester,
                                            playEntranceAnimation = mainContentVisible && selectedTab == 0
                                        )
                                        1 -> TgProxyTab(focusRequester = tabFocusRequesters[1], navBarFocusRequester = navBarFocusRequester)
                                        2 -> YoutubeTab(focusRequester = tabFocusRequesters[2], navBarFocusRequester = navBarFocusRequester)
                                        3 -> SettingsTab(focusRequester = tabFocusRequesters[3], navBarFocusRequester = navBarFocusRequester)
                                        4 -> InfoTab(focusRequester = tabFocusRequesters[4], navBarFocusRequester = navBarFocusRequester)
                                    }
                                }

                                val navBarProgress = rememberEntranceProgress(
                                    play = mainContentVisible,
                                    delayMillis = 300L,
                                    dampingRatio = 0.82f,
                                    stiffness = 150f
                                )
                                val navBarAlpha = navBarProgress.coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .graphicsLayer {
                                            alpha = navBarAlpha
                                            translationY = (1f - navBarAlpha) * 120.dp.toPx()
                                        }
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !com.rupleide.netfix.data.isActionSheetVisibleGlobal,
                                        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))) +
                                                androidx.compose.animation.slideInVertically(
                                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 160f)
                                                ) { fullHeight -> fullHeight },
                                        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f))) +
                                               androidx.compose.animation.slideOutVertically(
                                                   animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f))
                                               ) { fullHeight -> fullHeight },
                                        modifier = Modifier
                                    ) {
                                        ProxyNavigationBar(
                                            activeNavItems = activeNavItems,
                                            selectedTab = selectedTab,
                                            tabFocusRequester = tabFocusRequesters[selectedTab],
                                            navBarFocusRequester = navBarFocusRequester,
                                            showYoutubeAlert = onboardingCompleted && wantsYoutubeBypass && !setupDone,
                                            onTabSelected = { originalIndex ->
                                                val isVpnRunning = com.rupleide.netfix.data.appStatus.first == com.rupleide.netfix.data.AppStatus.Running
                                                if (isVpnRunning && originalIndex in listOf(1, 3)) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Выключите обход, чтобы изменять настройки",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    selectedTab = originalIndex
                                                }
                                            },
                                            modifier = Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showSplash) {
                        NetFixSplashScreen(onDismiss = {
                            showSplash = false
                            splashNotShownYet = false
                            mainContentVisible = true
                        })
                    } else if (!mainContentVisible) {
                        mainContentVisible = true
                    }
                }
            }
        }
    }

    companion object {
        var splashNotShownYet = true
    }
}

private data class NavItem(
    val label: String,
    val iconRes: Int
)

@Composable
private fun ProxyNavigationBar(
    activeNavItems: List<Pair<Int, NavItem>>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabFocusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    showYoutubeAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedColor = Color(0xFFF4F4F5)
    val unselectedColor = Color(0xFF71717A)
    val shellColor = Color(0xFF1E1E1E)
    val shellBorder = Color(0x22FFFFFF)
    val indicatorColor = Color(0xFF2D2D30)

    val selectedActiveIndex = activeNavItems.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
    val indicatorIndex = remember { androidx.compose.animation.core.Animatable(selectedActiveIndex.toFloat()) }
    val dragVisualIndex = indicatorIndex.value

    LaunchedEffect(selectedActiveIndex) {
        if (performanceModeGlobal) {
            indicatorIndex.snapTo(selectedActiveIndex.toFloat())
        } else {
            indicatorIndex.animateTo(
                targetValue = selectedActiveIndex.toFloat(),
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f)
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 550.dp)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        val trackPadding = 8.dp
        val itemWidth = (maxWidth - trackPadding * 2) / activeNavItems.size
        val indicatorOffset = trackPadding + itemWidth * dragVisualIndex

        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = shellColor,
            border = BorderStroke(1.dp, shellBorder),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = indicatorColor,
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .padding(vertical = 6.dp)
                        .width(itemWidth)
                        .fillMaxHeight()
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = trackPadding, vertical = 6.dp)
                ) {
                    activeNavItems.forEachIndexed { activeIndex, (originalIndex, item) ->
                        val emphasis = (1f - abs(activeIndex - dragVisualIndex)).coerceIn(0f, 1f)
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val isHighlighted = isPressed || isHovered || isFocused

                        val tabBgColor by animateColorAsState(
                            targetValue = if (isHighlighted) Color(0x1AFFFFFF) else Color.Transparent,
                            animationSpec = tween(if (performanceModeGlobal) 0 else 150),
                            label = "tabBg"
                        )
                        val tabIconColor by animateColorAsState(
                            targetValue = if (isHighlighted) Color.White else lerp(unselectedColor, selectedColor, emphasis),
                            animationSpec = tween(if (performanceModeGlobal) 0 else 150),
                            label = "tabIcon"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(tabBgColor)
                                .then(
                                    if (originalIndex == selectedTab) Modifier.focusRequester(navBarFocusRequester) else Modifier
                                )
                                .focusProperties {
                                    up = tabFocusRequester
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onTabSelected(originalIndex) }
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp),
                                    tint = tabIconColor
                                )
                                if (originalIndex == 4 && com.rupleide.netfix.data.updateInfoGlobal != null) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 4.dp, y = (-2).dp)
                                            .size(7.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(0xFFEAB308))
                                    )
                                }
                                if (originalIndex == 2 && showYoutubeAlert) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 4.dp, y = (-2).dp)
                                            .size(7.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(0xFFEAB308))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme3TypeOverride.labelSmall,
                                fontWeight = if (emphasis > 0.55f) FontWeight.SemiBold else FontWeight.Medium,
                                color = tabIconColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private object MaterialTheme3TypeOverride {
    val labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
}

@Composable
fun NetFixSplashScreen(onDismiss: () -> Unit) {
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0f) }
    
    val bgScaleAnim = remember { Animatable(1.0f) }
    val bgAlphaAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val entryDuration = 550
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = entryDuration - 150, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = entryDuration,
                    easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.15f)
                )
            )
        }

        delay(entryDuration + 500L)

        val exitDuration = 400
        launch {
            bgAlphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = exitDuration, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1.12f,
                animationSpec = tween(durationMillis = exitDuration, easing = FastOutSlowInEasing)
            )
        }
        launch {
            bgScaleAnim.animateTo(
                targetValue = 1.04f,
                animationSpec = tween(durationMillis = exitDuration, easing = FastOutSlowInEasing)
            )
        }

        delay(exitDuration + 50L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = bgAlphaAnim.value
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScaleAnim.value
                    scaleY = bgScaleAnim.value
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF222224),
                            Color(0xFF131314)
                        ),
                        center = Offset.Unspecified,
                        radius = 850f
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer {
                    alpha = alphaAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_netfix_logo_splash),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
