package com.rupleide.netfix.core.beta

data class TrustedClockState(
    val highWaterMs: Long = 0L,
    val trustedNowMs: Long = 0L,
    val trustedElapsedMs: Long = 0L,
    val trustedBootCount: Int = -1
)

object BetaClock {
    const val MIN_PLAUSIBLE_NETWORK_TIME_MS = 1793480400000L

    fun effectiveNowMs(
        deviceNowMs: Long,
        elapsedSinceBootMs: Long,
        bootCount: Int,
        state: TrustedClockState,
        builtAtMs: Long
    ): Long {
        val sameBootAsTrustedSample = state.trustedNowMs > 0L &&
            bootCount >= 0 &&
            bootCount == state.trustedBootCount &&
            elapsedSinceBootMs >= state.trustedElapsedMs
        val extrapolatedFromNetwork = if (sameBootAsTrustedSample) {
            state.trustedNowMs + (elapsedSinceBootMs - state.trustedElapsedMs)
        } else {
            0L
        }
        return maxOf(deviceNowMs, state.highWaterMs, extrapolatedFromNetwork, builtAtMs)
    }

    fun withNetworkSample(
        state: TrustedClockState,
        networkNowMs: Long,
        elapsedSinceBootMs: Long,
        bootCount: Int
    ): TrustedClockState {
        if (networkNowMs < MIN_PLAUSIBLE_NETWORK_TIME_MS) return state
        return state.copy(
            highWaterMs = maxOf(state.highWaterMs, networkNowMs),
            trustedNowMs = networkNowMs,
            trustedElapsedMs = elapsedSinceBootMs,
            trustedBootCount = bootCount
        )
    }

    fun isExpired(effectiveNowMs: Long, expiresAtMs: Long): Boolean =
        expiresAtMs > 0L && effectiveNowMs >= expiresAtMs
}
