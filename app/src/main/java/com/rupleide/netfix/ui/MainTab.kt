package com.rupleide.netfix.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MainTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE) }
    var telegramProxyEnabledByUser by remember { mutableStateOf(prefs.getBoolean("telegram_proxy_enabled_by_user", true)) }
    var wantsYoutubeBypass by remember { mutableStateOf(prefs.getBoolean("wants_youtube_bypass", true)) }

    androidx.compose.runtime.DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
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
    val updateInfo = com.rupleide.netfix.data.updateInfoGlobal
    val autoUpdateEnabled = com.rupleide.netfix.core.update.UpdateManager.isAutoUpdateEnabled(context)
    var showUpdateDialog by remember(updateInfo) { mutableStateOf(updateInfo != null && autoUpdateEnabled) }
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

    val lifecycleOwnerForSmartTube = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwnerForSmartTube) {
        lifecycleOwnerForSmartTube.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            smartTubeInstalled = com.rupleide.netfix.core.update.UpdateManager.isSmartTubeInstalled(context)
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 86.dp
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxButtonSize = if (isLandscape) 200.dp else 280.dp

    LaunchedEffect(Unit) {
        repeat(8) { index ->
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            delay(100L + index * 50L)
        }
    }

    var currentStatus by remember { mutableStateOf(appStatus.first) }
    var isTgProxyRunning by remember { mutableStateOf(com.rupleide.netfix.data.isTgProxyRunningGlobal) }
    var isStarting by remember { mutableStateOf(false) }

    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(currentStatus) {
        while (true) {
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
            delay(1000)
        }
    }

    LaunchedEffect(isStarting) {
        if (isStarting) {
            delay(12_000)
            isStarting = false
        }
    }

    val isRunning = when {
        wantsYoutubeBypass && telegramProxyEnabledByUser -> currentStatus == AppStatus.Running && isTgProxyRunning
        wantsYoutubeBypass -> currentStatus == AppStatus.Running
        telegramProxyEnabledByUser -> isTgProxyRunning
        else -> false
    }

    val isPending = when {
        wantsYoutubeBypass && telegramProxyEnabledByUser -> isStarting || (currentStatus == AppStatus.Running && !isTgProxyRunning)
        wantsYoutubeBypass -> isStarting && currentStatus != AppStatus.Running
        telegramProxyEnabledByUser -> isStarting && !isTgProxyRunning
        else -> false
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            if (com.rupleide.netfix.data.connectionStartTime == 0L) {
                com.rupleide.netfix.data.connectionStartTime = System.currentTimeMillis()
            }
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - com.rupleide.netfix.data.connectionStartTime) / 1000).toInt()
                delay(1000)
            }
        } else {
            com.rupleide.netfix.data.connectionStartTime = 0L
            elapsedSeconds = 0
        }
    }

    var latencyMs by remember { mutableStateOf(-1) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(currentStatus, performanceModeGlobal, lifecycleOwner) {
        if (performanceModeGlobal) {
            if (currentStatus == AppStatus.Running) {
                latencyMs = withContext(Dispatchers.IO) {
                    TgProxyController.measureLatency("1.1.1.1", 53)
                }
            } else {
                latencyMs = -1
            }
        } else {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    latencyMs = withContext(Dispatchers.IO) {
                        TgProxyController.measureLatency("1.1.1.1", 53)
                    }
                    delay(10000)
                }
            }
        }
    }



    fun startAll() {
        isStarting = true
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("service_enabled", true)
            .apply()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, com.rupleide.netfix.service.NetFixTileService::class.java)
            )
        } catch (_: Exception) {}
        
        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)

        if (wantsYoutube) {
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
                                android.util.Log.e("MainTab", "Failed to open Telegram link", e)
                            }
                        }
                    },
                    onError = {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isStarting = false
                        }
                        android.util.Log.e("MainTab", "Failed to start TG proxy")
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
        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        if (wantsYoutube) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                vpnLauncher.launch(intent)
            } else {
                startAll()
            }
        } else {
            startAll()
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val auraBaseColor = when {
        isRunning -> Color(0xFF22C55E)
        isPending -> Color(0xFF3B82F6)
        else -> Color(0xFF3B82F6)
    }
    val auraAlpha = when {
        isRunning -> 0.28f
        isPending -> 0.22f
        else -> 0.13f
    }

    val performanceMode = performanceModeGlobal

    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val auraAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (performanceMode) Int.MAX_VALUE else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraAngle"
    )
    val auraSecond by infiniteTransition.animateFloat(
        initialValue = (2f * Math.PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (performanceMode) Int.MAX_VALUE else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraSecond"
    )
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = if (performanceMode) 0.65f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (performanceMode) Int.MAX_VALUE else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraPulse"
    )

    val noiseImage = ImageBitmap.imageResource(id = R.drawable.noise)
    val noiseBrush = remember(noiseImage) {
        ShaderBrush(ImageShader(noiseImage, TileMode.Repeated, TileMode.Repeated))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        val screenHeight = maxHeight
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

        if (!performanceMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(noiseBrush, alpha = 0.04f)
            )
        }

        if (isLandscape) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = annotatedText,
                            inlineContent = inlineContent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Одна кнопки - настроит и запустит всё необходимое",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center
                        )

                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(
                                width = 1.dp,
                                color = Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bolt),
                                contentDescription = null,
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Задержка сети",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF4F4F5)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = StrategyTestManager.getActiveStrategyName(context),
                                    fontSize = 12.sp,
                                    color = Color(0xFFA1A1AA)
                                )
                            }
                        }
                        Text(
                            text = if (latencyMs >= 0) "$latencyMs мс" else "недоступно",
                            color = if (latencyMs >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    val tgButtonEnabled = isRunning
                    val ytButtonEnabled = currentStatus == AppStatus.Running

                    val tgInteractionSource = remember { MutableInteractionSource() }
                    val tgPressed by tgInteractionSource.collectIsPressedAsState()
                    val tgHovered by tgInteractionSource.collectIsHoveredAsState()
                    val tgFocused by tgInteractionSource.collectIsFocusedAsState()
                    val tgHighlighted = tgPressed || tgHovered || tgFocused

                    val tgBgColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color(0xFF38383A) else Color(0xFF242426)
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
                            if (ytHighlighted) Color(0xFF38383A) else Color(0xFF242426)
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
                        if (telegramProxyEnabledByUser) {
                            Box(
                                modifier = Modifier
                                    .then(if (wantsYoutubeBypass) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tgBgColor)
                                    .border(
                                        width = 1.dp,
                                        color = tgBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
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

                        if (wantsYoutubeBypass) {
                            Box(
                                modifier = Modifier
                                    .then(if (telegramProxyEnabledByUser) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ytBgColor)
                                    .border(
                                        width = 1.dp,
                                        color = ytBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
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
                                            val tvIntent = if (isSmartTv) pm.getLaunchIntentForPackage("com.google.android.youtube.tv") else null
                                            val intent = tvIntent
                                                ?: pm.getLaunchIntentForPackage("com.google.android.youtube")
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
                        .padding(bottom = navOverlayReserve),
                    contentAlignment = Alignment.Center
                ) {
                    PowerButton(
                        buttonSize = buttonSize,
                        isRunning = isRunning,
                        isPending = isPending,
                        elapsedSeconds = elapsedSeconds,
                        performanceMode = performanceMode,
                        focusRequester = focusRequester,
                        navBarFocusRequester = navBarFocusRequester,
                        onClick = {
                            if (isRunning || isPending) {
                                stopAll()
                            } else {
                                val hintCount = hintPrefs.getInt("long_press_hint_count", 0)
                                if (hintCount < 2) {
                                    hintPrefs.edit().putInt("long_press_hint_count", hintCount + 1).apply()
                                    showHintDialog = true
                                } else {
                                    handleNormalStart()
                                }
                            }
                        },
                        onLongClick = { showUnloadDialog = true },
                        auraAngle = auraAngle,
                        auraSecond = auraSecond,
                        auraPulse = auraPulse,
                        auraBaseColor = auraBaseColor,
                        auraAlpha = auraAlpha,
                        noiseBrush = noiseBrush
                    )
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
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp),
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
                            text = annotatedText,
                            inlineContent = inlineContent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Одна кнопки - настроит и запустит всё необходимое",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center
                        )

                    }
                }

                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.BiasAlignment(horizontalBias = 0f, verticalBias = -0.25f))
                ) {
                    PowerButton(
                        buttonSize = buttonSize,
                        isRunning = isRunning,
                        isPending = isPending,
                        elapsedSeconds = elapsedSeconds,
                        performanceMode = performanceMode,
                        focusRequester = focusRequester,
                        navBarFocusRequester = navBarFocusRequester,
                        onClick = {
                            if (isRunning || isPending) {
                                stopAll()
                            } else {
                                val hintCount = hintPrefs.getInt("long_press_hint_count", 0)
                                if (hintCount < 2) {
                                    hintPrefs.edit().putInt("long_press_hint_count", hintCount + 1).apply()
                                    showHintDialog = true
                                } else {
                                    handleNormalStart()
                                }
                            }
                        },
                        onLongClick = { showUnloadDialog = true },
                        auraAngle = auraAngle,
                        auraSecond = auraSecond,
                        auraPulse = auraPulse,
                        auraBaseColor = auraBaseColor,
                        auraAlpha = auraAlpha,
                        noiseBrush = noiseBrush
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = navOverlayReserve + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(
                                width = 1.dp,
                                color = Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bolt),
                                contentDescription = null,
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Задержка сети",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF4F4F5)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = StrategyTestManager.getActiveStrategyName(context),
                                    fontSize = 12.sp,
                                    color = Color(0xFFA1A1AA)
                                )
                            }
                        }
                        Text(
                            text = if (latencyMs >= 0) "$latencyMs мс" else "недоступно",
                            color = if (latencyMs >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    val tgButtonEnabled = isRunning
                    val ytButtonEnabled = currentStatus == AppStatus.Running

                    val tgInteractionSource = remember { MutableInteractionSource() }
                    val tgPressed by tgInteractionSource.collectIsPressedAsState()
                    val tgHovered by tgInteractionSource.collectIsHoveredAsState()
                    val tgFocused by tgInteractionSource.collectIsFocusedAsState()
                    val tgHighlighted = tgPressed || tgHovered || tgFocused

                    val tgBgColor by animateColorAsState(
                        targetValue = if (tgButtonEnabled) {
                            if (tgHighlighted) Color(0xFF38383A) else Color(0xFF242426)
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
                            if (ytHighlighted) Color(0xFF38383A) else Color(0xFF242426)
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
                        if (telegramProxyEnabledByUser) {
                            Box(
                                modifier = Modifier
                                    .then(if (wantsYoutubeBypass) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tgBgColor)
                                    .border(
                                        width = 1.dp,
                                        color = tgBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
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

                        if (wantsYoutubeBypass) {
                            Box(
                                modifier = Modifier
                                    .then(if (telegramProxyEnabledByUser) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ytBgColor)
                                    .border(
                                        width = 1.dp,
                                        color = ytBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
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
                                            val tvIntent = if (isSmartTv) pm.getLaunchIntentForPackage("com.google.android.youtube.tv") else null
                                            val intent = tvIntent
                                                ?: pm.getLaunchIntentForPackage("com.google.android.youtube")
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
        if (showUpdateDialog && updateInfo != null) {
            val displayVersion = if (updateInfo.version.startsWith("v", ignoreCase = true)) updateInfo.version else "v${updateInfo.version}"
            
            val laterInteractionSource = remember { MutableInteractionSource() }
            val laterPressed by laterInteractionSource.collectIsPressedAsState()
            val laterHovered by laterInteractionSource.collectIsHoveredAsState()
            val laterFocused by laterInteractionSource.collectIsFocusedAsState()
            val laterHighlighted = laterPressed || laterHovered || laterFocused
            
            val laterBgColor by animateColorAsState(
                targetValue = if (laterHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                animationSpec = tween(150),
                label = "laterBg"
            )
            val laterBorderColor by animateColorAsState(
                targetValue = if (laterHighlighted) Color(0xFFFFFFFF) else Color(0x1AFFFFFF),
                animationSpec = tween(150),
                label = "laterBorder"
            )
            val laterTextColor by animateColorAsState(
                targetValue = if (laterHighlighted) Color.White else Color(0xFFA1A1AA),
                animationSpec = tween(150),
                label = "laterText"
            )

            val updateBtnInteractionSource = remember { MutableInteractionSource() }
            val updateBtnPressed by updateBtnInteractionSource.collectIsPressedAsState()
            val updateBtnHovered by updateBtnInteractionSource.collectIsHoveredAsState()
            val updateBtnFocused by updateBtnInteractionSource.collectIsFocusedAsState()
            val updateBtnHighlighted = updateBtnPressed || updateBtnHovered || updateBtnFocused
            
            val updateBgColor by animateColorAsState(
                targetValue = if (updateBtnHighlighted) Color(0xFF5A9EFC) else Color(0xFF3B82F6),
                animationSpec = tween(150),
                label = "updateBg"
            )
            val updateBorderColor by animateColorAsState(
                targetValue = if (updateBtnHighlighted) Color.White else Color(0x1AFFFFFF),
                animationSpec = tween(150),
                label = "updateBorder"
            )
            val updateTextColor by animateColorAsState(
                targetValue = if (updateBtnHighlighted) Color.White else Color(0xFFF4F4F5),
                animationSpec = tween(150),
                label = "updateText"
            )

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { 
                    if (!isDownloadingUpdate) {
                        showUpdateDialog = false 
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = if (isLandscape) 500.dp else 320.dp)
                        .fillMaxWidth()
                        .heightIn(max = if (isLandscape) 280.dp else 450.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(
                            width = 1.dp,
                            color = Color(0x1AFFFFFF),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(if (isLandscape) 16.dp else 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp)
                        ) {
                            if (!isLandscape) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bolt),
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Найдено обновление",
                                color = Color(0xFFF4F4F5),
                                fontSize = if (isLandscape) 18.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Версия $displayVersion",
                                color = Color(0xFFF4F4F5),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )

                            if (isDownloadingUpdate) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Загрузка обновления...",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { updateDownloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFF3B82F6),
                                        trackColor = Color(0xFF2A2A2A)
                                    )
                                    Text(
                                        text = "${(updateDownloadProgress * 100).toInt()}%",
                                        color = Color(0xFFF4F4F5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Доступна новая версия NetFix. Рекомендуем обновиться.",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    if (updateInfo.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF161616), RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF242424), RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Что нового:",
                                                color = Color(0xFFF4F4F5),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Text(
                                                text = updateInfo.description,
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                if (updateErrorMessage != null) {
                                    Text(
                                        text = "Ошибка: $updateErrorMessage",
                                        color = Color(0xFFEF4444),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (!isDownloadingUpdate) {
                            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(laterBgColor)
                                        .border(
                                            width = 1.dp,
                                            color = laterBorderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            interactionSource = laterInteractionSource,
                                            indication = ripple()
                                        ) {
                                            showUpdateDialog = false
                                        }
                                        .padding(vertical = if (isLandscape) 10.dp else 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Позже",
                                        color = laterTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(updateBgColor)
                                        .border(
                                            width = 1.dp,
                                            color = updateBorderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            interactionSource = updateBtnInteractionSource,
                                            indication = ripple()
                                        ) {
                                            scope.launch {
                                                isDownloadingUpdate = true
                                                updateErrorMessage = null
                                                com.rupleide.netfix.core.update.UpdateManager.downloadAndInstallApk(
                                                    context = context,
                                                    downloadUrl = updateInfo.downloadUrl,
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
                                            }
                                        }
                                        .padding(vertical = if (isLandscape) 10.dp else 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (updateErrorMessage != null) "Повторить" else "Обновить",
                                        color = updateTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnloadDialog) {
        Dialog(onDismissRequest = { showUnloadDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Запуск в фоне и выход",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Выберите режим. Приложение запустит службу и закроется.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                val wantsYt = prefs.getBoolean("wants_youtube_bypass", true)
                val wantsTg = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
                val currentMode = when {
                    wantsYt && wantsTg -> "both"
                    wantsTg -> "telegram"
                    wantsYt -> "youtube"
                    else -> "both"
                }

                val options = listOf(
                    "both" to "Telegram и YouTube",
                    "telegram" to "Только Telegram",
                    "youtube" to "Только YouTube"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (key, label) ->
                        val isSelected = currentMode == key
                        val rowInteraction = remember { MutableInteractionSource() }
                        val rowPressed by rowInteraction.collectIsPressedAsState()
                        val rowHovered by rowInteraction.collectIsHoveredAsState()
                        val rowFocused by rowInteraction.collectIsFocusedAsState()
                        val rowHighlighted = rowPressed || rowHovered || rowFocused
                        val rowBg by animateColorAsState(
                            targetValue = if (rowHighlighted) Color(0xFF38383A) else Color.Transparent,
                            animationSpec = tween(150), label = ""
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(rowBg)
                                .border(
                                    width = if (rowHighlighted) 1.dp else 0.dp,
                                    color = if (rowHighlighted) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(
                                    interactionSource = rowInteraction,
                                    indication = null
                                ) {
                                    showUnloadDialog = false
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
                                    com.rupleide.netfix.service.WatchdogReceiver.cancelWatchdogAlarm(context)

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
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF4F4F5),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_bolt),
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF242424))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val cancelInteraction = remember { MutableInteractionSource() }
                val cancelPressed by cancelInteraction.collectIsPressedAsState()
                val cancelHovered by cancelInteraction.collectIsHoveredAsState()
                val cancelFocused by cancelInteraction.collectIsFocusedAsState()
                val cancelHighlighted = cancelPressed || cancelHovered || cancelFocused
                val cancelBg by animateColorAsState(
                    targetValue = if (cancelHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                    animationSpec = tween(150), label = ""
                )
                val cancelBorder by animateColorAsState(
                    targetValue = if (cancelHighlighted) Color.White else Color(0x1AFFFFFF),
                    animationSpec = tween(150), label = ""
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cancelBg)
                        .border(1.dp, cancelBorder, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = cancelInteraction,
                            indication = null
                        ) { showUnloadDialog = false }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Отмена",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showHintDialog) {
        Dialog(onDismissRequest = { showHintDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Запуск в фоне",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Вы можете зажать главную кнопку (долгое нажатие), чтобы запустить обход в фоне и сразу закрыть приложение для экономии оперативной памяти.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bgInteraction = remember { MutableInteractionSource() }
                    val bgPressed by bgInteraction.collectIsPressedAsState()
                    val bgHovered by bgInteraction.collectIsHoveredAsState()
                    val bgFocused by bgInteraction.collectIsFocusedAsState()
                    val bgHighlighted = bgPressed || bgHovered || bgFocused
                    val bgBg by animateColorAsState(
                        targetValue = if (bgHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                        animationSpec = tween(150), label = ""
                    )
                    val bgBorder by animateColorAsState(
                        targetValue = if (bgHighlighted) Color.White else Color(0x1AFFFFFF),
                        animationSpec = tween(150), label = ""
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgBg)
                            .border(1.dp, bgBorder, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = bgInteraction,
                                indication = null
                            ) {
                                showHintDialog = false
                                showUnloadDialog = true
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Запустить в фоне",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    val runInteraction = remember { MutableInteractionSource() }
                    val runPressed by runInteraction.collectIsPressedAsState()
                    val runHovered by runInteraction.collectIsHoveredAsState()
                    val runFocused by runInteraction.collectIsFocusedAsState()
                    val runHighlighted = runPressed || runHovered || runFocused
                    val runBg by animateColorAsState(
                        targetValue = if (runHighlighted) Color(0xFF5A9EFC) else Color(0xFF3B82F6),
                        animationSpec = tween(150), label = ""
                    )
                    val runBorder by animateColorAsState(
                        targetValue = if (runHighlighted) Color.White else Color(0x1AFFFFFF),
                        animationSpec = tween(150), label = ""
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(runBg)
                            .border(1.dp, runBorder, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = runInteraction,
                                indication = null
                            ) {
                                showHintDialog = false
                                handleNormalStart()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Обычный запуск",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


fun formatTimer(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PowerButton(
    buttonSize: androidx.compose.ui.unit.Dp,
    isRunning: Boolean,
    isPending: Boolean,
    elapsedSeconds: Int,
    performanceMode: Boolean,
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    auraAngle: Float,
    auraSecond: Float,
    auraPulse: Float,
    auraBaseColor: Color,
    auraAlpha: Float,
    noiseBrush: ShaderBrush
) {
    val mainInteractionSource = remember { MutableInteractionSource() }
    val mainPressed by mainInteractionSource.collectIsPressedAsState()
    val mainHovered by mainInteractionSource.collectIsHoveredAsState()
    val mainFocused by mainInteractionSource.collectIsFocusedAsState()
    val mainHighlighted = mainPressed || mainHovered || mainFocused

    val buttonScale by animateFloatAsState(targetValue = if (!performanceMode && mainPressed) 0.94f else 1f, label = "")

    val statusColor = when {
        isRunning -> Color(0xFF22C55E)
        isPending -> Color(0xFF3B82F6)
        else -> Color(0xFF7C6AF7)
    }

    val borderHighlightColor by animateColorAsState(
        targetValue = if (mainHighlighted) Color.White else statusColor,
        animationSpec = tween(if (performanceMode) 0 else 150),
        label = "mainBorder"
    )

    val isAnimating = isRunning || isPending

    Box(
        modifier = Modifier
            .size(buttonSize)
            .then(
                if (!performanceMode) Modifier.drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val orbitR = size.minDimension * 0.2f
                    val c1x = cx + kotlin.math.cos(auraAngle) * orbitR
                    val c1y = cy + kotlin.math.sin(auraAngle) * orbitR
                    val c2x = cx + kotlin.math.cos(auraSecond) * orbitR * 0.7f
                    val c2y = cy + kotlin.math.sin(auraSecond) * orbitR * 0.7f
                    val r = size.minDimension * 1.3f * auraPulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraBaseColor.copy(alpha = auraAlpha),
                                Color.Transparent
                            ),
                            center = Offset(c1x, c1y),
                            radius = r
                        ),
                        center = Offset(c1x, c1y),
                        radius = r
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraBaseColor.copy(alpha = auraAlpha * 0.6f),
                                Color.Transparent
                            ),
                            center = Offset(c2x, c2y),
                            radius = r * 0.75f
                        ),
                        center = Offset(c2x, c2y),
                        radius = r * 0.75f
                    )
                } else Modifier
            )
            .graphicsLayer(
                scaleX = buttonScale,
                scaleY = buttonScale
            )
            .shadow(
                elevation = if (!performanceMode && isAnimating) 32.dp else if (!performanceMode) 8.dp else 0.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = statusColor,
                spotColor = statusColor
            )
            .focusRequester(focusRequester)
            .focusProperties { down = navBarFocusRequester }
            .border(width = 2.5.dp, color = borderHighlightColor, shape = CircleShape)
            .background(Color(0xFF0A0A18), shape = CircleShape)
            .combinedClickable(
                interactionSource = mainInteractionSource,
                indication = ripple(bounded = true, radius = buttonSize / 2),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val innerSize = buttonSize * 0.88f
        val iconSize = buttonSize * 0.31f
        Box(
            modifier = Modifier
                .size(innerSize)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E1E38), Color(0xFF0F0F1E))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_power),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.height(buttonSize * 0.03f))
                when {
                    isRunning -> {
                        Text(
                            text = "Подключено",
                            fontSize = if (buttonSize < 220.dp) 14.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(buttonSize * 0.015f))
                        Text(
                            text = formatTimer(elapsedSeconds),
                            fontSize = if (buttonSize < 220.dp) 12.sp else 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF22C55E),
                            textAlign = TextAlign.Center
                        )
                    }
                    isPending -> {
                        Text(
                            text = "Подключение...",
                            fontSize = if (buttonSize < 220.dp) 13.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(buttonSize * 0.015f))
                        Text(
                            text = formatTimer(elapsedSeconds),
                            fontSize = if (buttonSize < 220.dp) 12.sp else 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3B82F6),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Text(
                            text = "Починить",
                            fontSize = if (buttonSize < 220.dp) 16.sp else 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "интернет",
                            fontSize = if (buttonSize < 220.dp) 14.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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
