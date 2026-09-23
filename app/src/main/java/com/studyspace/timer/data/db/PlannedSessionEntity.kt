package com.studyspace.timer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A planned study block — Phase 6 (Study Planner) of the expansion plan.
 * Scoped like [TaskEntity]/[SubjectEntity]: fixed preset pickers for date/
 * time rather than a calendar/time widget (same lower-risk reasoning as
 * `TaskEditorDialog`'s deadline chips — this project has never used
 * Material3's still-experimental `DatePicker`/`TimePicker` at the pinned
 * material3 1.2.1).
 *
 * @param dateEpochDay [java.time.LocalDate.toEpochDay] — a plain day, same
 *   convention as [TaskEntity.deadlineEpochDay]/[StudySessionEntity.dateEpochDay].
 * @param startMinuteOfDay minutes since local midnight (0-1439). Paired
 *   with [durationMinutes] rather than storing a separate end time, so the
 *   two can never end up inconsistent with each other.
 * @param subjectId / [taskId] `SET_NULL` on deletion, same reasoning as
 *   every other optional subject/task attribution in this app — deleting a
 *   subject or task never destroys a plan, just un-attributes it.
 * @param status [com.studyspace.timer.data.PlannedSessionStatus.name].
 * @param notes optional free-text note for the plan.
 *
 * **Deliberate scope decision for this phase, flagged rather than silently
 * decided:** there is no column linking a completed
 * [StudySessionEntity] back to the [PlannedSessionEntity] it fulfilled.
 * "Start" (from the Planner screen) pre-fills the Timer screen's existing
 * Phase-5 subject/task picker and jumps straight there, but the resulting
 * session is not auto-detected and auto-linked back to mark this plan
 * COMPLETED — the user marks a plan complete themselves from the Planner
 * screen. Auto-detection would need heuristics (which session matches
 * which plan?) this phase intentionally avoids; a real link (an explicit
 * `linkedSessionId` column + wiring through every timer ViewModel's save
 * path, mirroring how [TaskEntity]/[SubjectEntity] attribution was wired in
 * Phase 5) is a contained follow-up if wanted later. [MISSED] is instead
 * computed straightforwardly and honestly: a [PLANNED] plan whose time
 * window has fully passed becomes [MISSED] — see
 * `PlannedSessionRepository.refreshMissedSessions`.
 */
@Entity(
    tableName = "planned_sessions",
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
    indices = [Index("subjectId"), Index("taskId"), Index("dateEpochDay")]
)
data class PlannedSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val subjectId: Long? = null,
    val taskId: Long? = null,
    val notes: String? = null,
    val status: String,
    val createdAtEpochMillis: Long
)
