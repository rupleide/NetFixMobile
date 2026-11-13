package com.rupleide.netfix.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rupleide.netfix.BuildConfig
import com.rupleide.netfix.R
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.dpibypass.StrategyTestManager
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.data.isActionSheetVisibleGlobal
import com.rupleide.netfix.data.isConnectionTransitionGlobal
import com.rupleide.netfix.data.vpnConsentUnavailableGlobal
import kotlinx.coroutines.delay

private const val SHOW_DELAY_MS = 1400L
private const val BUSY_POLL_MS = 600L
private const val CONFIRMATION_MS = 1800L

private enum class UpdateAnswer { KeptAsBefore, EnabledNew }

@Composable
fun CompatibilityUpdateSheet(
    eligible: Boolean,
    canShow: Boolean
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getPreferences() }
    var visible by remember { mutableStateOf(false) }
    var resolved by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf<UpdateAnswer?>(null) }
    var previousBehavior by remember { mutableStateOf(CompatibilityManager.BASELINE_VERSION) }

    LaunchedEffect(eligible, canShow, resolved) {
        if (!eligible || !canShow || resolved || visible) return@LaunchedEffect
        delay(SHOW_DELAY_MS)
        while (
            StrategyTestManager.isTesting ||
            StrategyTestManager.isVerifying ||
            isConnectionTransitionGlobal ||
            isActionSheetVisibleGlobal ||
            vpnConsentUnavailableGlobal
        ) {
            delay(BUSY_POLL_MS)
        }
        previousBehavior = CompatibilityManager.behaviorVersion(prefs)
        CompatibilityManager.markPromptShown(prefs, BuildConfig.VERSION_CODE)
        visible = true
    }

    com.rupleide.netfix.data.TrackActionSheetVisible(visible)

    LaunchedEffect(answer) {
        if (answer != null) {
            delay(CONFIRMATION_MS)
            visible = false
            resolved = true
        }
    }

    val dismiss = {
        visible = false
        resolved = true
    }

    val current = answer
    NetFixActionSheet(
        visible = visible,
        onDismissRequest = dismiss,
        icon = if (current == null) R.drawable.ic_info else R.drawable.ic_check,
        title = if (current == null) "NetFix обновлён до ${BuildConfig.VERSION_NAME}" else "Готово",
        subtitle = when (current) {
            null -> "Была доработана сетевая часть и внесены многочисленные изменения.\nВсё работало на $previousBehavior?"
            UpdateAnswer.EnabledNew -> "Новые сетевые функции включены."
            UpdateAnswer.KeptAsBefore -> "Сеть работает как на $previousBehavior."
        },
        options = if (current == null) {
            listOf(
                NetFixSheetOption(label = "Да, всё работало", selected = true) {
                    answer = UpdateAnswer.KeptAsBefore
                },
                NetFixSheetOption(label = "Были проблемы, включить новое") {
                    CompatibilityManager.enableEverythingNew(prefs)
                    answer = UpdateAnswer.EnabledNew
                },
                NetFixSheetOption(label = "Начать с чистого листа") {
                    com.rupleide.netfix.core.AppReset.resetSettingsKeepingResults(context, "после обновления до ${BuildConfig.VERSION_NAME}")
                }
            )
        } else {
            emptyList()
        },
        closeLabel = if (current == null) "Пропустить" else "Хорошо",
        footnote = "«Начать с чистого листа» сбросит настройки и заново подберёт способ, если после обновления ничего не работает. Способы и результаты проверок сохранятся."
    )
}
