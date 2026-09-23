package com.studyspace.timer.screens.achievements

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.repository.Achievement
import com.studyspace.timer.data.repository.StreakSummary
import com.studyspace.timer.data.repository.computeAchievements
import com.studyspace.timer.data.repository.computeBestStreak
import com.studyspace.timer.data.repository.computeGoalEverMet
import com.studyspace.timer.data.repository.computeWeeklyConsistency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Backs [AchievementsScreen]. Deliberately composes four already-existing
 * flows — [com.studyspace.timer.data.repository.SessionRepository.allSessions]
 * (Phase 8), [com.studyspace.timer.data.repository.TaskRepository.allTasks]
 * (Phase 4), [com.studyspace.timer.settings.SettingsRepository.dailyGoalMinutes]
 * (Stage 8), and [com.studyspace.timer.data.repository.GoalRepository.weeklyGoal]
 * (Phase 2) — rather than adding any new Room query: every number this
 * screen needs already exists somewhere in the app, so this phase is pure
 * composition over Phases 2/4/8 plus one pre-expansion feature, same "no
 * new migration" shape Phases 7 and 8 both kept.
 *
 * [streakSummary] and [achievements] each run their own `combine()` over
 * the same four flows rather than sharing one combined flow — a small,
 * accepted amount of duplicated recomputation (grouping/summing a
 * personal-scale session list twice) in exchange for two independently
 * readable blocks, consistent with this project's stated preference for
 * simplicity over micro-optimization at this app's scale (see Phase 19's
 * "revisit if it's ever actually slow" stance elsewhere in `PROJECT_STATE.md`).
 */
class AchievementsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val sessionRepository = app.sessionRepository

    private val allSessions = sessionRepository.allSessions()
    private val tasks = app.taskRepository.allTasks()
    private val dailyGoalMinutes = app.settingsRepository.dailyGoalMinutes
    private val weeklyGoalMinutes = app.goalRepository.weeklyGoal().map { it?.targetMinutes }

    val streakSummary: StateFlow<StreakSummary> = combine(
        allSessions, tasks, dailyGoalMinutes, weeklyGoalMinutes
    ) { sessions, taskList, _, _ ->
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val weekStartEpochDay = today.minusDays(6).toEpochDay()
        val distinctDaysDesc = sessions.map { it.dateEpochDay }.distinct().sortedDescending()
        val weekSessions = sessions.filter { it.dateEpochDay >= weekStartEpochDay }

        StreakSummary(
            currentStreakDays = sessionRepository.computeStreak(distinctDaysDesc, todayEpochDay),
            bestStreakDays = computeBestStreak(distinctDaysDesc.sorted()),
            weeklyConsistencyPercent = computeWeeklyConsistency(weekSessions),
            totalFocusedMillis = sessions.sumOf { it.durationMillis },
            completedSessionCount = sessions.size,
            completedTaskCount = taskList.count { it.completed }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakSummary())

    val achievements: StateFlow<List<Achievement>> = combine(
        allSessions, tasks, dailyGoalMinutes, weeklyGoalMinutes
    ) { sessions, _, dailyGoal, weeklyGoal ->
        val today = LocalDate.now()
        val weekStartEpochDay = today.minusDays(6).toEpochDay()
        val weekTotalMillis = sessions.filter { it.dateEpochDay >= weekStartEpochDay }.sumOf { it.durationMillis }
        val distinctDaysAsc = sessions.map { it.dateEpochDay }.distinct().sorted()

        computeAchievements(
            sessionCount = sessions.size,
            totalFocusedMillis = sessions.sumOf { it.durationMillis },
            bestStreakDays = computeBestStreak(distinctDaysAsc),
            goalEverMet = computeGoalEverMet(sessions, dailyGoal, weekTotalMillis, weeklyGoal)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
