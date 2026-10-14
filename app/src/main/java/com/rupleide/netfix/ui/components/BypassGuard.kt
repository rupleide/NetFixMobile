package com.rupleide.netfix.ui.components

import android.content.Context
import android.widget.Toast
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.data.AppStatus
import com.rupleide.netfix.data.appStatus

object BypassGuard {
    private var lastNoticeAt = 0L

    fun isActive(): Boolean =
        appStatus.first == AppStatus.Running || TgProxyController.isRunning

    fun notifyBlocked(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastNoticeAt < 1500L) return
        lastNoticeAt = now
        Toast.makeText(context, "Выключите обход, чтобы изменить эту настройку", Toast.LENGTH_SHORT).show()
    }

    inline fun guarded(context: Context, action: () -> Unit) {
        if (isActive()) notifyBlocked(context) else action()
    }
}
