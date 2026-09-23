package com.studyspace.timer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedSessionDao {

    @Insert
    suspend fun insert(session: PlannedSessionEntity): Long

    @Update
    suspend fun update(session: PlannedSessionEntity)

    @Delete
    suspend fun delete(session: PlannedSessionEntity)

    /** Every plan for one calendar day, earliest start time first. */
    @Query("SELECT * FROM planned_sessions WHERE dateEpochDay = :dateEpochDay ORDER BY startMinuteOfDay ASC")
    fun forDate(dateEpochDay: Long): Flow<List<PlannedSessionEntity>>

    /**
     * Every still-[com.studyspace.timer.data.PlannedSessionStatus.PLANNED]
     * plan strictly after [dateEpochDay] (i.e. excluding today — the
     * Planner screen shows today's list separately via [forDate]), earliest
     * date/time first. Feeds the "Upcoming" section.
     */
    @Query(
        "SELECT * FROM planned_sessions WHERE dateEpochDay > :dateEpochDay AND status = 'PLANNED' " +
            "ORDER BY dateEpochDay ASC, startMinuteOfDay ASC"
    )
    fun upcoming(dateEpochDay: Long): Flow<List<PlannedSessionEntity>>

    /**
     * Every [com.studyspace.timer.data.PlannedSessionStatus.PLANNED] row
     * whose day is strictly before [dateEpochDay], or is [dateEpochDay]
     * itself with an end time (`startMinuteOfDay + durationMinutes`)
     * already before [minuteOfDay] — i.e. its whole window has passed with
     * no action taken. Read by `PlannedSessionRepository.refreshMissedSessions`
     * to flip these to [com.studyspace.timer.data.PlannedSessionStatus.MISSED].
     */
    @Query(
        "SELECT * FROM planned_sessions WHERE status = 'PLANNED' AND " +
            "(dateEpochDay < :dateEpochDay OR (dateEpochDay = :dateEpochDay AND (startMinuteOfDay + durationMinutes) < :minuteOfDay))"
    )
    suspend fun overduePlanned(dateEpochDay: Long, minuteOfDay: Int): List<PlannedSessionEntity>

    /**
     * Every plan from [sinceEpochDay] (inclusive) onward, regardless of
     * status. Same "caller sums/filters client-side" shape as
     * [com.studyspace.timer.data.db.StudySessionDao.sessionsSince] — feeds
     * Phase 7's planned-vs-actual analytics, where the caller further
     * bounds by an upper date and excludes CANCELLED plans itself (see
     * `computePlannedVsActual`).
     */
    @Query("SELECT * FROM planned_sessions WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay ASC, startMinuteOfDay ASC")
    fun sessionsSince(sinceEpochDay: Long): Flow<List<PlannedSessionEntity>>

    /**
     * Every planned session regardless of date or status. Phase 14 (Backup &
     * Restore) only — a full backup must include past/completed/missed/
     * cancelled plans too, not just [sessionsSince]'s "from today on".
     */
    @Query("SELECT * FROM planned_sessions ORDER BY dateEpochDay ASC, startMinuteOfDay ASC")
    fun allPlannedSessions(): Flow<List<PlannedSessionEntity>>

    /** Phase 14 (Backup & Restore) only: wipes every planned session before a restore writes the backup's rows back in. */
    @Query("DELETE FROM planned_sessions")
    suspend fun deleteAll()
}
