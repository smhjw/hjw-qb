package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenPlaceholderSupportTest {

    @Test
    fun showsRestorePlaceholderAndDashboardSkeletonBeforeStartupRestoreCompletes() {
        val flags = resolve(state = MainUiState())

        assertFalse(flags.hasSavedConnection)
        assertTrue(flags.showRestorePlaceholder)
        assertFalse(flags.showTorrentListContent)
        assertFalse(flags.showDashboardSnapshot)
        assertTrue(flags.showDashboardSkeleton)
    }

    @Test
    fun savedProfileWithoutHydrationKeepsRestorePlaceholderAfterStartup() {
        val base = MainUiState(
            serverProfiles = listOf(profile("alpha")),
            startupRestoreComplete = true,
            connected = false,
        )

        val notHydrated = resolve(state = base.copy(dashboardCacheHydrated = false))
        val hydrated = resolve(state = base.copy(dashboardCacheHydrated = true))

        assertTrue(notHydrated.hasSavedConnection)
        assertTrue(notHydrated.showRestorePlaceholder)
        assertFalse(hydrated.showRestorePlaceholder)
    }

    @Test
    fun hostWithUsernameCountsAsSavedConnectionWithoutProfiles() {
        val withHost = resolve(
            state = MainUiState(
                settings = ConnectionSettings(host = "nas.local", username = "admin"),
                startupRestoreComplete = true,
            ),
        )
        val withoutHost = resolve(state = MainUiState(startupRestoreComplete = true))

        assertTrue(withHost.hasSavedConnection)
        assertFalse(withoutHost.hasSavedConnection)
        assertFalse(withoutHost.showRestorePlaceholder)
    }

    @Test
    fun torrentListContentRequiresConnectionOrSnapshot() {
        assertTrue(resolve(state = MainUiState(connected = true)).showTorrentListContent)
        assertTrue(resolve(state = MainUiState(hasDashboardSnapshot = true)).showTorrentListContent)
        assertFalse(resolve(state = MainUiState()).showTorrentListContent)
    }

    @Test
    fun torrentDetailRestorePlaceholderRequiresIdentityMissingTorrentAndRestore() {
        val restoring = MainUiState()

        assertTrue(
            resolve(
                state = restoring,
                selectedTorrentPresent = false,
                selectedTorrentIdentity = "hash-1",
            ).showTorrentDetailRestorePlaceholder,
        )
        assertFalse(
            resolve(
                state = restoring,
                selectedTorrentPresent = false,
                selectedTorrentIdentity = "",
            ).showTorrentDetailRestorePlaceholder,
        )
        assertFalse(
            resolve(
                state = restoring,
                selectedTorrentPresent = true,
                selectedTorrentIdentity = "hash-1",
            ).showTorrentDetailRestorePlaceholder,
        )
        assertFalse(
            resolve(
                state = MainUiState(startupRestoreComplete = true),
                selectedTorrentPresent = false,
                selectedTorrentIdentity = "hash-1",
            ).showTorrentDetailRestorePlaceholder,
        )
    }

    @Test
    fun serverDashboardSkeletonShownOnlyWhileConnectingWithoutSnapshot() {
        val connecting = MainUiState(startupRestoreComplete = true, isConnecting = true)

        assertTrue(
            resolve(
                state = connecting,
                selectedServerProfilePresent = true,
                selectedDashboardSnapshotPresent = false,
            ).showServerDashboardSkeleton,
        )
        assertFalse(
            resolve(
                state = connecting,
                selectedServerProfilePresent = true,
                selectedDashboardSnapshotPresent = true,
            ).showServerDashboardSkeleton,
        )
        assertFalse(
            resolve(
                state = connecting,
                selectedServerProfilePresent = false,
                selectedDashboardSnapshotPresent = false,
            ).showServerDashboardSkeleton,
        )
        assertFalse(
            resolve(
                state = MainUiState(startupRestoreComplete = true, isConnecting = false),
                selectedServerProfilePresent = true,
                selectedDashboardSnapshotPresent = false,
            ).showServerDashboardSkeleton,
        )
    }

    @Test
    fun homeAggregateDashboardSnapshotFollowsProfilePresence() {
        val withProfiles = resolve(
            state = MainUiState(serverProfiles = listOf(profile("alpha"))),
            showHomeAggregateDashboard = true,
        )
        val withoutProfiles = resolve(
            state = MainUiState(),
            showHomeAggregateDashboard = true,
        )

        assertTrue(withProfiles.showDashboardSnapshot)
        assertFalse(withProfiles.showDashboardSkeleton)
        assertFalse(withoutProfiles.showDashboardSnapshot)
        assertTrue(withoutProfiles.showDashboardSkeleton)
    }

    @Test
    fun serverDashboardSnapshotShownWhenConnectedOrCached() {
        assertTrue(resolve(state = MainUiState(connected = true)).showDashboardSnapshot)
        assertTrue(resolve(state = MainUiState(hasDashboardSnapshot = true)).showDashboardSnapshot)
        assertTrue(
            resolve(
                state = MainUiState(
                    dashboardServerSnapshots = listOf(CachedDashboardServerSnapshot(profileId = "alpha")),
                ),
            ).showDashboardSnapshot,
        )
        assertFalse(resolve(state = MainUiState()).showDashboardSnapshot)
    }

    private fun resolve(
        state: MainUiState,
        selectedTorrentPresent: Boolean = false,
        selectedTorrentIdentity: String = "",
        selectedServerProfilePresent: Boolean = false,
        selectedDashboardSnapshotPresent: Boolean = false,
        showHomeAggregateDashboard: Boolean = false,
    ): MainScreenPlaceholderFlags {
        return resolveMainScreenPlaceholderFlags(
            state = state,
            selectedTorrentPresent = selectedTorrentPresent,
            selectedTorrentIdentity = selectedTorrentIdentity,
            selectedServerProfilePresent = selectedServerProfilePresent,
            selectedDashboardSnapshotPresent = selectedDashboardSnapshotPresent,
            showHomeAggregateDashboard = showHomeAggregateDashboard,
        )
    }

    private fun profile(id: String): ServerProfile {
        return ServerProfile(
            id = id,
            name = id,
            backendType = ServerBackendType.QBITTORRENT,
            host = "$id.local",
            port = 8080,
            useHttps = false,
            username = "admin",
            refreshSeconds = 5,
        )
    }
}
