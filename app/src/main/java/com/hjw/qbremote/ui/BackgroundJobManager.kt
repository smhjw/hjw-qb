package com.hjw.qbremote.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

internal class BackgroundJobManager(
    private val scope: CoroutineScope,
    private val getState: () -> MainUiState,
    private val onAutoRefresh: suspend () -> Unit,
    private val onHomeChartRefresh: suspend () -> Unit,
    private val onHourlyBoundaryRefresh: suspend () -> Unit = onAutoRefresh,
    private val awaitForeground: suspend () -> Unit = {},
) {
    private var autoRefreshJob: Job? = null
    private var homeChartRefreshJob: Job? = null
    private var hourlyBoundaryRefreshJob: Job? = null

    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = scope.launch {
            while (isActive) {
                awaitForeground()
                val state = getState()
                val base = state.settings.refreshSeconds.coerceIn(5, 120)
                val adaptiveSeconds = when (state.refreshScene) {
                    RefreshScene.TORRENT_DETAIL -> base
                    RefreshScene.SETTINGS -> (base * 2).coerceIn(10, 120)
                    RefreshScene.DASHBOARD -> base
                    RefreshScene.SERVER -> base
                }
                delay(adaptiveSeconds * 1000L)
                if (getState().connected) onAutoRefresh()
            }
        }
    }

    fun startHomeChartRefresh() {
        homeChartRefreshJob?.cancel()
        homeChartRefreshJob = scope.launch {
            while (isActive) {
                awaitForeground()
                val intervalSeconds = resolveHomeSpeedRefreshIntervalSeconds(getState().refreshScene)
                if (intervalSeconds == null) {
                    delay(1_000L)
                    continue
                }
                delay(intervalSeconds * 1_000L)
                onHomeChartRefresh()
            }
        }
    }

    fun startHourlyBoundaryRefresh() {
        hourlyBoundaryRefreshJob?.cancel()
        hourlyBoundaryRefreshJob = scope.launch {
            while (isActive) {
                awaitForeground()
                val now = ZonedDateTime.now()
                val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
                val untilNext = Duration.between(now, nextHour).toMillis().coerceAtLeast(1_000L)
                delay(untilNext)
                if (getState().connected) onHourlyBoundaryRefresh()
            }
        }
    }

    fun stopAll() {
        autoRefreshJob?.cancel()
        homeChartRefreshJob?.cancel()
        hourlyBoundaryRefreshJob?.cancel()
        autoRefreshJob = null
        homeChartRefreshJob = null
        hourlyBoundaryRefreshJob = null
    }
}
