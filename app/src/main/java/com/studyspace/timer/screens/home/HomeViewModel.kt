package com.studyspace.timer.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.data.repository.StudyStats
import com.studyspace.timer.data.repository.SubjectTotal
import com.studyspace.timer.data.repository.computeSubjectTotals
import com.studyspace.timer.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Backs [HomeScreen]'s real numbers (Stage 5) — today's total, session
 * count, this-week total, streak, and recent activity — all read from Room
 * via [com.studyspace.timer.data.repository.SessionRepository] instead of
 * the static placeholders Stage 2 shipped with.
 *
 * `WhileSubscribed(5_000)` on both flows means the underlying Room queries
 * stay alive briefly across quick navigation away/back (e.g. switching bottom
 * nav tabs) instead of restarting from empty every time, while still
 * stopping once nobody's observing for good.
 *
 * Stage 8: [dailyGoalMillis] reads the real, user-configurable goal from
 * [SettingsRepository] instead of `HomeScreen.kt`'s old fixed
 * `DAILY_GOAL_MILLIS` constant. Default (240 min / 4h) is identical to that
 * old constant, so a user who never opens Settings sees no change.
 *
 * Phase 10 (Daily Dashboard) additions — [todaySubjectTotals],
 * [subjects], [todaysPlans], [remainingTasks] — each reuses an
 * already-existing repository query rather than adding a new one:
 * [todaySubjectTotals] reuses [com.studyspace.timer.data.repository.SessionRepository.sessionsSince]
 * (Phase 7) with today's own epoch day as the lower bound — since no real
 * recorded session is ever future-dated, that's just "today's sessions"
 * with no new query needed — through the same pure, unit-tested
 * [computeSubjectTotals] Phase 8 already built for Analytics' "By Subject"
 * section. [todaysPlans] reuses [com.studyspace.timer.data.repository.PlannedSessionRepository.forDate]
 * (Phase 6). [remainingTasks] reuses [com.studyspace.timer.data.repository.TaskRepository.allTasks]
 * (Phase 4), filtered/sorted client-side — same "small dataset, filter in
 * Kotlin" convention every prior list screen in this app already follows.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.sessionRepository
    private val settingsRepository = app.settingsRepository

    val stats: StateFlow<StudyStats> = repository.studyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyStats())

    val recentSessions: StateFlow<List<StudySessionEntity>> = repository.recentSessions(limit = 6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyGoalMillis: StateFlow<Long> = settingsRepository.dailyGoalMinutes
        .map { minutes -> minutes * 60_000L }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_DAILY_GOAL_MINUTES * 60_000L
        )

    val todaySubjectTotals: StateFlow<List<SubjectTotal>> = repository.sessionsSince(LocalDate.now().toEpochDay())
        .map { computeSubjectTotals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = app.subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todaysPlans: StateFlow<List<PlannedSessionEntity>> = app.plannedSessionRepository.forDate(LocalDate.now().toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Incomplete tasks, soonest deadline first (no-deadline tasks last), capped at 5 for a dashboard-sized list. */
    val remainingTasks: StateFlow<List<TaskEntity>> = app.taskRepository.allTasks()
        .map { tasks ->
            tasks.filter { !it.completed }
                .sortedBy { it.deadlineEpochDay ?: Long.MAX_VALUE }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

