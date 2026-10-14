package com.rupleide.netfix.ui

import androidx.compose.foundation.layout.ColumnScope
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.NfTvTwoPane
import com.rupleide.netfix.ui.components.nfTvFocus
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.vector.ImageVector
import com.rupleide.netfix.R
import com.rupleide.netfix.core.update.UpdateManager
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfSoftButton
import com.rupleide.netfix.ui.components.NfBottomSheet
import com.rupleide.netfix.ui.components.NfMiniButton
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.rememberNetFixHaptics
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfDivider
import com.rupleide.netfix.ui.components.NfCornerAura
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.NfRow
import com.rupleide.netfix.ui.components.NfSection
import com.rupleide.netfix.ui.components.NfSlotText
import com.rupleide.netfix.ui.components.NfStatusCard
import com.rupleide.netfix.ui.components.QrLinkDialog
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.nfHeartbeat
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class InfoItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val warm: Boolean = false,
    val iconRes: Int? = null
)

private val infoItems = listOf(
    InfoItem("about", Icons.Default.Info, "О приложении", "Что умеет NetFix Mobile"),
    InfoItem("channel", Icons.Default.Campaign, "Telegram-канал", "Новости и новые способы раньше всех", iconRes = R.drawable.ic_telegram),
    InfoItem("feedback", Icons.Default.Help, "Обратная связь", "Telegram или GitHub Issues"),
    InfoItem("support", Icons.Default.Favorite, "Поддержать автора", "СБП или TON", warm = true),
    InfoItem("important", Icons.Default.Warning, "Важная информация", "Решения частых проблем"),
    InfoItem("windows", Icons.Default.Computer, "NetFix для Windows", "Версия для Windows 10/11"),
    InfoItem("privacy", Icons.Default.Security, "Приватность", "Данные пользователя не собираются"),
    InfoItem("license", Icons.Default.Copyright, "Лицензия и копирайты", "GPL-3.0 · ByeByeDPI, TG WS Proxy")
)

private val warmTint = Color(0xFFFDBA74)
private val warmBackground = Color(0x1FFB923C)

@Composable
fun InfoTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    val sharedPrefs = remember(context) { context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE) }
    val isTv = remember(sharedPrefs) { sharedPrefs.getBoolean("device_type_tv", false) }
    val qrCodesEnabled = if (isTv) true else sharedPrefs.getBoolean("qr_codes_enabled", false)

    var activeQrUrl by remember { mutableStateOf<String?>(null) }
    var activeQrLabel by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var listTop by remember { mutableStateOf(0) }
    var showSbpSheet by remember { mutableStateOf(false) }
    val appIcon = remember(context) {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap(256, 256).asImageBitmap()
        }.getOrNull()
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

    val handleLinkClick = { url: String, label: String ->
        if (qrCodesEnabled) {
            activeQrUrl = url
            activeQrLabel = label
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    val openTelegram = { domain: String, label: String ->
        if (qrCodesEnabled) {
            handleLinkClick("https://t.me/$domain", label)
        } else {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$domain")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                handleLinkClick("https://t.me/$domain", label)
            }
        }
    }

    val updateInfo = com.rupleide.netfix.data.updateInfoGlobal
    val updateStatus = com.rupleide.netfix.data.updateStatusGlobal
    val updateProgress = com.rupleide.netfix.data.updateProgressGlobal
    val displayVersion = updateInfo?.let {
        if (it.version.startsWith("v", ignoreCase = true)) it.version else "v${it.version}"
    } ?: ""
    val isDownloading = updateProgress >= 0
    val isChecking = updateStatus == "Проверка..."
    val isError = !isDownloading && updateStatus?.startsWith("Ошибка") == true
    val hasUpdate = updateInfo != null && !isDownloading && !isError

    val checkForUpdates: () -> Unit = {
        scope.launch {
            com.rupleide.netfix.data.updateStatusGlobal = "Проверка..."
            val info = UpdateManager.checkUpdate(context)
            if (info != null) {
                com.rupleide.netfix.data.updateInfoGlobal = info
                com.rupleide.netfix.data.updateStatusGlobal = null
            } else {
                com.rupleide.netfix.data.updateStatusGlobal = "У вас последняя версия"
                delay(3000)
                com.rupleide.netfix.data.updateStatusGlobal = null
            }
        }
    }

    val startDownload: () -> Unit = {
        val info = com.rupleide.netfix.data.updateInfoGlobal
        if (info != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                com.rupleide.netfix.data.updateStatusGlobal = "Скачивание..."
                com.rupleide.netfix.data.updateProgressGlobal = 0f
                UpdateManager.downloadAndInstallApk(
                    context = context.applicationContext,
                    downloadUrl = info.downloadUrl,
                    fileName = "update.apk",
                    onProgress = { progress -> com.rupleide.netfix.data.updateProgressGlobal = progress },
                    onError = { error ->
                        com.rupleide.netfix.data.updateStatusGlobal = "Ошибка: $error"
                        com.rupleide.netfix.data.updateProgressGlobal = -1f
                    }
                )
                if (com.rupleide.netfix.data.updateStatusGlobal?.startsWith("Ошибка") != true) {
                    com.rupleide.netfix.data.updateStatusGlobal = null
                }
                com.rupleide.netfix.data.updateProgressGlobal = -1f
            }
        }
    }

    val heroContent: @Composable () -> Unit = {
        val heroShape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .nfEnter(0)
                .fillMaxWidth()
                .clip(heroShape)
                .background(Nf.Glass)
                .border(1.dp, Nf.Line, heroShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "NetFix",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Nf.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bolt),
                            contentDescription = null,
                            tint = Nf.Text,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("NetFix Mobile", color = Nf.Text, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                    Text("Версия $currentVersion · Android", color = Nf.TextSub, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (!LocalIsTv.current) {
                Text(
                    text = "Единый инструмент для обхода блокировок и ускорения интернета.",
                    color = Nf.TextSub,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroAction(
                    iconRes = R.drawable.ic_telegram,
                    label = "Тг-Канал",
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    onClick = { openTelegram("NetFixRuBi", "Telegram-канал") }
                )
                HeroAction(
                    iconVector = Icons.Default.Favorite,
                    label = "Помочь",
                    pulse = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        expandedId = "support"
                        scope.launch { scrollState.animateScrollTo(listTop) }
                    }
                )
                HeroAction(
                    iconRes = R.drawable.ic_github,
                    label = "GitHub",
                    modifier = Modifier.weight(1f),
                    onClick = { handleLinkClick("https://github.com/rupleide/NetFixMobile", "GitHub NetFix Mobile") }
                )
            }
        }
    }

    val statusContent: @Composable () -> Unit = {
        val statusDot = when {
            isError -> Nf.Red
            isDownloading || isChecking -> Nf.Bright
            else -> Nf.Green
        }
        val statusBorder = when {
            isError -> Nf.Red.copy(alpha = 0.40f)
            isDownloading || isChecking -> Nf.Bright.copy(alpha = 0.35f)
            hasUpdate -> Nf.Bright.copy(alpha = 0.45f)
            else -> Nf.Line
        }
        NfStatusCard(
            dotColor = statusDot,
            title = "Версия и обновление",
            rightText = currentVersion,
            rightColor = Nf.TextSub,
            line = when {
                isDownloading -> "Скачиваем обновление"
                isChecking -> "Проверяем обновления…"
                isError -> "Не удалось обновить"
                hasUpdate -> "Доступна версия ${displayVersion.removePrefix("v")}"
                else -> "У вас последняя версия"
            },
            borderColor = statusBorder,
            modifier = Modifier.nfEnter(1),
            slot = {
                when {
                    isDownloading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NfProgress(fraction = updateProgress)
                        }
                        NfSlotText("${(updateProgress * 100).toInt()}%", Nf.Text)
                    }
                    isChecking -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NfProgress(fraction = 0f, indeterminate = true)
                        }
                        NfSlotText("Связываемся с GitHub", Nf.Text)
                    }
                    isError -> NfSlotText(updateStatus?.removePrefix("Ошибка:")?.trim().orEmpty().ifBlank { "Нет соединения с GitHub" }, Nf.RedText)
                    hasUpdate -> NfSlotText("Установлена $currentVersion · можно обновить сейчас", Nf.TextSub)
                    else -> NfSlotText("Можно проверить вручную", Nf.TextSub)
                }
            },
            buttons = {
                NfSoftButton(
                    text = when {
                        isDownloading -> "Скачивание: ${(updateProgress * 100).toInt()}%"
                        isChecking -> "Проверяем…"
                        isError -> "Повторить"
                        hasUpdate -> "Скачать и установить $displayVersion"
                        else -> "Проверить обновления"
                    },
                    style = NfButtonStyle.Soft,
                    enabled = !isDownloading && !isChecking,
                    onClick = { if (hasUpdate) startDownload() else checkForUpdates() }
                )
            }
        )
    }

    val detailsContent: @Composable ColumnScope.() -> Unit = {
        NfSection(
            title = "Подробнее",
            modifier = Modifier
                .nfEnter(2)
                .onGloballyPositioned { listTop = it.positionInParent().y.toInt() }
        ) {
            infoItems.forEachIndexed { index, item ->
                if (index > 0) NfDivider()
                val expanded = expandedId == item.id
                val chevronRotation by animateFloatAsState(
                    targetValue = if (expanded) 90f else 0f,
                    animationSpec = tween(220),
                    label = "infoChevron"
                )
                NfRow(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = if (item.iconRes == null) item.icon else null,
                    iconPainter = item.iconRes?.let { painterResource(id = it) },
                    iconTint = if (item.warm) warmTint else Nf.Text,
                    iconModifier = if (item.id == "support") Modifier.nfHeartbeat() else Modifier,
                    iconBackground = if (item.warm) warmBackground else Color(0x0FFFFFFF),
                    modifier = if (index == infoItems.lastIndex) Modifier.focusProperties { down = navBarFocusRequester } else Modifier,
                    onClick = { expandedId = if (expanded) null else item.id },
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Nf.TextSub,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation)
                        )
                    }
                )
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(260)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
                ) {
                    InfoBody(id = item.id, onLink = handleLinkClick, onTelegram = openTelegram, onSbp = { showSbpSheet = true })
                }
            }
        }

        Text(
            text = "NetFix Mobile $currentVersion · Open Source",
            color = Nf.Muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .nfEnter(3)
        )
    }

    val tv = LocalIsTv.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Nf.Bg),
        contentAlignment = Alignment.TopCenter
    ) {
        NfCornerAura(Nf.Bright.copy(alpha = 0.056f))
        if (tv) {
            NfTvTwoPane(
                title = "Информация",
                rightTitle = "Подробнее",
                hideSectionTitles = true,
                rightScroll = scrollState,
                left = {
                    heroContent()
                    statusContent()
                },
                right = { detailsContent() }
            )
        } else {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (isLandscape) 820.dp else 500.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = navOverlayReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            heroContent()
            statusContent()
            detailsContent()
        }
        }
    }

    SbpSheet(
        visible = showSbpSheet,
        onDismiss = { showSbpSheet = false },
        onFallback = { handleLinkClick("https://www.tinkoff.ru/rm/r_eELpDmupvc.SCiWRkVJON/bgKkD30493", "Донат через СБП") }
    )

    activeQrUrl?.let { url ->
        QrLinkDialog(
            url = url,
            label = activeQrLabel ?: "",
            onDismiss = {
                activeQrUrl = null
                activeQrLabel = null
            }
        )
    }
}

@Composable
private fun InfoText(text: String) {
    Text(
        text = text,
        color = Nf.TextSub,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun InfoBody(id: String, onLink: (String, String) -> Unit, onTelegram: (String, String) -> Unit, onSbp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (id) {
            "about" -> {
                InfoText("NetFix Mobile - это единый инструмент для обхода блокировок и ускорения интернета на вашем устройстве.")
                InfoText("NetFix Mobile объединяет лучшие утилиты для решения большинства проблем.")
            }
            "channel" -> {
                InfoText("Подпишитесь на канал NetFix, там выходят все новости проекта, новые способы обхода блокировок и обновления раньше всех.")
                NfSoftButton("Открыть канал", onClick = { onTelegram("NetFixRuBi", "Telegram-канал") })
            }
            "feedback" -> {
                InfoText("Если вам нужна помощь или вы нашли ошибку в приложении, напишите разработчику в Telegram, я обязательно прочитаю и помогу чем смогу.")
                InfoText("Если вопрос не срочный, можете задать его на GitHub.")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NfSoftButton("Telegram", modifier = Modifier.weight(1f), onClick = { onTelegram("sofirka_hanabi", "Telegram разработчика") })
                    NfSoftButton("GitHub Issues", modifier = Modifier.weight(1f), onClick = { onLink("https://github.com/rupleide/NetFixMobile/issues", "GitHub Issues") })
                }
            }
            "support" -> {
                InfoText("Все пожертвования идут напрямую российскому соло-разработчику из города Пермь. Я искренне благодарен каждому за поддержку - это действительно помогает развивать проект и мотивирует двигаться дальше.")
                InfoText("Авторизация и оплата происходят на официальном и безопасном сайте Т-Банка. Даже если у вас банк другого плательщика, вы всё равно сможете совершить перевод через СБП.")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DonateButton("СБП", R.drawable.ic_sbp, Color(0xFFFF6B00), Modifier.weight(1f)) {
                        onSbp()
                    }
                    DonateButton("TON", R.drawable.ic_ton, Color(0xFF0088CC), Modifier.weight(1f)) {
                        onLink("https://app.tonkeeper.com/transfer/UQCx8X4z86Jej2hc8l_IVni8e0Q8uDHhC8_PJ2zymxngVc2Q", "Донат через TON")
                    }
                }
            }
            "important" -> {
                InfoText("Если вы не хотите задавать вопрос напрямую, вы можете посмотреть решения частых проблем в этом документе.")
                NfSoftButton("Решения проблем", onClick = { onLink("https://github.com/rupleide/NetFixMobile/blob/main/TROUBLESHOOTING.md", "TROUBLESHOOTING.md") })
            }
            "windows" -> {
                InfoText("NetFix изначально создавался как программа для компьютеров под управлением Windows 10/11. ПК-версия является одним из лучших инструментов для обхода сетевых ограничений на компьютере в один клик и без использования сторонних VPN.")
                InfoText("Если вам нужен стабильный доступ на ПК, вы можете скачать официальный клиент NetFix для ПК.")
                NfSoftButton("Открыть GitHub проекта для ПК", onClick = { onLink("https://github.com/rupleide/NetFix", "GitHub NetFix (Windows)") })
            }
            "privacy" -> {
                InfoText("Приложение не собирает и не передаёт никакие данные пользователя.")
            }
            "license" -> {
                InfoText("NetFix Mobile является полностью свободным проектом с открытым исходным кодом и распространяется под лицензией GNU General Public License v3.0 (GPL-3.0).")
                InfoText("Выражаем огромную благодарность авторам оригинальных нативных решений, на которых базируется работа NetFix Android:")
                LinkRow("ByeByeDPI (GitHub)", "Разработчик: romanvht (обход DPI на уровне VPN)") {
                    onLink("https://github.com/romanvht/ByeByeDPI", "ByeByeDPI GitHub")
                }
                LinkRow("TG WS Proxy Android (GitHub)", "Разработчик: amurcanov (локальный прокси Telegram)") {
                    onLink("https://github.com/amurcanov/tg-ws-proxy-android", "TG WS Proxy GitHub")
                }
            }
        }
    }
}

@Composable
private fun DonateButton(text: String, iconRes: Int, brand: Color, modifier: Modifier, onClick: () -> Unit) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(52.dp)
            .nfTvFocus(corner = 12.dp, grow = 1.03f)
            .clip(shape)
            .background(if (pressed) Color(0xFF383838) else Color(0xFF242424))
            .border(1.dp, Color(0x1AFFFFFF), shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptics(NetFixHaptic.Tap)
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(text = text, color = Nf.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun HeroAction(label: String, modifier: Modifier, iconVector: ImageVector? = null, iconRes: Int? = null, pulse: Boolean = false, onClick: () -> Unit) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .height(76.dp)
            .nfTvFocus(corner = 16.dp, grow = 1.03f)
            .clip(shape)
            .background(if (pressed) Color(0xFF383838) else Color(0xFF242424))
            .border(1.dp, Color(0x1AFFFFFF), shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptics(NetFixHaptic.Tap)
                onClick()
            },
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (iconRes != null) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Nf.Text, modifier = Modifier.size(24.dp))
        } else if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Nf.Text,
                modifier = Modifier.size(24.dp).then(if (pulse) Modifier.nfHeartbeat() else Modifier)
            )
        }
        Text(text = label, color = Nf.Text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

private data class BankApp(val label: String, val packageName: String)

private val bankApps = listOf(
    BankApp("Сбер", "ru.sberbankmobile"),
    BankApp("Т-Банк", "com.idamob.tinkoff.android"),
    BankApp("Альфа-Банк", "ru.alfabank.mobile.android"),
    BankApp("ВТБ", "ru.vtb24.mobilebanking.android"),
    BankApp("Райффайзен", "ru.raiffeisennews"),
    BankApp("Газпромбанк", "ru.gazprombank.android.mobilebank.app"),
    BankApp("Совкомбанк", "ru.sovcomcard.halva.v1"),
    BankApp("Почта Банк", "ru.pochtabank.mobile")
)

private const val SbpPhoneDisplay = "8 995 478 80 89"
private const val SbpPhoneRaw = "89954788089"

@Composable
private fun SbpSheet(visible: Boolean, onDismiss: () -> Unit, onFallback: () -> Unit) {
    val context = LocalContext.current
    val installed = remember(context) {
        bankApps.filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
    }
    fun copyNumber() {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("СБП", SbpPhoneRaw))
        android.widget.Toast.makeText(context, "Номер скопирован", android.widget.Toast.LENGTH_SHORT).show()
    }
    NfBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        icon = Icons.Default.Favorite,
        title = "Перевод по СБП",
        subtitle = "Номер скопируется сам",
        closeLabel = "Закрыть",
        maxHeightFraction = 0.7f
    ) { entry ->
        val shape = RoundedCornerShape(14.dp)
        Text(
            text = "Выберите банк: откроется его приложение. В нём найдите перевод по номеру телефона и вставьте номер.",
            color = Nf.TextSub,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = entry(0).fillMaxWidth()
        )
        Row(
            modifier = entry(0)
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .background(Nf.Surface)
                .border(1.dp, Nf.Line, shape)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SbpPhoneDisplay,
                color = Nf.Text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
            NfMiniButton("Копировать", onClick = { copyNumber() }, width = 108.dp)
        }
        installed.forEachIndexed { index, bank ->
            BankRow(
                bank = bank,
                modifier = entry(minOf(index + 1, 4)),
                onClick = {
                    copyNumber()
                    val launch = context.packageManager.getLaunchIntentForPackage(bank.packageName)
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(launch)
                        } catch (_: Exception) {}
                    }
                    onDismiss()
                }
            )
        }
        NfSoftButton(
            text = if (installed.isEmpty()) "Страница оплаты" else "Другой способ: страница оплаты",
            modifier = entry(4),
            onClick = {
                onDismiss()
                onFallback()
            }
        )
    }
}

@Composable
private fun BankRow(bank: BankApp, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(bank.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(bank.packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Surface)
            .border(1.dp, Nf.Line, shape)
    ) {
        NfRow(
            title = bank.label,
            subtitle = "Открыть приложение",
            onClick = onClick,
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Nf.TextSub,
                    modifier = Modifier.size(18.dp)
                )
            },
            leading = {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        )
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Surface)
            .border(1.dp, Nf.Line, shape)
    ) {
        NfRow(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Nf.TextSub,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}
