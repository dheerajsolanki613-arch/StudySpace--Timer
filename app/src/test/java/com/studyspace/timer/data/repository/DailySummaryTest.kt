package com.studyspace.timer.data.repository

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class DailySummaryTest {
    // A fixed "today" and zone so nothing here depends on the real clock or the machine's timezone.
    private val today = LocalDate.of(2026, 9, 21)
    private val utc = ZoneId.of("UTC")
    private val fiveHours = 5 * 60 * 60_000L

    private fun session(date: LocalDate, minutes: Long, subjectId: Long? = null) = StudySessionEntity(
        type = SessionType.SELF_STUDY.name,
        label = "Study",
        startEpochMillis = 0L,
        durationMillis = minutes * 60_000L,
        completedNaturally = false,
        dateEpochDay = date.toEpochDay(),
        subjectId = subjectId
    )

    private fun task(completed: Boolean, completedAtMillis: Long?) = TaskEntity(
        title = "Task",
        priority = TaskPriority.MEDIUM.name,
        completed = completed,
        createdAtEpochMillis = 0L,
        completedAtEpochMillis = completedAtMillis
    )

    private fun millisAt(date: LocalDate, hour: Int, minute: Int, zone: ZoneId) =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun summary(
        sessions: List<StudySessionEntity> = emptyList(),
        tasks: List<TaskEntity> = emptyList(),
        date: LocalDate = today,
        zone: ZoneId = utc
    ) = computeDailySummary(sessions, tasks, fiveHours, date, zone)

    // --- empty / bucketing ---

    @Test
    fun `empty input is all zeros with no activity and no divide-by-zero`() {
        val result = summary()
        assertEquals(0L, result.totalMillis)
        assertEquals(0, result.sessionCount)
        assertEquals(0, result.subjectCount)
        assertNull(result.topSubject)
        assertEquals(0, result.tasksCompleted)
        assertEquals(0, result.goal.percent)
        assertFalse(result.goal.isComplete)
        assertFalse(result.hasActivity)
    }

    @Test
    fun `only the selected day's sessions are counted`() {
        val result = summary(
            sessions = listOf(
                session(today, 30),
                session(today, 45),
                session(today.minusDays(1), 120), // yesterday, excluded
                session(today.plusDays(1), 60)    // a later day fetched by the open-ended query, excluded
            )
        )
        assertEquals(75 * 60_000L, result.totalMillis)
        assertEquals(2, result.sessionCount)
        assertTrue(result.hasActivity)
    }

    @Test
    fun `a past day summarizes that day, not today`() {
        val yesterday = today.minusDays(1)
        val result = summary(
            sessions = listOf(session(today, 10), session(yesterday, 90)),
            date = yesterday
        )
        assertEquals(yesterday, result.date)
        assertEquals(90 * 60_000L, result.totalMillis)
        assertEquals(1, result.sessionCount)
    }

    // --- subjects ---

    @Test
    fun `subject count is distinct attributed subjects and ignores unattributed sessions`() {
        val result = summary(
            sessions = listOf(
                session(today, 20, subjectId = 1),
                session(today, 20, subjectId = 1),
                session(today, 20, subjectId = 2),
                session(today, 20, subjectId = null)
            )
        )
        assertEquals(2, result.subjectCount)
        // Unattributed time is still real study time and still counts toward the totals.
        assertEquals(4, result.sessionCount)
        assertEquals(80 * 60_000L, result.totalMillis)
    }

    @Test
    fun `top subject is ranked by total time, not by session count`() {
        val result = summary(
            sessions = listOf(
                session(today, 5, subjectId = 1),
                session(today, 5, subjectId = 1),
                session(today, 5, subjectId = 1),
                session(today, 120, subjectId = 2)
            )
        )
        assertEquals(2L, result.topSubject?.subjectId)
        assertEquals(120 * 60_000L, result.topSubject?.totalMillis)
    }

    @Test
    fun `top subject is never the unattributed bucket even when it has the most time`() {
        val result = summary(
            sessions = listOf(
                session(today, 300, subjectId = null),
                session(today, 10, subjectId = 7)
            )
        )
        assertEquals(7L, result.topSubject?.subjectId)
    }

    @Test
    fun `top subject is null when no session has a subject`() {
        val result = summary(sessions = listOf(session(today, 30), session(today, 30)))
        assertNull(result.topSubject)
        assertEquals(0, result.subjectCount)
    }

    @Test
    fun `equal-time subjects tie-break by id regardless of input order`() {
        val a = summary(sessions = listOf(session(today, 30, subjectId = 9), session(today, 30, subjectId = 3)))
        val b = summary(sessions = listOf(session(today, 30, subjectId = 3), session(today, 30, subjectId = 9)))
        assertEquals(3L, a.topSubject?.subjectId)
        assertEquals(3L, b.topSubject?.subjectId)
    }

    // --- goal ---

    @Test
    fun `goal progress uses the day's total against the goal`() {
        // 3h 42m of a 5h goal = 74% (matches the spec's own example), 1h 18m remaining.
        val result = summary(sessions = listOf(session(today, 3 * 60 + 42)))
        assertEquals(74, result.goal.percent)
        assertEquals(78 * 60_000L, result.goal.remainingMillis)
        assertFalse(result.goal.isComplete)
    }

    @Test
    fun `goal is complete when the day's total reaches the goal`() {
        val result = summary(sessions = listOf(session(today, 5 * 60)))
        assertTrue(result.goal.isComplete)
        assertEquals(100, result.goal.percent)
        assertEquals(0L, result.goal.remainingMillis)
    }

    // --- tasks completed ---

    @Test
    fun `tasks completed counts only tasks completed on that day`() {
        val result = summary(
            tasks = listOf(
                task(completed = true, completedAtMillis = millisAt(today, 9, 0, utc)),
                task(completed = true, completedAtMillis = millisAt(today, 23, 59, utc)),
                task(completed = true, completedAtMillis = millisAt(today.minusDays(1), 12, 0, utc)), // yesterday
                task(completed = false, completedAtMillis = null)                                     // still open
            )
        )
        assertEquals(2, result.tasksCompleted)
        assertTrue("completed tasks alone are activity", summary(tasks = listOf(task(true, millisAt(today, 9, 0, utc)))).hasActivity)
    }

    @Test
    fun `a task completed before completion times were recorded is never counted on any day`() {
        val legacy = task(completed = true, completedAtMillis = null)
        assertEquals(0, summary(tasks = listOf(legacy)).tasksCompleted)
        assertEquals(0, summary(tasks = listOf(legacy), date = today.minusDays(1)).tasksCompleted)
    }

    @Test
    fun `an open task with a stale timestamp is not counted`() {
        val stale = task(completed = false, completedAtMillis = millisAt(today, 10, 0, utc))
        assertEquals(0, summary(tasks = listOf(stale)).tasksCompleted)
    }

    @Test
    fun `completion time is placed on the day in the given zone, not UTC`() {
        val kolkata = ZoneId.of("Asia/Kolkata") // UTC+05:30
        // 00:30 local on `today` is still the previous UTC calendar day (19:00 UTC).
        val earlyMorning = task(completed = true, completedAtMillis = millisAt(today, 0, 30, kolkata))
        assertEquals(1, summary(tasks = listOf(earlyMorning), zone = kolkata).tasksCompleted)
        assertEquals(0, summary(tasks = listOf(earlyMorning), date = today.minusDays(1), zone = kolkata).tasksCompleted)
        // The same instant, read in UTC, belongs to the previous day.
        assertEquals(1, summary(tasks = listOf(earlyMorning), date = today.minusDays(1), zone = utc).tasksCompleted)
    }

    // --- date label ---

    @Test
    fun `date label says Today and Yesterday`() {
        assertEquals("Today", dailySummaryDateLabel(today, today, Locale.US))
        assertEquals("Yesterday", dailySummaryDateLabel(today.minusDays(1), today, Locale.US))
    }

    @Test
    fun `date label for an older day in the same year has no year`() {
        assertEquals("Monday, Sep 14", dailySummaryDateLabel(LocalDate.of(2026, 9, 14), today, Locale.US))
    }

    @Test
    fun `date label for a day in a different year includes the year`() {
        assertEquals("Wed, Dec 31, 2025", dailySummaryDateLabel(LocalDate.of(2025, 12, 31), today, Locale.US))
    }
}
