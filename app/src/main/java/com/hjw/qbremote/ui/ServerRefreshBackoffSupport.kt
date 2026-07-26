package com.hjw.qbremote.ui

internal const val SERVER_REFRESH_BACKOFF_MAX_MS = 300_000L

internal fun nextServerRetryDelayMs(
    baseIntervalMs: Long,
    consecutiveFailures: Int,
    maxMs: Long = SERVER_REFRESH_BACKOFF_MAX_MS,
): Long {
    val exponent = (consecutiveFailures - 1).coerceIn(0, 6)
    return (baseIntervalMs shl exponent).coerceAtMost(maxMs)
}
