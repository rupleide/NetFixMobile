package com.rupleide.netfix.core.compat

import android.content.SharedPreferences

enum class TunnelDnsMode(val storageValue: String) {
    System("system"),
    Cloudflare("cloudflare"),
    Custom("custom");

    companion object {
        fun fromStorage(value: String?): TunnelDnsMode =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

enum class CompatFeature(val storageKey: String, val sinceVersion: String) {
    AiAppsInBypass("compat_ai_apps_bypass", "1.0.5"),
    VideoFastFail("compat_video_fast_fail", "1.0.5"),
    YoutubeVideoStrategies("compat_youtube_video_strategies", "1.0.5"),
    CloudflareUnfreeze("compat_cf_unfreeze", "1.0.5"),
    SecureDns("compat_secure_dns", "1.0.5"),
    TgProxyWatchdog("compat_tg_proxy_watchdog", "1.0.5"),
    StreamProbe("compat_stream_probe", "1.0.5"),
    DiscordCombinedStrategy("compat_discord_combined_strategy", "1.0.5"),
    BatterySaver("compat_battery_saver", "1.0.5"),
    SmartTubeHosts("compat_smarttube_hosts", "1.0.5"),
    GeminiWithGoogleApp("compat_gemini_google_app", "1.0.5"),
    FreeProxyPort("compat_free_proxy_port", "1.0.5"),
    DiscordEchCheck("compat_discord_ech_check", "1.0.5"),
    EthernetScan("compat_ethernet_scan", "1.0.5"),
    ScanQuickFallback("compat_scan_quick_fallback", "1.0.5"),
    SystemTunnelDns("compat_system_tunnel_dns", "1.0.5"),
    DiscordOffOnTv("compat_discord_off_on_tv", "1.0.5");

    companion object {
        fun inVersion(version: String): List<CompatFeature> = entries.filter { it.sinceVersion == version }

        val versions: List<String> get() = entries.map { it.sinceVersion }.distinct().sortedWith(VersionOrder)
    }
}

object VersionOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val left = a.split('.').map { it.toIntOrNull() ?: 0 }
        val right = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}

object CompatibilityManager {
    const val BASELINE_VERSION = "1.0.4"
    const val KEY_LAST_RUN_VERSION_CODE = "last_run_version_code"
    const val KEY_BEHAVIOR_VERSION = "compat_behavior_version"
    const val KEY_PROMPTED_UP_TO = "compat_prompted_up_to"
    const val KEY_TUNNEL_DNS_MODE = "compat_tunnel_dns_mode"
    const val KEY_TUNNEL_DNS_CUSTOM = "compat_tunnel_dns_custom"
    const val KEY_TUNNEL_DNS_NAME = "compat_tunnel_dns_name"
    val KEY_DISCORD_ECH_CHECK = CompatFeature.DiscordEchCheck.storageKey

    private const val KEY_LEGACY_PROFILE = "compat_profile"
    private const val LEGACY_PROFILE_OLD_BEHAVIOR = "legacy"
    private const val PROFILE_ERA_VERSION = "1.0.5"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val CLOUDFLARE_DNS = "1.1.1.1"
    private const val MAX_CUSTOM_DNS_SERVERS = 2
    private val dnsSeparators = Regex("[\\s,;]+")

    val latestVersion: String
        get() = CompatFeature.versions.lastOrNull() ?: BASELINE_VERSION

    val behaviorChoices: List<String>
        get() = (listOf(BASELINE_VERSION) + CompatFeature.versions).distinct().sortedWith(VersionOrder)

    fun behaviorVersion(prefs: SharedPreferences): String {
        prefs.getString(KEY_BEHAVIOR_VERSION, null)?.let { return it }
        return when (prefs.getString(KEY_LEGACY_PROFILE, null)) {
            null, LEGACY_PROFILE_OLD_BEHAVIOR -> BASELINE_VERSION
            else -> PROFILE_ERA_VERSION
        }
    }

    fun isLatestBehavior(prefs: SharedPreferences): Boolean =
        VersionOrder.compare(behaviorVersion(prefs), latestVersion) >= 0

    fun isCustomized(prefs: SharedPreferences): Boolean =
        CompatFeature.entries.any { prefs.contains(it.storageKey) }

    fun setBehaviorVersion(prefs: SharedPreferences, version: String) {
        val tunnelDnsChoiceExists = VersionOrder.compare(CompatFeature.SystemTunnelDns.sinceVersion, version) <= 0
        val editor = prefs.edit()
            .putString(KEY_BEHAVIOR_VERSION, version)
            .remove(KEY_LEGACY_PROFILE)
        if (!tunnelDnsChoiceExists) editor.remove(KEY_TUNNEL_DNS_MODE)
        CompatFeature.entries.forEach { editor.remove(it.storageKey) }
        editor.apply()
    }

    fun enableEverythingNew(prefs: SharedPreferences) = setBehaviorVersion(prefs, latestVersion)

    fun featureEnabled(prefs: SharedPreferences, feature: CompatFeature): Boolean =
        if (prefs.contains(feature.storageKey)) {
            prefs.getBoolean(feature.storageKey, false)
        } else {
            VersionOrder.compare(feature.sinceVersion, behaviorVersion(prefs)) <= 0
        }

    private fun enabledByVersion(prefs: SharedPreferences, feature: CompatFeature): Boolean =
        VersionOrder.compare(feature.sinceVersion, behaviorVersion(prefs)) <= 0

    fun setFeature(prefs: SharedPreferences, feature: CompatFeature, enabled: Boolean) {
        pinBehaviorVersion(prefs)
        if (enabled == enabledByVersion(prefs, feature)) {
            prefs.edit().remove(feature.storageKey).apply()
        } else {
            prefs.edit().putBoolean(feature.storageKey, enabled).apply()
        }
    }

    fun discordEchCheckEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.DiscordEchCheck)

    fun setDiscordEchCheck(prefs: SharedPreferences, enabled: Boolean) =
        setFeature(prefs, CompatFeature.DiscordEchCheck, enabled)

    fun cloudflareUnfreezeEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.CloudflareUnfreeze)

    fun freeProxyPortEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.FreeProxyPort)

    fun tgProxyWatchdogEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.TgProxyWatchdog)

    fun ethernetScanEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.EthernetScan)

    fun streamProbeEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.StreamProbe)

    fun smartTubeHostsEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.SmartTubeHosts)

    fun discordCombinedStrategyEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.DiscordCombinedStrategy)

    fun scanQuickFallbackEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.ScanQuickFallback)

    fun discordOffOnTvEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.DiscordOffOnTv)

    fun secureDnsEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.SecureDns)

    fun aiAppsInBypassEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.AiAppsInBypass)

    fun batterySaverEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.BatterySaver)

    fun youtubeVideoStrategiesEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.YoutubeVideoStrategies)

    fun videoFastFailEnabled(prefs: SharedPreferences): Boolean =
        featureEnabled(prefs, CompatFeature.VideoFastFail)

    const val GEMINI_PACKAGE = "com.google.android.apps.bard"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    fun vpnAppsFor(prefs: SharedPreferences, selected: Set<String>): Set<String> =
        if (GEMINI_PACKAGE in selected && featureEnabled(prefs, CompatFeature.GeminiWithGoogleApp)) selected + GOOGLE_APP_PACKAGE else selected

    fun adoptLatestForFreshInstall(prefs: SharedPreferences) {
        if (prefs.contains(KEY_BEHAVIOR_VERSION) || prefs.contains(KEY_LEGACY_PROFILE)) return
        prefs.edit()
            .putString(KEY_BEHAVIOR_VERSION, latestVersion)
            .putString(KEY_PROMPTED_UP_TO, latestVersion)
            .apply()
    }

    fun shouldPromptAfterUpdate(prefs: SharedPreferences, currentVersionCode: Int): Boolean {
        if (prefs.getInt(KEY_LAST_RUN_VERSION_CODE, 0) >= currentVersionCode) return false
        if (!prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)) return false
        val alreadyAsked = prefs.getString(KEY_PROMPTED_UP_TO, null) ?: legacyPromptedUpTo(prefs)
        val newestKnown = listOfNotNull(behaviorVersion(prefs), alreadyAsked).maxWith(VersionOrder)
        return VersionOrder.compare(latestVersion, newestKnown) > 0
    }

    fun markPromptShown(prefs: SharedPreferences, versionCode: Int) {
        pinBehaviorVersion(prefs)
        prefs.edit()
            .putInt(KEY_LAST_RUN_VERSION_CODE, versionCode)
            .putString(KEY_PROMPTED_UP_TO, latestVersion)
            .apply()
    }

    fun markVersionSeen(prefs: SharedPreferences, versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_RUN_VERSION_CODE, versionCode).apply()
    }

    private fun legacyPromptedUpTo(prefs: SharedPreferences): String? =
        if (prefs.contains(KEY_LEGACY_PROFILE)) PROFILE_ERA_VERSION else null

    private fun pinBehaviorVersion(prefs: SharedPreferences) {
        if (prefs.contains(KEY_BEHAVIOR_VERSION)) return
        prefs.edit()
            .putString(KEY_BEHAVIOR_VERSION, behaviorVersion(prefs))
            .remove(KEY_LEGACY_PROFILE)
            .apply()
    }

    fun tunnelDnsMode(prefs: SharedPreferences): TunnelDnsMode {
        prefs.getString(KEY_TUNNEL_DNS_MODE, null)?.let { return TunnelDnsMode.fromStorage(it) }
        return if (featureEnabled(prefs, CompatFeature.SystemTunnelDns)) TunnelDnsMode.System else TunnelDnsMode.Cloudflare
    }

    fun customTunnelDns(prefs: SharedPreferences): String =
        prefs.getString(KEY_TUNNEL_DNS_CUSTOM, "").orEmpty()

    private fun tunnelDnsModeByVersion(prefs: SharedPreferences): TunnelDnsMode =
        if (featureEnabled(prefs, CompatFeature.SystemTunnelDns)) TunnelDnsMode.System else TunnelDnsMode.Cloudflare

    fun setTunnelDnsMode(prefs: SharedPreferences, mode: TunnelDnsMode) {
        pinBehaviorVersion(prefs)
        if (mode != TunnelDnsMode.Custom && mode == tunnelDnsModeByVersion(prefs)) {
            prefs.edit().remove(KEY_TUNNEL_DNS_MODE).apply()
        } else {
            prefs.edit().putString(KEY_TUNNEL_DNS_MODE, mode.storageValue).apply()
        }
    }

    fun setCustomTunnelDns(prefs: SharedPreferences, raw: String, name: String? = null): Boolean {
        val servers = parseDnsServers(raw)
        if (servers.isEmpty()) return false
        pinBehaviorVersion(prefs)
        val editor = prefs.edit()
            .putString(KEY_TUNNEL_DNS_CUSTOM, servers.joinToString(", "))
            .putString(KEY_TUNNEL_DNS_MODE, TunnelDnsMode.Custom.storageValue)
        if (name != null) editor.putString(KEY_TUNNEL_DNS_NAME, name) else editor.remove(KEY_TUNNEL_DNS_NAME)
        editor.apply()
        return true
    }

    fun customTunnelDnsName(prefs: SharedPreferences): String? =
        prefs.getString(KEY_TUNNEL_DNS_NAME, null)?.takeIf { it.isNotBlank() }

    fun tunnelDnsServers(prefs: SharedPreferences, privateDnsConfigured: Boolean): List<String> {
        if (privateDnsConfigured) return emptyList()
        return when (tunnelDnsMode(prefs)) {
            TunnelDnsMode.System -> emptyList()
            TunnelDnsMode.Cloudflare -> listOf(CLOUDFLARE_DNS)
            TunnelDnsMode.Custom -> parseDnsServers(customTunnelDns(prefs))
        }
    }

    fun parseDnsServers(raw: String): List<String> =
        raw.split(dnsSeparators)
            .filter { it.isNotEmpty() && isIpLiteral(it) }
            .distinct()
            .take(MAX_CUSTOM_DNS_SERVERS)

    fun isIpLiteral(value: String): Boolean =
        if (value.contains(':')) isIpv6Literal(value) else isIpv4Literal(value)

    private fun isIpv4Literal(value: String): Boolean {
        val octets = value.split('.')
        if (octets.size != 4) return false
        return octets.all { octet ->
            octet.isNotEmpty() && octet.length <= 3 && octet.all { it.isDigit() } && octet.toInt() in 0..255
        }
    }

    private fun isIpv6Literal(value: String): Boolean {
        if (value.count { it == ':' } < 2 || value.contains(":::")) return false
        val compressedParts = value.split("::")
        if (compressedParts.size > 2) return false
        val isCompressed = compressedParts.size == 2
        val groups = compressedParts
            .flatMap { part -> if (part.isEmpty()) emptyList() else part.split(':') }
            .toMutableList()
        var groupCount = groups.size
        val lastGroup = groups.lastOrNull()
        if (lastGroup != null && lastGroup.contains('.')) {
            if (!isIpv4Literal(lastGroup)) return false
            groups.removeAt(groups.lastIndex)
            groupCount += 1
        }
        val groupsAreHex = groups.all { group ->
            group.isNotEmpty() && group.length <= 4 && group.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        }
        if (!groupsAreHex) return false
        return if (isCompressed) groupCount < 8 else groupCount == 8
    }
}
