package com.rupleide.netfix.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.rupleide.netfix.core.dpibypass.ByeDpiVpnService
import com.rupleide.netfix.core.dpibypass.ServiceManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.core.tgproxy.TgProxyService
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.STOP_ACTION
import com.rupleide.netfix.data.START_ACTION
import com.rupleide.netfix.data.START_FROM_TILE_ACTION
import com.rupleide.netfix.MainActivity
import java.lang.ref.WeakReference

class NetFixTileService : TileService() {

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())
        private var listeningTile: WeakReference<NetFixTileService>? = null

        fun requestRefresh(context: Context) {
            val appContext = context.applicationContext
            mainHandler.post {
                val activeTile = listeningTile?.get()
                if (activeTile != null) {
                    activeTile.updateTileState()
                } else {
                    try {
                        TileService.requestListeningState(
                            appContext,
                            ComponentName(appContext, NetFixTileService::class.java)
                        )
                    } catch (_: Exception) {}
                }
            }
        }

        fun isNetFixRunning(): Boolean =
            ByeDpiVpnService.isConnected || TgProxyController.isRunning
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningTile = WeakReference(this)
        updateTileState()
    }

    override fun onStopListening() {
        if (listeningTile?.get() === this) {
            listeningTile = null
        }
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isNetFixRunning()) {
            stopAll()
        } else {
            startAll()
        }
    }

    private fun stopAll() {
        val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", false).apply()
        ServiceManager.stop(this)

        val intent = Intent(this, TgProxyService::class.java).apply {
            action = STOP_ACTION
        }
        startService(intent)
        TgProxyController.stop()

        showTileState(isActive = false)
    }

    private fun startAll() {
        val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        val wantsYoutube = prefs.getBoolean("wants_youtube_bypass", true)
        val wantsTelegram = prefs.getBoolean("telegram_proxy_enabled_by_user", true)

        if (wantsYoutube && VpnService.prepare(this) != null) {
            openAppAndStartFromTile()
            return
        }

        prefs.edit().putBoolean("service_enabled", true).apply()

        val youtubeStarted = !wantsYoutube || ServiceManager.start(this, Mode.VPN)
        val telegramStarted = !wantsTelegram || TgProxyService.startInForeground(this, openTelegram = false)

        if (!youtubeStarted || !telegramStarted) {
            openAppAndStartFromTile()
            return
        }

        showTileState(isActive = true)
    }

    private fun openAppAndStartFromTile() {
        val startAppIntent = Intent(this, MainActivity::class.java).apply {
            action = START_FROM_TILE_ACTION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                startAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(startAppIntent)
        }
    }

    private fun updateTileState() {
        showTileState(isNetFixRunning())
    }

    private fun showTileState(isActive: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "NetFix Mobile"
        tile.updateTile()
    }
}
