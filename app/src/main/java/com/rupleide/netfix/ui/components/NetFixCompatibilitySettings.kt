package com.rupleide.netfix.ui.components

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.R
import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.data.isActionSheetVisibleGlobal
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOSTNAME_RESOLVE_DELAY_MS = 700L

class CompatibilitySettingsState(private val prefs: SharedPreferences) {
    var behaviorVersion by mutableStateOf(CompatibilityManager.behaviorVersion(prefs))
    var customized by mutableStateOf(CompatibilityManager.isCustomized(prefs))
    var dnsMode by mutableStateOf(CompatibilityManager.tunnelDnsMode(prefs))
    var customDns by mutableStateOf(CompatibilityManager.customTunnelDns(prefs))
    var customDnsName by mutableStateOf(CompatibilityManager.customTunnelDnsName(prefs))
    var resolvingDns by mutableStateOf<String?>(null)
    val features = mutableStateMapOf<CompatFeature, Boolean>().apply {
        CompatFeature.entries.forEach { put(it, CompatibilityManager.featureEnabled(prefs, it)) }
    }
    var advancedExpanded by mutableStateOf(customized)
    var showVersionSheet by mutableStateOf(false)
    var showDnsSheet by mutableStateOf(false)

    val anySheetVisible: Boolean
        get() = showVersionSheet || showDnsSheet

    fun applyBehaviorVersion(version: String) {
        CompatibilityManager.setBehaviorVersion(prefs, version)
        reload()
    }

    fun applyDnsMode(mode: TunnelDnsMode) {
        CompatibilityManager.setTunnelDnsMode(prefs, mode)
        reload()
    }

    fun applyCustomDns(raw: String, name: String? = null): Boolean {
        val accepted = CompatibilityManager.setCustomTunnelDns(prefs, raw, name)
        if (accepted) reload()
        return accepted
    }

    fun applyFeature(feature: CompatFeature, enabled: Boolean) {
        CompatibilityManager.setFeature(prefs, feature, enabled)
        reload()
    }

    private fun reload() {
        behaviorVersion = CompatibilityManager.behaviorVersion(prefs)
        customized = CompatibilityManager.isCustomized(prefs)
        dnsMode = CompatibilityManager.tunnelDnsMode(prefs)
        customDns = CompatibilityManager.customTunnelDns(prefs)
        customDnsName = CompatibilityManager.customTunnelDnsName(prefs)
        CompatFeature.entries.forEach { features[it] = CompatibilityManager.featureEnabled(prefs, it) }
    }
}

@Composable
fun rememberCompatibilitySettingsState(prefs: SharedPreferences): CompatibilitySettingsState =
    remember(prefs) { CompatibilitySettingsState(prefs) }

private fun behaviorLabel(version: String): String =
    if (version == CompatibilityManager.latestVersion) "Всё новое ($version)" else "Как в $version"

private fun behaviorShortLabel(version: String, customized: Boolean): String = when {
    customized -> "Своя"
    version == CompatibilityManager.latestVersion -> "Новая"
    else -> version
}

private fun featureTitle(feature: CompatFeature): String = when (feature) {
    CompatFeature.FreeProxyPort -> "Обход занятого порта"
    CompatFeature.EthernetScan -> "Подбор по кабелю"
    CompatFeature.DiscordEchCheck -> "Точная проверка Discord"
    CompatFeature.TgProxyWatchdog -> "Чинить зависший Telegram"
    CompatFeature.StreamProbe -> "Проверка видео при подборе"
    CompatFeature.SmartTubeHosts -> "Проверка для SmartTube"
    CompatFeature.DiscordCombinedStrategy -> "Способ для чата и голоса Discord"
    CompatFeature.ScanQuickFallback -> "Запасной результат подбора"
    CompatFeature.SystemTunnelDns -> "Системный DNS в туннеле"
    CompatFeature.DiscordOffOnTv -> "Без Discord на ТВ"
    CompatFeature.CloudflareUnfreeze -> "Обход заморозки Cloudflare"
    CompatFeature.GeminiWithGoogleApp -> "Gemini вместе с приложением Google"
    CompatFeature.SecureDns -> "Защищённый DNS"
    CompatFeature.AiAppsInBypass -> "ChatGPT и Gemini в обходе"
    CompatFeature.BatterySaver -> "Экономия батареи"
    CompatFeature.YoutubeVideoStrategies -> "Новые способы для видео YouTube"
    CompatFeature.VideoFastFail -> "Быстрый старт видео YouTube"
}

private fun featureSubtitle(feature: CompatFeature): String = when (feature) {
    CompatFeature.FreeProxyPort -> "Если порт занят другой программой, NetFix возьмёт свободный"
    CompatFeature.EthernetScan -> "Для ТВ и приставок с интернетом по кабелю"
    CompatFeature.DiscordEchCheck -> "Подбор проверяет ещё один адрес Discord, когда «Проверять Discord» включено"
    CompatFeature.TgProxyWatchdog -> "Если Telegram долго висит на «Подключение…», прокси перезапустится сам"
    CompatFeature.StreamProbe -> "Подбор загружает кусок настоящего видео и выбирает самый быстрый способ"
    CompatFeature.SmartTubeHosts -> "Если стоит SmartTube, подбор проверяет и его серверы"
    CompatFeature.DiscordCombinedStrategy -> "Ещё один способ в подборе, с которым работают и чат, и голос"
    CompatFeature.ScanQuickFallback -> "Если полная проверка не прошла, берётся лучший способ из быстрой"
    CompatFeature.SystemTunnelDns -> "DNS вашей сети вместо 1.1.1.1"
    CompatFeature.DiscordOffOnTv -> "На ТВ «Проверять Discord» по умолчанию выключено"
    CompatFeature.CloudflareUnfreeze -> "Если сеть обрывает Discord, ChatGPT и другие сайты, NetFix найдёт рабочий адрес"
    CompatFeature.GeminiWithGoogleApp -> "Если Gemini в списке, через NetFix пойдёт и приложение Google"
    CompatFeature.SecureDns -> "Оператор не подменит адреса сайтов, а ChatGPT и Gemini откроются с любым DNS"
    CompatFeature.AiAppsInBypass -> "ChatGPT и Gemini сразу в списке NetFix, убрать можно в «Обходе приложений»"
    CompatFeature.BatterySaver -> "При выключенном экране прокси Telegram не держит лишние соединения"
    CompatFeature.YoutubeVideoStrategies -> "Подбор пробует способы, которые открывают серверы видео YouTube"
    CompatFeature.VideoFastFail -> "YouTube не ждёт заблокированный сервер видео, а зависшее видео перезапускается само"
}

private fun featureIcon(feature: CompatFeature) = when (feature) {
    CompatFeature.FreeProxyPort -> Icons.Default.Hub
    CompatFeature.EthernetScan -> Icons.Default.Cable
    CompatFeature.DiscordEchCheck -> Icons.Default.Forum
    CompatFeature.TgProxyWatchdog -> Icons.Default.Sync
    CompatFeature.StreamProbe -> Icons.Default.PlayCircle
    CompatFeature.SmartTubeHosts -> Icons.Default.Tv
    CompatFeature.DiscordCombinedStrategy -> Icons.Default.Headset
    CompatFeature.ScanQuickFallback -> Icons.Default.SettingsBackupRestore
    CompatFeature.SystemTunnelDns -> Icons.Default.Dns
    CompatFeature.DiscordOffOnTv -> Icons.Default.Tv
    CompatFeature.CloudflareUnfreeze -> Icons.Default.Cloud
    CompatFeature.GeminiWithGoogleApp -> Icons.Default.AutoAwesome
    CompatFeature.SecureDns -> Icons.Default.Dns
    CompatFeature.AiAppsInBypass -> Icons.Default.AutoAwesome
    CompatFeature.BatterySaver -> Icons.Default.BatteryChargingFull
    CompatFeature.YoutubeVideoStrategies -> Icons.Default.AutoAwesome
    CompatFeature.VideoFastFail -> Icons.Default.PlayCircle
}

private val featuresWithOwnControl = setOf(CompatFeature.SystemTunnelDns, CompatFeature.SecureDns)

private fun dnsModeLabel(mode: TunnelDnsMode, customName: String?): String = when (mode) {
    TunnelDnsMode.System -> "Системный"
    TunnelDnsMode.Cloudflare -> "1.1.1.1"
    TunnelDnsMode.Custom -> customName ?: "Свой адрес"
}

private val tunnelDnsPresets = com.rupleide.netfix.core.dnscheck.TunnelDnsCatalog.presets

private const val TUNNEL_DNS_SYSTEM = com.rupleide.netfix.core.dnscheck.TunnelDnsCatalog.SYSTEM
private const val TUNNEL_DNS_CLOUDFLARE = com.rupleide.netfix.core.dnscheck.TunnelDnsCatalog.CLOUDFLARE
private const val TUNNEL_DNS_CUSTOM = com.rupleide.netfix.core.dnscheck.TunnelDnsCatalog.CUSTOM

private fun restartBypassForDns(context: android.content.Context) {
    com.rupleide.netfix.core.dpibypass.ServiceManager.restart(context.applicationContext, com.rupleide.netfix.data.Mode.VPN)
}

private val dnsHostname = Regex("""^(?=.{4,253}$)([a-zA-Z0-9-]{1,63}\.)+[a-zA-Z]{2,63}$""")

private suspend fun resolveDnsAddress(address: String): List<String> = withContext(Dispatchers.IO) {
    val trimmed = address.trim()
    if (CompatibilityManager.isIpLiteral(trimmed)) return@withContext listOf(trimmed)
    if (!dnsHostname.matches(trimmed)) return@withContext emptyList()
    try {
        java.net.InetAddress.getAllByName(trimmed)
            .filterIsInstance<java.net.Inet4Address>()
            .mapNotNull { it.hostAddress }
            .distinct()
            .take(2)
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun VersionGroupLabel(text: String) {
    Text(
        text = text,
        color = Nf.TextSub,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
fun ColumnScope.CompatibilitySettingsRows(
    state: CompatibilitySettingsState,
    extraAdvancedRows: @Composable ColumnScope.() -> Unit = {}
) {
    NfRow(
        title = "Работа сети",
        newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.COMPAT,
        subtitle = if (state.customized) {
            "Свой набор в тонкой настройке. Выберите версию, чтобы вернуть как было"
        } else {
            "Если после обновления что-то перестало работать, выберите прошлую версию"
        },
        icon = Icons.Default.History,
        onClick = { state.showVersionSheet = true },
        trailing = { NfValueTrailing(behaviorShortLabel(state.behaviorVersion, state.customized)) }
    )
    NfDivider()
    val chevronRotation by animateFloatAsState(
        targetValue = if (state.advancedExpanded) 90f else 0f,
        animationSpec = tween(220),
        label = "compatChevron"
    )
    NfRow(
        title = "Тонкая настройка",
        newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.TELEGRAM,
        subtitle = "Отдельные функции и обход через root. Обычно трогать не нужно",
        icon = Icons.Default.Tune,
        onClick = { state.advancedExpanded = !state.advancedExpanded },
        trailing = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Nf.TextSub,
                modifier = Modifier.size(18.dp).rotate(chevronRotation)
            )
        }
    )
    AnimatedVisibility(
        visible = state.advancedExpanded,
        enter = expandVertically(tween(260)) + fadeIn(tween(220)),
        exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
    ) {
        Column {
            CompatibilityAdvancedRows(state)
            extraAdvancedRows()
        }
    }
    Text(
        text = "Не помогло? Нажмите «?» во вкладке «Информация», помощник проверит и подскажет.",
        color = Nf.TextSub,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp)
    )
}

@Composable
private fun ColumnScope.CompatibilityAdvancedRows(state: CompatibilitySettingsState) {
    NfDivider()
    CompatFeature.versions.forEach { version ->
        VersionGroupLabel("Новое в $version")
        CompatFeature.inVersion(version).filterNot { it in featuresWithOwnControl }.forEach { feature ->
            NfSwitchRow(
                title = featureTitle(feature),
                subtitle = featureSubtitle(feature),
                icon = featureIcon(feature),
                checked = state.features[feature] == true,
                onCheckedChange = { state.applyFeature(feature, it) }
            )
            NfDivider()
        }
    }
}

@Composable
fun ColumnScope.TunnelDnsRows(state: CompatibilitySettingsState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    NfSwitchRow(
        title = featureTitle(CompatFeature.SecureDns),
        subtitle = featureSubtitle(CompatFeature.SecureDns),
        icon = featureIcon(CompatFeature.SecureDns),
        newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.SECURE_DNS,
        checked = state.features[CompatFeature.SecureDns] == true,
        onCheckedChange = {
            state.applyFeature(CompatFeature.SecureDns, it)
            restartBypassForDns(context)
        }
    )
    NfDivider()
    NfRow(
        title = "DNS внутри туннеля",
        newId = com.rupleide.netfix.core.whatsnew.WhatsNewIds.TUNNEL_DNS,
        subtitle = "Только когда NetFix включён",
        icon = Icons.Default.Dns,
        onClick = { state.showDnsSheet = true },
        trailing = { NfValueTrailing(dnsModeLabel(state.dnsMode, state.customDnsName)) }
    )
    if (state.dnsMode == TunnelDnsMode.Custom) {
        var draft by remember(state.customDnsName) {
            mutableStateOf(state.customDnsName?.let { name -> tunnelDnsPresets.firstOrNull { it.name == name }?.address ?: name } ?: state.customDns)
        }
        var resolveFailed by remember { mutableStateOf(false) }
        var resolvedHost by remember { mutableStateOf<String?>(null) }
        val isHostname = dnsHostname.matches(draft.trim())
        val draftIsInvalid = draft.isNotBlank() && !isHostname && CompatibilityManager.parseDnsServers(draft).isEmpty()
        var appliedServers by remember { mutableStateOf(state.customDns) }
        LaunchedEffect(draft) {
            resolveFailed = false
            delay(HOSTNAME_RESOLVE_DELAY_MS)
            if (isHostname) {
                val host = draft.trim()
                val servers = resolveDnsAddress(host)
                if (servers.isEmpty()) {
                    resolveFailed = true
                    return@LaunchedEffect
                }
                val name = tunnelDnsPresets.firstOrNull { it.address.equals(host, ignoreCase = true) }?.name ?: host
                state.applyCustomDns(servers.joinToString(", "), name)
                resolvedHost = host
            }
            if (!draftIsInvalid && state.customDns.isNotBlank() && state.customDns != appliedServers) {
                appliedServers = state.customDns
                restartBypassForDns(context)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 8.dp)) {
            NetFixTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    if (!dnsHostname.matches(it.trim())) state.applyCustomDns(it)
                },
                label = "Адрес DNS",
                placeholder = "Например 8.8.8.8 или dns.geohide.ru",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
        }
        val hint = when {
            draftIsInvalid -> "Нужен IP-адрес вроде 8.8.8.8 или имя вроде dns.geohide.ru"
            resolveFailed -> "Не получилось узнать адрес ${draft.trim()}. Проверьте имя или интернет"
            isHostname && resolvedHost == draft.trim() -> "${draft.trim()}: ${state.customDns}"
            isHostname -> "Узнаю адрес…"
            else -> "IP-адрес или имя DNS. Пока адрес не задан, используется системный DNS."
        }
        Text(
            text = hint,
            color = if (draftIsInvalid || resolveFailed) Nf.RedText else Nf.TextSub,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun CompatibilitySettingsSheets(state: CompatibilitySettingsState) {
    com.rupleide.netfix.data.TrackActionSheetVisible(state.anySheetVisible)
    val context = androidx.compose.ui.platform.LocalContext.current
    val dnsScope = androidx.compose.runtime.rememberCoroutineScope()

    NetFixActionSheet(
        visible = state.showVersionSheet,
        onDismissRequest = { state.showVersionSheet = false },
        icon = Icons.Default.History,
        title = "Работа сети",
        subtitle = "Выберите, как NetFix работает с сетью. Если на прошлой версии всё работало, а сейчас нет, выберите её.",
        options = CompatibilityManager.behaviorChoices.reversed().map { version ->
            NetFixSheetOption(
                label = behaviorLabel(version),
                selected = !state.customized && state.behaviorVersion == version
            ) {
                state.applyBehaviorVersion(version)
                state.showVersionSheet = false
            }
        },
        footnote = "Изменения применятся при следующем запуске обхода"
    )

    val customIsPreset = tunnelDnsPresets.any { it.name == state.customDnsName }
    val customServers = CompatibilityManager.parseDnsServers(state.customDns).filter { !it.contains(':') }
    val customServer = customServers.firstOrNull()
    val tunnelDnsChoices = listOf(
        com.rupleide.netfix.ui.TunnelDnsChoice(
            key = TUNNEL_DNS_SYSTEM,
            label = TUNNEL_DNS_SYSTEM,
            address = "DNS вашей сети",
            selected = state.dnsMode == TunnelDnsMode.System,
            systemDns = true,
            server = { emptyList() }
        ),
        com.rupleide.netfix.ui.TunnelDnsChoice(
            key = TUNNEL_DNS_CLOUDFLARE,
            label = TUNNEL_DNS_CLOUDFLARE,
            address = "1.1.1.1",
            selected = state.dnsMode == TunnelDnsMode.Cloudflare,
            server = { listOf("1.1.1.1") }
        )
    ) + tunnelDnsPresets.map { preset ->
        com.rupleide.netfix.ui.TunnelDnsChoice(
            key = preset.name,
            label = preset.name,
            address = preset.address,
            selected = state.dnsMode == TunnelDnsMode.Custom && state.customDnsName == preset.name,
            server = { com.rupleide.netfix.core.dnscheck.TunnelDnsCheck.resolveServers(preset.address) }
        )
    } + com.rupleide.netfix.ui.TunnelDnsChoice(
        key = TUNNEL_DNS_CUSTOM,
        label = TUNNEL_DNS_CUSTOM,
        address = if (customIsPreset) "" else customServer.orEmpty(),
        selected = state.dnsMode == TunnelDnsMode.Custom && !customIsPreset,
        server = { customServers }
    )
    com.rupleide.netfix.ui.TunnelDnsSheet(
        visible = state.showDnsSheet,
        choices = tunnelDnsChoices,
        onClose = { state.showDnsSheet = false },
        onSelect = { picked ->
            val choice = picked.key
            val preset = tunnelDnsPresets.firstOrNull { it.name == choice }
            when {
                choice == TUNNEL_DNS_SYSTEM -> {
                    state.applyDnsMode(TunnelDnsMode.System)
                    state.showDnsSheet = false
                    restartBypassForDns(context)
                }
                choice == TUNNEL_DNS_CLOUDFLARE -> {
                    state.applyDnsMode(TunnelDnsMode.Cloudflare)
                    state.showDnsSheet = false
                    restartBypassForDns(context)
                }
                preset != null -> {
                    if (state.resolvingDns == null) {
                        state.resolvingDns = preset.name
                        dnsScope.launch {
                            val servers = resolveDnsAddress(preset.address)
                            state.resolvingDns = null
                            if (servers.isEmpty()) {
                                Toast.makeText(context, "Не получилось узнать адрес ${preset.name}, проверьте интернет", Toast.LENGTH_SHORT).show()
                            } else {
                                state.applyCustomDns(servers.joinToString(", "), preset.name)
                                state.showDnsSheet = false
                                restartBypassForDns(context)
                            }
                        }
                    }
                }
                else -> {
                    state.applyDnsMode(TunnelDnsMode.Custom)
                    state.showDnsSheet = false
                    if (customServer != null) restartBypassForDns(context)
                }
            }
        }
    )
}
