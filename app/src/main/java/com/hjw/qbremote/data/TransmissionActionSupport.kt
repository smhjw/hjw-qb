package com.hjw.qbremote.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hjw.qbremote.data.model.AddTorrentRequest

private val transmissionActionGson = Gson()

internal fun buildJsonObject(vararg pairs: Pair<String, Any?>): JsonObject {
    val result = JsonObject()
    for ((key, value) in pairs) {
        if (value == null) continue
        result.add(key, transmissionActionGson.toJsonTree(value))
    }
    return result
}

internal fun resolveTransmissionRenamePath(currentTorrentName: String): String {
    return currentTorrentName.trim()
        .takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Current torrent name is missing.")
}

internal fun buildTransmissionAddTorrentArguments(
    request: AddTorrentRequest,
    common: MutableMap<String, Any?>,
): JsonObject {
    val labels = request.tags
        .split(',', ';', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    if (request.savePath.isNotBlank()) {
        common["download-dir"] = request.savePath.trim()
    }
    if (request.paused) {
        common["paused"] = true
    }
    if (labels.isNotEmpty()) {
        common["labels"] = labels
    }
    if (request.uploadLimitBytes >= 0L) {
        val uploadLimitKb = transmissionLimitKilobytes(request.uploadLimitBytes)
        common["uploadLimited"] = uploadLimitKb > 0
        common["uploadLimit"] = uploadLimitKb
    }
    if (request.downloadLimitBytes >= 0L) {
        val downloadLimitKb = transmissionLimitKilobytes(request.downloadLimitBytes)
        common["downloadLimited"] = downloadLimitKb > 0
        common["downloadLimit"] = downloadLimitKb
    }
    return transmissionActionGson.toJsonTree(common).asJsonObject
}

private fun transmissionLimitKilobytes(bytes: Long): Int {
    return (bytes / 1024L).toInt().coerceAtLeast(0)
}
