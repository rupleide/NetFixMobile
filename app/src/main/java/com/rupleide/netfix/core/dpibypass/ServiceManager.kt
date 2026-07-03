package com.rupleide.netfix.core.dpibypass

import android.content.Context
import android.content.Intent
import android.util.Log
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

    fun start(context: Context, mode: Mode) {
        Log.i(TAG, "Starting VPN")
        val intent = Intent(context, ByeDpiVpnService::class.java)
        intent.action = START_ACTION
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        Log.i(TAG, "Stopping VPN")
        val intent = Intent(context, ByeDpiVpnService::class.java)
        intent.action = STOP_ACTION
        ContextCompat.startForegroundService(context, intent)
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
                start(context, mode)
            }
        }
    }
}
