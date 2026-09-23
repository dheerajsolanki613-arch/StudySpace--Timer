package com.studyspace.timer.data.repository

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.db.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AdvancedAnalyticsTest {
    private val today = LocalDate.of(2026, 1, 15) // a Thursday
    private val todayEpochDay = today.toEpochDay()

    private fun session(
        dateEpochDay: Long,
        durationMillis: Long,
        type: SessionType = SessionType.SELF_STUDY,
        completedNaturally: Boolean = false,
        hour: Int = 10
    ): StudySessionEntity {
        val startMillis = LocalDateTime.of(LocalDate.ofEpochDay(dateEpochDay), java.time.LocalTime.of(hour, 0))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return StudySessionEntity(
            type = type.name,
            label = "Study",
            startEpochMillis = startMillis,
            durationMillis = durationMillis,
            completedNaturally = completedNaturally,
            dateEpochDay = dateEpochDay
        )
    }

    // --- sessionsInRange ---

    @Test
    fun `sessionsInRange for ALL_TIME returns everything unfiltered`() {
        val sessions = listOf(session(todayEpochDay - 200, 60_000L))
        val result = sessionsInRange(sessions, AnalyticsRange.ALL_TIME, today)
        assertEquals(1, result.size)
    }

    @Test
    fun `sessionsInRange for 7 days excludes an 8-day-old session`() {
        val sessions = listOf(
            session(todayEpochDay - 6, 60_000L), // included: exactly 7 days ago inclusive of today
            session(todayEpochDay - 7, 60_000L)  // excluded
        )
        val result = sessionsInRange(sessions, AnalyticsRange.LAST_7_DAYS, today)
        assertEquals(1, result.size)
        assertEquals(todayEpochDay - 6, result[0].dateEpochDay)
    }

    // --- computeAdvancedStats ---

    @Test
    fun `empty range gives all-zero defaults, no crash`() {
        val stats = computeAdvancedStats(emptyList(), AnalyticsRange.LAST_7_DAYS, today)
        assertEquals(0L, stats.totalMillis)
        assertEquals(0, stats.sessionCount)
        assertNull(stats.weeklyAverageMillis)
    }

    @Test
    fun `total, count, average and longest are computed correctly`() {
        val sessions = listOf(
            session(todayEpochDay, 30 * 60_000L),
            session(todayEpochDay, 90 * 60_000L)
        )
        val stats = computeAdvancedStats(sessions, AnalyticsRange.TODAY, today)
        assertEquals(120 * 60_000L, stats.totalMillis)
        assertEquals(2, stats.sessionCount)
        assertEquals(60 * 60_000L, stats.averageSessionMillis)
        assertEquals(90 * 60_000L, stats.longestSessionMillis)
    }

    @Test
    fun `weekly average is null for a range shorter than 7 days`() {
        val stats = computeAdvancedStats(listOf(session(todayEpochDay, 60_000L)), AnalyticsRange.TODAY, today)
        assertNull(stats.weeklyAverageMillis)
    }

    @Test
    fun `weekly average is populated for a 30-day range`() {
        val sessions = listOf(session(todayEpochDay, 30 * 24 * 60 * 60_000L / 30)) // arbitrary non-zero total
        val stats = computeAdvancedStats(sessions, AnalyticsRange.LAST_30_DAYS, today)
        assertTrue(stats.weeklyAverageMillis != null)
    }

    @Test
    fun `completed pomodoros only counts POMODORO sessions that completed naturally`() {
        val sessions = listOf(
            session(todayEpochDay, 25 * 60_000L, type = SessionType.POMODORO, completedNaturally = true),
            session(todayEpochDay, 10 * 60_000L, type = SessionType.POMODORO, completedNaturally = false),
            session(todayEpochDay, 25 * 60_000L, type = SessionType.NORMAL_TIMER, completedNaturally = true)
        )
        val stats = computeAdvancedStats(sessions, AnalyticsRange.TODAY, today)
        assertEquals(1, stats.completedPomodoros)
    }

    @Test
    fun `completion rate ignores stopwatch modes that can never complete naturally`() {
        val sessions = listOf(
            session(todayEpochDay, 60_000L, type = SessionType.SELF_STUDY, completedNaturally = false),
            session(todayEpochDay, 60_000L, type = SessionType.SELF_STUDY, completedNaturally = false),
            session(todayEpochDay, 60_000L, type = SessionType.NORMAL_TIMER, completedNaturally = true)
        )
        val stats = computeAdvancedStats(sessions, AnalyticsRange.TODAY, today)
        // Only the one NORMAL_TIMER session counts as target-based, and it completed naturally -> 100%.
        assertEquals(100, stats.completionRatePercent)
    }

    @Test
    fun `all-time daily average uses days since the earliest session, not a fixed window`() {
        val sessions = listOf(session(todayEpochDay - 1, 100 * 60_000L)) // one session, 2 days ago through today = span 2
        val stats = computeAdvancedStats(sessions, AnalyticsRange.ALL_TIME, today)
        assertEquals(50 * 60_000L, stats.dailyAverageMillis) // 100min / 2 days
    }

    // --- computeSubjectTotals ---

    @Test
    fun `subject totals group by subject including unattributed`() {
        val sessions = listOf(
            session(todayEpochDay, 60_000L).copy(subjectId = 1L),
            session(todayEpochDay, 30_000L).copy(subjectId = 1L),
            session(todayEpochDay, 45_000L).copy(subjectId = null)
        )
        val totals = computeSubjectTotals(sessions)
        assertEquals(2, totals.size)
        assertEquals(1L, totals[0].subjectId) // 90_000L, widest first
        assertEquals(90_000L, totals[0].totalMillis)
        assertNull(totals[1].subjectId)
    }

    // --- computeProductivityPatterns ---

    @Test
    fun `empty input yields all nulls and zeros, not a crash`() {
        val patterns = computeProductivityPatterns(emptyList(), AnalyticsRange.LAST_7_DAYS, today)
        assertNull(patterns.mostProductiveDayLabel)
        assertNull(patterns.mostProductiveHourRangeLabel)
        assertEquals(0, patterns.consistencyPercent)
    }

    @Test
    fun `most productive day is ranked by total duration, not session count`() {
        // Tuesday: five short sessions. Sunday: one long session that outweighs them.
        val tuesday = todayEpochDay - 2
        val sunday = todayEpochDay - 4
        val sessions = (1..5).map { session(tuesday, 5 * 60_000L) } +
            listOf(session(sunday, 180 * 60_000L))
        val patterns = computeProductivityPatterns(sessions, AnalyticsRange.LAST_7_DAYS, today)
        assertEquals(
            LocalDate.ofEpochDay(sunday).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
            patterns.mostProductiveDayLabel
        )
    }

    @Test
    fun `consistency percent is capped at 100 and reflects distinct study days`() {
        val sessions = listOf(session(todayEpochDay, 60_000L), session(todayEpochDay, 60_000L))
        val patterns = computeProductivityPatterns(sessions, AnalyticsRange.TODAY, today)
        assertEquals(100, patterns.consistencyPercent) // 1 distinct day / 1-day range
    }
}
