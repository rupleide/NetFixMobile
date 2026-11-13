package com.rupleide.netfix

import com.rupleide.netfix.core.tgproxy.TgProxyRecoveryAction
import com.rupleide.netfix.core.tgproxy.TgProxyRecoveryPlanner
import com.rupleide.netfix.core.tgproxy.TgProxyStatsSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TgProxyRecoveryPlannerTest {

    private fun stalled(total: Long) = TgProxyStatsSample(total, 10, "1.0 MB")

    private fun TgProxyRecoveryPlanner.feedStall(startTotal: Long, nowMs: Long): TgProxyRecoveryAction {
        var action = TgProxyRecoveryAction.None
        for (step in 0 until 4) action = onSample(stalled(startTotal + step * 2), nowMs + step * 30_000L)
        return action
    }

    @Test
    fun parsesStatsLine() {
        val sample = TgProxyStatsSample.parse("total=12 active=2 ws=5 tcp_fb=1 cf=3 bad=0 err=2 pool=3/4 up=1.0 KB down=2.5 MB")
        assertNotNull(sample)
        assertEquals(12L, sample!!.totalConnections)
        assertEquals(9L, sample.routedConnections)
        assertEquals("2.5", sample.downloaded.take(3))
    }

    @Test
    fun malformedStatsAreIgnored() {
        assertNull(TgProxyStatsSample.parse(""))
        assertNull(TgProxyStatsSample.parse("total=x ws=1 tcp_fb=1 cf=1"))
    }

    @Test
    fun idleProxyIsNeverTreatedAsStalled() {
        val planner = TgProxyRecoveryPlanner()
        repeat(10) { assertEquals(TgProxyRecoveryAction.None, planner.onSample(stalled(5), it * 30_000L)) }
    }

    @Test
    fun healthyTrafficIsNotTreatedAsStalled() {
        val planner = TgProxyRecoveryPlanner()
        for (step in 0 until 8) {
            val sample = TgProxyStatsSample(step * 3L, step * 3L, "${step} MB")
            assertEquals(TgProxyRecoveryAction.None, planner.onSample(sample, step * 30_000L))
        }
    }

    @Test
    fun stallFirstFlushesPoolsThenRestartsCore() {
        val planner = TgProxyRecoveryPlanner()
        assertEquals(TgProxyRecoveryAction.FlushPools, planner.feedStall(0, 0))
        assertEquals(TgProxyRecoveryAction.RestartCore, planner.feedStall(20, 200_000))
    }

    @Test
    fun restartsAreLimitedWithinTheWindow() {
        val planner = TgProxyRecoveryPlanner()
        planner.feedStall(0, 0)
        val actions = (1..5).map { planner.feedStall(it * 20L, it * 200_000L) }
        assertEquals(3, actions.count { it == TgProxyRecoveryAction.RestartCore })
        assertEquals(TgProxyRecoveryAction.None, actions.last())
    }

    @Test
    fun restartBudgetRecoversAfterTheWindowPasses() {
        val planner = TgProxyRecoveryPlanner()
        planner.feedStall(0, 0)
        repeat(3) { planner.feedStall(100L * (it + 1), 100_000L * (it + 1)) }
        assertEquals(TgProxyRecoveryAction.RestartCore, planner.feedStall(1000, 40 * 60_000L))
    }

    @Test
    fun networkChangeGivesAFreshStart() {
        val planner = TgProxyRecoveryPlanner()
        planner.feedStall(0, 0)
        planner.onNetworkChanged()
        assertEquals(TgProxyRecoveryAction.FlushPools, planner.feedStall(50, 300_000))
    }

    private fun realLine(total: Int, active: Int, cf: Int, down: String) =
        TgProxyStatsSample.parse("total=$total active=$active ws=0 tcp_fb=0 cf=$cf bad=10 err=0 pool=0/0 up=4.0MB down=$down")!!

    @Test
    fun beelineStallFromUserLogIsDetected() {
        val planner = TgProxyRecoveryPlanner()
        val stall = listOf(
            realLine(363, 2, 317, "67.1MB"),
            realLine(364, 1, 317, "67.1MB"),
            realLine(365, 1, 317, "67.1MB"),
            realLine(368, 3, 317, "67.1MB")
        )
        val actions = stall.mapIndexed { index, sample -> planner.onSample(sample, index * 30_000L) }
        assertEquals(TgProxyRecoveryAction.FlushPools, actions.last())
    }

    @Test
    fun quietBeelineHourFromUserLogIsNotTreatedAsStall() {
        val planner = TgProxyRecoveryPlanner()
        val quiet = listOf(
            realLine(343, 1, 302, "63.5MB"),
            realLine(343, 0, 302, "63.5MB"),
            realLine(346, 3, 302, "63.5MB"),
            realLine(347, 1, 306, "63.5MB"),
            realLine(347, 0, 306, "63.5MB"),
            realLine(350, 2, 308, "63.5MB")
        )
        quiet.forEachIndexed { index, sample ->
            assertEquals(TgProxyRecoveryAction.None, planner.onSample(sample, index * 30_000L))
        }
    }
}
