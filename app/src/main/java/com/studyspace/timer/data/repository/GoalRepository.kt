package com.studyspace.timer.data.repository

import com.studyspace.timer.data.GoalScope
import com.studyspace.timer.data.db.StudyGoalDao
import com.studyspace.timer.data.db.StudyGoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for [StudyGoalEntity] storage — same role for goals
 * that [SessionRepository] plays for sessions. Only the [GoalScope.WEEKLY]
 * goal is wired up yet; [GoalScope.SUBJECT] goals wait on the Subject
 * system (a later phase) since a subject goal needs a real subject to
 * attach to.
 */
class GoalRepository(private val dao: StudyGoalDao) {

    /** The current weekly goal, or null if the user hasn't set one. */
    fun weeklyGoal(): Flow<StudyGoalEntity?> = dao.goalForScope(GoalScope.WEEKLY.name)

    /**
     * Replaces the weekly goal. This deletes any existing weekly-goal row
     * first rather than relying on `@Insert(REPLACE)`: SQLite's unique index
     * never considers two NULLs equal, so a `REPLACE` insert against
     * `(scope, subjectId)` with `subjectId = NULL` never collides with a
     * prior weekly-goal row — it would silently add a second one instead of
     * overwriting the first. See [com.studyspace.timer.data.db.StudyGoalDao].
     */
    suspend fun setWeeklyGoalMinutes(minutes: Int) {
        dao.deleteScope(GoalScope.WEEKLY.name)
        dao.upsert(
            StudyGoalEntity(
                scope = GoalScope.WEEKLY.name,
                subjectId = null,
                targetMinutes = minutes,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearWeeklyGoal() {
        dao.deleteScope(GoalScope.WEEKLY.name)
    }

    companion object {
        /** Preset chips on the weekly-goal picker, same idea as [com.studyspace.timer.settings.SettingsRepository.DAILY_GOAL_PRESET_MINUTES]. */
        val WEEKLY_GOAL_PRESET_HOURS = listOf(5, 10, 15, 20, 25, 30, 35, 40)
    }
}
