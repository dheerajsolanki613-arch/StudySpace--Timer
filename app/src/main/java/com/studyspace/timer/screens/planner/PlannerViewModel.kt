package com.studyspace.timer.screens.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Backs [PlannerScreen]. Same `AndroidViewModel` + `StateFlow` +
 * `WhileSubscribed(5_000)` pattern as `TasksViewModel`/`SubjectsViewModel`.
 *
 * `todayEpochDay` is captured once at construction (same known limitation
 * already documented for `HomeViewModel.studyStats()` — doesn't roll over
 * at midnight if this screen is left open across the boundary; not fixed
 * here either, for the same reason: fixing it needs a live clock source
 * this project doesn't have yet, and re-deriving it is a small, contained
 * follow-up whenever that's addressed project-wide).
 */
class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.plannedSessionRepository

    private val todayEpochDay: Long = LocalDate.now().toEpochDay()

    val today: StateFlow<List<PlannedSessionEntity>> = repository.forDate(todayEpochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcoming: StateFlow<List<PlannedSessionEntity>> = repository.upcoming(todayEpochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = app.subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = app.taskRepository.allTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val nowMinuteOfDay = LocalTime.now().let { it.hour * 60 + it.minute }
        viewModelScope.launch {
            repository.refreshMissedSessions(todayEpochDay, nowMinuteOfDay)
        }
    }

    fun createPlan(
        dateEpochDay: Long,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        subjectId: Long?,
        taskId: Long?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.createPlan(dateEpochDay, startMinuteOfDay, durationMinutes, subjectId, taskId, notes)
        }
    }

    fun updatePlan(
        existing: PlannedSessionEntity,
        dateEpochDay: Long,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        subjectId: Long?,
        taskId: Long?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.updatePlan(existing, dateEpochDay, startMinuteOfDay, durationMinutes, subjectId, taskId, notes)
        }
    }

    fun markCompleted(plan: PlannedSessionEntity) {
        viewModelScope.launch { repository.markCompleted(plan) }
    }

    fun cancelPlan(plan: PlannedSessionEntity) {
        viewModelScope.launch { repository.cancelPlan(plan) }
    }

    fun deletePlan(plan: PlannedSessionEntity) {
        viewModelScope.launch { repository.deletePlan(plan) }
    }
}
