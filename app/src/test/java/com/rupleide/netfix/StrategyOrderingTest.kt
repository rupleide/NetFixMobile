package com.rupleide.netfix

import com.rupleide.netfix.core.dpibypass.StrategyOrdering
import org.junit.Assert.assertEquals
import org.junit.Test

class StrategyOrderingTest {
    private val latencies = mapOf("a" to 180L, "b" to 42L, "c" to -1L, "d" to 0L, "e" to 42L)

    @Test
    fun fastestComesFirstAndUnmeasuredLast() {
        val sorted = StrategyOrdering.sortedByLatency(listOf("a", "b", "c", "d", "e")) { latencies[it] }
        assertEquals(listOf("b", "e", "a", "c", "d"), sorted)
    }

    @Test
    fun missingResultCountsAsUnmeasured() {
        val sorted = StrategyOrdering.sortedByLatency(listOf("x", "a")) { latencies[it] }
        assertEquals(listOf("a", "x"), sorted)
    }

    @Test
    fun equalLatencyKeepsOriginalOrder() {
        val sorted = StrategyOrdering.sortedByLatency(listOf("e", "b")) { latencies[it] }
        assertEquals(listOf("e", "b"), sorted)
    }

    @Test
    fun keyIsMaxForNonPositive() {
        assertEquals(Long.MAX_VALUE, StrategyOrdering.latencySortKey(null))
        assertEquals(Long.MAX_VALUE, StrategyOrdering.latencySortKey(-1L))
        assertEquals(Long.MAX_VALUE, StrategyOrdering.latencySortKey(0L))
        assertEquals(15L, StrategyOrdering.latencySortKey(15L))
    }

    @Test
    fun mostOpenedComesFirstThenFaster() {
        val opened = mapOf("a" to (3 to 6), "b" to (6 to 6), "e" to (6 to 6), "d" to (0 to 0))
        val criteria = listOf(
            StrategyOrdering.byOpened<String> { opened[it] },
            StrategyOrdering.byLatency { latencies[it] }
        )
        assertEquals(listOf("e", "b", "a", "d", "x"), StrategyOrdering.sortedByCriteria(listOf("x", "d", "a", "e", "b"), criteria))
    }

    @Test
    fun latencyFirstThenOpenedBreaksTies() {
        val opened = mapOf("b" to (3 to 6), "e" to (6 to 6))
        val criteria = listOf(
            StrategyOrdering.byLatency<String> { latencies[it] },
            StrategyOrdering.byOpened { opened[it] }
        )
        assertEquals(listOf("e", "b", "a"), StrategyOrdering.sortedByCriteria(listOf("a", "b", "e"), criteria))
    }

    @Test
    fun noCriteriaKeepsOrder() {
        assertEquals(listOf("c", "a"), StrategyOrdering.sortedByCriteria(listOf("c", "a"), emptyList()))
    }
}
