package com.studyspace.timer.planning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class AvailabilityCodecTest {
    @Test
    fun `round trips a list of blocks`() {
        val blocks = listOf(
            AvailabilityBlock(DayOfWeek.MONDAY, 18 * 60, 21 * 60),
            AvailabilityBlock(DayOfWeek.SATURDAY, 9 * 60, 12 * 60)
        )
        val json = buildAvailabilityJson(blocks)
        assertEquals(blocks, parseAvailability(json))
    }

    @Test
    fun `an empty list round trips to an empty list`() {
        assertEquals(emptyList<AvailabilityBlock>(), parseAvailability(buildAvailabilityJson(emptyList())))
    }

    @Test
    fun `garbage text parses to an empty list rather than throwing`() {
        assertEquals(emptyList<AvailabilityBlock>(), parseAvailability("not json"))
    }

    @Test
    fun `an entry with an invalid time range is dropped, not the whole list`() {
        val json = """[
            {"dayOfWeek":"MONDAY","startMinuteOfDay":600,"endMinuteOfDay":500},
            {"dayOfWeek":"TUESDAY","startMinuteOfDay":600,"endMinuteOfDay":700}
        ]"""
        val result = parseAvailability(json)
        assertEquals(1, result.size)
        assertEquals(DayOfWeek.TUESDAY, result.single().dayOfWeek)
    }

    @Test
    fun `an entry with an unrecognised day name is dropped`() {
        val json = """[{"dayOfWeek":"FUNDAY","startMinuteOfDay":600,"endMinuteOfDay":700}]"""
        assertTrue(parseAvailability(json).isEmpty())
    }
}
