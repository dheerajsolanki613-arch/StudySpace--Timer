package com.studyspace.timer.data.repository

import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.StudySessionEntity
import java.time.LocalDate

/**
 * One range's planned-vs-actual comparison (Phase 7).
 *
 * @param plannedMillis summed [PlannedSessionEntity.durationMinutes] (as
 *   millis) for every non-[PlannedSessionStatus.CANCELLED] plan in the
 *   range — a cancelled plan was called off, so it was never really
 *   "planned" for that time anymore; excluding it keeps [plannedMillis]
 *   matching what the user actually intended to study, not a stale intent
 *   they explicitly withdrew.
 * @param actualMillis summed [StudySessionEntity.durationMillis] for every
 *   real recorded session in the range — completely independent of whether
 *   any of it happened to fulfill a plan (see [PlannedSessionEntity]'s
 *   class doc: there's no linking column), same "real recorded time
 *   regardless of intent" reasoning [SessionRepository]'s other stats
 *   already use.
 */
data class PlannedVsActual(
    val plannedMillis: Long,
    val actualMillis: Long
) {
    /**
     * `actual / planned`, as a percent, 0 when nothing was planned. Not
     * capped at 100 — studying *more* than was planned genuinely means
     * over 100%, and clamping it would quietly hide that (an "unsupported
     * claims" honesty call, same spirit as Phase 8's analytics section of
     * the spec).
     */
    val completionPercent: Int
        get() = if (plannedMillis <= 0L) 0 else ((actualMillis * 100) / plannedMillis).toInt()
}

/** Today / this-week / this-month planned-vs-actual, for the Analytics screen's comparison cards. */
data class PlannedVsActualAnalytics(
    val today: PlannedVsActual = PlannedVsActual(0L, 0L),
    val week: PlannedVsActual = PlannedVsActual(0L, 0L),
    val month: PlannedVsActual = PlannedVsActual(0L, 0L)
)

/**
 * Pure aggregation over already-fetched planned/actual lists — same
 * "recompute small datasets in plain Kotlin, unit-test the pure function"
 * shape as [SessionRepository.computeSubjectStats]/[SessionRepository.computeStreak].
 * Both input lists are expected to already cover at least
 * `today.minusDays(29)` through today (callers fetch with
 * `sessionsSince(monthStart)`, unbounded above — any plan dated *after*
 * today is simply ignored here rather than fetched with a second, narrower
 * query, same reasoning [SessionRepository.weeklyAnalytics] already uses
 * for reusing one wider query and filtering in Kotlin); rows outside that
 * window are harmless to pass in; they're filtered out by each bucket's own
 * date range below.
 *
 * Every bucket is bounded **through today, never past it** — "planned for
 * next week" isn't yet a completion result, so it's deliberately excluded
 * from "this week"/"this month" rather than inflating [PlannedVsActual.plannedMillis]
 * with intentions that haven't come due yet.
 */
fun computePlannedVsActual(
    plannedSessions: List<PlannedSessionEntity>,
    actualSessions: List<StudySessionEntity>,
    today: LocalDate
): PlannedVsActualAnalytics {
    val todayEpochDay = today.toEpochDay()
    val weekStartEpochDay = today.minusDays(6).toEpochDay()
    val monthStartEpochDay = today.minusDays(29).toEpochDay()

    fun plannedTotal(sinceDay: Long): Long = plannedSessions
        .filter { it.dateEpochDay in sinceDay..todayEpochDay && it.status != PlannedSessionStatus.CANCELLED.name }
        .sumOf { it.durationMinutes * 60_000L }

    fun actualTotal(sinceDay: Long): Long = actualSessions
        .filter { it.dateEpochDay in sinceDay..todayEpochDay }
        .sumOf { it.durationMillis }

    return PlannedVsActualAnalytics(
        today = PlannedVsActual(plannedTotal(todayEpochDay), actualTotal(todayEpochDay)),
        week = PlannedVsActual(plannedTotal(weekStartEpochDay), actualTotal(weekStartEpochDay)),
        month = PlannedVsActual(plannedTotal(monthStartEpochDay), actualTotal(monthStartEpochDay))
    )
}
