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

    fun start(context: Context, mode: Mode): Boolean {
        Log.i(TAG, "Starting VPN")
        val intent = Intent(context, ByeDpiVpnService::class.java)
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
        if (!ByeDpiVpnService.isAlive) return
        Log.i(TAG, "Stopping VPN")
        val intent = Intent(context, ByeDpiVpnService::class.java)
        intent.action = STOP_ACTION
        try {
            context.startService(intent)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "VPN stop request rejected: ${e.message}")
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
