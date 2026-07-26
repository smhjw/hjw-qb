package com.hjw.qbremote.data

import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.DashboardData
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo

enum class ServerBackendType {
    QBITTORRENT,
    TRANSMISSION,
}

data class ServerCapabilities(
    val supportsCategories: Boolean = false,
    val supportsTags: Boolean = true,
    val supportsCountryDistribution: Boolean = false,
    val supportsExportTorrent: Boolean = false,
    val supportsRename: Boolean = false,
    val supportsPerTorrentSpeedLimit: Boolean = false,
    val supportsShareRatio: Boolean = false,
    val supportsTrackers: Boolean = true,
    val supportsTrackerMutation: Boolean = false,
    val supportsReannounce: Boolean = false,
    val supportsRecheck: Boolean = false,
    val supportsFiles: Boolean = true,
    val supportsMoveLocation: Boolean = true,
    val supportsAddTorrent: Boolean = true,
    val supportsAdvancedAddOptions: Boolean = false,
)

data class CachedDashboardServerSnapshot(
    val profileId: String = "",
    val profileName: String = "",
    val backendType: ServerBackendType = ServerBackendType.QBITTORRENT,
    val host: String = "",
    val port: Int = 0,
    val useHttps: Boolean = false,
    val serverVersion: String = "-",
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val dailyTagUploadDate: String = "",
    val dailyTagUploadStats: List<CachedDailyTagUploadStat> = emptyList(),
    val dailyCountryUploadDate: String = "",
    val dailyCountryUploadStats: List<CountryUploadRecord> = emptyList(),
    val lastUpdatedAt: Long = 0L,
    val errorMessage: String = "",
    val isStale: Boolean = false,
)

data class DashboardSnapshotFetchResult(
    val serverVersion: String = "-",
    val dashboardData: DashboardData = DashboardData(
        transferInfo = TransferInfo(),
        torrents = emptyList(),
    ),
)

interface TorrentBackend {
    val backendType: ServerBackendType
    val capabilities: ServerCapabilities

    suspend fun connect(settings: ConnectionSettings): Result<Unit>
    fun clearSession()
    suspend fun fetchTransferInfo(): Result<TransferInfo>
    suspend fun fetchDashboard(): Result<DashboardData>
    suspend fun fetchDashboardSnapshot(settings: ConnectionSettings): Result<DashboardSnapshotFetchResult>
    suspend fun pauseTorrent(hash: String): Result<Unit>
    suspend fun resumeTorrent(hash: String): Result<Unit>
    suspend fun deleteTorrent(hash: String, deleteFiles: Boolean): Result<Unit>
    suspend fun reannounceTorrent(hash: String): Result<Unit>
    suspend fun recheckTorrent(hash: String): Result<Unit>
    suspend fun fetchServerVersion(): Result<String>
    suspend fun fetchTorrentDetail(hash: String): Result<com.hjw.qbremote.data.model.TorrentDetailData>
    suspend fun fetchTorrentTrackers(hash: String): Result<List<com.hjw.qbremote.data.model.TorrentTracker>>
    suspend fun addTracker(hash: String, trackerUrl: String): Result<Unit>
    suspend fun editTracker(hash: String, tracker: com.hjw.qbremote.data.model.TorrentTracker, newUrl: String): Result<Unit>
    suspend fun removeTracker(hash: String, tracker: com.hjw.qbremote.data.model.TorrentTracker): Result<Unit>
    suspend fun exportTorrentFile(hash: String): Result<ByteArray>
    suspend fun fetchCategoryOptions(): Result<List<String>>
    suspend fun fetchTagOptions(): Result<List<String>>
    suspend fun fetchCountryPeerSnapshots(hashes: List<String>): Result<List<CountryPeerSnapshot>>
    suspend fun renameTorrent(hash: String, name: String): Result<Unit>
    suspend fun setTorrentLocation(hash: String, location: String): Result<Unit>
    suspend fun setTorrentCategory(hash: String, category: String): Result<Unit>
    suspend fun setTorrentTags(hash: String, oldTags: String, newTags: String): Result<Unit>
    suspend fun setTorrentSpeedLimit(hash: String, downloadLimitBytes: Long, uploadLimitBytes: Long): Result<Unit>
    suspend fun fetchGlobalSpeedLimits(): Result<GlobalSpeedLimits>
    suspend fun setGlobalSpeedLimits(limits: GlobalSpeedLimits): Result<Unit>
    suspend fun setTorrentShareRatio(hash: String, ratioLimit: Double): Result<Unit>
    suspend fun addTorrent(request: com.hjw.qbremote.data.model.AddTorrentRequest): Result<Unit>
}

fun defaultCapabilitiesFor(backendType: ServerBackendType): ServerCapabilities {
    return when (backendType) {
        ServerBackendType.QBITTORRENT -> ServerCapabilities(
            supportsCategories = true,
            supportsTags = true,
            supportsCountryDistribution = true,
            supportsExportTorrent = true,
            supportsRename = true,
            supportsPerTorrentSpeedLimit = true,
            supportsShareRatio = true,
            supportsTrackers = true,
            supportsTrackerMutation = true,
            supportsReannounce = true,
            supportsRecheck = true,
            supportsFiles = true,
            supportsMoveLocation = true,
            supportsAddTorrent = true,
            supportsAdvancedAddOptions = true,
        )

        ServerBackendType.TRANSMISSION -> ServerCapabilities(
            supportsCategories = false,
            supportsTags = true,
            supportsCountryDistribution = false,
            supportsExportTorrent = false,
            supportsRename = true,
            supportsPerTorrentSpeedLimit = true,
            supportsShareRatio = true,
            supportsTrackers = true,
            supportsTrackerMutation = true,
            supportsReannounce = true,
            supportsRecheck = true,
            supportsFiles = true,
            supportsMoveLocation = true,
            supportsAddTorrent = true,
            supportsAdvancedAddOptions = false,
        )
    }
}
