package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ConnectionSettings
import com.hjw.qbremote.data.HomeAggregateSpeedHistorySnapshot
import com.hjw.qbremote.data.HomeSpeedHistoryPoint
import com.hjw.qbremote.data.ServerCapabilities
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.model.TransferInfo

internal fun buildPendingActionKey(
    profileId: String,
    hash: String,
): String {
    return "${profileId.trim()}|${hash.trim()}"
}

internal fun shouldApplyActiveProfileAsyncResult(
    requestedProfileId: String,
    requestVersion: Long,
    activeProfileId: String?,
    activeRequestVersion: Long,
): Boolean {
    val normalizedProfileId = requestedProfileId.trim()
    return normalizedProfileId.isNotBlank() &&
        activeProfileId == normalizedProfileId &&
        activeRequestVersion == requestVersion
}

internal fun buildDailyUploadTrackingScopeKey(
    activeProfileId: String?,
    settings: ConnectionSettings,
): String {
    val normalizedProfileId = activeProfileId.orEmpty().trim()
    if (normalizedProfileId.isNotBlank()) {
        return "profile:$normalizedProfileId"
    }

    val host = settings.host.trim().lowercase()
    return if (host.isNotBlank()) {
        "server:${settings.useHttps}|$host|${settings.port}"
    } else {
        "default"
    }
}

internal fun normalizeProfileIdsForRefresh(
    profiles: List<ServerProfile>,
): List<String> {
    return profiles
        .map { profile -> profile.id.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

internal fun resolvePreferredProfileId(
    availableIds: List<String>,
    primaryCandidate: String?,
    secondaryCandidate: String?,
): String? {
    if (availableIds.isEmpty()) return null
    val availableIdSet = availableIds.toHashSet()
    return primaryCandidate?.takeIf { it in availableIdSet }
        ?: secondaryCandidate?.takeIf { it in availableIdSet }
        ?: availableIds.firstOrNull()
}

internal fun filterDashboardPreferencesForProfiles(
    preferences: Map<String, ServerDashboardPreferences>,
    profiles: List<ServerProfile>,
): Map<String, ServerDashboardPreferences> {
    if (preferences.isEmpty() || profiles.isEmpty()) return emptyMap()
    val profileIdSet = profiles.mapTo(mutableSetOf()) { profile -> profile.id }
    return preferences.filterKeys { profileId -> profileId in profileIdSet }
}

internal fun resolveSelectedDashboardProfileId(
    activeProfileId: String?,
    selectedDashboardProfileId: String?,
    snapshots: List<CachedDashboardServerSnapshot>,
): String? {
    return resolvePreferredProfileId(
        availableIds = snapshots.map { snapshot -> snapshot.profileId },
        primaryCandidate = activeProfileId,
        secondaryCandidate = selectedDashboardProfileId,
    )
}

internal fun applyDashboardSnapshotsToState(
    current: MainUiState,
    orderedSnapshots: List<CachedDashboardServerSnapshot>,
    aggregate: DashboardAggregateState,
): MainUiState {
    return current.copy(
        dashboardServerSnapshots = orderedSnapshots,
        selectedDashboardProfileId = resolveSelectedDashboardProfileId(
            activeProfileId = current.activeServerProfileId,
            selectedDashboardProfileId = current.selectedDashboardProfileId,
            snapshots = orderedSnapshots,
        ),
        dashboardAggregate = aggregate.copy(
            chartTransferInfo = current.dashboardAggregate.chartTransferInfo,
        ),
        aggregateOnlineServerCount = orderedSnapshots.count { !it.isStale },
    )
}

internal fun restoreHomeRealtimeSpeedSeries(
    snapshot: HomeAggregateSpeedHistorySnapshot,
    maxPoints: Int,
): List<RealtimeSpeedPoint> {
    if (maxPoints <= 0) return emptyList()
    return snapshot.points
        .map { point ->
            RealtimeSpeedPoint(
                timestamp = point.timestamp.coerceAtLeast(0L),
                uploadSpeed = point.uploadSpeed.coerceAtLeast(0L),
                downloadSpeed = point.downloadSpeed.coerceAtLeast(0L),
                onlineServerCount = point.onlineServerCount.coerceAtLeast(0),
            )
        }
        .sortedBy { point -> point.timestamp }
        .toList()
        .takeLast(maxPoints)
}

internal fun buildHomeRealtimeSpeedScopeKey(
    profileIds: List<String>,
    fallbackScopeKey: String,
): String {
    val profileSetKey = profileIds
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .joinToString(",")
    return if (profileSetKey.isNotBlank()) {
        "profiles:$profileSetKey"
    } else {
        "fallback:${fallbackScopeKey.trim()}"
    }
}

internal fun restoreHomeRealtimeSpeedSeriesForScope(
    snapshot: HomeAggregateSpeedHistorySnapshot?,
    scopeKey: String,
    maxPoints: Int,
): List<RealtimeSpeedPoint> {
    if (snapshot == null) return emptyList()
    val normalizedScopeKey = scopeKey.trim()
    if (normalizedScopeKey.isBlank() || snapshot.scopeKey != normalizedScopeKey) return emptyList()
    return restoreHomeRealtimeSpeedSeries(snapshot, maxPoints)
}

internal fun resolveHomeSpeedRefreshIntervalSeconds(scene: RefreshScene): Int? {
    return if (scene == RefreshScene.DASHBOARD) 3 else null
}

internal fun buildHomeChartTransferInfo(
    transferInfos: Collection<TransferInfo>,
): TransferInfo {
    return transferInfos.fold(TransferInfo()) { acc, transferInfo ->
        TransferInfo(
            downloadSpeed = acc.downloadSpeed + transferInfo.downloadSpeed,
            uploadSpeed = acc.uploadSpeed + transferInfo.uploadSpeed,
            downloadedTotal = acc.downloadedTotal + transferInfo.downloadedTotal,
            uploadedTotal = acc.uploadedTotal + transferInfo.uploadedTotal,
            downloadRateLimit = acc.downloadRateLimit + transferInfo.downloadRateLimit,
            uploadRateLimit = acc.uploadRateLimit + transferInfo.uploadRateLimit,
            freeSpaceOnDisk = acc.freeSpaceOnDisk + transferInfo.freeSpaceOnDisk,
            dhtNodes = acc.dhtNodes + transferInfo.dhtNodes,
        )
    }
}

internal fun applyHomeChartRefreshToAggregate(
    aggregate: DashboardAggregateState,
    chartTransferInfo: TransferInfo,
    chartSeries: List<RealtimeSpeedPoint>,
): DashboardAggregateState {
    return aggregate.copy(
        chartTransferInfo = chartTransferInfo,
        realtimeSpeedSeries = chartSeries,
    )
}

internal fun applyServerSwitchReset(
    current: MainUiState,
    settings: ConnectionSettings,
    activeProfileId: String?,
    capabilities: ServerCapabilities,
): MainUiState {
    return current.copy(
        settings = settings,
        activeServerProfileId = activeProfileId,
        activeCapabilities = capabilities,
        connected = false,
        serverVersion = "-",
        transferInfo = TransferInfo(),
        torrents = emptyList(),
        dailyTagUploadDate = "",
        dailyTagUploadStats = emptyList(),
        dailyCountryUploadDate = "",
        dailyCountryUploadStats = emptyList(),
        selectedDashboardProfileId = activeProfileId ?: current.selectedDashboardProfileId,
        dashboardCacheHydrated = false,
        hasDashboardSnapshot = false,
        detailHash = "",
        detailLoading = false,
        detailProperties = null,
        detailFiles = emptyList(),
        detailTrackers = emptyList(),
        pendingActionKeys = emptySet(),
    )
}

internal fun prepareServerDashboardTransitionState(
    current: MainUiState,
    profileId: String,
): MainUiState {
    return current.copy(
        selectedDashboardProfileId = profileId,
        dashboardSessionToken = current.dashboardSessionToken + 1L,
        isConnecting = true,
        connected = false,
        errorMessage = null,
        pendingBackendRepair = current.pendingBackendRepair
            ?.takeUnless { it.profileId != profileId },
        serverVersion = "-",
        transferInfo = TransferInfo(),
        torrents = emptyList(),
        dailyTagUploadDate = "",
        dailyTagUploadStats = emptyList(),
        dailyCountryUploadDate = "",
        dailyCountryUploadStats = emptyList(),
        categoryOptions = emptyList(),
        tagOptions = emptyList(),
        dashboardCacheHydrated = false,
        hasDashboardSnapshot = false,
        detailHash = "",
        detailLoading = false,
        detailProperties = null,
        detailFiles = emptyList(),
        detailTrackers = emptyList(),
        pendingActionKeys = emptySet(),
    )
}

internal fun prepareConnectingProfileState(
    current: MainUiState,
    settings: ConnectionSettings,
    profileId: String,
    capabilities: ServerCapabilities,
): MainUiState {
    return current.copy(
        settings = settings,
        activeServerProfileId = profileId,
        selectedDashboardProfileId = profileId,
        dashboardSessionToken = current.dashboardSessionToken + 1L,
        activeCapabilities = capabilities,
        isConnecting = true,
        connected = false,
        pendingBackendRepair = null,
        errorMessage = null,
        serverVersion = "-",
        transferInfo = TransferInfo(),
        torrents = emptyList(),
        dailyTagUploadDate = "",
        dailyTagUploadStats = emptyList(),
        dailyCountryUploadDate = "",
        dailyCountryUploadStats = emptyList(),
        categoryOptions = emptyList(),
        tagOptions = emptyList(),
        detailHash = "",
        detailLoading = false,
        detailProperties = null,
        detailFiles = emptyList(),
        detailTrackers = emptyList(),
        pendingActionKeys = emptySet(),
    )
}
