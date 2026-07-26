package com.hjw.qbremote.ui

import com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot
import com.hjw.qbremote.data.model.CountryPeerSnapshot
import com.hjw.qbremote.data.model.CountryUploadRecord
import com.hjw.qbremote.data.model.DailyCountryUploadStats
import com.hjw.qbremote.data.model.TorrentInfo
import java.time.LocalDate
import java.util.Locale

internal fun resolveActiveCountryUploadHashes(
    previous: DailyCountryUploadTrackingSnapshot?,
    today: LocalDate,
    torrents: List<TorrentInfo>,
): List<String> {
    val lastSeenByTorrent = previous?.lastSeenByTorrent?.toMutableMap() ?: mutableMapOf()
    val snapshotDate = runCatching {
        previous?.date?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    }.getOrNull()

    if (snapshotDate != today) {
        lastSeenByTorrent.clear()
    }

    val activeHashes = mutableListOf<String>()
    torrents.forEach { torrent ->
        val trackingKey = dailyCountryTorrentTrackingKey(torrent)
        val hash = torrent.hash.trim()
        if (hash.isBlank()) return@forEach
        val currentUploaded = torrent.uploaded.coerceAtLeast(0L)
        val previousUploaded = lastSeenByTorrent[trackingKey]
        lastSeenByTorrent[trackingKey] = currentUploaded
        if (previousUploaded == null) {
            if (torrent.uploadSpeed > 0L) {
                activeHashes += hash
            }
            return@forEach
        }
        if (currentUploaded > previousUploaded || torrent.uploadSpeed > 0L) {
            activeHashes += hash
        }
    }
    return activeHashes.distinct()
}

internal fun advanceDailyCountryUploadTrackingSnapshot(
    previous: DailyCountryUploadTrackingSnapshot?,
    today: LocalDate,
    torrents: List<TorrentInfo>,
    samples: List<CountryPeerSnapshot>,
): Pair<DailyCountryUploadTrackingSnapshot, DailyCountryUploadStats> {
    val totalsByCountry = previous?.totalsByCountry?.toMutableMap() ?: mutableMapOf()
    val peerSnapshots = previous?.peerSnapshots?.toMutableMap() ?: mutableMapOf()
    val lastSeenByTorrent = previous?.lastSeenByTorrent?.toMutableMap() ?: mutableMapOf()
    val snapshotDate = runCatching {
        previous?.date?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    }.getOrNull()

    if (snapshotDate != today) {
        totalsByCountry.clear()
        peerSnapshots.clear()
        lastSeenByTorrent.clear()
    }

    val activeKeys = torrents.map(::dailyCountryTorrentTrackingKey).toSet()
    lastSeenByTorrent.keys.retainAll(activeKeys)
    torrents.forEach { torrent ->
        val trackingKey = dailyCountryTorrentTrackingKey(torrent)
        if (torrent.hash.trim().isBlank()) return@forEach
        lastSeenByTorrent[trackingKey] = torrent.uploaded.coerceAtLeast(0L)
    }

    val currentPeerSnapshots = samples.associateBy { it.key }
    val fallbackNames = samples
        .groupBy { it.countryCode.trim().uppercase(Locale.US) }
        .mapValues { (_, entries) ->
            entries.firstNotNullOfOrNull { it.countryName.trim().takeIf(String::isNotBlank) }.orEmpty()
        }

    samples.forEach { entry ->
        val countryCode = entry.countryCode.trim().uppercase(Locale.US)
        if (countryCode.isBlank()) return@forEach
        val previousUploaded = peerSnapshots[entry.key]?.uploadedBytes?.coerceAtLeast(0L)
        val currentUploaded = entry.uploadedBytes.coerceAtLeast(0L)
        val delta = when {
            previousUploaded == null -> 0L
            currentUploaded < previousUploaded -> currentUploaded
            else -> currentUploaded - previousUploaded
        }
        if (delta <= 0L) return@forEach
        totalsByCountry[countryCode] = (totalsByCountry[countryCode] ?: 0L) + delta
    }

    peerSnapshots.keys.retainAll(currentPeerSnapshots.keys)
    peerSnapshots.putAll(currentPeerSnapshots)

    val snapshot = DailyCountryUploadTrackingSnapshot(
        date = today.toString(),
        totalsByCountry = totalsByCountry,
        peerSnapshots = peerSnapshots,
        lastSeenByTorrent = lastSeenByTorrent,
    )
    val stats = DailyCountryUploadStats(
        dateLabel = today.toString(),
        countries = totalsByCountry.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .map { (countryCode, uploadedBytes) ->
                CountryUploadRecord(
                    countryCode = countryCode,
                    countryName = fallbackNames[countryCode].orEmpty(),
                    uploadedBytes = uploadedBytes,
                )
            },
    )
    return snapshot to stats
}

internal data class CountryTrackingHashResolution(
    val lastSeenByTorrent: Map<String, Long>,
    val activeHashes: Map<String, Long>,
    val candidateHashes: List<String>,
)

internal fun resolveTrackedCountryHashes(
    torrents: List<TorrentInfo>,
    lastSeenByTorrent: Map<String, Long>,
    activeHashes: Map<String, Long>,
    now: Long,
    ttlMs: Long,
): CountryTrackingHashResolution {
    val nextLastSeenByTorrent = lastSeenByTorrent.toMutableMap()
    val nextActiveHashes = activeHashes.toMutableMap()
    val hashesByTrackingKey = torrents.associateBy(::dailyCountryTorrentTrackingKey)
    val normalizedTorrentHashes = torrents
        .map { torrent -> torrent.hash.trim() }
        .filter { hash -> hash.isNotBlank() }
        .toHashSet()

    torrents.forEach { torrent ->
        val trackingKey = dailyCountryTorrentTrackingKey(torrent)
        val hash = torrent.hash.trim()
        if (hash.isBlank()) return@forEach
        val currentUploaded = torrent.uploaded.coerceAtLeast(0L)
        val previousUploaded = nextLastSeenByTorrent[trackingKey]
        nextLastSeenByTorrent[trackingKey] = currentUploaded

        if (previousUploaded == null) {
            if (torrent.uploadSpeed > 0L) {
                nextActiveHashes[hash] = now + ttlMs
            }
            return@forEach
        }

        if (currentUploaded > previousUploaded || torrent.uploadSpeed > 0L) {
            nextActiveHashes[hash] = now + ttlMs
        }
    }

    nextLastSeenByTorrent.keys.retainAll(hashesByTrackingKey.keys)
    nextActiveHashes.entries.removeAll { (hash, expiresAt) ->
        expiresAt < now || hash !in normalizedTorrentHashes
    }

    return CountryTrackingHashResolution(
        lastSeenByTorrent = nextLastSeenByTorrent,
        activeHashes = nextActiveHashes,
        candidateHashes = nextActiveHashes.keys
            .filter { hash -> hash in normalizedTorrentHashes }
            .sorted(),
    )
}

internal fun dailyCountryTorrentTrackingKey(torrent: TorrentInfo): String {
    return torrent.hash.ifBlank {
        "${torrent.name}|${torrent.addedOn}|${torrent.savePath}|${torrent.size}"
    }
}
