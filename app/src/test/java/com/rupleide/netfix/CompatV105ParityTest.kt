package com.rupleide.netfix

import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.tgproxy.TgProxyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatV105ParityTest {

    @Test
    fun keepingV105LeavesNewerFeaturesOff() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.setBehaviorVersion(prefs, "1.0.5")
        CompatFeature.entries
            .filter { CompatibilityManager.behaviorChoices.indexOf(it.sinceVersion) > CompatibilityManager.behaviorChoices.indexOf("1.0.5") }
            .forEach { assertFalse(it.name, CompatibilityManager.featureEnabled(prefs, it)) }
        assertFalse(CompatibilityManager.tgProxyCfFrontEnabled(prefs))
        assertFalse(CompatibilityManager.tgProxyDefaultDcEnabled(prefs))
        assertFalse(CompatibilityManager.tgProxyCfPlainEnabled(prefs))
        assertFalse(CompatibilityManager.scanSecureResolveEnabled(prefs))
        assertTrue(CompatibilityManager.videoFastFailEnabled(prefs))
    }

    @Test
    fun autoDcIpsStayEmptyWhenKeepingV105() {
        assertEquals("", TgProxyController.dcIpsForCore("", defaultDcEnabled = false))
        assertEquals(TgProxyController.DEFAULT_DC_IPS, TgProxyController.dcIpsForCore("", defaultDcEnabled = true))
        assertEquals("2:1.2.3.4", TgProxyController.dcIpsForCore("2:1.2.3.4", defaultDcEnabled = true))
    }

    @Test
    fun everythingNewTurnsTelegramFallbackOn() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.enableEverythingNew(prefs)
        assertTrue(CompatibilityManager.tgProxyCfFrontEnabled(prefs))
        assertTrue(CompatibilityManager.tgProxyDefaultDcEnabled(prefs))
        assertTrue(CompatibilityManager.tgProxyCfPlainEnabled(prefs))
    }
}
