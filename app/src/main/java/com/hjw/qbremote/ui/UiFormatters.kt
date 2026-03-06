package com.hjw.qbremote.ui

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
