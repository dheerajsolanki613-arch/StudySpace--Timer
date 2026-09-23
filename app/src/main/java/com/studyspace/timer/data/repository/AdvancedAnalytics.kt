package com.studyspace.timer.data.repository

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.db.StudySessionEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** A selectable time range for the Analytics screen's Phase 8 sections. [days] is `null` for "All time". */
enum class AnalyticsRange(val label: String, val days: Int?) {
    TODAY("Today", 1),
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30),
    LAST_90_DAYS("90 days", 90),
    ALL_TIME("All time", null)
}

/**
 * Modes that have an actual target to reach (a countdown to zero, a
 * Pomodoro work phase, a Focus session) as opposed to an open-ended
 * stopwatch ([SessionType.SELF_STUDY]/[SessionType.ONLINE_STUDY], where
 * [StudySessionEntity.completedNaturally] is always `false` — see that
 * field's KDoc). [AdvancedStats.completionRatePercent] only considers these
 * three, so a self-study-heavy history doesn't drag an honest "did you
 * finish what you started" number down to near-zero for a reason that has
 * nothing to do with actually finishing anything.
 */
private val TARGET_BASED_TYPES = setOf(
    SessionType.NORMAL_TIMER.name, SessionType.POMODORO.name, SessionType.FOCUS_MODE.name
)

/**
 * Phase 8 "Statistics" section numbers for one [AnalyticsRange].
 *
 * @param weeklyAverageMillis `null` when [AnalyticsRange] spans fewer than
 *   7 days (i.e. only [AnalyticsRange.TODAY], or [AnalyticsRange.ALL_TIME]
 *   on an install less than a week old) — extrapolating "per week" from a
 *   single day would be exactly the kind of unsupported claim this phase's
 *   spec explicitly rules out, so this is "not enough data yet" rather
 *   than a misleading number.
 */
data class AdvancedStats(
    val totalMillis: Long = 0L,
    val sessionCount: Int = 0,
    val averageSessionMillis: Long = 0L,
    val longestSessionMillis: Long = 0L,
    val completedPomodoros: Int = 0,
    val completionRatePercent: Int = 0,
    val dailyAverageMillis: Long = 0L,
    val weeklyAverageMillis: Long? = null
)

/** One subject's (or `null` = unattributed) total study time within a range, for the "By Subject" breakdown. */
data class SubjectTotal(val subjectId: Long?, val totalMillis: Long)

/**
 * Phase 8 "Productivity patterns" for one [AnalyticsRange]. Every field is
 * `null`/zero — never a fabricated guess — when there isn't enough data to
 * say anything, per the spec's explicit "do not make unsupported claims...
 * present statistics as descriptive data" instruction for this phase.
 *
 * @param completionRatePercent intentionally the **same value and the same
 *   calculation** as [AdvancedStats.completionRatePercent] — the spec lists
 *   "session completion rate" under Productivity Patterns separately from
 *   "Focus completion rate" under Statistics, but they describe the same
 *   thing (finished vs. abandoned, across target-based modes), so this
 *   reuses rather than silently computing a second, subtly different
 *   number under a similar name. `AnalyticsScreen` labels both instances
 *   so the duplication reads as intentional, not as two different metrics.
 */
data class ProductivityPatterns(
    val mostProductiveDayLabel: String? = null,
    val mostProductiveHourRangeLabel: String? = null,
    val consistencyPercent: Int = 0,
    val completionRatePercent: Int = 0
)

/**
 * Filters [sessions] (expected to already cover at least [range]'s window —
 * callers fetch via [SessionRepository.allSessions], unbounded, and let
 * this do the date-range slicing) down to just the ones inside [range],
 * relative to [today]. `ALL_TIME` returns [sessions] unchanged.
 */
fun sessionsInRange(sessions: List<StudySessionEntity>, range: AnalyticsRange, today: LocalDate): List<StudySessionEntity> {
    val days = range.days ?: return sessions
    val sinceEpochDay = today.minusDays((days - 1).toLong()).toEpochDay()
    return sessions.filter { it.dateEpochDay >= sinceEpochDay }
}

/**
 * Days the range actually spans, for averaging. A fixed range's span is
 * just [AnalyticsRange.days]; "All time"'s span is measured from the
 * earliest session actually in [sessions] through [today] (not from
 * install date, which this app doesn't track) — an install with one
 * session yesterday has an "All time" span of 2 days, not some arbitrary
 * constant.
 */
private fun elapsedDays(sessions: List<StudySessionEntity>, range: AnalyticsRange, today: LocalDate): Int {
    range.days?.let { return it }
    if (sessions.isEmpty()) return 1
    val earliestEpochDay = sessions.minOf { it.dateEpochDay }
    return (today.toEpochDay() - earliestEpochDay + 1).toInt().coerceAtLeast(1)
}

private fun completionRate(sessions: List<StudySessionEntity>): Int {
    val targetBased = sessions.filter { it.type in TARGET_BASED_TYPES }
    if (targetBased.isEmpty()) return 0
    return (targetBased.count { it.completedNaturally } * 100) / targetBased.size
}

/**
 * Pure aggregation over an already-range-filtered session list (see
 * [sessionsInRange]) — same "recompute a small dataset in plain Kotlin,
 * unit-test the pure function" shape as
 * [SessionRepository.computeSubjectStats]/[SessionRepository.computeStreak].
 */
fun computeAdvancedStats(sessions: List<StudySessionEntity>, range: AnalyticsRange, today: LocalDate): AdvancedStats {
    if (sessions.isEmpty()) return AdvancedStats()

    val totalMillis = sessions.sumOf { it.durationMillis }
    val count = sessions.size
    val days = elapsedDays(sessions, range, today)
    val dailyAverage = totalMillis / days

    return AdvancedStats(
        totalMillis = totalMillis,
        sessionCount = count,
        averageSessionMillis = totalMillis / count,
        longestSessionMillis = sessions.maxOf { it.durationMillis },
        completedPomodoros = sessions.count { it.type == SessionType.POMODORO.name && it.completedNaturally },
        completionRatePercent = completionRate(sessions),
        dailyAverageMillis = dailyAverage,
        weeklyAverageMillis = if (days >= 7) (totalMillis * 7) / days else null
    )
}

/**
 * Per-subject totals within an already-range-filtered session list, widest
 * first. `subjectId = null` groups every session with no subject
 * attributed — included, not dropped, since that's real (if
 * un-categorized) study time, and `AnalyticsScreen` labels that entry "No
 * subject" rather than silently omitting it.
 */
fun computeSubjectTotals(sessions: List<StudySessionEntity>): List<SubjectTotal> =
    sessions.groupBy { it.subjectId }
        .map { (subjectId, group) -> SubjectTotal(subjectId, group.sumOf { it.durationMillis }) }
        .sortedByDescending { it.totalMillis }

/**
 * 2-hour buckets (0-2, 2-4, ... 22-24) rather than per-hour — per-hour
 * buckets over a typical person's handful of sessions a day rarely get
 * more than one hit each, which would make "most productive hour" mostly
 * noise (whichever hour happened to get a session first). A wider bucket
 * needs a few real sessions landing in the same window before it can claim
 * one, which is closer to an actual pattern.
 */
private fun hourBucketStart(startEpochMillis: Long): Int {
    val hour = Instant.ofEpochMilli(startEpochMillis).atZone(ZoneId.systemDefault()).hour
    return (hour / 2) * 2
}

private fun formatHourBucketRange(bucketStartHour: Int): String =
    "${formatMinuteOfDay(bucketStartHour * 60)} – ${formatMinuteOfDay((bucketStartHour + 2) * 60)}"

/**
 * Pure aggregation over an already-range-filtered session list, same shape
 * as [computeAdvancedStats]. [ProductivityPatterns.mostProductiveDayLabel]/
 * [ProductivityPatterns.mostProductiveHourRangeLabel] rank by total study
 * *time* in each bucket, not session *count* — a single 3-hour Sunday
 * session should be able to make Sunday "most productive" over five
 * scattered 5-minute Tuesday check-ins, which a count-based ranking would
 * get backwards.
 */
fun computeProductivityPatterns(sessions: List<StudySessionEntity>, range: AnalyticsRange, today: LocalDate): ProductivityPatterns {
    if (sessions.isEmpty()) return ProductivityPatterns()

    val mostProductiveDay = sessions
        .groupBy { LocalDate.ofEpochDay(it.dateEpochDay).dayOfWeek }
        .mapValues { (_, group) -> group.sumOf { it.durationMillis } }
        .maxByOrNull { it.value }
        ?.key

    val mostProductiveHourBucket = sessions
        .groupBy { hourBucketStart(it.startEpochMillis) }
        .mapValues { (_, group) -> group.sumOf { it.durationMillis } }
        .maxByOrNull { it.value }
        ?.key

    val distinctDaysStudied = sessions.map { it.dateEpochDay }.distinct().size
    val days = elapsedDays(sessions, range, today)
    val consistency = ((distinctDaysStudied * 100) / days).coerceAtMost(100)

    return ProductivityPatterns(
        mostProductiveDayLabel = mostProductiveDay?.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        mostProductiveHourRangeLabel = mostProductiveHourBucket?.let { formatHourBucketRange(it) },
        consistencyPercent = consistency,
        completionRatePercent = completionRate(sessions)
    )
}
