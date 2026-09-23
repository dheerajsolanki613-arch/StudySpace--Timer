package com.studyspace.timer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyGoalDao {

    /**
     * REPLACE on conflict with the unique (scope, subjectId) index replaces
     * an existing row **only when subjectId is non-null**: SQLite treats
     * NULLs as distinct in a unique index, so two rows with the same scope
     * and a NULL subjectId never conflict. The weekly goal has a NULL
     * subjectId, so callers must delete the scope first (see
     * `GoalRepository.setWeeklyGoalMinutes`) — otherwise every edit adds a
     * row instead of replacing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: StudyGoalEntity): Long

    /**
     * The goal for a scope with no subject (i.e. the weekly goal), if one has
     * been set. Newest wins: databases written before the duplicate-row fix
     * may hold several rows for the scope, and the most recently set one is
     * what the user last chose.
     */
    @Query(
        "SELECT * FROM study_goals WHERE scope = :scope AND subjectId IS NULL " +
            "ORDER BY createdAtEpochMillis DESC, id DESC LIMIT 1"
    )
    fun goalForScope(scope: String): Flow<StudyGoalEntity?>

    @Query("DELETE FROM study_goals WHERE scope = :scope AND subjectId IS NULL")
    suspend fun deleteScope(scope: String)

    /** Phase 14 (Backup & Restore) only: wipes every goal (any scope) before a restore writes the backup's rows back in. */
    @Query("DELETE FROM study_goals")
    suspend fun deleteAll()
}
