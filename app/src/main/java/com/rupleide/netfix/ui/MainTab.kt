package com.rupleide.netfix.ui

import androidx.compose.ui.draw.drawWithCache
import com.rupleide.netfix.core.dpibypass.launchIntentWithLeanback
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.Search
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfTvButton
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.uiShowTelegram
import com.rupleide.netfix.ui.components.uiShowYoutube
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import android.content.res.Configuration
import android.net.TrafficStats
import android.net.Uri
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rupleide.netfix.ui.components.NetFixUpdateSheet
import androidx.compose.material.icons.filled.Info
import com.rupleide.netfix.ui.components.NetFixUnloadSheet
import com.rupleide.netfix.ui.components.UnloadOption
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.appStatus
import com.rupleide.netfix.ui.main.netFixGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.findRootCoordinates
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.core.dpibypass.shortName
import com.rupleide.netfix.ui.components.rememberNetFixHaptics
import com.rupleide.netfix.ui.components.launchVpnConsentSafely
import com.rupleide.netfix.data.STARTED_BROADCAST
import com.rupleide.netfix.data.STOPPED_BROADCAST
import com.rupleide.netfix.data.FAILED_BROADCAST
import com.rupleide.netfix.data.performanceModeGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

private const val AURA_CYCLE_SECONDS = 18f

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbAura(
    auraClock: Float,
    auraBaseColor: Color,
    auraAlpha: Float
) {
    val fullTurn = (2f * Math.PI).toFloat()
    val auraAngle = (auraClock % 6f) / 6f * fullTurn
    val auraSecond = fullTurn - (auraClock % 9f) / 9f * fullTurn
    val pulsePhase = (auraClock % 6f) / 3f
    val auraPulse = 0.65f + 0.2f * (if (pulsePhase < 1f) pulsePhase else 2f - pulsePhase)
    val orbitCenterX = size.width / 2f
    val orbitCenterY = size.height / 2f
    val orbitRadius = size.minDimension * 0.2f
    val firstSpotX = orbitCenterX + kotlin.math.cos(auraAngle) * orbitRadius
    val firstSpotY = orbitCenterY + kotlin.math.sin(auraAngle) * orbitRadius
    val secondSpotX = orbitCenterX + kotlin.math.cos(auraSecond) * orbitRadius * 0.7f
    val secondSpotY = orbitCenterY + kotlin.math.sin(auraSecond) * orbitRadius * 0.7f
    val spotRadius = size.minDimension * 1.3f * auraPulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(auraBaseColor.copy(alpha = auraAlpha), Color.Transparent),
            center = Offset(firstSpotX, firstSpotY),
            radius = spotRadius
        ),
        center = Offset(firstSpotX, firstSpotY),
        radius = spotRadius
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(auraBaseColor.copy(alpha = auraAlpha * 0.6f), Color.Transparent),
            center = Offset(secondSpotX, secondSpotY),
            radius = spotRadius * 0.75f
        ),
        center = Offset(secondSpotX, secondSpotY),
        radius = spotRadius * 0.75f
    )
}

private fun smoothVeilBrush(
    vertical: Boolean,
    startEnd: Float,
    endStart: Float,
    startAlpha: Float,
    endAlpha: Float
): Brush {
    val steps = 14
    val base = Color(0xFF161616)
    val stops = ArrayList<Pair<Float, Color>>(steps * 2 + 2)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val eased = t * t * (3f - 2f * t)
        stops += (startEnd * t) to base.copy(alpha = startAlpha * (1f - eased))
    }
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val eased = t * t * (3f - 2f * t)
        stops += (endStart + (1f - endStart) * t) to base.copy(alpha = endAlpha * eased)
    }
    val array = stops.toTypedArray()
    return if (vertical) Brush.verticalGradient(*array) else Brush.horizontalGradient(*array)
}

@Composable
fun MainTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    playEntranceAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val headerProgress = rememberEntranceProgress(playEntranceAnimation, delayMillis = 20L, dampingRatio = 1f, stiffness = 180f)
    val buttonProgress = rememberEntranceProgress(playEntranceAnimation, delayMillis = 90L, dampingRatio = 0.62f, stiffness = 150f)
    val cardProgress = rememberEntranceProgress(playEntranceAnimation, delayMillis = 190L, dampingRatio = 0.86f, stiffness = 160f)
    val quickButtonsProgress = rememberEntranceProgress(playEntranceAnimation, delayMillis = 250L, dampingRatio = 0.86f, stiffness = 160f)
    val globeRevealEnabled = com.rupleide.netfix.ui.main.rememberAnimationsEnabled()
    val globeReveal = remember { androidx.compose.animation.core.Animatable(if (globeRevealEnabled) 0f else 1f) }
    LaunchedEffect(playEntranceAnimation) {
        if (playEntranceAnimation && globeReveal.value < 1f) {
            globeReveal.animateTo(1f, tween(1540, delayMillis = 80, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }

    val headerAlpha = headerProgress.coerceIn(0f, 1f)
    val buttonAlpha = buttonProgress.coerceIn(0f, 1f)
    val buttonScale = 0.62f + 0.38f * buttonProgress
    val cardAlpha = cardProgress.coerceIn(0f, 1f)
    val quickButtonsAlpha = quickButtonsProgress.coerceIn(0f, 1f)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE) }
    var telegramProxyEnabledByUser by remember { mutableStateOf(prefs.getBoolean("telegram_proxy_enabled_by_user", true)) }
    var wantsYoutubeBypass by remember { mutableStateOf(prefs.getBoolean("wants_youtube_bypass", true)) }
    var showTelegramTile by remember { mutableStateOf(prefs.uiShowTelegram()) }
    var showYoutubeTile by remember { mutableStateOf(prefs.uiShowYoutube()) }

    androidx.compose.runtime.DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            showTelegramTile = sharedPreferences.uiShowTelegram()
            showYoutubeTile = sharedPreferences.uiShowYoutube()
            if (key == "telegram_proxy_enabled_by_user") {
                telegramProxyEnabledByUser = sharedPreferences.getBoolean("telegram_proxy_enabled_by_user", true)
            }
            if (key == "wants_youtube_bypass") {
                wantsYoutubeBypass = sharedPreferences.getBoolean("wants_youtube_bypass", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val realUpdateInfo = com.rupleide.netfix.data.updateInfoGlobal
    val forceUpdateTest = com.rupleide.netfix.data.forceUpdateTestGlobal
    val updateInfo = if (forceUpdateTest) {
        com.rupleide.netfix.core.update.UpdateManager.UpdateInfo(
            version = "3.1.0-test",
            downloadUrl = "https://github.com/rupleide/NetFixMobile/releases/download/v3.1.0-test/app-release.apk",
            description = "Тестовое обновление для проверки шторки.\n- Добавлена новая анимация\n- Оптимизирован расход памяти"
        )
    } else {
        realUpdateInfo
    }
    val autoUpdateEnabled = com.rupleide.netfix.core.update.UpdateManager.isAutoUpdateEnabled(context)
    var showUpdateDialog by remember(updateInfo, forceUpdateTest) {
        mutableStateOf(
            (updateInfo != null && autoUpdateEnabled && !com.rupleide.netfix.data.updateSheetDismissedThisSession) || forceUpdateTest
        )
    }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0f) }
    var updateErrorMessage by remember { mutableStateOf<String?>(null) }
    var isDownloadingSmartTube by remember { mutableStateOf(false) }
    var smartTubeDownloadProgress by remember { mutableStateOf(0f) }
    var smartTubeInstalled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showUnloadDialog by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }
    val hintPrefs = remember { context.getSharedPreferences("netfix_hints", Context.MODE_PRIVATE) }
    LaunchedEffect(showUpdateDialog, showUnloadDialog, showHintDialog) {
        com.rupleide.netfix.data.isActionSheetVisibleGlobal = showUpdateDialog || showUnloadDialog || showHintDialog
    }

    val lifecycleOwnerForSmartTube = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwnerForSmartTube) {
        lifecycleOwnerForSmartTube.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            smartTubeInstalled = com.rupleide.netfix.core.update.UpdateManager.isSmartTubeInstalled(context)
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxButtonSize = if (isLandscape) 200.dp else 280.dp

    val tvSingleFocus = com.rupleide.netfix.ui.components.LocalIsTv.current
    LaunchedEffect(Unit) {
        repeat(if (tvSingleFocus) 1 else 8) { index ->
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            delay(100L + index * 50L)
        }
    }

    var currentStatus by remember { mutableStateOf(appStatus.first) }
    var isTgProxyRunning by remember { mutableStateOf(com.rupleide.netfix.data.isTgProxyRunningGlobal) }
    var isStarting by remember { mutableStateOf(false) }
    var isStopping by remember { mutableStateOf(false) }

    LaunchedEffect(isStopping) {
        if (isStopping) {
            delay(6_000)
            isStopping = false
        }
    }

    var lastStartFailed by remember { mutableStateOf(false) }

    LaunchedEffect(currentStatus, isStarting, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                currentStatus = appStatus.first
                if (isStopping && currentStatus != AppStatus.Running && !com.rupleide.netfix.core.tgproxy.TgProxyController.isRunning) {
                    isStopping = false
                }
                val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
                val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
                var open = false
                if (wantsTelegram) {
                    val port = TgProxyController.getPort(context)
                    open = withContext(Dispatchers.IO) {
                        TgProxyController.isPortOpen(
                            TgProxyController.DEFAULT_BIND_IP,
                            port,
                            500
                        )
                    }
                    isTgProxyRunning = open
                    com.rupleide.netfix.data.isTgProxyRunningGlobal = open
                } else {
                    isTgProxyRunning = false
                    com.rupleide.netfix.data.isTgProxyRunningGlobal = false
                }
                val conditionToStopStarting = when {
                    wantsYoutube && wantsTelegram -> currentStatus == AppStatus.Running && open
                    wantsYoutube -> currentStatus == AppStatus.Running
                    wantsTelegram -> open
                    else -> true
                }
                if (conditionToStopStarting) {
                    isStarting = false
                }
                delay(if (isStarting) 1000 else 5000)
            }
        }
    }

    LaunchedEffect(isStarting) {
        if (isStarting) {
            delay(12_000)
            isStarting = false
        }
    }

    val isRunning = when {
        isStopping -> false
        wantsYoutubeBypass && telegramProxyEnabledByUser -> isTgProxyRunning && (currentStatus == AppStatus.Running || !isStarting)
        wantsYoutubeBypass -> currentStatus == AppStatus.Running
        telegramProxyEnabledByUser -> isTgProxyRunning
        else -> false
    }
    val telegramOnly = isRunning && wantsYoutubeBypass && telegramProxyEnabledByUser && currentStatus != AppStatus.Running

    val isPending = when {
        isStopping -> false
        wantsYoutubeBypass && telegramProxyEnabledByUser -> isStarting || (currentStatus == AppStatus.Running && !isTgProxyRunning)
        wantsYoutubeBypass -> isStarting && currentStatus != AppStatus.Running
        telegramProxyEnabledByUser -> isStarting && !isTgProxyRunning
        else -> false
    }

    val connectionHaptics = rememberNetFixHaptics()
    var previousIsRunning by remember { mutableStateOf(isRunning) }
    var previousIsPending by remember { mutableStateOf(isPending) }
    LaunchedEffect(isRunning, isPending) {
        when {
            isRunning || isPending -> lastStartFailed = false
            !isRunning && previousIsRunning -> connectionHaptics(NetFixHaptic.ToggleOff)
            previousIsPending && !isStopping -> {
                lastStartFailed = true
                connectionHaptics(NetFixHaptic.Reject)
            }
        }
        previousIsRunning = isRunning
        previousIsPending = isPending
    }

    val rawGlobeStatus = when {
        isRunning -> com.rupleide.netfix.ui.globe.GlobeStatus.Connected
        isPending -> com.rupleide.netfix.ui.globe.GlobeStatus.Connecting
        lastStartFailed -> com.rupleide.netfix.ui.globe.GlobeStatus.Error
        else -> com.rupleide.netfix.ui.globe.GlobeStatus.Off
    }
    val globeStatus = com.rupleide.netfix.ui.main.rememberDebouncedGlobeStatus(rawGlobeStatus)

    LaunchedEffect(isRunning) {
        if (isRunning) {
            if (com.rupleide.netfix.data.connectionStartTime == 0L) {
                com.rupleide.netfix.data.connectionStartTime = System.currentTimeMillis()
            }
        } else {
            com.rupleide.netfix.data.connectionStartTime = 0L
        }
    }

    var latencyMs by remember { mutableStateOf(-1) }

    LaunchedEffect(currentStatus, performanceModeGlobal, lifecycleOwner) {
        if (currentStatus != AppStatus.Running) {
            latencyMs = -1
        } else {
            if (performanceModeGlobal) {
                latencyMs = withContext(Dispatchers.IO) {
                    TgProxyController.measureLatency("1.1.1.1", 53)
                }
            } else {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (true) {
                        latencyMs = withContext(Dispatchers.IO) {
                            TgProxyController.measureLatency("1.1.1.1", 53)
                        }
                        delay(20000)
                    }
                }
            }
        }
    }

    fun startAll() {
        isStopping = false
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
        com.rupleide.netfix.core.debug.AppDebugManager.log("Запуск всех служб (YouTube: $wantsYoutube, Telegram: $wantsTelegram)")
        isStarting = true
        prefs.edit()
            .putBoolean("service_enabled", true)
            .apply()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, com.rupleide.netfix.service.NetFixTileService::class.java)
            )
        } catch (_: Exception) {}
        
        if (wantsYoutube) {
            com.rupleide.netfix.core.debug.AppDebugManager.log("Запуск VPN ByeDPI")
            ServiceManager.start(context, Mode.VPN)
        }

        if (wantsTelegram) {
            val openTg = prefs.getBoolean("open_tg_on_connect", true)
            if (wantsYoutube) {
                TgProxyController.startAsync(
                    context = context,
                    onSuccess = {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isStarting = false
                        }
                        val port = TgProxyController.getPort(context)
                        val secret = TgProxyController.getOrGenerateSecret(context)
                        val url = TgProxyController.getTgProxyUrl(
                            TgProxyController.DEFAULT_BIND_IP,
                            port,
                            secret
                        )
                        val alreadyConfigured = prefs.getBoolean("tg_proxy_configured", false)
                        if (openTg && !alreadyConfigured) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                                prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                            } catch (e: Exception) {
                                Log.e("MainTab", "Failed to open Telegram link", e)
                            }
                        }
                    },
                    onError = {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isStarting = false
                        }
                        Log.e("MainTab", "Failed to start TG proxy")
                    }
                )
            } else {
                val intent = Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                    action = com.rupleide.netfix.data.START_ACTION
                    putExtra("open_tg", openTg)
                }
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            }
        } else {
            if (!wantsYoutube) {
                isStarting = false
            }
        }
    }

    fun stopAll() {
        isStopping = true
        com.rupleide.netfix.core.debug.AppDebugManager.log("Остановка всех служб")
        isStarting = false
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("service_enabled", false)
            .apply()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, com.rupleide.netfix.service.NetFixTileService::class.java)
            )
        } catch (_: Exception) {}
        ServiceManager.stop(context)
        val intent = Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
            action = com.rupleide.netfix.data.STOP_ACTION
        }
        context.startService(intent)
        TgProxyController.stop()
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startAll()
        }
    }

    fun handleNormalStart() {
        prefs.edit()
            .putBoolean("wants_youtube_bypass", prefs.uiShowYoutube())
            .putBoolean("telegram_proxy_enabled_by_user", prefs.uiShowTelegram())
            .apply()
        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        if (wantsYoutube) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                vpnLauncher.launchVpnConsentSafely(intent)
            } else {
                startAll()
            }
        } else {
            startAll()
        }
    }

    LaunchedEffect(com.rupleide.netfix.data.tileStartRequestedGlobal) {
        if (com.rupleide.netfix.data.tileStartRequestedGlobal) {
            com.rupleide.netfix.data.tileStartRequestedGlobal = false
            if (!isRunning && !isStarting) {
                handleNormalStart()
            }
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentStatus = appStatus.first
                if (currentStatus == AppStatus.Halted) {
                    isStarting = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(STARTED_BROADCAST)
            addAction(STOPPED_BROADCAST)
            addAction(FAILED_BROADCAST)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val networkKind = com.rupleide.netfix.ui.main.rememberCurrentNetworkKind()
    val performanceMode = performanceModeGlobal
    val isSmartTvDevice = remember { prefs.getBoolean("is_smart_tv", false) }
    val tvMode = com.rupleide.netfix.ui.components.LocalIsTv.current
    val isLowEndDevice = com.rupleide.netfix.ui.main.rememberIsLowEndDevice()
    val isGlassBlurAllowed = !performanceMode && !isSmartTvDevice && !isLandscape && !isLowEndDevice
    val glassHazeState = if (isGlassBlurAllowed) dev.chrisbanes.haze.rememberHazeState() else null
    val globeQuality = com.rupleide.netfix.ui.globe.rememberGlobeQuality(performanceMode = performanceMode || isLowEndDevice)
    var orbCenterYFraction by remember { mutableStateOf(0.40f) }
    val globeHome = remember { com.rupleide.netfix.ui.globe.GlobeDefaults.homeForDevice() }
    var landscapeOrbCenterXFraction by remember { mutableStateOf(0.76f) }
    var landscapeOrbCenterYFraction by remember { mutableStateOf(0.46f) }
    var globeEndpoints by remember { mutableStateOf(com.rupleide.netfix.ui.globe.GlobeDefaults.randomEndpoints(globeHome)) }
    LaunchedEffect(rawGlobeStatus) {
        if (rawGlobeStatus == com.rupleide.netfix.ui.globe.GlobeStatus.Connecting) {
            globeEndpoints = com.rupleide.netfix.ui.globe.GlobeDefaults.randomEndpoints(globeHome)
        }
    }
    val headerTextShadow = with(LocalDensity.current) {
        Shadow(color = Color(0xE6161616), blurRadius = 16.dp.toPx())
    }
    val animationsEnabled = com.rupleide.netfix.ui.main.rememberAnimationsEnabled()
    val latencyCardEntrance = if (animationsEnabled) cardAlpha else 1f
    val quickCardsEntrance = if (animationsEnabled) quickButtonsAlpha else 1f

    val auraBaseColor by animateColorAsState(
        targetValue = when {
            isRunning -> Color(0xFF22C55E)
            isPending -> Color(0xFF3B82F6)
            lastStartFailed -> Color(0xFFEF4444)
            else -> Color(0xFFD4D4D8)
        },
        animationSpec = tween(durationMillis = 1200, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
        label = "auraColor"
    )
    val auraAlpha by animateFloatAsState(
        targetValue = when {
            isRunning -> 0.28f
            isPending -> 0.22f
            else -> 0.13f
        },
        animationSpec = tween(durationMillis = 1200, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
        label = "auraAlpha"
    )
    val auraClock = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val onOrbClick: () -> Unit = {
        connectionHaptics(NetFixHaptic.Tap)
        if (isRunning || isPending) {
            stopAll()
        } else {
            val hintCount = hintPrefs.getInt("long_press_hint_count", 0)
            if (hintCount < 1) {
                hintPrefs.edit().putInt("long_press_hint_count", hintCount + 1).apply()
                showHintDialog = true
            } else {
                handleNormalStart()
            }
        }
    }
    val onOrbLongClick: () -> Unit = {
        connectionHaptics(NetFixHaptic.LongPress)
        showUnloadDialog = true
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        val screenHeight = maxHeight
        val screenHeightPx = constraints.maxHeight.toFloat()
        val buttonSize = if (isLandscape) {
            (configuration.screenWidthDp.dp * 0.28f).coerceIn(180.dp, maxButtonSize)
        } else {
            val topReserve = 180.dp
            val bottomReserve = navOverlayReserve + 142.dp
            val availableHeightForButton = maxHeight - topReserve - bottomReserve - 24.dp
            (configuration.screenWidthDp.dp * 0.72f)
                .coerceAtMost(availableHeightForButton)
                .coerceIn(150.dp, maxButtonSize)
        }

        if (isLandscape && !performanceMode) {
            com.rupleide.netfix.ui.globe.NetFixGlobeBackground(
                status = globeStatus,
                quality = globeQuality,
                home = globeHome,
                endpoints = globeEndpoints,
                centerXFraction = landscapeOrbCenterXFraction,
                centerYFraction = landscapeOrbCenterYFraction,
                radiusFraction = 0.95f,
                radiusOfHeight = true,
                ambientClock = auraClock,
                ambientPeriod = AURA_CYCLE_SECONDS,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val veilVertical = if (tvMode) {
                            smoothVeilBrush(vertical = true, startEnd = 0.18f, endStart = 0.85f, startAlpha = 0.45f, endAlpha = 0.45f)
                        } else {
                            smoothVeilBrush(vertical = true, startEnd = 0.22f, endStart = 0.78f, startAlpha = 0.9f, endAlpha = 0.9f)
                        }
                        val veilHorizontal = if (tvMode) {
                            smoothVeilBrush(vertical = false, startEnd = 0.35f, endStart = 0.97f, startAlpha = 0.3f, endAlpha = 0.15f)
                        } else {
                            smoothVeilBrush(vertical = false, startEnd = 0.5f, endStart = 0.95f, startAlpha = 0.85f, endAlpha = 0.5f)
                        }
                        onDrawWithContent {
                            drawContent()
                            drawRect(brush = veilVertical)
                            drawRect(brush = veilHorizontal)
                            drawRect(Color(0xFF161616), alpha = 1f - globeReveal.value)
                        }
                    }
            )
        }

        if (!isLandscape && !performanceMode) {
            com.rupleide.netfix.ui.globe.NetFixGlobeBackground(
                status = globeStatus,
                quality = globeQuality,
                home = globeHome,
                endpoints = globeEndpoints,
                centerYFraction = orbCenterYFraction,
                radiusFraction = 0.8f,
                ambientClock = auraClock,
                ambientPeriod = AURA_CYCLE_SECONDS,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (glassHazeState != null) Modifier.hazeSource(glassHazeState) else Modifier
                    )
                    .drawWithCache {
                        val veil = smoothVeilBrush(vertical = true, startEnd = 0.24f, endStart = 0.62f, startAlpha = 1f, endAlpha = 1f)
                        onDrawWithContent {
                            drawContent()
                            drawRect(brush = veil)
                            drawRect(Color(0xFF161616), alpha = 1f - globeReveal.value)
                        }
                    }
            )
        }

        if (tvMode) {
            val tgEnabled = isRunning
            val ytEnabled = currentStatus == AppStatus.Running
            val openTelegram: () -> Unit = {
                val port = TgProxyController.getPort(context)
                val secret = TgProxyController.getOrGenerateSecret(context)
                val url = TgProxyController.getTgProxyUrl(TgProxyController.DEFAULT_BIND_IP, port, secret)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                } catch (_: Exception) {}
            }
            var ytTick by remember { mutableIntStateOf(0) }
            var ytAwaitStart by remember { mutableStateOf(false) }
            var showYoutubeCheck by remember { mutableStateOf(false) }
            var showYoutubeVerdict by remember { mutableStateOf(false) }
            var showYoutubeFailed by remember { mutableStateOf(false) }
            var ytFailedReason by remember { mutableStateOf("") }
            val ytTickRead = ytTick

            fun installSmartTube() {
                if (!isDownloadingSmartTube) {
                    scope.launch {
                        isDownloadingSmartTube = true
                        val url = com.rupleide.netfix.core.update.UpdateManager.getSmartTubeLatestUrl()
                        com.rupleide.netfix.core.update.UpdateManager.downloadAndInstallApk(
                            context = context,
                            downloadUrl = url,
                            fileName = "smarttube.apk",
                            onProgress = { progress -> smartTubeDownloadProgress = progress },
                            onError = { isDownloadingSmartTube = false }
                        )
                        isDownloadingSmartTube = false
                    }
                }
            }

            fun officialYoutubeIntent(): Intent? {
                val pm = context.packageManager
                return pm.launchIntentWithLeanback("com.google.android.youtube.tv")
                    ?: pm.launchIntentWithLeanback("com.google.android.youtube")
            }

            fun launchOfficialYoutube() {
                val intent = officialYoutubeIntent()
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }

            fun startYoutubeCheck() {
                showYoutubeCheck = false
                if (officialYoutubeIntent() == null) {
                    ytFailedReason = "Официальный YouTube не установлен на этом ТВ."
                    showYoutubeFailed = true
                } else {
                    ytAwaitStart = true
                }
            }

            LaunchedEffect(ytAwaitStart) {
                if (!ytAwaitStart) return@LaunchedEffect
                if (currentStatus != AppStatus.Running) {
                    handleNormalStart()
                }
                var waited = 0
                while (currentStatus != AppStatus.Running && waited < 40_000) {
                    delay(300)
                    waited += 300
                }
                if (currentStatus == AppStatus.Running) {
                    prefs.edit().putBoolean("yt_check_pending", true).commit()
                    launchOfficialYoutube()
                } else {
                    android.widget.Toast.makeText(context, "Не удалось включить обход", android.widget.Toast.LENGTH_SHORT).show()
                }
                ytAwaitStart = false
            }

            LaunchedEffect(Unit) {
                if (prefs.getBoolean("yt_check_open_now", false)) {
                    delay(900)
                    showYoutubeCheck = true
                }
            }
            LaunchedEffect(showYoutubeCheck) {
                if (showYoutubeCheck) {
                    delay(500)
                    prefs.edit().putBoolean("yt_check_open_now", false).apply()
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && prefs.getBoolean("yt_check_pending", false)) {
                        showYoutubeVerdict = true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val ytModeNow = prefs.getString("youtube_mode", "official") ?: "official"
            val checkKind = networkKind ?: com.rupleide.netfix.core.dpibypass.NetworkKind.Wifi
            val scanYoutubeOk = StrategyTestManager.resultFor(checkKind, StrategyTestManager.selectedStrategyFor(context, checkKind))
                ?.let { it.youtubeOk >= 4 } == true
            val checkAlreadyHandled = prefs.getBoolean("yt_check_failed", false) || prefs.getBoolean("yt_check_done", false)
            val offerYoutubeCheck = ytTickRead >= 0 && ytModeNow == "smarttube" && scanYoutubeOk && !checkAlreadyHandled && officialYoutubeIntent() != null
            val openYoutube: () -> Unit = {
                if (ytAwaitStart) {
                    Unit
                } else if (ytModeNow == "smarttube") {
                    if (offerYoutubeCheck) {
                        showYoutubeCheck = true
                    } else if (smartTubeInstalled) {
                        com.rupleide.netfix.core.update.UpdateManager.openSmartTube(context)
                    } else {
                        installSmartTube()
                    }
                } else {
                    launchOfficialYoutube()
                }
            }
            val statusDot = when (globeStatus) {
                com.rupleide.netfix.ui.globe.GlobeStatus.Connected -> Nf.Green
                com.rupleide.netfix.ui.globe.GlobeStatus.Error -> Nf.Red
                com.rupleide.netfix.ui.globe.GlobeStatus.Connecting -> Nf.Bright
                com.rupleide.netfix.ui.globe.GlobeStatus.Off -> Nf.Muted
            }
            val statusTitle = when (globeStatus) {
                com.rupleide.netfix.ui.globe.GlobeStatus.Connected -> if (telegramOnly) "Работает только Telegram" else "Обход работает"
                com.rupleide.netfix.ui.globe.GlobeStatus.Error -> "Не удалось подключиться"
                com.rupleide.netfix.ui.globe.GlobeStatus.Connecting -> "Подключаемся…"
                com.rupleide.netfix.ui.globe.GlobeStatus.Off -> "Обход выключен"
            }
            val statusSub = buildString {
                networkKind?.shortName?.let { append(it) }
                if (globeStatus == com.rupleide.netfix.ui.globe.GlobeStatus.Connected && latencyMs >= 0) {
                    if (isNotEmpty()) append(" · ")
                    append("$latencyMs мс")
                }
            }
            val tgSubtitle = if (tgEnabled) "Прокси работает" else "Сначала включите обход"
            val ytMode = prefs.getString("youtube_mode", "official") ?: "official"
            val ytTitle = if (ytMode == "smarttube") "SmartTube" else "YouTube"
            val ytSubtitle = when {
                ytAwaitStart -> "Включаем обход…"
                isDownloadingSmartTube -> "Скачивание: ${(smartTubeDownloadProgress * 100).toInt()}%"
                offerYoutubeCheck -> "Проверить работу"
                ytMode == "smarttube" && !smartTubeInstalled -> "Нажмите, чтобы установить"
                ytEnabled -> "Готов к запуску"
                else -> "Сначала включите обход"
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp + com.rupleide.netfix.ui.components.NfTv.RailWidth, end = 48.dp, top = 40.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusDot))
                        Text(statusTitle, color = Nf.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (statusSub.isNotEmpty()) {
                            Text(statusSub, color = Nf.TextSub, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInRoot()
                                val rootSize = coordinates.findRootCoordinates().size
                                if (rootSize.width > 0 && rootSize.height > 0) {
                                    landscapeOrbCenterXFraction =
                                        ((position.x + coordinates.size.width / 2f) / rootSize.width).coerceIn(0.2f, 0.95f)
                                    landscapeOrbCenterYFraction =
                                        ((position.y + coordinates.size.height / 2f) / rootSize.height).coerceIn(0.2f, 0.8f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val tvOrbSize = (screenHeight - 130.dp).coerceIn(180.dp, 320.dp)
                        Box(modifier = Modifier.size(tvOrbSize), contentAlignment = Alignment.Center) {
                            if (!performanceMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .graphicsLayer { }
                                        .drawBehind { drawOrbAura(auraClock.floatValue, auraBaseColor, auraAlpha) }
                                )
                            }
                            com.rupleide.netfix.ui.main.NetFixPowerOrb(
                                status = globeStatus,
                                onClick = onOrbClick,
                                onLongClick = onOrbLongClick,
                                focusRequester = focusRequester,
                                navBarFocusRequester = navBarFocusRequester,
                                orbSize = tvOrbSize,
                            telegramOnly = telegramOnly
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .width(352.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Сервисы", color = Nf.TextSub, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (showTelegramTile) {
                            TvServiceCard(
                                iconRes = R.drawable.ic_telegram,
                                title = "Telegram",
                                subtitle = tgSubtitle,
                                active = tgEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = openTelegram
                            )
                        }
                        if (showYoutubeTile) {
                            TvServiceCard(
                                iconRes = R.drawable.ic_youtube,
                                title = ytTitle,
                                subtitle = ytSubtitle,
                                active = ytEnabled || offerYoutubeCheck || (ytMode == "smarttube" && !smartTubeInstalled),
                                modifier = Modifier.weight(1f),
                                onClick = openYoutube
                            )
                        }
                    }
                    com.rupleide.netfix.ui.main.LatencyStrategyCard(
                        status = globeStatus,
                        latencyMs = latencyMs,
                        strategyName = StrategyTestManager.nameForNetwork(context, networkKind),
                        networkName = networkKind?.shortName,
                        onStrategyClick = { com.rupleide.netfix.data.onNavigateToTab?.invoke(2) },
                        hazeState = null
                    )
                }
                }
            }
            com.rupleide.netfix.ui.components.NfBottomSheet(
                visible = showYoutubeCheck,
                onDismiss = { showYoutubeCheck = false },
                icon = androidx.compose.material.icons.Icons.Default.Search,
                title = "Проверить работу",
                subtitle = "",
                closeLabel = "Закрыть"
            ) { entry ->
                Text(
                    text = "Вы прошли полное сканирование, и по его результатам YouTube у вас должен работать. Хотите сначала проверить его? NetFix включит обход и откроет YouTube. Когда вернётесь, ответьте, заработал ли он. Если нет, NetFix сам скачает SmartTube.",
                    color = Nf.TextSub,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = entry(0)
                )
                com.rupleide.netfix.ui.components.NfButton(
                    text = "Проверить на YouTube",
                    modifier = entry(1),
                    onClick = { startYoutubeCheck() }
                )
                com.rupleide.netfix.ui.components.NfButton(
                    text = if (smartTubeInstalled) "Открыть SmartTube" else "Всё равно скачать SmartTube",
                    style = com.rupleide.netfix.ui.components.NfButtonStyle.Secondary,
                    modifier = entry(2),
                    onClick = {
                        showYoutubeCheck = false
                        if (smartTubeInstalled) {
                            com.rupleide.netfix.core.update.UpdateManager.openSmartTube(context)
                        } else {
                            installSmartTube()
                        }
                    }
                )
            }
            com.rupleide.netfix.ui.components.NfBottomSheet(
                visible = showYoutubeVerdict,
                onDismiss = {
                    prefs.edit().putBoolean("yt_check_pending", false).apply()
                    showYoutubeVerdict = false
                },
                icon = androidx.compose.material.icons.Icons.Default.Search,
                title = "Заработал ли YouTube?",
                subtitle = "",
                closeLabel = "Решить позже"
            ) { entry ->
                Text(
                    text = "Вы вернулись из YouTube. Видео открываются и воспроизводятся без зависаний? Если да, плитка YouTube останется у вас навсегда.",
                    color = Nf.TextSub,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = entry(0)
                )
                com.rupleide.netfix.ui.components.NfButton(
                    text = "Да, работает",
                    modifier = entry(1),
                    onClick = {
                        prefs.edit()
                            .putString("youtube_mode", "official")
                            .putBoolean("yt_check_done", true)
                            .putBoolean("yt_check_failed", false)
                            .putBoolean("yt_check_pending", false)
                            .apply()
                        showYoutubeVerdict = false
                        ytTick++
                    }
                )
                com.rupleide.netfix.ui.components.NfButton(
                    text = "Нет, не работает",
                    style = com.rupleide.netfix.ui.components.NfButtonStyle.Secondary,
                    modifier = entry(2),
                    onClick = {
                        prefs.edit()
                            .putBoolean("yt_check_failed", true)
                            .putBoolean("yt_check_pending", false)
                            .apply()
                        showYoutubeVerdict = false
                        ytTick++
                        ytFailedReason = "Официальный YouTube на этом ТВ не заработал."
                        if (!smartTubeInstalled) {
                            scope.launch {
                                delay(450)
                                showYoutubeFailed = true
                            }
                        }
                    }
                )
            }
            com.rupleide.netfix.ui.components.NfBottomSheet(
                visible = showYoutubeFailed,
                onDismiss = { showYoutubeFailed = false },
                icon = androidx.compose.material.icons.Icons.Default.Search,
                title = "Установим SmartTube",
                subtitle = "",
                closeLabel = "Закрыть"
            ) { entry ->
                Text(
                    text = ytFailedReason + " NetFix скачает SmartTube сам, а проверить работу можно уже в нём.",
                    color = Nf.TextSub,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = entry(0)
                )
                com.rupleide.netfix.ui.components.NfButton(
                    text = "Скачать SmartTube",
                    modifier = entry(1),
                    onClick = {
                        showYoutubeFailed = false
                        installSmartTube()
                    }
                )
            }
        } else if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = navOverlayReserve),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    com.rupleide.netfix.ui.main.LatencyStrategyCard(
                        status = globeStatus,
                        latencyMs = latencyMs,
                        strategyName = StrategyTestManager.nameForNetwork(context, networkKind),
                        networkName = networkKind?.shortName,
                        onStrategyClick = { com.rupleide.netfix.data.onNavigateToTab?.invoke(2) },
                        hazeState = null
                    )

                    val tgButtonEnabled = isRunning
                    val ytButtonEnabled = currentStatus == AppStatus.Running

                    val tgInteractionSource = remember { MutableInteractionSource() }
                    val tgPressed by tgInteractionSource.collectIsPressedAsState()
                    val tgHovered by tgInteractionSource.collectIsHoveredAsState()
                    val tgFocused by tgInteractionSource.collectIsFocusedAsState()
                    val tgHighlighted = tgPressed || tgHovered || tgFocused

                    val tgBgColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color(0xFF383838) else Color(0xFF242424)
                        } else {
                            if (tgHighlighted) Color(0xFF202022) else Color(0xFF161618)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "tgBg"
                    )
                    val tgBorderColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color(0xFFFFFFFF) else Color(0x1AFFFFFF)
                        } else {
                            if (tgHighlighted) Color(0x33FFFFFF) else Color(0x0DFFFFFF)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "tgBorder"
                    )
                    val tgContentColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color.White else Color(0xFFF4F4F5)
                        } else {
                            if (tgHighlighted) Color(0xFFA1A1AA) else Color(0xFF555555)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "tgContent"
                    )

                    val ytInteractionSource = remember { MutableInteractionSource() }
                    val ytPressed by ytInteractionSource.collectIsPressedAsState()
                    val ytHovered by ytInteractionSource.collectIsHoveredAsState()
                    val ytFocused by ytInteractionSource.collectIsFocusedAsState()
                    val ytHighlighted = ytPressed || ytHovered || ytFocused

                    val ytBgColor by animateColorAsState(
                        targetValue = if (ytButtonEnabled) {
                            if (ytHighlighted) Color(0xFF383838) else Color(0xFF242424)
                        } else {
                            if (ytHighlighted) Color(0xFF202022) else Color(0xFF161618)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "ytBg"
                    )
                    val ytBorderColor by animateColorAsState(
                        targetValue = if (ytButtonEnabled) {
                            if (ytHighlighted) Color(0xFFFFFFFF) else Color(0x1AFFFFFF)
                        } else {
                            if (ytHighlighted) Color(0x33FFFFFF) else Color(0x0DFFFFFF)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "ytBorder"
                    )
                    val ytContentColor by animateColorAsState(
                        targetValue = if (ytButtonEnabled) {
                            if (ytHighlighted) Color.White else Color(0xFFF4F4F5)
                        } else {
                            if (ytHighlighted) Color(0xFFA1A1AA) else Color(0xFF555555)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "ytContent"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showTelegramTile) {
                            Box(
                                modifier = Modifier
                                    .then(if (showYoutubeTile) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .netFixGlassQuickCard(glassHazeState, tgHighlighted)
                                    .focusProperties { down = navBarFocusRequester }
                                    .clickable(
                                        enabled = tgButtonEnabled,
                                        interactionSource = tgInteractionSource,
                                        indication = ripple()
                                    ) {
                                        val port = TgProxyController.getPort(context)
                                        val secret = TgProxyController.getOrGenerateSecret(context)
                                        val url = TgProxyController.getTgProxyUrl(
                                            TgProxyController.DEFAULT_BIND_IP,
                                            port,
                                            secret
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                            val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                                            prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_telegram),
                                        contentDescription = null,
                                        tint = tgContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Telegram",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tgContentColor
                                    )
                                }
                            }
                        }

                        if (showYoutubeTile) {
                            Box(
                                modifier = Modifier
                                    .then(if (showTelegramTile) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .netFixGlassQuickCard(glassHazeState, ytHighlighted)
                                    .focusProperties { down = navBarFocusRequester }
                                    .clickable(
                                        enabled = ytButtonEnabled && !isDownloadingSmartTube,
                                        interactionSource = ytInteractionSource,
                                        indication = ripple()
                                    ) {
                                        val isSmartTv = prefs.getBoolean("is_smart_tv", false)
                                        val youtubeMode = prefs.getString("youtube_mode", "official") ?: "official"
                                        val useSmartTube = youtubeMode == "smarttube"
                                        if (useSmartTube) {
                                            if (smartTubeInstalled) {
                                                com.rupleide.netfix.core.update.UpdateManager.openSmartTube(context)
                                            } else {
                                                scope.launch {
                                                    isDownloadingSmartTube = true
                                                    val url = com.rupleide.netfix.core.update.UpdateManager.getSmartTubeLatestUrl()
                                                    com.rupleide.netfix.core.update.UpdateManager.downloadAndInstallApk(
                                                        context = context,
                                                        downloadUrl = url,
                                                        fileName = "smarttube.apk",
                                                        onProgress = { progress ->
                                                            smartTubeDownloadProgress = progress
                                                        },
                                                        onError = {
                                                            isDownloadingSmartTube = false
                                                        }
                                                    )
                                                    isDownloadingSmartTube = false
                                                }
                                            }
                                        } else {
                                            val pm = context.packageManager
                                            val tvIntent = if (isSmartTv) pm.launchIntentWithLeanback("com.google.android.youtube.tv") else null
                                            val intent = tvIntent
                                                ?: pm.launchIntentWithLeanback("com.google.android.youtube")
                                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_youtube),
                                        contentDescription = null,
                                        tint = ytContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isDownloadingSmartTube) {
                                            "Скачивание: ${(smartTubeDownloadProgress * 100).toInt()}%"
                                        } else {
                                            val youtubeMode = prefs.getString("youtube_mode", "official") ?: "official"
                                            if (youtubeMode == "smarttube") {
                                                if (smartTubeInstalled) "SmartTube" else "Установить SmartTube"
                                            } else {
                                                "YouTube"
                                            }
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ytContentColor
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = navOverlayReserve)
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInRoot()
                            val rootSize = coordinates.findRootCoordinates().size
                            if (rootSize.width > 0 && rootSize.height > 0) {
                                landscapeOrbCenterXFraction =
                                    ((position.x + coordinates.size.width / 2f) / rootSize.width).coerceIn(0.4f, 0.95f)
                                landscapeOrbCenterYFraction =
                                    ((position.y + coordinates.size.height / 2f) / rootSize.height).coerceIn(0.2f, 0.8f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(buttonSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!performanceMode) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { }
                                    .drawBehind {
                                        drawOrbAura(auraClock.floatValue, auraBaseColor, auraAlpha)
                                    }
                            )
                        }
                        com.rupleide.netfix.ui.main.NetFixPowerOrb(
                            status = globeStatus,
                            onClick = onOrbClick,
                            onLongClick = onOrbLongClick,
                            focusRequester = focusRequester,
                            navBarFocusRequester = navBarFocusRequester,
                            orbSize = buttonSize,
                    telegramOnly = telegramOnly
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp)
                    .graphicsLayer {
                        alpha = headerAlpha
                        translationY = (1f - headerAlpha) * 22.dp.toPx()
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val inlineContent = remember {
                            mapOf(
                                "bolt_icon" to InlineTextContent(
                                    Placeholder(
                                        width = 24.sp,
                                        height = 24.sp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bolt),
                                        contentDescription = null,
                                        tint = Color(0xFFF4F4F5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        }
                        val annotatedText = remember {
                            buildAnnotatedString {
                                appendInlineContent("bolt_icon", "[bolt]")
                                append("  Починить Telegram и YouTube")
                            }
                        }
                        Text(
                            text = "Починить Telegram и YouTube",
                            fontFamily = com.rupleide.netfix.ui.theme.NetFixFonts.Unbounded,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(shadow = headerTextShadow)
                        )
                        Text(
                            text = "Одна кнопка настроит и запустит всё необходимое",
                            fontFamily = com.rupleide.netfix.ui.theme.NetFixFonts.Manrope,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(shadow = headerTextShadow)
                        )

                    }
                }

            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.BiasAlignment(horizontalBias = 0f, verticalBias = -0.25f))
                    .onGloballyPositioned { coordinates ->
                        if (screenHeightPx > 0f) {
                            val orbCenterY = coordinates.positionInParent().y + coordinates.size.height / 2f
                            orbCenterYFraction = (orbCenterY / screenHeightPx).coerceIn(0.2f, 0.8f)
                        }
                    }
                    .graphicsLayer {
                        alpha = buttonAlpha
                        scaleX = buttonScale
                        scaleY = buttonScale
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!performanceMode) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { }
                            .drawBehind {
                                drawOrbAura(auraClock.floatValue, auraBaseColor, auraAlpha)
                            }
                    )
                }
                com.rupleide.netfix.ui.main.NetFixPowerOrb(
                    status = globeStatus,
                    onClick = onOrbClick,
                    onLongClick = onOrbLongClick,
                    focusRequester = focusRequester,
                    navBarFocusRequester = navBarFocusRequester,
                    orbSize = buttonSize,
                    telegramOnly = telegramOnly
                )
            }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = navOverlayReserve + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                com.rupleide.netfix.ui.main.LatencyStrategyCard(
                    status = globeStatus,
                    latencyMs = latencyMs,
                    strategyName = StrategyTestManager.nameForNetwork(context, networkKind),
                        networkName = networkKind?.shortName,
                    onStrategyClick = { com.rupleide.netfix.data.onNavigateToTab?.invoke(2) },
                    hazeState = glassHazeState,
                    modifier = Modifier.graphicsLayer {
                        alpha = latencyCardEntrance
                        val entranceScale = 0.95f + 0.05f * latencyCardEntrance
                        scaleX = entranceScale
                        scaleY = entranceScale
                    }
                )

                    val tgButtonEnabled = isRunning
                    val ytButtonEnabled = currentStatus == AppStatus.Running

                    val tgInteractionSource = remember { MutableInteractionSource() }
                    val tgPressed by tgInteractionSource.collectIsPressedAsState()
                    val tgHovered by tgInteractionSource.collectIsHoveredAsState()
                    val tgFocused by tgInteractionSource.collectIsFocusedAsState()
                    val tgHighlighted = tgPressed || tgHovered || tgFocused

                    val tgBgColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color(0xFF383838) else Color(0xFF242424)
                        } else {
                            if (tgHighlighted) Color(0xFF202022) else Color(0xFF161618)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "tgBg"
                    )
                    val tgBorderColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color(0xFFFFFFFF) else Color(0x1AFFFFFF)
                        } else {
                            if (tgHighlighted) Color(0x33FFFFFF) else Color(0x0DFFFFFF)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "tgBorder"
                    )
                    val tgContentColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color.White else Color(0xFFF4F4F5)
                        } else {
                            if (tgHighlighted) Color(0xFFA1A1AA) else Color(0xFF555555)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "tgContent"
                    )

                    val ytInteractionSource = remember { MutableInteractionSource() }
                    val ytPressed by ytInteractionSource.collectIsPressedAsState()
                    val ytHovered by ytInteractionSource.collectIsHoveredAsState()
                    val ytFocused by ytInteractionSource.collectIsFocusedAsState()
                    val ytHighlighted = ytPressed || ytHovered || ytFocused

                    val ytBgColor by animateColorAsState(
                        targetValue = if (ytButtonEnabled) {
                            if (ytHighlighted) Color(0xFF383838) else Color(0xFF242424)
                        } else {
                            if (ytHighlighted) Color(0xFF202022) else Color(0xFF161618)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "ytBg"
                    )
                    val ytBorderColor by animateColorAsState(
                        targetValue = if (ytButtonEnabled) {
                            if (ytHighlighted) Color(0xFFFFFFFF) else Color(0x1AFFFFFF)
                        } else {
                            if (ytHighlighted) Color(0x33FFFFFF) else Color(0x0DFFFFFF)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "ytBorder"
                    )
                    val ytContentColor by animateColorAsState(
                        targetValue = if (ytButtonEnabled) {
                            if (ytHighlighted) Color.White else Color(0xFFF4F4F5)
                        } else {
                            if (ytHighlighted) Color(0xFFA1A1AA) else Color(0xFF555555)
                        },
                        animationSpec = tween(if (performanceMode) 0 else 150),
                        label = "ytContent"
                    )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = quickCardsEntrance
                            val entranceScale = 0.95f + 0.05f * quickCardsEntrance
                            scaleX = entranceScale
                            scaleY = entranceScale
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        if (showTelegramTile) {
                            Box(
                                modifier = Modifier
                                    .then(if (showYoutubeTile) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .netFixGlassQuickCard(glassHazeState, tgHighlighted)
                                    .focusProperties { down = navBarFocusRequester }
                                    .clickable(
                                        enabled = tgButtonEnabled,
                                        interactionSource = tgInteractionSource,
                                        indication = ripple()
                                    ) {
                                        val port = TgProxyController.getPort(context)
                                        val secret = TgProxyController.getOrGenerateSecret(context)
                                        val url = TgProxyController.getTgProxyUrl(
                                            TgProxyController.DEFAULT_BIND_IP,
                                            port,
                                            secret
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                            val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                                            prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_telegram),
                                        contentDescription = null,
                                        tint = tgContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Telegram",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tgContentColor
                                    )
                                }
                            }
                        }

                        if (showYoutubeTile) {
                            Box(
                                modifier = Modifier
                                    .then(if (showTelegramTile) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .netFixGlassQuickCard(glassHazeState, ytHighlighted)
                                    .focusProperties { down = navBarFocusRequester }
                                    .clickable(
                                        enabled = ytButtonEnabled && !isDownloadingSmartTube,
                                        interactionSource = ytInteractionSource,
                                        indication = ripple()
                                    ) {
                                        val isSmartTv = prefs.getBoolean("is_smart_tv", false)
                                        val youtubeMode = prefs.getString("youtube_mode", "official") ?: "official"
                                        val useSmartTube = youtubeMode == "smarttube"
                                        if (useSmartTube) {
                                            if (smartTubeInstalled) {
                                                com.rupleide.netfix.core.update.UpdateManager.openSmartTube(context)
                                            } else {
                                                scope.launch {
                                                    isDownloadingSmartTube = true
                                                    val url = com.rupleide.netfix.core.update.UpdateManager.getSmartTubeLatestUrl()
                                                    com.rupleide.netfix.core.update.UpdateManager.downloadAndInstallApk(
                                                        context = context,
                                                        downloadUrl = url,
                                                        fileName = "smarttube.apk",
                                                        onProgress = { progress ->
                                                            smartTubeDownloadProgress = progress
                                                        },
                                                        onError = {
                                                            isDownloadingSmartTube = false
                                                        }
                                                    )
                                                    isDownloadingSmartTube = false
                                                }
                                            }
                                        } else {
                                            val pm = context.packageManager
                                            val tvIntent = if (isSmartTv) pm.launchIntentWithLeanback("com.google.android.youtube.tv") else null
                                            val intent = tvIntent
                                                ?: pm.launchIntentWithLeanback("com.google.android.youtube")
                                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_youtube),
                                        contentDescription = null,
                                        tint = ytContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isDownloadingSmartTube) {
                                            "Скачивание: ${(smartTubeDownloadProgress * 100).toInt()}%"
                                        } else {
                                            val youtubeMode = prefs.getString("youtube_mode", "official") ?: "official"
                                            if (youtubeMode == "smarttube") {
                                                if (smartTubeInstalled) "SmartTube" else "Установить SmartTube"
                                            } else {
                                                "YouTube"
                                            }
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ytContentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        NetFixUpdateSheet(
        visible = showUpdateDialog,
        onDismissRequest = {
            if (!isDownloadingUpdate) {
                showUpdateDialog = false
                com.rupleide.netfix.data.updateSheetDismissedThisSession = true
                com.rupleide.netfix.data.forceUpdateTestGlobal = false
            }
        },
        versionText = if (updateInfo != null) (if (updateInfo.version.startsWith("v", ignoreCase = true)) updateInfo.version else "v${updateInfo.version}") else "",
        descriptionText = updateInfo?.description ?: "",
        isDownloading = isDownloadingUpdate,
        downloadProgress = updateDownloadProgress,
        errorMessage = updateErrorMessage,
        onUpdate = {
            scope.launch {
                isDownloadingUpdate = true
                updateErrorMessage = null
                com.rupleide.netfix.core.update.UpdateManager.downloadAndInstallApk(
                    context = context,
                    downloadUrl = updateInfo!!.downloadUrl,
                    fileName = "update.apk",
                    onProgress = { progress ->
                        updateDownloadProgress = progress
                    },
                    onError = { err ->
                        isDownloadingUpdate = false
                        updateErrorMessage = err
                    }
                )
                isDownloadingUpdate = false
                showUpdateDialog = false
                com.rupleide.netfix.data.forceUpdateTestGlobal = false
            }
        },
        onLater = {
            showUpdateDialog = false
            com.rupleide.netfix.data.updateSheetDismissedThisSession = true
            com.rupleide.netfix.data.forceUpdateTestGlobal = false
        }
    )

    }

    val unloadWantsYt = prefs.getBoolean("wants_youtube_bypass", true)
    val unloadWantsTg = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
    val unloadCurrentMode = when {
        unloadWantsYt && unloadWantsTg -> "both"
        unloadWantsTg -> "telegram"
        unloadWantsYt -> "youtube"
        else -> "both"
    }

    NetFixUnloadSheet(
        visible = showUnloadDialog,
        onDismissRequest = { showUnloadDialog = false },
        selectedKey = unloadCurrentMode,
        options = listOf(
            UnloadOption(key = "both", label = "Telegram и YouTube", subtitle = "Обход для мессенджера и стриминга"),
            UnloadOption(key = "telegram", label = "Только Telegram", subtitle = "Только MTProto-прокси, без VPN"),
            UnloadOption(key = "youtube", label = "Только YouTube", subtitle = "Только VPN-тоннель, без прокси")
        ),
        onOptionSelected = { key ->
            showUnloadDialog = false
            if (!prefs.contains("ui_show_youtube")) {
                prefs.edit()
                    .putBoolean("ui_show_youtube", unloadWantsYt)
                    .putBoolean("ui_show_telegram", unloadWantsTg)
                    .apply()
            }
            val edit = prefs.edit()
            when (key) {
                "both" -> {
                    edit.putBoolean("wants_youtube_bypass", true)
                    edit.putBoolean("telegram_proxy_enabled_by_user", true)
                }
                "telegram" -> {
                    edit.putBoolean("wants_youtube_bypass", false)
                    edit.putBoolean("telegram_proxy_enabled_by_user", true)
                }
                "youtube" -> {
                    edit.putBoolean("wants_youtube_bypass", true)
                    edit.putBoolean("telegram_proxy_enabled_by_user", false)
                }
            }
            edit.putBoolean("service_enabled", true)
            edit.putBoolean("econom_mode", true)
            edit.apply()

            ServiceManager.stop(context)
            val stopIntent = Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                action = com.rupleide.netfix.data.STOP_ACTION
            }
            context.startService(stopIntent)
            TgProxyController.stop()
            com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(context)

            scope.launch {
                kotlinx.coroutines.delay(800)
                val finalWantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
                val finalWantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
                if (finalWantsYoutube) {
                    ServiceManager.start(context, Mode.VPN)
                }
                if (finalWantsTelegram) {
                    val openTg = prefs.getBoolean("open_tg_on_connect", true)
                    if (finalWantsYoutube) {
                        TgProxyController.startAsync(
                            context = context,
                            onSuccess = {
                                val port = TgProxyController.getPort(context)
                                val secret = TgProxyController.getOrGenerateSecret(context)
                                val url = TgProxyController.getTgProxyUrl(
                                    TgProxyController.DEFAULT_BIND_IP, port, secret
                                )
                                val alreadyConfigured = prefs.getBoolean("tg_proxy_configured", false)
                                if (openTg && !alreadyConfigured) {
                                    val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(tgIntent)
                                        prefs.edit().putBoolean("tg_proxy_configured", true).apply()
                                    } catch (_: Exception) {}
                                }
                            },
                            onError = {}
                        )
                    } else {
                        val startIntent = Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                            action = com.rupleide.netfix.data.START_ACTION
                            putExtra("open_tg", openTg)
                        }
                        androidx.core.content.ContextCompat.startForegroundService(context, startIntent)
                    }
                }
                val activity = context as? android.app.Activity
                activity?.finishAndRemoveTask()
            }
        }
    )

    com.rupleide.netfix.ui.components.NfBottomSheet(
        visible = showHintDialog,
        onDismiss = {
            showHintDialog = false
            handleNormalStart()
        },
        icon = androidx.compose.material.icons.Icons.Default.Info,
        title = "Запуск в фоне",
        subtitle = "Обход без открытого приложения",
        closeLabel = "Понятно"
    ) { entry ->
        Text(
            text = "Зажмите главную кнопку, чтобы запустить обход и сразу закрыть приложение. Так оно не занимает оперативную память, а обход продолжает работать.",
            color = com.rupleide.netfix.ui.components.Nf.TextSub,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = entry(0).fillMaxWidth()
        )
    }
}

@Composable
fun StatItem(icon: String, title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 14.sp, color = Color(0xFFA1A1AA))
            Text(text = title, fontSize = 12.sp, color = Color(0xFFA1A1AA))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF4F4F5)
        )
    }
}

private fun Modifier.netFixGlassQuickCard(
    hazeState: dev.chrisbanes.haze.HazeState?,
    isHighlighted: Boolean
): Modifier = this
    .netFixGlass(hazeState)
    .background(if (isHighlighted) Color.White.copy(alpha = 0.06f) else Color.Transparent)
    .border(
        width = 1.dp,
        color = if (isHighlighted) Color.White.copy(alpha = 0.5f) else Color.Transparent,
        shape = com.rupleide.netfix.ui.main.GlassCornerShape
    )


@Composable
private fun TvServiceCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(96.dp)
            .nfTvFocus(corner = 20.dp)
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                if (active) onClick()
            }
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = if (active) 0.09f else 0.05f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 36.dp)
                .size(150.dp)
                .rotate(-14f)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(title, color = if (active) Nf.Text else Nf.TextSub, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
        Text(
            subtitle,
            textAlign = TextAlign.Center,
            color = if (active) Nf.GreenText else Nf.Muted,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
        }
}
