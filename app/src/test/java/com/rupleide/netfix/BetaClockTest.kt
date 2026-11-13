package com.rupleide.netfix

import com.rupleide.netfix.core.beta.BetaClock
import com.rupleide.netfix.core.beta.TrustedClockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaClockTest {

    private val expiresAt = 1795208400000L
    private val nov10 = 1794258000000L
    private val nov25 = 1795640400000L
    private val hour = 3_600_000L

    @Test
    fun honestClockBeforeDeadlineIsNotExpired() {
        val now = BetaClock.effectiveNowMs(nov10, 1000L, 3, TrustedClockState(), 0L)
        assertFalse(BetaClock.isExpired(now, expiresAt))
    }

    @Test
    fun honestClockAfterDeadlineIsExpired() {
        val now = BetaClock.effectiveNowMs(nov25, 1000L, 3, TrustedClockState(), 0L)
        assertTrue(BetaClock.isExpired(now, expiresAt))
    }

    @Test
    fun movingDeviceClockBackDoesNotHelpAfterNetworkSample() {
        val sampled = BetaClock.withNetworkSample(TrustedClockState(), nov25, 10_000L, 3)
        val now = BetaClock.effectiveNowMs(nov10, 10_000L + hour, 3, sampled, 0L)
        assertEquals(nov25 + hour, now)
        assertTrue(BetaClock.isExpired(now, expiresAt))
    }

    @Test
    fun rebootAndClockBackStillStopsAtHighWaterMark() {
        val sampled = BetaClock.withNetworkSample(TrustedClockState(), nov25, 10_000L, 3)
        val now = BetaClock.effectiveNowMs(nov10, 500L, 4, sampled, 0L)
        assertEquals(nov25, now)
        assertTrue(BetaClock.isExpired(now, expiresAt))
    }

    @Test
    fun deviceClockBeforeBuildDateIsRaisedToBuildDate() {
        val now = BetaClock.effectiveNowMs(0L, 500L, 4, TrustedClockState(), nov10)
        assertEquals(nov10, now)
    }

    @Test
    fun implausibleNetworkTimeIsIgnored() {
        val state = TrustedClockState(highWaterMs = nov10)
        assertEquals(state, BetaClock.withNetworkSample(state, 1000L, 10L, 1))
    }

    @Test
    fun releaseBuildNeverExpires() {
        assertFalse(BetaClock.isExpired(nov25, 0L))
    }

    @Test
    fun deadlineIsEndOfNovember20InMoscow() {
        val lastMinute = expiresAt - 60_000L
        assertFalse(BetaClock.isExpired(lastMinute, expiresAt))
        assertTrue(BetaClock.isExpired(expiresAt, expiresAt))
    }
}
