package com.studyspace.timer.data.repository

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.db.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    private fun session(dateEpochDay: Long, durationMillis: Long) = StudySessionEntity(
        type = SessionType.SELF_STUDY.name,
        label = "Study",
        startEpochMillis = 0L,
        durationMillis = durationMillis,
        completedNaturally = false,
        dateEpochDay = dateEpochDay
    )

    // --- computeBestStreak ---

    @Test
    fun `empty days gives zero best streak`() {
        assertEquals(0, computeBestStreak(emptyList()))
    }

    @Test
    fun `single day is a streak of one`() {
        assertEquals(1, computeBestStreak(listOf(100L)))
    }

    @Test
    fun `finds the longest run even if it is not the most recent one`() {
        // Days 100-104 (5-day run), gap, then days 110-111 (2-day run).
        val days = listOf(100L, 101L, 102L, 103L, 104L, 110L, 111L)
        assertEquals(5, computeBestStreak(days))
    }

    @Test
    fun `a broken streak does not erase a past longer run`() {
        val days = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 20L) // 7-day run, then an isolated day
        assertEquals(7, computeBestStreak(days))
    }

    // --- computeWeeklyConsistency ---

    @Test
    fun `zero sessions gives zero consistency`() {
        assertEquals(0, computeWeeklyConsistency(emptyList()))
    }

    @Test
    fun `four distinct days out of seven is roughly 57 percent`() {
        val sessions = listOf(session(1L, 1000L), session(2L, 1000L), session(3L, 1000L), session(4L, 1000L))
        assertEquals(57, computeWeeklyConsistency(sessions))
    }

    @Test
    fun `multiple sessions on the same day only count once`() {
        val sessions = listOf(session(1L, 1000L), session(1L, 2000L), session(1L, 3000L))
        assertEquals(14, computeWeeklyConsistency(sessions)) // 1/7 day, not 3/7
    }

    // --- computeGoalEverMet ---

    @Test
    fun `no sessions and no weekly goal means goal never met`() {
        assertFalse(computeGoalEverMet(emptyList(), dailyGoalMinutes = 240, currentWeekTotalMillis = 0L, weeklyGoalMinutes = null))
    }

    @Test
    fun `a single day meeting the daily goal counts`() {
        val sessions = listOf(session(5L, 250 * 60_000L)) // 250 minutes on one day
        assertTrue(computeGoalEverMet(sessions, dailyGoalMinutes = 240, currentWeekTotalMillis = 0L, weeklyGoalMinutes = null))
    }

    @Test
    fun `a day under the daily goal does not count on its own`() {
        val sessions = listOf(session(5L, 100 * 60_000L))
        assertFalse(computeGoalEverMet(sessions, dailyGoalMinutes = 240, currentWeekTotalMillis = 0L, weeklyGoalMinutes = null))
    }

    @Test
    fun `meeting the current week's weekly goal counts even with no matching day`() {
        val sessions = listOf(session(5L, 50 * 60_000L), session(6L, 50 * 60_000L))
        val weekTotal = 20 * 60 * 60_000L // 20 hours this week
        assertTrue(computeGoalEverMet(sessions, dailyGoalMinutes = 240, currentWeekTotalMillis = weekTotal, weeklyGoalMinutes = 15))
    }

    // --- computeAchievements ---

    @Test
    fun `all locked with zero history`() {
        val achievements = computeAchievements(sessionCount = 0, totalFocusedMillis = 0L, bestStreakDays = 0, goalEverMet = false)
        assertTrue(achievements.all { !it.unlocked })
        assertEquals(AchievementId.entries.size, achievements.size)
    }

    @Test
    fun `first session and ten sessions unlock at the right thresholds`() {
        val nine = computeAchievements(sessionCount = 9, totalFocusedMillis = 0L, bestStreakDays = 0, goalEverMet = false)
        assertTrue(nine.first { it.id == AchievementId.FIRST_SESSION }.unlocked)
        assertFalse(nine.first { it.id == AchievementId.TEN_SESSIONS }.unlocked)

        val ten = computeAchievements(sessionCount = 10, totalFocusedMillis = 0L, bestStreakDays = 0, goalEverMet = false)
        assertTrue(ten.first { it.id == AchievementId.TEN_SESSIONS }.unlocked)
    }

    @Test
    fun `focus hour achievements unlock at 10, 50 and 100 hours`() {
        val fortyNineHours = computeAchievements(
            sessionCount = 1, totalFocusedMillis = 49 * 3_600_000L, bestStreakDays = 0, goalEverMet = false
        )
        assertTrue(fortyNineHours.first { it.id == AchievementId.TEN_FOCUS_HOURS }.unlocked)
        assertFalse(fortyNineHours.first { it.id == AchievementId.FIFTY_FOCUS_HOURS }.unlocked)

        val hundredHours = computeAchievements(
            sessionCount = 1, totalFocusedMillis = 100 * 3_600_000L, bestStreakDays = 0, goalEverMet = false
        )
        assertTrue(hundredHours.first { it.id == AchievementId.HUNDRED_FOCUS_HOURS }.unlocked)
    }

    @Test
    fun `streak achievements use best-ever streak, not current`() {
        val achievements = computeAchievements(sessionCount = 1, totalFocusedMillis = 0L, bestStreakDays = 7, goalEverMet = false)
        assertTrue(achievements.first { it.id == AchievementId.SEVEN_DAY_STREAK }.unlocked)
        assertFalse(achievements.first { it.id == AchievementId.THIRTY_DAY_STREAK }.unlocked)
    }
}
