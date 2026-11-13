package com.rupleide.netfix.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.core.dnscheck.DnsCheckResult
import com.rupleide.netfix.core.dnscheck.TunnelDnsCheck
import com.rupleide.netfix.core.dpibypass.StrategyOrdering
import com.rupleide.netfix.core.help.HostVerdict
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfBottomSheet
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfProgress
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.requestFocusSafe
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import com.rupleide.netfix.ui.components.rememberNetFixHaptics

data class TunnelDnsChoice(
    val key: String,
    val label: String,
    val address: String,
    val selected: Boolean,
    val systemDns: Boolean = false,
    val encrypted: Boolean = false,
    val server: suspend () -> List<String>
)

private const val TV_FOCUS_DELAY_MS = 520L
private const val SORT_DNS_KEY = "tunnel_dns_sort_keys"
private const val DNS_SORT_BEST = "best"
private const val DNS_SORT_SPEED = "speed"
private const val DNS_SORT_DEFAULT_KEYS = "best,speed"
private val ALL_DNS_SORTS = listOf(DNS_SORT_BEST, DNS_SORT_SPEED) + TunnelDnsCheck.targets.map { "target:${it.label}" }

private data class DnsVerdict(val label: String, val textColor: Color, val dotColor: Color)

private fun dnsVerdictOf(result: DnsCheckResult?): DnsVerdict = when {
    result == null -> DnsVerdict("Не проверен", Nf.TextSub, Nf.Muted)
    result.failure != null && result.verdicts.isEmpty() -> DnsVerdict("Не проверен", Nf.TextSub, Nf.Muted)
    result.okTargets == result.totalTargets -> DnsVerdict("Работает", Nf.Green, Nf.Green)
    result.okTargets > 0 -> DnsVerdict("Работает частично", Nf.Yellow, Nf.Yellow)
    else -> DnsVerdict("Не работает", Nf.RedText, Nf.Red)
}

private fun shortSummary(result: DnsCheckResult?): String? {
    if (result == null) return null
    if (result.verdicts.isEmpty()) return result.failure
    val opened = TunnelDnsCheck.targets.filter { result.verdictFor(it) == HostVerdict.Ok }.map { it.label }
    return when {
        opened.isEmpty() -> "Ничего не открылось"
        opened.size == result.totalTargets -> "Открывается всё"
        else -> opened.joinToString(", ")
    }
}

@Composable
fun TunnelDnsSheet(
    visible: Boolean,
    choices: List<TunnelDnsChoice>,
    onClose: () -> Unit,
    onSelect: (TunnelDnsChoice) -> Unit,
    title: String = "DNS внутри туннеля",
    sortPrefKey: String = SORT_DNS_KEY,
    checkWithoutNetFix: Boolean = false
) {
    val context = LocalContext.current
    val tv = LocalIsTv.current
    val prefs = remember { context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE) }
    var sortKeys by remember { mutableStateOf(parseSortKeys(prefs.getString(sortPrefKey, DNS_SORT_DEFAULT_KEYS), ALL_DNS_SORTS)) }
    var openedKey by remember { mutableStateOf<String?>(null) }
    val opened = choices.firstOrNull { it.key == openedKey }
    val network = remember(visible) {
        TunnelDnsCheck.load(context)
        TunnelDnsCheck.networkKey(context)
    }
    val resultOf = { key: String -> TunnelDnsCheck.resultFor(network, key) }
    val anyBusy = choices.any { TunnelDnsCheck.isBusy(it.key) }
    val answerMsOf = { key: String -> resultOf(key)?.answerMs?.takeIf { ms -> ms >= 0 } ?: Long.MAX_VALUE }

    val openedOf = { target: String? ->
        { choice: TunnelDnsChoice ->
            resultOf(choice.key)?.takeIf { it.verdicts.isNotEmpty() }?.let { result ->
                if (target == null) {
                    result.okTargets to result.totalTargets
                } else {
                    val checkTarget = TunnelDnsCheck.targets.first { it.label == target }
                    (if (result.verdictFor(checkTarget) == HostVerdict.Ok) 1 else 0) to 1
                }
            }
        }
    }
    val criteria = sortKeys.map { key ->
        when (key) {
            DNS_SORT_BEST -> StrategyOrdering.byOpened(openedOf(null))
            DNS_SORT_SPEED -> StrategyOrdering.byLatency<TunnelDnsChoice> { answerMsOf(it.key).takeIf { ms -> ms != Long.MAX_VALUE } }
            else -> StrategyOrdering.byOpened(openedOf(TunnelDnsCheck.targets.first { "target:${it.label}" == key }.label))
        }
    }
    val displayed = StrategyOrdering.sortedByCriteria(choices, criteria)

    val startCheck: (TunnelDnsChoice) -> Unit = { choice ->
        TunnelDnsCheck.start(context, choice.key, choice.server, choice.systemDns, choice.encrypted, checkWithoutNetFix)
    }

    NfBottomSheet(
        visible = visible,
        onDismiss = if (opened != null) ({ openedKey = null }) else ({
            openedKey = null
            onClose()
        }),
        icon = if (opened != null) Icons.Default.NetworkCheck else Icons.Default.Dns,
        title = opened?.label ?: title,
        subtitle = if (opened != null) {
            "Проверка откроет эти сайты через NetFix с этим DNS."
        } else {
            "Значок справа проверит, что открывается с этим DNS"
        },
        closeLabel = if (opened != null) "Назад" else "Готово",
        maxHeightFraction = 0.72f,
        fixedHeight = opened == null,
        scrollResetKey = openedKey,
        footer = { entry ->
            AnimatedVisibility(visible = opened == null, enter = fadeIn(tween(220)), exit = ExitTransition.None) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    NfButton(
                        text = if (anyBusy) "Проверяю…" else "Проверить все",
                        style = NfButtonStyle.Secondary,
                        modifier = entry(5).fillMaxWidth(),
                        onClick = { if (!anyBusy) choices.filter { it.address.isNotBlank() || it.systemDns }.forEach(startCheck) }
                    )
                }
            }
        }
    ) { entry ->
        AnimatedContent(
            targetState = openedKey,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                val forward = targetState != null
                (fadeIn(tween(260, delayMillis = 60)) +
                    slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { if (forward) it / 5 else -it / 5 })
                    .togetherWith(
                        fadeOut(tween(160)) +
                            slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) -it / 5 else it / 5 }
                    )
                    .using(SizeTransform(clip = false) { _, _ -> snap() })
            },
            label = "dnsSheetBody"
        ) { screenKey ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val detail = choices.firstOrNull { it.key == screenKey }
                if (detail != null) {
                    DnsDetail(
                        choice = detail,
                        result = resultOf(detail.key),
                        busy = TunnelDnsCheck.isBusy(detail.key),
                        tv = tv,
                        entry = entry,
                        onCheck = { startCheck(detail) },
                        onSelect = {
                            onSelect(detail)
                            openedKey = null
                        }
                    )
                } else {
                    if (choices.any { resultOf(it.key) != null }) {
                        SortMenu(
                            options = listOf(
                                SortOption(DNS_SORT_BEST, "Работе", "работа")
                            ) + TunnelDnsCheck.targets.map { SortOption("target:${it.label}", it.label, it.label) } +
                                SortOption(DNS_SORT_SPEED, "Скорости", "скорость"),
                            selectedKeys = sortKeys,
                            onChange = { keys ->
                                sortKeys = keys
                                prefs.edit().putString(sortPrefKey, keys.joinToString(",")).apply()
                            },
                            modifier = entry(0)
                        )
                    }
                    val selectedFocus = remember { FocusRequester() }
                    if (tv) {
                        LaunchedEffect(visible, screenKey) {
                            if (visible) {
                                delay(TV_FOCUS_DELAY_MS)
                                selectedFocus.requestFocusSafe()
                            }
                        }
                    }
                    displayed.forEachIndexed { index, choice ->
                        DnsRow(
                            choice = choice,
                            result = resultOf(choice.key),
                            checking = TunnelDnsCheck.isBusy(choice.key),
                            focusRequester = selectedFocus.takeIf { tv && choice.selected },
                            modifier = entry(minOf(index + 1, 4)),
                            onSelect = { onSelect(choice) },
                            onOpen = { openedKey = choice.key }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnsCheckButton(onClick: () -> Unit) {
    val haptics = rememberNetFixHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(Nf.SurfacePressed)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Nf.Line, shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptics(NetFixHaptic.Tap)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.NetworkCheck, contentDescription = "Проверить DNS", tint = Color(0xFFA1A1A1), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DnsRow(
    choice: TunnelDnsChoice,
    result: DnsCheckResult?,
    checking: Boolean,
    focusRequester: FocusRequester?,
    modifier: Modifier,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
    val verdict = dnsVerdictOf(result)
    val failing = result != null && result.verdicts.isNotEmpty() && result.okTargets == 0 && !checking
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        choice.selected -> Nf.Green.copy(alpha = 0.8f)
        checking -> Nf.Bright.copy(alpha = 0.45f)
        failing -> Nf.Red.copy(alpha = 0.40f)
        else -> Color(0x14FFFFFF)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .nfTvFocus(corner = 14.dp, inside = true)
            .clip(shape)
            .background(if (choice.selected) Nf.Green.copy(alpha = 0.07f) else Nf.Surface)
            .border(if (choice.selected) 2.dp else 1.dp, borderColor, shape)
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSelect),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioMark(choice.selected)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = choice.label,
                        color = Nf.Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!checking && result != null && result.verdicts.isNotEmpty()) {
                        Text(
                            text = "${result.okTargets} из ${result.totalTargets}",
                            color = verdict.textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (checking) NfProgress(0f, modifier = Modifier.width(56.dp), height = 4, indeterminate = true)
                    Text(
                        text = when {
                            checking -> "Проверяю…"
                            result != null -> shortSummary(result).orEmpty() + if (result.direct && result.verdicts.isNotEmpty()) " · без NetFix" else ""
                            else -> choice.address.ifBlank { "Не проверен" }
                        },
                        color = if (checking) Nf.Bright else if (result != null) verdict.textColor else Nf.TextSub,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        DnsCheckButton(onClick = onOpen)
    }
}

@Composable
private fun DnsDetail(
    choice: TunnelDnsChoice,
    result: DnsCheckResult?,
    busy: Boolean,
    tv: Boolean,
    entry: @Composable (Int) -> Modifier,
    onCheck: () -> Unit,
    onSelect: () -> Unit
) {
    val verdict = dnsVerdictOf(result)
    val shape = RoundedCornerShape(14.dp)
    val canCheck = choice.systemDns || choice.address.isNotBlank()
    Column(
        modifier = entry(0)
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(if (busy) Nf.Bright.copy(alpha = 0.08f) else verdict.dotColor.copy(alpha = if (result == null) 0.06f else 0.10f))
            .border(1.dp, if (busy) Nf.Bright.copy(alpha = 0.35f) else verdict.dotColor.copy(alpha = if (result == null) 0.20f else 0.35f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        if (busy) {
            Text("Проверяю «${choice.label}»…", color = Nf.Bright, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            NfProgress(0f, indeterminate = true)
        } else {
            StatusLine(
                text = verdict.label + if (result != null && result.verdicts.isNotEmpty()) " · открыто ${result.okTargets} из ${result.totalTargets}" else "",
                color = verdict.textColor,
                dotColor = verdict.dotColor
            )
            Text(
                text = when {
                    result?.failure != null -> result.failure
                    result != null -> listOfNotNull(
                        result.server?.let { "Адрес $it" } ?: "DNS вашей сети",
                        result.answerMs.takeIf { it >= 0 }?.let { "ответ $it мс" },
                        "без NetFix".takeIf { result.direct }
                    ).joinToString(" · ")
                    !canCheck -> "Сначала впишите адрес в настройках"
                    else -> "${choice.address.ifBlank { "DNS вашей сети" }} · нажмите «Проверить»"
                },
                color = Nf.TextSub,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    val checkFocus = remember { FocusRequester() }
    if (tv) {
        LaunchedEffect(choice.key) {
            delay(TV_FOCUS_DELAY_MS)
            checkFocus.requestFocusSafe()
        }
    }
    val actions: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(modifier = rowModifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NfButton(
                text = if (busy) "Проверяю…" else "Проверить",
                style = NfButtonStyle.Secondary,
                enabled = canCheck,
                modifier = Modifier.weight(1f).focusRequester(checkFocus),
                onClick = { if (!busy) onCheck() }
            )
            NfButton(
                text = if (choice.selected) "Выбран" else "Выбрать",
                style = if (choice.selected) NfButtonStyle.Tinted else NfButtonStyle.Secondary,
                enabled = !choice.selected,
                modifier = Modifier.weight(1f),
                onClick = onSelect
            )
        }
    }
    if (tv) actions(entry(1))

    Text(
        "Что проверяется",
        color = Nf.TextSub,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = entry(1).padding(start = 4.dp, top = 4.dp)
    )
    TunnelDnsCheck.targets.forEachIndexed { index, target ->
        val targetVerdict = if (busy) null else result?.verdictFor(target)
        TargetRow(
            label = target.label,
            hosts = target.hosts.joinToString(", ") { it.substringBefore('/') },
            verdict = targetVerdict,
            tv = tv,
            modifier = entry(minOf(index + 2, 4))
        )
    }

    if (!tv) actions(entry(4))
}

@Composable
private fun TargetRow(label: String, hosts: String, verdict: HostVerdict?, tv: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val (icon, tint, note) = when (verdict) {
        HostVerdict.Ok -> Triple(Icons.Default.Check, Nf.Green, "Открывается")
        HostVerdict.RegionBlocked -> Triple(Icons.Default.Public, Nf.Yellow, "Не пускает из России")
        HostVerdict.Blocked -> Triple(Icons.Default.Close, Nf.RedText, "Не открывается")
        null -> Triple(null, Nf.TextOff, "")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (tv) Modifier.focusable() else Modifier)
            .nfTvFocus(corner = 12.dp, inside = true)
            .clip(shape)
            .background(Nf.Surface)
            .border(1.dp, Nf.Line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Nf.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Text(
                if (note.isNotEmpty()) note else hosts,
                color = if (note.isNotEmpty()) tint else Nf.TextSub,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (icon != null) tint.copy(alpha = 0.14f) else Color.Transparent)
                .graphicsLayer { alpha = if (icon != null) 1f else 0f },
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}
