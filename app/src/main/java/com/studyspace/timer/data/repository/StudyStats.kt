package com.studyspace.timer.data.repository

/**
 * Dashboard-facing aggregate numbers, computed by [SessionRepository] from
 * stored sessions so Home (Stage 5) and Analytics (Stage 7) derive the same
 * numbers the same way instead of each re-implementing the math.
 *
 * @param todayTotalMillis sum of session durations that started today (device zone).
 * @param todaySessionCount number of sessions started today.
 * @param weekTotalMillis sum of session durations over the last 7 days
 *   (today plus the preceding 6), a rolling window rather than a
 *   calendar-week bucket.
 * @param streakDays consecutive days, ending today, with at least one
 *   session — see [SessionRepository.computeStreak] for exactly how "ending
 *   today" is treated before today has a session of its own yet.
 */
data class StudyStats(
    val todayTotalMillis: Long = 0L,
    val todaySessionCount: Int = 0,
    val weekTotalMillis: Long = 0L,
    val streakDays: Int = 0
)
