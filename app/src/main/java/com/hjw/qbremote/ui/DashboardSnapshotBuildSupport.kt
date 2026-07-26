package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerProfile
import com.hjw.qbremote.data.defaultCapabilitiesFor

internal fun buildActiveDashboardServerSnapshot(
    profile: ServerProfile,
    state: MainUiState,
): CachedDashboardServerSnapshot {
    return CachedDashboardServerSnapshot(
        profileId = profile.id,
        profileName = profile.name,
        backendType = profile.backendType,
        host = profile.host,
        port = profile.port,
        useHttps = profile.useHttps,
        serverVersion = state.serverVersion,
        transferInfo = state.transferInfo,
        torrents = state.torrents,
        dailyTagUploadDate = state.dailyTagUploadDate,
        dailyTagUploadStats = state.dailyTagUploadStats.map { stat ->
            CachedDailyTagUploadStat(
                tag = stat.tag,
                uploadedBytes = stat.uploadedBytes,
                torrentCount = stat.torrentCount,
                isNoTag = stat.isNoTag,
            )
        },
        dailyCountryUploadDate = state.dailyCountryUploadDate,
        dailyCountryUploadStats = state.dailyCountryUploadStats,
        lastUpdatedAt = System.currentTimeMillis(),
        errorMessage = "",
        isStale = false,
    )
}

internal fun buildCachedDashboardSnapshotFromFetch(
    profile: ServerProfile,
    fetched: com.hjw.qbremote.data.DashboardSnapshotFetchResult,
    previousSnapshot: CachedDashboardServerSnapshot?,
): CachedDashboardServerSnapshot {
    val preservedCountryDate = if (defaultCapabilitiesFor(profile.backendType).supportsCountryDistribution) {
        previousSnapshot?.dailyCountryUploadDate.orEmpty()
    } else {
        ""
    }
    val preservedCountryStats = if (defaultCapabilitiesFor(profile.backendType).supportsCountryDistribution) {
        previousSnapshot?.dailyCountryUploadStats ?: emptyList()
    } else {
        emptyList()
    }
    return CachedDashboardServerSnapshot(
        profileId = profile.id,
        profileName = profile.name,
        backendType = profile.backendType,
        host = profile.host,
        port = profile.port,
        useHttps = profile.useHttps,
        serverVersion = fetched.serverVersion,
        transferInfo = fetched.dashboardData.transferInfo,
        torrents = fetched.dashboardData.torrents,
        dailyTagUploadDate = previousSnapshot?.dailyTagUploadDate.orEmpty(),
        dailyTagUploadStats = previousSnapshot?.dailyTagUploadStats ?: emptyList(),
        dailyCountryUploadDate = preservedCountryDate,
        dailyCountryUploadStats = preservedCountryStats,
        lastUpdatedAt = System.currentTimeMillis(),
        errorMessage = "",
        isStale = false,
    )
}

// 区别于 VerticalReorderSupport.kt 的 orderDashboardServerSnapshots（按拖拽顺序重排），本函数按 profile 列表合并缓存快照。
internal fun orderedDashboardServerSnapshots(
    profiles: List<ServerProfile>,
    snapshotsById: Map<String, CachedDashboardServerSnapshot>,
): List<CachedDashboardServerSnapshot> {
    return profiles.map { profile ->
        snapshotsById[profile.id]?.copy(
            profileName = profile.name,
            backendType = profile.backendType,
            host = profile.host,
            port = profile.port,
            useHttps = profile.useHttps,
        ) ?: CachedDashboardServerSnapshot(
            profileId = profile.id,
            profileName = profile.name,
            backendType = profile.backendType,
            host = profile.host,
            port = profile.port,
            useHttps = profile.useHttps,
            isStale = true,
        )
    }
}

internal fun buildStaleDashboardServerSnapshot(
    profileId: String,
    profileName: String,
    backendType: ServerBackendType,
    host: String,
    port: Int,
    useHttps: Boolean,
    previousSnapshot: CachedDashboardServerSnapshot?,
    errorMessage: String,
): CachedDashboardServerSnapshot {
    return (previousSnapshot ?: CachedDashboardServerSnapshot(
        profileId = profileId,
        profileName = profileName,
        backendType = backendType,
        host = host,
        port = port,
        useHttps = useHttps,
    )).copy(
        profileName = profileName,
        backendType = backendType,
        host = host,
        port = port,
        useHttps = useHttps,
        errorMessage = errorMessage,
        isStale = true,
    )
}
