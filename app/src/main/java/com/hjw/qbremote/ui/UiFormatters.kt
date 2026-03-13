package com.hjw.qbremote.ui

import com.hjw.qbremote.data.model.CountryUploadRecord
import java.text.SimpleDateFormat
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

fun formatUploadAmountInMbOrGb(value: Long): String {
    val megabytes = value.coerceAtLeast(0L).toDouble() / 1024.0 / 1024.0
    return if (megabytes >= 1024.0) {
        String.format(Locale.US, "%.2fGB", megabytes / 1024.0)
    } else {
        String.format(Locale.US, "%.2fMB", megabytes)
    }
}

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
