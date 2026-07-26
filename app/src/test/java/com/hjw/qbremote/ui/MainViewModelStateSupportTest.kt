package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelStateSupportTest {

    @Test
    fun applyServerSwitchReset_clearsSessionStateAndAppliesNewIdentity() {
        val settings = ConnectionSettings(host = "next.example.com", port = 9091)
        val capabilities = defaultCapabilitiesFor(ServerBackendType.TRANSMISSION)

        val result = applyServerSwitchReset(
            current = dirtyState(),
            settings = settings,
            activeProfileId = "alpha",
            capabilities = capabilities,
        )

        assertEquals(settings, result.settings)
        assertEquals("alpha", result.activeServerProfileId)
        assertEquals(capabilities, result.activeCapabilities)
        assertFalse(result.connected)
        assertEquals("-", result.serverVersion)
        assertEquals(TransferInfo(), result.transferInfo)
        assertTrue(result.torrents.isEmpty())
        assertEquals("", result.dailyTagUploadDate)
        assertTrue(result.dailyTagUploadStats.isEmpty())
        assertEquals("", result.dailyCountryUploadDate)
        assertTrue(result.dailyCountryUploadStats.isEmpty())
        assertEquals("alpha", result.selectedDashboardProfileId)
        assertFalse(result.dashboardCacheHydrated)
        assertFalse(result.hasDashboardSnapshot)
        assertEquals("", result.detailHash)
        assertFalse(result.detailLoading)
        assertNull(result.detailProperties)
        assertTrue(result.detailFiles.isEmpty())
        assertTrue(result.detailTrackers.isEmpty())
        assertTrue(result.pendingActionKeys.isEmpty())
    }

    @Test
    fun applyServerSwitchReset_keepsPreviousDashboardSelectionWhenProfileIdMissing() {
        val result = applyServerSwitchReset(
            current = MainUiState(selectedDashboardProfileId = "beta"),
            settings = ConnectionSettings(),
            activeProfileId = null,
            capabilities = defaultCapabilitiesFor(ServerBackendType.QBITTORRENT),
        )

        assertNull(result.activeServerProfileId)
        assertEquals("beta", result.selectedDashboardProfileId)
    }

    @Test
    fun prepareServerDashboardTransitionState_targetsProfileAndResetsSession() {
        val current = dirtyState().copy(
            dashboardSessionToken = 7L,
            pendingBackendRepair = PendingBackendRepair(
                profileId = "other",
                profileName = "Other",
                expectedBackend = ServerBackendType.QBITTORRENT,
                detectedBackend = ServerBackendType.TRANSMISSION,
            ),
            errorMessage = UiMessage.Text("boom"),
        )

        val result = prepareServerDashboardTransitionState(current, profileId = "alpha")

        assertEquals("alpha", result.selectedDashboardProfileId)
        assertEquals(8L, result.dashboardSessionToken)
        assertTrue(result.isConnecting)
        assertFalse(result.connected)
        assertNull(result.errorMessage)
        assertNull(result.pendingBackendRepair)
        assertEquals("-", result.serverVersion)
        assertEquals(TransferInfo(), result.transferInfo)
        assertTrue(result.torrents.isEmpty())
        assertEquals("", result.dailyTagUploadDate)
        assertTrue(result.dailyTagUploadStats.isEmpty())
        assertEquals("", result.dailyCountryUploadDate)
        assertTrue(result.dailyCountryUploadStats.isEmpty())
        assertTrue(result.categoryOptions.isEmpty())
        assertTrue(result.tagOptions.isEmpty())
        assertFalse(result.dashboardCacheHydrated)
        assertFalse(result.hasDashboardSnapshot)
        assertEquals("", result.detailHash)
        assertFalse(result.detailLoading)
        assertNull(result.detailProperties)
        assertTrue(result.detailFiles.isEmpty())
        assertTrue(result.detailTrackers.isEmpty())
        assertTrue(result.pendingActionKeys.isEmpty())
    }

    @Test
    fun prepareServerDashboardTransitionState_keepsBackendRepairForSameProfile() {
        val repair = PendingBackendRepair(
            profileId = "alpha",
            profileName = "Alpha",
            expectedBackend = ServerBackendType.QBITTORRENT,
            detectedBackend = ServerBackendType.TRANSMISSION,
        )

        val result = prepareServerDashboardTransitionState(
            current = MainUiState(pendingBackendRepair = repair),
            profileId = "alpha",
        )

        assertEquals(repair, result.pendingBackendRepair)
    }

    private fun dirtyState(): MainUiState {
        return MainUiState(
            connected = true,
            serverVersion = "v5.0.1",
            transferInfo = TransferInfo(uploadSpeed = 10L, downloadSpeed = 20L),
            torrents = listOf(TorrentInfo(hash = "hash-1", name = "Old torrent")),
            dailyTagUploadDate = "2026-07-25",
            dailyTagUploadStats = listOf(DailyTagUploadStat(tag = "movies", uploadedBytes = 1_024L, torrentCount = 1)),
            dailyCountryUploadDate = "2026-07-25",
            dailyCountryUploadStats = listOf(CountryUploadRecord(countryCode = "US", uploadedBytes = 2_048L)),
            categoryOptions = listOf("movies"),
            tagOptions = listOf("4k"),
            selectedDashboardProfileId = "beta",
            dashboardCacheHydrated = true,
            hasDashboardSnapshot = true,
            detailHash = "hash-1",
            detailLoading = true,
            detailProperties = TorrentProperties(savePath = "/downloads"),
            detailFiles = listOf(TorrentFileInfo(index = 0, name = "file.iso")),
            detailTrackers = listOf(TorrentTracker(url = "http://tracker.example.com")),
            pendingActionKeys = setOf("beta|hash-1"),
        )
    }
}
