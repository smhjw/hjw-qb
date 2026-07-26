package com.hjw.qbremote.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerRefreshBackoffSupportTest {

    @Test
    fun nextServerRetryDelayMs_growsExponentiallyAndCapsAt300Seconds() {
        assertEquals(
            listOf(5_000L, 10_000L, 20_000L, 40_000L, 80_000L, 160_000L, 300_000L, 300_000L),
            (1..8).map { streak -> nextServerRetryDelayMs(baseIntervalMs = 5_000L, consecutiveFailures = streak) },
        )
    }

    @Test
    fun nextServerRetryDelayMs_nonPositiveStreakUsesBaseInterval() {
        assertEquals(5_000L, nextServerRetryDelayMs(baseIntervalMs = 5_000L, consecutiveFailures = 0))
        assertEquals(5_000L, nextServerRetryDelayMs(baseIntervalMs = 5_000L, consecutiveFailures = -3))
    }

    @Test
    fun nextServerRetryDelayMs_capsOversizedBaseIntervalAtDefaultMax() {
        assertEquals(
            SERVER_REFRESH_BACKOFF_MAX_MS,
            nextServerRetryDelayMs(baseIntervalMs = 400_000L, consecutiveFailures = 1),
        )
    }

    @Test
    fun nextServerRetryDelayMs_respectsCustomMax() {
        assertEquals(
            30_000L,
            nextServerRetryDelayMs(baseIntervalMs = 5_000L, consecutiveFailures = 4, maxMs = 30_000L),
        )
    }
}
