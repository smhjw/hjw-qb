package com.hjw.qbremote.ui

import com.hjw.qbremote.data.GlobalSpeedLimits
import com.hjw.qbremote.data.ScheduleDayPreset

internal fun parseLimitKbToBytes(value: String): Long {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return -1L
    val kb = trimmed.toLongOrNull() ?: throw IllegalArgumentException("限速值必须是数字")
    if (kb < 0L) return -1L
    return kb * 1024L
}

internal fun isValidGlobalSpeedLimitKbInput(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isBlank()) return true
    val parsed = normalized.toLongOrNull() ?: return false
    return parsed in 0L..Int.MAX_VALUE.toLong()
}

internal fun isValidScheduleHourInput(value: String): Boolean =
    value.trim().toIntOrNull() in 0..23

internal fun isValidScheduleMinuteInput(value: String): Boolean =
    value.trim().toIntOrNull() in 0..59

internal fun GlobalSpeedLimits.hasSameConfiguredLimits(other: GlobalSpeedLimits): Boolean {
    return downloadLimitKb == other.downloadLimitKb &&
        uploadLimitKb == other.uploadLimitKb &&
        alternativeDownloadLimitKb == other.alternativeDownloadLimitKb &&
        alternativeUploadLimitKb == other.alternativeUploadLimitKb &&
        schedulerEnabled == other.schedulerEnabled &&
        scheduleStartMinutes == other.scheduleStartMinutes &&
        scheduleEndMinutes == other.scheduleEndMinutes &&
        scheduleDayPreset == other.scheduleDayPreset
}

internal fun buildGlobalSpeedLimitsOrNull(
    downloadLimitKb: String,
    uploadLimitKb: String,
    alternativeDownloadLimitKb: String,
    alternativeUploadLimitKb: String,
    schedulerEnabled: Boolean,
    scheduleStartHour: String = "0",
    scheduleStartMinute: String = "0",
    scheduleEndHour: String = "0",
    scheduleEndMinute: String = "0",
    scheduleDayPreset: ScheduleDayPreset = ScheduleDayPreset.EVERY_DAY,
    originalScheduleStartMinutes: Int = 0,
    originalScheduleEndMinutes: Int = 0,
    originalScheduleDayPreset: ScheduleDayPreset = ScheduleDayPreset.EVERY_DAY,
): GlobalSpeedLimits? {
    val values = listOf(
        downloadLimitKb,
        uploadLimitKb,
        alternativeDownloadLimitKb,
        alternativeUploadLimitKb,
    )
    if (values.any { !isValidGlobalSpeedLimitKbInput(it) }) return null
    if (schedulerEnabled && (
            !isValidScheduleHourInput(scheduleStartHour) ||
                !isValidScheduleMinuteInput(scheduleStartMinute) ||
                !isValidScheduleHourInput(scheduleEndHour) ||
                !isValidScheduleMinuteInput(scheduleEndMinute)
            )
    ) {
        return null
    }
    fun String.asLimit(): Long = trim().toLongOrNull() ?: 0L
    fun scheduleMinutes(hour: String, minute: String): Int =
        hour.trim().toInt() * 60 + minute.trim().toInt()
    return GlobalSpeedLimits(
        downloadLimitKb = downloadLimitKb.asLimit(),
        uploadLimitKb = uploadLimitKb.asLimit(),
        alternativeDownloadLimitKb = alternativeDownloadLimitKb.asLimit(),
        alternativeUploadLimitKb = alternativeUploadLimitKb.asLimit(),
        schedulerEnabled = schedulerEnabled,
        scheduleStartMinutes = if (schedulerEnabled) {
            scheduleMinutes(scheduleStartHour, scheduleStartMinute)
        } else {
            originalScheduleStartMinutes
        },
        scheduleEndMinutes = if (schedulerEnabled) {
            scheduleMinutes(scheduleEndHour, scheduleEndMinute)
        } else {
            originalScheduleEndMinutes
        },
        scheduleDayPreset = if (schedulerEnabled) scheduleDayPreset else originalScheduleDayPreset,
    )
}
