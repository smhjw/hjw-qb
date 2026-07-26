package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.CountryUploadRecord
import java.text.SimpleDateFormat
import java.net.URI
import java.net.URLEncoder
import java.util.Date
import java.util.Locale

fun formatBytes(value: Long): String {
    if (value <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var idx = 0
    while (size >= 1024 && idx < units.lastIndex) {
        size /= 1024.0
        idx++
    }
    return String.format(Locale.US, "%.2f %s", size, units[idx])
}

fun formatSpeed(value: Long): String = "${formatBytes(value)}/s"

fun formatPercent(progress: Float): String {
    val pct = (progress * 100f).coerceIn(0f, 100f)
    return String.format(Locale.US, "%.2f%%", pct)
}

fun formatRatio(value: Double): String {
    if (!value.isFinite() || value < 0.0) return "-"
    return String.format(Locale.US, "%.2f", value)
}

fun formatAddedOn(seconds: Long): String {
    if (seconds <= 0L) return "-"
    val date = Date(seconds * 1000L)
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
}

fun formatActiveAgo(lastActivitySeconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (lastActivitySeconds <= 0L) return "--"
    val elapsedSeconds = ((nowMillis / 1000L) - lastActivitySeconds).coerceAtLeast(0L)
    if (elapsedSeconds == 0L) return "0s"
    if (elapsedSeconds < 60L) return "${elapsedSeconds}s"

    val minutes = elapsedSeconds / 60L
    val seconds = elapsedSeconds % 60L
    if (minutes < 60L) return "${minutes}m ${seconds}s"

    val hours = minutes / 60L
    val remainMinutes = minutes % 60L
    if (hours < 24L) return "${hours}h ${remainMinutes}m"

    val days = hours / 24L
    val remainHours = hours % 24L
    return "${days}d ${remainHours}h"
}

fun normalizeCountryCodeForDisplay(countryCode: String): String {
    return when (countryCode.trim().uppercase(Locale.US)) {
        "TW" -> "CN"
        else -> countryCode.trim().uppercase(Locale.US)
    }
}

fun mergeCountryUploadRecordsForDisplay(countries: List<CountryUploadRecord>): List<CountryUploadRecord> {
    if (countries.isEmpty()) return emptyList()
    return countries
        .groupBy { normalizeCountryCodeForDisplay(it.countryCode) }
        .mapNotNull { (countryCode, records) ->
            if (countryCode.isBlank()) return@mapNotNull null
            CountryUploadRecord(
                countryCode = countryCode,
                countryName = records.firstNotNullOfOrNull { it.countryName.trim().takeIf(String::isNotBlank) }.orEmpty(),
                uploadedBytes = records.sumOf { it.uploadedBytes.coerceAtLeast(0L) },
            )
        }
        .filter { it.uploadedBytes > 0L }
        .sortedByDescending { it.uploadedBytes }
}

fun localizedCountryNameForDisplay(
    countryCode: String,
    fallbackName: String = "",
    locale: Locale = Locale.getDefault(),
): String {
    val normalizedCode = normalizeCountryCodeForDisplay(countryCode)
    if (normalizedCode.isBlank()) return fallbackName.ifBlank { countryCode.trim() }
    if (normalizedCode == "CN") {
        return if (locale.language.startsWith("zh")) "中国" else "China"
    }
    val localized = runCatching {
        Locale("", normalizedCode).getDisplayCountry(locale).trim()
    }.getOrDefault("")
    return when {
        localized.isNotBlank() && !localized.equals(normalizedCode, ignoreCase = true) -> localized
        fallbackName.isNotBlank() -> fallbackName
        else -> normalizedCode
    }
}

fun compactCountryLabelForDisplay(
    countryCode: String,
    fallbackName: String = "",
    locale: Locale = Locale.getDefault(),
): String {
    val normalizedCode = normalizeCountryCodeForDisplay(countryCode)
    val localizedName = localizedCountryNameForDisplay(
        countryCode = normalizedCode,
        fallbackName = fallbackName,
        locale = locale,
    )
    return if (
        locale.language == "en" &&
        localizedName.length > 10 &&
        localizedName.all { it.isLetter() || it == ' ' || it == '-' }
    ) {
        normalizedCode
    } else {
        localizedName
    }
}

fun buildMagnetUri(
    hash: String,
    name: String,
    trackerUrls: List<String> = emptyList(),
): String {
    val normalizedHash = hash.trim()
    if (normalizedHash.isBlank()) return ""

    val builder = StringBuilder("magnet:?xt=urn:btih:")
    builder.append(normalizedHash)

    val normalizedName = name.trim()
    if (normalizedName.isNotBlank()) {
        builder.append("&dn=")
        builder.append(urlEncodeMagnetComponent(normalizedName))
    }

    trackerUrls
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .forEach { trackerUrl ->
            builder.append("&tr=")
            builder.append(urlEncodeMagnetComponent(trackerUrl))
        }

    return builder.toString()
}

fun buildTorrentExportFileName(
    torrentName: String,
    hash: String,
): String {
    val baseName = torrentName
        .trim()
        .ifBlank { hash.trim().ifBlank { "torrent" } }
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .trim('.')
        .ifBlank { "torrent" }
    return if (baseName.endsWith(".torrent", ignoreCase = true)) {
        baseName
    } else {
        "$baseName.torrent"
    }
}

fun formatTrackerSiteName(
    tracker: String,
    unknownLabel: String,
): String {
    val host = parseTrackerHost(tracker) ?: return unknownLabel
    val normalized = host
        .trim()
        .lowercase(Locale.US)
        .split('.')
        .filter { it.isNotBlank() }
        .dropWhile { it in TRACKER_HOST_PREFIXES }
    if (normalized.isEmpty()) return unknownLabel
    if (normalized.size == 1) return normalized.first()

    val candidate = when {
        normalized.size >= 3 &&
            normalized.last().length == 2 &&
            normalized[normalized.lastIndex - 1] in DOUBLE_SUFFIX_MARKERS -> {
            normalized[normalized.lastIndex - 2]
        }

        else -> normalized[normalized.lastIndex - 1]
    }
    return candidate.ifBlank { normalized.firstOrNull().orEmpty().ifBlank { unknownLabel } }
}

private val TRACKER_HOST_PREFIXES = setOf("tracker", "announce", "www")
private val DOUBLE_SUFFIX_MARKERS = setOf("co", "com", "net", "org", "gov", "edu")
private val SENSITIVE_TRACKER_QUERY_KEYS = setOf("passkey", "authkey", "token", "key", "uid")

private fun parseTrackerHost(tracker: String): String? {
    val trimmed = tracker.trim()
    if (trimmed.isBlank()) return null
    val direct = runCatching { URI(trimmed).host.orEmpty().trim() }.getOrDefault("")
    if (direct.isNotBlank()) return direct

    val fallback = trimmed
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("udp://")
        .substringBefore('/')
        .substringBefore(':')
        .trim()
    return fallback.takeIf { it.isNotBlank() }
}

fun maskTrackerUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return "-"
    return runCatching {
        val uri = URI(trimmed)
        val scheme = uri.scheme?.takeIf { it.isNotBlank() }?.let { "$it://" }.orEmpty()
        val authority = uri.rawAuthority?.takeIf { it.isNotBlank() }
            ?: parseTrackerHost(trimmed).orEmpty()
        val path = maskTrackerPath(uri.rawPath.orEmpty())
        val query = maskTrackerQuery(uri.rawQuery.orEmpty())
        buildString {
            append(scheme)
            append(authority)
            append(path)
            if (query.isNotBlank()) {
                append('?')
                append(query)
            }
        }.ifBlank { trimmed }
    }.getOrElse {
        maskTrackerFallback(trimmed)
    }
}

fun isMutableTrackerUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return false
    val uppercase = trimmed.uppercase(Locale.US)
    if (
        uppercase.contains("[DHT]") ||
        uppercase.contains("[PEX]") ||
        uppercase.contains("[LSD]")
    ) {
        return false
    }
    return runCatching {
        val uri = URI(trimmed)
        !uri.scheme.isNullOrBlank() && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

private fun maskTrackerPath(path: String): String {
    if (path.isBlank()) return ""
    val maskedSegments = path
        .split('/')
        .map { segment ->
            when {
                segment.isBlank() -> segment
                shouldMaskTrackerPathSegment(segment) -> "******"
                else -> segment
            }
        }
    return maskedSegments.joinToString("/")
}

private fun maskTrackerQuery(query: String): String {
    if (query.isBlank()) return ""
    return query
        .split('&')
        .filter { it.isNotBlank() }
        .joinToString("&") { part ->
            val key = part.substringBefore('=').trim()
            val value = part.substringAfter('=', missingDelimiterValue = "")
            when {
                key.isBlank() -> part
                key.lowercase(Locale.US) in SENSITIVE_TRACKER_QUERY_KEYS -> "$key=******"
                shouldMaskTrackerPathSegment(value) -> "$key=******"
                else -> part
            }
        }
}

private fun shouldMaskTrackerPathSegment(segment: String): Boolean {
    val trimmed = segment.trim()
    if (trimmed.length < 14) return false
    if (trimmed.equals("announce", ignoreCase = true)) return false
    val alphaCount = trimmed.count(Char::isLetter)
    val digitCount = trimmed.count(Char::isDigit)
    return (alphaCount >= 4 && digitCount >= 2) || trimmed.contains('%') || trimmed.contains('_')
}

private fun maskTrackerFallback(url: String): String {
    val scheme = when {
        url.startsWith("https://", ignoreCase = true) -> "https://"
        url.startsWith("http://", ignoreCase = true) -> "http://"
        url.startsWith("udp://", ignoreCase = true) -> "udp://"
        else -> ""
    }
    val withoutScheme = url.removePrefix("https://").removePrefix("http://").removePrefix("udp://")
    val host = withoutScheme.substringBefore('/').substringBefore('?').trim()
    val pathAndQuery = withoutScheme.removePrefix(host)
    val maskedPathAndQuery = pathAndQuery
        .replace(Regex("([?&](?:passkey|authkey|token|key|uid)=)[^&]+", RegexOption.IGNORE_CASE), "$1******")
    return buildString {
        append(scheme)
        append(host)
        append(maskedPathAndQuery)
    }.ifBlank { url }
}

private fun urlEncodeMagnetComponent(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
