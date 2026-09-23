package com.studyspace.timer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedSessionTimeTest {

    @Test
    fun `formats midnight and noon correctly`() {
        assertEquals("12:00 AM", formatMinuteOfDay(0))
        assertEquals("12:00 PM", formatMinuteOfDay(12 * 60))
    }

    @Test
    fun `formats a normal afternoon time`() {
        assertEquals("4:30 PM", formatMinuteOfDay(16 * 60 + 30))
    }

    @Test
    fun `formats a morning time with leading zero minutes`() {
        assertEquals("9:05 AM", formatMinuteOfDay(9 * 60 + 5))
    }

    @Test
    fun `time range within the same day has no day suffix`() {
        val range = formatPlannedTimeRange(startMinuteOfDay = 16 * 60, durationMinutes = 90)
        assertEquals("4:00 PM – 5:30 PM", range)
    }

    @Test
    fun `time range crossing midnight gets a next-day suffix`() {
        val range = formatPlannedTimeRange(startMinuteOfDay = 23 * 60, durationMinutes = 120)
        assertEquals("11:00 PM – 1:00 AM (+1d)", range)
    }

    @Test
    fun `duration formats hours and minutes`() {
        assertEquals("1h 30m", formatPlannedDuration(90))
        assertEquals("2h", formatPlannedDuration(120))
        assertEquals("45m", formatPlannedDuration(45))
    }
}
