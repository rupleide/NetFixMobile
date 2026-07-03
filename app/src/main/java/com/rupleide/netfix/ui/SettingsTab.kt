package com.rupleide.netfix.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.rupleide.netfix.ui.components.NetFixSwitch
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
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.appStatus
import com.rupleide.netfix.data.performanceModeGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

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

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHighlighted = enabled && (isPressed || isHovered || isFocused)

    val rowBgColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0x1AFFFFFF) else Color.Transparent,
        animationSpec = tween(150),
        label = "rowBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBgColor, shape = RoundedCornerShape(8.dp))
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onCheckedChange(!checked) }
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color(0xFFF4F4F5) else Color(0xFF555555),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (enabled) Color(0xFFA1A1AA) else Color(0xFF444444),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        NetFixSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun SettingsClickRow(
    title: String,
    subtitle: String? = null,
    actionContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHighlighted = isPressed || isHovered || isFocused

    val rowBgColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0x1AFFFFFF) else Color.Transparent,
        animationSpec = tween(150),
        label = "rowBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBgColor, shape = RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFF4F4F5),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
            }
        }
        if (actionContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            actionContent()
        }
    }
}

@Composable
fun SettingsActionButton(
    label: String,
    textColor: Color = Color(0xFFF4F4F5),
    containerColor: Color = Color(0xFF242426),
    borderColor: Color = Color(0x1AFFFFFF),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHighlighted = isPressed || isHovered || isFocused

    val currentBgColor by animateColorAsState(
        targetValue = if (enabled && isHighlighted) Color(0xFF38383A) else containerColor,
        animationSpec = tween(150),
        label = "actionBg"
    )
    val currentBorderColor by animateColorAsState(
        targetValue = if (enabled && isHighlighted) Color(0xFFFFFFFF) else borderColor,
        animationSpec = tween(150),
        label = "actionBorder"
    )
    val currentTextColor by animateColorAsState(
        targetValue = if (enabled && isHighlighted) Color.White else textColor,
        animationSpec = tween(150),
        label = "actionText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(currentBgColor)
            .border(1.dp, currentBorderColor, RoundedCornerShape(12.dp))
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = currentTextColor,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
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
        com.rupleide.netfix.service.WatchdogReceiver.cancelWatchdogAlarm(context)
        
        context.stopService(android.content.Intent(context, com.rupleide.netfix.core.dpibypass.ByeDpiVpnService::class.java))
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
    var performanceMode by remember {
        mutableStateOf(
            sharedPrefs.getBoolean("performance_mode", false)
        )
    }
    var showBypassModeDialog by remember { mutableStateOf(false) }
    var showUnloadDialog by remember { mutableStateOf(false) }
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
    var showDnsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
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
        privateDnsMode = android.provider.Settings.Global.getString(
            context.contentResolver,
            "private_dns_mode"
        ) ?: "off"
    }

    LaunchedEffect(Unit) {
        repeat(8) { index ->
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            delay(100L + index * 50L)
        }
    }

    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 86.dp

    var isBatteryIgnored by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        while (true) {
            isBatteryIgnored = VendorAutoStartHelper.isBatteryOptimizationIgnored(context)
            delay(2000)
        }
    }

    var showExcludedAppsDialog by remember { mutableStateOf(false) }
    var selectedApps by remember {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("selected_apps", null)
        val initial = if (saved == null) {
            val defaultPkgs = setOf(
                "com.google.android.youtube",
                "com.google.android.youtube.tv",
                "com.liskovsoft.videomanager.v2",
                "com.liskovsoft.smarttubetv.beta",
                "com.teamsmart.videomanager.tv",
                "app.revanced.android.youtube",
                "com.google.android.apps.youtube.music",
                "com.google.android.apps.youtube.kids",
                "org.schabi.newpipe",
                "org.schabi.newpipe.legacy",
                "com.kapp.youtube",
                "com.bg.vanced",
                "com.libretube",
                "com.liskovsoft.smarttubetv"
            )
            prefs.edit().putStringSet("selected_apps", defaultPkgs).apply()
            defaultPkgs
        } else {
            saved
        }
        mutableStateOf(initial)
    }
    var installedApps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
                .filter { pkg ->
                    val flags = pkg.applicationInfo?.flags ?: 0
                    (flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .sortedBy { pkg ->
                    pkg.applicationInfo?.let {
                        context.packageManager.getApplicationLabel(it).toString()
                    } ?: pkg.packageName
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (isLandscape) 820.dp else 500.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Система",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                SettingsSwitchRow(
                    title = "Автозапуск",
                    subtitle = "Запуск при загрузке устройства",
                    checked = autostart,
                    onCheckedChange = { checked ->
                        autostart = checked
                        TgProxyController.setAutostartEnabled(context, checked)
                    },
                    modifier = Modifier.focusRequester(focusRequester)
                )

                HorizontalDivider(color = Color(0xFF2A2A2A))

                SettingsSwitchRow(
                    title = "Автоподключение",
                    subtitle = "Подключаться при открытии приложения",
                    checked = autoConnect,
                    onCheckedChange = { checked ->
                        autoConnect = checked
                        context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("auto_connect_on_start", checked)
                            .apply()
                    }
                )

                HorizontalDivider(color = Color(0xFF2A2A2A))

                SettingsSwitchRow(
                    title = "Открыть Telegram при подключении",
                    subtitle = "Применять настройки прокси автоматически",
                    checked = openTgOnConnect,
                    onCheckedChange = { checked ->
                        openTgOnConnect = checked
                        context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("open_tg_on_connect", checked)
                            .apply()
                    }
                )

                HorizontalDivider(color = Color(0xFF2A2A2A))

                SettingsSwitchRow(
                    title = "Автообновление",
                    subtitle = "Автоматическая проверка обновлений в фоне",
                    checked = autoUpdate,
                    onCheckedChange = { checked ->
                        autoUpdate = checked
                        UpdateManager.setAutoUpdateEnabled(context, checked)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Интерфейс",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                SettingsSwitchRow(
                    title = "Режим максимальной производительности",
                    subtitle = "Отключает анимации и визуальные эффекты",
                    checked = performanceMode,
                    onCheckedChange = { checked ->
                        performanceMode = checked
                        performanceModeGlobal = checked
                        context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("performance_mode", checked)
                            .apply()
                    }
                )

                HorizontalDivider(color = Color(0xFF2A2A2A))

                SettingsSwitchRow(
                    title = "Показывать QR-коды",
                    subtitle = if (isTv) "Включено автоматически для Смарт ТВ" else "Отображать QR-коды вместе с ссылками",
                    checked = qrCodesEnabled,
                    onCheckedChange = { checked ->
                        qrCodesEnabled = checked
                        sharedPrefs.edit().putBoolean("qr_codes_enabled", checked).apply()
                    },
                    enabled = !isTv
                )

                HorizontalDivider(color = Color(0xFF2A2A2A))

                SettingsClickRow(
                    title = "Сбросить онбординг",
                    subtitle = "Показать приветственный экран при перезапуске",
                    actionContent = {
                        SettingsActionButton(
                            label = "Сбросить",
                            onClick = {
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
                        )
                    },
                    onClick = {
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
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Батарея и разрешения",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                SettingsClickRow(
                    title = "Оптимизация батареи",
                    subtitle = "Разрешить неограниченную фоновую работу",
                    actionContent = {
                        if (isBatteryIgnored) {
                            SettingsActionButton(
                                label = "Разрешено",
                                textColor = Color(0xFF22C55E),
                                containerColor = Color(0xFF1B2E1E),
                                borderColor = Color(0x3322C55E),
                                enabled = false,
                                onClick = {}
                            )
                        } else {
                            SettingsActionButton(
                                label = "Разрешить",
                                onClick = { VendorAutoStartHelper.requestIgnoreBatteryOptimization(context) }
                            )
                        }
                    },
                    onClick = {
                        if (!isBatteryIgnored) {
                            VendorAutoStartHelper.requestIgnoreBatteryOptimization(context)
                        }
                    }
                )

                val vendorIntent = remember { VendorAutoStartHelper.getVendorAutoStartIntent(context) }
                if (vendorIntent != null) {
                    HorizontalDivider(color = Color(0xFF2A2A2A))

                    SettingsClickRow(
                        title = "Автозапуск (вендор)",
                        subtitle = "Открыть системные настройки для фоновой работы",
                        actionContent = {
                            SettingsActionButton(
                                label = "Настроить",
                                onClick = { VendorAutoStartHelper.openVendorAutoStartSettings(context) }
                            )
                        },
                        onClick = { VendorAutoStartHelper.openVendorAutoStartSettings(context) }
                    )
                }

                HorizontalDivider(color = Color(0xFF2A2A2A))

                SettingsClickRow(
                    title = "Запустить в фоне и выйти",
                    subtitle = "Запустит выбранный обход и полностью закроет приложение для экономии памяти (без Watchdog)",
                    actionContent = {
                        SettingsActionButton(
                            label = "Запуск",
                            onClick = { showUnloadDialog = true }
                        )
                    },
                    onClick = { showUnloadDialog = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AppBlocking,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Обход приложений",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                SettingsClickRow(
                    title = "Включить VPN для...",
                    subtitle = if (selectedApps.isNotEmpty())
                        "Выбрано: ${selectedApps.size} прил. (только они пойдут через VPN)"
                    else
                        "Выберите приложения для обхода блокировок",
                    actionContent = {
                        SettingsActionButton(
                            label = "Настроить",
                            onClick = { showExcludedAppsDialog = true }
                        )
                    },
                    onClick = { showExcludedAppsDialog = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bolt),
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Режим работы",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Выберите, какие именно сервисы должен обходить NetFix.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val bypassModeText = when (bypassMode) {
                    "both" -> "Telegram и YouTube"
                    "telegram" -> "Только Telegram"
                    "youtube" -> "Только YouTube"
                    else -> "Telegram и YouTube"
                }

                SettingsClickRow(
                    title = "Обходить сервисы",
                    subtitle = "Выбрано: $bypassModeText",
                    actionContent = {
                        SettingsActionButton(
                            label = "Настроить",
                            onClick = { showBypassModeDialog = true }
                        )
                    },
                    onClick = { showBypassModeDialog = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Маршрутизация DNS",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Позволяет обходить блокировки сайтов (ChatGPT) и мобильных игр (Brawl Stars, Null's). Рекомендуется использовать Xbox DNS.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SettingsClickRow(
                    title = "DNS-серверы",
                    subtitle = "Выбрано: $selectedDnsPreset",
                    actionContent = {
                        SettingsActionButton(
                            label = "Настроить",
                            onClick = { showDnsDialog = true }
                        )
                    },
                    onClick = { showDnsDialog = true }
                )

                val isPrivateDnsBlocking = privateDnsMode == "opportunistic" || privateDnsMode == "hostname"
                val isCustomDnsSelected = selectedDnsPreset != "Стандартный (Отключено)"
                if (isPrivateDnsBlocking && isCustomDnsSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2D1F0A))
                            .border(1.dp, Color(0x55F97316), RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                try {
                                    val intent = android.content.Intent("android.settings.PRIVATE_DNS_SETTINGS")
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                        )
                        Column {
                            Text(
                                text = "Приватный DNS Android включён",
                                color = Color(0xFFF97316),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Он переопределяет DNS VPN. Нажмите чтобы открыть настройки и выбрать «Отключить».",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Выключение",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                SettingsClickRow(
                    title = "Полное закрытие",
                    subtitle = "Остановка всех фоновых служб и выгрузка приложения",
                    actionContent = {
                        SettingsActionButton(
                            label = "Выйти",
                            onClick = { performFullExit() }
                        )
                    },
                    onClick = { performFullExit() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Сброс",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                SettingsClickRow(
                    title = "Сброс приложения",
                    subtitle = "Полная очистка или сброс настроек с сохранением тестов",
                    actionContent = {
                        SettingsActionButton(
                            label = "Сбросить",
                            onClick = { showResetDialog = true }
                        )
                    },
                    modifier = Modifier.focusProperties { down = navBarFocusRequester },
                    onClick = { showResetDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(navOverlayReserve))
        }
    }

    if (showExcludedAppsDialog) {
        Dialog(onDismissRequest = { showExcludedAppsDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Включить VPN для...",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Только выбранные приложения будут направляться в обход через VPN. Остальные пойдут напрямую.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Загрузка...",
                            color = Color(0xFFA1A1AA),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    var searchQuery by remember { mutableStateOf("") }
                    val initialSelectedApps = remember(showExcludedAppsDialog) { selectedApps.toSet() }
                    val filteredApps = remember(searchQuery, installedApps) {
                        val baseList = if (searchQuery.isBlank()) installedApps
                        else installedApps.filter { pkg ->
                            val name = pkg.applicationInfo?.let {
                                context.packageManager.getApplicationLabel(it).toString()
                            } ?: pkg.packageName
                            name.contains(searchQuery, ignoreCase = true) ||
                                pkg.packageName.contains(searchQuery, ignoreCase = true)
                        }
                        baseList.sortedWith(compareByDescending<PackageInfo> { pkg ->
                            initialSelectedApps.contains(pkg.packageName)
                        }.thenBy { pkg ->
                            pkg.applicationInfo?.let {
                                context.packageManager.getApplicationLabel(it).toString()
                            } ?: pkg.packageName
                        })
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Поиск приложений...",
                                color = Color(0xFF555555),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF333333),
                            cursorColor = Color(0xFF3B82F6),
                            focusedTextColor = Color(0xFFF4F4F5),
                            unfocusedTextColor = Color(0xFFF4F4F5)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(filteredApps) { packageInfo ->
                            val pkg = packageInfo.packageName
                            val appName = packageInfo.applicationInfo?.let {
                                context.packageManager.getApplicationLabel(it).toString()
                            } ?: pkg
                            val isChecked = selectedApps.contains(pkg)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val newSet = selectedApps.toMutableSet()
                                        if (isChecked) newSet.remove(pkg) else newSet.add(pkg)
                                        selectedApps = newSet
                                        context.getSharedPreferences(
                                            context.packageName + "_preferences",
                                            android.content.Context.MODE_PRIVATE
                                        ).edit().putStringSet("selected_apps", newSet).apply()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = appName,
                                        color = Color(0xFFF4F4F5),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = pkg,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF3B82F6),
                                        uncheckedColor = Color(0xFF3A3A3A),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFF242424))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3B82F6))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showExcludedAppsDialog = false }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Готово",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showDnsDialog) {
        Dialog(onDismissRequest = { showDnsDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Маршрутизация DNS",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Выберите пресет DNS для обхода ограничений",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                val dnsList = listOf(
                    "Стандартный (Отключено)",
                    "Cloudflare Secure DNS",
                    "Google Public DNS",
                    "AdGuard DNS (Блокировка рекламы)",
                    "Xbox DNS (xbox-dns.ru / ChatGPT / Brawl)",
                    "Supercell Xbox DNS (supercell.xbox-dns.ru)",
                    "NullsProxy DNS (dns.nullsproxy.com)",
                    "Comss.one DNS (dns.comss.one)",
                    "Geohide DNS (dns.geohide.ru)"
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(dnsList) { preset ->
                        val isSelected = selectedDnsPreset == preset
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val isHighlighted = isPressed || isHovered || isFocused

                        val itemBgColor by animateColorAsState(
                            targetValue = if (isHighlighted) Color(0x1AFFFFFF) else Color.Transparent,
                            animationSpec = tween(150),
                            label = "dnsItemBg"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemBgColor)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    selectedDnsPreset = preset
                                    context.getSharedPreferences(
                                        context.packageName + "_preferences",
                                        android.content.Context.MODE_PRIVATE
                                    ).edit().putString("custom_dns_preset", preset).apply()
                                    showDnsDialog = false

                                    val hostname = when (preset) {
                                        "Cloudflare Secure DNS" -> "one.one.one.one"
                                        "Google Public DNS" -> "dns.google"
                                        "AdGuard DNS (Блокировка рекламы)" -> "dns.adguard-dns.com"
                                        "Xbox DNS (xbox-dns.ru / ChatGPT / Brawl)" -> "dot.xbox-dns.ru"
                                        "Supercell Xbox DNS (supercell.xbox-dns.ru)" -> "dot.xbox-dns.ru"
                                        "NullsProxy DNS (dns.nullsproxy.com)" -> "dns.nullsproxy.com"
                                        "Comss.one DNS (dns.comss.one)" -> "dns.comss.one"
                                        "Geohide DNS (dns.geohide.ru)" -> "dns.geohide.ru"
                                        else -> null
                                    }

                                    if (hostname != null) {
                                        try {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Private DNS", hostname)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "Адрес скопирован: $hostname", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (_: Exception) {}
                                    }

                                    try {
                                        val intent = android.content.Intent("android.settings.PRIVATE_DNS_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = preset,
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF242426))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDnsDialog = false }
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

    if (showBypassModeDialog) {
        Dialog(onDismissRequest = { showBypassModeDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Режим работы",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Выберите, какие сервисы обходить",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                val modes = listOf(
                    "both" to "Telegram и YouTube",
                    "telegram" to "Только Telegram",
                    "youtube" to "Только YouTube"
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(modes) { item ->
                        val modeKey = item.first
                        val modeLabel = item.second
                        val isSelected = bypassMode == modeKey

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val isHighlighted = isPressed || isHovered || isFocused

                        val itemBgColor by animateColorAsState(
                            targetValue = if (isHighlighted) Color(0x1AFFFFFF) else Color.Transparent,
                            animationSpec = tween(150),
                            label = "modeItemBg"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemBgColor)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    bypassMode = modeKey
                                    val edit = sharedPrefs.edit()
                                    when (modeKey) {
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
                                    edit.putBoolean("service_enabled", false).apply()
                                    ServiceManager.stop(context)
                                    val intent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                                        action = com.rupleide.netfix.data.STOP_ACTION
                                    }
                                    context.startService(intent)
                                    TgProxyController.stop()
                                    showBypassModeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = modeLabel,
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF4F4F5),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF242426))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showBypassModeDialog = false }
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

    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Сброс приложения",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Выберите вариант сброса на выбор:",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val resetAllInteraction = remember { MutableInteractionSource() }
                val resetAllPressed by resetAllInteraction.collectIsPressedAsState()
                val resetAllHovered by resetAllInteraction.collectIsHoveredAsState()
                val resetAllFocused by resetAllInteraction.collectIsFocusedAsState()
                val resetAllHighlighted = resetAllPressed || resetAllHovered || resetAllFocused

                val resetAllBgColor by animateColorAsState(
                    targetValue = if (resetAllHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                    animationSpec = tween(150),
                    label = ""
                )
                val resetAllBorderColor by animateColorAsState(
                    targetValue = if (resetAllHighlighted) Color.White else Color(0x1AFFFFFF),
                    animationSpec = tween(150),
                    label = ""
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(resetAllBgColor)
                        .border(1.dp, resetAllBorderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = resetAllInteraction,
                            indication = null
                        ) {
                            com.rupleide.netfix.core.dpibypass.ServiceManager.stop(context)
                            val stopIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                                action = com.rupleide.netfix.data.STOP_ACTION
                            }
                            context.startService(stopIntent)
                            com.rupleide.netfix.core.tgproxy.TgProxyController.stop()
                            com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(context)
                            com.rupleide.netfix.service.WatchdogReceiver.cancelWatchdogAlarm(context)

                            val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                            prefs.edit().clear().commit()
                            val hintPrefs = context.getSharedPreferences("netfix_hints", android.content.Context.MODE_PRIVATE)
                            hintPrefs.edit().clear().commit()
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            java.lang.System.exit(0)
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сбросить всё",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }

                val resetKeepTestsInteraction = remember { MutableInteractionSource() }
                val resetKeepTestsPressed by resetKeepTestsInteraction.collectIsPressedAsState()
                val resetKeepTestsHovered by resetKeepTestsInteraction.collectIsHoveredAsState()
                val resetKeepTestsFocused by resetKeepTestsInteraction.collectIsFocusedAsState()
                val resetKeepTestsHighlighted = resetKeepTestsPressed || resetKeepTestsHovered || resetKeepTestsFocused

                val resetKeepTestsBgColor by animateColorAsState(
                    targetValue = if (resetKeepTestsHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                    animationSpec = tween(150),
                    label = ""
                )
                val resetKeepTestsBorderColor by animateColorAsState(
                    targetValue = if (resetKeepTestsHighlighted) Color.White else Color(0x1AFFFFFF),
                    animationSpec = tween(150),
                    label = ""
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(resetKeepTestsBgColor)
                        .border(1.dp, resetKeepTestsBorderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = resetKeepTestsInteraction,
                            indication = null
                        ) {
                            com.rupleide.netfix.core.dpibypass.ServiceManager.stop(context)
                            val stopIntent = android.content.Intent(context, com.rupleide.netfix.core.tgproxy.TgProxyService::class.java).apply {
                                action = com.rupleide.netfix.data.STOP_ACTION
                            }
                            context.startService(stopIntent)
                            com.rupleide.netfix.core.tgproxy.TgProxyController.stop()
                            com.rupleide.netfix.service.WatchdogWorker.cancelPeriodicWork(context)
                            com.rupleide.netfix.service.WatchdogReceiver.cancelWatchdogAlarm(context)

                            val prefs = context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                            val isSetupComplete = prefs.getBoolean("wizard_is_setup_complete", false)
                            val isSetupCompleteOld = prefs.getBoolean("is_setup_complete", false)
                            val currentTestIndex = prefs.getInt("wizard_current_test_index", 0)
                            val currentTestIndexOld = prefs.getInt("current_test_index", 0)
                            val selectedAppsSet = prefs.getStringSet("selected_apps", null)

                            prefs.edit().clear().commit()

                            val hintPrefs = context.getSharedPreferences("netfix_hints", android.content.Context.MODE_PRIVATE)
                            hintPrefs.edit().clear().commit()

                            val edit = prefs.edit()
                            edit.putBoolean("wizard_is_setup_complete", isSetupComplete)
                            edit.putBoolean("is_setup_complete", isSetupCompleteOld)
                            edit.putInt("wizard_current_test_index", currentTestIndex)
                            edit.putInt("current_test_index", currentTestIndexOld)
                            if (selectedAppsSet != null) {
                                edit.putStringSet("selected_apps", selectedAppsSet)
                            }
                            edit.commit()

                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            java.lang.System.exit(0)
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сбросить всё, кроме результатов тестов",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val cancelInteraction = remember { MutableInteractionSource() }
                val cancelPressed by cancelInteraction.collectIsPressedAsState()
                val cancelHovered by cancelInteraction.collectIsHoveredAsState()
                val cancelFocused by cancelInteraction.collectIsFocusedAsState()
                val cancelHighlighted = cancelPressed || cancelHovered || cancelFocused

                val cancelBgColor by animateColorAsState(
                    targetValue = if (cancelHighlighted) Color(0xFF38383A) else Color(0xFF242426),
                    animationSpec = tween(150),
                    label = ""
                )
                val cancelBorderColor by animateColorAsState(
                    targetValue = if (cancelHighlighted) Color.White else Color(0x1AFFFFFF),
                    animationSpec = tween(150),
                    label = ""
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cancelBgColor)
                        .border(1.dp, cancelBorderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = cancelInteraction,
                            indication = null
                        ) { showResetDialog = false }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Отмена",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
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

                val wantsYt = sharedPrefs.getBoolean("wants_youtube_bypass", true)
                val wantsTg = sharedPrefs.getBoolean("telegram_proxy_enabled_by_user", true)
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { (key, label) ->
                        val isSelected = currentMode == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
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
                                    com.rupleide.netfix.service.WatchdogReceiver.cancelWatchdogAlarm(context)

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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF242426))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
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
}

