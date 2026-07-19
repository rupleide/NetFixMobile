package com.rupleide.netfix.ui

import com.rupleide.netfix.ui.components.NetFixTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.rupleide.netfix.ui.components.NetFixEditStrategySheet
import com.rupleide.netfix.ui.components.NetFixStrategySheet
import com.rupleide.netfix.ui.components.StrategyAction
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.appStatus

@Composable
fun YoutubeTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 86.dp

    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE) }
    val isSmartTv = remember(context) { sharedPrefs.getBoolean("is_smart_tv", false) }

    var currentTestIndex by remember { mutableStateOf(sharedPrefs.getInt("wizard_current_test_index", 0)) }
    var hasLaunchedTest by remember { mutableStateOf(sharedPrefs.getBoolean("wizard_has_launched_test", false)) }
    var isSetupComplete by remember {
        val complete = sharedPrefs.getBoolean("wizard_is_setup_complete", false)
        Log.e("NetFixDebug", "YoutubeTab init: isSetupComplete = $complete")
        mutableStateOf(complete)
    }
    var youtubeMode by remember { mutableStateOf(sharedPrefs.getString("youtube_mode", "smarttube") ?: "smarttube") }
    var testStarted by remember { mutableStateOf(sharedPrefs.getBoolean("wizard_test_started", false)) }
    var noStrategiesWorked by remember { mutableStateOf(sharedPrefs.getBoolean("wizard_no_strategies_worked", false)) }
    var showAllStrategiesList by remember { mutableStateOf(false) }
    var showActionMenuForStrategy by remember { mutableStateOf<Triple<Int, String, String>?>(null) }
    var editingStrategy by remember { mutableStateOf<String?>(null) }
    var deletingStrategy by remember { mutableStateOf<String?>(null) }
    var editNameText by remember { mutableStateOf("") }
    var editNotesText by remember { mutableStateOf("") }
    val isSmartTube = youtubeMode == "smarttube"

    var activeCmd by remember { mutableStateOf(sharedPrefs.getString("byedpi_cmd_args", null)) }
    var manualMode by remember { mutableStateOf(sharedPrefs.getBoolean("strategy_manual_mode", false)) }
    val advancedMode = true
    val smartTubeInstallFocusRequester = remember { FocusRequester() }
    val noStrategiesRetryFocusRequester = remember { FocusRequester() }
    val autoModeToggleFocusRequester = remember { FocusRequester() }
    val manualModeListFocusRequester = remember { FocusRequester() }
    val wizardPlayFocusRequester = remember { FocusRequester() }
    val manualOpenYoutubeFocusRequester = remember { FocusRequester() }

    fun saveManualMode(enabled: Boolean) {
        manualMode = enabled
        sharedPrefs.edit().putBoolean("strategy_manual_mode", enabled).apply()
        com.rupleide.netfix.core.debug.AppDebugManager.log("Режим YouTube: ${if (enabled) "Ручной" else "Автоматический"}")
    }

    fun getSortedStrategies(): List<Triple<Int, String, String>> {
        val currentCmd = activeCmd
        return StrategyTestManager.testResults.sortedWith { a, b ->
            val aStrategy = a.second
            val bStrategy = b.second
            val aNormalized = aStrategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
            val bNormalized = bStrategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
            val aPinned = StrategyTestManager.pinnedStrategies[aStrategy] ?: false
            val bPinned = StrategyTestManager.pinnedStrategies[bStrategy] ?: false
            val aCurrent = currentCmd != null && currentCmd.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == aNormalized
            val bCurrent = currentCmd != null && currentCmd.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == bNormalized
            
            val aWeight = (if (aPinned) 2 else 0) + (if (aCurrent) 1 else 0)
            val bWeight = (if (bPinned) 2 else 0) + (if (bCurrent) 1 else 0)
            
            bWeight.compareTo(aWeight)
        }
    }

    val presets = remember {
        listOf(
            "-o1 -a1 -At,r,s -d1 -a1",
            "-q2 -s2 -s3+s -r3 -s4 -r4 -s5+s -r5+s -s6 -s7+s -r8 -s9+s -Qr -Mh,d,r -a1 -At,r -s2+s -r2 -d2 -s3 -r3 -r4 -s4 -d5+s -r5 -d6 -s7+s -d7 -a1",
            "-f-200 -Qr -s3:5+sm -a1 -As -d1 -s4+sm -s8+sh -f-300 -d6+sh -a1 -At,r,s -o2 -f-30 -As -r5 -Mh -r6+sh -f-250 -s2:7+s -s3:6+sm -a1 -At,r,s -s3:5+sm -s6+s -s7:9+s -q30+sm -a1",
            "-d1 -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -r1+s -S -a1 -As -d1 -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -S -a1",
            "-n google.com -Qr -f-204 -s1:5+sm -a1 -As -d1 -s3+s -s5+s -q7 -a1 -As -o2 -f-43 -a1 -As -r5 -Mh -s1:5+s -s3:7+sm -a1"
        )
    }

    val smartTubeInstalled = remember { mutableStateOf(com.rupleide.netfix.core.update.UpdateManager.isSmartTubeInstalled(context)) }
    var isDownloadingSmartTube by remember { mutableStateOf(false) }
    var smartTubeDownloadProgress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            smartTubeInstalled.value = com.rupleide.netfix.core.update.UpdateManager.isSmartTubeInstalled(context)
        }
    }

    LaunchedEffect(currentTestIndex, hasLaunchedTest, isSetupComplete, smartTubeInstalled.value) {
        repeat(8) { index ->
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(100L + index * 50L)
        }
    }

    fun saveCurrentTestIndex(index: Int) {
        currentTestIndex = index
        sharedPrefs.edit().putInt("wizard_current_test_index", index).apply()
    }

    fun saveHasLaunchedTest(launched: Boolean) {
        hasLaunchedTest = launched
        sharedPrefs.edit().putBoolean("wizard_has_launched_test", launched).apply()
    }

    fun saveIsSetupComplete(complete: Boolean) {
        Log.e("NetFixDebug", "saveIsSetupComplete called with: $complete", Throwable())
        isSetupComplete = complete
        sharedPrefs.edit().putBoolean("wizard_is_setup_complete", complete).apply()
    }

    fun saveTestStarted(started: Boolean) {
        testStarted = started
        sharedPrefs.edit().putBoolean("wizard_test_started", started).apply()
    }

    fun saveNoStrategiesWorked(worked: Boolean) {
        noStrategiesWorked = worked
        sharedPrefs.edit().putBoolean("wizard_no_strategies_worked", worked).apply()
    }

    var wasTesting by remember { mutableStateOf(StrategyTestManager.isTesting) }
    LaunchedEffect(StrategyTestManager.isTesting) {
        if (wasTesting && !StrategyTestManager.isTesting) {
            if (testStarted && StrategyTestManager.currentTestIndex > 0) {
                if (StrategyTestManager.bestStrategyResult != null) {
                    saveIsSetupComplete(false)
                    saveTestStarted(true)
                    saveHasLaunchedTest(false)
                    saveCurrentTestIndex(0)
                    saveNoStrategiesWorked(false)
                } else {
                    saveIsSetupComplete(false)
                    saveNoStrategiesWorked(true)
                    saveTestStarted(false)
                }
            }
        }
        wasTesting = StrategyTestManager.isTesting
    }

    fun launchYoutubeApp() {
        val target = if (!isSmartTv) "YouTube" else if (isSmartTube) "SmartTube" else "YouTube TV"
        com.rupleide.netfix.core.debug.AppDebugManager.log("Запуск внешнего плеера: $target")
        if (!isSmartTv) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent != null) {
                context.startActivity(intent)
            } else {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                } catch (_: Exception) {
                    android.widget.Toast.makeText(context, "YouTube не найден на устройстве", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else if (isSmartTube) {
            com.rupleide.netfix.core.update.UpdateManager.openSmartTube(context)
        } else {
            val pm = context.packageManager
            var intent = pm.getLaunchIntentForPackage("com.google.android.youtube.tv")
            if (intent == null) {
                intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
            }
            if (intent != null) {
                context.startActivity(intent)
            } else {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                } catch (_: Exception) {
                    android.widget.Toast.makeText(context, "YouTube не найден на устройстве", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startAll() {
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
        if (appStatus.first == AppStatus.Running) {
            ServiceManager.restart(context, Mode.VPN)
        } else {
            ServiceManager.start(context, Mode.VPN)
        }
        com.rupleide.netfix.core.tgproxy.TgProxyController.startAsync(
            context = context,
            onSuccess = {},
            onError = {}
        )
    }

    fun stopAll() {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit()
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
        com.rupleide.netfix.core.tgproxy.TgProxyController.stop()
    }

    var showVpnDeniedDialog by remember { mutableStateOf(false) }
    var showTestingTypeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editingStrategy, deletingStrategy, showActionMenuForStrategy, showVpnDeniedDialog, showTestingTypeDialog, showAllStrategiesList) {
        com.rupleide.netfix.data.isActionSheetVisibleGlobal = (editingStrategy != null) || 
            (deletingStrategy != null) || 
            (showActionMenuForStrategy != null) || 
            showVpnDeniedDialog || 
            showTestingTypeDialog || 
            showAllStrategiesList
    }

    val vpnLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            showVpnDeniedDialog = false
            startAll()
            if (!isSetupComplete && testStarted) {
                launchYoutubeApp()
                saveHasLaunchedTest(true)
            } else {
                launchYoutubeApp()
            }
        } else {
            showVpnDeniedDialog = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.TopCenter
    ) {
        if (StrategyTestManager.isTesting) {
            val facts = remember {
                listOf(
                    "Первый реестр запрещённых сайтов создан в России 1 ноября 2012 года на основании закона № 139-ФЗ.",
                    "Закон о «суверенном Рунете» № 90-ФЗ вступил в силу 1 ноября 2019 года, обязав операторов установить ТСПУ.",
                    "ТСПУ (технические средства противодействия угрозам) - это оборудование DPI, управляемое напрямую Роскомнадзором.",
                    "Десктопный NetFix для Windows и это Android-приложение созданы одним соло-разработчиком Rubi (rupleide). Десктопная версия уже имеет более 300 звёзд на GitHub и десятки тысяч скачиваний.",
                    "Мобильный NetFix объединил в одном приложении два сложных нативных ядра на Rust и C. Главная цель проекта, убрать сложные настройки и консоли из рук пользователя.",
                    "Instagram и Facebook были официально внесены в реестр запрещённых сайтов и заблокированы в России в марте 2022 года.",
                    "С 2021 года Роскомнадзор активно блокирует VPN-протоколы (OpenVPN, WireGuard) по цифровым сигнатурам пакетов.",
                    "Закон «Лугового» разрешил Генеральной прокуратуре блокировать сайты во внесудебном порядке за экстремизм.",
                    "С 1 января 2022 года закон о «приземлении» обязал крупные IT-компании открывать филиалы в Российской Федерации.",
                    "В 2021-2022 годах суды РФ назначили Google оборотные штрафы на сумму более 20 млрд рублей за неудаление контента.",
                    "Замедление YouTube летом 2024 года фактически осуществляется через ТСПУ, анализирующие SNI сетевых пакетов.",
                    "Серверы Google Global Cache устанавливались у провайдеров РФ для ускорения загрузки видео из локаческой сети.",
                    "Технология DPI (Deep Packet Inspection) позволяет «вскрывать» пакеты и фильтровать HTTPS-трафик по доменному имени.",
                    "Почтовый сервис ProtonMail был заблокирован в РФ в 2020 году из-за рассылки ложных угроз о минировании.",
                    "Крупнейший русскоязычный торрент-трекер Rutracker.org был заблокирован «пожизненно» решением суда в 2016 году.",
                    "В августе 2024 года Павел Дуров был задержан в аэропорту Парижа по обвинениям французских властей, связанным с модерацией в Telegram.",
                    "Осенью 2024 года Telegram обновил правила, разрешив передавать властям IP-адреса и номера телефонов нарушителей при наличии ордера.",
                    "В 2024 году Telegram запустил внутреннюю валюту Telegram Stars для оплаты цифровых услуг и покупок в мини-приложениях.",
                    "В 2024-2025 годах бум кликеров (Notcoin, Hamster Kombat) привёл к рекордному росту ежемесячной аудитории Telegram до 950 млн человек.",
                    "Интеграция блокчейна TON и кошелька Wallet превратила Telegram в один из крупнейших мировых хабов для криптовалютных микротранзакций.",
                    "В 2025 году в Telegram появилась встроенная поддержка децентрализованных веб-страниц и глобальный каталог мини-приложений.",
                    "Мессенджер Telegram был официально запущен 14 августа 2013 года. Его визитной карточкой стал собственный протокол шифрования MTProto.",
                    "В 2025 году общее число платных подписчиков Premium-версии мессенджера Telegram превысило отметку в 10 миллионов пользователей.",
                    "С 2024 года авторы публичных каналов в Telegram могут получать до 50% дохода от показа рекламы с мгновенным выводом средств в TON.",
                    "Популярный геймерский мессенджер Discord был заблокирован Роскомнадзором на территории РФ в октябре 2024 года.",
                    "В 2025 году бизнес-аккаунты Telegram получили поддержку ИИ-ассистентов, способных автоматически обрабатывать клиентские запросы.",
                    "Функция поиска людей поблизости в Telegram была переработана из-за соображений приватности и борьбы с роботизированным спамом.",
                    "В разное время доступ к Telegram полностью или частично блокировали в Китае, Иране, Бразилии и ряде других стран.",
                    "В 2026 году Telegram активно развивает децентрализованные методы облачного хранения данных для повышения отказоустойчивости.",
                    "Защищенный мессенджер Signal, использующий сквозное шифрование, был заблокирован на территории РФ в август 2024 года."
                )
            }
            var currentFactIndex by remember { mutableStateOf(0) }

            LaunchedEffect(currentFactIndex) {
                kotlinx.coroutines.delay(11000)
                currentFactIndex = (currentFactIndex + 1) % facts.size
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = if (isLandscape) 600.dp else 400.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(0.7f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFF4F4F5),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Подбор конфигурации",
                                color = Color(0xFFF4F4F5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        val progress = if (StrategyTestManager.totalStrategiesCount > 0) {
                            StrategyTestManager.currentTestIndex.toFloat() / StrategyTestManager.totalStrategiesCount
                        } else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                    .background(Color(0xFF3B82F6))
                            )
                        }

                        val statusText = if (advancedMode) {
                            StrategyTestManager.currentProgress
                        } else {
                            "Подбираем лучший способ для вашей сети..."
                        }

                        Text(
                            text = statusText,
                            color = Color(0xFFA1A1AA),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x1AFFFFFF))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val prevInteractionSource = remember { MutableInteractionSource() }
                            val prevPressed by prevInteractionSource.collectIsPressedAsState()
                            val prevHovered by prevInteractionSource.collectIsHoveredAsState()
                            val prevFocused by prevInteractionSource.collectIsFocusedAsState()
                            val prevHighlighted = prevPressed || prevHovered || prevFocused
                            val prevColor by animateColorAsState(
                                targetValue = if (prevHighlighted) Color.White else Color(0x80FFFFFF),
                                animationSpec = tween(150),
                                label = "prevColor"
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (prevHighlighted) Color(0x1AFFFFFF) else Color.Transparent)
                                    .clickable(
                                        interactionSource = prevInteractionSource,
                                        indication = null
                                    ) {
                                        currentFactIndex = if (currentFactIndex > 0) currentFactIndex - 1 else facts.size - 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow),
                                    contentDescription = null,
                                    tint = prevColor,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer(rotationZ = -90f)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 155.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Text(
                                    text = "ИНТЕРЕСНЫЙ ФАКТ",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedContent(
                                    targetState = facts[currentFactIndex],
                                    transitionSpec = {
                                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                                    },
                                    label = "factAnimation"
                                ) { factText ->
                                    Text(
                                        text = factText,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                        textAlign = TextAlign.Center,
                                        minLines = 4
                                    )
                                }
                            }

                            val nextInteractionSource = remember { MutableInteractionSource() }
                            val nextPressed by nextInteractionSource.collectIsPressedAsState()
                            val nextHovered by nextInteractionSource.collectIsHoveredAsState()
                            val nextFocused by nextInteractionSource.collectIsFocusedAsState()
                            val nextHighlighted = nextPressed || nextHovered || nextFocused
                            val nextColor by animateColorAsState(
                                targetValue = if (nextHighlighted) Color.White else Color(0x80FFFFFF),
                                animationSpec = tween(150),
                                label = "nextColor"
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (nextHighlighted) Color(0x1AFFFFFF) else Color.Transparent)
                                    .clickable(
                                        interactionSource = nextInteractionSource,
                                        indication = null
                                    ) {
                                        currentFactIndex = (currentFactIndex + 1) % facts.size
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow),
                                    contentDescription = null,
                                    tint = nextColor,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer(rotationZ = 90f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                if (isSmartTv && isSmartTube && !smartTubeInstalled.value) {
                                    smartTubeInstallFocusRequester.requestFocus()
                                } else if (noStrategiesWorked) {
                                    noStrategiesRetryFocusRequester.requestFocus()
                                } else if (isSetupComplete) {
                                    if (manualMode) {
                                        manualModeListFocusRequester.requestFocus()
                                    } else {
                                        autoModeToggleFocusRequester.requestFocus()
                                    }
                                } else {
                                    wizardPlayFocusRequester.requestFocus()
                                }
                            }
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = if (isLandscape) 820.dp else 500.dp)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = navOverlayReserve + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            if (isSmartTv && isSmartTube && !smartTubeInstalled.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color(0xFFF4F4F5),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Установка SmartTube",
                            color = Color(0xFFF4F4F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Официальный YouTube сейчас полностью заблокирован в РФ и не работает. Для просмотра видео на телевизоре необходимо установить специальный ТВ-плеер SmartTube.",
                        color = Color(0xFFA1A1AA),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    val btnInteractionSource = remember { MutableInteractionSource() }
                    val btnPressed by btnInteractionSource.collectIsPressedAsState()
                    val btnHovered by btnInteractionSource.collectIsHoveredAsState()
                    val btnFocused by btnInteractionSource.collectIsFocusedAsState()
                    val btnHighlighted = btnPressed || btnHovered || btnFocused

                    val btnBgColor by animateColorAsState(
                        targetValue = if (btnHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                        animationSpec = tween(150),
                        label = ""
                    )
                    val btnBorderColor by animateColorAsState(
                        targetValue = if (btnHighlighted) Color.White else Color(0x1AFFFFFF),
                        animationSpec = tween(150),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(btnBgColor)
                            .border(1.dp, btnBorderColor, RoundedCornerShape(12.dp))
                            .focusRequester(smartTubeInstallFocusRequester)
                            .focusProperties { down = navBarFocusRequester }
                            .clickable(
                                interactionSource = btnInteractionSource,
                                indication = null,
                                enabled = !isDownloadingSmartTube
                            ) {
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
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDownloadingSmartTube) {
                                "Скачивание: ${(smartTubeDownloadProgress * 100).toInt()}%"
                            } else {
                                "Установить SmartTube"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            if (showAllStrategiesList) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = Color(0xFFF4F4F5),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Все стратегии (${StrategyTestManager.testResults.size})",
                                color = Color(0xFFF4F4F5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "Назад",
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                showAllStrategiesList = false
                            }
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (item in getSortedStrategies()) {
                            val (index, strategy, status) = item
                            val normalized = strategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
                            val manualStatus = sharedPrefs.getString("manual_status_$normalized", null)
                            val displayStatus = when (manualStatus) {
                                "working" -> "Работает"
                                "failed" -> "Не работает"
                                else -> status.replace(" (Успешно)", "")
                            }
                            val statusColor = when (manualStatus) {
                                "working" -> Color(0xFF22C55E)
                                "failed" -> Color(0xFFEF4444)
                                else -> if (status.contains("мс")) Color(0xFF22C55E) else Color(0xFFEF4444)
                            }
                            val isPinned = StrategyTestManager.pinnedStrategies[strategy] ?: false
                            val customName = StrategyTestManager.customNames[strategy]
                            val notes = StrategyTestManager.strategyNotes[strategy]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF242426))
                                    .border(1.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showActionMenuForStrategy = item
                                        }
                                ) {
                                    Text(
                                        text = StrategyTestManager.getStrategyName(strategy, context),
                                        color = Color(0xFFF4F4F5),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    if (advancedMode) {
                                        Text(
                                            text = strategy,
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        if (!notes.isNullOrBlank()) {
                                            Text(
                                                text = "Заметка: $notes",
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 11.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayStatus,
                                        color = statusColor,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clickable {
                                                showActionMenuForStrategy = item
                                            }
                                            .padding(end = 4.dp)
                                    )

                                    if (advancedMode) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    StrategyTestManager.togglePin(context, strategy)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = if (isPinned) R.drawable.ic_star_solid else R.drawable.ic_star_custom),
                                                contentDescription = "Pin",
                                                tint = if (isPinned) Color(0xFFFBBF24) else Color(0xFFA1A1AA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    editingStrategy = strategy
                                                    editNameText = customName ?: ""
                                                    editNotesText = notes ?: ""
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_edit_custom),
                                                contentDescription = "Edit",
                                                tint = Color(0xFFA1A1AA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    deletingStrategy = strategy
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_delete),
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            } else if (isSetupComplete) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_youtube),
                            contentDescription = null,
                            tint = Color(0xFFF4F4F5),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Настройка YouTube",
                            color = Color(0xFFF4F4F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    val currentCmd = activeCmd
                    val activePing = if (currentCmd != null) {
                         val item = StrategyTestManager.testResults.find { it.second.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == currentCmd.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") }
                        item?.third?.replace(" (Успешно)", "") ?: "неизвестно"
                    } else {
                        "неизвестно"
                    }
                    val friendlyName = StrategyTestManager.getActiveStrategyName(context)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF242426))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Сейчас используется:",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = friendlyName,
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (activePing != "неизвестно") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Задержка: $activePing",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp
                            )
                        }
                    }

                    val autoInteractionSource1 = remember { MutableInteractionSource() }
                    val autoFocused1 by autoInteractionSource1.collectIsFocusedAsState()
                    val autoHovered1 by autoInteractionSource1.collectIsHoveredAsState()
                    val autoHighlighted1 = autoFocused1 || autoHovered1

                    val manualInteractionSource1 = remember { MutableInteractionSource() }
                    val manualFocused1 by manualInteractionSource1.collectIsFocusedAsState()
                    val manualHovered1 by manualInteractionSource1.collectIsHoveredAsState()
                    val manualHighlighted1 = manualFocused1 || manualHovered1

                    val targetModeIndex = if (manualMode) 1f else 0f
                    val modeIndicatorIndex = remember { androidx.compose.animation.core.Animatable(targetModeIndex) }
                    val modeDragVisualIndex = modeIndicatorIndex.value

                    LaunchedEffect(manualMode) {
                        if (com.rupleide.netfix.data.performanceModeGlobal) {
                            modeIndicatorIndex.snapTo(targetModeIndex)
                        } else {
                            modeIndicatorIndex.animateTo(
                                targetValue = targetModeIndex,
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 350,
                                    easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f)
                                )
                            )
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161616))
                            .padding(4.dp)
                    ) {
                        val itemWidth = (maxWidth - 4.dp) / 2
                        val indicatorOffset = (itemWidth + 4.dp) * modeDragVisualIndex

                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(itemWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF3B82F6))
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val autoBgColor by animateColorAsState(
                                targetValue = if (autoHighlighted1 && manualMode) Color(0x1AFFFFFF) else Color.Transparent,
                                animationSpec = tween(150),
                                label = ""
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(autoBgColor)
                                    .then(
                                        if (autoHighlighted1) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .focusRequester(autoModeToggleFocusRequester)
                                    .clickable(
                                        interactionSource = autoInteractionSource1,
                                        indication = null
                                    ) { saveManualMode(false) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Автоматически",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            val manualBgColor by animateColorAsState(
                                targetValue = if (manualHighlighted1 && !manualMode) Color(0x1AFFFFFF) else Color.Transparent,
                                animationSpec = tween(150),
                                label = ""
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(manualBgColor)
                                    .then(
                                        if (manualHighlighted1) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clickable(
                                        interactionSource = manualInteractionSource1,
                                        indication = null
                                    ) { saveManualMode(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Вручную",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    val openInteractionSource = remember { MutableInteractionSource() }
                    val openPressed by openInteractionSource.collectIsPressedAsState()
                    val openHovered by openInteractionSource.collectIsHoveredAsState()
                    val openFocused by openInteractionSource.collectIsFocusedAsState()
                    val openHighlighted = openPressed || openHovered || openFocused

                    val openBgColor by animateColorAsState(
                        targetValue = if (openHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                        animationSpec = tween(150),
                        label = ""
                    )
                    val openBorderColor by animateColorAsState(
                        targetValue = if (openHighlighted) Color.White else Color(0x1AFFFFFF),
                        animationSpec = tween(150),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(openBgColor)
                            .border(1.dp, openBorderColor, RoundedCornerShape(12.dp))
                            .focusRequester(manualOpenYoutubeFocusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.DirectionDown && event.type == KeyEventType.KeyDown) {
                                    try { manualModeListFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                } else false
                            }
                            .clickable(
                                interactionSource = openInteractionSource,
                                indication = null
                            ) {
                                if (appStatus.first != AppStatus.Running) {
                                    val intent = android.net.VpnService.prepare(context)
                                    if (intent != null) {
                                        vpnLauncher.launch(intent)
                                    } else {
                                        startAll()
                                        launchYoutubeApp()
                                    }
                                } else {
                                    launchYoutubeApp()
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!isSmartTv) "Открыть YouTube" else if (isSmartTube) "SmartTube" else "Открыть YouTube",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    AnimatedContent(
                        targetState = manualMode,
                        transitionSpec = {
                            if (com.rupleide.netfix.data.performanceModeGlobal) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                val forward = targetState && !initialState
                                (fadeIn(tween(220)) + androidx.compose.animation.scaleIn(
                                    tween(220),
                                    initialScale = if (forward) 0.96f else 1.04f
                                )) togetherWith (fadeOut(tween(180)) + androidx.compose.animation.scaleOut(
                                    tween(180),
                                    targetScale = if (forward) 1.04f else 0.96f
                                ))
                            }
                        },
                        label = "modeTransition",
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp)
                    ) { isManual ->
                        if (!isManual) {
                            val resetInteractionSource = remember { MutableInteractionSource() }
                            val resetPressed by resetInteractionSource.collectIsPressedAsState()
                            val resetHovered by resetInteractionSource.collectIsHoveredAsState()
                            val resetFocused by resetInteractionSource.collectIsFocusedAsState()
                            val resetHighlighted = resetPressed || resetHovered || resetFocused

                            val resetTextColor by animateColorAsState(
                                targetValue = if (resetHighlighted) Color.White else Color(0xFFA1A1AA),
                                animationSpec = tween(150),
                                label = ""
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusProperties { down = navBarFocusRequester }
                                    .clickable(
                                        interactionSource = resetInteractionSource,
                                        indication = null
                                    ) {
                                        com.rupleide.netfix.core.debug.AppDebugManager.log("Открыт диалог выбора типа тестирования YouTube")
                                        showTestingTypeDialog = true
                                    },
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Подобрать лучший способ заново",
                                    color = resetTextColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Выберите стратегию обхода:",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    getSortedStrategies().forEachIndexed { idx, item ->
                                        val (index, strategy, status) = item
                                        val normalized = strategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
                                        val manualStatus = sharedPrefs.getString("manual_status_$normalized", null)
                                        val displayStatus = when (manualStatus) {
                                            "working" -> "Работает"
                                            "failed" -> "Не работает"
                                            else -> status.replace(" (Успешно)", "")
                                        }
                                        val statusColor = when (manualStatus) {
                                            "working" -> Color(0xFF22C55E)
                                            "failed" -> Color(0xFFEF4444)
                                            else -> if (status.contains("мс")) Color(0xFF22C55E) else Color(0xFFEF4444)
                                        }
                                        val isPinned = StrategyTestManager.pinnedStrategies[strategy] ?: false
                                        val customName = StrategyTestManager.customNames[strategy]
                                        val notes = StrategyTestManager.strategyNotes[strategy]
                                        val currentCmd = activeCmd
                                 val isCurrent = currentCmd != null && currentCmd.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == normalized

                                        val rowInteractionSource = remember { MutableInteractionSource() }
                                        val rowFocused by rowInteractionSource.collectIsFocusedAsState()
                                        val rowHovered by rowInteractionSource.collectIsHoveredAsState()

                                        val pinInteractionSource = remember { MutableInteractionSource() }
                                        val pinFocused by pinInteractionSource.collectIsFocusedAsState()
                                        val pinHovered by pinInteractionSource.collectIsHoveredAsState()

                                        val editInteractionSource = remember { MutableInteractionSource() }
                                        val editFocused by editInteractionSource.collectIsFocusedAsState()
                                        val editHovered by editInteractionSource.collectIsHoveredAsState()

                                        val deleteInteractionSource = remember { MutableInteractionSource() }
                                        val deleteFocused by deleteInteractionSource.collectIsFocusedAsState()
                                        val deleteHovered by deleteInteractionSource.collectIsHoveredAsState()

                                        val rowHighlighted = rowFocused || rowHovered || pinFocused || pinHovered || editFocused || editHovered || deleteFocused || deleteHovered

                                        val rowBgColor by animateColorAsState(
                                            targetValue = if (rowHighlighted) Color(0xFF38383A) else if (isCurrent) Color(0xFF2D2D30) else Color(0xFF242426),
                                            animationSpec = tween(150),
                                            label = ""
                                        )
                                        val rowBorderColor by animateColorAsState(
                                            targetValue = if (rowHighlighted) Color.White else Color(0x1AFFFFFF),
                                            animationSpec = tween(150),
                                            label = ""
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(rowBgColor)
                                                .border(1.5.dp, rowBorderColor, RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .then(if (idx == 0) Modifier.focusRequester(manualModeListFocusRequester) else Modifier)
                                                    .clickable(
                                                        interactionSource = rowInteractionSource,
                                                        indication = null
                                                    ) {
                                                        showActionMenuForStrategy = item
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = StrategyTestManager.getStrategyName(strategy, context),
                                                        color = Color(0xFFF4F4F5),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (advancedMode) {
                                                        Text(
                                                            text = strategy,
                                                            color = Color(0xFFA1A1AA),
                                                            fontSize = 11.sp,
                                                            maxLines = 1
                                                        )
                                                        if (!notes.isNullOrBlank()) {
                                                            Text(
                                                                text = "Заметка: $notes",
                                                                color = Color(0xFFA1A1AA),
                                                                fontSize = 11.sp,
                                                                maxLines = 2
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = displayStatus,
                                                    color = statusColor,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                            }

                                            if (advancedMode) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                     val pinHighlighted = pinFocused || pinHovered
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(if (pinHighlighted) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(6.dp))
                                                            .clickable(
                                                                interactionSource = pinInteractionSource,
                                                                indication = null
                                                            ) {
                                                                StrategyTestManager.togglePin(context, strategy)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = if (isPinned) R.drawable.ic_star_solid else R.drawable.ic_star_custom),
                                                            contentDescription = "Pin",
                                                            tint = if (isPinned) Color(0xFFFBBF24) else Color(0xFFA1A1AA),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    val editHighlighted = editFocused || editHovered
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(if (editHighlighted) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(6.dp))
                                                            .clickable(
                                                                interactionSource = editInteractionSource,
                                                                indication = null
                                                            ) {
                                                                editingStrategy = strategy
                                                                editNameText = customName ?: ""
                                                                editNotesText = notes ?: ""
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_edit_custom),
                                                            contentDescription = "Edit",
                                                            tint = Color(0xFFA1A1AA),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    val deleteHighlighted = deleteFocused || deleteHovered
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(if (deleteHighlighted) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(6.dp))
                                                            .clickable(
                                                                interactionSource = deleteInteractionSource,
                                                                indication = null
                                                            ) {
                                                                deletingStrategy = strategy
                                                             },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_delete),
                                                            contentDescription = "Delete",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            } else if (noStrategiesWorked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Не нашли рабочий способ",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Попробуйте улучшить соединение и отключить сторонние VPN-приложения, затем пройдите сканирование заново.\n\nТакже вы можете нажать на кнопку возврата к списку стратегий ниже и попробовать выбрать или настроить конфигурацию вручную.",
                        color = Color(0xFFA1A1AA),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    val retryInteractionSource = remember { MutableInteractionSource() }
                    val retryPressed by retryInteractionSource.collectIsPressedAsState()
                    val retryHovered by retryInteractionSource.collectIsHoveredAsState()
                    val retryFocused by retryInteractionSource.collectIsFocusedAsState()
                    val retryHighlighted = retryPressed || retryHovered || retryFocused

                    val retryBgColor by animateColorAsState(
                        targetValue = if (retryHighlighted) Color(0xFF3B82F6).copy(alpha = 0.85f) else Color(0xFF3B82F6),
                        animationSpec = tween(150),
                        label = ""
                    )
                    val retryBorderColor by animateColorAsState(
                        targetValue = if (retryHighlighted) Color.White else Color.Transparent,
                        animationSpec = tween(150),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(retryBgColor)
                            .border(1.5.dp, retryBorderColor, RoundedCornerShape(12.dp))
                            .focusRequester(noStrategiesRetryFocusRequester)
                            .focusProperties { if (isSmartTube) down = navBarFocusRequester }
                            .clickable(
                                interactionSource = retryInteractionSource,
                                indication = null
                            ) {
                                saveNoStrategiesWorked(false)
                                saveCurrentTestIndex(0)
                                saveHasLaunchedTest(false)
                                saveTestStarted(true)
                                StrategyTestManager.startTesting(context)
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Попробовать снова",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    val backListInteractionSource = remember { MutableInteractionSource() }
                    val backListPressed by backListInteractionSource.collectIsPressedAsState()
                    val backListHovered by backListInteractionSource.collectIsHoveredAsState()
                    val backListFocused by backListInteractionSource.collectIsFocusedAsState()
                    val backListHighlighted = backListPressed || backListHovered || backListFocused

                    val backListBgColor by animateColorAsState(
                        targetValue = if (backListHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                        animationSpec = tween(150),
                        label = ""
                    )
                    val backListBorderColor by animateColorAsState(
                        targetValue = if (backListHighlighted) Color.White else Color(0x1AFFFFFF),
                        animationSpec = tween(150),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(backListBgColor)
                            .border(1.dp, backListBorderColor, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = backListInteractionSource,
                                indication = null
                            ) {
                                saveNoStrategiesWorked(false)
                                saveManualMode(true)
                                saveIsSetupComplete(true)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Выбрать вручную",
                            color = Color(0xFFA1A1AA),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (!testStarted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_youtube),
                            contentDescription = null,
                            tint = Color(0xFFF4F4F5),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Настройка YouTube",
                            color = Color(0xFFF4F4F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Для работы YouTube нужно подобрать подходящую стратегию обхода блокировок под вашу сеть.",
                        color = Color(0xFFA1A1AA),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    val autoInteractionSource2 = remember { MutableInteractionSource() }
                    val autoFocused2 by autoInteractionSource2.collectIsFocusedAsState()
                    val autoHovered2 by autoInteractionSource2.collectIsHoveredAsState()
                    val autoHighlighted2 = autoFocused2 || autoHovered2
                    val autoBgColor2 = if (!manualMode) Color(0xFF3B82F6) else if (autoHighlighted2) Color(0x33FFFFFF) else Color.Transparent
                    val autoBorderColor2 = if (autoHighlighted2) Color.White else Color.Transparent

                    val manualInteractionSource2 = remember { MutableInteractionSource() }
                    val manualFocused2 by manualInteractionSource2.collectIsFocusedAsState()
                    val manualHovered2 by manualInteractionSource2.collectIsHoveredAsState()
                    val manualHighlighted2 = manualFocused2 || manualHovered2
                    val manualBgColor2 = if (manualMode) Color(0xFF3B82F6) else if (manualHighlighted2) Color(0x33FFFFFF) else Color.Transparent
                    val manualBorderColor2 = if (manualHighlighted2) Color.White else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(autoBgColor2)
                                .border(1.5.dp, autoBorderColor2, RoundedCornerShape(6.dp))
                                .focusRequester(autoModeToggleFocusRequester)
                                .clickable(
                                    interactionSource = autoInteractionSource2,
                                    indication = null
                                ) { saveManualMode(false) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Автоматически",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(manualBgColor2)
                                .border(1.5.dp, manualBorderColor2, RoundedCornerShape(6.dp))
                                .clickable(
                                    interactionSource = manualInteractionSource2,
                                    indication = null
                                ) { saveManualMode(true) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Вручную",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (!manualMode) {
                        val testBtnInteractionSource = remember { MutableInteractionSource() }
                        val testBtnPressed by testBtnInteractionSource.collectIsPressedAsState()
                        val testBtnHovered by testBtnInteractionSource.collectIsHoveredAsState()
                        val testBtnFocused by testBtnInteractionSource.collectIsFocusedAsState()
                        val testBtnHighlighted = testBtnPressed || testBtnHovered || testBtnFocused

                        val testBtnBgColor by animateColorAsState(
                            targetValue = if (testBtnHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                            animationSpec = tween(150),
                            label = ""
                        )
                        val testBtnBorderColor by animateColorAsState(
                            targetValue = if (testBtnHighlighted) Color.White else Color(0x1AFFFFFF),
                            animationSpec = tween(150),
                            label = ""
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(testBtnBgColor)
                                .border(1.dp, testBtnBorderColor, RoundedCornerShape(12.dp))
                                .focusRequester(wizardPlayFocusRequester)
                                .focusProperties { if (isSmartTube) down = navBarFocusRequester }
                                .clickable(
                                    interactionSource = testBtnInteractionSource,
                                    indication = null
                                ) {
                                    val successfulStrategies = StrategyTestManager.testResults.filter { it.third.contains("мс") }.map { it.second }
                                    if (successfulStrategies.isNotEmpty()) {
                                        val strategy = successfulStrategies[0]
                                        sharedPrefs.edit()
                                            .putString("byedpi_cmd_args", strategy)
                                            .putBoolean("byedpi_enable_cmd_settings", true)
                                            .apply()
                                        com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy = strategy
                                        saveCurrentTestIndex(0)
                                        saveHasLaunchedTest(false)
                                        saveTestStarted(true)
                                        saveNoStrategiesWorked(false)
                                    } else {
                                        saveCurrentTestIndex(0)
                                        saveHasLaunchedTest(false)
                                        saveTestStarted(true)
                                        StrategyTestManager.startTesting(context)
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Подобрать способ автоматически",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Выберите стратегию обхода:",
                            color = Color(0xFFA1A1AA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            getSortedStrategies().forEachIndexed { idx, item ->
                                val (index, strategy, status) = item
                                val normalized = strategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
                                val manualStatus = sharedPrefs.getString("manual_status_$normalized", null)
                                val displayStatus = when (manualStatus) {
                                    "working" -> "Работает"
                                    "failed" -> "Не работает"
                                    else -> status.replace(" (Успешно)", "")
                                }
                                val statusColor = when (manualStatus) {
                                    "working" -> Color(0xFF22C55E)
                                    "failed" -> Color(0xFFEF4444)
                                    else -> if (status.contains("мс")) Color(0xFF22C55E) else Color(0xFFEF4444)
                                }
                                val isPinned = StrategyTestManager.pinnedStrategies[strategy] ?: false
                                val customName = StrategyTestManager.customNames[strategy]
                                val notes = StrategyTestManager.strategyNotes[strategy]
                                val currentCmd = activeCmd
                                 val isCurrent = currentCmd != null && currentCmd.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com") == normalized

                                val rowInteractionSource = remember { MutableInteractionSource() }
                                val rowFocused by rowInteractionSource.collectIsFocusedAsState()
                                val rowHovered by rowInteractionSource.collectIsHoveredAsState()

                                val pinInteractionSource = remember { MutableInteractionSource() }
                                val pinFocused by pinInteractionSource.collectIsFocusedAsState()
                                val pinHovered by pinInteractionSource.collectIsHoveredAsState()

                                val editInteractionSource = remember { MutableInteractionSource() }
                                val editFocused by editInteractionSource.collectIsFocusedAsState()
                                val editHovered by editInteractionSource.collectIsHoveredAsState()

                                val deleteInteractionSource = remember { MutableInteractionSource() }
                                val deleteFocused by deleteInteractionSource.collectIsFocusedAsState()
                                val deleteHovered by deleteInteractionSource.collectIsHoveredAsState()

                                val rowHighlighted = rowFocused || rowHovered || pinFocused || pinHovered || editFocused || editHovered || deleteFocused || deleteHovered

                                val rowBgColor by animateColorAsState(
                                    targetValue = if (rowHighlighted) Color(0xFF38383A) else if (isCurrent) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFF242426),
                                    animationSpec = tween(150),
                                    label = ""
                                )
                                val rowBorderColor by animateColorAsState(
                                    targetValue = if (rowHighlighted) Color.White else if (isCurrent) Color(0xFF3B82F6) else Color(0x1AFFFFFF),
                                    animationSpec = tween(150),
                                    label = ""
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(rowBgColor)
                                        .border(1.5.dp, rowBorderColor, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(if (idx == 0) Modifier.focusRequester(manualModeListFocusRequester) else Modifier)
                                            .clickable(
                                                interactionSource = rowInteractionSource,
                                                indication = null
                                            ) {
                                                showActionMenuForStrategy = item
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = StrategyTestManager.getStrategyName(strategy, context),
                                                color = Color(0xFFF4F4F5),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            if (advancedMode) {
                                                Text(
                                                    text = strategy,
                                                    color = Color(0xFFA1A1AA),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                                if (!notes.isNullOrBlank()) {
                                                    Text(
                                                        text = "Заметка: $notes",
                                                        color = Color(0xFFA1A1AA),
                                                        fontSize = 11.sp,
                                                        maxLines = 2
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = displayStatus,
                                            color = statusColor,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }

                                    if (advancedMode) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                             val pinHighlighted = pinFocused || pinHovered
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(if (pinHighlighted) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(6.dp))
                                                    .clickable(
                                                        interactionSource = pinInteractionSource,
                                                        indication = null
                                                    ) {
                                                        StrategyTestManager.togglePin(context, strategy)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = if (isPinned) R.drawable.ic_star_solid else R.drawable.ic_star_custom),
                                                    contentDescription = "Pin",
                                                    tint = if (isPinned) Color(0xFFFBBF24) else Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            val editHighlighted = editFocused || editHovered
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(if (editHighlighted) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(6.dp))
                                                    .clickable(
                                                        interactionSource = editInteractionSource,
                                                        indication = null
                                                    ) {
                                                        editingStrategy = strategy
                                                        editNameText = customName ?: ""
                                                        editNotesText = notes ?: ""
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_edit_custom),
                                                    contentDescription = "Edit",
                                                    tint = Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            val deleteHighlighted = deleteFocused || deleteHovered
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(if (deleteHighlighted) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(6.dp))
                                                    .clickable(
                                                        interactionSource = deleteInteractionSource,
                                                        indication = null
                                                    ) {
                                                        deletingStrategy = strategy
                                                     },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_delete),
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            } else {
                val wasManual = sharedPrefs.getBoolean("was_manual_selection", false)
                val headerText = if (wasManual) {
                    "Проверка выбранной стратегии"
                } else if (!isSmartTv) {
                    if (hasLaunchedTest) {
                        "Шаг ${currentTestIndex + 1}: Подбор способа обхода"
                    } else if (currentTestIndex == 0) {
                        "Финальный шаг 🎉"
                    } else {
                        "Пробуем Способ ${currentTestIndex + 1}"
                    }
                } else if (!isSmartTube) {
                    "Шаг ${currentTestIndex + 1}: Настройка Официального YouTube"
                } else if (hasLaunchedTest) {
                    "Шаг ${currentTestIndex + 1}: Подбор способа обхода"
                } else if (currentTestIndex == 0) {
                    "Финальный шаг 🎉"
                } else {
                    "Пробуем Способ ${currentTestIndex + 1}"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFF4F4F5),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = headerText,
                            color = Color(0xFFF4F4F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    if (!hasLaunchedTest) {
                        val descText = if (wasManual) {
                            if (!isSmartTv) {
                                "Вы выбрали стратегию вручную. Проверьте её работу в официальном приложении YouTube:\n\n" +
                                "1. Нажмите «Открыть YouTube» (выбранная конфигурация применится автоматически).\n" +
                                "2. Убедитесь, что видеоролики начали воспроизводиться без зависаний.\n" +
                                "3. Вернитесь в NetFix и укажите, помог ли вам этот способ."
                            } else {
                                "Вы выбрали стратегию вручную. Проверьте её работу:\n\n" +
                                "1. Нажмите «Открыть SmartTube» (выбранная конфигурация применится автоматически).\n" +
                                "2. Убедитесь, что видеоролики начали воспроизводиться без зависаний.\n" +
                                "3. Вернитесь в NetFix и укажите, помог ли вам этот способ."
                            }
                        } else if (!isSmartTv) {
                            if (currentTestIndex == 0) {
                                "Поздравляем с завершением начальной настройки! Теперь нужно найти способ обхода, который работает именно у вашего оператора или провайдера. На основе проверки сети NetFix уже отобрал лучшие варианты.\n\n" +
                                "1. Нажмите «Открыть YouTube» (наш VPN запустится автоматически).\n" +
                                "2. Проверьте работу любого видео в официальном приложении YouTube.\n" +
                                "3. Вернитесь в NetFix и нажмите «👍 Работает» или «👎 Не работает»."
                            } else {
                                "Предыдущий вариант не подошел, ничего страшного, мы переключились на другую схему обхода. Нажмите «Открыть YouTube», проверьте видеоролики в приложении и возвращайтесь. Мы обязательно найдем рабочий способ!"
                            }
                        } else if (!isSmartTube) {
                            "Вы выбрали Официальный YouTube. К сожалению, из-за ограничений системы и нехватки оперативной памяти ТВ этот метод не гарантирует 100% работу. Но мы всё равно постараемся подобрать рабочий способ!"
                        } else if (currentTestIndex == 0) {
                            "Поздравляем с завершением начальной настройки! Теперь нужно найти способ обхода, который работает именно у вашего провайдера. На основе проверки сети NetFix уже отобрал лучшие варианты.\n\n" +
                            "1. Нажмите «Открыть SmartTube» (наш VPN запустится автоматически).\n" +
                            "2. Проверьте работу любого видеоролика.\n" +
                            "3. Вернитесь в NetFix и нажмите «👍 Работает» или «👎 Не работает».\n\n" +
                            "Переключаться между приложениями на ТВ может быть непривычно, но это единственный способ подобрать конфигурацию, которая гарантированно будет работать у вас!"
                        } else {
                            "Предыдущий вариант не подошел, ничего страшного, мы переключились на другую схему обхода. Нажмите «Открыть SmartTube», проверьте видеоролики и возвращайтесь. Мы обязательно найдем рабочий способ!"
                        }

                        Text(
                            text = descText,
                            color = Color(0xFFA1A1AA),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        val playInteractionSource = remember { MutableInteractionSource() }
                        val playPressed by playInteractionSource.collectIsPressedAsState()
                        val playHovered by playInteractionSource.collectIsHoveredAsState()
                        val playFocused by playInteractionSource.collectIsFocusedAsState()
                        val playHighlighted = playPressed || playHovered || playFocused

                        val playBgColor by animateColorAsState(
                            targetValue = if (playHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                            animationSpec = tween(150),
                            label = ""
                        )
                        val playBorderColor by animateColorAsState(
                            targetValue = if (playHighlighted) Color.White else Color(0x1AFFFFFF),
                            animationSpec = tween(150),
                            label = ""
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(playBgColor)
                                .border(1.dp, playBorderColor, RoundedCornerShape(12.dp))
                                .focusRequester(wizardPlayFocusRequester)
                                .focusProperties { if (isSmartTube) down = navBarFocusRequester }
                                .clickable(
                                    interactionSource = playInteractionSource,
                                    indication = null
                                ) {
                                    val successfulStrategies = StrategyTestManager.testResults.filter { it.third.contains("мс") }.map { it.second }
                                    val strategy = successfulStrategies.getOrNull(currentTestIndex) ?: (presets.getOrNull(currentTestIndex) ?: presets[0])
                                    sharedPrefs.edit()
                                        .putString("byedpi_cmd_args", strategy)
                                        .putBoolean("byedpi_enable_cmd_settings", true)
                                        .apply()
                                    com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy = strategy
                                    val intent = android.net.VpnService.prepare(context)
                                    if (intent != null) {
                                        vpnLauncher.launch(intent)
                                    } else {
                                        startAll()
                                        launchYoutubeApp()
                                        saveHasLaunchedTest(true)
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (!isSmartTv) "Открыть YouTube" else if (isSmartTube) "SmartTube" else "Открыть YouTube",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Text(
                            text = if (!isSmartTv) {
                                "Видеоролики в приложении начали загружаться без зависаний?"
                            } else {
                                "Видеоролики в плеере начали загружаться без зависаний?"
                            },
                            color = Color(0xFFA1A1AA),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val okInteractionSource = remember { MutableInteractionSource() }
                            val okPressed by okInteractionSource.collectIsPressedAsState()
                            val okHovered by okInteractionSource.collectIsHoveredAsState()
                            val okFocused by okInteractionSource.collectIsFocusedAsState()
                            val okHighlighted = okPressed || okHovered || okFocused

                            val okBgColor by animateColorAsState(
                                targetValue = if (okHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                                animationSpec = tween(150),
                                label = ""
                            )
                            val okBorderColor by animateColorAsState(
                                targetValue = if (okHighlighted) Color.White else Color(0x1AFFFFFF),
                                animationSpec = tween(150),
                                label = ""
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(okBgColor)
                                    .border(1.dp, okBorderColor, RoundedCornerShape(12.dp))
                                    .focusRequester(wizardPlayFocusRequester)
                                    .focusProperties { if (isSmartTube) down = navBarFocusRequester }
                                    .clickable(
                                        interactionSource = okInteractionSource,
                                        indication = null
                                    ) {
                                        val successfulStrategies = StrategyTestManager.testResults.filter { it.third.contains("мс") }.map { it.second }
                                        val strategy = com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy 
                                            ?: (successfulStrategies.getOrNull(currentTestIndex) ?: (presets.getOrNull(currentTestIndex) ?: presets[0]))
                                        val normalized = strategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
                                        sharedPrefs.edit()
                                            .putString("byedpi_cmd_args", strategy)
                                            .putBoolean("byedpi_enable_cmd_settings", true)
                                            .putString("manual_status_$normalized", "working")
                                            .putBoolean("was_manual_selection", false)
                                            .apply()
                                        com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy = strategy
                                        saveIsSetupComplete(true)
                                        saveHasLaunchedTest(false)
                                        saveTestStarted(false)
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👍 Работает",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            val failInteractionSource = remember { MutableInteractionSource() }
                            val failPressed by failInteractionSource.collectIsPressedAsState()
                            val failHovered by failInteractionSource.collectIsHoveredAsState()
                            val failFocused by failInteractionSource.collectIsFocusedAsState()
                            val failHighlighted = failPressed || failHovered || failFocused

                            val failBgColor by animateColorAsState(
                                targetValue = if (failHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                                animationSpec = tween(150),
                                label = ""
                            )
                            val failBorderColor by animateColorAsState(
                                targetValue = if (failHighlighted) Color.White else Color(0x1AFFFFFF),
                                animationSpec = tween(150),
                                label = ""
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(failBgColor)
                                    .border(1.dp, failBorderColor, RoundedCornerShape(12.dp))
                                    .focusProperties { if (isSmartTube) down = navBarFocusRequester }
                                    .clickable(
                                        interactionSource = failInteractionSource,
                                        indication = null
                                    ) {
                                        val successfulStrategies = StrategyTestManager.testResults.filter { it.third.contains("мс") }.map { it.second }
                                        val strategy = com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy 
                                            ?: (successfulStrategies.getOrNull(currentTestIndex) ?: (presets.getOrNull(currentTestIndex) ?: presets[0]))
                                        val normalized = strategy.replace("{sni}", "youtube.com,googlevideo.com,ytimg.com,ggpht.com,google.com")
                                        val wasManual = sharedPrefs.getBoolean("was_manual_selection", false)
                                        sharedPrefs.edit()
                                            .putString("manual_status_$normalized", "failed")
                                            .putBoolean("was_manual_selection", false)
                                            .apply()
                                        stopAll()
                                        com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy = null
                                        if (wasManual) {
                                            saveManualMode(true)
                                            saveIsSetupComplete(true)
                                        } else {
                                            val successfulStrategiesCount = StrategyTestManager.testResults.filter { it.third.contains("мс") }.size
                                            val maxIndex = if (successfulStrategiesCount > 0) successfulStrategiesCount - 1 else presets.size - 1
                                            if (currentTestIndex >= maxIndex) {
                                                saveNoStrategiesWorked(true)
                                                saveTestStarted(false)
                                            } else {
                                                val nextIndex = currentTestIndex + 1
                                                saveCurrentTestIndex(nextIndex)
                                            }
                                        }
                                        saveHasLaunchedTest(false)
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👎 Не работает",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    if (isSmartTv && !isSmartTube) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Ничего не заработало? Официальный YouTube часто блокирует обходы на телевизорах. Рекомендуем переключиться на SmartTube, он гарантированно запустится без зависаний и рекламы.",
                                color = Color(0xFFA1A1AA),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.focusable()
                            )

                            val switchInteractionSource = remember { MutableInteractionSource() }
                            val switchPressed by switchInteractionSource.collectIsPressedAsState()
                            val switchHovered by switchInteractionSource.collectIsHoveredAsState()
                            val switchFocused by switchInteractionSource.collectIsFocusedAsState()
                            val switchHighlighted = switchPressed || switchHovered || switchFocused

                            val switchBgColor by animateColorAsState(
                                targetValue = if (switchHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                                animationSpec = tween(150),
                                label = ""
                            )
                            val switchBorderColor by animateColorAsState(
                                targetValue = if (switchHighlighted) Color.White else Color(0x1AFFFFFF),
                                animationSpec = tween(150),
                                label = ""
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(switchBgColor)
                                    .border(1.5.dp, switchBorderColor, RoundedCornerShape(12.dp))
                                    .focusProperties { down = navBarFocusRequester }
                                    .clickable(
                                        interactionSource = switchInteractionSource,
                                        indication = null
                                    ) {
                                        sharedPrefs.edit().putString("youtube_mode", "smarttube").apply()
                                        youtubeMode = "smarttube"
                                        stopAll()
                                        saveIsSetupComplete(false)
                                        saveCurrentTestIndex(0)
                                        saveHasLaunchedTest(false)
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Перейти на SmartTube",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                    }
                }
            }
            }
            }

        NetFixEditStrategySheet(
        visible = editingStrategy != null,
        onDismissRequest = { editingStrategy = null },
        nameValue = editNameText,
        onNameChange = { editNameText = it },
        notesValue = editNotesText,
        onNotesChange = { editNotesText = it },
        onSave = {
            val strategy = editingStrategy!!
            StrategyTestManager.renameStrategy(context, strategy, editNameText)
            StrategyTestManager.updateNotes(context, strategy, editNotesText)
            editingStrategy = null
        }
    )

            NetFixStrategySheet(
        visible = deletingStrategy != null,
        onDismissRequest = { deletingStrategy = null },
        title = "Удалить стратегию?",
        subtitle = "Вы действительно хотите удалить эту стратегию из списка?",
        iconRes = R.drawable.ic_delete,
        actions = listOf(
            StrategyAction(
                label = "Удалить",
                destructive = true,
                bold = true
            ) {
                StrategyTestManager.deleteStrategy(context, deletingStrategy!!)
                deletingStrategy = null
            }
        )
    )

            val menuStrategy = showActionMenuForStrategy?.second
    val menuFriendlyName = menuStrategy?.let { StrategyTestManager.getStrategyName(it, context) } ?: ""
    val menuIndex = showActionMenuForStrategy?.first ?: 0

    NetFixStrategySheet(
        visible = showActionMenuForStrategy != null,
        onDismissRequest = { showActionMenuForStrategy = null },
        title = menuFriendlyName,
        subtitle = "Выберите действие для этой конфигурации:",
        iconRes = R.drawable.ic_settings,
        actions = listOf(
            StrategyAction(
                label = "Применить сразу",
                bold = true
            ) {
                if (menuStrategy != null) {
                    StrategyTestManager.applyStrategy(context, menuIndex, menuStrategy)
                    activeCmd = menuStrategy
                    saveIsSetupComplete(true)
                    saveTestStarted(false)
                    saveHasLaunchedTest(false)
                }
                showActionMenuForStrategy = null
            },
            StrategyAction(
                label = "Проверить работу"
            ) {
                if (menuStrategy != null) {
                    sharedPrefs.edit().putBoolean("was_manual_selection", true).apply()
                    StrategyTestManager.applyStrategy(context, menuIndex, menuStrategy)
                    activeCmd = menuStrategy
                    saveIsSetupComplete(false)
                    saveTestStarted(true)
                    saveHasLaunchedTest(false)
                    saveCurrentTestIndex(menuIndex)
                }
                showActionMenuForStrategy = null
                showAllStrategiesList = false
            }
        )
    )

            NetFixStrategySheet(
        visible = showVpnDeniedDialog,
        onDismissRequest = { showVpnDeniedDialog = false },
        title = "Требуется разрешение",
        subtitle = "Для работы обхода блокировок нужно разрешить подключение VPN в системе.",
        iconRes = R.drawable.ic_info,
        actions = listOf(
            StrategyAction(
                label = "Попробовать снова",
                bold = true
            ) {
                showVpnDeniedDialog = false
                val intent = android.net.VpnService.prepare(context)
                if (intent != null) {
                    vpnLauncher.launch(intent)
                } else {
                    startAll()
                    if (!isSetupComplete && testStarted) {
                        launchYoutubeApp()
                        saveHasLaunchedTest(true)
                    } else {
                        launchYoutubeApp()
                    }
                }
            }
        )
    )

            NetFixStrategySheet(
        visible = showTestingTypeDialog,
        onDismissRequest = { showTestingTypeDialog = false },
        title = "Подобрать способ обхода",
        subtitle = "Как вы хотите найти рабочую схему обхода ограничений YouTube для вашей сети?",
        iconRes = R.drawable.ic_smart_tv,
        actions = listOf(
            StrategyAction(
                label = "Проверить способы по очереди",
                bold = true
            ) {
                showTestingTypeDialog = false
                stopAll()
                val strategy = presets.getOrNull(0) ?: "-i 127.0.0.1 -p 1080"
                com.rupleide.netfix.core.debug.AppDebugManager.log("Выбран пошаговый перебор стратегий YouTube")
                sharedPrefs.edit()
                    .putString("byedpi_cmd_args", strategy)
                    .putBoolean("byedpi_enable_cmd_settings", true)
                    .putBoolean("was_manual_selection", false)
                    .apply()
                com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy = strategy
                saveIsSetupComplete(false)
                saveTestStarted(true)
                saveHasLaunchedTest(false)
                saveCurrentTestIndex(0)
                saveNoStrategiesWorked(false)
            },
            StrategyAction(
                label = "Провести сканирование ещё раз"
            ) {
                showTestingTypeDialog = false
                stopAll()
                com.rupleide.netfix.core.debug.AppDebugManager.log("Запущен автоподбор стратегий YouTube (сканирование)")
                com.rupleide.netfix.core.dpibypass.StrategyTestManager.appliedStrategy = null
                saveIsSetupComplete(false)
                saveCurrentTestIndex(0)
                saveHasLaunchedTest(false)
                saveTestStarted(true)
                saveNoStrategiesWorked(false)
                StrategyTestManager.startTesting(context)
            }
        )
    )
            }
        }
    }
}
