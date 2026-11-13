package com.rupleide.netfix.core.tgproxy

data class TgProxyStatsSample(
    val totalConnections: Long,
    val routedConnections: Long,
    val downloaded: String
) {
    companion object {
        fun parse(raw: String): TgProxyStatsSample? {
            val fields = raw.trim().split(' ')
                .mapNotNull { token ->
                    val separator = token.indexOf('=')
                    if (separator <= 0) null else token.substring(0, separator) to token.substring(separator + 1)
                }
                .toMap()
            val total = fields["total"]?.toLongOrNull() ?: return null
            val ws = fields["ws"]?.toLongOrNull() ?: return null
            val tcp = fields["tcp_fb"]?.toLongOrNull() ?: return null
            val cf = fields["cf"]?.toLongOrNull() ?: return null
            return TgProxyStatsSample(total, ws + tcp + cf, fields["down"].orEmpty())
        }
    }
}

enum class TgProxyRecoveryAction { None, FlushPools, RestartCore }

class TgProxyRecoveryPlanner(
    private val windowSize: Int = 4,
    private val minNewConnections: Long = 4,
    private val maxRestarts: Int = 3,
    private val restartWindowMs: Long = 30 * 60_000L
) {
    private val samples = ArrayDeque<TgProxyStatsSample>()
    private val restartTimes = ArrayDeque<Long>()
    private var flushedInThisStall = false

    fun onSample(sample: TgProxyStatsSample, nowMs: Long): TgProxyRecoveryAction {
        samples.addLast(sample)
        while (samples.size > windowSize) samples.removeFirst()
        if (samples.size < windowSize) return TgProxyRecoveryAction.None

        val first = samples.first()
        val last = samples.last()
        val progressed = last.routedConnections > first.routedConnections || last.downloaded != first.downloaded
        if (progressed) {
            flushedInThisStall = false
            return TgProxyRecoveryAction.None
        }
        if (last.totalConnections - first.totalConnections < minNewConnections) return TgProxyRecoveryAction.None

        if (!flushedInThisStall) {
            flushedInThisStall = true
            samples.clear()
            return TgProxyRecoveryAction.FlushPools
        }

        while (restartTimes.isNotEmpty() && nowMs - restartTimes.first() > restartWindowMs) restartTimes.removeFirst()
        samples.clear()
        if (restartTimes.size >= maxRestarts) return TgProxyRecoveryAction.None
        restartTimes.addLast(nowMs)
        return TgProxyRecoveryAction.RestartCore
    }

    fun onNetworkChanged() {
        samples.clear()
        restartTimes.clear()
        flushedInThisStall = false
    }
}
