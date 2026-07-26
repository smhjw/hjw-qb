package com.hjw.qbremote.ui

import com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.TorrentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyCountryTrackingSupportTest {

    @Test
    fun resolveTrackedCountryHashes_marksUploadingTorrentAsSamplingCandidate() {
        val resolution = resolveTrackedCountryHashes(
            torrents = listOf(
                TorrentInfo(
                    hash = "hash-1",
                    name = "Ubuntu ISO",
                    uploaded = 2_048L,
                    uploadSpeed = 512L,
                ),
            ),
            lastSeenByTorrent = emptyMap(),
            activeHashes = emptyMap(),
            now = 10_000L,
            ttlMs = 1_500L,
        )

        assertEquals(listOf("hash-1"), resolution.candidateHashes)
        assertEquals(2_048L, resolution.lastSeenByTorrent.getValue("hash-1"))
        assertTrue(resolution.activeHashes.getValue("hash-1") > 10_000L)
    }

    @Test
    fun advanceDailyCountryUploadTrackingSnapshot_resetsAccumulationOnNewDay() {
        val today = LocalDate.of(2026, 4, 2)
        val previous = DailyCountryUploadTrackingSnapshot(
            date = "2026-04-01",
            totalsByCountry = mapOf("US" to 4_096L),
            peerSnapshots = mapOf(
                "peer-1" to CountryPeerSnapshot(key = "peer-1", countryCode = "US", uploadedBytes = 1_000L),
            ),
            lastSeenByTorrent = mapOf("hash-1" to 100L),
        )

        val (snapshot, stats) = advanceDailyCountryUploadTrackingSnapshot(
            previous = previous,
            today = today,
            torrents = listOf(TorrentInfo(hash = "hash-1", name = "Ubuntu", uploaded = 200L)),
            samples = listOf(
                CountryPeerSnapshot(
                    key = "peer-1",
                    countryCode = "US",
                    countryName = "United States",
                    uploadedBytes = 1_500L,
                ),
            ),
        )

        assertEquals("2026-04-02", snapshot.date)
        assertTrue(snapshot.totalsByCountry.isEmpty())
        assertEquals(1_500L, snapshot.peerSnapshots.getValue("peer-1").uploadedBytes)
        assertEquals(mapOf("hash-1" to 200L), snapshot.lastSeenByTorrent)
        assertEquals("2026-04-02", stats.dateLabel)
        assertTrue(stats.countries.isEmpty())
    }

    @Test
    fun advanceDailyCountryUploadTrackingSnapshot_accumulatesPerPeerDeltasWithinSameDay() {
        val today = LocalDate.of(2026, 4, 2)
        val previous = DailyCountryUploadTrackingSnapshot(
            date = today.toString(),
            totalsByCountry = mapOf("US" to 500L, "JP" to 100L),
            peerSnapshots = mapOf(
                "us-peer" to CountryPeerSnapshot(key = "us-peer", countryCode = "US", uploadedBytes = 1_000L),
                "jp-peer" to CountryPeerSnapshot(key = "jp-peer", countryCode = "JP", uploadedBytes = 1_000L),
            ),
        )

        val (snapshot, stats) = advanceDailyCountryUploadTrackingSnapshot(
            previous = previous,
            today = today,
            torrents = emptyList(),
            samples = listOf(
                CountryPeerSnapshot(
                    key = "us-peer",
                    countryCode = "US",
                    countryName = "United States",
                    uploadedBytes = 1_600L,
                ),
                CountryPeerSnapshot(
                    key = "jp-peer",
                    countryCode = "JP",
                    countryName = "Japan",
                    uploadedBytes = 300L,
                ),
            ),
        )

        assertEquals(1_100L, snapshot.totalsByCountry.getValue("US"))
        assertEquals(400L, snapshot.totalsByCountry.getValue("JP"))
        assertEquals(listOf("US", "JP"), stats.countries.map { it.countryCode })
        assertEquals(1_100L, stats.countries.first().uploadedBytes)
        assertEquals("United States", stats.countries.first().countryName)
    }

    @Test
    fun advanceDailyCountryUploadTrackingSnapshot_prunesDepartedPeersAndTorrents() {
        val today = LocalDate.of(2026, 4, 2)
        val previous = DailyCountryUploadTrackingSnapshot(
            date = today.toString(),
            totalsByCountry = mapOf("DE" to 700L),
            peerSnapshots = mapOf(
                "stale-peer" to CountryPeerSnapshot(key = "stale-peer", countryCode = "DE", uploadedBytes = 2_000L),
                "live-peer" to CountryPeerSnapshot(key = "live-peer", countryCode = "DE", uploadedBytes = 100L),
            ),
            lastSeenByTorrent = mapOf("gone-torrent" to 10L, "kept-torrent" to 20L),
        )

        val (snapshot, stats) = advanceDailyCountryUploadTrackingSnapshot(
            previous = previous,
            today = today,
            torrents = listOf(TorrentInfo(hash = "kept-torrent", name = "Kept", uploaded = 30L)),
            samples = listOf(
                CountryPeerSnapshot(
                    key = "live-peer",
                    countryCode = "DE",
                    countryName = "Germany",
                    uploadedBytes = 150L,
                ),
            ),
        )

        assertEquals(setOf("live-peer"), snapshot.peerSnapshots.keys)
        assertEquals(150L, snapshot.peerSnapshots.getValue("live-peer").uploadedBytes)
        assertEquals(mapOf("kept-torrent" to 30L), snapshot.lastSeenByTorrent)
        assertEquals(750L, snapshot.totalsByCountry.getValue("DE"))
        assertEquals(750L, stats.countries.single().uploadedBytes)
    }
}
