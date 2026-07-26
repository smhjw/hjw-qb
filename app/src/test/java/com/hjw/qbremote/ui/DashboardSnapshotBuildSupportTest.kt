package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSnapshotBuildSupportTest {

    @Test
    fun buildActiveDashboardServerSnapshot_combinesProfileIdentityWithLiveState() {
        val profile = profile("alpha", name = "Alpha", host = "qb.local", port = 8080, useHttps = true)
        val state = MainUiState(
            serverVersion = "v5.0.1",
            transferInfo = TransferInfo(uploadSpeed = 11L, downloadSpeed = 22L),
            torrents = listOf(TorrentInfo(hash = "hash-1", name = "Live torrent")),
            dailyTagUploadDate = "2026-07-26",
            dailyTagUploadStats = listOf(DailyTagUploadStat(tag = "movies", uploadedBytes = 64L, torrentCount = 2)),
            dailyCountryUploadDate = "2026-07-26",
            dailyCountryUploadStats = listOf(CountryUploadRecord(countryCode = "US", uploadedBytes = 32L)),
        )

        val snapshot = buildActiveDashboardServerSnapshot(profile, state)

        assertEquals("alpha", snapshot.profileId)
        assertEquals("Alpha", snapshot.profileName)
        assertEquals(ServerBackendType.QBITTORRENT, snapshot.backendType)
        assertEquals("qb.local", snapshot.host)
        assertEquals(8080, snapshot.port)
        assertTrue(snapshot.useHttps)
        assertEquals("v5.0.1", snapshot.serverVersion)
        assertEquals(state.transferInfo, snapshot.transferInfo)
        assertEquals(state.torrents, snapshot.torrents)
        assertEquals("2026-07-26", snapshot.dailyTagUploadDate)
        assertEquals(
            listOf(CachedDailyTagUploadStat(tag = "movies", uploadedBytes = 64L, torrentCount = 2, isNoTag = false)),
            snapshot.dailyTagUploadStats,
        )
        assertEquals("2026-07-26", snapshot.dailyCountryUploadDate)
        assertEquals(state.dailyCountryUploadStats, snapshot.dailyCountryUploadStats)
        assertEquals("", snapshot.errorMessage)
        assertFalse(snapshot.isStale)
        assertTrue(snapshot.lastUpdatedAt > 0L)
    }

    @Test
    fun orderedDashboardServerSnapshots_followsProfileOrderAndFillsMissingWithStalePlaceholders() {
        val beta = profile("beta", name = "Beta")
        val alpha = profile("alpha", name = "Alpha Renamed", host = "new.host", port = 9090, useHttps = true)
        val cached = CachedDashboardServerSnapshot(
            profileId = "alpha",
            profileName = "Alpha Old",
            backendType = ServerBackendType.TRANSMISSION,
            host = "old.host",
            port = 1,
            useHttps = false,
            torrents = listOf(TorrentInfo(hash = "hash-1", name = "Cached torrent")),
            isStale = false,
        )

        val result = orderedDashboardServerSnapshots(
            profiles = listOf(beta, alpha),
            snapshotsById = mapOf("alpha" to cached),
        )

        assertEquals(listOf("beta", "alpha"), result.map { it.profileId })
        val placeholder = result[0]
        assertTrue(placeholder.isStale)
        assertTrue(placeholder.torrents.isEmpty())
        assertEquals("Beta", placeholder.profileName)
        val merged = result[1]
        assertEquals("Alpha Renamed", merged.profileName)
        assertEquals(ServerBackendType.QBITTORRENT, merged.backendType)
        assertEquals("new.host", merged.host)
        assertEquals(9090, merged.port)
        assertTrue(merged.useHttps)
        assertEquals(cached.torrents, merged.torrents)
        assertFalse(merged.isStale)
    }

    @Test
    fun buildStaleDashboardServerSnapshot_preservesPreviousDataWhileMarkingStale() {
        val previous = CachedDashboardServerSnapshot(
            profileId = "alpha",
            profileName = "Old Name",
            backendType = ServerBackendType.QBITTORRENT,
            host = "old.host",
            port = 8080,
            useHttps = false,
            serverVersion = "v5.0.1",
            transferInfo = TransferInfo(uploadSpeed = 9L),
            torrents = listOf(TorrentInfo(hash = "hash-1", name = "Kept torrent")),
            dailyTagUploadDate = "2026-07-25",
            dailyTagUploadStats = listOf(CachedDailyTagUploadStat(tag = "movies", uploadedBytes = 1L, torrentCount = 1)),
            dailyCountryUploadDate = "2026-07-25",
            dailyCountryUploadStats = listOf(CountryUploadRecord(countryCode = "US", uploadedBytes = 2L)),
            lastUpdatedAt = 1_234L,
            errorMessage = "",
            isStale = false,
        )

        val result = buildStaleDashboardServerSnapshot(
            profileId = "alpha",
            profileName = "New Name",
            backendType = ServerBackendType.QBITTORRENT,
            host = "new.host",
            port = 9090,
            useHttps = true,
            previousSnapshot = previous,
            errorMessage = "Connection refused",
        )

        assertTrue(result.isStale)
        assertEquals("Connection refused", result.errorMessage)
        assertEquals("New Name", result.profileName)
        assertEquals("new.host", result.host)
        assertEquals(9090, result.port)
        assertTrue(result.useHttps)
        assertEquals("v5.0.1", result.serverVersion)
        assertEquals(previous.transferInfo, result.transferInfo)
        assertEquals(previous.torrents, result.torrents)
        assertEquals(previous.dailyTagUploadStats, result.dailyTagUploadStats)
        assertEquals(previous.dailyCountryUploadStats, result.dailyCountryUploadStats)
        assertEquals(1_234L, result.lastUpdatedAt)
    }

    @Test
    fun buildStaleDashboardServerSnapshot_buildsEmptyStalePlaceholderWithoutPrevious() {
        val result = buildStaleDashboardServerSnapshot(
            profileId = "alpha",
            profileName = "Alpha",
            backendType = ServerBackendType.TRANSMISSION,
            host = "tr.local",
            port = 9091,
            useHttps = false,
            previousSnapshot = null,
            errorMessage = "timeout",
        )

        assertTrue(result.isStale)
        assertEquals("timeout", result.errorMessage)
        assertEquals("alpha", result.profileId)
        assertEquals(ServerBackendType.TRANSMISSION, result.backendType)
        assertEquals("tr.local", result.host)
        assertEquals(9091, result.port)
        assertTrue(result.torrents.isEmpty())
        assertEquals("-", result.serverVersion)
        assertEquals(0L, result.lastUpdatedAt)
    }

    private fun profile(
        id: String,
        name: String = id,
        host: String = "$id.local",
        port: Int = 8080,
        useHttps: Boolean = false,
    ): ServerProfile {
        return ServerProfile(
            id = id,
            name = name,
            backendType = ServerBackendType.QBITTORRENT,
            host = host,
            port = port,
            useHttps = useHttps,
            username = "admin",
            refreshSeconds = 5,
        )
    }
}
