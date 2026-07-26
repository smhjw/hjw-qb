package com.hjw.qbremote.ui

import com.hjw.qbremote.data.ScheduleDayPreset
import com.hjw.qbremote.data.GlobalSpeedLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSpeedLimitSupportTest {
    @Test
    fun globalSpeedLimitInput_acceptsUnlimitedAndTransmissionMaximum() {
        assertTrue(isValidGlobalSpeedLimitKbInput(""))
        assertTrue(isValidGlobalSpeedLimitKbInput("0"))
        assertTrue(isValidGlobalSpeedLimitKbInput(Int.MAX_VALUE.toString()))
        assertFalse(isValidGlobalSpeedLimitKbInput("-1"))
        assertFalse(isValidGlobalSpeedLimitKbInput("2147483648"))
        assertFalse(isValidGlobalSpeedLimitKbInput("fast"))
    }

    @Test
    fun buildGlobalSpeedLimitsOrNull_mapsBlankToUnlimitedAndKeepsScheduler() {
        val limits = buildGlobalSpeedLimitsOrNull(
            downloadLimitKb = "",
            uploadLimitKb = "2048",
            alternativeDownloadLimitKb = "0",
            alternativeUploadLimitKb = "3072",
            schedulerEnabled = true,
            scheduleStartHour = "8",
            scheduleStartMinute = "05",
            scheduleEndHour = "20",
            scheduleEndMinute = "34",
            scheduleDayPreset = ScheduleDayPreset.MONDAY,
        )

        assertEquals(0L, limits?.downloadLimitKb)
        assertEquals(2048L, limits?.uploadLimitKb)
        assertEquals(0L, limits?.alternativeDownloadLimitKb)
        assertEquals(3072L, limits?.alternativeUploadLimitKb)
        assertTrue(limits?.schedulerEnabled == true)
        assertEquals(485, limits?.scheduleStartMinutes)
        assertEquals(1234, limits?.scheduleEndMinutes)
        assertEquals(ScheduleDayPreset.MONDAY, limits?.scheduleDayPreset)
    }

    @Test
    fun scheduleTimeInput_acceptsBoundariesAndRejectsInvalidValues() {
        assertTrue(isValidScheduleHourInput("0"))
        assertTrue(isValidScheduleHourInput("23"))
        assertFalse(isValidScheduleHourInput("24"))
        assertTrue(isValidScheduleMinuteInput("0"))
        assertTrue(isValidScheduleMinuteInput("59"))
        assertFalse(isValidScheduleMinuteInput("60"))
        assertFalse(isValidScheduleMinuteInput(""))
    }

    @Test
    fun disabledScheduler_preservesOriginalServerSchedule() {
        val limits = buildGlobalSpeedLimitsOrNull(
            downloadLimitKb = "0",
            uploadLimitKb = "0",
            alternativeDownloadLimitKb = "0",
            alternativeUploadLimitKb = "0",
            schedulerEnabled = false,
            scheduleStartHour = "bad",
            scheduleStartMinute = "bad",
            scheduleEndHour = "bad",
            scheduleEndMinute = "bad",
            originalScheduleStartMinutes = 125,
            originalScheduleEndMinutes = 1439,
            originalScheduleDayPreset = ScheduleDayPreset.WEEKENDS,
        )

        assertEquals(125, limits?.scheduleStartMinutes)
        assertEquals(1439, limits?.scheduleEndMinutes)
        assertEquals(ScheduleDayPreset.WEEKENDS, limits?.scheduleDayPreset)
    }

    @Test
    fun buildGlobalSpeedLimitsOrNull_rejectsInvalidField() {
        assertNull(
            buildGlobalSpeedLimitsOrNull(
                downloadLimitKb = "none",
                uploadLimitKb = "0",
                alternativeDownloadLimitKb = "0",
                alternativeUploadLimitKb = "0",
                schedulerEnabled = false,
            ),
        )
    }

    @Test
    fun configuredLimitReadback_ignoresCurrentModeButRejectsChangedUploadLimit() {
        val requested = GlobalSpeedLimits(uploadLimitKb = 2048, alternativeModeEnabled = false)

        assertTrue(
            requested.hasSameConfiguredLimits(
                requested.copy(alternativeModeEnabled = true),
            ),
        )
        assertFalse(
            requested.hasSameConfiguredLimits(
                requested.copy(uploadLimitKb = 1024),
            ),
        )
    }
}
