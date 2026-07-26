package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HomeDashboardSpeedRefreshSupportTest {

    @Test
    fun resolveHomeSpeedRefreshIntervalSeconds_usesThreeSecondsOnlyOnDashboard() {
        assertEquals(3, resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.DASHBOARD))
        assertNull(resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.SERVER))
        assertNull(resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.TORRENT_DETAIL))
        assertNull(resolveHomeSpeedRefreshIntervalSeconds(RefreshScene.SETTINGS))
    }

    @Test
    fun buildHomeChartTransferInfo_sumsSuccessfulProfileTransferInfoOnly() {
        val aggregate = buildHomeChartTransferInfo(
            transferInfos = listOf(
                TransferInfo(uploadSpeed = 5L, downloadSpeed = 7L),
                TransferInfo(uploadSpeed = 11L, downloadSpeed = 13L),
            ),
        )

        assertEquals(16L, aggregate.uploadSpeed)
        assertEquals(20L, aggregate.downloadSpeed)
    }

    @Test
    fun applyHomeChartRefreshToAggregate_preservesSummaryTransferInfo() {
        val summaryTransferInfo = TransferInfo(uploadSpeed = 100L, downloadSpeed = 200L)
        val chartTransferInfo = TransferInfo(uploadSpeed = 30L, downloadSpeed = 40L)
        val chartSeries = listOf(
            RealtimeSpeedPoint(timestamp = 1L, uploadSpeed = 30L, downloadSpeed = 40L, onlineServerCount = 1),
        )

        val updated = applyHomeChartRefreshToAggregate(
            aggregate = DashboardAggregateState(
                transferInfo = summaryTransferInfo,
                realtimeSpeedSeries = emptyList(),
            ),
            chartTransferInfo = chartTransferInfo,
            chartSeries = chartSeries,
        )

        assertSame(summaryTransferInfo, updated.transferInfo)
        assertSame(chartTransferInfo, updated.chartTransferInfo)
        assertEquals(chartSeries, updated.realtimeSpeedSeries)
    }
}
