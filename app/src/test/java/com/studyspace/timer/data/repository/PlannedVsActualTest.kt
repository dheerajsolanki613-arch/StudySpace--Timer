package com.studyspace.timer.data.repository

import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PlannedVsActualTest {
    // An arbitrary fixed "today" so the test doesn't depend on the real clock.
    private val today = LocalDate.of(2026, 1, 15)
    private val todayEpochDay = today.toEpochDay()

    private fun plan(
        dateEpochDay: Long,
        durationMinutes: Int,
        status: PlannedSessionStatus = PlannedSessionStatus.PLANNED
    ) = PlannedSessionEntity(
        dateEpochDay = dateEpochDay,
        startMinuteOfDay = 9 * 60,
        durationMinutes = durationMinutes,
        status = status.name,
        createdAtEpochMillis = 0L
    )

    private fun session(dateEpochDay: Long, durationMillis: Long) = StudySessionEntity(
        type = SessionType.SELF_STUDY.name,
        label = "Study",
        startEpochMillis = 0L,
        durationMillis = durationMillis,
        completedNaturally = true,
        dateEpochDay = dateEpochDay
    )

    @Test
    fun `empty input is all zeros with zero percent, no divide-by-zero`() {
        val result = computePlannedVsActual(emptyList(), emptyList(), today)
        assertEquals(0L, result.today.plannedMillis)
        assertEquals(0L, result.today.actualMillis)
        assertEquals(0, result.today.completionPercent)
    }

    @Test
    fun `today bucket only counts today's rows`() {
        val planned = listOf(
            plan(todayEpochDay, durationMinutes = 60),
            plan(todayEpochDay - 1, durationMinutes = 90) // yesterday, excluded from "today"
        )
        val actual = listOf(
            session(todayEpochDay, 30 * 60_000L),
            session(todayEpochDay - 1, 45 * 60_000L)
        )
        val result = computePlannedVsActual(planned, actual, today)
        assertEquals(60 * 60_000L, result.today.plannedMillis)
        assertEquals(30 * 60_000L, result.today.actualMillis)
        assertEquals(50, result.today.completionPercent)
    }

    @Test
    fun `cancelled plans are excluded from planned totals`() {
        val planned = listOf(
            plan(todayEpochDay, durationMinutes = 60, status = PlannedSessionStatus.CANCELLED),
            plan(todayEpochDay, durationMinutes = 30, status = PlannedSessionStatus.COMPLETED)
        )
        val result = computePlannedVsActual(planned, emptyList(), today)
        assertEquals(30 * 60_000L, result.today.plannedMillis)
    }

    @Test
    fun `plans dated after today are excluded from every bucket`() {
        val planned = listOf(plan(todayEpochDay + 3, durationMinutes = 60))
        val result = computePlannedVsActual(planned, emptyList(), today)
        assertEquals(0L, result.today.plannedMillis)
        assertEquals(0L, result.week.plannedMillis)
        assertEquals(0L, result.month.plannedMillis)
    }

    @Test
    fun `week bucket includes the last 7 days including today`() {
        val planned = listOf(
            plan(todayEpochDay, durationMinutes = 60),
            plan(todayEpochDay - 6, durationMinutes = 60), // exactly 7 days ago, included
            plan(todayEpochDay - 7, durationMinutes = 60)  // 8 days ago, excluded
        )
        val result = computePlannedVsActual(planned, emptyList(), today)
        assertEquals(120 * 60_000L, result.week.plannedMillis)
    }

    @Test
    fun `completion over 100 percent is not clamped`() {
        val planned = listOf(plan(todayEpochDay, durationMinutes = 30))
        val actual = listOf(session(todayEpochDay, 60 * 60_000L))
        val result = computePlannedVsActual(planned, actual, today)
        assertEquals(200, result.today.completionPercent)
    }
}
