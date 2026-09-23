package com.studyspace.timer.screens.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.repository.DailySummary
import com.studyspace.timer.data.repository.computeDailySummary
import com.studyspace.timer.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Backs [DailySummaryScreen] (Phase 11). Composes three already-existing
 * flows — [com.studyspace.timer.data.repository.SessionRepository.sessionsSince]
 * (Phase 7), [com.studyspace.timer.data.repository.TaskRepository.allTasks]
 * (Phase 4) and [SettingsRepository.dailyGoalMinutes] (Stage 8) — through
 * the pure, unit-tested [computeDailySummary]. No new Room query.
 *
 * The session query is re-subscribed (via `flatMapLatest`) whenever the
 * selected day changes, and is bounded below by that day, so viewing today
 * or yesterday never loads the whole session history — unlike the unbounded
 * `allSessions()` the Analytics and Achievements screens read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailySummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val subjects: StateFlow<List<SubjectEntity>> = app.subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<DailySummary> = _selectedDate
        .flatMapLatest { date ->
            combine(
                app.sessionRepository.sessionsSince(date.toEpochDay()),
                app.taskRepository.allTasks(),
                app.settingsRepository.dailyGoalMinutes
            ) { sessions, tasks, goalMinutes ->
                computeDailySummary(sessions, tasks, goalMinutes * 60_000L, date)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            computeDailySummary(
                sessions = emptyList(),
                tasks = emptyList(),
                dailyGoalMillis = SettingsRepository.DEFAULT_DAILY_GOAL_MINUTES * 60_000L,
                date = _selectedDate.value
            )
        )

    fun showPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    /** No-op on today — a summary for a day that hasn't happened yet would only ever be empty. */
    fun showNextDay() {
        val next = _selectedDate.value.plusDays(1)
        if (!next.isAfter(LocalDate.now())) _selectedDate.value = next
    }

    fun showToday() {
        _selectedDate.value = LocalDate.now()
    }
}
