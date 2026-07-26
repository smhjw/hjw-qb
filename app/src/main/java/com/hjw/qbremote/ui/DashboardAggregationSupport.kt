package com.hjw.qbremote.ui

import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.defaultCapabilitiesFor
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TransferInfo
import java.util.Locale

private const val DASHBOARD_AGGREGATE_NO_TAG_KEY = "__NO_TAG__"

internal fun buildDashboardAggregateFromSnapshots(
    snapshots: List<CachedDashboardServerSnapshot>,
): DashboardAggregateState {
    if (snapshots.isEmpty()) return DashboardAggregateState()

    val liveSnapshots = snapshots.filter { !it.isStale }
    val aggregateSource = liveSnapshots.ifEmpty { snapshots }
    val totalTransfer = liveSnapshots.fold(TransferInfo()) { acc, snapshot ->
        TransferInfo(
            downloadSpeed = acc.downloadSpeed + snapshot.transferInfo.downloadSpeed,
            uploadSpeed = acc.uploadSpeed + snapshot.transferInfo.uploadSpeed,
            downloadedTotal = acc.downloadedTotal + snapshot.transferInfo.downloadedTotal,
            uploadedTotal = acc.uploadedTotal + snapshot.transferInfo.uploadedTotal,
            downloadRateLimit = acc.downloadRateLimit + snapshot.transferInfo.downloadRateLimit,
            uploadRateLimit = acc.uploadRateLimit + snapshot.transferInfo.uploadRateLimit,
            freeSpaceOnDisk = acc.freeSpaceOnDisk + snapshot.transferInfo.freeSpaceOnDisk,
            dhtNodes = acc.dhtNodes + snapshot.transferInfo.dhtNodes,
        )
    }
    val mergedTorrents = aggregateSource.flatMap { it.torrents }
    val mergedTagStats = aggregateDashboardTagStats(aggregateSource)
    val mergedCountryStats = aggregateDashboardCountryStats(aggregateSource)

    return DashboardAggregateState(
        transferInfo = totalTransfer,
        torrents = mergedTorrents,
        dailyTagUploadDate = mergedTagStats.first,
        dailyTagUploadStats = mergedTagStats.second,
        dailyCountryUploadDate = mergedCountryStats.first,
        dailyCountryUploadStats = mergedCountryStats.second,
        totalServerCount = snapshots.size,
        categoryCoverageServerCount = snapshots.count {
            defaultCapabilitiesFor(it.backendType).supportsCategories
        },
        countryCoverageServerCount = snapshots.count {
            defaultCapabilitiesFor(it.backendType).supportsCountryDistribution
        },
    )
}

private fun aggregateDashboardTagStats(
    snapshots: List<CachedDashboardServerSnapshot>,
): Pair<String, List<DailyTagUploadStat>> {
    val totals = linkedMapOf<String, DailyTagUploadStat>()
    snapshots.forEach { snapshot ->
        snapshot.dailyTagUploadStats.forEach { stat ->
            val key = if (stat.isNoTag) DASHBOARD_AGGREGATE_NO_TAG_KEY else stat.tag.trim().lowercase(Locale.US)
            val existing = totals[key]
            totals[key] = DailyTagUploadStat(
                tag = if (stat.isNoTag) DASHBOARD_AGGREGATE_NO_TAG_KEY else stat.tag,
                uploadedBytes = (existing?.uploadedBytes ?: 0L) + stat.uploadedBytes,
                torrentCount = (existing?.torrentCount ?: 0) + stat.torrentCount,
                isNoTag = stat.isNoTag,
            )
        }
    }
    val date = snapshots.map { it.dailyTagUploadDate.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    return date to totals.values
        .filter { it.uploadedBytes > 0L }
        .sortedByDescending { it.uploadedBytes }
}

private fun aggregateDashboardCountryStats(
    snapshots: List<CachedDashboardServerSnapshot>,
): Pair<String, List<CountryUploadRecord>> {
    val totals = linkedMapOf<String, CountryUploadRecord>()
    snapshots
        .filter { defaultCapabilitiesFor(it.backendType).supportsCountryDistribution }
        .forEach { snapshot ->
            snapshot.dailyCountryUploadStats.forEach recordLoop@{ record ->
                val key = record.countryCode.trim().uppercase(Locale.US)
                if (key.isBlank()) return@recordLoop
                val existing = totals[key]
                totals[key] = CountryUploadRecord(
                    countryCode = key,
                    countryName = record.countryName.ifBlank { existing?.countryName.orEmpty() },
                    uploadedBytes = (existing?.uploadedBytes ?: 0L) + record.uploadedBytes,
                )
            }
        }
    val date = snapshots.map { it.dailyCountryUploadDate.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    return date to totals.values
        .filter { it.uploadedBytes > 0L }
        .sortedByDescending { it.uploadedBytes }
}
