package com.rupleide.netfix.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rupleide.netfix.core.dpibypass.AppListMode
import com.rupleide.netfix.core.dpibypass.appListMode
import com.rupleide.netfix.core.dpibypass.getPreferences
import com.rupleide.netfix.core.dpibypass.selectedAppsWithDefaults

data class YoutubeClientApp(val packageName: String, val label: String)

data class YoutubeClientDownload(val label: String, val note: String, val url: String)

object YoutubeClient {
    const val KEY = "youtube_client_package"
    const val OFFICIAL = "com.google.android.youtube"

    val downloads = listOf(
        YoutubeClientDownload("ReVanced", "YouTube без рекламы. Ставится через ReVanced Manager", "https://revanced.app/download"),
        YoutubeClientDownload("GmsCore", "Нужен ReVanced, чтобы войти в аккаунт Google", "https://github.com/ReVanced/GmsCore/releases/latest"),
        YoutubeClientDownload("NewPipe", "Лёгкий, без аккаунта и рекламы", "https://newpipe.net/#download"),
        YoutubeClientDownload("PipePipe", "Как NewPipe, с комментариями и SponsorBlock", "https://github.com/InfinityLoop1308/PipePipe/releases/latest"),
        YoutubeClientDownload("SmartTube", "Для ТВ и приставок", "https://github.com/yuliskov/SmartTube/releases/latest")
    )
    private const val OFFICIAL_TV = "com.google.android.youtube.tv"

    private val knownClients = listOf(
        OFFICIAL,
        "app.revanced.android.youtube",
        "app.rvx.android.youtube",
        "app.morphe.android.youtube",
        "com.vanced.android.youtube",
        "org.smarttube.stable",
        "org.smarttube.beta",
        "com.teamsmart.videomanager.tv",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "org.schabi.newpipe",
        "org.polymorphicshade.tubular",
        "InfinityLoop1309.NewPipeEnhanced",
        "com.libretube",
        "com.github.libretube",
        "com.futo.platformplayer"
    )

    private fun installed(context: Context, packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: Exception) {
        false
    }

    private fun labelOf(context: Context, packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName
    }

    fun installedClients(context: Context): List<YoutubeClientApp> {
        val customChoice = chosenPackage(context)?.takeIf { it !in knownClients }
        return (knownClients + listOfNotNull(customChoice))
            .filter { installed(context, it) }
            .map { YoutubeClientApp(it, labelOf(context, it)) }
    }

    fun otherLaunchableApps(context: Context): List<YoutubeClientApp> {
        val pm = context.packageManager
        val categories = listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)
        return categories
            .flatMap { category ->
                runCatching { pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0) }.getOrDefault(emptyList())
            }
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName && it !in knownClients }
            .map { YoutubeClientApp(it, labelOf(context, it)) }
            .sortedBy { it.label.lowercase() }
    }

    fun includeInBypass(context: Context, packageName: String): Boolean {
        if (packageName == OFFICIAL) return false
        val prefs = context.getPreferences()
        if (prefs.appListMode() != AppListMode.SelectedAppsOnly) return false
        val selected = prefs.selectedAppsWithDefaults()
        if (packageName in selected) return false
        prefs.edit()
            .putStringSet("selected_apps", selected + packageName)
            .remove("selected_apps_installed_count")
            .apply()
        return true
    }

    fun chosenPackage(context: Context): String? =
        context.getPreferences().getString(KEY, null)?.takeIf { it != OFFICIAL && installed(context, it) }

    fun choose(context: Context, packageName: String?) {
        val edit = context.getPreferences().edit()
        if (packageName == null || packageName == OFFICIAL) edit.remove(KEY) else edit.putString(KEY, packageName)
        edit.apply()
    }

    fun buttonLabel(context: Context): String = chosenPackage(context)?.let { labelOf(context, it) } ?: "YouTube"

    fun launchIntent(context: Context, isTv: Boolean): Intent {
        val pm = context.packageManager
        val chosen = chosenPackage(context)?.let { pm.getLaunchIntentForPackage(it) ?: pm.getLeanbackLaunchIntentForPackage(it) }
        val official = (if (isTv) pm.getLeanbackLaunchIntentForPackage(OFFICIAL_TV) ?: pm.getLaunchIntentForPackage(OFFICIAL_TV) else null)
            ?: pm.getLaunchIntentForPackage(OFFICIAL)
        val intent = chosen ?: official ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
