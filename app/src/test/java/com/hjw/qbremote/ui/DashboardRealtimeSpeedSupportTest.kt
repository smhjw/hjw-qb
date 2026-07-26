package com.hjw.qbremote.ui

import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class DashboardRealtimeSpeedSupportTest {

    @Test
    fun restoreHomeRealtimeSpeedSeries_usesPersistedPointsInTimestampOrder() {
        val restored = invokeRestoreHomeRealtimeSpeedSeries(
            snapshot = HomeAggregateSpeedHistorySnapshot(
                points = listOf(
                    HomeSpeedHistoryPoint(timestamp = 1_000L, uploadSpeed = 10L, downloadSpeed = 20L, onlineServerCount = 1),
                    HomeSpeedHistoryPoint(timestamp = 2_000L, uploadSpeed = 20L, downloadSpeed = 40L, onlineServerCount = 2),
                    HomeSpeedHistoryPoint(timestamp = 3_000L, uploadSpeed = 30L, downloadSpeed = 60L, onlineServerCount = 3),
                ),
            ),
            maxPoints = 10,
        )

        assertEquals(listOf(1_000L, 2_000L, 3_000L), restored.map { it.timestamp })
        assertEquals(listOf(10L, 20L, 30L), restored.map { it.uploadSpeed })
        assertEquals(listOf(20L, 40L, 60L), restored.map { it.downloadSpeed })
        assertEquals(listOf(1, 2, 3), restored.map { it.onlineServerCount })
    }

    @Test
    fun resolveRealtimeChartPoints_keepsChartVisibleForIdleTransferState() {
        val resolved = invokeResolveRealtimeChartPoints(
            series = emptyList(),
            transferInfo = TransferInfo(uploadSpeed = 0L, downloadSpeed = 0L),
        )

        assertTrue("Idle dashboard should still provide drawable chart points.", resolved.size >= 2)
        assertTrue("Idle fallback points should stay at zero speed.", resolved.all { it.uploadSpeed == 0L && it.downloadSpeed == 0L })
    }

    @Test
    fun resolveRealtimeChartPoints_singlePointAndSameTransferInfo_keepsStableDuplicatePoint() {
        val resolved = invokeResolveRealtimeChartPoints(
            series = listOf(
                RealtimeSpeedPoint(timestamp = 10L, uploadSpeed = 120L, downloadSpeed = 340L, onlineServerCount = 2),
            ),
            transferInfo = TransferInfo(uploadSpeed = 120L, downloadSpeed = 340L),
        )

        assertTrue("Single-point series should be expanded to drawable 2+ points.", resolved.size >= 2)
        assertTrue("Expanded points should keep the same upload speed.", resolved.all { it.uploadSpeed == 120L })
        assertTrue("Expanded points should keep the same download speed.", resolved.all { it.downloadSpeed == 340L })
    }

    @Test
    fun resolveRealtimeChartPoints_singlePointAndDifferentTransferInfo_appendsCurrentTransferPoint() {
        val resolved = invokeResolveRealtimeChartPoints(
            series = listOf(
                RealtimeSpeedPoint(timestamp = 20L, uploadSpeed = 80L, downloadSpeed = 160L, onlineServerCount = 1),
            ),
            transferInfo = TransferInfo(uploadSpeed = 240L, downloadSpeed = 360L),
        )

        assertTrue("Single-point series should still produce drawable 2+ points.", resolved.size >= 2)
        val appended = resolved.last()
        assertEquals(240L, appended.uploadSpeed)
        assertEquals(360L, appended.downloadSpeed)
    }

    @Test
    fun resolveRealtimeChartPoints_multiPointAndSameTransferInfo_doesNotAppendRedundantPoint() {
        val sourceSeries = listOf(
            RealtimeSpeedPoint(timestamp = 30L, uploadSpeed = 100L, downloadSpeed = 200L, onlineServerCount = 1),
            RealtimeSpeedPoint(timestamp = 31L, uploadSpeed = 300L, downloadSpeed = 500L, onlineServerCount = 1),
        )
        val resolved = invokeResolveRealtimeChartPoints(
            series = sourceSeries,
            transferInfo = TransferInfo(uploadSpeed = 300L, downloadSpeed = 500L),
        )

        assertEquals("Existing drawable series should not get redundant tail point.", sourceSeries.size, resolved.size)
        assertEquals(sourceSeries.map { it.uploadSpeed to it.downloadSpeed }, resolved.map { it.uploadSpeed to it.downloadSpeed })
    }

    @Test
    fun resolveRealtimeChartPoints_multiPointAndDifferentTransferInfo_appendsCurrentTransferPoint() {
        val sourceSeries = listOf(
            RealtimeSpeedPoint(timestamp = 40L, uploadSpeed = 90L, downloadSpeed = 180L, onlineServerCount = 1),
            RealtimeSpeedPoint(timestamp = 41L, uploadSpeed = 110L, downloadSpeed = 210L, onlineServerCount = 1),
        )
        val resolved = invokeResolveRealtimeChartPoints(
            series = sourceSeries,
            transferInfo = TransferInfo(uploadSpeed = 220L, downloadSpeed = 330L),
        )

        assertEquals(sourceSeries.size + 1, resolved.size)
        val appended = resolved.last()
        assertEquals(220L, appended.uploadSpeed)
        assertEquals(330L, appended.downloadSpeed)
    }

    @Test
    fun buildRealtimeAxisValues_usesReadableTicksForNearZeroRates() {
        val axisForOne = invokeBuildRealtimeAxisValues(listOf(0L, 1L))
        val axisForTwo = invokeBuildRealtimeAxisValues(listOf(0L, 2L))

        assertEquals(listOf(3L, 2L, 1L, 0L), axisForOne)
        assertEquals(listOf(3L, 2L, 1L, 0L), axisForTwo)
    }

    @Test
    fun buildRealtimeAxisInputValues_flattensSeriesAndClampsNegativeRates() {
        val values = invokeBuildRealtimeAxisInputValues(
            points = listOf(
                RealtimeSpeedPoint(timestamp = 1L, uploadSpeed = -5L, downloadSpeed = 12L, onlineServerCount = 1),
                RealtimeSpeedPoint(timestamp = 2L, uploadSpeed = 30L, downloadSpeed = -9L, onlineServerCount = 1),
            ),
            transferInfo = TransferInfo(uploadSpeed = -7L, downloadSpeed = 50L),
        )

        assertEquals(listOf(0L, 50L, 0L, 12L, 30L, 0L), values)
    }

    @Test
    fun restoreHomeRealtimeSpeedSeries_trimsToNewestAllowedPoints() {
        val restored = invokeRestoreHomeRealtimeSpeedSeries(
            snapshot = HomeAggregateSpeedHistorySnapshot(
                points = (1L..80L).map { timestamp ->
                    HomeSpeedHistoryPoint(
                        timestamp = timestamp,
                        uploadSpeed = timestamp * 10L,
                        downloadSpeed = timestamp * 20L,
                        onlineServerCount = 1,
                    )
                },
            ),
            maxPoints = 60,
        )

        assertEquals(60, restored.size)
        assertEquals((21L..80L).toList(), restored.map { it.timestamp })
        assertTrue(restored.all { it.onlineServerCount == 1 })
    }

    private fun invokeRestoreHomeRealtimeSpeedSeries(
        snapshot: HomeAggregateSpeedHistorySnapshot,
        maxPoints: Int,
    ): List<RealtimeSpeedPoint> {
        return invokeStaticRealtimePointsFunction(
            containerClassName = "com.hjw.qbremote.ui.MainViewModelStateSupportKt",
            functionName = "restoreHomeRealtimeSpeedSeries",
            parameterTypes = arrayOf(
                HomeAggregateSpeedHistorySnapshot::class.java,
                Int::class.javaPrimitiveType ?: Int::class.java,
            ),
            args = arrayOf(snapshot, maxPoints),
        )
    }

    private fun invokeResolveRealtimeChartPoints(
        series: List<RealtimeSpeedPoint>,
        transferInfo: TransferInfo,
    ): List<RealtimeSpeedPoint> {
        return invokeStaticRealtimePointsFunction(
            containerClassName = "com.hjw.qbremote.ui.DashboardComponentsKt",
            functionName = "resolveRealtimeChartPoints",
            parameterTypes = arrayOf(
                List::class.java,
                TransferInfo::class.java,
            ),
            args = arrayOf(series, transferInfo),
        )
    }

    private fun invokeBuildRealtimeAxisValues(
        values: List<Long>,
    ): List<Long> {
        val method = findRequiredStaticMethod(
            containerClassName = "com.hjw.qbremote.ui.DashboardComponentsKt",
            functionName = "buildRealtimeAxisValues",
            parameterTypes = arrayOf(List::class.java),
        )
        val result = method.invoke(null, values)
            ?: error("`buildRealtimeAxisValues` returned null.")
        @Suppress("UNCHECKED_CAST")
        return result as? List<Long>
            ?: error("`buildRealtimeAxisValues` should return List<Long>.")
    }

    private fun invokeBuildRealtimeAxisInputValues(
        points: List<RealtimeSpeedPoint>,
        transferInfo: TransferInfo,
    ): List<Long> {
        val method = findRequiredStaticMethod(
            containerClassName = "com.hjw.qbremote.ui.DashboardComponentsKt",
            functionName = "buildRealtimeAxisInputValues",
            parameterTypes = arrayOf(List::class.java, TransferInfo::class.java),
        )
        val result = method.invoke(null, points, transferInfo)
            ?: error("`buildRealtimeAxisInputValues` returned null.")
        @Suppress("UNCHECKED_CAST")
        return result as? List<Long>
            ?: error("`buildRealtimeAxisInputValues` should return List<Long>.")
    }

    private fun invokeStaticRealtimePointsFunction(
        containerClassName: String,
        functionName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any>,
    ): List<RealtimeSpeedPoint> {
        val method = findRequiredStaticMethod(
            containerClassName = containerClassName,
            functionName = functionName,
            parameterTypes = parameterTypes,
        )
        val result = method.invoke(null, *args)
            ?: error("`$functionName` returned null.")
        @Suppress("UNCHECKED_CAST")
        return result as? List<RealtimeSpeedPoint>
            ?: error("`$functionName` should return List<RealtimeSpeedPoint>.")
    }

    private fun findRequiredStaticMethod(
        containerClassName: String,
        functionName: String,
        parameterTypes: Array<Class<*>>,
    ): Method {
        val container = Class.forName(containerClassName)
        return runCatching {
            container.getDeclaredMethod(functionName, *parameterTypes)
        }.getOrElse {
            val signature = parameterTypes.joinToString(", ") { it.simpleName }
            error("Expected `${container.simpleName}.$functionName($signature)` as planned top-level helper.")
        }.also { it.isAccessible = true }
    }
}
