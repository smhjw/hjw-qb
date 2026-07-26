package com.hjw.qbremote.ui

internal data class SharedTorrentInput(
    val id: Long,
    val urls: String,
)

// Bounds against malicious ACTION_SEND payloads: cap total scanned input,
// per-URL length (magnets with many trackers are typically 2-4KB), and URL count.
private const val MAX_SHARED_INPUT_LENGTH = 64_000
private const val MAX_SHARED_URL_LENGTH = 8_192
private const val MAX_SHARED_URL_COUNT = 50

private val sharedTorrentUrlPattern = Regex(
    pattern = "(?i)(magnet:\\?[^\\s]+|https?://[^\\s]+)",
)

internal fun normalizeSharedTorrentInput(raw: String): String {
    val bounded = raw.take(MAX_SHARED_INPUT_LENGTH)
    return sharedTorrentUrlPattern
        .findAll(bounded)
        .map { match ->
            match.value.trim().trimEnd('.', ',', ';', ')', ']', '}', '，', '。', '；')
        }
        .filter { value -> value.isNotBlank() }
        .filter { value -> value.length <= MAX_SHARED_URL_LENGTH }
        .distinct()
        .take(MAX_SHARED_URL_COUNT)
        .joinToString("\n")
}

internal fun mergeSharedTorrentInputs(existing: String, incoming: String): String {
    return sequenceOf(existing, incoming)
        .flatMap { value -> value.lineSequence() }
        .map { value -> value.trim() }
        .filter { value -> value.isNotBlank() }
        .distinct()
        .take(MAX_SHARED_URL_COUNT)
        .joinToString("\n")
}
