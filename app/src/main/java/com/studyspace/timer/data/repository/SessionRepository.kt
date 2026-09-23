package com.studyspace.timer.data.repository

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.db.StudySessionDao
import com.studyspace.timer.data.db.StudySessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single access point for study-session storage. Timer ViewModels call
 * [recordSession] when a session ends; Home (Stage 5) and, later, Analytics
 * (Stage 7) read [recentSessions] and [studyStats].
 */
class SessionRepository(
    private val dao: StudySessionDao,
    /**
     * Phase 12: called after a session has been written (never for one dropped
     * by the minimum-duration filter), so something outside the data layer —
     * currently the goal-reached notification — can react without this class
     * knowing about notifications. Defaults to none, so tests and any other
     * construction site are unaffected.
     */
    private val onSessionRecorded: ((StudySessionEntity) -> Unit)? = null
) {

    /** Most recent sessions across every mode, for "Recent Activity". */
    fun recentSessions(limit: Int = 10): Flow<List<StudySessionEntity>> = dao.recentSessions(limit)

    /** Phase 7 (Planned vs Actual): every session on or after [sinceEpochDay], for [computePlannedVsActual]. */
    fun sessionsSince(sinceEpochDay: Long): Flow<List<StudySessionEntity>> = dao.sessionsSince(sinceEpochDay)

    /** Phase 8 (Advanced Analytics): every session ever recorded, for [sessionsInRange] to filter client-side. */
    fun allSessions(): Flow<List<StudySessionEntity>> = dao.allSessions()

    /**
     * Rolling "today" + "this week" + "streak" numbers. Computed from two
     * queries: the last 7 days of sessions (today + week totals) and every
     * distinct day that has ever had a session (streak, since a streak can
     * run longer than 7 days). Recomputes on every emission from either
     * query; it does **not** re-fire automatically at midnight if the
     * screen is left open across a day boundary — a known Stage 5
     * simplification, not a bug, since the app is normally opened fresh
     * each session.
     */
    fun studyStats(): Flow<StudyStats> {
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val weekStartEpochDay = today.minusDays(6).toEpochDay()

        return combine(
            dao.sessionsSince(weekStartEpochDay),
            dao.distinctSessionDaysDesc()
        ) { weekSessions, distinctDaysDesc ->
            val todaySessions = weekSessions.filter { it.dateEpochDay == todayEpochDay }
            StudyStats(
                todayTotalMillis = todaySessions.sumOf { it.durationMillis },
                todaySessionCount = todaySessions.size,
                weekTotalMillis = weekSessions.sumOf { it.durationMillis },
                streakDays = computeStreak(distinctDaysDesc, todayEpochDay)
            )
        }
    }

    /**
     * Stage 7 analytics: the last 7 days' study time, bucketed by day (for
     * the weekly chart) and by [SessionType] (for the per-mode breakdown
     * cards). Deliberately reuses [dao.sessionsSince] — the same query
     * [studyStats] already uses for its week total — rather than adding a
     * new `@Query`, so this doesn't introduce a second, unverified
     * Room/SQL surface; all the bucketing happens in plain Kotlin over
     * data that's already flowing through a tested query.
     */
    fun weeklyAnalytics(): Flow<WeeklyAnalytics> {
        val today = LocalDate.now()
        val weekStartEpochDay = today.minusDays(6).toEpochDay()

        return dao.sessionsSince(weekStartEpochDay).map { sessions ->
            val dailyTotals = (0..6).map { offset ->
                val date = today.minusDays((6 - offset).toLong())
                val total = sessions
                    .filter { it.dateEpochDay == date.toEpochDay() }
                    .sumOf { it.durationMillis }
                DayTotal(date = date, totalMillis = total)
            }

            val typeTotals = sessions
                .mapNotNull { session ->
                    runCatching { SessionType.valueOf(session.type) }.getOrNull()?.let { it to session.durationMillis }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, durations) -> durations.sum() }

            WeeklyAnalytics(dailyTotals = dailyTotals, typeTotals = typeTotals)
        }
    }

    /**
     * Subject Detail's aggregate numbers for one subject, recomputed on
     * every emission from [dao.sessionsForSubject] — same "small dataset,
     * fine to recompute in Kotlin" reasoning [studyStats] and
     * [weeklyAnalytics] already use, rather than a set of bespoke
     * per-stat SQL queries.
     */
    fun subjectStats(subjectId: Long): Flow<SubjectStats> {
        val today = LocalDate.now()
        return dao.sessionsForSubject(subjectId).map { sessions ->
            computeSubjectStats(sessions, today)
        }
    }

    /**
     * Pure aggregation over an already-fetched session list — split out
     * from [subjectStats] so it can be covered by a fast local JUnit test
     * (see `SubjectStatsTest`) without a Room/Flow/Android dependency,
     * same reasoning as [computeStreak].
     */
    internal fun computeSubjectStats(sessions: List<StudySessionEntity>, today: LocalDate): SubjectStats {
        val todayEpochDay = today.toEpochDay()
        val weekStartEpochDay = today.minusDays(6).toEpochDay()
        val monthStartEpochDay = today.minusDays(29).toEpochDay()

        val total = sessions.sumOf { it.durationMillis }
        val todayTotal = sessions.filter { it.dateEpochDay == todayEpochDay }.sumOf { it.durationMillis }
        val weekTotal = sessions.filter { it.dateEpochDay >= weekStartEpochDay }.sumOf { it.durationMillis }
        val monthTotal = sessions.filter { it.dateEpochDay >= monthStartEpochDay }.sumOf { it.durationMillis }
        val count = sessions.size
        val average = if (count > 0) total / count else 0L
        val distinctDaysDesc = sessions.map { it.dateEpochDay }.distinct().sortedDescending()

        return SubjectStats(
            totalMillis = total,
            todayMillis = todayTotal,
            weekMillis = weekTotal,
            monthMillis = monthTotal,
            sessionCount = count,
            averageSessionMillis = average,
            streakDays = computeStreak(distinctDaysDesc, todayEpochDay)
        )
    }

    /**
     * Saves a session, unless it's shorter than [MIN_RECORDABLE_MILLIS] —
     * a floor to keep an accidental Start-then-immediately-Stop tap from
     * polluting history and streak/stat counts as a "real" session.
     */
    suspend fun recordSession(
        type: SessionType,
        label: String,
        startEpochMillis: Long,
        durationMillis: Long,
        completedNaturally: Boolean,
        subjectId: Long? = null,
        taskId: Long? = null
    ) {
        if (durationMillis < MIN_RECORDABLE_MILLIS) return
        val startDate = Instant.ofEpochMilli(startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val session = StudySessionEntity(
            type = type.name,
            label = label,
            startEpochMillis = startEpochMillis,
            durationMillis = durationMillis,
            completedNaturally = completedNaturally,
            dateEpochDay = startDate.toEpochDay(),
            subjectId = subjectId,
            taskId = taskId
        )
        dao.insert(session)
        onSessionRecorded?.invoke(session)
    }

    /**
     * Consecutive days, ending at [todayEpochDay], present in
     * [distinctDaysDesc] (already sorted newest-first). If today has no
     * session yet, the streak is still reported as running through
     * yesterday rather than showing 0 the moment the clock passes
     * midnight and before the user has studied today — it only actually
     * breaks once a day is skipped entirely.
     */
    internal fun computeStreak(distinctDaysDesc: List<Long>, todayEpochDay: Long): Int {
        if (distinctDaysDesc.isEmpty()) return 0
        val days = distinctDaysDesc.toHashSet()
        var cursor = todayEpochDay
        if (!days.contains(cursor)) cursor -= 1
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor -= 1
        }
        return streak
    }

    companion object {
        const val MIN_RECORDABLE_MILLIS = 10_000L
    }
}
