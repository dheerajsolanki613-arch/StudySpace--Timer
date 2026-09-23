package com.studyspace.timer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskDueInfoTest {
    private val today = 20_000L // arbitrary epoch day, only relative offsets matter

    @Test
    fun `no deadline is urgency NONE with a null label`() {
        val info = computeTaskDueInfo(null, today)
        assertEquals(TaskDueUrgency.NONE, info.urgency)
        assertNull(info.label)
    }

    @Test
    fun `a past deadline is overdue`() {
        val info = computeTaskDueInfo(today - 3, today)
        assertEquals(TaskDueUrgency.OVERDUE, info.urgency)
        assertEquals("Overdue", info.label)
    }

    @Test
    fun `today's deadline is due today`() {
        val info = computeTaskDueInfo(today, today)
        assertEquals(TaskDueUrgency.DUE_TODAY, info.urgency)
        assertEquals("Due today", info.label)
    }

    @Test
    fun `tomorrow's deadline has its own label`() {
        val info = computeTaskDueInfo(today + 1, today)
        assertEquals(TaskDueUrgency.UPCOMING, info.urgency)
        assertEquals("Due tomorrow", info.label)
    }

    @Test
    fun `a deadline further out counts the days`() {
        val info = computeTaskDueInfo(today + 5, today)
        assertEquals(TaskDueUrgency.UPCOMING, info.urgency)
        assertEquals("Due in 5 days", info.label)
    }
}
