package com.rupleide.netfix.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.core.tgproxy.CfProxyChecker
import com.rupleide.netfix.core.tgproxy.CfServerResult
import com.rupleide.netfix.core.tgproxy.CfServerState
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfBottomSheet
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CfServersSheet(visible: Boolean, onDismiss: () -> Unit, onPinnedChange: (String?) -> Unit) {
    val context = LocalContext.current
    val results = remember { mutableStateListOf<CfServerResult>() }
    var running by remember { mutableStateOf(false) }
    var runRequest by remember { mutableIntStateOf(0) }
    var pinned by remember(visible) { mutableStateOf(TgProxyController.pinnedCfDomain(context)) }
    val total = remember(visible) { CfProxyChecker.knownDomains(context).size }

    LaunchedEffect(runRequest) {
        if (runRequest == 0) return@LaunchedEffect
        running = true
        results.clear()
        val sorted = withContext(Dispatchers.IO) {
            CfProxyChecker.checkAll(context) { result ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (running && results.none { it.domain == result.domain }) results.add(result)
                }
            }
        }
        running = false
        results.clear()
        results.addAll(sorted)
    }

    val choose: (String?) -> Unit = { domain ->
        pinned = domain
        TgProxyController.pinCfDomain(context, domain)
        onPinnedChange(domain)
    }

    NfBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        icon = Icons.Default.Cloud,
        title = "Серверы Cloudflare",
        subtitle = "Через них Telegram работает, если прямое подключение заблокировано",
        closeLabel = "Закрыть",
        maxHeightFraction = 0.8f
    ) { entry ->
        NfButton(
            text = when {
                running -> "Проверяю ${results.size} из $total…"
                results.isEmpty() -> "Проверить серверы"
                else -> "Проверить заново"
            },
            style = NfButtonStyle.Secondary,
            modifier = entry(0),
            onClick = { if (!running) runRequest++ }
        )
        if (running) {
            NfProgress(fraction = if (total > 0) results.size.toFloat() / total else 0f, modifier = entry(1))
        }
        val working = results.filter { it.state == CfServerState.Works }
        if (!running && results.isNotEmpty()) {
            Text(
                text = if (working.isEmpty()) {
                    "Ни один сервер не ответил. Telegram пойдёт напрямую, если это возможно."
                } else {
                    "Работают ${working.size} из ${results.size}. Нажмите на сервер, чтобы Telegram всегда шёл через него."
                },
                color = Nf.TextSub,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = entry(2)
            )
        }
        NfButton(
            text = "Автоматически",
            style = if (pinned == null) NfButtonStyle.Tinted else NfButtonStyle.Secondary,
            modifier = entry(3),
            onClick = { choose(null) }
        )
        working.forEachIndexed { index, result ->
            NfButton(
                text = "${result.domain} · ${result.latencyMs} мс",
                style = if (pinned == result.domain) NfButtonStyle.Tinted else NfButtonStyle.Secondary,
                modifier = entry(index + 4),
                onClick = { choose(result.domain) }
            )
        }
        if (pinned != null && working.none { it.domain == pinned } && !running) {
            Text(
                text = if (results.isEmpty()) "Сейчас закреплён $pinned." else "Закреплённый $pinned сейчас не отвечает. Выберите другой или «Автоматически».",
                color = if (results.isEmpty()) Nf.TextSub else Nf.Yellow,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = entry(working.size + 4)
            )
        }
        results.filter { it.state != CfServerState.Works }.forEach { result ->
            Text(
                text = "${result.domain}: ${result.detail}",
                color = if (result.state == CfServerState.Busy) Nf.Yellow else Nf.TextSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
