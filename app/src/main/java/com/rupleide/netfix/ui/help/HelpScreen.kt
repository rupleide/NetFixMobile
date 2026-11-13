package com.rupleide.netfix.ui.help

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.repeatOnLifecycle
import com.rupleide.netfix.R
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.launchIntentWithLeanback
import com.rupleide.netfix.core.help.HelpActions
import com.rupleide.netfix.core.help.HelpAnswer
import com.rupleide.netfix.core.help.HelpFix
import com.rupleide.netfix.core.help.HelpFixKind
import com.rupleide.netfix.core.help.HelpLevel
import com.rupleide.netfix.core.help.HelpPlanner
import com.rupleide.netfix.core.help.HelpProbes
import com.rupleide.netfix.core.help.HelpReminderWorker
import com.rupleide.netfix.core.help.HelpRepairStore
import com.rupleide.netfix.core.help.HelpReport
import com.rupleide.netfix.core.help.HelpReportStep
import com.rupleide.netfix.core.help.HelpSession
import com.rupleide.netfix.core.help.HelpStage
import com.rupleide.netfix.core.help.HelpSymptom
import com.rupleide.netfix.core.help.role
import com.rupleide.netfix.core.help.textFor
import com.rupleide.netfix.core.help.titleFor
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfCornerAura
import com.rupleide.netfix.ui.components.NfDivider
import com.rupleide.netfix.ui.components.NfInputField
import com.rupleide.netfix.ui.components.NfMiniButton
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.NfRow
import com.rupleide.netfix.ui.components.NfSection
import com.rupleide.netfix.ui.components.QrLinkDialog
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.requestFocusSafe
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TV_FIRST_ITEM_FOCUS_DELAY_MS = 450L

private fun symptomIcon(symptom: HelpSymptom): ImageVector = when (symptom) {
    HelpSymptom.YoutubeDown -> Icons.Default.PlayCircle
    HelpSymptom.YoutubeSlow -> Icons.Default.Speed
    HelpSymptom.YoutubeFromDesktop -> Icons.Default.Smartphone
    HelpSymptom.YoutubeBrowser -> Icons.Default.Language
    HelpSymptom.TelegramNoConnect, HelpSymptom.TelegramDrops -> Icons.Default.SyncProblem
    HelpSymptom.Discord -> Icons.Default.Headset
    HelpSymptom.OtherApp -> Icons.Default.Apps
    HelpSymptom.BrokeApp -> Icons.Default.BrokenImage
    HelpSymptom.WontStart -> Icons.Default.PowerSettingsNew
    HelpSymptom.WorseAfterUpdate -> Icons.Default.History
    HelpSymptom.Tv -> Icons.Default.Tv
    HelpSymptom.ScanZero -> Icons.Default.SearchOff
    HelpSymptom.StopsInBackground -> Icons.Default.BatteryAlert
    HelpSymptom.Other -> Icons.Default.QuestionMark
}

private fun searchWords(symptom: HelpSymptom): String = when (symptom) {
    HelpSymptom.YoutubeDown, HelpSymptom.YoutubeSlow, HelpSymptom.YoutubeFromDesktop -> "ютуб ютьюб youtube видео ролики шортс shorts тормозит лагает"
    HelpSymptom.YoutubeBrowser -> "ютуб youtube браузер хром chrome сайт"
    HelpSymptom.TelegramNoConnect, HelpSymptom.TelegramDrops -> "телеграм телега тг tg telegram прокси соединение подключение"
    HelpSymptom.Discord -> "дискорд дс discord звонки голос войс"
    HelpSymptom.OtherApp -> "инстаграм инста instagram ватсап whatsapp твиттер x чатгпт chatgpt спотифай spotify фейсбук приложение"
    HelpSymptom.BrokeApp -> "сломалось приложение спотифай spotify сайт 4pda не грузит"
    HelpSymptom.WontStart -> "не включается не запускается кнопка порт впн vpn"
    HelpSymptom.WorseAfterUpdate -> "обновление обновил хуже версия"
    HelpSymptom.Tv -> "тв телевизор приставка смарттуб smarttube кабель ethernet"
    HelpSymptom.ScanZero -> "подбор скан стратегии способы 0 ноль"
    HelpSymptom.StopsInBackground -> "выключается фон батарея сам отключается"
    HelpSymptom.Other -> "другое не знаю поддержка разработчик отчёт"
}

private fun matches(symptom: HelpSymptom, query: String): Boolean {
    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val haystack = (symptom.title + " " + symptom.subtitle + " " + searchWords(symptom)).lowercase()
    return words.all { it in haystack }
}

private val timeFormat = SimpleDateFormat("dd.MM в HH:mm", Locale.getDefault())

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { HelpSession.onOpened(context) }
    val close: () -> Unit = {
        HelpSession.onClosed(context)
        onBack()
    }
    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val bottomReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()
    val scrollState = rememberScrollState()
    val tv = LocalIsTv.current
    val entryFocus = remember { FocusRequester() }
    var beforeTakenAt by remember { mutableStateOf(HelpRepairStore.takenAt(context)) }
    var revertNotice by remember { mutableStateOf<String?>(null) }
    var qrUrl by remember { mutableStateOf<String?>(null) }

    val stage = HelpSession.stage
    LaunchedEffect(stage, HelpSession.index, HelpSession.reportStep) {
        scrollState.animateScrollTo(0)
        beforeTakenAt = HelpRepairStore.takenAt(context)
    }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        HelpSession.consentHandled(context, result.resultCode == android.app.Activity.RESULT_OK)
    }
    LaunchedEffect(HelpSession.pendingConsent) {
        HelpSession.pendingConsent?.let { intent ->
            try {
                consentLauncher.launch(intent)
            } catch (_: Exception) {
                HelpSession.consentHandled(context, false)
            }
        }
    }

    val handleBack: () -> Unit = {
        when {
            HelpSession.isBusy -> Unit
            stage == HelpStage.Pick -> close()
            else -> HelpSession.back()
        }
    }
    BackHandler(onBack = handleBack)

    val revertAll: () -> Unit = {
        if (HelpSession.revertAll(context)) {
            HelpSession.reset()
            beforeTakenAt = null
            revertNotice = "Готово, всё вернул как было"
        }
    }

    val pickSymptom: (HelpSymptom) -> Unit = {
        revertNotice = null
        HelpSession.choose(context, it)
    }
    val stageBody: @Composable (Boolean) -> Unit = { withStatus ->
        when (stage) {
            HelpStage.Pick -> PickStage(beforeTakenAt = beforeTakenAt, onRevert = revertAll, onPick = pickSymptom)
            HelpStage.PickApp -> PickAppStage(onPick = { HelpSession.pickApp(context, it) })
            HelpStage.Checking -> BusyStage(if (tv) "Проверяю приставку и NetFix" else "Проверяю телефон и NetFix")
            HelpStage.Applying -> {
                if (withStatus) HelpStatusCard()
                HelpPlanCard(
                    busyText = when {
                        HelpSession.busyText != null -> HelpSession.busyText.orEmpty()
                        HelpActions.progressText != null -> HelpActions.progressText.orEmpty()
                        StrategyTestManager.isTesting -> StrategyTestManager.currentProgress.ifBlank { "Подбираю способы" }
                        else -> "Делаю и проверяю"
                    }
                )
            }
            HelpStage.Fix -> {
                HelpSession.notice?.let { HelpNoticeLine(it, Nf.Yellow) }
                HelpSession.errorText?.let { HelpNoticeLine(it, Nf.Red) }
                if (withStatus) HelpStatusCard()
                HelpPlanCard(busyText = null)
            }
            HelpStage.Verify -> if (HelpSession.waitingForUser) HelpWaitingView() else HelpVerifyView(onLater = onBack)
            HelpStage.Done -> {
                val closeDone: () -> Unit = {
                    HelpSession.reset()
                    close()
                }
                when {
                    HelpSession.solved -> HelpSolvedView(beforeTakenAt, revertAll, closeDone)
                    HelpSession.queue.isEmpty() -> HelpNothingView(closeDone)
                    else -> HelpFailedView(beforeTakenAt, revertAll, closeDone)
                }
            }
            HelpStage.Report -> ReportStage(
                onShowQr = { reportText -> qrUrl = HelpReport.supportLinkWithText(reportText) },
                onFinish = {
                    HelpSession.finishReport()
                    close()
                }
            )
        }
    }

    if (tv) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Nf.Bg)
        ) {
            NfCornerAura(Nf.Bright.copy(alpha = 0.056f))
            com.rupleide.netfix.ui.components.NfTvTwoPane(
                title = HelpSession.symptom?.takeIf { stage != HelpStage.Pick }?.title ?: "Помощник",
                rightTitle = tvRightTitle(stage),
                rightScroll = scrollState,
                left = {
                    HelpTvSidePane(
                        stage = stage,
                        beforeTakenAt = beforeTakenAt,
                        onRevert = revertAll,
                        onShowQr = { qrUrl = HelpReport.SUPPORT_LINK }
                    )
                },
                right = {
                    revertNotice?.let { Notice(it, Nf.GreenText) }
                    if (stage == HelpStage.Pick) TvSymptomGrid(onPick = pickSymptom) else stageBody(false)
                }
            )
        }
        qrUrl?.let { url ->
            QrLinkDialog(url = url, label = "Поддержка NetFix", onDismiss = { qrUrl = null }, displayText = "t.me/${HelpReport.SUPPORT_TELEGRAM}")
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Nf.Bg),
        contentAlignment = Alignment.TopCenter
    ) {
        NfCornerAura(Nf.Bright.copy(alpha = 0.056f))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .focusRequester(entryFocus)
                        .nfTvFocus(corner = 12.dp, grow = 1.05f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = handleBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Назад", tint = Nf.Text, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = HelpSession.symptom?.takeIf { stage != HelpStage.Pick }?.title ?: "Помощник",
                    color = Nf.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .nfEnter(0)
                )
            }

            revertNotice?.let { Notice(it, Nf.GreenText) }

            stageBody(true)
        }
    }

    qrUrl?.let { url ->
        QrLinkDialog(url = url, label = "Поддержка NetFix", onDismiss = { qrUrl = null })
    }
}

@Composable
private fun Notice(text: String, color: Color) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(14.dp)
    )
}

@Composable
private fun BodyText(text: String, color: Color = Nf.TextSub) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun CardTitle(text: String, color: Color = Nf.Text) {
    Text(text, color = color, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun RevertRow(takenAt: Long, onRevert: () -> Unit) {
    NfSection(title = null, modifier = Modifier.nfEnter(1)) {
        NfRow(
            title = "Вернуть как было",
            subtitle = "Помощник менял настройки ${timeFormat.format(Date(takenAt))}",
            icon = Icons.Default.Restore,
            onClick = onRevert,
            trailing = { NfMiniButton("Вернуть", onClick = onRevert) }
        )
    }
}

@Composable
private fun PickStage(beforeTakenAt: Long?, onRevert: () -> Unit, onPick: (HelpSymptom) -> Unit) {
    val tv = LocalIsTv.current
    var query by remember { mutableStateOf("") }
    if (!tv) {
        NfInputField(
            value = query,
            onValueChange = { query = it.take(40) },
            placeholder = "Поиск: YouTube, Telegram, Instagram…",
            modifier = Modifier.nfEnter(1),
            trailing = { SearchTrailing(query) { query = "" } }
        )
    }
    if (beforeTakenAt != null && query.isBlank()) RevertRow(beforeTakenAt, onRevert)
    val ordered = if (tv) {
        listOf(HelpSymptom.Tv) + HelpSymptom.entries.filter { it != HelpSymptom.Tv && it != HelpSymptom.YoutubeFromDesktop && it != HelpSymptom.YoutubeBrowser }
    } else {
        HelpSymptom.entries.toList()
    }
    val symptoms = ordered.filter { matches(it, query) }.ifEmpty { listOf(HelpSymptom.Other) }
    val firstSymptomFocus = remember { FocusRequester() }
    if (tv) {
        LaunchedEffect(Unit) {
            delay(TV_FIRST_ITEM_FOCUS_DELAY_MS)
            firstSymptomFocus.requestFocusSafe()
        }
    }
    NfSection(title = "Что не работает?", modifier = Modifier.nfEnter(2)) {
        symptoms.forEachIndexed { index, symptom ->
            if (index > 0) NfDivider()
            val telegram = symptom == HelpSymptom.TelegramNoConnect || symptom == HelpSymptom.TelegramDrops
            NfRow(
                title = symptom.title,
                modifier = if (tv && index == 0) Modifier.focusRequester(firstSymptomFocus) else Modifier,
                subtitle = symptom.subtitle,
                icon = symptomIcon(symptom).takeUnless { telegram },
                iconPainter = if (telegram) painterResource(R.drawable.ic_telegram) else null,
                onClick = { onPick(symptom) },
                trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

private fun tvRightTitle(stage: HelpStage): String = when (stage) {
    HelpStage.Pick -> "Что не работает?"
    HelpStage.PickApp -> "Какое приложение?"
    HelpStage.Checking -> "Проверка"
    HelpStage.Applying, HelpStage.Fix -> "Что сделать"
    HelpStage.Verify -> "Проверьте"
    HelpStage.Done -> "Итог"
    HelpStage.Report -> "Поддержка"
}

private val tvSymptoms: List<HelpSymptom>
    get() = listOf(HelpSymptom.Tv) + HelpSymptom.entries.filter {
        it != HelpSymptom.Tv && it != HelpSymptom.YoutubeFromDesktop && it != HelpSymptom.YoutubeBrowser
    }

@Composable
private fun HelpTvSidePane(stage: HelpStage, beforeTakenAt: Long?, onRevert: () -> Unit, onShowQr: () -> Unit) {
    when (stage) {
        HelpStage.Pick, HelpStage.PickApp -> {
            BodyText("Выберите, что не работает. NetFix проверит приставку и предложит, что исправить. Без вашего согласия ничего не меняется.")
            if (beforeTakenAt != null) RevertRow(beforeTakenAt, onRevert)
            NfButton(
                text = "QR-код поддержки",
                style = NfButtonStyle.Secondary,
                onClick = onShowQr
            )
            BodyText("Наведите камеру телефона на QR-код и напишите в поддержку, если помощник не справится.")
        }
        HelpStage.Checking -> BodyText("Проверяю интернет, NetFix и DNS. Это несколько секунд.")
        HelpStage.Done -> HelpStatusCard(expandedByDefault = true)
        else -> {
            HelpStatusCard(expandedByDefault = true)
            if (beforeTakenAt != null) RevertRow(beforeTakenAt, onRevert)
        }
    }
}

@Composable
private fun TvSymptomGrid(onPick: (HelpSymptom) -> Unit) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(TV_FIRST_ITEM_FOCUS_DELAY_MS)
        firstFocus.requestFocusSafe()
    }
    tvSymptoms.chunked(2).forEachIndexed { rowIndex, pair ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .nfEnter(rowIndex + 1),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEachIndexed { columnIndex, symptom ->
                TvSymptomTile(
                    symptom = symptom,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (rowIndex == 0 && columnIndex == 0) Modifier.focusRequester(firstFocus) else Modifier),
                    onClick = { onPick(symptom) }
                )
            }
            if (pair.size == 1) androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TvSymptomTile(symptom: HelpSymptom, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val telegram = symptom == HelpSymptom.TelegramNoConnect || symptom == HelpSymptom.TelegramDrops
    Row(
        modifier = modifier
            .height(76.dp)
            .nfTvFocus(corner = 18.dp, inside = true)
            .clip(shape)
            .background(Nf.Surface)
            .border(1.dp, Nf.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (telegram) {
            Icon(painterResource(R.drawable.ic_telegram), contentDescription = null, tint = Nf.Text, modifier = Modifier.size(22.dp))
        } else {
            Icon(symptomIcon(symptom), contentDescription = null, tint = Nf.Text, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = symptom.title,
                color = Nf.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = symptom.subtitle,
                color = Nf.TextSub,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchTrailing(query: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (query.isNotEmpty()) Modifier.clickable(onClick = onClear) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (query.isEmpty()) Icons.Default.Search else Icons.Default.Close,
            contentDescription = if (query.isEmpty()) null else "Очистить",
            tint = Nf.TextSub,
            modifier = Modifier.size(20.dp)
        )
    }
}

private data class AppChoice(val packageName: String, val label: String)

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    val loadedIcon = icon
    if (loadedIcon != null) {
        Image(bitmap = loadedIcon, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
    } else {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Nf.Surface))
    }
}

@Composable
private fun PickAppStage(onPick: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val popular = remember {
        (listOfNotNull(com.rupleide.netfix.core.YoutubeClient.chosenPackage(context)) + HelpPlanner.popularApps)
            .distinct()
            .filter { HelpProbes.isInstalled(context, it) }
            .map { AppChoice(it, HelpProbes.appLabel(context, it)) }
    }
    val all by produceState<List<AppChoice>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != context.packageName }
                .map { AppChoice(it, HelpProbes.appLabel(context, it)) }
                .sortedBy { it.label.lowercase() }
        }
    }
    NfInputField(
        value = query,
        onValueChange = { query = it.take(40) },
        placeholder = "Найти приложение",
        modifier = Modifier.nfEnter(1),
        trailing = { SearchTrailing(query) { query = "" } }
    )
    val rowFor: @Composable (AppChoice) -> Unit = { app ->
        NfRow(
            title = app.label,
            onClick = { onPick(app.packageName) },
            leading = { AppIcon(app.packageName) },
            trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(18.dp)) }
        )
    }
    val needle = query.trim().lowercase()
    if (needle.isEmpty() && popular.isNotEmpty()) {
        NfSection(title = "Частые", modifier = Modifier.nfEnter(2)) {
            popular.forEachIndexed { index, app ->
                if (index > 0) NfDivider()
                rowFor(app)
            }
        }
    }
    val list = all
    if (list == null) {
        NfProgress(fraction = 0f, indeterminate = true)
    } else {
        val shown = list.filter { needle.isEmpty() || needle in it.label.lowercase() }
        if (shown.isNotEmpty()) {
            NfSection(title = if (needle.isEmpty()) "Все приложения" else "Найдено") {
                shown.forEachIndexed { index, app ->
                    if (index > 0) NfDivider()
                    rowFor(app)
                }
            }
        }
    }
}

@Composable
private fun BusyStage(text: String) {
    Card {
        Text(text, color = Nf.Text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
        NfProgress(fraction = 0f, indeterminate = true)
    }
}

internal fun verifyPackage(context: android.content.Context): String? {
    val facts = HelpSession.facts
    return when (HelpSession.symptom) {
        HelpSymptom.TelegramNoConnect, HelpSymptom.TelegramDrops -> HelpReport.installedTelegram(context)
        HelpSymptom.YoutubeBrowser -> facts?.browserPackage
        else -> facts?.targetPackage
    }
}

internal fun launchApp(context: android.content.Context, packageName: String) {
    val intent = context.packageManager.launchIntentWithLeanback(packageName) ?: return
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

@Composable
private fun ReportStage(onShowQr: (String) -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    val tv = LocalIsTv.current
    var sent by remember { mutableStateOf(false) }
    var openedChat by remember { mutableStateOf(false) }
    val sendNow: () -> Unit = {
        val text = HelpSession.supportText(context)
        HelpReport.copyText(context, text)
        val hasTelegram = HelpReport.installedTelegram(context) != null
        val opened = !tv && HelpReport.openSupportChat(context, text)
        openedChat = opened && hasTelegram
        if (!opened) onShowQr(text)
        sent = true
    }
    LaunchedEffect(HelpSession.reportStep) {
        if (HelpSession.reportStep == HelpReportStep.Ready && !sent) sendNow()
    }
    when (HelpSession.reportStep) {
        HelpReportStep.Consent -> HelpSupportForm(onSend = { HelpSession.buildReport(context) })
        HelpReportStep.Building -> BusyStage("Готовлю сообщение")
        HelpReportStep.Ready -> HelpSupportSent(openedChat = openedChat, onRetry = sendNow, onFinish = onFinish)
    }
}
