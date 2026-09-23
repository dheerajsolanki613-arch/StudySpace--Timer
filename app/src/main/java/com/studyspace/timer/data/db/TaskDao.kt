package com.studyspace.timer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val ORDER_BY_CLAUSE =
    "ORDER BY completed ASC, CASE WHEN deadlineEpochDay IS NULL THEN 1 ELSE 0 END ASC, deadlineEpochDay ASC, createdAtEpochMillis DESC"

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    /** Incomplete first, then soonest deadline first (tasks with no deadline sort last within each group), newest-created as the final tiebreaker. */
    @Query("SELECT * FROM tasks $ORDER_BY_CLAUSE")
    fun allTasks(): Flow<List<TaskEntity>>

    /** Same ordering as [allTasks], scoped to one subject — feeds Subject Detail's task list. */
    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId $ORDER_BY_CLAUSE")
    fun tasksForSubject(subjectId: Long): Flow<List<TaskEntity>>

    /** Phase 14 (Backup & Restore) only: wipes every task before a restore writes the backup's rows back in. */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
