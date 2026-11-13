package com.rupleide.netfix.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

const val START_ACTION = "start"
const val STOP_ACTION = "stop"
const val RESUME_ACTION = "resume"
const val PAUSE_ACTION = "pause"
const val START_FROM_TILE_ACTION = "com.rupleide.netfix.action.START_FROM_TILE"

const val STARTED_BROADCAST = "com.rupleide.netfix.STARTED"
const val STOPPED_BROADCAST = "com.rupleide.netfix.STOPPED"
const val FAILED_BROADCAST = "com.rupleide.netfix.FAILED"

const val SENDER = "sender"

var appStatus = AppStatus.Halted to Mode.VPN
var performanceModeGlobal by mutableStateOf(false)
var connectionStartTime: Long = 0L
var isTgProxyRunningGlobal = false
var rxSpeedGlobal = 0.0
var txSpeedGlobal = 0.0
var pingMsGlobal: Int? = null

var updateInfoGlobal by mutableStateOf<com.rupleide.netfix.core.update.UpdateManager.UpdateInfo?>(null)
var updateProgressGlobal by mutableStateOf(-1f)
var updateStatusGlobal by mutableStateOf<String?>(null)
var onNavigateToTab: ((Int) -> Unit)? = null
var forceUpdateTestGlobal by mutableStateOf(false)
val openActionSheets = androidx.compose.runtime.mutableStateMapOf<Any, Boolean>()
val isActionSheetVisibleGlobal: Boolean get() = openActionSheets.isNotEmpty()

@androidx.compose.runtime.Composable
fun TrackActionSheetVisible(visible: Boolean) {
    val owner = androidx.compose.runtime.remember { Any() }
    androidx.compose.runtime.DisposableEffect(visible) {
        if (visible) openActionSheets[owner] = true
        onDispose { openActionSheets.remove(owner) }
    }
}
var isConnectionTransitionGlobal by mutableStateOf(false)
var isTvFullScreenOverlayGlobal by mutableStateOf(false)
val sheetScrimAlphas = androidx.compose.runtime.mutableStateMapOf<Any, Float>()
fun sheetScrimAlphaMax(): Float = sheetScrimAlphas.values.maxOrNull() ?: 0f
var tileStartRequestedGlobal by mutableStateOf(false)
var updateSheetDismissedThisSession = false
var vpnConsentUnavailableGlobal by mutableStateOf(false)
var helpOpenRequestGlobal by mutableStateOf<String?>(null)
val helpScreenVisibleGlobal = mutableStateOf(false)
var whatsNewRequestGlobal by mutableStateOf<String?>(null)
var settingsSectionRequestGlobal by mutableStateOf<Int?>(null)
var settingHighlightGlobal by mutableStateOf<String?>(null)
var settingHighlightRequestedAtGlobal = 0L
const val SETTING_HIGHLIGHT_TTL_MS = 5000L

fun requestSettingHighlight(id: String?) {
    settingHighlightRequestedAtGlobal = android.os.SystemClock.elapsedRealtime()
    settingHighlightGlobal = id
}

fun isSettingHighlightFresh(): Boolean =
    android.os.SystemClock.elapsedRealtime() - settingHighlightRequestedAtGlobal < SETTING_HIGHLIGHT_TTL_MS

fun setStatus(status: AppStatus, mode: Mode) {
    appStatus = status to mode
}
