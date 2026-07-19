package com.rupleide.netfix.ui

import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.verticalScroll
import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.rupleide.netfix.core.update.UpdateManager
import com.rupleide.netfix.ui.components.NetFixButton
import com.rupleide.netfix.ui.components.QrLinkDialog
import com.rupleide.netfix.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.ui.focus.focusProperties

@Composable
fun InfoTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 86.dp
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

    LaunchedEffect(Unit) {
        repeat(8) { index ->
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
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = navOverlayReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "О приложении",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "NetFix - это единый инструмент для обхода блокировок и ускорения интернета на вашем устройстве.\n\nNetFix объединяет лучшие утилиты для решения большинства проблем.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Версия и обновление",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                val displayVersion = com.rupleide.netfix.data.updateInfoGlobal?.let {
                    if (it.version.startsWith("v", ignoreCase = true)) it.version else "v${it.version}"
                } ?: ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Статус", color = Color(0xFFA1A1AA), fontSize = 14.sp)
                    Text(
                        text = if (com.rupleide.netfix.data.updateInfoGlobal != null) "Найдено обновление: $displayVersion" else "У вас последняя версия",
                        color = if (com.rupleide.netfix.data.updateInfoGlobal != null) Color(0xFF3B82F6) else Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Установленная версия", color = Color(0xFFA1A1AA), fontSize = 14.sp)
                    Text(text = currentVersion, color = Color(0xFFF4F4F5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val infoInteractionSource = remember { MutableInteractionSource() }
                val infoPressed by infoInteractionSource.collectIsPressedAsState()
                val infoHovered by infoInteractionSource.collectIsHoveredAsState()
                val infoFocused by infoInteractionSource.collectIsFocusedAsState()
                val infoHighlighted = infoPressed || infoHovered || infoFocused

                val buttonText = when {
                    com.rupleide.netfix.data.updateProgressGlobal >= 0 -> "Скачивание: ${(com.rupleide.netfix.data.updateProgressGlobal * 100).toInt()}%"
                    com.rupleide.netfix.data.updateStatusGlobal == "Проверка..." -> "Проверка..."
                    com.rupleide.netfix.data.updateInfoGlobal != null -> "Скачать и установить $displayVersion"
                    com.rupleide.netfix.data.updateStatusGlobal != null -> com.rupleide.netfix.data.updateStatusGlobal!!
                    else -> "Проверить обновления"
                }

                val buttonEnabled = com.rupleide.netfix.data.updateStatusGlobal != "Проверка..." && com.rupleide.netfix.data.updateProgressGlobal < 0

                val infoBgColor by animateColorAsState(
                    targetValue = if (buttonEnabled) {
                        if (com.rupleide.netfix.data.updateInfoGlobal != null) {
                            if (infoHighlighted) Color(0xFF5A9EFC) else Color(0xFF3B82F6)
                        } else {
                            if (infoHighlighted) Color(0xFF38383A) else Color(0xFF242426)
                        }
                    } else {
                        Color(0xFF2E2E2E)
                    },
                    animationSpec = tween(150),
                    label = "infoBtnBg"
                )
                val infoBorderColor by animateColorAsState(
                    targetValue = if (buttonEnabled) {
                        if (infoHighlighted) Color.White else Color(0x1AFFFFFF)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(150),
                    label = "infoBtnBorder"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(infoBgColor)
                        .border(1.dp, infoBorderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = buttonEnabled,
                            interactionSource = infoInteractionSource,
                            indication = null
                        ) {
                            if (com.rupleide.netfix.data.updateInfoGlobal != null) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    com.rupleide.netfix.data.updateStatusGlobal = "Скачивание..."
                                    com.rupleide.netfix.data.updateProgressGlobal = 0f
                                    UpdateManager.downloadAndInstallApk(
                                        context = context.applicationContext,
                                        downloadUrl = com.rupleide.netfix.data.updateInfoGlobal!!.downloadUrl,
                                        fileName = "update.apk",
                                        onProgress = { progress -> com.rupleide.netfix.data.updateProgressGlobal = progress },
                                        onError = { error ->
                                            com.rupleide.netfix.data.updateStatusGlobal = "Ошибка: $error"
                                            com.rupleide.netfix.data.updateProgressGlobal = -1f
                                        }
                                    )
                                    com.rupleide.netfix.data.updateStatusGlobal = null
                                    com.rupleide.netfix.data.updateProgressGlobal = -1f
                                }
                            } else {
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
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Telegram-канал",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Подпишитесь на канал NetFix, там выходят все новости проекта, новые способы обхода блокировок и обновления раньше всех.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                NetFixButton(
                    text = "Открыть канал",
                    onClick = { handleLinkClick("https://t.me/NetFixRuBi", "Telegram-канал") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Обратная связь",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Если вам нужна помощь или вы нашли ошибку в приложении, напишите разработчику в Telegram, я обязательно прочитаю и помогу чем смогу.\n\nЕсли вопрос не срочный, можете задать его на GitHub.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NetFixButton(
                        text = "Telegram",
                        onClick = { handleLinkClick("https://t.me/RUPLEiDE", "Telegram разработчика") },
                        modifier = Modifier.weight(1f)
                    )
                    NetFixButton(
                        text = "GitHub Issues",
                        onClick = { handleLinkClick("https://github.com/rupleide/NetFixMobile/issues", "GitHub Issues") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Поддержать автора",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Все пожертвования идут напрямую российскому соло-разработчику из города Пермь. Я искренне благодарен каждому за поддержку - это действительно помогает развивать проект и мотивирует двигаться дальше.\n\nАвторизация и оплата происходят на официальном и безопасном сайте Т-Банка. Даже если у вас банк другого плательщика, вы всё равно сможете совершить перевод через СБП.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NetFixButton(
                        text = "СБП",
                        onClick = { handleLinkClick("https://www.tinkoff.ru/rm/r_eELpDmupvc.SCiWRkVJON/bgKkD30493", "Донат через СБП") },
                        iconRes = com.rupleide.netfix.R.drawable.ic_sbp,
                        backgroundColor = Color(0xFFFF6B00),
                        highlightedColor = Color(0xFFE05E00),
                        modifier = Modifier.weight(1f)
                    )
                    NetFixButton(
                        text = "TON",
                        onClick = { handleLinkClick("https://app.tonkeeper.com/transfer/UQCx8X4z86Jej2hc8l_IVni8e0Q8uDHhC8_PJ2zymxngVc2Q", "Донат через TON") },
                        iconRes = com.rupleide.netfix.R.drawable.ic_ton,
                        backgroundColor = Color(0xFF0088CC),
                        highlightedColor = Color(0xFF0077BB),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Важная информация",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Если вы не хотите задавать вопрос напрямую, вы можете посмотреть решения частых проблем в этом документе.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                NetFixButton(
                    text = "Решения проблем",
                    onClick = { handleLinkClick("https://github.com/rupleide/NetFixMobile/blob/main/TROUBLESHOOTING.md", "TROUBLESHOOTING.md") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "NetFix для Windows",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "NetFix изначально создавался как программа для компьютеров под управлением Windows 10/11. ПК-версия является одним из лучших инструментов для обхода сетевых ограничений на компьютере в один клик и без использования сторонних VPN.\n\nЕсли вам нужен стабильный доступ на ПК, вы можете скачать официальный клиент NetFix для ПК.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                NetFixButton(
                    text = "Открыть GitHub проекта для ПК",
                    onClick = { handleLinkClick("https://github.com/rupleide/NetFix", "GitHub NetFix (Windows)") },
                    modifier = Modifier.fillMaxWidth().focusProperties { down = navBarFocusRequester }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Приватность",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Приложение не собирает и не передаёт никакие данные пользователя.",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Copyright,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Лицензия и копирайты",
                        color = Color(0xFFF4F4F5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "NetFix является полностью свободным проектом с открытым исходным кодом и распространяется под лицензией GNU General Public License v3.0 (GPL-3.0).\n\nВыражаем огромную благодарность авторам оригинальных нативных решений, на которых базируется работа NetFix Android:",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                val openBbdUrl = "https://github.com/romanvht/ByeByeDPI"
                val bbdInteraction = remember { MutableInteractionSource() }
                val bbdPressed by bbdInteraction.collectIsPressedAsState()
                val bbdHovered by bbdInteraction.collectIsHoveredAsState()
                val bbdFocused by bbdInteraction.collectIsFocusedAsState()
                val bbdHighlighted = bbdPressed || bbdHovered || bbdFocused
                val bbdBg by animateColorAsState(if (bbdHighlighted) Color(0xFF38383A) else Color(0xFF242426), tween(150), label = "")
                val bbdBorder by animateColorAsState(if (bbdHighlighted) Color.White else Color(0x1AFFFFFF), tween(150), label = "")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bbdBg)
                        .border(1.dp, bbdBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = bbdInteraction,
                            indication = null
                        ) {
                            handleLinkClick(openBbdUrl, "ByeByeDPI GitHub")
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ByeByeDPI (GitHub)",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Разработчик: romanvht (обход DPI на уровне VPN)",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                    }
                }

                val openTgwsUrl = "https://github.com/amurcanov/tg-ws-proxy-android"
                val tgwsInteraction = remember { MutableInteractionSource() }
                val tgwsPressed by tgwsInteraction.collectIsPressedAsState()
                val tgwsHovered by tgwsInteraction.collectIsHoveredAsState()
                val tgwsFocused by tgwsInteraction.collectIsFocusedAsState()
                val tgwsHighlighted = tgwsPressed || tgwsHovered || tgwsFocused
                val tgwsBg by animateColorAsState(if (tgwsHighlighted) Color(0xFF38383A) else Color(0xFF242426), tween(150), label = "")
                val tgwsBorder by animateColorAsState(if (tgwsHighlighted) Color.White else Color(0x1AFFFFFF), tween(150), label = "")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tgwsBg)
                        .border(1.dp, tgwsBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = tgwsInteraction,
                            indication = null
                        ) {
                            handleLinkClick(openTgwsUrl, "TG WS Proxy GitHub")
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TG WS Proxy Android (GitHub)",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Разработчик: amurcanov (локальный прокси Telegram)",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

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
