package com.rupleide.netfix.ui.help

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.help.HelpActions
import com.rupleide.netfix.core.help.HelpAnswer
import com.rupleide.netfix.core.help.HelpAttempt
import com.rupleide.netfix.core.help.HelpFix
import com.rupleide.netfix.core.help.HelpFixKind
import com.rupleide.netfix.core.help.HelpFixRole
import com.rupleide.netfix.core.help.HelpLevel
import com.rupleide.netfix.core.help.HelpMeasure
import com.rupleide.netfix.core.help.HelpMeasureKind
import com.rupleide.netfix.core.help.HelpPlanner
import com.rupleide.netfix.core.help.HelpProbes
import com.rupleide.netfix.core.help.HelpReminderWorker
import com.rupleide.netfix.core.help.HelpReport
import com.rupleide.netfix.core.help.HelpScore
import com.rupleide.netfix.core.help.HelpSession
import com.rupleide.netfix.core.help.HelpSymptom
import com.rupleide.netfix.core.help.role
import com.rupleide.netfix.core.help.textFor
import com.rupleide.netfix.core.help.titleFor
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfInputField
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.requestFocusSafe
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay

private const val SHORT_LABEL = 10
private val savedCheckFormat = java.text.SimpleDateFormat("dd.MM в HH:mm", java.util.Locale.getDefault())
private const val TV_PRIMARY_FOCUS_DELAY_MS = 450L
private const val TV_PRIMARY_FOCUS_RETRY_MS = 250L
private const val TV_PRIMARY_FOCUS_ATTEMPTS = 8
private val Soft = Color(0xFFD4D4D8)
private val Line = Color(0x14FFFFFF)
private val Muted = Color(0x2EFFFFFF)

@Composable
private fun Modifier.tvPrimaryFocus(key: Any?): Modifier {
    if (!LocalIsTv.current) return this
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        delay(TV_PRIMARY_FOCUS_DELAY_MS)
        repeat(TV_PRIMARY_FOCUS_ATTEMPTS) {
            if (focused) return@LaunchedEffect
            requester.requestFocusSafe()
            delay(TV_PRIMARY_FOCUS_RETRY_MS)
        }
    }
    return this.focusRequester(requester).onFocusChanged { focused = it.hasFocus }
}

@Composable
private fun HCard(modifier: Modifier = Modifier, padding: Int = 16, gap: Int = 12, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(padding.dp),
        verticalArrangement = Arrangement.spacedBy(gap.dp)
    ) {
        content()
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Nf.TextSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
private fun Body(text: String, color: Color = Nf.TextSub, size: Int = 14) {
    Text(text, color = color, fontSize = size.sp, lineHeight = (size + 6).sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Title(text: String, size: Int = 20) {
    Text(text, color = Nf.Text, fontSize = size.sp, lineHeight = (size + 6).sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Dot(color: Color, size: Int = 8) {
    Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(color))
}

@Composable
private fun Pill(text: String, color: Color = Soft) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Nf.Surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Nf.TextSub,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

private fun scoreColor(score: HelpScore?, problems: Int): Color = when {
    score == null -> if (problems > 0) Nf.Red else Nf.Green
    score.success -> Nf.Green
    score.partial -> Nf.Yellow
    else -> Nf.Red
}

@Composable
private fun ScoreTile(score: HelpScore?, problems: Int) {
    val color = scoreColor(score, problems)
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (score != null && score.short.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(score.short, color = color, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(score.caption, color = Nf.TextSub, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        } else {
            Icon(if (problems > 0) Icons.Default.Warning else Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
    }
}

private fun levelColor(level: HelpLevel): Color = when (level) {
    HelpLevel.Ok -> Nf.Green
    HelpLevel.Warn -> Nf.Yellow
    HelpLevel.Bad -> Nf.Red
}

@Composable
fun HelpStatusCard(expandedByDefault: Boolean = false) {
    val baseline = HelpSession.baseline
    val findings = HelpSession.findings
    val problems = findings.filter { it.level != HelpLevel.Ok && it.text != baseline?.text }
    val fine = findings.filter { it.level == HelpLevel.Ok && it.text != baseline?.text }
    var showFine by remember { mutableStateOf(expandedByDefault) }
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .nfEnter(1)
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        ScoreTile(baseline, problems.size)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = baseline?.text ?: if (problems.isEmpty()) "Проблем не нашёл" else "Нашёл, что исправить",
                color = Nf.Text,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            problems.forEach { finding ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.padding(top = 6.dp)) { Dot(levelColor(finding.level)) }
                    Text(finding.text, color = Soft, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (showFine) {
                fine.forEach { finding ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.padding(top = 6.dp)) { Dot(Nf.Green) }
                        Text(finding.text, color = Nf.TextSub, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (fine.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFine = !showFine }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (showFine) "Скрыть" else "Остальное в порядке (${fine.size})",
                        color = Nf.TextSub,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(if (showFine) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun lastAttempt(fix: HelpFix): HelpAttempt? = HelpSession.attempts.lastOrNull { it.fix == fix }

private fun resultLabel(attempt: HelpAttempt): String = when (attempt.answer) {
    HelpAnswer.Worked -> "помогло"
    HelpAnswer.Applied -> "сделано"
    HelpAnswer.Manual -> "сделано"
    HelpAnswer.Same -> if (attempt.fix.role == HelpFixRole.Change) "не помогло, вернул" else "сделано"
    HelpAnswer.Worse -> "стало хуже, вернул"
    HelpAnswer.Skipped -> when {
        attempt.fix.role == HelpFixRole.Advice -> "прочитано"
        attempt.note != null -> "не получилось"
        else -> "пропущен"
    }
}

private fun positive(attempt: HelpAttempt?): Boolean =
    attempt != null && attempt.answer in setOf(HelpAnswer.Worked, HelpAnswer.Applied, HelpAnswer.Manual)

@Composable
private fun StepBullet(number: Int, state: Int, attempt: HelpAttempt?) {
    val size = 28.dp
    when {
        state < 0 && positive(attempt) -> Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Nf.Green.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Nf.Green, modifier = Modifier.size(16.dp))
        }
        state < 0 -> Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Nf.Surface),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.width(8.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Nf.TextSub))
        }
        else -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Nf.Sheet)
                .border(if (state == 0) 2.dp else 1.5.dp, if (state == 0) Nf.Text else Muted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = if (state == 0) Nf.Text else Nf.TextSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun measurable(): Boolean =
    HelpSession.symptom?.let { HelpMeasure.kindFor(it, HelpSession.facts) } != HelpMeasureKind.None

@Composable
fun HelpPlanCard(busyText: String?) {
    val context = LocalContext.current
    val queue = HelpSession.queue
    val current = HelpSession.index
    val appLabel = HelpSession.facts?.targetLabel
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Label("План ремонта")
        Label("${(current + 1).coerceAtMost(queue.size)} из ${queue.size}")
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .nfEnter(2)
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp)
    ) {
        queue.forEachIndexed { i, fix ->
            val state = i.compareTo(current)
            val attempt = lastAttempt(fix)
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.width(28.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    StepBullet(i + 1, state, attempt)
                    if (i < queue.lastIndex) {
                        Box(modifier = Modifier.padding(vertical = 4.dp).width(2.dp).weight(1f).background(Line))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(bottom = if (state == 0) 20.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        state < 0 -> Row(modifier = Modifier.heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(fix.titleFor(appLabel), color = Nf.TextSub, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            attempt?.let { Text(resultLabel(it), color = Nf.TextSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        state > 0 -> Row(modifier = Modifier.heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(fix.titleFor(appLabel), color = Soft, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (fix.role == HelpFixRole.Manual) Pill("вручную")
                        }
                        else -> CurrentStep(fix, appLabel, busyText, context)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentStep(fix: HelpFix, appLabel: String?, busyText: String?, context: android.content.Context) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) { Text(fix.titleFor(appLabel), color = Nf.Text, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold) }
        if (fix.role == HelpFixRole.Manual) Pill("вручную")
    }
    Body(HelpPlanner.detailFor(fix, HelpSession.facts) ?: fix.textFor(appLabel), Soft, 15)
    if (busyText != null) {
        val verifying = StrategyTestManager.isVerifying && StrategyTestManager.verifyTotal > 0
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (verifying) "Проверяю способы" else busyText, color = Soft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (verifying) Text("${StrategyTestManager.verifyDone} из ${StrategyTestManager.verifyTotal}", color = Nf.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (verifying) {
            SegmentedBar(StrategyTestManager.verifyDone, StrategyTestManager.verifyTotal)
        } else {
            NfProgress(fraction = 0f, indeterminate = true)
        }
        Body("Можно свернуть NetFix, работа не остановится.", size = 13)
        return
    }
    if (fix.kind != HelpFixKind.Advice && measurable()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(16.dp))
            Text(
                if (fix.role == HelpFixRole.Change) "Потом сам проверю. Не поможет, верну как было" else "Потом сам проверю, помогло ли",
                color = Nf.TextSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
    val savedCheckAt = remember(fix, HelpSession.index) { HelpActions.savedCheckAt(context, fix) }
    if (savedCheckAt != null) {
        val what = if (fix == HelpFix.TunnelDnsBest) "DNS" else "Способы"
        Body("$what уже проверяли ${savedCheckFormat.format(java.util.Date(savedCheckAt))}. Можно взять ту проверку или проверить заново.", Soft, 14)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NfButton("Взять прошлую проверку", onClick = { HelpSession.apply(context, useSaved = true) }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus(fix))
            NfButton("Проверить заново", onClick = { HelpSession.apply(context) }, style = NfButtonStyle.Secondary)
            NfButton("Пропустить", onClick = { HelpSession.skip(context) }, style = NfButtonStyle.Secondary)
        }
        return
    }
    val freshCheck = remember(fix, HelpSession.index) { HelpActions.hasFreshCheck(context, fix) }
    val label = when (fix.kind) {
        HelpFixKind.Auto -> "Исправить"
        HelpFixKind.Manual -> "Открыть настройки"
        HelpFixKind.Advice -> "Понятно"
    }
    when {
        fix.kind == HelpFixKind.Advice -> NfButton(label, onClick = { HelpSession.apply(context) }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus(fix))
        label.length <= SHORT_LABEL -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NfButton(label, onClick = { HelpSession.apply(context, useSaved = freshCheck) }, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1.2f).tvPrimaryFocus(fix))
            NfButton("Пропустить", onClick = { HelpSession.skip(context) }, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f))
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NfButton(label, onClick = { HelpSession.apply(context, useSaved = freshCheck) }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus(fix))
            NfButton("Пропустить", onClick = { HelpSession.skip(context) }, style = NfButtonStyle.Secondary)
        }
    }
}

@Composable
private fun SegmentedBar(done: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total.coerceIn(1, 20)) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            i < done -> Nf.Blue
                            i == done -> Nf.Blue.copy(alpha = 0.40f)
                            else -> Color(0x1AFFFFFF)
                        }
                    )
            )
        }
    }
}

@Composable
fun HelpNoticeLine(text: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 6.dp)) { Dot(color) }
        Text(text, color = Soft, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ResultBox(passed: Boolean, text: String, detail: String?) {
    val color = if (passed) Nf.Green else Nf.Red
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (passed) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, color = Nf.Text, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        detail?.let { Box(modifier = Modifier.padding(start = 30.dp)) { Body(it, size = 13) } }
    }
}

private fun appliedParts(fix: HelpFix): Pair<String, List<String>> {
    val note = HelpSession.appliedNote ?: return "Сделал: «${fix.titleFor(HelpSession.facts?.targetLabel)}»" to emptyList()
    val title = note.substringBefore(": ")
    val pills = note.substringAfter(": ", "").split(" · ", ", ").filter { it.isNotBlank() }
    return title to pills
}

@Composable
fun HelpVerifyView(onLater: () -> Unit) {
    val context = LocalContext.current
    val fix = HelpSession.currentFix ?: return
    val target = remember(HelpSession.facts) { verifyPackage(context) }
    val appName = target?.let { HelpProbes.appLabel(context, it) }
    val after = HelpSession.scoreAfterFix
    val before = HelpSession.scoreBeforeFix
    val (title, pills) = appliedParts(fix)
    HCard(modifier = Modifier.nfEnter(1)) {
        Label("Шаг ${HelpSession.index + 1} из ${HelpSession.queue.size} · сделано")
        Text(title, color = Nf.Text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
        if (pills.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { pills.take(3).forEach { Pill(it) } }
        }
        HelpSession.autoCheck?.let { check ->
            val detail = if (before != null && after != null && before.short.isNotEmpty() && after.short.isNotEmpty() && before.short != after.short) {
                if (before.caption != after.caption || before.caption.endsWith("/с")) {
                    "Было ${before.short} ${before.caption}, стало ${after.short} ${after.caption}"
                } else {
                    "Было ${before.short}, стало ${after.short}"
                }
            } else null
            ResultBox(check.passed, check.text, detail)
        }
    }
    HCard(modifier = Modifier.nfEnter(2), gap = 10) {
        Title("Работает?")
        val tv = LocalIsTv.current
        Body(
            when {
                tv && appName != null -> "Откройте $appName и включите любое видео. Если не идёт, закройте $appName и откройте заново."
                appName != null -> "Откройте $appName и проверьте сами."
                HelpSession.symptom == HelpSymptom.StopsInBackground ->
                    "Поработайте с ${if (tv) "приставкой" else "телефоном"}. Если NetFix больше не выключается, нажмите «Да»."
                else -> "Проверьте и ответьте."
            }
        )
        if (target != null) {
            NfButton("Открыть $appName", onClick = { launchApp(context, target) }, style = NfButtonStyle.Secondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NfButton("Да", onClick = { HelpSession.answer(context, HelpAnswer.Worked) }, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f).tvPrimaryFocus(fix))
            NfButton("Нет", onClick = { HelpSession.answer(context, HelpAnswer.Same) }, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f))
        }
        if (fix.role == HelpFixRole.Change) {
            NfButton(HelpAnswer.Worse.label, onClick = { HelpSession.answer(context, HelpAnswer.Worse) }, style = NfButtonStyle.Secondary)
            Text("«Нет» и «Стало хуже» вернут как было", color = Nf.TextSub, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        if (HelpSession.symptom == HelpSymptom.TelegramDrops) {
            NfButton(
                "Напомнить через ${HelpReminderWorker.DELAY_MINUTES} минут",
                onClick = {
                    HelpReminderWorker.schedule(context, HelpSymptom.TelegramDrops)
                    onLater()
                },
                style = NfButtonStyle.Secondary
            )
        }
    }
    NextSteps()
}

@Composable
private fun NextSteps() {
    val queue = HelpSession.queue
    val from = HelpSession.index + 1
    if (from > queue.lastIndex) return
    val appLabel = HelpSession.facts?.targetLabel
    Label("Дальше, если не поможет", Modifier.padding(start = 4.dp, top = 4.dp))
    HCard(modifier = Modifier.nfEnter(3), padding = 0, gap = 0) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            queue.drop(from).forEachIndexed { i, fix ->
                if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Line))
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepBullet(from + i + 1, 1, null)
                    Text(fix.titleFor(appLabel), color = Soft, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.weight(1f))
                    if (fix.role == HelpFixRole.Manual) Pill("вручную")
                }
            }
        }
    }
}

@Composable
private fun NumberedLine(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(Nf.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Nf.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.weight(1f).padding(top = 2.dp)) { Body(text, Soft) }
    }
}

@Composable
fun HelpWaitingView() {
    val context = LocalContext.current
    val fix = HelpSession.currentFix ?: return
    val telegram = HelpSession.symptom == HelpSymptom.TelegramNoConnect && fix.role != HelpFixRole.Manual
    val check = HelpSession.autoCheck
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(fix, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            HelpSession.checkNow(context)
        }
    }
    HCard(modifier = Modifier.nfEnter(1), gap = 14) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("Шаг ${HelpSession.index + 1} из ${HelpSession.queue.size}")
            if (fix.role == HelpFixRole.Manual) Pill("вручную")
        }
        Title(fix.titleFor(HelpSession.facts?.targetLabel))
        val waitText = when {
            HelpSession.checking -> "Проверяю…"
            check != null && !check.passed -> check.text
            telegram -> "Жду, пока откроете Telegram. Проверю сам, как вернётесь."
            else -> "Жду, пока сделаете в настройках. Проверю сам, как вернётесь."
        }
        val shape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Nf.Yellow.copy(alpha = 0.08f))
                .border(1.dp, Nf.Yellow.copy(alpha = 0.24f), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.padding(top = 6.dp)) { Dot(Nf.Yellow, 10) }
            Text(waitText, color = Color(0xFFE4E4E7), fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                telegram -> {
                    NumberedLine(1, "Откройте Telegram, если нужно, нажмите «Подключить»")
                    NumberedLine(2, "Подождите секунд 10, пока загрузятся чаты")
                    NumberedLine(3, "Вернитесь сюда")
                }
                else -> Body(fix.textFor(HelpSession.facts?.targetLabel), Soft)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val telegramApp = if (telegram) HelpReport.installedTelegram(context) else null
            if (telegram) {
                telegramApp?.let { tg ->
                    NfButton("Открыть Telegram", onClick = { launchApp(context, tg) }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus(fix))
                }
            } else {
                NfButton("Открыть настройки ещё раз", onClick = { HelpSession.apply(context) }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus(fix))
            }
            NfButton(
                "Пропустить",
                onClick = { HelpSession.skip(context) },
                style = NfButtonStyle.Secondary,
                modifier = if (telegram && telegramApp == null) Modifier.tvPrimaryFocus(fix) else Modifier
            )
        }
    }
}

@Composable
private fun AttemptRow(attempt: HelpAttempt) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (positive(attempt) || (attempt.answer == HelpAnswer.Same && attempt.fix.role != HelpFixRole.Change)) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Nf.Green, modifier = Modifier.size(18.dp))
        } else {
            Box(modifier = Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(8.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Nf.TextSub))
            }
        }
        Text(
            attempt.fix.titleFor(HelpSession.facts?.targetLabel),
            color = if (positive(attempt)) Nf.Text else Soft,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Text(resultLabel(attempt), color = Nf.TextSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun AttemptsCard(title: String?) {
    val attempts = HelpSession.attempts.toList()
    if (attempts.isEmpty()) return
    title?.let { Label(it, Modifier.padding(start = 4.dp, top = 4.dp)) }
    HCard(padding = 0, gap = 0) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            attempts.forEachIndexed { i, attempt ->
                if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Line))
                AttemptRow(attempt)
            }
        }
    }
}

@Composable
private fun RevertLink(beforeTakenAt: Long?, onRevert: () -> Unit) {
    if (beforeTakenAt == null) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        TextLink("Вернуть как было до ремонта", onRevert)
        Text("Можно в течение 7 дней", color = Nf.TextSub, fontSize = 13.sp, modifier = Modifier.padding(top = 0.dp))
    }
}

@Composable
private fun RoundBadge(color: Color, icon: ImageVector, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.30f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
fun HelpSolvedView(beforeTakenAt: Long?, onRevert: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val target = remember(HelpSession.facts) { verifyPackage(context) }
    val appName = target?.let { HelpProbes.appLabel(context, it) }
    HCard(modifier = Modifier.nfEnter(1), padding = 20, gap = 10) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundBadge(Nf.Green, Icons.Default.Check, 72)
            Text("Готово!", color = Nf.Text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            val result = HelpSession.autoCheck?.takeIf { it.passed }?.text ?: HelpSession.baseline?.takeIf { it.success }?.text
            result?.let { Text(it, color = Soft, fontSize = 15.sp, textAlign = TextAlign.Center) }
        }
    }
    AttemptsCard(null)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (target != null) {
            NfButton("Открыть $appName", onClick = { launchApp(context, target) }, style = NfButtonStyle.Secondary)
        }
        NfButton("Закрыть", onClick = onClose, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus("solved"))
    }
    RevertLink(beforeTakenAt, onRevert)
}

@Composable
fun HelpFailedView(beforeTakenAt: Long?, onRevert: () -> Unit, onClose: () -> Unit) {
    val tried = HelpSession.attempts.count { it.answer != HelpAnswer.Skipped }
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .nfEnter(1)
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        val checkPasses = HelpSession.baseline?.success == true
        RoundBadge(if (checkPasses) Nf.Blue else Nf.Red, if (checkPasses) Icons.Default.Check else Icons.Default.Close, 48)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val now = HelpSession.baseline
            val total = HelpSession.queue.size
            Text(if (now?.success == true) "По проверке всё работает" else "Не получилось", color = Nf.Text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Body(
                buildString {
                    append(if (tried > 0) "Сделал шагов: $tried из $total." else "Все шаги пропущены.")
                    when {
                        now == null -> Unit
                        now.success -> append(" ${now.text}. Если у вас всё равно не так, напишите в поддержку, разберёмся.")
                        else -> append(" ${now.text}.")
                    }
                },
                Soft
            )
        }
    }
    HelpSession.notice?.let { HelpNoticeLine(it, Nf.Yellow) }
    AttemptsCard("Что пробовал")
    HCard(gap = 10) {
        Body("Напишите в поддержку, разберёмся вручную. Что я нашёл и что пробовал, добавлю в сообщение сам.", Soft)
        NfButton("Написать в поддержку", onClick = { HelpSession.openReport() }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus("failed"))
        NfButton("Закрыть", onClick = onClose, style = NfButtonStyle.Secondary)
    }
    RevertLink(beforeTakenAt, onRevert)
}

@Composable
fun HelpNothingView(onClose: () -> Unit) {
    HelpStatusCard(expandedByDefault = true)
    HCard(modifier = Modifier.nfEnter(2), gap = 10) {
        Title("Исправлять нечего")
        Body("Всё, что я умею проверять, в порядке. Если всё равно не работает, напишите в поддержку, я добавлю в сообщение то, что проверил.")
        NfButton("Написать в поддержку", onClick = { HelpSession.openReport() }, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus("nothing"))
        NfButton("Закрыть", onClick = onClose, style = NfButtonStyle.Secondary)
    }
}

@Composable
private fun IncludedLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Nf.Green, modifier = Modifier.size(16.dp))
        Text(text, color = Soft, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun HelpSupportForm(onSend: () -> Unit) {
    HCard(modifier = Modifier.nfEnter(1), gap = 8) {
        Text("Что случилось?", color = Nf.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        NfInputField(
            value = HelpSession.userNote,
            onValueChange = { HelpSession.userNote = it.take(500) },
            placeholder = "Например: лента грузится, видео нет"
        )
        Body("Можно коротко, остальное добавлю сам.", size = 13)
    }
    HCard(modifier = Modifier.nfEnter(2), gap = 2) {
        Label("Добавлю к сообщению", Modifier.padding(bottom = 4.dp))
        HelpSession.symptom?.takeIf { it != HelpSymptom.Other }?.let { IncludedLine("Проблему: ${it.title}") }
        HelpSession.baseline?.let { IncludedLine("Что нашёл: ${it.text}") }
        val tried = HelpSession.attempts.count { it.answer != HelpAnswer.Skipped }
        if (tried > 0) IncludedLine("Что пробовал: шагов $tried")
        IncludedLine("Модель устройства и версию Android")
        IncludedLine("Настройки NetFix")
        IncludedLine("Журнал с момента, как открыли помощник")
    }
    val tv = LocalIsTv.current
    NfButton(if (tv) "Показать QR-код поддержки" else "Открыть чат поддержки", onClick = onSend, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus("support"))
    Text(
        if (tv) {
            "Наведите камеру телефона на QR-код, откроется чат @${HelpReport.SUPPORT_TELEGRAM} с готовым текстом. Текст можно поправить перед отправкой. IP-адреса и ключи скрыты."
        } else {
            "Откроется Telegram, чат @${HelpReport.SUPPORT_TELEGRAM}. Текст можно поправить перед отправкой. IP-адреса и ключи скрыты."
        },
        color = Nf.TextSub,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    )
}

@Composable
fun HelpSupportSent(openedChat: Boolean, onRetry: () -> Unit, onFinish: () -> Unit) {
    HCard(modifier = Modifier.nfEnter(1), padding = 20, gap = 10) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val tv = LocalIsTv.current
            RoundBadge(Nf.Blue, if (openedChat || tv) Icons.Default.Send else Icons.Default.ContentCopy, 64)
            Text(
                when {
                    openedChat -> "Осталось отправить"
                    tv -> "Отправьте с телефона"
                    else -> "Сообщение скопировано"
                },
                color = Nf.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                when {
                    openedChat -> "Текст уже в чате @${HelpReport.SUPPORT_TELEGRAM}, нажмите в Telegram «Отправить». Сначала ответит бот, если нужен человек, напишите «!разработчик»."
                    tv -> "Наведите камеру телефона на QR-код. Откроется чат @${HelpReport.SUPPORT_TELEGRAM} с готовым сообщением, нажмите «Отправить»."
                    else -> "Откройте Telegram, найдите @${HelpReport.SUPPORT_TELEGRAM} и вставьте сообщение."
                },
                color = Soft,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
    NfButton("Готово", onClick = onFinish, style = NfButtonStyle.Secondary, modifier = Modifier.tvPrimaryFocus(openedChat))
    TextLink(
        when {
            openedChat -> "Чат не открылся? Ещё раз"
            LocalIsTv.current -> "Показать QR-код ещё раз"
            else -> "Открыть чат поддержки"
        },
        onRetry
    )
}
