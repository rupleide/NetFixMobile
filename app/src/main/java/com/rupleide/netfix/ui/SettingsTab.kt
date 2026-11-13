package com.rupleide.netfix.ui

import com.rupleide.netfix.core.dpibypass.launchIntentWithLeanback
import androidx.compose.foundation.layout.ColumnScope
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.NfTvSectionItem
import com.rupleide.netfix.ui.components.NfTvTwoPane
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.rupleide.netfix.ui.components.NetFixSwitch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import com.rupleide.netfix.ui.components.BypassGuard
import com.rupleide.netfix.ui.components.CompatibilitySettingsRows
import com.rupleide.netfix.ui.components.TunnelDnsRows
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfCornerAura
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfDivider
import com.rupleide.netfix.ui.components.NfMiniButton
import com.rupleide.netfix.ui.components.NfRow
import com.rupleide.netfix.ui.components.NfScreenTitle
import com.rupleide.netfix.ui.components.NfSection
import com.rupleide.netfix.ui.components.NfSwitchRow
import com.rupleide.netfix.ui.components.NfValueTrailing
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.rememberStagedLevel
import com.rupleide.netfix.ui.components.NetFixTextField
import com.rupleide.netfix.ui.components.NetFixActionSheet
import com.rupleide.netfix.ui.components.NetFixLogsSheet
import com.rupleide.netfix.ui.components.NetFixResetSheet
import com.rupleide.netfix.ui.components.ResetAction
import com.rupleide.netfix.ui.components.NetFixUnloadSheet
import com.rupleide.netfix.ui.components.UnloadOption
import com.rupleide.netfix.ui.components.NetFixSheetOption
import com.rupleide.netfix.R
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.service.VendorAutoStartHelper
import com.rupleide.netfix.core.update.UpdateManager
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.rememberNetFixHaptics
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.appListMode
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults
import com.rupleide.netfix.core.dpibypass.rememberAiAppChoice
import com.rupleide.netfix.core.dpibypass.aiAppsToAdd
import com.rupleide.netfix.core.dpibypass.setAppListMode
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.appStatus
import com.rupleide.netfix.data.performanceModeGlobal
import com.rupleide.netfix.ui.main.liteAnimationsEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private var cachedInstalledApps: List<PackageInfo>? = null

private val settingsChipIcons = listOf(
    R.drawable.ic_play,
    R.drawable.ic_settings,
    R.drawable.ic_bolt,
    R.drawable.ic_phone,
    R.drawable.ic_arrow,
    R.drawable.ic_smart_tv,
    R.drawable.ic_check,
    R.drawable.ic_info,
    R.drawable.ic_delete
)

private val settingsChips = listOf("Запуск", "Система", "Батарея", "Интерфейс", "DNS", "Совместимость", "Данные", "Отладка", "Сброс")

@Composable
private fun SettingsChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(10.dp)
    val background by animateColorAsState(
        targetValue = when {
            pressed -> Color(0xFF333333)
            selected -> Color(0xFF2E2E2E)
            else -> Color(0xFF242424)
        },
        animationSpec = tween(280),
        label = "chipBg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) Color(0x2EFFFFFF) else Color(0x14FFFFFF),
        animationSpec = tween(280),
        label = "chipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFE4E4E7) else Color(0xFFA1A1AA),
        animationSpec = tween(280),
        label = "chipText"
    )
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptics(NetFixHaptic.Tap)
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun Modifier.trackTop(map: MutableMap<Int, Int>, key: Int): Modifier =
    this.onGloballyPositioned { map[key] = it.positionInParent().y.toInt() }

private val autoExcludePackages = listOf(
    "ru.gosuslugi.gost",
    "ru.gosuslugi.key",
    "ru.avito.app",
    "ru.mts.mymts",
    "ru.sberbankmobile",
    "ru.tinkoff.activities",
    "ru.tinkoff.investments",
    "ru.alfa.mobile.fastara",
    "ru.alfabank.investments",
    "ru.vtb24.mobilebanking",
    "ru.vtb.invest",
    "ru.gpb.mobilebank",
    "ru.sovcomcard.halva.v1",
    "ru.raiffeisennews",
    "ru.mts.money",
    "ru.openbank",
    "ru.pochtabank.mobile",
    "ru.dublgis.2gismobile",
    "com.wildberries.ru",
    "ru.ozon.app.android",
    "ru.yandex.market",
    "ru.sbermegamarket.app",
    "ru.samokat.app",
    "ru.vk.store",
    "com.vkontakte.android",
    "ru.vk.video",
    "com.boom",
    "ru.ok.android",
    "ru.rutube.video",
    "ru.kinopoisk",
    "ru.yandex.music"
)

private fun dnsHostnameFor(preset: String): String? = when (preset) {
    "Cloudflare Secure DNS" -> "one.one.one.one"
    "Google Public DNS" -> "dns.google"
    "AdGuard DNS (Блокировка рекламы)" -> "dns.adguard-dns.com"
    "Supercell Xbox DNS (supercell.xbox-dns.ru)" -> "supercell.xbox-dns.ru"
    "NullsProxy DNS (dns.nullsproxy.com)" -> "dns.nullsproxy.com"
    "Comss.one DNS (dns.comss.one)" -> "dns.comss.one"
    "Geohide DNS (dns.geohide.ru)" -> "dns.geohide.ru"
    "AstraCat DNS (dns.astracat.network)" -> "dns.astracat.network"
    "WLRus DNS (ChatGPT / Gemini)" -> "dns.wlrus.cfd"
    "Dagami DNS (ChatGPT / Gemini)" -> "dns.dagami.ru"
    else -> null
}

private val privateDnsPresets = listOf(
    "WLRus DNS (ChatGPT / Gemini)",
    "Comss.one DNS (dns.comss.one)",
    "Geohide DNS (dns.geohide.ru)",
    "AstraCat DNS (dns.astracat.network)",
    "Dagami DNS (ChatGPT / Gemini)",
    "NullsProxy DNS (dns.nullsproxy.com)",
    "Supercell Xbox DNS (supercell.xbox-dns.ru)",
    "Cloudflare Secure DNS",
    "Google Public DNS",
    "AdGuard DNS (Блокировка рекламы)"
)

private fun actualPrivateDnsPreset(mode: String, specifier: String): String? = when {
    mode != "hostname" || specifier.isBlank() -> "Стандартный (Отключено)"
    else -> privateDnsPresets.firstOrNull { dnsHostnameFor(it).equals(specifier, ignoreCase = true) }
}

private fun copyDnsHostname(context: android.content.Context, hostname: String) {
    try {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Private DNS", hostname))
        android.widget.Toast.makeText(context, "Адрес скопирован", android.widget.Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

@Composable
fun SettingsTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE) }
    val isTv = remember(sharedPrefs) { sharedPrefs.getBoolean("device_type_tv", false) }
    val scope = rememberCoroutineScope()
    val performFullExit = {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", false).commit()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, com.rupleide.netfix.service.NetFixTileService::class.java)
            )
        } catch (_: Exception) {}
        
        com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(context)
        
        ServiceManager.stop(context)
        context.stopService(android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java))
        com.rupleide.netfix.core.tgproxy.TgProxyController.stop()
        
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancelAll()
        
        (context as? android.app.Activity)?.let { activity ->
            activity.finishAndRemoveTask()
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            kotlin.system.exitProcess(0)
        }, 150L)
    }
    var qrCodesEnabled by remember {
        mutableStateOf(
            if (isTv) true else sharedPrefs.getBoolean("qr_codes_enabled", false)
        )
    }
    var autostart by remember { mutableStateOf(TgProxyController.isAutostartEnabled(context)) }
    var autoConnect by remember { mutableStateOf(false) }
    var openTgOnConnect by remember { mutableStateOf(false) }
    var autoUpdate by remember { mutableStateOf(UpdateManager.isAutoUpdateEnabled(context)) }
    var isSelectedAppsOnlyMode by remember {
        mutableStateOf(sharedPrefs.appListMode() == com.rupleide.netfix.core.dpibypass.AppListMode.SelectedAppsOnly)
    }
    var rootMode by remember { mutableStateOf(ServiceManager.rootMode(context)) }
    var scanCheckDiscord by remember {
        mutableStateOf(com.rupleide.netfix.core.dpibypass.StrategyTester.discordCheckEnabled(sharedPrefs))
    }
    var tgProxyWakeLock by remember {
        mutableStateOf(sharedPrefs.getBoolean("tg_proxy_wakelock_enabled", false))
    }
    val aggressiveFirmware = remember { com.rupleide.netfix.ui.components.detectAggressiveFirmware() }
    var isStableProxyHintDismissed by remember {
        mutableStateOf(sharedPrefs.getBoolean(com.rupleide.netfix.ui.components.STABLE_PROXY_HINT_DISMISSED_KEY, false))
    }
    var performanceMode by remember {
        mutableStateOf(
            sharedPrefs.getBoolean("performance_mode", false)
        )
    }
    var liteAnimations by remember {
        mutableStateOf(sharedPrefs.liteAnimationsEnabled(context))
    }
    var showDebugMenuDialog by remember { mutableStateOf(false) }
    var showBypassModeDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }
    var dnsInstructionPreset by remember { mutableStateOf<String?>(null) }
    var showYoutubePlayerSheet by remember { mutableStateOf(false) }
    var youtubePlayerMode by remember { mutableStateOf(sharedPrefs.getString("youtube_mode", "official") ?: "official") }
    var showYoutubeClientSheet by remember { mutableStateOf(false) }
    var youtubeClientSheetOpensPicker by remember { mutableStateOf(false) }
    var youtubeClientLabel by remember { mutableStateOf(com.rupleide.netfix.core.YoutubeClient.buttonLabel(context)) }
    var showAppsScreen by remember { mutableStateOf(false) }
    val tvApps = LocalIsTv.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showUnloadDialog by remember { mutableStateOf(false) }
    val compatibilityState = com.rupleide.netfix.ui.components.rememberCompatibilitySettingsState(sharedPrefs)
    androidx.compose.runtime.DisposableEffect(tvApps, showAppsScreen) {
        com.rupleide.netfix.data.isTvFullScreenOverlayGlobal = tvApps && showAppsScreen
        onDispose { com.rupleide.netfix.data.isTvFullScreenOverlayGlobal = false }
    }
    com.rupleide.netfix.data.TrackActionSheetVisible(
        showBypassModeDialog || showDebugMenuDialog || showDnsDialog || showResetDialog || showUnloadDialog || dnsInstructionPreset != null || showYoutubePlayerSheet || showYoutubeClientSheet || (tvApps && showAppsScreen)
    )

    val exportAllLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = com.rupleide.netfix.core.dpibypass.StrategyTestManager.exportAllResultsJson(context)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                android.widget.Toast.makeText(context, "Данные сохранены", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Ошибка экспорта: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: ""
                val count = com.rupleide.netfix.core.dpibypass.StrategyTestManager.importStrategiesFromJson(context, jsonString)
                if (count > 0) {
                    android.widget.Toast.makeText(context, "Импортировано способов: $count", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "В файле нет способов", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Ошибка импорта: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val backupFallbackFile = remember { java.io.File(context.getExternalFilesDir(null), "netfix_backup.json") }
    val runExport: () -> Unit = {
        try {
            exportAllLauncher.launch("netfix_backup.json")
        } catch (_: Exception) {
            try {
                backupFallbackFile.parentFile?.mkdirs()
                backupFallbackFile.writeText(com.rupleide.netfix.core.dpibypass.StrategyTestManager.exportAllResultsJson(context))
                android.widget.Toast.makeText(context, "Сохранено: ${backupFallbackFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Ошибка экспорта: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val runImport: () -> Unit = {
        try {
            importLauncher.launch("*/*")
        } catch (_: Exception) {
            try {
                if (!backupFallbackFile.exists()) {
                    android.widget.Toast.makeText(context, "Файл не найден: ${backupFallbackFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    val count = com.rupleide.netfix.core.dpibypass.StrategyTestManager.importStrategiesFromJson(context, backupFallbackFile.readText())
                    android.widget.Toast.makeText(context, if (count > 0) "Импортировано способов: $count" else "В файле нет способов", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Ошибка импорта: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    var bypassMode by remember {
        val wantsYt = sharedPrefs.getBoolean("wants_youtube_bypass", true)
        val wantsTg = sharedPrefs.getBoolean("telegram_proxy_enabled_by_user", true)
        val initialValue = when {
            wantsYt && wantsTg -> "both"
            wantsTg -> "telegram"
            wantsYt -> "youtube"
            else -> "both"
        }
        mutableStateOf(initialValue)
    }
    var selectedDnsPreset by remember { mutableStateOf("Стандартный (Отключено)") }
    var privateDnsMode by remember { mutableStateOf("") }

    LaunchedEffect(context) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
        autoConnect = prefs.getBoolean("auto_connect_on_start", false)
        openTgOnConnect = prefs.getBoolean("open_tg_on_connect", true)
        selectedDnsPreset = prefs.getString("custom_dns_preset", "Стандартный (Отключено)") ?: "Стандартный (Отключено)"
        val wantsYt = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTg = prefs.getBoolean("telegram_proxy_enabled_by_user", true)
        bypassMode = when {
            wantsYt && wantsTg -> "both"
            wantsTg -> "telegram"
            wantsYt -> "youtube"
            else -> "both"
        }
    }
    var privateDnsSpecifier by remember { mutableStateOf("") }
    androidx.lifecycle.compose.LifecycleResumeEffect(context) {
        privateDnsMode = android.provider.Settings.Global.getString(context.contentResolver, "private_dns_mode") ?: "off"
        privateDnsSpecifier = android.provider.Settings.Global.getString(context.contentResolver, "private_dns_specifier").orEmpty().trim()
        onPauseOrDispose { }
    }

    val tvSingleFocus = com.rupleide.netfix.ui.components.LocalIsTv.current
    LaunchedEffect(Unit) {
        repeat(if (tvSingleFocus) 1 else 8) { index ->
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            delay(100L + index * 50L)
        }
    }

    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()

    var isBatteryIgnored by remember { mutableStateOf(false) }
    val batteryCheckLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(context, batteryCheckLifecycle) {
        batteryCheckLifecycle.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                isBatteryIgnored = VendorAutoStartHelper.isBatteryOptimizationIgnored(context)
                delay(2000)
            }
        }
    }

    var selectedApps by remember {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
        mutableStateOf(prefs.selectedAppsWithDefaults())
    }
    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var appsLoaded by remember { mutableStateOf(false) }
    var appsFailed by remember { mutableStateOf(false) }
    var appsReloadKey by remember { mutableStateOf(0) }
    val savedInstalledSelectedCount = remember { sharedPrefs.getInt("selected_apps_installed_count", -1) }
    val actualSelectedCount = remember(selectedApps, installedApps) {
        if (installedApps.isEmpty()) {
            if (savedInstalledSelectedCount >= 0) savedInstalledSelectedCount else selectedApps.size
        } else {
            val installedSet = installedApps.map { pkg -> pkg.packageName }.toSet()
            selectedApps.count { name -> name in installedSet }
        }
    }

    LaunchedEffect(actualSelectedCount, installedApps) {
        if (installedApps.isNotEmpty()) {
            sharedPrefs.edit().putInt("selected_apps_installed_count", actualSelectedCount).apply()
        }
    }

    LaunchedEffect(appsReloadKey) {
        appsFailed = false
        val cached = cachedInstalledApps
        if (cached != null && appsReloadKey == 0) {
            installedApps = cached
            appsLoaded = true
            return@LaunchedEffect
        }
        if (appsReloadKey == 0) delay(900)
        try {
            val loaded = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                pm.getInstalledPackages(PackageManager.GET_META_DATA)
                    .filter { pkg ->
                        val flags = pkg.applicationInfo?.flags ?: 0
                        val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystem = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        isUpdatedSystem || !isSystem || pm.launchIntentWithLeanback(pkg.packageName) != null
                    }
                    .map { pkg ->
                        val label = pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg.packageName
                        label to pkg
                    }
                    .sortedBy { it.first.lowercase() }
                    .map { it.second }
            }
            cachedInstalledApps = loaded
            installedApps = loaded
            appsLoaded = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            appsFailed = true
        }
    }

    val settingsScroll = rememberScrollState()
    val stage = rememberStagedLevel(3)
    val sectionTops = remember { mutableStateMapOf<Int, Int>() }
    val barHeightPx = with(density) { 52.dp.roundToPx() }
    val barNaturalPx = with(density) { 57.dp.roundToPx() }
    var viewportPx by remember { mutableStateOf(0) }
    val activeChip by remember {
        derivedStateOf {
            val maxScroll = settingsScroll.maxValue
            val scrolled = settingsScroll.value
            if (maxScroll > 0 && scrolled >= maxScroll - 12) {
                settingsChips.lastIndex
            } else {
                val progress = if (maxScroll > 0) scrolled.toFloat() / maxScroll else 0f
                val threshold = scrolled + (progress * viewportPx * 0.8f).toInt()
                var active = 0
                settingsChips.indices.forEach { index ->
                    val top = sectionTops[index] ?: Int.MAX_VALUE
                    if (top - barHeightPx - 24 <= threshold) active = index
                }
                active
            }
        }
    }
    var debugEnabled by remember { mutableStateOf(com.rupleide.netfix.core.debug.AppDebugManager.isLoggingEnabled()) }
    var vendorIntent by remember { mutableStateOf<android.content.Intent?>(null) }
    LaunchedEffect(context) {
        vendorIntent = withContext(Dispatchers.IO) { VendorAutoStartHelper.getVendorAutoStartIntent(context) }
    }
    val bypassModeText = when (bypassMode) {
        "telegram" -> "Только Telegram"
        "youtube" -> "Только YouTube"
        else -> "Telegram и YouTube"
    }
    val actualDnsPreset = actualPrivateDnsPreset(privateDnsMode, privateDnsSpecifier)
    val shortDnsName = when {
        privateDnsMode != "hostname" -> "Нет"
        actualDnsPreset != null -> actualDnsPreset.substringBefore(" (").trim()
        else -> privateDnsSpecifier.ifBlank { "Нет" }
    }
    val isPrivateDnsBlocking = privateDnsMode == "hostname" && privateDnsSpecifier.isNotBlank()
    val isCustomDnsSelected = compatibilityState.dnsMode != com.rupleide.netfix.core.compat.TunnelDnsMode.System
    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    val performOnboardingReset = {
        com.rupleide.netfix.core.debug.AppDebugManager.log("Выполнен сброс онбординга с перезапуском приложения")
        context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", false)
            .commit()
        val restartIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (restartIntent != null) {
            restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(restartIntent)
        }
        java.lang.System.exit(0)
    }

    val tv = LocalIsTv.current
    var tvSection by remember { mutableIntStateOf(0) }
    val sectionListEntry = remember { FocusRequester() }
    val shown: (Int) -> Boolean = { !tv || tvSection == it }
    LaunchedEffect(com.rupleide.netfix.data.settingsSectionRequestGlobal) {
        val section = com.rupleide.netfix.data.settingsSectionRequestGlobal ?: return@LaunchedEffect
        delay(500)
        if (tv) {
            tvSection = section
        } else {
            val top = sectionTops[section]
            if (top != null) settingsScroll.animateScrollTo((top - barHeightPx - 12).coerceAtLeast(0))
        }
        com.rupleide.netfix.data.settingsSectionRequestGlobal = null
    }

    val sectionsContent: @Composable ColumnScope.() -> Unit = {
            if (!tv && aggressiveFirmware != null && !tgProxyWakeLock && !isStableProxyHintDismissed) {
                com.rupleide.netfix.ui.components.StableProxyHintCard(
                    firmware = aggressiveFirmware,
                    onEnable = {
                        BypassGuard.guarded(context) {
                            tgProxyWakeLock = true
                            sharedPrefs.edit().putBoolean("tg_proxy_wakelock_enabled", true).apply()
                        }
                    },
                    onDismiss = {
                        isStableProxyHintDismissed = true
                        sharedPrefs.edit().putBoolean(com.rupleide.netfix.ui.components.STABLE_PROXY_HINT_DISMISSED_KEY, true).apply()
                    }
                )
            }

            if (shown(0)) NfSection(
                title = "Запуск",
                modifier = Modifier.nfEnter(1).trackTop(sectionTops, 0),
                note = "Включите стабильный прокси, если Telegram-прокси отключается через 10–15 сек. Может повлиять на расход батареи."
            ) {
                NfRow(
                    title = "Режим работы",
                    subtitle = "Какие сервисы обходить",
                    iconPainter = painterResource(id = R.drawable.ic_bolt),
                    modifier = if (tv) Modifier else Modifier.focusRequester(focusRequester),
                    onClick = { BypassGuard.guarded(context) { showBypassModeDialog = true } },
                    trailing = {
                        NfValueTrailing(
                            when (bypassMode) {
                                "telegram" -> "Telegram"
                                "youtube" -> "YouTube"
                                else -> "Telegram + YT"
                            }
                        )
                    }
                )
                NfDivider()
                NfRow(
                    title = "Обход приложений",
                    subtitle = when {
                        !isSelectedAppsOnlyMode -> "Обход для всех приложений"
                        actualSelectedCount > 0 -> "Только нужные · выбрано $actualSelectedCount"
                        else -> "Выберите приложения, которые не открываются"
                    },
                    icon = Icons.Default.AppBlocking,
                    onClick = { showAppsScreen = true },
                    trailing = { NfValueTrailing() }
                )
                NfDivider()
                NfSwitchRow(
                    title = "Проверять Discord",
                    newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.DISCORD,
                    subtitle = if (scanCheckDiscord) {
                        "Подбор способа учитывает и YouTube, и Discord"
                    } else {
                        "Подбор смотрит только на YouTube. Включите, если нужен Discord"
                    },
                    icon = Icons.Default.Forum,
                    checked = scanCheckDiscord,
                    onCheckedChange = { checked ->
                        scanCheckDiscord = checked
                        sharedPrefs.edit().putBoolean(com.rupleide.netfix.core.dpibypass.StrategyTester.KEY_SCAN_CHECK_DISCORD, checked).apply()
                    }
                )
                NfDivider()
                if (tv) {
                    NfRow(
                        title = "Плеер YouTube",
                        subtitle = "Что открывает кнопка YouTube на главной",
                        iconPainter = painterResource(id = R.drawable.ic_youtube),
                        onClick = { showYoutubePlayerSheet = true },
                        trailing = { NfValueTrailing(if (youtubePlayerMode == "smarttube") "SmartTube" else youtubeClientLabel) }
                    )
                    NfDivider()
                } else {
                    NfRow(
                        title = "Приложение YouTube",
                        subtitle = "Что открывает кнопка YouTube на главной",
                        iconPainter = painterResource(id = R.drawable.ic_youtube),
                        newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.YOUTUBE_APP,
                        onClick = { showYoutubeClientSheet = true },
                        trailing = { NfValueTrailing(youtubeClientLabel) }
                    )
                    NfDivider()
                }
                NfSwitchRow(
                    title = "Режим стабильного прокси",
                    subtitle = "Если прокси Telegram отключается",
                    icon = Icons.Default.Timer,
                    checked = tgProxyWakeLock,
                    onCheckedChange = { checked ->
                        BypassGuard.guarded(context) {
                            tgProxyWakeLock = checked
                            sharedPrefs.edit().putBoolean("tg_proxy_wakelock_enabled", checked).apply()
                        }
                    }
                )
            }

            if (shown(1)) NfSection(title = "Система", modifier = Modifier.nfEnter(2).trackTop(sectionTops, 1)) {
                NfSwitchRow(
                    title = "Автозапуск",
                    subtitle = "Запуск при загрузке устройства",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = autostart,
                    onCheckedChange = { checked ->
                        autostart = checked
                        TgProxyController.setAutostartEnabled(context, checked)
                    }
                )
                NfDivider()
                NfSwitchRow(
                    title = "Автоподключение",
                    subtitle = "Подключаться при открытии приложения",
                    icon = Icons.Default.Link,
                    checked = autoConnect,
                    onCheckedChange = { checked ->
                        autoConnect = checked
                        sharedPrefs.edit().putBoolean("auto_connect_on_start", checked).apply()
                    }
                )
                NfDivider()
                NfSwitchRow(
                    title = "Открыть Telegram при подключении",
                    subtitle = "Применять настройки прокси автоматически",
                    icon = Icons.Default.Send,
                    checked = openTgOnConnect,
                    onCheckedChange = { checked ->
                        openTgOnConnect = checked
                        sharedPrefs.edit().putBoolean("open_tg_on_connect", checked).apply()
                    }
                )
                NfDivider()
                NfSwitchRow(
                    title = "Автообновление",
                    subtitle = "Автоматическая проверка обновлений в фоне",
                    icon = Icons.Default.Update,
                    checked = autoUpdate,
                    onCheckedChange = { checked ->
                        autoUpdate = checked
                        UpdateManager.setAutoUpdateEnabled(context, checked)
                    }
                )
            }

            if (stage >= 1) {
                if (shown(2)) NfSection(
                    title = "Батарея и разрешения",
                    modifier = Modifier.nfEnter(3).trackTop(sectionTops, 2),
                    note = "Запуск в фоне закроет приложение полностью для экономии памяти, без Watchdog."
                ) {
                    NfRow(
                        title = "Оптимизация батареи",
                        subtitle = "Неограниченная фоновая работа",
                        icon = Icons.Default.BatteryChargingFull,
                        onClick = if (isBatteryIgnored) null else ({ VendorAutoStartHelper.requestIgnoreBatteryOptimization(context) }),
                        trailing = {
                            if (isBatteryIgnored) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Nf.GreenText, modifier = Modifier.size(18.dp))
                                    Text("Разрешено", color = Nf.GreenText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                NfMiniButton("Разрешить", onClick = { VendorAutoStartHelper.requestIgnoreBatteryOptimization(context) })
                            }
                        }
                    )
                    if (vendorIntent != null) {
                        NfDivider()
                        NfRow(
                            title = "Автозапуск (вендор)",
                            subtitle = "Системные настройки фоновой работы",
                            icon = Icons.Default.Settings,
                            onClick = { VendorAutoStartHelper.openVendorAutoStartSettings(context) },
                            trailing = { NfMiniButton("Настроить", onClick = { VendorAutoStartHelper.openVendorAutoStartSettings(context) }) }
                        )
                    }
                    NfDivider()
                    NfRow(
                        title = "Запустить в фоне и выйти",
                        subtitle = "Запустит обход и закроет приложение",
                        icon = Icons.Default.ExitToApp,
                        onClick = { showUnloadDialog = true },
                        trailing = { NfMiniButton("Запуск", onClick = { showUnloadDialog = true }) }
                    )
                }
            }

            if (stage >= 1) {
                if (shown(3)) NfSection(title = "Интерфейс", modifier = Modifier.nfEnter(4).trackTop(sectionTops, 3)) {
                    NfSwitchRow(
                        title = "Режим максимальной производительности",
                        subtitle = "Отключает анимации и визуальные эффекты",
                        icon = Icons.Default.Speed,
                        checked = performanceMode,
                        onCheckedChange = { checked ->
                            performanceMode = checked
                            performanceModeGlobal = checked
                            sharedPrefs.edit().putBoolean("performance_mode", checked).apply()
                        }
                    )
                    NfDivider()
                    NfSwitchRow(
                        title = "Оптимизация анимаций на главном экране",
                        subtitle = "Упрощённый глобус без размытия: меньше нагрев и расход батареи",
                        icon = Icons.Default.Animation,
                        enabled = !performanceMode,
                        checked = liteAnimations,
                        onCheckedChange = { checked ->
                            liteAnimations = checked
                            sharedPrefs.edit().putBoolean(com.rupleide.netfix.ui.main.LITE_ANIMATIONS_KEY, checked).apply()
                        }
                    )
                    NfDivider()
                    NfSwitchRow(
                        title = "Показывать QR-коды",
                        subtitle = if (isTv) "Включено автоматически для Смарт ТВ" else "Отображать QR-коды вместе с ссылками",
                        icon = Icons.Default.QrCode,
                        enabled = !isTv,
                        checked = qrCodesEnabled,
                        onCheckedChange = { checked ->
                            qrCodesEnabled = checked
                            sharedPrefs.edit().putBoolean("qr_codes_enabled", checked).apply()
                        }
                    )
                    NfDivider()
                    NfRow(
                        title = "Сбросить онбординг",
                        subtitle = "Приветственный экран при перезапуске",
                        icon = Icons.Default.Replay,
                        onClick = { performOnboardingReset() },
                        trailing = { NfMiniButton("Сбросить", onClick = { performOnboardingReset() }) }
                    )
                }
            }

            if (stage >= 2) {
                if (shown(4)) NfSection(
                    title = "DNS",
                    modifier = Modifier.nfEnter(5).trackTop(sectionTops, 4),
                    note = "DNS внутри туннеля работает только для приложений из «Обхода приложений». Маршрутизация DNS действует на ${if (tvSingleFocus) "всю приставку" else "весь телефон"}, даже на банки и другие приложения вне NetFix. Если она включена, DNS внутри туннеля не действует."
                ) {
                    TunnelDnsRows(compatibilityState)
                    NfDivider()
                    NfRow(
                        title = "Маршрутизация DNS",
                        subtitle = if (tvSingleFocus) "Частный DNS на всю приставку" else "Частный DNS на весь телефон",
                        icon = Icons.Default.Dns,
                        onClick = {
                            BypassGuard.guarded(context) {
                                com.rupleide.netfix.core.debug.AppDebugManager.log("Открыт диалог выбора DNS-серверов")
                                showDnsDialog = true
                            }
                        },
                        trailing = { NfValueTrailing(shortDnsName) }
                    )
                    if (isPrivateDnsBlocking && isCustomDnsSelected) {
                        val warnShape = RoundedCornerShape(14.dp)
                        Row(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .height(88.dp)
                                .nfTvFocus(corner = 16.dp, grow = 1.03f)
                                .clip(warnShape)
                                .background(Nf.Yellow.copy(alpha = 0.10f))
                                .border(1.dp, Nf.Yellow.copy(alpha = 0.35f), warnShape)
                                .clickable {
                                    try {
                                        context.startActivity(android.content.Intent("android.settings.PRIVATE_DNS_SETTINGS"))
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                                        } catch (_: Exception) {}
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Nf.Yellow, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Приватный DNS Android включён",
                                    color = Nf.Yellow,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Он важнее DNS внутри туннеля. Чтобы работал DNS туннеля, выберите в настройках «Отключено».",
                                    color = Nf.TextSub,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            NfValueTrailing()
                        }
                    }
                }
            }

            if (stage >= 2) {
                if (shown(5)) NfSection(
                    title = "Совместимость",
                    modifier = Modifier.nfEnter(6).trackTop(sectionTops, 5),
                    note = "Всё новое из этой версии уже включено. Обычно здесь ничего менять не нужно."
                ) {
                    CompatibilitySettingsRows(compatibilityState) {
                        NfDivider()
                        NfSwitchRow(
                            title = "Обход через root",
                            subtitle = "Нужен root. Работает без VPN, только для выбранных приложений и IPv4",
                            icon = Icons.Default.Settings,
                            checked = rootMode,
                            onCheckedChange = { checked ->
                                if (ServiceManager.isAlive) {
                                    android.widget.Toast.makeText(context,
                                        "Сначала остановите обход кнопкой в приложении или уведомлении",
                                        android.widget.Toast.LENGTH_LONG).show()
                                    return@NfSwitchRow
                                }
                                BypassGuard.guarded(context) {
                                    rootMode = checked
                                    sharedPrefs.edit().putBoolean("byedpi_root_mode", checked).apply()
                                    if (checked) {
                                        isSelectedAppsOnlyMode = true
                                        sharedPrefs.setAppListMode(com.rupleide.netfix.core.dpibypass.AppListMode.SelectedAppsOnly)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (stage >= 2) {
                if (shown(6)) NfSection(
                    title = "Экспорт и импорт",
                    modifier = Modifier.nfEnter(7).trackTop(sectionTops, 6),
                    note = "В файле все способы, их названия, выбор и результаты проверки для Wi-Fi и мобильной сети. Его можно загрузить на другом телефоне."
                ) {
                    NfRow(
                        title = "Экспорт всех данных",
                        subtitle = "Способы, названия и результаты для Wi-Fi и мобильной сети",
                        icon = Icons.Default.Upload,
                        onClick = { runExport() },
                        trailing = { NfMiniButton("Экспорт", onClick = { runExport() }) }
                    )
                    NfDivider()
                    NfRow(
                        title = "Импорт данных",
                        subtitle = "Файл экспорта NetFix",
                        icon = Icons.Default.Download,
                        onClick = { runImport() },
                        trailing = { NfMiniButton("Импорт", onClick = { runImport() }) }
                    )
                }
            }

            if (stage >= 3) {
                if (shown(7)) NfSection(title = "Отладка", modifier = Modifier.nfEnter(8).trackTop(sectionTops, 7)) {
                    NfSwitchRow(
                        title = "Запись логов",
                        subtitle = "Записывать историю действий в память",
                        icon = Icons.Default.BugReport,
                        checked = debugEnabled,
                        onCheckedChange = { checked ->
                            debugEnabled = checked
                            com.rupleide.netfix.core.debug.AppDebugManager.setEnabled(context, checked)
                        }
                    )
                    NfDivider()
                    NfRow(
                        title = "Просмотр логов",
                        subtitle = "История событий и логов приложения",
                        icon = Icons.Default.Description,
                        onClick = { showDebugMenuDialog = true },
                        trailing = { NfValueTrailing() }
                    )
                    NfDivider()
                    NfRow(
                        title = "Поделиться диагностикой",
                        subtitle = "Файл с настройками сети и ошибками. Адреса и ключи скрыты",
                        icon = Icons.Default.Share,
                        onClick = {
                            val appContext = context.applicationContext
                            Thread {
                                val message = com.rupleide.netfix.core.debug.DiagnosticReport.shareOrSave(appContext)
                                if (message != null) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        android.widget.Toast.makeText(appContext, message, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }.start()
                        },
                        trailing = { NfValueTrailing() }
                    )
                    NfDivider()
                    NfRow(
                        title = "Помощник",
                        newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.HELP,
                        subtitle = "Проверит, что не так, и предложит исправления по одному",
                        icon = Icons.Default.Help,
                        onClick = { com.rupleide.netfix.data.helpOpenRequestGlobal = "" },
                        trailing = { NfValueTrailing() }
                    )
                }
            }

            if (stage >= 3) {
                if (shown(8)) NfSection(title = "Выключение", modifier = Modifier.nfEnter(9).trackTop(sectionTops, 8)) {
                    NfRow(
                        title = "Полное закрытие",
                        subtitle = "Остановить все службы и выгрузить",
                        icon = Icons.Default.ExitToApp,
                        onClick = { performFullExit() },
                        trailing = { NfMiniButton("Выйти", onClick = { performFullExit() }) }
                    )
                }
            }

            if (stage >= 3) {
                if (shown(8)) NfSection(
                    title = "Сброс",
                    titleColor = Nf.RedText,
                    borderColor = Nf.Red.copy(alpha = 0.25f),
                    modifier = Modifier.nfEnter(10)
                ) {
                    NfRow(
                        title = "Сброс приложения",
                        subtitle = "Полная очистка или с сохранением тестов",
                        icon = Icons.Default.Refresh,
                        iconTint = Nf.RedText,
                        iconBackground = Nf.Red.copy(alpha = 0.12f),
                        modifier = Modifier.focusProperties { down = navBarFocusRequester },
                        onClick = {
                            com.rupleide.netfix.core.debug.AppDebugManager.log("Открыт диалог сброса приложения")
                            showResetDialog = true
                        },
                        trailing = {
                            NfMiniButton("Сбросить", style = NfButtonStyle.Danger, onClick = {
                                com.rupleide.netfix.core.debug.AppDebugManager.log("Открыт диалог сброса приложения")
                                showResetDialog = true
                            })
                        }
                    )
                }
            }

            if (stage >= 3 && !tv) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NetFix $versionName", color = Nf.Muted, fontSize = 12.sp, lineHeight = 16.sp)
                    Text("Настройки сохраняются сразу", color = Nf.Muted, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Nf.Bg),
        contentAlignment = Alignment.TopCenter
    ) {
        if (tv) {
            NfTvTwoPane(
                modifier = Modifier.focusProperties { canFocus = !showAppsScreen },
                title = "Настройки",
                rightTitle = settingsChips[tvSection],
                hideSectionTitles = true,
                leftEntry = sectionListEntry,
                rightScroll = settingsScroll,
                left = {
                    settingsChips.forEachIndexed { index, label ->
                        NfTvSectionItem(
                            label = label,
                            iconRes = settingsChipIcons[index],
                            selected = tvSection == index,
                            onFocus = { tvSection = index },
                            modifier = (if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                                .then(if (index == tvSection) Modifier.focusRequester(sectionListEntry) else Modifier),
                            danger = index == settingsChips.lastIndex
                        )
                    }
                    Text(
                        text = "NetFix $versionName · сохраняется сразу",
                        color = Nf.TextSub,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                right = { sectionsContent() }
            )
        } else {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (isLandscape) 820.dp else 500.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .onSizeChanged { viewportPx = it.height }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(settingsScroll)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = navOverlayReserve + 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NfScreenTitle("Настройки", Modifier.nfEnter(0))

                Spacer(modifier = Modifier.height(36.dp))

                sectionsContent()
            }

            val chipsScroll = rememberScrollState()
            val chipBounds = remember { mutableStateMapOf<Int, Int>() }
            val chipMargin = with(density) { 40.dp.roundToPx() }
            LaunchedEffect(activeChip) {
                val left = chipBounds[activeChip] ?: return@LaunchedEffect
                chipsScroll.animateScrollTo((left - chipMargin).coerceAtLeast(0))
            }
            Row(
                modifier = Modifier
                    .offset { IntOffset(0, maxOf(barNaturalPx - settingsScroll.value, 0)) }
                    .fillMaxWidth()
                    .background(Nf.Bg)
                    .horizontalScroll(chipsScroll)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settingsChips.forEachIndexed { index, label ->
                    SettingsChip(
                        label = label,
                        selected = index == activeChip,
                        modifier = Modifier.onGloballyPositioned { chipBounds[index] = it.positionInParent().x.toInt() },
                        onClick = {
                            val top = sectionTops[index]
                            if (top != null) {
                                scope.launch { settingsScroll.animateScrollTo((top - barHeightPx - 12).coerceAtLeast(0)) }
                            }
                        }
                    )
                }
            }
        }
        }

        NfCornerAura(Nf.Bright.copy(alpha = 0.056f))

        val appsScreenContent: @Composable () -> Unit = {
            AppBypassScreen(
                selectedOnly = isSelectedAppsOnlyMode,
                onSelectedOnlyChange = { checked ->
                    if (rootMode && !checked) {
                        android.widget.Toast.makeText(context, "Root: выберите приложения для обхода", android.widget.Toast.LENGTH_LONG).show()
                        return@AppBypassScreen
                    }
                    isSelectedAppsOnlyMode = checked
                    sharedPrefs.setAppListMode(
                        if (checked) com.rupleide.netfix.core.dpibypass.AppListMode.SelectedAppsOnly
                        else com.rupleide.netfix.core.dpibypass.AppListMode.AllTraffic
                    )
                    if (ServiceManager.isConnected) {
                        ServiceManager.restart(context, Mode.VPN)
                    }
                },
                installedApps = installedApps,
                loaded = appsLoaded,
                failed = appsFailed,
                selectedApps = selectedApps,
                onToggle = { pkg ->
                    val newSet = selectedApps.toMutableSet()
                    val nowSelected = newSet.add(pkg)
                    if (!nowSelected) newSet.remove(pkg)
                    selectedApps = newSet
                    sharedPrefs.edit().putStringSet("selected_apps", newSet).apply()
                    sharedPrefs.rememberAiAppChoice(pkg, nowSelected)
                },
                onPickPopular = {
                    val newSet = selectedApps + com.rupleide.netfix.core.dpibypass.defaultYoutubeApps + com.rupleide.netfix.core.dpibypass.extraBypassApps + sharedPrefs.aiAppsToAdd()
                    selectedApps = newSet
                    sharedPrefs.edit().putStringSet("selected_apps", newSet).apply()
                },
                onRetry = { appsReloadKey += 1 },
                onBack = { showAppsScreen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
        AnimatedVisibility(
            visible = showAppsScreen,
            enter = slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(260)),
            exit = slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(200))
        ) {
            appsScreenContent()
        }
    }

    val systemInfo = remember(showDebugMenuDialog) { if (showDebugMenuDialog) com.rupleide.netfix.core.debug.AppDebugManager.generateSystemInfoDump(context) else "" }
    val logsList = remember(showDebugMenuDialog) { if (showDebugMenuDialog) com.rupleide.netfix.core.debug.AppDebugManager.getLogs() else emptyList() }
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    com.rupleide.netfix.ui.components.NfBottomSheet(
        visible = showYoutubePlayerSheet,
        onDismiss = { showYoutubePlayerSheet = false },
        icon = Icons.Default.Settings,
        title = "Плеер YouTube",
        subtitle = "Что открывать на главном экране",
        closeLabel = "Закрыть"
    ) { entry ->
        val customPlayerChosen = youtubePlayerMode == "official" && com.rupleide.netfix.core.YoutubeClient.chosenPackage(context) != null
        Text(
            text = "Сейчас выбран: " + when {
                youtubePlayerMode == "smarttube" -> "SmartTube"
                customPlayerChosen -> youtubeClientLabel
                else -> "официальный YouTube"
            },
            color = Nf.TextSub,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = entry(0)
        )
        NfButton(
            text = "Официальный YouTube",
            style = if (youtubePlayerMode == "official" && !customPlayerChosen) NfButtonStyle.Tinted else NfButtonStyle.Secondary,
            modifier = entry(1),
            onClick = {
                sharedPrefs.edit()
                    .putString("youtube_mode", "official")
                    .putBoolean("yt_check_done", true)
                    .putBoolean("yt_check_failed", false)
                    .putBoolean("yt_check_pending", false)
                    .apply()
                com.rupleide.netfix.core.YoutubeClient.choose(context, null)
                youtubeClientLabel = com.rupleide.netfix.core.YoutubeClient.buttonLabel(context)
                youtubePlayerMode = "official"
                showYoutubePlayerSheet = false
            }
        )
        if (customPlayerChosen) {
            NfButton(
                text = youtubeClientLabel,
                style = NfButtonStyle.Tinted,
                modifier = entry(4),
                onClick = { showYoutubePlayerSheet = false }
            )
        }
        NfButton(
            text = "SmartTube",
            style = if (youtubePlayerMode == "smarttube") NfButtonStyle.Tinted else NfButtonStyle.Secondary,
            modifier = entry(2),
            onClick = {
                sharedPrefs.edit()
                    .putString("youtube_mode", "smarttube")
                    .putBoolean("yt_check_done", true)
                    .putBoolean("yt_check_pending", false)
                    .apply()
                youtubePlayerMode = "smarttube"
                showYoutubePlayerSheet = false
            }
        )
        NfButton(
            text = "Другое приложение…",
            style = NfButtonStyle.Secondary,
            modifier = entry(5),
            onClick = {
                showYoutubePlayerSheet = false
                youtubeClientSheetOpensPicker = true
                showYoutubeClientSheet = true
            }
        )
        NfButton(
            text = "Проверить YouTube заново",
            style = NfButtonStyle.Secondary,
            modifier = entry(3),
            onClick = {
                sharedPrefs.edit()
                    .putString("youtube_mode", "smarttube")
                    .putBoolean("yt_check_done", false)
                    .putBoolean("yt_check_failed", false)
                    .putBoolean("yt_check_pending", false)
                    .putBoolean("yt_check_open_now", true)
                    .apply()
                youtubePlayerMode = "smarttube"
                showYoutubePlayerSheet = false
                com.rupleide.netfix.data.onNavigateToTab?.invoke(0)
            }
        )
    }

    val youtubeClients = remember(showYoutubeClientSheet) { com.rupleide.netfix.core.YoutubeClient.installedClients(context) }
    val chosenYoutubeClient = remember(showYoutubeClientSheet) {
        com.rupleide.netfix.core.YoutubeClient.chosenPackage(context) ?: com.rupleide.netfix.core.YoutubeClient.OFFICIAL
    }
    var pickingOtherYoutubeClient by remember(showYoutubeClientSheet) { mutableStateOf(youtubeClientSheetOpensPicker) }
    var otherYoutubeClientQuery by remember(showYoutubeClientSheet) { mutableStateOf("") }
    var downloadingYoutubeClient by remember(showYoutubeClientSheet) { mutableStateOf(false) }
    val otherYoutubeClients by androidx.compose.runtime.produceState<List<com.rupleide.netfix.core.YoutubeClientApp>?>(initialValue = null, pickingOtherYoutubeClient) {
        value = if (!pickingOtherYoutubeClient) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.rupleide.netfix.core.YoutubeClient.otherLaunchableApps(context)
        }
    }
    val chooseYoutubeClient: (String) -> Unit = { packageName ->
        com.rupleide.netfix.core.YoutubeClient.choose(context, packageName)
        if (youtubePlayerMode != "official") {
            sharedPrefs.edit()
                .putString("youtube_mode", "official")
                .putBoolean("yt_check_done", true)
                .putBoolean("yt_check_failed", false)
                .putBoolean("yt_check_pending", false)
                .apply()
            youtubePlayerMode = "official"
        }
        if (com.rupleide.netfix.core.YoutubeClient.includeInBypass(context, packageName)) {
            selectedApps = sharedPrefs.selectedAppsWithDefaults()
            com.rupleide.netfix.core.dpibypass.ServiceManager.restart(context.applicationContext, com.rupleide.netfix.data.Mode.VPN)
        }
        youtubeClientLabel = com.rupleide.netfix.core.YoutubeClient.buttonLabel(context)
        showYoutubeClientSheet = false
        youtubeClientSheetOpensPicker = false
    }
    com.rupleide.netfix.ui.components.NfBottomSheet(
        visible = showYoutubeClientSheet,
        onDismiss = {
            if (downloadingYoutubeClient) {
                downloadingYoutubeClient = false
            } else if (pickingOtherYoutubeClient && !youtubeClientSheetOpensPicker) {
                pickingOtherYoutubeClient = false
            } else {
                showYoutubeClientSheet = false
                youtubeClientSheetOpensPicker = false
            }
        },
        icon = Icons.Default.Settings,
        title = "Приложение YouTube",
        subtitle = when {
            downloadingYoutubeClient -> "Откроется страница загрузки. После установки выберите приложение здесь"
            pickingOtherYoutubeClient -> "Выберите приложение. NetFix сам добавит его в список приложений"
            else -> "Что открывать кнопкой YouTube на главном экране"
        },
        closeLabel = if (downloadingYoutubeClient || (pickingOtherYoutubeClient && !youtubeClientSheetOpensPicker)) "Назад" else "Закрыть",
        scrollResetKey = pickingOtherYoutubeClient to downloadingYoutubeClient
    ) { entry ->
        if (downloadingYoutubeClient) {
            com.rupleide.netfix.core.YoutubeClient.downloads.forEachIndexed { index, download ->
                NfRow(
                    title = download.label,
                    subtitle = download.note,
                    iconPainter = painterResource(id = R.drawable.ic_youtube),
                    modifier = entry(index),
                    onClick = {
                        try {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(download.url))
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(context, "Не нашлось браузера", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        } else if (pickingOtherYoutubeClient) {
            com.rupleide.netfix.ui.components.NfInputField(
                value = otherYoutubeClientQuery,
                onValueChange = { otherYoutubeClientQuery = it.take(40) },
                placeholder = "Найти приложение",
                modifier = entry(0)
            )
            val loaded = otherYoutubeClients
            if (loaded == null) {
                com.rupleide.netfix.ui.components.NfProgress(fraction = 0f, indeterminate = true)
            } else {
                val needle = otherYoutubeClientQuery.trim().lowercase()
                val shown = loaded.filter { needle.isEmpty() || needle in it.label.lowercase() }
                if (shown.isEmpty()) {
                    Text(
                        text = "Ничего не нашлось",
                        color = Nf.TextSub,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = entry(1)
                    )
                }
                shown.forEachIndexed { index, app ->
                    NfButton(
                        text = app.label,
                        style = if (app.packageName == chosenYoutubeClient) NfButtonStyle.Tinted else NfButtonStyle.Secondary,
                        modifier = entry(index + 2),
                        onClick = { chooseYoutubeClient(app.packageName) }
                    )
                }
            }
        } else {
            val options = if (youtubeClients.any { it.packageName == com.rupleide.netfix.core.YoutubeClient.OFFICIAL }) {
                youtubeClients
            } else {
                listOf(com.rupleide.netfix.core.YoutubeClientApp(com.rupleide.netfix.core.YoutubeClient.OFFICIAL, "YouTube")) + youtubeClients
            }
            if (options.size == 1) {
                Text(
                    text = "Других приложений YouTube не найдено. Если ваше приложение не в списке, нажмите «Другое приложение»",
                    color = Nf.TextSub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = entry(0)
                )
            }
            options.forEachIndexed { index, client ->
                NfButton(
                    text = client.label,
                    style = if (client.packageName == chosenYoutubeClient) NfButtonStyle.Tinted else NfButtonStyle.Secondary,
                    modifier = entry(index + 1),
                    onClick = { chooseYoutubeClient(client.packageName) }
                )
            }
            NfButton(
                text = "Другое приложение…",
                style = NfButtonStyle.Secondary,
                modifier = entry(options.size + 1),
                onClick = { pickingOtherYoutubeClient = true }
            )
            NfButton(
                text = "Скачать другое приложение…",
                style = NfButtonStyle.Secondary,
                modifier = entry(options.size + 2),
                onClick = { downloadingYoutubeClient = true }
            )
        }
    }

    NetFixLogsSheet(
        visible = showDebugMenuDialog,
        onDismissRequest = { showDebugMenuDialog = false },
        icon = R.drawable.ic_info,
        title = "Логи отладки",
        subtitle = "История последних действий приложения (хранится временно в оперативной памяти):",
        systemInfo = systemInfo,
        logsList = logsList,
        onCopy = {
            val text = systemInfo + "\n" + logsList.joinToString("\n")
            val clip = android.content.ClipData.newPlainText("NetFix Logs", text)
            clipboardManager.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Логи скопированы", android.widget.Toast.LENGTH_SHORT).show()
        },
        onClear = {
            com.rupleide.netfix.core.debug.AppDebugManager.clear()
            showDebugMenuDialog = false
            android.widget.Toast.makeText(context, "Логи очищены", android.widget.Toast.LENGTH_SHORT).show()
        }
    )

    val dnsList = listOf(
        "Стандартный (Отключено)",
        "WLRus DNS (ChatGPT / Gemini)",
        "Comss.one DNS (dns.comss.one)",
        "Geohide DNS (dns.geohide.ru)",
        "AstraCat DNS (dns.astracat.network)",
        "Dagami DNS (ChatGPT / Gemini)",
        "NullsProxy DNS (dns.nullsproxy.com)",
        "Supercell Xbox DNS (supercell.xbox-dns.ru)",
        "Cloudflare Secure DNS",
        "Google Public DNS",
        "AdGuard DNS (Блокировка рекламы)"
    )

    val privateDnsChoices = dnsList.map { preset ->
        val hostname = dnsHostnameFor(preset)
        TunnelDnsChoice(
            key = "private:$preset",
            label = preset.substringBefore(" (").trim(),
            address = hostname ?: "Частный DNS выключен",
            selected = preset == actualDnsPreset.orEmpty(),
            systemDns = hostname == null,
            encrypted = hostname != null,
            server = { listOfNotNull(hostname) }
        )
    }
    TunnelDnsSheet(
        visible = showDnsDialog,
        choices = privateDnsChoices,
        title = "Маршрутизация DNS",
        sortPrefKey = "private_dns_sort_keys",
        checkWithoutNetFix = true,
        onClose = { showDnsDialog = false },
        onSelect = { picked ->
            val preset = dnsList.first { "private:$it" == picked.key }
            selectedDnsPreset = preset
            context.getSharedPreferences(
                context.packageName + "_preferences",
                android.content.Context.MODE_PRIVATE
            ).edit().putString("custom_dns_preset", preset).apply()
            showDnsDialog = false
            dnsInstructionPreset = preset
        }
    )

    val dnsInstructionHostname = dnsInstructionPreset?.let { dnsHostnameFor(it) }
    com.rupleide.netfix.ui.components.NfBottomSheet(
        visible = dnsInstructionPreset != null,
        onDismiss = { dnsInstructionPreset = null },
        icon = Icons.Default.Dns,
        title = if (dnsInstructionHostname != null) "Как включить DNS" else "Как отключить DNS",
        subtitle = dnsInstructionPreset?.substringBefore(" (")?.trim().orEmpty(),
        closeLabel = "Позже",
        maxHeightFraction = 0.9f
    ) { entry ->
        if (dnsInstructionHostname != null) {
            Row(
                modifier = entry(0)
                    .fillMaxWidth()
                    .nfTvFocus(corner = 14.dp, grow = 1.03f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF242424))
                    .border(1.dp, Nf.Line, RoundedCornerShape(14.dp))
                    .clickable {
                        copyDnsHostname(context, dnsInstructionHostname)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dnsInstructionHostname,
                    color = Nf.Text,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Копировать",
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
        val steps = if (dnsInstructionHostname != null) {
            listOf(
                "Нажмите «Открыть настройки», адрес скопируется сам.",
                "Найдите пункт «Частный DNS» (на некоторых телефонах «Приватный DNS»).",
                "Выберите «Имя хоста поставщика частного DNS».",
                "Вставьте адрес выше и нажмите «Сохранить»."
            )
        } else {
            listOf(
                "Нажмите «Открыть настройки».",
                "Найдите пункт «Частный DNS» (на некоторых телефонах «Приватный DNS»).",
                "Выберите «Отключено» и сохраните."
            )
        }
        Column(
            modifier = entry(1).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, text ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFF242424)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Nf.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = text,
                        color = Nf.TextSub,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    )
                }
            }
            if (dnsInstructionPreset?.startsWith("AstraCat") == true) {
                Text(
                    text = "Другие адреса AstraCat: обычный DNS 135.106.217.200 и 135.106.197.22, DoH https://dns.astracat.network/dns-query.",
                    color = Nf.TextSub,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Text(
                text = "Если откроются общие настройки сети: Настройки, Сеть и интернет, Дополнительно, Частный DNS.",
                color = Nf.TextOff,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        NfButton(
            text = "Открыть настройки",
            style = NfButtonStyle.Secondary,
            modifier = entry(2),
            onClick = {
                dnsInstructionHostname?.let { copyDnsHostname(context, it) }
                dnsInstructionPreset = null
                try {
                    context.startActivity(android.content.Intent("android.settings.PRIVATE_DNS_SETTINGS"))
                } catch (_: Exception) {
                    try {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
        )
    }

    com.rupleide.netfix.ui.components.CompatibilitySettingsSheets(compatibilityState)

        NetFixActionSheet(
        visible = showBypassModeDialog,
        onDismissRequest = { showBypassModeDialog = false },
        icon = R.drawable.ic_bolt,
        title = "Режим работы",
        subtitle = "Выберите, какие именно сервисы должен обходить NetFix.",
        options = listOf(
            NetFixSheetOption(
                label = "Telegram и YouTube",
                selected = bypassMode == "both",
                onClick = {
                    bypassMode = "both"
                    val edit = sharedPrefs.edit()
                    edit.putBoolean("wants_youtube_bypass", true)
                    edit.putBoolean("telegram_proxy_enabled_by_user", true)
                    edit.putBoolean("ui_show_youtube", true)
                    edit.putBoolean("ui_show_telegram", true)
                    edit.putBoolean("service_enabled", false).apply()
                    ServiceManager.stop(context)
                    val stopIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                        action = com.rupleide.netfix.data.STOP_ACTION
                    }
                    context.startService(stopIntent)
                    TgProxyController.stop()
                    showBypassModeDialog = false
                }
            ),
            NetFixSheetOption(
                label = "Только Telegram",
                selected = bypassMode == "telegram",
                onClick = {
                    bypassMode = "telegram"
                    val edit = sharedPrefs.edit()
                    edit.putBoolean("wants_youtube_bypass", false)
                    edit.putBoolean("telegram_proxy_enabled_by_user", true)
                    edit.putBoolean("ui_show_youtube", false)
                    edit.putBoolean("ui_show_telegram", true)
                    edit.putBoolean("service_enabled", false).apply()
                    ServiceManager.stop(context)
                    val stopIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                        action = com.rupleide.netfix.data.STOP_ACTION
                    }
                    context.startService(stopIntent)
                    TgProxyController.stop()
                    showBypassModeDialog = false
                }
            ),
            NetFixSheetOption(
                label = "Только YouTube",
                selected = bypassMode == "youtube",
                onClick = {
                    bypassMode = "youtube"
                    val edit = sharedPrefs.edit()
                    edit.putBoolean("wants_youtube_bypass", true)
                    edit.putBoolean("telegram_proxy_enabled_by_user", false)
                    edit.putBoolean("ui_show_youtube", true)
                    edit.putBoolean("ui_show_telegram", false)
                    edit.putBoolean("service_enabled", false).apply()
                    ServiceManager.stop(context)
                    val stopIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                        action = com.rupleide.netfix.data.STOP_ACTION
                    }
                    context.startService(stopIntent)
                    TgProxyController.stop()
                    showBypassModeDialog = false
                }
            )
        )
    )

    NetFixResetSheet(
        visible = showResetDialog,
        onDismissRequest = { showResetDialog = false },
        actions = listOf(
            ResetAction(
                label = "Сбросить всё",
                subtitle = "Полный сброс настроек с перезапуском"
            ) {
                com.rupleide.netfix.core.AppReset.resetEverything(context)
            },
            ResetAction(
                label = "Сбросить всё, кроме результатов тестов",
                subtitle = "Настройки удаляются, результаты тестов сохраняются"
            ) {
                com.rupleide.netfix.core.AppReset.resetSettingsKeepingResults(context, "из настроек")
            }
        )
    )

    val unloadWantsYt = sharedPrefs.getBoolean("wants_youtube_bypass", true)
    val unloadWantsTg = sharedPrefs.getBoolean("telegram_proxy_enabled_by_user", true)
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
            val edit = sharedPrefs.edit()
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
            val stopIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                action = com.rupleide.netfix.data.STOP_ACTION
            }
            context.startService(stopIntent)
            TgProxyController.stop()

            com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(context)

            scope.launch {
                kotlinx.coroutines.delay(800)
                val wantsYoutube = sharedPrefs.getBoolean("wants_youtube_bypass", true)
                val wantsTelegram = sharedPrefs.getBoolean("telegram_proxy_enabled_by_user", true)

                if (wantsYoutube) {
                    ServiceManager.start(context, Mode.VPN)
                }

                if (wantsTelegram) {
                    val openTg = sharedPrefs.getBoolean("open_tg_on_connect", true)
                    if (wantsYoutube) {
                        TgProxyController.startAsync(
                            context = context,
                            onSuccess = {
                                val port = TgProxyController.getPort(context)
                                val secret = TgProxyController.getOrGenerateSecret(context)
                                val url = TgProxyController.getTgProxyUrl(
                                    TgProxyController.DEFAULT_BIND_IP,
                                    port,
                                    secret
                                )
                                val alreadyConfigured = sharedPrefs.getBoolean("tg_proxy_configured", false)
                                if (openTg && !alreadyConfigured) {
                                    val tgIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(tgIntent)
                                        sharedPrefs.edit().putBoolean("tg_proxy_configured", true).apply()
                                    } catch (_: Exception) {}
                                }
                            },
                            onError = {}
                        )
                    } else {
                        val startIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
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
}
