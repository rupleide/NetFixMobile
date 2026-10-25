package com.rupleide.netfix.core.dpibypass

import android.content.Context
import android.content.Intent
import com.rupleide.netfix.core.debug.AppDebugManager as Log
import androidx.core.content.ContextCompat
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.Mode
import com.rupleide.netfix.data.START_ACTION
import com.rupleide.netfix.data.STOP_ACTION
import com.rupleide.netfix.data.appStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ServiceManager {
    private val TAG: String = ServiceManager::class.java.simpleName
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isAlive: Boolean get() = ByeDpiVpnService.isAlive || RootDpiService.isAlive
    val isConnected: Boolean get() = ByeDpiVpnService.isConnected || RootDpiService.isConnected

    fun rootMode(context: Context): Boolean = context.getPreferences().getBoolean("byedpi_root_mode", false)

    fun prepare(context: Context): Intent? =
        if (rootMode(context)) null else android.net.VpnService.prepare(context)

    fun serviceClass(context: Context): Class<*> =
        if (rootMode(context)) RootDpiService::class.java else ByeDpiVpnService::class.java

    fun start(context: Context, mode: Mode): Boolean {
        Log.i(TAG, "Starting DPI bypass")
        val intent = Intent(context, serviceClass(context))
        intent.action = START_ACTION
        return try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "VPN start not allowed from background: ${e.message}")
            false
        }
    }

    fun stop(context: Context) {
        for (service in listOf(
            ByeDpiVpnService::class.java to ByeDpiVpnService.isAlive,
            RootDpiService::class.java to RootDpiService.isAlive,
        )) {
            if (!service.second) continue
            try {
                context.startService(Intent(context, service.first).setAction(STOP_ACTION))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "DPI stop request rejected: ${e.message}")
            }
        }
    }

    fun restart(context: Context, mode: Mode) {
        if (appStatus.first == AppStatus.Running) {
            stop(context)
            scope.launch {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 3000L) {
                    if (appStatus.first == AppStatus.Halted) break
                    delay(100)
                }
                context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("service_enabled", true)
                    .apply()
                start(context, mode)
            }
        }
    }
}
