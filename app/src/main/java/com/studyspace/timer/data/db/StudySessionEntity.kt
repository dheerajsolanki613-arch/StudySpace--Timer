package com.studyspace.timer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded study session, written by a [com.studyspace.timer.timer.TimerEngine]-
 * backed ViewModel when a session ends with a non-zero elapsed time — either
 * because it reached its target (countdown/Pomodoro work phase) or because
 * the user stopped it early (any mode). See
 * [com.studyspace.timer.data.repository.SessionRepository.recordSession] for
 * the save path and the minimum-duration filter that keeps accidental
 * start/stop taps out of the table.
 *
 * @param type [com.studyspace.timer.data.SessionType.name] — stored as a
 *   plain string rather than a Room enum TypeConverter so old rows stay
 *   readable even if the enum's members change later.
 * @param label Human-readable label shown in "Recent Activity" (e.g.
 *   "Self-Study", "Pomodoro — Work"). Kept separate from [type] since one
 *   type can have more than one label in future stages.
 * @param startEpochMillis wall-clock time the session began
 *   ([System.currentTimeMillis]). Used only for display/ordering — actual
 *   elapsed-time accuracy comes from [durationMillis], which is computed by
 *   the drift-resistant [com.studyspace.timer.timer.TimerEngine] using
 *   [android.os.SystemClock.elapsedRealtime].
 * @param durationMillis time actually spent studying in this session.
 * @param completedNaturally true if the timer ran to its target (countdown
 *   reached zero, or a Pomodoro work phase finished); false if the user
 *   stopped it early. Always false for open-ended stopwatch modes
 *   (Self-Study, Online Study, Focus Mode), which have no target to reach.
 * @param dateEpochDay [java.time.LocalDate.toEpochDay] of [startEpochMillis]
 *   in the device's zone at the moment the session started — a plain Long
 *   column so day-bucketed queries (today's total, this week, streak) don't
 *   need date math inside SQL.
 * @param subjectId optional [SubjectEntity] this session is attributed to
 *   (Subject phase). `SET_NULL` on delete: removing a subject un-attributes
 *   its past sessions rather than deleting them — study history is never
 *   destroyed by a subject-management action.
 * @param taskId optional [TaskEntity] this session was started from (Tasks
 *   phase) — set once timer screens gain "start from a task" (a later
 *   phase); `null` for every session recorded before then and for any
 *   session not started from a task. `SET_NULL` on delete, same reasoning
 *   as [subjectId].
 */
@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // Phase 19 (Performance): dateEpochDay and startEpochMillis added here — every
    // other table's queries are either small/bounded (subjects, tasks, goals) or
    // already indexed on what they filter by (planned_sessions.dateEpochDay, from
    // migration 4→5). study_sessions is the one table that grows unboundedly (every
    // completed session, forever), and dateEpochDay/startEpochMillis are exactly what
    // StudySessionDao's hottest queries filter or sort by: sessionsSince (called by
    // Analytics, the Home dashboard, Goals, streaks, and Phase 17's smart planner) and
    // distinctSessionDaysDesc filter/sort on dateEpochDay; recentSessions (the Home
    // dashboard's "Recent Activity", read on every app open) is ORDER BY
    // startEpochMillis DESC LIMIT :limit, which a B-tree index lets SQLite satisfy by
    // walking the index backwards and stopping at :limit rows, instead of scanning and
    // sorting the whole table first. See AppDatabase.MIGRATION_6_7.
    indices = [Index("subjectId"), Index("taskId"), Index("dateEpochDay"), Index("startEpochMillis")]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val label: String,
    val startEpochMillis: Long,
    val durationMillis: Long,
    val completedNaturally: Boolean,
    val dateEpochDay: Long,
    val subjectId: Long? = null,
    val taskId: Long? = null
)

