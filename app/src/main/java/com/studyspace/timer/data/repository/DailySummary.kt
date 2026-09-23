package com.studyspace.timer.data.repository

import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Phase 11 — one calendar day's study summary. Every field is a plain count
 * or total of real stored data; there is deliberately no "score", no
 * comparison against other days, and no generated encouragement text — the
 * spec's own instruction for this phase is "do not generate fake
 * motivational statistics".
 *
 * @param subjectCount distinct subjects with at least one session that day.
 *   Sessions with no subject are real study time (they count toward
 *   [totalMillis]/[sessionCount]) but are not "a subject", so they don't
 *   inflate this.
 * @param topSubject the attributed subject with the most study *time* that
 *   day (not most sessions — same reasoning as
 *   [computeProductivityPatterns]), or `null` if no session that day had a
 *   subject. Never a [SubjectTotal] with a `null` id.
 * @param goal the day's total measured against the daily goal, using the
 *   same [GoalProgress] math as Home and the Goals screen (so this screen
 *   can never disagree with them about what "90%" means).
 * @param tasksCompleted tasks whose completion time falls on this day (in
 *   the device's zone). Tasks completed before completion times were
 *   recorded (see [TaskEntity.completedAtEpochMillis]) are not counted here.
 */
data class DailySummary(
    val date: LocalDate,
    val totalMillis: Long,
    val sessionCount: Int,
    val subjectCount: Int,
    val topSubject: SubjectTotal?,
    val goal: GoalProgress,
    val tasksCompleted: Int
) {
    /** False for a day with no recorded sessions and no completed tasks — drives the screen's empty-state note. */
    val hasActivity: Boolean
        get() = sessionCount > 0 || tasksCompleted > 0
}

/**
 * Pure aggregation over already-fetched data, same "recompute a small
 * dataset in plain Kotlin, unit-test the pure function" shape as
 * [computeAdvancedStats]/[computePlannedVsActual].
 *
 * [sessions] may contain rows from other days (callers fetch with
 * `sessionsSince(date)`, which is open-ended above); only rows whose
 * [StudySessionEntity.dateEpochDay] equals [date] are used. [zone] is only
 * used to place [TaskEntity.completedAtEpochMillis] on a calendar day —
 * sessions already carry a precomputed local `dateEpochDay`. It is a
 * parameter (defaulting to the device zone) so tests don't depend on the
 * machine they run on.
 *
 * [dailyGoalMillis] is the *current* daily goal. Past goal targets aren't
 * stored anywhere, so a past day is measured against today's goal — the
 * screen says so, and this is the same disclosed simplification
 * [computeGoalEverMet] already makes.
 */
fun computeDailySummary(
    sessions: List<StudySessionEntity>,
    tasks: List<TaskEntity>,
    dailyGoalMillis: Long,
    date: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): DailySummary {
    val epochDay = date.toEpochDay()
    val daySessions = sessions.filter { it.dateEpochDay == epochDay }
    val totalMillis = daySessions.sumOf { it.durationMillis }

    // Attributed subjects only, most study time first; ties broken by id so
    // the result never depends on the order the DAO happened to return rows.
    val attributedTotals = computeSubjectTotals(daySessions)
        .filter { it.subjectId != null }
        .sortedWith(compareByDescending<SubjectTotal> { it.totalMillis }.thenBy { it.subjectId })

    val tasksCompleted = tasks.count { task ->
        val completedAt = task.completedAtEpochMillis
        task.completed && completedAt != null &&
            Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate() == date
    }

    return DailySummary(
        date = date,
        totalMillis = totalMillis,
        sessionCount = daySessions.size,
        subjectCount = attributedTotals.size,
        topSubject = attributedTotals.firstOrNull(),
        goal = GoalProgress(label = "Goal", currentMillis = totalMillis, targetMillis = dailyGoalMillis),
        tasksCompleted = tasksCompleted
    )
}

/**
 * "Today" / "Yesterday" / "Monday, Sep 21" (with the year appended only when
 * it differs from [today]'s, so a date from last year isn't ambiguous).
 */
fun dailySummaryDateLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String {
    if (date == today) return "Today"
    if (date == today.minusDays(1)) return "Yesterday"
    val pattern = if (date.year == today.year) "EEEE, MMM d" else "EEE, MMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}
