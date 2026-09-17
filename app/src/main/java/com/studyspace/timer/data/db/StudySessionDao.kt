package com.studyspace.timer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Insert
    suspend fun insert(session: StudySessionEntity): Long

    /** Most recent sessions across all modes, newest first, for "Recent Activity". */
    @Query("SELECT * FROM study_sessions ORDER BY startEpochMillis DESC LIMIT :limit")
    fun recentSessions(limit: Int): Flow<List<StudySessionEntity>>

    /**
     * Every session from [sinceEpochDay] (inclusive) onward, newest first.
     * Used for "today" (sinceEpochDay = today) and "this week"
     * (sinceEpochDay = today - 6) totals — the caller sums/filters
     * [StudySessionEntity.durationMillis] client-side rather than doing the
     * date math in SQL.
     */
    @Query("SELECT * FROM study_sessions WHERE dateEpochDay >= :sinceEpochDay ORDER BY startEpochMillis DESC")
    fun sessionsSince(sinceEpochDay: Long): Flow<List<StudySessionEntity>>

    /** Every distinct day (all-time) that has at least one session, newest first — used to compute the study streak. */
    @Query("SELECT DISTINCT dateEpochDay FROM study_sessions ORDER BY dateEpochDay DESC")
    fun distinctSessionDaysDesc(): Flow<List<Long>>
}
