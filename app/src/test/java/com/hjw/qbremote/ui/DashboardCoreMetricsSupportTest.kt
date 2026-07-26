package com.hjw.qbremote.ui

import com.hjw.qbremote.R
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.model.TorrentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardCoreMetricsSupportTest {
    @Test
    fun firstTrackerFromMagnetUri_decodesFirstOfMultipleTrackers() {
        val magnet = "magnet:?xt=urn:btih:abc&tr=https%3A%2F%2Ftracker.m-team.cc%2Fannounce&tr=udp%3A%2F%2Fbackup.example%3A80"

        assertEquals(
            "https://tracker.m-team.cc/announce",
            firstTrackerFromMagnetUri(magnet),
        )
        assertEquals(
            "https://tracker.m-team.cc/announce",
            firstTrackerFromMagnetUri(java.net.URLEncoder.encode(magnet, "UTF-8")),
        )
    }

    @Test
    fun trackerDistribution_prefersWorkingTrackerThenMagnetAndLabelsTrueAbsence() {
        val display = buildServerDashboardDisplayState(
            snapshot = CachedDashboardServerSnapshot(
                profileId = "qb",
                backendType = ServerBackendType.QBITTORRENT,
                torrents = listOf(
                    TorrentInfo(
                        tracker = "https://announce.working.example/announce",
                        magnetUri = "magnet:?tr=https%3A%2F%2Ftracker.fallback.example%2Fannounce",
                    ),
                    TorrentInfo(
                        magnetUri = "magnet:?xt=urn:btih:def&tr=https%3A%2F%2Ftracker.m-team.cc%2Fannounce",
                    ),
                    TorrentInfo(),
                ),
            ),
            backendType = ServerBackendType.QBITTORRENT,
            preferences = null,
        )

        val labels = display.trackerSiteBarEntries.map { it.label }
        assertTrue(labels.contains(LegendLabelSpec.Raw("working")))
        assertTrue(labels.contains(LegendLabelSpec.Raw("m-team")))
        assertTrue(labels.contains(LegendLabelSpec.Res(R.string.dashboard_tracker_site_none)))
        assertFalse(labels.contains(LegendLabelSpec.Raw("unknown")))
    }

    @Test
    fun shareRatioDistribution_usesExactBucketBoundariesAndMedian() {
        val display = buildShareRatioDistributionDisplay(
            listOf(0.999, 1.0, 1.999, 2.0, 4.999, 5.0).map { ratio ->
                TorrentInfo(ratio = ratio)
            },
        )

        assertEquals(listOf(1, 2, 2, 1), display.bucketCounts)
        assertEquals(1, display.belowOneCount)
        assertEquals(6, display.totalCount)
        assertEquals(1.9995, display.medianRatio, 0.00001)
        assertEquals(ShareRatioDistributionDisplay(), buildShareRatioDistributionDisplay(emptyList()))
    }
}
