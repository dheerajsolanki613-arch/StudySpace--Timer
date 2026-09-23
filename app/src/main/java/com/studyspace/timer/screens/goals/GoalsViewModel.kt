package com.studyspace.timer.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.StudyGoalEntity
import com.studyspace.timer.data.repository.GoalProgress
import com.studyspace.timer.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs [GoalsScreen]: today's progress against the existing daily goal
 * (Stage 8, [SettingsRepository]) and progress against the new weekly goal
 * ([com.studyspace.timer.data.repository.GoalRepository]), both computed
 * from the same [com.studyspace.timer.data.repository.SessionRepository.studyStats]
 * numbers Home already reads — no new session query needed.
 *
 * The weekly goal compares against [com.studyspace.timer.data.repository.StudyStats.weekTotalMillis],
 * which is a rolling 7-day window (today plus the preceding 6), not a
 * Monday-Sunday calendar week — same window Home's "This week" stat and
 * Analytics already use, so "weekly goal" means the same thing everywhere
 * in the app rather than introducing a second definition of "week".
 */
class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val sessionRepository = app.sessionRepository
    private val settingsRepository = app.settingsRepository
    private val goalRepository = app.goalRepository

    val dailyGoalProgress: StateFlow<GoalProgress> = combine(
        sessionRepository.studyStats(),
        settingsRepository.dailyGoalMinutes
    ) { stats, dailyMinutes ->
        GoalProgress(
            label = "Today",
            currentMillis = stats.todayTotalMillis,
            targetMillis = dailyMinutes * 60_000L
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GoalProgress("Today", 0L, SettingsRepository.DEFAULT_DAILY_GOAL_MINUTES * 60_000L)
    )

    val weeklyGoal: StateFlow<StudyGoalEntity?> = goalRepository.weeklyGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weeklyGoalProgress: StateFlow<GoalProgress?> = combine(
        sessionRepository.studyStats(),
        goalRepository.weeklyGoal()
    ) { stats, goal ->
        goal?.let {
            GoalProgress(
                label = "This week",
                currentMillis = stats.weekTotalMillis,
                targetMillis = it.targetMinutes * 60_000L
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWeeklyGoalHours(hours: Int) {
        viewModelScope.launch { goalRepository.setWeeklyGoalMinutes(hours * 60) }
    }

    fun clearWeeklyGoal() {
        viewModelScope.launch { goalRepository.clearWeeklyGoal() }
    }
}
