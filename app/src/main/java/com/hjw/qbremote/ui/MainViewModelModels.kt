package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.GlobalSpeedLimits
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.CountryPeerCountRecord
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo

enum class RefreshScene {
    DASHBOARD,
    SERVER,
    TORRENT_DETAIL,
    SETTINGS,
}

data class DailyTagUploadStat(
    val tag: String,
    val uploadedBytes: Long,
    val torrentCount: Int,
    val isNoTag: Boolean = false,
)

data class RealtimeSpeedPoint(
    val timestamp: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val onlineServerCount: Int = 0,
)

data class DashboardAggregateState(
    val transferInfo: TransferInfo = TransferInfo(),
    val chartTransferInfo: TransferInfo? = null,
    val torrents: List<TorrentInfo> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<DailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryPeerCountRecord> = emptyList(),
    val realtimeSpeedSeries: List<RealtimeSpeedPoint> = emptyList(),
    val totalServerCount: Int = 0,
    val categoryCoverageServerCount: Int = 0,
    val countryCoverageServerCount: Int = 0,
)

data class PendingBackendRepair(
    val profileId: String,
    val profileName: String,
    val expectedBackend: ServerBackendType,
    val detectedBackend: ServerBackendType,
    val detail: String = "",
)

internal data class NotificationNavigationTarget(
    val id: Long,
    val profileId: String,
    val torrentHash: String,
)

@androidx.compose.runtime.Immutable
internal data class MainUiState(
    val settings: ConnectionSettings = ConnectionSettings(),
    val serverProfiles: List<ServerProfile> = emptyList(),
    val activeServerProfileId: String? = null,
    val activeCapabilities: ServerCapabilities = defaultCapabilitiesFor(ServerBackendType.QBITTORRENT),
    val aggregateOnlineServerCount: Int = 0,
    val isConnecting: Boolean = false,
    val isSavingServerProfile: Boolean = false,
    val isAddingTorrent: Boolean = false,
    val isManualRefreshing: Boolean = false,
    val connected: Boolean = false,
    val serverVersion: String = "-",
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val detailHash: String = "",
    val detailLoading: Boolean = false,
    val detailProperties: TorrentProperties? = null,
    val detailFiles: List<TorrentFileInfo> = emptyList(),
    val detailTrackers: List<TorrentTracker> = emptyList(),
    val categoryOptions: List<String> = emptyList(),
    val tagOptions: List<String> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<DailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryPeerCountRecord> = emptyList(),
    val dashboardServerSnapshots: List<CachedDashboardServerSnapshot> = emptyList(),
    val serverDashboardPreferences: Map<String, ServerDashboardPreferences> = emptyMap(),
    val selectedDashboardProfileId: String? = null,
    val dashboardSessionToken: Long = 0L,
    val dashboardRefreshHoldProfileId: String? = null,
    val dashboardRefreshHoldAllProfiles: Boolean = false,
    val dashboardAggregate: DashboardAggregateState = DashboardAggregateState(),
    val dashboardCacheHydrated: Boolean = false,
    val hasDashboardSnapshot: Boolean = false,
    val customBackgroundAvailable: Boolean = true,
    val startupRestoreComplete: Boolean = false,
    val refreshScene: RefreshScene = RefreshScene.DASHBOARD,
    val pendingActionKeys: Set<String> = emptySet(),
    val pendingBackendRepair: PendingBackendRepair? = null,
    val globalSpeedLimitDialogVisible: Boolean = false,
    val globalSpeedLimitProfileId: String = "",
    val globalSpeedLimits: GlobalSpeedLimits? = null,
    val globalSpeedLimitLoading: Boolean = false,
    val globalSpeedLimitSaving: Boolean = false,
    val globalSpeedLimitLoadFailed: Boolean = false,
    val sharedTorrentInput: SharedTorrentInput? = null,
    val notificationNavigationTarget: NotificationNavigationTarget? = null,
    val errorMessage: UiMessage? = null,
)

internal sealed interface DashboardSnapshotRefreshResult {
    val profile: ServerProfile
    val previousSnapshot: CachedDashboardServerSnapshot?

    data class Fresh(
        override val profile: ServerProfile,
        val settings: ConnectionSettings,
        val fetched: com.hjw.qbremote.data.DashboardSnapshotFetchResult,
        override val previousSnapshot: CachedDashboardServerSnapshot?,
    ) : DashboardSnapshotRefreshResult

    data class Failure(
        override val profile: ServerProfile,
        val error: Throwable,
        override val previousSnapshot: CachedDashboardServerSnapshot?,
    ) : DashboardSnapshotRefreshResult
}

internal data class DashboardStatsRefreshInput(
    val profile: ServerProfile,
    val settings: ConnectionSettings,
    val torrents: List<TorrentInfo>,
    val baseSnapshot: CachedDashboardServerSnapshot,
)
