package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDailyTagUploadStat
import com.hjw.qbremote.data.DashboardCacheSnapshot
import com.hjw.qbremote.data.model.TransferInfo

internal fun applyDashboardCacheHydration(
    current: MainUiState,
    cache: DashboardCacheSnapshot?,
): MainUiState {
    return if (cache == null) {
        current.copy(
            serverVersion = "-",
            transferInfo = TransferInfo(),
            torrents = emptyList(),
            dailyTagUploadDate = "",
            dailyTagUploadStats = emptyList(),
            dailyCountryUploadDate = "",
            dailyCountryUploadStats = emptyList(),
            categoryOptions = emptyList(),
            tagOptions = emptyList(),
            dashboardCacheHydrated = true,
            hasDashboardSnapshot = false,
        )
    } else {
        current.copy(
            transferInfo = cache.transferInfo,
            torrents = cache.torrents,
            dailyTagUploadDate = cache.dailyTagUploadDate,
            dailyTagUploadStats = cache.dailyTagUploadStats.map { stat -> stat.toDailyTagUploadStat() },
            dailyCountryUploadDate = cache.dailyCountryUploadDate,
            dailyCountryUploadStats = cache.dailyCountryUploadStats,
            dashboardCacheHydrated = true,
            hasDashboardSnapshot = true,
        )
    }
}

internal fun buildDashboardCacheSnapshot(state: MainUiState): DashboardCacheSnapshot {
    return DashboardCacheSnapshot(
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
    )
}

private fun CachedDailyTagUploadStat.toDailyTagUploadStat(): DailyTagUploadStat {
    return DailyTagUploadStat(
        tag = tag,
        uploadedBytes = uploadedBytes,
        torrentCount = torrentCount,
        isNoTag = isNoTag,
    )
}
