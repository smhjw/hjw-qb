package com.hjw.qbremote.data

private const val DASHBOARD_CARD_COUNTRY_FLOW = "country_flow"
private const val DASHBOARD_CARD_CATEGORY_SHARE = "category_share"
private const val DASHBOARD_CARD_DAILY_UPLOAD = "daily_upload"
private const val DASHBOARD_CARD_TAG_UPLOAD = "tag_upload"
private const val DASHBOARD_CARD_TORRENT_STATE = "torrent_state"
private const val DASHBOARD_CARD_TRACKER_SITE = "tracker_site"
private const val DASHBOARD_CARD_SHARE_RATIO_DISTRIBUTION = "share_ratio_distribution"
private val VALID_DASHBOARD_CARD_KEYS = setOf(
    DASHBOARD_CARD_COUNTRY_FLOW,
    DASHBOARD_CARD_CATEGORY_SHARE,
    DASHBOARD_CARD_DAILY_UPLOAD,
    DASHBOARD_CARD_TAG_UPLOAD,
    DASHBOARD_CARD_TORRENT_STATE,
    DASHBOARD_CARD_TRACKER_SITE,
    DASHBOARD_CARD_SHARE_RATIO_DISTRIBUTION,
)

internal fun DailyUploadTrackingSnapshot.normalized(): DailyUploadTrackingSnapshot {
    return copy(
        date = date.trim(),
        totalsByTag = totalsByTag
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, value) -> value.coerceAtLeast(0L) },
        countedTagsByTorrent = if (countedTagsByTorrent.size > 500) emptyMap() else countedTagsByTorrent
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, tags) ->
                tags.map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
            },
        lastSeenByTorrent = if (lastSeenByTorrent.size > 500) emptyMap() else lastSeenByTorrent
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, value) -> value.coerceAtLeast(0L) },
    )
}

internal fun DailyCountryUploadTrackingSnapshot.normalized(): DailyCountryUploadTrackingSnapshot {
    return copy(
        date = date.trim(),
        totalsByCountry = totalsByCountry
            .mapKeys { it.key.trim().uppercase() }
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, value) -> value.coerceAtLeast(0L) },
        peerSnapshots = if (peerSnapshots.size > 1000) emptyMap() else peerSnapshots
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, snapshot) ->
                snapshot.copy(
                    key = snapshot.key.trim(),
                    peerAddress = snapshot.peerAddress.trim(),
                    countryCode = snapshot.countryCode.trim().uppercase(),
                    countryName = snapshot.countryName.trim(),
                    uploadedBytes = snapshot.uploadedBytes.coerceAtLeast(0L),
                )
            },
        lastSeenByTorrent = if (lastSeenByTorrent.size > 500) emptyMap() else lastSeenByTorrent
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, value) -> value.coerceAtLeast(0L) },
    )
}

internal fun DashboardCacheSnapshot.normalized(): DashboardCacheSnapshot {
    return copy(
        torrents = sanitizeDashboardCacheForPersistence(this).torrents,
        dailyTagUploadDate = dailyTagUploadDate.trim(),
        dailyTagUploadStats = dailyTagUploadStats.map { it.copy(tag = it.tag.trim()) },
        dailyCountryUploadDate = dailyCountryUploadDate.trim(),
        dailyCountryUploadStats = dailyCountryUploadStats.map { record ->
            record.copy(
                countryCode = record.countryCode.trim().uppercase(),
                countryName = record.countryName.trim(),
                uploadedBytes = record.uploadedBytes.coerceAtLeast(0L),
            )
        },
    )
}

internal fun HomeSpeedHistoryPoint.normalized(): HomeSpeedHistoryPoint {
    return copy(
        timestamp = timestamp.coerceAtLeast(0L),
        uploadSpeed = uploadSpeed.coerceAtLeast(0L),
        downloadSpeed = downloadSpeed.coerceAtLeast(0L),
        onlineServerCount = onlineServerCount.coerceAtLeast(0),
    )
}

internal fun HomeAggregateSpeedHistorySnapshot.normalized(): HomeAggregateSpeedHistorySnapshot {
    return copy(
        scopeKey = scopeKey.trim(),
        points = points
            .map { it.normalized() }
            .sortedBy { it.timestamp },
    )
}

internal fun CachedDashboardServerSnapshot.normalized(): CachedDashboardServerSnapshot {
    return copy(
        torrents = sanitizeDashboardServerSnapshotForPersistence(this).torrents,
        profileId = profileId.trim(),
        profileName = profileName.trim(),
        host = host.trim(),
        port = port.coerceIn(1, 65535),
        serverVersion = serverVersion.trim().ifBlank { "-" },
        dailyTagUploadDate = dailyTagUploadDate.trim(),
        dailyTagUploadStats = dailyTagUploadStats.map { it.copy(tag = it.tag.trim()) },
        dailyCountryUploadDate = dailyCountryUploadDate.trim(),
        dailyCountryUploadStats = dailyCountryUploadStats.map { record ->
            record.copy(
                countryCode = record.countryCode.trim().uppercase(),
                countryName = record.countryName.trim(),
                uploadedBytes = record.uploadedBytes.coerceAtLeast(0L),
            )
        },
        lastUpdatedAt = lastUpdatedAt.coerceAtLeast(0L),
        errorMessage = errorMessage.trim(),
    )
}

internal fun ServerDashboardPreferences.normalized(): ServerDashboardPreferences {
    val normalizedVisible = visibleCards
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { it in VALID_DASHBOARD_CARD_KEYS }
        .distinct()
    val normalizedOrder = cardOrder
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { it in VALID_DASHBOARD_CARD_KEYS }
        .let { tokens ->
            val ordered = buildList {
                tokens.distinct().forEach(::add)
                normalizedVisible.forEach { token ->
                    if (!contains(token)) add(token)
                }
            }
            ordered.joinToString(",")
        }
    return copy(
        visibleCards = normalizedVisible,
        cardOrder = normalizedOrder,
    )
}

internal fun defaultServerDashboardPreferences(settings: ConnectionSettings): ServerDashboardPreferences {
    val visibleCards = when (settings.serverBackendType) {
        ServerBackendType.QBITTORRENT -> listOf(
            DASHBOARD_CARD_COUNTRY_FLOW,
            DASHBOARD_CARD_CATEGORY_SHARE,
            DASHBOARD_CARD_DAILY_UPLOAD,
            DASHBOARD_CARD_TRACKER_SITE,
        )
        ServerBackendType.TRANSMISSION -> listOf(
            DASHBOARD_CARD_CATEGORY_SHARE,
            DASHBOARD_CARD_TAG_UPLOAD,
            DASHBOARD_CARD_TORRENT_STATE,
            DASHBOARD_CARD_TRACKER_SITE,
        )
    }
    return ServerDashboardPreferences(
        visibleCards = visibleCards,
        cardOrder = visibleCards.joinToString(","),
    ).normalized()
}
