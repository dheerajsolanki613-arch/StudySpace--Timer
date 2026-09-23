package com.studyspace.timer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A study task, optionally attributed to a [SubjectEntity] and optionally
 * organized under a chapter/topic — the "Subject → Chapter → Topic → Task"
 * hierarchy the spec illustrates, modeled as two free-text fields on the
 * task rather than two more full entities with their own CRUD screens.
 * That's a deliberate scope call for this phase: [chapter]/[topic] give the
 * same organizing/grouping value the example shows (e.g. filtering or
 * grouping the Tasks list by them) without a Chapter and Topic management
 * UI this phase doesn't otherwise need. If per-chapter/topic management
 * (rename a chapter across all its tasks, etc.) is wanted later, promoting
 * these to real entities is a contained follow-up, not a rewrite.
 *
 * @param priority [com.studyspace.timer.data.TaskPriority.name].
 * @param deadlineEpochDay optional [java.time.LocalDate.toEpochDay] — a
 *   plain day, not a moment in time, same convention as
 *   [StudySessionEntity.dateEpochDay] (a task is "due Tuesday", not due at
 *   a specific millisecond).
 * @param estimatedDurationMinutes optional — display-only in this phase
 *   (not yet compared against actual time spent, which needs the timer
 *   integration a later phase adds).
 * @param subjectId `SET_NULL` on the subject's deletion, same reasoning as
 *   [StudySessionEntity.subjectId] — deleting a subject never destroys a
 *   task, just un-attributes it.
 * @param completedAtEpochMillis Phase 11 (Daily Summary): the wall-clock
 *   moment the task was marked complete ([System.currentTimeMillis]), or
 *   `null` if it is not complete. Needed so a day's summary can say "N tasks
 *   completed" for *that* day — [completed] alone can't answer that.
 *   Tasks already completed before this column existed keep `null` (no
 *   completion time was ever recorded for them, and inventing one would be a
 *   fabricated statistic), so they count toward all-time totals such as
 *   Achievements' "tasks done" but never toward any single day's summary.
 *   Cleared back to `null` if the task is re-opened. See
 *   [com.studyspace.timer.data.repository.applyTaskCompletion].
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val chapter: String? = null,
    val topic: String? = null,
    val subjectId: Long? = null,
    val priority: String,
    val deadlineEpochDay: Long? = null,
    val estimatedDurationMinutes: Int? = null,
    val completed: Boolean = false,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null
)
