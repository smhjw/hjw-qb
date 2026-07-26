package com.hjw.qbremote.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDayPresetTest {
    @Test
    fun `qb scheduler days round trip for every preset`() {
        ScheduleDayPreset.entries.forEach { preset ->
            assertEquals(preset, ScheduleDayPreset.fromQbSchedulerDays(preset.qbSchedulerDays))
        }
    }

    @Test
    fun `out of range qb scheduler days fall back to every day`() {
        assertEquals(ScheduleDayPreset.EVERY_DAY, ScheduleDayPreset.fromQbSchedulerDays(-1))
        assertEquals(ScheduleDayPreset.EVERY_DAY, ScheduleDayPreset.fromQbSchedulerDays(99))
    }

    @Test
    fun `transmission day mask round trips for every preset`() {
        ScheduleDayPreset.entries.forEach { preset ->
            assertEquals(preset, ScheduleDayPreset.fromTransmissionDayMask(preset.transmissionDayMask))
        }
    }

    @Test
    fun `unknown transmission day mask falls back to every day`() {
        assertEquals(ScheduleDayPreset.EVERY_DAY, ScheduleDayPreset.fromTransmissionDayMask(0x00))
        assertEquals(ScheduleDayPreset.EVERY_DAY, ScheduleDayPreset.fromTransmissionDayMask(0x33))
    }
}
