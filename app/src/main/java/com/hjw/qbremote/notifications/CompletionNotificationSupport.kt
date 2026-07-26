package com.hjw.qbremote.notifications

import com.hjw.qbremote.data.model.TorrentInfo

internal data class CompletedTorrentTransition(
    val key: String,
    val torrent: TorrentInfo,
)

internal fun torrentCompletionKey(profileId: String, torrentHash: String): String {
    return "${profileId.trim()}|${torrentHash.trim().lowercase()}"
}

internal fun completionStateToken(torrent: TorrentInfo): String {
    val state = torrent.state.trim().lowercase()
    return if (isCompletedTorrentState(state, torrent.progress)) "completed" else state
}

internal fun findCompletedTorrentTransitions(
    profileId: String,
    previousStates: Map<String, String>,
    torrents: List<TorrentInfo>,
): List<CompletedTorrentTransition> {
    return torrents.mapNotNull { torrent ->
        val hash = torrent.hash.trim()
        if (hash.isBlank()) return@mapNotNull null
        val key = torrentCompletionKey(profileId, hash)
        val previous = previousStates[key]?.trim()?.lowercase() ?: return@mapNotNull null
        val current = completionStateToken(torrent)
        if (isDownloadingTorrentState(previous) && current == "completed") {
            CompletedTorrentTransition(key, torrent)
        } else {
            null
        }
    }
}

internal fun mergeCompletionStates(
    profileId: String,
    previousStates: Map<String, String>,
    torrents: List<TorrentInfo>,
): Map<String, String> {
    val prefix = "${profileId.trim()}|"
    val current = torrents.mapNotNull { torrent ->
        torrent.hash.trim().takeIf(String::isNotBlank)?.let { hash ->
            torrentCompletionKey(profileId, hash) to completionStateToken(torrent)
        }
    }.toMap()
    return previousStates.filterKeys { !it.startsWith(prefix) } + current
}

internal fun mergeProfileScopedCompletionStates(
    persisted: Map<String, String>,
    profileId: String,
    states: Map<String, String>,
): Map<String, String>? {
    val prefix = "$profileId|"
    val updated = persisted.filterKeys { !it.startsWith(prefix) } +
        states.filterKeys { it.startsWith(prefix) }
    if (updated == persisted) return null
    return updated
}

private fun isDownloadingTorrentState(state: String): Boolean {
    return state in setOf("downloading", "forceddl", "stalldl", "stalleddl", "queueddl")
}

private fun isCompletedTorrentState(state: String, progress: Float): Boolean {
    return state in setOf("uploading", "forcedup", "stalledup", "queuedup", "pausedup", "seeding", "seedpending", "seedwait") ||
        (progress >= 1f && state !in setOf("checking", "checkingup", "checkingdl", "moving", "error", "missingfiles"))
}
