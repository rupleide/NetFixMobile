package com.rupleide.netfix

import com.rupleide.netfix.core.compat.CompatFeature
import com.rupleide.netfix.core.compat.CompatibilityManager
import com.rupleide.netfix.core.dpibypass.VideoFastFail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFastFailTest {

    @Test
    fun wifiGetsShortAckTimeout() {
        assertEquals(
            listOf("-T", "1.5:0:3:1", "--keep-timeout-suffix", "googlevideo.com", "--quic-giveup", "400"),
            VideoFastFail.argsFor(arrayOf("ciadpi", "-r5+s", "-s25+s"), mobile = false)
        )
    }

    @Test
    fun mobileGetsLongerAckTimeout() {
        assertEquals(
            listOf("-T", "2.5:0:3:1", "--keep-timeout-suffix", "googlevideo.com", "--quic-giveup", "400"),
            VideoFastFail.argsFor(arrayOf("ciadpi", "-r5+s"), mobile = true)
        )
    }

    @Test
    fun ownTimeoutInStrategyIsKept() {
        val quicOnly = listOf("--quic-giveup", "400")
        assertEquals(quicOnly, VideoFastFail.argsFor(arrayOf("ciadpi", "-T", "3", "-r5+s"), mobile = false))
        assertEquals(quicOnly, VideoFastFail.argsFor(arrayOf("ciadpi", "--timeout=3"), mobile = false))
        assertEquals(quicOnly, VideoFastFail.argsFor(arrayOf("ciadpi", "-T3"), mobile = false))
        assertEquals(emptyList<String>(), VideoFastFail.argsFor(arrayOf("ciadpi", "-T3", "--quic-giveup", "900"), mobile = false))
    }

    @Test
    fun offWhenKeepingPreviousVersion() {
        val prefs = InMemoryPreferences()
        CompatibilityManager.adoptLatestForFreshInstall(prefs)
        assertTrue(CompatibilityManager.videoFastFailEnabled(prefs))
        CompatibilityManager.setBehaviorVersion(prefs, "1.0.4")
        assertFalse(CompatibilityManager.videoFastFailEnabled(prefs))
        assertFalse(CompatibilityManager.featureEnabled(prefs, CompatFeature.VideoFastFail))
    }
}
