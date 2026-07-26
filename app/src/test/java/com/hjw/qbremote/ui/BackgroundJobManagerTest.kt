package com.hjw.qbremote.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundJobManagerTest {

    @Test
    fun startAutoRefresh_doesNotTickWhileAwaitForegroundSuspends() = runTest {
        val foreground = MutableStateFlow(false)
        var autoRefreshCount = 0
        val manager = BackgroundJobManager(
            scope = backgroundScope,
            getState = { MainUiState(connected = true) },
            onAutoRefresh = { autoRefreshCount += 1 },
            onHomeChartRefresh = {},
            awaitForeground = { foreground.first { it } },
        )

        manager.startAutoRefresh()
        advanceTimeBy(60_000L)
        runCurrent()

        assertEquals(0, autoRefreshCount)

        foreground.value = true
        runCurrent()
        advanceTimeBy(5_001L)
        runCurrent()

        assertEquals(1, autoRefreshCount)
    }

    @Test
    fun startHomeChartRefresh_resumesAfterForegroundRestored() = runTest {
        val foreground = MutableStateFlow(false)
        var homeChartCount = 0
        val manager = BackgroundJobManager(
            scope = backgroundScope,
            getState = { MainUiState(refreshScene = RefreshScene.DASHBOARD) },
            onAutoRefresh = {},
            onHomeChartRefresh = { homeChartCount += 1 },
            awaitForeground = { foreground.first { it } },
        )

        manager.startHomeChartRefresh()
        advanceTimeBy(30_000L)
        runCurrent()

        assertEquals(0, homeChartCount)

        foreground.value = true
        runCurrent()
        advanceTimeBy(3_001L)
        runCurrent()

        assertEquals(1, homeChartCount)
    }

    @Test
    fun startHourlyBoundaryRefresh_invokesOnlyHourlyCallback() = runTest {
        var autoRefreshCount = 0
        var hourlyCount = 0
        val manager = BackgroundJobManager(
            scope = backgroundScope,
            getState = { MainUiState(connected = true) },
            onAutoRefresh = { autoRefreshCount += 1 },
            onHomeChartRefresh = {},
            onHourlyBoundaryRefresh = { hourlyCount += 1 },
        )

        manager.startHourlyBoundaryRefresh()
        advanceTimeBy(3_600_001L)
        runCurrent()

        assertTrue(hourlyCount >= 1)
        assertEquals(0, autoRefreshCount)
    }

    @Test
    fun startAutoRefresh_doesNotInvokeHourlyBoundaryCallback() = runTest {
        var autoRefreshCount = 0
        var hourlyCount = 0
        val manager = BackgroundJobManager(
            scope = backgroundScope,
            getState = { MainUiState(connected = true) },
            onAutoRefresh = { autoRefreshCount += 1 },
            onHomeChartRefresh = {},
            onHourlyBoundaryRefresh = { hourlyCount += 1 },
        )

        manager.startAutoRefresh()
        advanceTimeBy(10_001L)
        runCurrent()

        assertEquals(2, autoRefreshCount)
        assertEquals(0, hourlyCount)
    }
}
