package com.rupleide.netfix

import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.dpibypass.StrategyTester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatV104ParityTest {

    private val lastStrategyOfV104 = "-s1+s -d3+s -a1"

    private fun userWhoKeptV104(isTv: Boolean = false) = InMemoryPreferences().apply {
        edit().putBoolean("onboarding_completed", true).putBoolean("is_smart_tv", isTv).apply()
        CompatibilityManager.markPromptShown(this, 8)
    }

    @Test
    fun everyNetworkSwitchIsOff() {
        val prefs = userWhoKeptV104()
        CompatFeature.entries.forEach { assertFalse(it.name, CompatibilityManager.featureEnabled(prefs, it)) }
    }

    @Test
    fun tunnelDnsIsCloudflareLikeV104() {
        val prefs = userWhoKeptV104()
        assertEquals(TunnelDnsMode.Cloudflare, CompatibilityManager.tunnelDnsMode(prefs))
        assertEquals(listOf("1.1.1.1"), CompatibilityManager.tunnelDnsServers(prefs, false))
        assertEquals(emptyList<String>(), CompatibilityManager.tunnelDnsServers(prefs, true))
    }

    @Test
    fun scanUsesExactlyTheV104StrategyList() {
        val scanned = StrategyTester.scanStrategies(userWhoKeptV104())
        assertEquals(StrategyTester.defaultStrategies.dropLast(1), scanned)
        assertEquals(lastStrategyOfV104, scanned.last())
    }

    @Test
    fun scanChecksTheV104HostSet() {
        assertEquals(StrategyTester.quickSitesFor(includeEch = false), StrategyTester.quickSitesFor(false, true))
        assertEquals(
            StrategyTester.testSitesFor(includeEch = false),
            StrategyTester.testSitesFor(includeEch = false, includeDiscord = true, includeSmartTube = false)
        )
        assertTrue(StrategyTester.discordCheckEnabled(userWhoKeptV104(isTv = true)))
        assertTrue(StrategyTester.discordCheckEnabled(userWhoKeptV104(isTv = false)))
    }

    @Test
    fun choosingV104InSettingsGivesTheSameResult() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(prefs)
        CompatibilityManager.setBehaviorVersion(prefs, "1.0.4")
        CompatFeature.entries.forEach { assertFalse(it.name, CompatibilityManager.featureEnabled(prefs, it)) }
        assertEquals(listOf("1.1.1.1"), CompatibilityManager.tunnelDnsServers(prefs, false))
        assertEquals(lastStrategyOfV104, StrategyTester.scanStrategies(prefs).last())
    }

    @Test
    fun geminiDoesNotPullGoogleAppLikeV104() {
        val selected = setOf(CompatibilityManager.GEMINI_PACKAGE, "org.telegram.messenger")
        assertEquals(selected, CompatibilityManager.vpnAppsFor(userWhoKeptV104(), selected))
        val prefs = userWhoKeptV104()
        CompatibilityManager.enableEverythingNew(prefs)
        assertEquals(selected + CompatibilityManager.GOOGLE_APP_PACKAGE, CompatibilityManager.vpnAppsFor(prefs, selected))
        assertEquals(setOf("org.telegram.messenger"), CompatibilityManager.vpnAppsFor(prefs, setOf("org.telegram.messenger")))
    }

    @Test
    fun switchingBackToDefaultsIsNotCustom() {
        val prefs = userWhoKeptV104()
        CompatibilityManager.setFeature(prefs, CompatFeature.CloudflareUnfreeze, true)
        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.System)
        assertTrue(CompatibilityManager.isCustomized(prefs))
        CompatibilityManager.setFeature(prefs, CompatFeature.CloudflareUnfreeze, false)
        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.Cloudflare)
        assertFalse(CompatibilityManager.isCustomized(prefs))
        assertEquals(listOf("1.1.1.1"), CompatibilityManager.tunnelDnsServers(prefs, false))
        CompatFeature.entries.forEach { assertFalse(it.name, CompatibilityManager.featureEnabled(prefs, it)) }
    }

    @Test
    fun enablingNewGivesTheV105Behavior() {
        val prefs = userWhoKeptV104(isTv = true)
        CompatibilityManager.enableEverythingNew(prefs)
        assertEquals(emptyList<String>(), CompatibilityManager.tunnelDnsServers(prefs, false))
        assertEquals(StrategyTester.YOUTUBE_VIDEO_STRATEGIES + StrategyTester.defaultStrategies, StrategyTester.scanStrategies(prefs))
        assertFalse(StrategyTester.discordCheckEnabled(prefs))
    }
}
