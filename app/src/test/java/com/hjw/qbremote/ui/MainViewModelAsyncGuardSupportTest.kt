package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.TransferInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelAsyncGuardSupportTest {

    @Test
    fun shouldApplyActiveProfileAsyncResult_acceptsMatchingProfileAndVersion() {
        assertTrue(
            shouldApplyActiveProfileAsyncResult(
                requestedProfileId = "alpha",
                requestVersion = 7L,
                activeProfileId = "alpha",
                activeRequestVersion = 7L,
            ),
        )
    }

    @Test
    fun shouldApplyActiveProfileAsyncResult_rejectsStaleProfileSwitch() {
        assertFalse(
            shouldApplyActiveProfileAsyncResult(
                requestedProfileId = "alpha",
                requestVersion = 7L,
                activeProfileId = "beta",
                activeRequestVersion = 7L,
            ),
        )
    }

    @Test
    fun shouldApplyActiveProfileAsyncResult_rejectsStaleRequestVersion() {
        assertFalse(
            shouldApplyActiveProfileAsyncResult(
                requestedProfileId = "alpha",
                requestVersion = 7L,
                activeProfileId = "alpha",
                activeRequestVersion = 8L,
            ),
        )
    }

    @Test
    fun buildDailyUploadTrackingScopeKey_prefersExplicitProfileScope() {
        val scopeKey = buildDailyUploadTrackingScopeKey(
            activeProfileId = " profile-1 ",
            settings = ConnectionSettings(
                host = "seedbox.example.com",
                port = 8080,
                useHttps = true,
            ),
        )

        assertEquals("profile:profile-1", scopeKey)
    }

    @Test
    fun buildDailyUploadTrackingScopeKey_fallsBackToServerCoordinatesWithoutProfile() {
        val scopeKey = buildDailyUploadTrackingScopeKey(
            activeProfileId = " ",
            settings = ConnectionSettings(
                host = " SeedBox.Example.com ",
                port = 9091,
                useHttps = false,
            ),
        )

        assertEquals("server:false|seedbox.example.com|9091", scopeKey)
    }

    @Test
    fun normalizeProfileIdsForRefresh_returnsSortedDistinctTrimmedIds() {
        val normalized = normalizeProfileIdsForRefresh(
            listOf(
                ServerProfile(" beta ", "Beta", ServerBackendType.QBITTORRENT, "beta", 8080, false, "admin", 5),
                ServerProfile("alpha", "Alpha", ServerBackendType.QBITTORRENT, "alpha", 8080, false, "admin", 5),
                ServerProfile("beta", "Beta 2", ServerBackendType.QBITTORRENT, "beta2", 8080, false, "admin", 5),
                ServerProfile(" ", "Blank", ServerBackendType.QBITTORRENT, "blank", 8080, false, "admin", 5),
            ),
        )

        assertEquals(listOf("alpha", "beta"), normalized)
    }

    @Test
    fun resolvePreferredProfileId_prefersPrimaryThenSecondaryThenFirstAvailable() {
        assertEquals(
            "beta",
            resolvePreferredProfileId(
                availableIds = listOf("alpha", "beta", "gamma"),
                primaryCandidate = "beta",
                secondaryCandidate = "gamma",
            ),
        )
        assertEquals(
            "gamma",
            resolvePreferredProfileId(
                availableIds = listOf("alpha", "beta", "gamma"),
                primaryCandidate = "missing",
                secondaryCandidate = "gamma",
            ),
        )
        assertEquals(
            "alpha",
            resolvePreferredProfileId(
                availableIds = listOf("alpha", "beta", "gamma"),
                primaryCandidate = "missing",
                secondaryCandidate = "also-missing",
            ),
        )
    }

    @Test
    fun filterDashboardPreferencesForProfiles_keepsOnlyExistingProfileEntries() {
        val filtered = filterDashboardPreferencesForProfiles(
            preferences = mapOf(
                "alpha" to ServerDashboardPreferences(cardOrder = "country_flow"),
                "beta" to ServerDashboardPreferences(cardOrder = "daily_upload"),
                "orphan" to ServerDashboardPreferences(cardOrder = "tag_upload"),
            ),
            profiles = listOf(
                ServerProfile("beta", "Beta", ServerBackendType.QBITTORRENT, "beta", 8080, false, "admin", 5),
                ServerProfile("alpha", "Alpha", ServerBackendType.QBITTORRENT, "alpha", 8080, false, "admin", 5),
            ),
        )

        assertEquals(setOf("alpha", "beta"), filtered.keys)
        assertEquals("country_flow", filtered["alpha"]?.cardOrder)
        assertEquals("daily_upload", filtered["beta"]?.cardOrder)
    }

    @Test
    fun resolveSelectedDashboardProfileId_prefersActiveThenSelectedThenFirstSnapshot() {
        val snapshots = listOf(
            CachedDashboardServerSnapshot(profileId = "alpha"),
            CachedDashboardServerSnapshot(profileId = "beta"),
        )

        assertEquals(
            "beta",
            resolveSelectedDashboardProfileId(
                activeProfileId = "beta",
                selectedDashboardProfileId = "alpha",
                snapshots = snapshots,
            ),
        )
        assertEquals(
            "alpha",
            resolveSelectedDashboardProfileId(
                activeProfileId = "missing",
                selectedDashboardProfileId = "alpha",
                snapshots = snapshots,
            ),
        )
        assertEquals(
            "alpha",
            resolveSelectedDashboardProfileId(
                activeProfileId = "missing",
                selectedDashboardProfileId = "also-missing",
                snapshots = snapshots,
            ),
        )
    }

    @Test
    fun applyDashboardSnapshotsToState_preservesChartTransferInfoAndCountsOnlineSnapshots() {
        val current = MainUiState(
            activeServerProfileId = "beta",
            selectedDashboardProfileId = "alpha",
            dashboardAggregate = DashboardAggregateState(
                chartTransferInfo = TransferInfo(uploadSpeed = 11L, downloadSpeed = 22L),
            ),
        )
        val orderedSnapshots = listOf(
            CachedDashboardServerSnapshot(profileId = "alpha", isStale = true),
            CachedDashboardServerSnapshot(profileId = "beta", isStale = false),
        )
        val aggregate = DashboardAggregateState(
            transferInfo = TransferInfo(uploadSpeed = 1L, downloadSpeed = 2L),
            chartTransferInfo = TransferInfo(uploadSpeed = 99L, downloadSpeed = 88L),
        )

        val updated = applyDashboardSnapshotsToState(
            current = current,
            orderedSnapshots = orderedSnapshots,
            aggregate = aggregate,
        )

        assertEquals(orderedSnapshots, updated.dashboardServerSnapshots)
        assertEquals("beta", updated.selectedDashboardProfileId)
        assertEquals(1, updated.aggregateOnlineServerCount)
        assertEquals(11L, updated.dashboardAggregate.chartTransferInfo?.uploadSpeed)
        assertEquals(22L, updated.dashboardAggregate.chartTransferInfo?.downloadSpeed)
        assertEquals(1L, updated.dashboardAggregate.transferInfo.uploadSpeed)
        assertEquals(2L, updated.dashboardAggregate.transferInfo.downloadSpeed)
    }

    @Test
    fun prepareConnectingProfileState_resetsTransientServerFieldsAndBumpsSession() {
        val current = MainUiState(
            settings = ConnectionSettings(host = "old-host", port = 8080),
            activeServerProfileId = "old",
            selectedDashboardProfileId = "old",
            dashboardSessionToken = 5L,
            aggregateOnlineServerCount = 3,
            isConnecting = false,
            connected = true,
            serverVersion = "v1",
            transferInfo = TransferInfo(uploadSpeed = 10L, downloadSpeed = 20L),
            torrents = listOf(com.hjw.qbremote.data.model.TorrentInfo(hash = "abc", name = "Torrent")),
            detailHash = "abc",
            detailLoading = true,
            categoryOptions = listOf("movies"),
            tagOptions = listOf("tag-1"),
            pendingActionKeys = setOf("old|abc"),
            pendingBackendRepair = PendingBackendRepair(
                profileId = "old",
                profileName = "Old",
                expectedBackend = ServerBackendType.QBITTORRENT,
                detectedBackend = ServerBackendType.TRANSMISSION,
            ),
            errorMessage = UiMessage.Text("boom"),
        )
        val nextSettings = ConnectionSettings(
            host = "next-host",
            port = 9091,
            useHttps = true,
            serverBackendType = ServerBackendType.TRANSMISSION,
        )

        val updated = prepareConnectingProfileState(
            current = current,
            settings = nextSettings,
            profileId = "next",
            capabilities = ServerCapabilities(supportsReannounce = true),
        )

        assertEquals(nextSettings, updated.settings)
        assertEquals("next", updated.activeServerProfileId)
        assertEquals("next", updated.selectedDashboardProfileId)
        assertEquals(6L, updated.dashboardSessionToken)
        assertTrue(updated.isConnecting)
        assertFalse(updated.connected)
        assertEquals("-", updated.serverVersion)
        assertEquals(0L, updated.transferInfo.uploadSpeed)
        assertTrue(updated.torrents.isEmpty())
        assertEquals("", updated.detailHash)
        assertFalse(updated.detailLoading)
        assertTrue(updated.categoryOptions.isEmpty())
        assertTrue(updated.tagOptions.isEmpty())
        assertTrue(updated.pendingActionKeys.isEmpty())
        assertEquals(null, updated.pendingBackendRepair)
        assertEquals(null, updated.errorMessage)
        assertTrue(updated.activeCapabilities.supportsReannounce)
        assertEquals(3, updated.aggregateOnlineServerCount)
    }
}
