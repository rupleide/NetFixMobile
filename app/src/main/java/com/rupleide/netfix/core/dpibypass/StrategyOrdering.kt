package com.rupleide.netfix.core.dpibypass

object StrategyOrdering {
    fun latencySortKey(averageMs: Long?): Long =
        if (averageMs == null || averageMs <= 0L) Long.MAX_VALUE else averageMs

    fun <T> sortedByLatency(items: List<T>, averageMsOf: (T) -> Long?): List<T> =
        items.sortedBy { latencySortKey(averageMsOf(it)) }

    fun openedShareKey(opened: Pair<Int, Int>?): Double =
        opened?.let { (ok, total) -> if (total > 0) ok.toDouble() / total else -1.0 } ?: -2.0

    fun <T> byOpened(openedOf: (T) -> Pair<Int, Int>?): Comparator<T> =
        compareByDescending { openedShareKey(openedOf(it)) }

    fun <T> byLatency(averageMsOf: (T) -> Long?): Comparator<T> =
        compareBy { latencySortKey(averageMsOf(it)) }

    fun <T> sortedByCriteria(items: List<T>, criteria: List<Comparator<T>>): List<T> =
        if (criteria.isEmpty()) items else items.sortedWith(criteria.reduce { first, next -> first.then(next) })
}
