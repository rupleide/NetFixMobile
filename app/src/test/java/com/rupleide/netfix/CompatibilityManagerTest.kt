package com.rupleide.netfix

import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.compat.TunnelDnsMode
import com.rupleide.netfix.core.compat.VersionOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityManagerTest {

    private fun updatedUser(lastRunVersionCode: Int? = null) = InMemoryPreferences().apply {
        val editor = edit().putBoolean("onboarding_completed", true)
        if (lastRunVersionCode != null) editor.putInt("last_run_version_code", lastRunVersionCode)
        editor.apply()
    }

    @Test
    fun existingUserFromPreviousVersionIsPrompted() {
        assertTrue(CompatibilityManager.shouldPromptAfterUpdate(updatedUser(), 8))
        assertTrue(CompatibilityManager.shouldPromptAfterUpdate(updatedUser(lastRunVersionCode = 7), 8))
    }

    @Test
    fun freshInstallBeforeOnboardingIsNotPrompted() {
        assertFalse(CompatibilityManager.shouldPromptAfterUpdate(InMemoryPreferences(), 8))
    }

    @Test
    fun sameVersionIsNotPrompted() {
        assertFalse(CompatibilityManager.shouldPromptAfterUpdate(updatedUser(lastRunVersionCode = 8), 8))
    }

    @Test
    fun updatedUserKeepsOldBehaviorUntilAnswering() {
        val prefs = updatedUser()
        assertEquals(CompatibilityManager.BASELINE_VERSION, CompatibilityManager.behaviorVersion(prefs))
        assertTrue(CompatFeature.entries.none { CompatibilityManager.featureEnabled(prefs, it) })
    }

    @Test
    fun keepingOldBehaviorIsNotAskedAgainUntilNewFeaturesAppear() {
        val prefs = updatedUser()
        CompatibilityManager.markPromptShown(prefs, 8)
        assertFalse(CompatibilityManager.shouldPromptAfterUpdate(prefs, 9))
        assertEquals(CompatibilityManager.BASELINE_VERSION, CompatibilityManager.behaviorVersion(prefs))
    }

    @Test
    fun userAskedForOlderFeatureSetIsAskedWhenNewerFeaturesShip() {
        val prefs = updatedUser()
        prefs.edit()
            .putString(CompatibilityManager.KEY_BEHAVIOR_VERSION, "1.0.4")
            .putString(CompatibilityManager.KEY_PROMPTED_UP_TO, "1.0.4")
            .apply()
        assertTrue(CompatibilityManager.shouldPromptAfterUpdate(prefs, 9))
    }

    @Test
    fun enablingEverythingNewTurnsOnAllFeatures() {
        val prefs = updatedUser()
        CompatibilityManager.markPromptShown(prefs, 8)
        CompatibilityManager.enableEverythingNew(prefs)
        assertTrue(CompatFeature.entries.all { CompatibilityManager.featureEnabled(prefs, it) })
        assertTrue(CompatibilityManager.isLatestBehavior(prefs))
        assertFalse(CompatibilityManager.shouldPromptAfterUpdate(prefs, 9))
    }

    @Test
    fun freshInstallGetsEverythingNewWithoutOverridingChoice() {
        val fresh = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(fresh)
        assertEquals(CompatibilityManager.latestVersion, CompatibilityManager.behaviorVersion(fresh))
        assertTrue(CompatFeature.entries.all { CompatibilityManager.featureEnabled(fresh, it) })

        val chosen = InMemoryPreferences()
        CompatibilityManager.setBehaviorVersion(chosen, "1.0.4")
        CompatibilityManager.adoptLatestForFreshInstall(chosen)
        assertEquals("1.0.4", CompatibilityManager.behaviorVersion(chosen))
    }

    @Test
    fun freshInstallIsNotPromptedAfterItsFirstUpdate() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(prefs)
        prefs.edit().putBoolean("onboarding_completed", true).putInt("last_run_version_code", 8).apply()
        assertFalse(CompatibilityManager.shouldPromptAfterUpdate(prefs, 9))
    }

    @Test
    fun profilesFromEarlierBuildsAreMigrated() {
        val legacy = updatedUser()
        legacy.edit().putString("compat_profile", "legacy").apply()
        assertEquals("1.0.4", CompatibilityManager.behaviorVersion(legacy))
        assertFalse(CompatibilityManager.shouldPromptAfterUpdate(legacy, 9))

        val optimized = updatedUser()
        optimized.edit().putString("compat_profile", "optimized").apply()
        assertEquals("1.0.5", CompatibilityManager.behaviorVersion(optimized))
        assertTrue(CompatibilityManager.featureEnabled(optimized, CompatFeature.StreamProbe))
    }

    @Test
    fun versionsCompareNumerically() {
        assertTrue(VersionOrder.compare("1.0.10", "1.0.9") > 0)
        assertTrue(VersionOrder.compare("1.0.4", "1.0.5") < 0)
        assertEquals(0, VersionOrder.compare("1.0.5", "1.0.5"))
    }

    @Test
    fun behaviorChoicesStartFromBaseline() {
        assertEquals(listOf("1.0.4", "1.0.5"), CompatibilityManager.behaviorChoices)
        assertEquals("1.0.5", CompatibilityManager.latestVersion)
    }

    @Test
    fun systemDnsGivesEmptyServerList() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(prefs)
        assertEquals(emptyList<String>(), CompatibilityManager.tunnelDnsServers(prefs, false))
    }

    @Test
    fun cloudflareDnsGivesOneOneOneOneAndIsNotNetworkTweak() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(prefs)
        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.Cloudflare)
        assertEquals(listOf("1.1.1.1"), CompatibilityManager.tunnelDnsServers(prefs, false))
        assertFalse(CompatibilityManager.isCustomized(prefs))
    }

    @Test
    fun choosingNewBehaviorKeepsTunnelDnsChoice() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(prefs)
        CompatibilityManager.setCustomTunnelDns(prefs, "212.109.195.93", "Comss")
        CompatibilityManager.enableEverythingNew(prefs)
        assertEquals(TunnelDnsMode.Custom, CompatibilityManager.tunnelDnsMode(prefs))
        CompatibilityManager.setBehaviorVersion(prefs, "1.0.4")
        assertEquals(TunnelDnsMode.Cloudflare, CompatibilityManager.tunnelDnsMode(prefs))
    }

    @Test
    fun privateDnsAlwaysWinsOverTunnelDns() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.Cloudflare)
        assertEquals(emptyList<String>(), CompatibilityManager.tunnelDnsServers(prefs, true))
    }

    @Test
    fun customDnsKeepsOnlyValidLiteralsAndLimitsToTwo() {
        val prefs = InMemoryPreferences()
        assertTrue(CompatibilityManager.setCustomTunnelDns(prefs, "8.8.8.8, bad.host 999.1.1.1 2001:4860:4860::8888 9.9.9.9"))
        assertEquals(listOf("8.8.8.8", "2001:4860:4860::8888"), CompatibilityManager.tunnelDnsServers(prefs, false))
    }

    @Test
    fun customDnsWithoutValidAddressIsRejected() {
        val prefs = InMemoryPreferences()
        assertFalse(CompatibilityManager.setCustomTunnelDns(prefs, "dns.example.com"))
        assertFalse(prefs.contains(CompatibilityManager.KEY_TUNNEL_DNS_MODE))
    }

    @Test
    fun ipLiteralValidation() {
        assertTrue(CompatibilityManager.isIpLiteral("1.1.1.1"))
        assertTrue(CompatibilityManager.isIpLiteral("::1"))
        assertTrue(CompatibilityManager.isIpLiteral("2606:4700:4700::1111"))
        assertFalse(CompatibilityManager.isIpLiteral("256.1.1.1"))
        assertFalse(CompatibilityManager.isIpLiteral("1.1.1"))
        assertFalse(CompatibilityManager.isIpLiteral("1:2:3:4:5:6:7:8:9"))
        assertFalse(CompatibilityManager.isIpLiteral("example.com"))
    }

    @Test
    fun choosingVersionClearsFineTuningAndRestoresSystemDns() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.enableEverythingNew(prefs)
        CompatibilityManager.setFeature(prefs, CompatFeature.FreeProxyPort, false)
        CompatibilityManager.setTunnelDnsMode(prefs, TunnelDnsMode.Cloudflare)
        assertTrue(CompatibilityManager.isCustomized(prefs))
        CompatibilityManager.setBehaviorVersion(prefs, "1.0.4")
        assertFalse(CompatibilityManager.isCustomized(prefs))
        assertEquals(TunnelDnsMode.Cloudflare, CompatibilityManager.tunnelDnsMode(prefs))
        assertTrue(CompatFeature.entries.none { CompatibilityManager.featureEnabled(prefs, it) })
    }

    @Test
    fun switchingOneFeatureKeepsTheOthers() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.enableEverythingNew(prefs)
        CompatibilityManager.setFeature(prefs, CompatFeature.EthernetScan, false)
        assertFalse(CompatibilityManager.ethernetScanEnabled(prefs))
        assertTrue(CompatibilityManager.freeProxyPortEnabled(prefs))
        assertTrue(CompatibilityManager.discordEchCheckEnabled(prefs))
    }

    @Test
    fun oldBehaviorThenOneFeatureEnabledKeepsTheRestOff() {
        val prefs = updatedUser()
        CompatibilityManager.setFeature(prefs, CompatFeature.FreeProxyPort, true)
        assertTrue(CompatibilityManager.freeProxyPortEnabled(prefs))
        assertFalse(CompatibilityManager.ethernetScanEnabled(prefs))
        assertEquals("1.0.4", prefs.getString(CompatibilityManager.KEY_BEHAVIOR_VERSION, null))
    }

    @Test
    fun featuresAreGroupedByVersion() {
        assertEquals(listOf("1.0.5"), CompatFeature.versions)
        assertEquals(CompatFeature.entries.toList(), CompatFeature.inVersion("1.0.5"))
    }
}
