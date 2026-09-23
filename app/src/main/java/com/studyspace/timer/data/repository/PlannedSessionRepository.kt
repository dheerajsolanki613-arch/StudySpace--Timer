package com.studyspace.timer.data.repository

import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.db.PlannedSessionDao
import com.studyspace.timer.data.db.PlannedSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for [PlannedSessionEntity] storage — same role
 * [SubjectRepository]/[TaskRepository] play for their own tables.
 */
class PlannedSessionRepository(private val dao: PlannedSessionDao) {

    fun forDate(dateEpochDay: Long): Flow<List<PlannedSessionEntity>> = dao.forDate(dateEpochDay)

    fun upcoming(dateEpochDay: Long): Flow<List<PlannedSessionEntity>> = dao.upcoming(dateEpochDay)

    /** Phase 7 (Planned vs Actual): every plan on or after [sinceEpochDay], for [computePlannedVsActual]. */
    fun sessionsSince(sinceEpochDay: Long): Flow<List<PlannedSessionEntity>> = dao.sessionsSince(sinceEpochDay)

    suspend fun createPlan(
        dateEpochDay: Long,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        subjectId: Long?,
        taskId: Long?,
        notes: String?
    ): Long = dao.insert(
        PlannedSessionEntity(
            dateEpochDay = dateEpochDay,
            startMinuteOfDay = startMinuteOfDay,
            durationMinutes = durationMinutes,
            subjectId = subjectId,
            taskId = taskId,
            notes = notes?.trim()?.ifBlank { null },
            status = PlannedSessionStatus.PLANNED.name,
            createdAtEpochMillis = System.currentTimeMillis()
        )
    )

    suspend fun updatePlan(
        existing: PlannedSessionEntity,
        dateEpochDay: Long,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        subjectId: Long?,
        taskId: Long?,
        notes: String?
    ) {
        dao.update(
            existing.copy(
                dateEpochDay = dateEpochDay,
                startMinuteOfDay = startMinuteOfDay,
                durationMinutes = durationMinutes,
                subjectId = subjectId,
                taskId = taskId,
                notes = notes?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun markCompleted(plan: PlannedSessionEntity) {
        dao.update(plan.copy(status = PlannedSessionStatus.COMPLETED.name))
    }

    suspend fun cancelPlan(plan: PlannedSessionEntity) {
        dao.update(plan.copy(status = PlannedSessionStatus.CANCELLED.name))
    }

    suspend fun deletePlan(plan: PlannedSessionEntity) = dao.delete(plan)

    /**
     * Flips every [com.studyspace.timer.data.PlannedSessionStatus.PLANNED]
     * plan whose window has fully passed to
     * [com.studyspace.timer.data.PlannedSessionStatus.MISSED]. Called once
     * from `PlannerViewModel.init` (and safe to call repeatedly/idempotently
     * — an already-[MISSED] row is never matched by
     * [PlannedSessionDao.overduePlanned]'s `status = 'PLANNED'` filter, so
     * calling this twice in a row does nothing the second time).
     */
    suspend fun refreshMissedSessions(nowDateEpochDay: Long, nowMinuteOfDay: Int) {
        val overdue = dao.overduePlanned(nowDateEpochDay, nowMinuteOfDay)
        overdue.forEach { plan ->
            dao.update(plan.copy(status = PlannedSessionStatus.MISSED.name))
        }
    }

    companion object {
        /** Preset start times offered by the planner editor, 6 AM to 10 PM on the hour/half-hour. */
        val START_TIME_PRESETS: List<Pair<String, Int>> = buildList {
            var minute = 6 * 60
            while (minute <= 22 * 60) {
                add(formatMinuteOfDay(minute) to minute)
                minute += 30
            }
        }

        /** Preset durations offered by the planner editor. */
        val DURATION_PRESETS: List<Pair<String, Int>> = listOf(
            "30m" to 30, "45m" to 45, "1h" to 60, "1h 30m" to 90, "2h" to 120, "3h" to 180
        )

        /** Preset date offsets (days from today) offered by the planner editor. */
        val DATE_OFFSET_PRESETS: List<Pair<String, Long>> = listOf(
            "Today" to 0L, "Tomorrow" to 1L, "In 2 days" to 2L, "In 3 days" to 3L, "In 1 week" to 7L
        )
    }
}
