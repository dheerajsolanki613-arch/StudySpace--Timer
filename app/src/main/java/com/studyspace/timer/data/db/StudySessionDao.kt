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

    /** Every session attributed to a subject, newest first — feeds Subject Detail's stats (Subject phase). */
    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY startEpochMillis DESC")
    fun sessionsForSubject(subjectId: Long): Flow<List<StudySessionEntity>>

    /**
     * Every session ever recorded, newest first. Phase 8 (Advanced
     * Analytics)'s "All time" range and its four fixed ranges (Today/7/30/90
     * days) all read from this single flow, filtered client-side by
     * [com.studyspace.timer.data.repository.sessionsInRange] — same "one
     * wide query, bucket in Kotlin" shape [sessionsSince] already
     * established for the narrower ranges
     * [com.studyspace.timer.data.repository.SessionRepository.weeklyAnalytics]/
     * [com.studyspace.timer.data.repository.SessionRepository.studyStats]
     * use, just with no lower bound at all this time since "All time" has
     * none by definition.
     */
    @Query("SELECT * FROM study_sessions ORDER BY startEpochMillis DESC")
    fun allSessions(): Flow<List<StudySessionEntity>>

    /** Phase 14 (Backup & Restore) only: wipes every session before a restore writes the backup's rows back in. */
    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}
