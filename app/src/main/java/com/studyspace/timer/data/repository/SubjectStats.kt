package com.studyspace.timer.data.repository

/**
 * Subject Detail's aggregate numbers — same "computed once, shown
 * consistently" role [StudyStats] plays for the dashboard. See
 * [SessionRepository.subjectStats] for exactly how each field is derived.
 *
 * @param totalMillis all-time total for this subject.
 * @param todayMillis sum of today's sessions for this subject.
 * @param weekMillis rolling 7-day window (today plus the preceding 6),
 *   same window [StudyStats.weekTotalMillis] uses — "this week" means the
 *   same thing everywhere in the app.
 * @param monthMillis rolling 30-day window, same rolling-window philosophy
 *   as [weekMillis] rather than a calendar month.
 * @param sessionCount number of sessions ever recorded for this subject.
 * @param averageSessionMillis [totalMillis] / [sessionCount], or 0 if there
 *   are no sessions yet.
 * @param streakDays consecutive days, ending today, with at least one
 *   session *for this subject* — computed the same way
 *   [SessionRepository.computeStreak] computes the app-wide streak, just
 *   over this subject's own sessions.
 */
data class SubjectStats(
    val totalMillis: Long = 0L,
    val todayMillis: Long = 0L,
    val weekMillis: Long = 0L,
    val monthMillis: Long = 0L,
    val sessionCount: Int = 0,
    val averageSessionMillis: Long = 0L,
    val streakDays: Int = 0
)
