package com.hjw.qbremote.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.hjw.qbremote.R
import com.hjw.qbremote.data.CachedDashboardServerSnapshot
import com.hjw.qbremote.data.ServerBackendType
import com.hjw.qbremote.data.ServerDashboardPreferences
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TransferInfo
import java.net.URLDecoder
import java.util.Locale

internal sealed interface LegendLabelSpec {
    data class Raw(val text: String) : LegendLabelSpec
    data class Res(@StringRes val resId: Int) : LegendLabelSpec
}

internal enum class LegendValueKind {
    TORRENT_COUNT,
    BYTES,
}

@Immutable
internal data class PieLegendSeedEntry(
    val label: LegendLabelSpec,
    val value: Long,
    val valueKind: LegendValueKind,
)

@Immutable
internal data class DashboardBarSeedEntry(
    val label: LegendLabelSpec,
    val value: Long,
    val valueKind: LegendValueKind,
)

@Immutable
internal data class ServerDashboardDisplayState(
    val profileId: String? = null,
    val backendType: ServerBackendType = ServerBackendType.QBITTORRENT,
    val serverVersion: String = "-",
    val transferInfo: TransferInfo = TransferInfo(),
    val torrents: List<TorrentInfo> = emptyList(),
    val torrentCount: Int = 0,
    val countryUploadStats: List<CountryUploadRecord> = emptyList(),
    val countryTopBarEntries: List<DashboardBarSeedEntry> = emptyList(),
    val tagUploadDate: String = "",
    val availableCards: List<DashboardChartCard> = emptyList(),
    val resolvedPreferences: ServerDashboardPreferences = ServerDashboardPreferences(),
    val categoryShareBarEntries: List<DashboardBarSeedEntry> = emptyList(),
    val dailyUploadEntries: List<PieLegendSeedEntry> = emptyList(),
    val transmissionLabelBarEntries: List<DashboardBarSeedEntry> = emptyList(),
    val transmissionStateEntries: List<PieLegendSeedEntry> = emptyList(),
    val trackerSiteBarEntries: List<DashboardBarSeedEntry> = emptyList(),
    val trackerSitePieEntries: List<PieLegendSeedEntry> = emptyList(),
    val hasContent: Boolean = false,
)

private enum class TransmissionStateBucket(@StringRes val labelRes: Int) {
    UPLOADING(R.string.status_uploading),
    DOWNLOADING(R.string.status_downloading),
    PAUSED(R.string.status_paused),
    QUEUED(R.string.state_queued),
    CHECKING(R.string.status_checking),
    COMPLETED(R.string.status_completed),
    ERROR(R.string.status_error),
    UNKNOWN(R.string.state_unknown),
}

internal fun buildServerDashboardDisplayState(
    snapshot: CachedDashboardServerSnapshot?,
    backendType: ServerBackendType,
    preferences: ServerDashboardPreferences?,
): ServerDashboardDisplayState {
    val availableCards = availableDashboardCardsForBackend(backendType)
    val resolvedPreferences = resolveServerDashboardPreferencesForBackend(
        preferences = preferences,
        backendType = backendType,
    )
    if (snapshot == null) {
        return ServerDashboardDisplayState(
            backendType = backendType,
            availableCards = availableCards,
            resolvedPreferences = resolvedPreferences,
        )
    }

    val torrents = snapshot.torrents
    val dailyUploadEntries = buildDailyUploadSeedEntries(snapshot)
    val mergedCountryStats = mergeCountryUploadRecordsForDisplay(snapshot.dailyCountryUploadStats)

    return ServerDashboardDisplayState(
        profileId = snapshot.profileId,
        backendType = backendType,
        serverVersion = snapshot.serverVersion.ifBlank { "-" },
        transferInfo = snapshot.transferInfo,
        torrents = torrents,
        torrentCount = maxOf(torrents.size, snapshot.transferInfo.totalTorrentCount),
        countryUploadStats = mergedCountryStats,
        countryTopBarEntries = mergedCountryStats
            .sortedByDescending { it.uploadedBytes }
            .take(5)
            .map { entry ->
                DashboardBarSeedEntry(
                    label = LegendLabelSpec.Raw(
                        compactCountryLabelForDisplay(
                            countryCode = entry.countryCode,
                            fallbackName = entry.countryName,
                            locale = Locale.getDefault(),
                        ),
                    ),
                    value = entry.uploadedBytes,
                    valueKind = LegendValueKind.BYTES,
                )
            },
        tagUploadDate = snapshot.dailyTagUploadDate,
        availableCards = availableCards,
        resolvedPreferences = resolvedPreferences,
        categoryShareBarEntries = if (backendType == ServerBackendType.QBITTORRENT) {
            buildCategoryShareBarEntries(torrents)
        } else {
            emptyList()
        },
        dailyUploadEntries = dailyUploadEntries,
        transmissionLabelBarEntries = if (backendType == ServerBackendType.TRANSMISSION) {
            buildTransmissionLabelBarEntries(torrents)
        } else {
            emptyList()
        },
        transmissionStateEntries = if (backendType == ServerBackendType.TRANSMISSION) {
            buildTransmissionStateSeedEntries(torrents)
        } else {
            emptyList()
        },
        trackerSiteBarEntries = buildTransmissionTrackerSiteBarEntries(torrents),
        trackerSitePieEntries = collapsePieEntries(
            entries = buildTransmissionTrackerSiteBarEntries(torrents).map {
                PieLegendSeedEntry(it.label, it.value, it.valueKind)
            },
            maxEntries = 8,
            otherLabel = LegendLabelSpec.Res(R.string.chart_other_label),
        ),
        hasContent = true,
    )
}

internal fun availableDashboardCardsForBackend(
    backendType: ServerBackendType,
): List<DashboardChartCard> {
    return when (backendType) {
        ServerBackendType.QBITTORRENT -> listOf(
            DashboardChartCard.COUNTRY_FLOW,
            DashboardChartCard.CATEGORY_SHARE,
            DashboardChartCard.DAILY_UPLOAD,
            DashboardChartCard.TRACKER_SITE,
        )

        ServerBackendType.TRANSMISSION -> listOf(
            DashboardChartCard.CATEGORY_SHARE,
            DashboardChartCard.TAG_UPLOAD,
            DashboardChartCard.TORRENT_STATE,
            DashboardChartCard.TRACKER_SITE,
        )
    }
}

internal fun defaultServerDashboardPreferencesForBackend(
    backendType: ServerBackendType,
): ServerDashboardPreferences {
    val visibleCards = availableDashboardCardsForBackend(backendType).map { it.storageKey }
    return ServerDashboardPreferences(
        visibleCards = visibleCards,
        cardOrder = visibleCards.joinToString(","),
    )
}

internal fun resolveServerDashboardPreferencesForBackend(
    preferences: ServerDashboardPreferences?,
    backendType: ServerBackendType,
): ServerDashboardPreferences {
    val defaults = defaultServerDashboardPreferencesForBackend(backendType)
    val availableCards = availableDashboardCardsForBackend(backendType)
    val availableStorageKeys = availableCards.map { it.storageKey }.toSet()
    val rawVisibleCards = preferences?.visibleCards
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
    val filteredVisibleCards = rawVisibleCards
        ?.filter { token -> token in availableStorageKeys }
        .orEmpty()
    val visibleCards = when {
        preferences == null -> defaults.visibleCards
        filteredVisibleCards.isNotEmpty() -> filteredVisibleCards
        rawVisibleCards.isNullOrEmpty() -> emptyList()
        else -> defaults.visibleCards
    }
    val rawOrder = preferences?.cardOrder?.takeIf { it.isNotBlank() }
    val parsedOrder = rawOrder
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() && it in availableStorageKeys }
        .orEmpty()
    val cardOrder = when {
        preferences == null -> defaults.cardOrder
        parsedOrder.isNotEmpty() -> {
            val merged = buildList {
                parsedOrder.distinct().forEach(::add)
                availableCards.map { it.storageKey }.forEach { key ->
                    if (key !in this) add(key)
                }
            }
            merged.joinToString(",")
        }
        rawOrder == null -> defaults.cardOrder
        else -> defaults.cardOrder
    }
    return defaults.copy(
        visibleCards = visibleCards,
        cardOrder = cardOrder,
    )
}

private fun buildCategoryShareBarEntries(
    torrents: List<TorrentInfo>,
): List<DashboardBarSeedEntry> {
    val grouped = linkedMapOf<LegendLabelSpec, Long>()
    torrents.forEach { torrent ->
        val normalized = torrent.category.trim()
        val labelSpec = when {
            normalized.isBlank() || normalized == "-" || normalized.equals("null", ignoreCase = true) ->
                LegendLabelSpec.Res(R.string.no_category)
            else -> LegendLabelSpec.Raw(normalized)
        }
        grouped[labelSpec] = (grouped[labelSpec] ?: 0L) + 1L
    }
    return collapseBarEntries(
        grouped.entries.sortedByDescending { it.value }.map { (label, count) ->
            DashboardBarSeedEntry(
                label = label,
                value = count,
                valueKind = LegendValueKind.TORRENT_COUNT,
            )
        },
        maxEntries = 7,
        otherLabel = LegendLabelSpec.Res(R.string.chart_other_label),
    )
}

private fun buildDailyUploadSeedEntries(
    snapshot: CachedDashboardServerSnapshot,
): List<PieLegendSeedEntry> {
    val entries = snapshot.dailyTagUploadStats
        .filter { it.uploadedBytes > 0L }
        .map { stat ->
            PieLegendSeedEntry(
                label = if (stat.isNoTag) {
                    LegendLabelSpec.Res(R.string.no_tags)
                } else {
                    LegendLabelSpec.Raw(stat.tag)
                },
                value = stat.uploadedBytes,
                valueKind = LegendValueKind.BYTES,
            )
        }
    return collapsePieEntries(
        entries = entries.sortedByDescending { it.value },
        maxEntries = 7,
        otherLabel = LegendLabelSpec.Res(R.string.chart_other_label),
    )
}

private fun buildTransmissionLabelBarEntries(
    torrents: List<TorrentInfo>,
): List<DashboardBarSeedEntry> {
    val grouped = linkedMapOf<LegendLabelSpec, Long>()
    torrents.forEach { torrent ->
        val tags = parseTags(torrent.tags)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }
            .distinctBy { it.lowercase() }

        if (tags.isEmpty()) {
            val label = LegendLabelSpec.Res(R.string.no_tags)
            grouped[label] = (grouped[label] ?: 0L) + 1L
        } else {
            tags.forEach { tag ->
                val label = LegendLabelSpec.Raw(tag)
                grouped[label] = (grouped[label] ?: 0L) + 1L
            }
        }
    }
    return collapseBarEntries(
        entries = grouped.entries.sortedByDescending { it.value }.map { (label, count) ->
            DashboardBarSeedEntry(
                label = label,
                value = count,
                valueKind = LegendValueKind.TORRENT_COUNT,
            )
        },
        maxEntries = 7,
        otherLabel = LegendLabelSpec.Res(R.string.chart_other_label),
    )
}

private fun buildTransmissionStateSeedEntries(
    torrents: List<TorrentInfo>,
): List<PieLegendSeedEntry> {
    val grouped = torrents.groupingBy(::transmissionStateBucketOf).eachCount()
    val rawEntries = TransmissionStateBucket.entries.mapNotNull { bucket ->
        grouped[bucket]?.takeIf { it > 0 }?.let { count ->
            PieLegendSeedEntry(
                label = LegendLabelSpec.Res(bucket.labelRes),
                value = count.toLong(),
                valueKind = LegendValueKind.TORRENT_COUNT,
            )
        }
    }
    return collapsePieEntries(
        entries = rawEntries,
        maxEntries = 7,
        otherLabel = LegendLabelSpec.Res(R.string.chart_other_label),
    )
}

private fun buildTransmissionTrackerSiteBarEntries(
    torrents: List<TorrentInfo>,
): List<DashboardBarSeedEntry> {
    val grouped = linkedMapOf<LegendLabelSpec, Long>()
    torrents.forEach { torrent ->
        val trackerLabel = trackerSiteLegendLabel(torrent)
        grouped[trackerLabel] = (grouped[trackerLabel] ?: 0L) + 1L
    }
    return collapseBarEntries(
        entries = grouped.entries.sortedByDescending { it.value }.map { (label, count) ->
            DashboardBarSeedEntry(
                label = label,
                value = count,
                valueKind = LegendValueKind.TORRENT_COUNT,
            )
        },
        maxEntries = 7,
        otherLabel = LegendLabelSpec.Res(R.string.chart_other_label),
    )
}

private fun transmissionStateBucketOf(torrent: TorrentInfo): TransmissionStateBucket {
    val state = normalizeTorrentState(effectiveTorrentState(torrent))
    return when {
        state in setOf("uploading", "forcedup") -> TransmissionStateBucket.UPLOADING
        state == "stalledup" || (torrent.progress >= 1f && state in setOf("pausedup", "stoppedup")) ->
            TransmissionStateBucket.COMPLETED
        state in setOf("downloading", "forceddl", "stalleddl", "metadl", "forcedmetadl", "allocating", "moving") ->
            TransmissionStateBucket.DOWNLOADING
        state in setOf("checkingdl", "checkingup", "checkingresumedata") ->
            TransmissionStateBucket.CHECKING
        state in setOf("queueddl", "queuedup") -> TransmissionStateBucket.QUEUED
        state in setOf("pauseddl", "pausedup", "stoppeddl", "stoppedup") ->
            if (torrent.progress >= 1f) TransmissionStateBucket.COMPLETED else TransmissionStateBucket.PAUSED
        state in setOf("error", "missingfiles") -> TransmissionStateBucket.ERROR
        torrent.progress >= 1f -> TransmissionStateBucket.COMPLETED
        else -> TransmissionStateBucket.UNKNOWN
    }
}

private fun trackerSiteLegendLabel(torrent: TorrentInfo): LegendLabelSpec {
    val trackerSite = formatTrackerSiteName(
        tracker = torrent.tracker,
        unknownLabel = "",
    ).trim()
    if (trackerSite.isNotBlank()) return LegendLabelSpec.Raw(trackerSite)

    val magnetTracker = firstTrackerFromMagnetUri(torrent.magnetUri)
    if (magnetTracker != null) {
        val magnetSite = formatTrackerSiteName(
            tracker = magnetTracker,
            unknownLabel = "",
        ).trim()
        if (magnetSite.isNotBlank()) return LegendLabelSpec.Raw(magnetSite)
    }

    return if (torrent.tracker.isBlank() && torrent.magnetUri.isBlank()) {
        LegendLabelSpec.Res(R.string.dashboard_tracker_site_none)
    } else {
        LegendLabelSpec.Res(R.string.dashboard_tracker_site_unknown)
    }
}

internal fun firstTrackerFromMagnetUri(magnetUri: String): String? {
    var candidate = magnetUri.trim()
    if (candidate.isBlank()) return null
    var attempts = 0
    while (!candidate.startsWith("magnet:", ignoreCase = true) && attempts < 3) {
        val decoded = runCatching { URLDecoder.decode(candidate, "UTF-8") }.getOrNull() ?: return null
        if (decoded == candidate) return null
        candidate = decoded.trim()
        attempts++
    }
    if (!candidate.startsWith("magnet:", ignoreCase = true)) return null

    val query = candidate.substringAfter('?', missingDelimiterValue = "")
    if (query.isBlank()) return null
    query.split('&').forEach { param ->
        val key = param.substringBefore('=', missingDelimiterValue = "")
        if (!key.equals("tr", ignoreCase = true)) return@forEach
        val rawValue = param.substringAfter('=', missingDelimiterValue = "").trim()
        if (rawValue.isBlank()) return@forEach
        val decodedValue = runCatching { URLDecoder.decode(rawValue, "UTF-8") }
            .getOrDefault(rawValue)
            .trim()
        if (decodedValue.isNotBlank()) return decodedValue
    }
    return null
}

private fun collapsePieEntries(
    entries: List<PieLegendSeedEntry>,
    maxEntries: Int,
    otherLabel: LegendLabelSpec,
): List<PieLegendSeedEntry> {
    if (entries.isEmpty()) return emptyList()
    if (entries.size <= maxEntries) return entries

    val safeMax = maxEntries.coerceAtLeast(2)
    val head = entries.take(safeMax - 1)
    val otherValue = entries.drop(safeMax - 1).sumOf { it.value }
    return if (otherValue > 0L) {
        head + PieLegendSeedEntry(
            label = otherLabel,
            value = otherValue,
            valueKind = entries.first().valueKind,
        )
    } else {
        head
    }
}

private fun collapseBarEntries(
    entries: List<DashboardBarSeedEntry>,
    maxEntries: Int,
    otherLabel: LegendLabelSpec,
): List<DashboardBarSeedEntry> {
    if (entries.isEmpty()) return emptyList()
    if (entries.size <= maxEntries) return entries

    val safeMax = maxEntries.coerceAtLeast(2)
    val head = entries.take(safeMax - 1)
    val otherValue = entries.drop(safeMax - 1).sumOf { it.value }
    return if (otherValue > 0L) {
        head + DashboardBarSeedEntry(
            label = otherLabel,
            value = otherValue,
            valueKind = entries.first().valueKind,
        )
    } else {
        head
    }
}
