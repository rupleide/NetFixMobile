package com.rupleide.netfix.ui

import androidx.compose.material3.Text
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.NfTvButton
import com.rupleide.netfix.ui.components.NfTvTwoPane
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.rupleide.netfix.ui.components.nfTvFocus
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.ui.components.BypassGuard
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfSoftButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfDivider
import com.rupleide.netfix.ui.components.NfCornerAura
import com.rupleide.netfix.ui.components.NfFieldBlock
import com.rupleide.netfix.ui.components.NfInputField
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.NfScreenTitle
import com.rupleide.netfix.ui.components.NfSection
import com.rupleide.netfix.ui.components.NfSegmented
import com.rupleide.netfix.ui.components.NfSlotText
import com.rupleide.netfix.ui.components.NfStatusCard
import com.rupleide.netfix.ui.components.NfSwitchRow
import com.rupleide.netfix.ui.components.nfAura
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.rememberStagedLevel
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MIN_PORT = 1024
private const val MAX_PORT = 65535

@Composable
fun TgProxyTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactButtons = configuration.screenWidthDp < 380
    val scrollState = rememberScrollState()
    val stage = rememberStagedLevel(1)
    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()

    var port by remember { mutableStateOf(TgProxyController.getPort(context).toString()) }
    var dcIps by remember { mutableStateOf(TgProxyController.getDcIps(context)) }
    var isDcAuto by remember { mutableStateOf(dcIps.isEmpty()) }
    var poolSize by remember { mutableIntStateOf(TgProxyController.getPoolSize(context)) }
    var secretKey by remember { mutableStateOf(TgProxyController.getOrGenerateSecret(context)) }
    var showSecret by remember { mutableStateOf(false) }
    var confirmRegenerate by remember { mutableStateOf(false) }
    var cfEnabled by remember { mutableStateOf(TgProxyController.isCfEnabled(context)) }
    var cfPriority by remember { mutableStateOf(TgProxyController.isCfPriority(context)) }
    var latency by remember { mutableStateOf<Int?>(null) }

    val portNumber = port.toIntOrNull()
    val portValid = portNumber != null && portNumber in MIN_PORT..MAX_PORT
    val portError = port.isNotEmpty() && !portValid

    LaunchedEffect(confirmRegenerate) {
        if (confirmRegenerate) {
            delay(4000L)
            confirmRegenerate = false
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(port, lifecycleOwner) {
        val currentPort = port.toIntOrNull() ?: TgProxyController.DEFAULT_PORT
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                latency = withContext(Dispatchers.IO) {
                    TgProxyController.measureLatency(TgProxyController.DEFAULT_BIND_IP, currentPort)
                }
                delay(5000)
            }
        }
    }

    val proxyUrl = TgProxyController.getTgProxyUrl(
        TgProxyController.DEFAULT_BIND_IP,
        portNumber ?: TgProxyController.DEFAULT_PORT,
        secretKey
    )
    val shownLatency = latency
    val isChecking = portValid && shownLatency == null
    val isWorking = portValid && shownLatency != null && shownLatency >= 0

    val dotColor = when {
        !portValid -> Nf.Muted
        isChecking -> Nf.Bright
        isWorking -> Nf.Green
        else -> Nf.Muted
    }
    val auraColor = when {
        !portValid -> Color(0x0EF4F4F5)
        isChecking -> Color(0x1AF4F4F5)
        isWorking -> Color(0x2922C55E)
        else -> Color(0x0EF4F4F5)
    }
    val statusTitle = when {
        isChecking -> "Подключаемся…"
        isWorking -> "Прокси работает"
        else -> "Прокси выключен"
    }
    val rightText = when {
        isChecking -> "… мс"
        isWorking -> "$shownLatency мс"
        else -> "- мс"
    }
    val rightColor = when {
        isWorking -> Nf.GreenText
        isChecking -> Nf.Text
        else -> Nf.Muted
    }
    val borderColor = when {
        isWorking -> Nf.Green.copy(alpha = 0.45f)
        isChecking -> Nf.Bright.copy(alpha = 0.35f)
        else -> Nf.Line
    }

    fun copyLink() {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Proxy", proxyUrl))
        Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
    }

    fun openInTelegram() {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(proxyUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(context, "Telegram не найден", Toast.LENGTH_SHORT).show()
        }
    }

    val statusCardContent: @Composable () -> Unit = {
        NfStatusCard(
            dotColor = dotColor,
            title = statusTitle,
            rightText = rightText,
            rightColor = rightColor,
            line = "${TgProxyController.DEFAULT_BIND_IP}:${port.ifEmpty { "-" }}",
            borderColor = borderColor,
            modifier = Modifier.nfEnter(1),
            slot = {
                when {
                    !portValid -> NfSlotText("Исправьте порт, чтобы включить", Nf.Muted)
                    isChecking -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NfProgress(fraction = 0f, indeterminate = true)
                        }
                        NfSlotText("Запускаем прокси", Nf.Text)
                    }
                    isWorking -> NfSlotText(
                        "${latencyQuality(shownLatency ?: 0)} · пул: ${poolLabel(poolSize)}",
                        Nf.GreenText
                    )
                    else -> NfSlotText("Включите обход на главном экране", Nf.Muted)
                }
            },
            buttons = {
                if (LocalIsTv.current) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NfTvButton(text = "Открыть в Telegram", enabled = isWorking, onClick = { openInTelegram() })
                        NfTvButton(text = "Копировать ссылку", enabled = portValid, onClick = { copyLink() })
                    }
                } else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NfSoftButton(
                        text = if (compactButtons) "Открыть" else "Открыть в Telegram",
                        enabled = isWorking,
                        modifier = Modifier.weight(1f),
                        onClick = { openInTelegram() }
                    )
                    NfSoftButton(
                        text = if (compactButtons) "Ссылка" else "Копировать ссылку",
                        enabled = portValid,
                        modifier = Modifier.weight(1f),
                        onClick = { copyLink() }
                    )
                }
            }
        )
    }

    val sectionsContent: @Composable ColumnScope.() -> Unit = {
        NfSection(title = "Подключение", modifier = Modifier.nfEnter(2), cardPadding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NfFieldBlock(
                    label = "Порт подключения",
                    isError = portError,
                    help = if (portError) "Порт должен быть от $MIN_PORT до $MAX_PORT" else null
                ) {
                    NfInputField(
                        value = port,
                        onValueChange = { value ->
                            if (BypassGuard.isActive()) {
                                BypassGuard.notifyBlocked(context)
                                return@NfInputField
                            }
                            val filtered = value.filter { it.isDigit() }
                            if (filtered.length <= 5) {
                                port = filtered
                                val newPort = filtered.toIntOrNull()
                                if (newPort != null && newPort in MIN_PORT..MAX_PORT) {
                                    TgProxyController.setPort(context, newPort)
                                    com.rupleide.netfix.core.debug.AppDebugManager.log("Изменен порт TG прокси: $newPort")
                                }
                            }
                        },
                        placeholder = "Порт",
                        isError = portError,
                        keyboardType = KeyboardType.Number
                    )
                }

                NfSwitchRow(
                    title = "Автовыбор IP-адресов DC",
                    subtitle = "Использовать встроенные адреса Telegram",
                    checked = isDcAuto,
                    onCheckedChange = { checked ->
                        if (BypassGuard.isActive()) {
                            BypassGuard.notifyBlocked(context)
                            return@NfSwitchRow
                        }
                        isDcAuto = checked
                        if (checked) {
                            dcIps = ""
                            TgProxyController.setDcIps(context, "")
                        }
                    }
                )

                if (!isDcAuto) {
                    NfFieldBlock(label = "IP-адреса DC", help = "Формат: номер DC и адрес, через запятую") {
                        NfInputField(
                            value = dcIps,
                            onValueChange = { value ->
                                if (BypassGuard.isActive()) {
                                    BypassGuard.notifyBlocked(context)
                                    return@NfInputField
                                }
                                dcIps = value
                                TgProxyController.setDcIps(context, value)
                            },
                            placeholder = "2:ip,4:ip",
                            monospace = true
                        )
                    }
                }

                NfDivider()

                NfFieldBlock(label = "Пул WebSocket-соединений") {
                    NfSegmented(
                        options = listOf("2", "4", "6"),
                        selectedIndex = listOf(2, 4, 6).indexOf(poolSize).coerceAtLeast(0),
                        onSelect = { index ->
                            if (BypassGuard.isActive()) {
                                BypassGuard.notifyBlocked(context)
                                return@NfSegmented
                            }
                            val size = listOf(2, 4, 6)[index]
                            poolSize = size
                            TgProxyController.setPoolSize(context, size)
                        },
                        indicatorColor = Nf.Blue,
                        selectedTextColor = Color.White
                    )
                }
            }
        }

        if (stage >= 1) {
        NfSection(
            title = "Безопасность",
            modifier = Modifier.nfEnter(3),
            cardPadding = 16.dp,
            note = if (confirmRegenerate) "Старые ссылки на прокси перестанут работать." else null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NfFieldBlock(label = "Секретный ключ") {
                    NfInputField(
                        value = secretKey,
                        onValueChange = {},
                        placeholder = "Ключ",
                        readOnly = true,
                        monospace = true,
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .nfTvFocus(corner = 10.dp, grow = 1.03f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showSecret = !showSecret },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showSecret) "Скрыть ключ" else "Показать ключ",
                                    tint = Nf.TextSub,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NfSoftButton(
                        text = "Копировать",
                        modifier = Modifier.weight(1f),
                        onClick = { copyLink() }
                    )
                    NfSoftButton(
                        text = if (confirmRegenerate) "Точно пересоздать?" else "Пересоздать",
                        style = if (confirmRegenerate) NfButtonStyle.DangerSolid else NfButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (BypassGuard.isActive()) {
                                BypassGuard.notifyBlocked(context)
                                confirmRegenerate = false
                                return@NfSoftButton
                            }
                            if (confirmRegenerate) {
                                secretKey = TgProxyController.regenerateSecret(context)
                                confirmRegenerate = false
                                Toast.makeText(context, "Ключ обновлен", Toast.LENGTH_SHORT).show()
                            } else {
                                confirmRegenerate = true
                            }
                        }
                    )
                }
            }
        }

        NfSection(
            title = "Cloudflare CDN Proxy",
            modifier = Modifier.nfEnter(4),
            note = "Задержка и состояние прокси показаны в карточке вверху экрана."
        ) {
            NfSwitchRow(
                title = "Использовать Cloudflare CDN",
                subtitle = "Туннелирование трафика через Cloudflare",
                checked = cfEnabled,
                onCheckedChange = { checked ->
                    if (BypassGuard.isActive()) {
                        BypassGuard.notifyBlocked(context)
                        return@NfSwitchRow
                    }
                    cfEnabled = checked
                    TgProxyController.setCfEnabled(context, checked)
                }
            )
            NfDivider()
            NfSwitchRow(
                title = "Приоритет Cloudflare",
                subtitle = "Приоритетное использование CDN адресов",
                checked = cfPriority,
                onCheckedChange = { checked ->
                    if (BypassGuard.isActive()) {
                        BypassGuard.notifyBlocked(context)
                        return@NfSwitchRow
                    }
                    cfPriority = checked
                    TgProxyController.setCfPriority(context, checked)
                },
                switchModifier = Modifier.focusProperties { down = navBarFocusRequester }
            )
        }
        }
    }

    val tv = LocalIsTv.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Nf.Bg),
        contentAlignment = Alignment.TopCenter
    ) {
        NfCornerAura(auraColor)
        if (tv) {
            NfTvTwoPane(
                title = "Прокси Telegram",
                rightTitle = "Настройки",
                left = {
                    statusCardContent()
                    Text(
                        text = "Порт, ключ и Cloudflare настраиваются справа. Изменения применяются сразу.",
                        color = Nf.TextSub,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                right = { sectionsContent() }
            )
        } else {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (isLandscape) 820.dp else 500.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = navOverlayReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NfScreenTitle("Прокси Telegram", Modifier.nfEnter(0))
            statusCardContent()
            sectionsContent()
        }
        }
    }
}

private fun latencyQuality(ms: Int): String = when {
    ms < 100 -> "Отличная задержка"
    ms < 250 -> "Хорошая задержка"
    else -> "Высокая задержка"
}

private fun poolLabel(size: Int): String = if (size >= 5) "$size соединений" else "$size соединения"
