package com.studyspace.timer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A subject the user studies (e.g. Mathematics, Physics), created/renamed/
 * deleted from the Subjects screen. A [StudySessionEntity] can optionally
 * reference one via [StudySessionEntity.subjectId] — "optionally" because
 * every session mode that existed before this phase (Self-Study, Online
 * Study, Normal Timer, Pomodoro, Focus Mode) still works with no subject
 * selected at all; subject selection at timer-start time is later work
 * (timer integration), not this phase's job.
 *
 * @param icon a single emoji, chosen from [com.studyspace.timer.data.repository.SubjectRepository.ICON_PRESETS]
 *   rather than a drawable resource — keeps this entity itself free of any
 *   resource-id churn risk across app versions.
 * @param colorArgb a packed ARGB color int (same format
 *   `android.graphics.Color`/`androidx.compose.ui.graphics.Color(Int)` use),
 *   chosen from [com.studyspace.timer.data.repository.SubjectRepository.COLOR_PRESETS].
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val icon: String,
    val colorArgb: Int,
    val createdAtEpochMillis: Long
)
