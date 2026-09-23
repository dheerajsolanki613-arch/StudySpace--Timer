package com.studyspace.timer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Insert
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    /** Alphabetical, case-insensitive — a fixed, predictable order rather than creation order. */
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun allSubjects(): Flow<List<SubjectEntity>>

    /** Phase 14 (Backup & Restore) only: wipes every subject before a restore writes the backup's rows back in. */
    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}
