package com.hjw.qbremote.data

enum class ScheduleDayPreset {
    EVERY_DAY,
    WEEKDAYS,
    WEEKENDS,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ;

    val qbSchedulerDays: Int
        get() = ordinal

    val transmissionDayMask: Int
        get() = when (this) {
            EVERY_DAY -> 0x7F
            WEEKDAYS -> 0x3E
            WEEKENDS -> 0x41
            MONDAY -> 0x02
            TUESDAY -> 0x04
            WEDNESDAY -> 0x08
            THURSDAY -> 0x10
            FRIDAY -> 0x20
            SATURDAY -> 0x40
            SUNDAY -> 0x01
        }

    companion object {
        fun fromQbSchedulerDays(value: Int): ScheduleDayPreset =
            entries.getOrElse(value) { EVERY_DAY }

        fun fromTransmissionDayMask(mask: Int): ScheduleDayPreset =
            entries.firstOrNull { it != EVERY_DAY && it.transmissionDayMask == mask } ?: EVERY_DAY
    }
}

data class GlobalSpeedLimits(
    val downloadLimitKb: Long = 0L,
    val uploadLimitKb: Long = 0L,
    val alternativeDownloadLimitKb: Long = 0L,
    val alternativeUploadLimitKb: Long = 0L,
    val schedulerEnabled: Boolean = false,
    val scheduleStartMinutes: Int = 0,
    val scheduleEndMinutes: Int = 0,
    val scheduleDayPreset: ScheduleDayPreset = ScheduleDayPreset.EVERY_DAY,
    val alternativeModeEnabled: Boolean? = null,
)
