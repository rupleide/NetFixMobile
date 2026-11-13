package com.rupleide.netfix

import com.rupleide.netfix.core.dpibypass.StrategyTester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyListTest {
    private val combinedDiscordStrategy =
        "-o1 -r-5+se -a1 -At,r,s -q2 -s2 -s3+s -r3 -s4 -r4 -s5+s -r5+s -s6 -s7+s -r8 -s9+s -Qr -Mh,d,r -a1"

    @Test
    fun combinedDiscordStrategyIsAppendedLast() {
        assertEquals(combinedDiscordStrategy, StrategyTester.defaultStrategies.last())
    }

    @Test
    fun combinedDiscordStrategyKeepsPreviousNumbering() {
        assertEquals(1, StrategyTester.defaultStrategies.count { it == combinedDiscordStrategy })
        assertTrue(StrategyTester.defaultStrategies.first().startsWith("-o1 -d1 -a1"))
    }

    @Test
    fun everyStrategyStartsWithAnOption() {
        assertTrue(StrategyTester.defaultStrategies.all { it.startsWith("-") })
    }

    @Test
    fun echHostIsAddedOnlyWhenRequested() {
        assertFalse(StrategyTester.ECH_PROBE_HOST in StrategyTester.quickSitesFor(false))
        assertFalse(StrategyTester.ECH_PROBE_HOST in StrategyTester.testSitesFor(false))
        assertFalse(StrategyTester.ECH_PROBE_HOST in StrategyTester.discordProbeHostsFor(false))
        assertEquals(StrategyTester.ECH_PROBE_HOST, StrategyTester.quickSitesFor(true).last())
        assertEquals(StrategyTester.ECH_PROBE_HOST, StrategyTester.testSitesFor(true).last())
        assertEquals(StrategyTester.ECH_PROBE_HOST, StrategyTester.discordProbeHostsFor(true).last())
        assertEquals(StrategyTester.discordProbeHostsFor(false).size + 1, StrategyTester.discordProbeHostsFor(true).size)
    }

    @Test
    fun discordHostsAndEchAreDroppedWhenDiscordCheckIsOff() {
        val quick = StrategyTester.quickSitesFor(includeEch = true, includeDiscord = false)
        val full = StrategyTester.testSitesFor(includeEch = true, includeDiscord = false)
        assertTrue(quick.none { "discord" in it } && StrategyTester.ECH_PROBE_HOST !in quick)
        assertTrue(full.none { "discord" in it } && StrategyTester.ECH_PROBE_HOST !in full)
        assertTrue(quick.isNotEmpty() && full.isNotEmpty())
        assertTrue(StrategyTester.discordProbeHostsFor(includeEch = true, includeDiscord = false).isEmpty())
    }

    @Test
    fun smartTubeHostsAreAddedOnlyToFullScanWhenRequested() {
        val withSmartTube = StrategyTester.testSitesFor(includeEch = false, includeSmartTube = true)
        val without = StrategyTester.testSitesFor(includeEch = false)
        assertTrue(StrategyTester.SMARTTUBE_HOSTS.all { it in withSmartTube })
        assertTrue(StrategyTester.SMARTTUBE_HOSTS.none { it in without })
        assertEquals(withSmartTube.size, withSmartTube.distinct().size)
        assertTrue(StrategyTester.SMARTTUBE_HOSTS.none { it in StrategyTester.quickSitesFor(false) })
    }

    @Test
    fun hostBreakdownHidesDiscordWhenNotChecked() {
        val youtubeOnly = com.rupleide.netfix.core.dpibypass.StrategyTestManager.RankedConfig("-o1", 5, 6, 0, 0, 40)
        val both = com.rupleide.netfix.core.dpibypass.StrategyTestManager.RankedConfig("-o1", 5, 6, 4, 6, 40)
        assertEquals("YouTube 5/6", youtubeOnly.hostBreakdown)
        assertEquals("YouTube 5/6 · Discord 4/6", both.hostBreakdown)
        assertTrue(youtubeOnly.isWorking)
    }
}
