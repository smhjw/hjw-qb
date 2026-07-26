package com.hjw.qbremote.ui

internal data class MainScreenPlaceholderFlags(
    val hasSavedConnection: Boolean,
    val showRestorePlaceholder: Boolean,
    val showTorrentListContent: Boolean,
    val showTorrentDetailRestorePlaceholder: Boolean,
    val showServerDashboardSkeleton: Boolean,
    val showDashboardSnapshot: Boolean,
    val showDashboardSkeleton: Boolean,
)

internal fun resolveMainScreenPlaceholderFlags(
    state: MainUiState,
    selectedTorrentPresent: Boolean,
    selectedTorrentIdentity: String,
    selectedServerProfilePresent: Boolean,
    selectedDashboardSnapshotPresent: Boolean,
    showHomeAggregateDashboard: Boolean,
): MainScreenPlaceholderFlags {
    val hasSavedConnection = state.serverProfiles.isNotEmpty() ||
        (state.settings.host.trim().isNotBlank() && state.settings.username.trim().isNotBlank())
    val showRestorePlaceholder = !state.startupRestoreComplete ||
        (hasSavedConnection && !state.dashboardCacheHydrated && !state.connected)
    val showTorrentListContent = state.connected || state.hasDashboardSnapshot
    val showTorrentDetailRestorePlaceholder = !selectedTorrentPresent &&
        selectedTorrentIdentity.isNotBlank() &&
        showRestorePlaceholder
    val showServerDashboardSkeleton = selectedServerProfilePresent &&
        !selectedDashboardSnapshotPresent &&
        (state.isConnecting || showRestorePlaceholder)
    val showDashboardSnapshot = if (showHomeAggregateDashboard) {
        state.serverProfiles.isNotEmpty()
    } else {
        state.connected || state.hasDashboardSnapshot || state.dashboardServerSnapshots.isNotEmpty()
    }
    val showDashboardSkeleton = !showDashboardSnapshot && showRestorePlaceholder
    return MainScreenPlaceholderFlags(
        hasSavedConnection = hasSavedConnection,
        showRestorePlaceholder = showRestorePlaceholder,
        showTorrentListContent = showTorrentListContent,
        showTorrentDetailRestorePlaceholder = showTorrentDetailRestorePlaceholder,
        showServerDashboardSkeleton = showServerDashboardSkeleton,
        showDashboardSnapshot = showDashboardSnapshot,
        showDashboardSkeleton = showDashboardSkeleton,
    )
}
