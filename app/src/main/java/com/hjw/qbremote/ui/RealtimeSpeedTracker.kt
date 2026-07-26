package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionStore
import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.model.TransferInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val HOME_REALTIME_SPEED_MIN_SAMPLE_INTERVAL_MS = 1_000L
internal const val HOME_REALTIME_SPEED_MAX_POINTS = 60

// Preferences DataStore rewrites the whole file on every edit; persisting each
// 1s sample caused constant disk write amplification while the dashboard was
// open. Samples stay in memory; disk only sees one write per interval.
internal const val HOME_REALTIME_SPEED_PERSIST_INTERVAL_MS = 15_000L

internal class RealtimeSpeedTracker(
    private val connectionStore: ConnectionStore,
) {
    val mutex = Mutex()
    val series = mutableListOf<RealtimeSpeedPoint>()
    private var restored = false
    private var scopeKey: String? = null
    private var lastPersistAtMs = 0L

    suspend fun sampleLocked(
        transferInfo: TransferInfo,
        onlineServerCount: Int,
        key: String,
    ): List<RealtimeSpeedPoint> {
        scopeKey = key
        restored = true
        val nextPoint = RealtimeSpeedPoint(
            timestamp = System.currentTimeMillis(),
            uploadSpeed = transferInfo.uploadSpeed.coerceAtLeast(0L),
            downloadSpeed = transferInfo.downloadSpeed.coerceAtLeast(0L),
            onlineServerCount = onlineServerCount.coerceAtLeast(0),
        )
        val lastPoint = series.lastOrNull()
        if (
            lastPoint != null &&
            nextPoint.timestamp - lastPoint.timestamp < HOME_REALTIME_SPEED_MIN_SAMPLE_INTERVAL_MS
        ) {
            series[series.lastIndex] = nextPoint
        } else {
            series += nextPoint
        }
        while (series.size > HOME_REALTIME_SPEED_MAX_POINTS) {
            series.removeAt(0)
        }
        val now = nextPoint.timestamp
        if (now - lastPersistAtMs >= HOME_REALTIME_SPEED_PERSIST_INTERVAL_MS) {
            lastPersistAtMs = now
            persistLocked(key, series.toList())
        }
        return series.toList()
    }

    /** Flushes the in-memory series to disk immediately (call on app stop/scope switch). */
    suspend fun flushLocked() {
        val key = scopeKey ?: return
        if (!restored) return
        lastPersistAtMs = System.currentTimeMillis()
        persistLocked(key, series.toList())
    }

    suspend fun clearLocked(key: String) {
        scopeKey = key
        restored = true
        series.clear()
        persistLocked(key, emptyList())
    }

    suspend fun resetLocked(clearPersisted: Boolean) {
        val previousKey = scopeKey
        series.clear()
        restored = false
        scopeKey = null
        if (clearPersisted && !previousKey.isNullOrBlank()) {
            persistLocked(previousKey, emptyList())
        }
    }

    suspend fun ensureLoadedLocked(key: String) {
        if (scopeKey != key) {
            scopeKey = key
            restored = false
            series.clear()
        }
        if (restored) return

        val snapshot = connectionStore.loadHomeAggregateSpeedHistorySnapshot(key)
        val restoredPoints = restoreHomeRealtimeSpeedSeriesForScope(
            snapshot = snapshot,
            scopeKey = key,
            maxPoints = HOME_REALTIME_SPEED_MAX_POINTS,
        )
        series.clear()
        series.addAll(restoredPoints)
        restored = true
    }

    fun resolveScopeKey(snapshots: List<CachedDashboardServerSnapshot>, fallbackKey: String): String {
        return buildHomeRealtimeSpeedScopeKey(
            profileIds = snapshots.map { it.profileId },
            fallbackScopeKey = fallbackKey,
        )
    }

    suspend fun withLock(block: suspend () -> Unit) {
        mutex.withLock { block() }
    }

    suspend fun <T> withLockReturning(block: suspend () -> T): T {
        return mutex.withLock { block() }
    }

    fun currentScopeKey(): String? = scopeKey

    private suspend fun persistLocked(key: String, pointsSnapshot: List<RealtimeSpeedPoint>) {
        connectionStore.saveHomeAggregateSpeedHistorySnapshot(
            scopeKey = key,
            snapshot = HomeAggregateSpeedHistorySnapshot(
                scopeKey = key,
                points = pointsSnapshot.map { point ->
                    HomeSpeedHistoryPoint(
                        timestamp = point.timestamp,
                        uploadSpeed = point.uploadSpeed,
                        downloadSpeed = point.downloadSpeed,
                        onlineServerCount = point.onlineServerCount,
                    )
                },
            ),
        )
    }
}
