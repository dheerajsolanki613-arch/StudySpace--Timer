package com.studyspace.timer.data.repository

import com.studyspace.timer.data.db.StudySessionEntity

/**
 * Phase 9 (Streaks & Achievements) top-line numbers — all derived from real
 * stored history, nothing fabricated or estimated. See `AchievementsScreen`
 * for how these are laid out.
 */
data class StreakSummary(
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val weeklyConsistencyPercent: Int = 0,
    val totalFocusedMillis: Long = 0L,
    val completedSessionCount: Int = 0,
    val completedTaskCount: Int = 0
)

/**
 * The 8 achievements named in the spec. `title`/`description` live here
 * (not hardcoded in the screen) so [computeAchievements] and the UI can't
 * drift out of sync about what each one means.
 */
enum class AchievementId(val title: String, val description: String) {
    FIRST_SESSION("First Session", "Complete your first study session"),
    TEN_SESSIONS("10 Sessions", "Complete 10 study sessions"),
    TEN_FOCUS_HOURS("10 Focus Hours", "Reach 10 total hours of recorded study time"),
    FIFTY_FOCUS_HOURS("50 Focus Hours", "Reach 50 total hours of recorded study time"),
    HUNDRED_FOCUS_HOURS("100 Focus Hours", "Reach 100 total hours of recorded study time"),
    SEVEN_DAY_STREAK("7-Day Streak", "Study 7 days in a row, at any point"),
    THIRTY_DAY_STREAK("30-Day Streak", "Study 30 days in a row, at any point"),
    GOAL_COMPLETED("Goal Completed", "Meet your daily goal on some day, or your weekly goal this week")
}

data class Achievement(val id: AchievementId, val unlocked: Boolean)

/**
 * The longest run of consecutive study days **anywhere** in
 * [distinctDaysAsc] (already sorted oldest-first, one entry per day that
 * has at least one recorded session) — not just the run ending today,
 * unlike [SessionRepository.computeStreak]. Deliberately separate from
 * that function rather than reusing it: the 7/30-Day Streak achievements
 * need to stay unlocked forever once earned (a broken streak shouldn't
 * revoke a past achievement), while [SessionRepository.computeStreak]'s
 * whole point is the opposite — a live number that *does* drop to 0 the
 * day a streak breaks, for the Home/Subject-detail "current streak"
 * displays. Same input shape, deliberately different question asked of it.
 */
internal fun computeBestStreak(distinctDaysAsc: List<Long>): Int {
    if (distinctDaysAsc.isEmpty()) return 0
    var best = 1
    var current = 1
    for (i in 1 until distinctDaysAsc.size) {
        current = if (distinctDaysAsc[i] == distinctDaysAsc[i - 1] + 1) current + 1 else 1
        if (current > best) best = current
    }
    return best
}

/**
 * Percentage of the last 7 calendar days (today inclusive) that have at
 * least one recorded session, capped at 100. [sessions] is expected to
 * already be pre-filtered to that 7-day window by the caller (same
 * "caller filters, function just sums" split every other pure function in
 * this file/`AdvancedAnalytics.kt` uses).
 */
internal fun computeWeeklyConsistency(sessions: List<StudySessionEntity>): Int {
    val distinctDays = sessions.map { it.dateEpochDay }.distinct().size
    return ((distinctDays * 100) / 7).coerceAtMost(100)
}

/**
 * Whether the daily or weekly study goal has ever genuinely been met.
 *
 * **Deliberate scope simplification, flagged rather than silently
 * decided:** this checks *every* day in [allSessions] against the
 * *current* [dailyGoalMinutes] target, and only the *current* week's total
 * ([currentWeekTotalMillis]) against the *current* [weeklyGoalMinutes] —
 * not every historical week against whatever the weekly goal happened to
 * be at the time. There's no stored history of past goal *targets* (just
 * past goal *progress*, via recorded sessions), so "was last week's now-
 * changed goal met, back when it was a different number" isn't a question
 * this data can actually answer. A user who lowers their daily goal after
 * the fact may see this newly true on days that wouldn't have qualified
 * under the old, higher target — an acceptable, disclosed trade-off for a
 * single lightweight achievement, not treated as a bug.
 */
internal fun computeGoalEverMet(
    allSessions: List<StudySessionEntity>,
    dailyGoalMinutes: Int,
    currentWeekTotalMillis: Long,
    weeklyGoalMinutes: Int?
): Boolean {
    val dailyGoalMillis = dailyGoalMinutes * 60_000L
    val anyDayMet = dailyGoalMillis > 0L && allSessions
        .groupBy { it.dateEpochDay }
        .any { (_, daySessions) -> daySessions.sumOf { it.durationMillis } >= dailyGoalMillis }
    val weekMet = weeklyGoalMinutes != null && weeklyGoalMinutes > 0 &&
        currentWeekTotalMillis >= weeklyGoalMinutes * 60_000L
    return anyDayMet || weekMet
}

/**
 * The 8 spec achievements, each purely a threshold check against real
 * numbers — no persisted "unlocked" flag anywhere, and deliberately so:
 * every achievement here except [AchievementId.GOAL_COMPLETED] is
 * monotonic (session counts and total hours only ever go up;
 * [computeBestStreak] already looks at all of history, not just the
 * current streak, so a broken streak can't un-earn a past 7/30-day
 * badge) — recomputing live from history gives the exact same *stable*
 * answer a stored flag would, with no migration, no write path, and no
 * chance of the stored flag and the real data ever disagreeing.
 * [AchievementId.GOAL_COMPLETED] is the one exception to full stability —
 * see [computeGoalEverMet]'s KDoc.
 */
fun computeAchievements(
    sessionCount: Int,
    totalFocusedMillis: Long,
    bestStreakDays: Int,
    goalEverMet: Boolean
): List<Achievement> {
    val totalHours = totalFocusedMillis / 3_600_000L
    return AchievementId.entries.map { id ->
        val unlocked = when (id) {
            AchievementId.FIRST_SESSION -> sessionCount >= 1
            AchievementId.TEN_SESSIONS -> sessionCount >= 10
            AchievementId.TEN_FOCUS_HOURS -> totalHours >= 10
            AchievementId.FIFTY_FOCUS_HOURS -> totalHours >= 50
            AchievementId.HUNDRED_FOCUS_HOURS -> totalHours >= 100
            AchievementId.SEVEN_DAY_STREAK -> bestStreakDays >= 7
            AchievementId.THIRTY_DAY_STREAK -> bestStreakDays >= 30
            AchievementId.GOAL_COMPLETED -> goalEverMet
        }
        Achievement(id, unlocked)
    }
}
