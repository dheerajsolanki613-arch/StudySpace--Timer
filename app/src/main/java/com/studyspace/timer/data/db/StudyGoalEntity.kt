package com.studyspace.timer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-set study target, scoped by [com.studyspace.timer.data.GoalScope].
 * Deliberately does **not** cover the daily goal — that single overall
 * number already lives in
 * [com.studyspace.timer.settings.SettingsRepository.dailyGoalMinutes]
 * (DataStore, Stage 8) and stays there rather than being duplicated here;
 * this table only covers goals that value doesn't (weekly, and later
 * per-subject).
 *
 * @param scope [com.studyspace.timer.data.GoalScope.name], stored as a
 *   plain string, same convention as every other enum column in this app
 *   (see [StudySessionEntity.type]).
 * @param subjectId reserved for [com.studyspace.timer.data.GoalScope.SUBJECT]
 *   goals once the Subject system exists — always `null` for a `WEEKLY`
 *   goal. Adding the column now (rather than in a second later migration)
 *   means the Subject phase can start writing subject goals without another
 *   schema change to this table.
 * @param targetMinutes the goal target, in minutes.
 * @param createdAtEpochMillis when this goal was last set — display-only,
 *   not used in any progress calculation.
 *
 * The unique index on (scope, subjectId) means there is at most one goal
 * row per scope (or per scope+subject once subjects exist);
 * [com.studyspace.timer.data.db.StudyGoalDao.upsert] relies on this to
 * replace an existing goal rather than accumulating duplicates when the
 * user changes their weekly target.
 */
@Entity(
    tableName = "study_goals",
    indices = [Index(value = ["scope", "subjectId"], unique = true)]
)
data class StudyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val scope: String,
    val subjectId: Long? = null,
    val targetMinutes: Int,
    val createdAtEpochMillis: Long
)
