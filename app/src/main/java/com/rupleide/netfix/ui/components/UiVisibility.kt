package com.rupleide.netfix.ui.components

import android.content.SharedPreferences

fun SharedPreferences.uiShowYoutube(): Boolean =
    getBoolean("ui_show_youtube", getBoolean("wants_youtube_bypass", true))

fun SharedPreferences.uiShowTelegram(): Boolean =
    getBoolean("ui_show_telegram", getBoolean("telegram_proxy_enabled_by_user", true))
