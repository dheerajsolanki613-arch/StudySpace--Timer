package com.studyspace.timer.screens.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.planning.AvailabilityBlock
import com.studyspace.timer.planning.PlannableTask
import com.studyspace.timer.planning.SmartPlanRequest
import com.studyspace.timer.planning.SuggestedSession
import com.studyspace.timer.planning.UnscheduledRemainder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** One named, one-tap availability choice. Selecting several unions their blocks. */
data class AvailabilityPreset(val label: String, val blocks: List<AvailabilityBlock>)

private fun weekdays(start: Int, end: Int) =
    listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        .map { AvailabilityBlock(it, start, end) }

private fun weekend(start: Int, end: Int) =
    listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).map { AvailabilityBlock(it, start, end) }

private fun everyDay(start: Int, end: Int) =
    DayOfWeek.values().map { AvailabilityBlock(it, start, end) }

/**
 * Phase 17 — a small, fixed catalog of common availability windows, rather
 * than a free-form weekly calendar editor: this keeps "tell the app when
 * you're free" to a few taps instead of a day-by-day time-range builder,
 * while still producing real [AvailabilityBlock]s that drive real scheduling
 * output. A person with unusual hours can select several presets and get a
 * reasonable approximation, or just decline all of them (an empty selection
 * simply means [com.studyspace.timer.planning.SmartPlanner] has nothing to suggest into — handled as
 * [SmartPlanUiState.NothingToSuggest], not a crash).
 */
val AVAILABILITY_PRESETS: List<AvailabilityPreset> = listOf(
    AvailabilityPreset("Weekday mornings", weekdays(6 * 60, 8 * 60)),
    AvailabilityPreset("Weekday evenings", weekdays(18 * 60, 21 * 60)),
    AvailabilityPreset("Weekend mornings", weekend(9 * 60, 12 * 60)),
    AvailabilityPreset("Weekend afternoons", weekend(13 * 60, 17 * 60)),
    AvailabilityPreset("Late nights, every day", everyDay(21 * 60, 23 * 60))
)

const val MIN_DAYS_AHEAD = 3
const val DEFAULT_DAYS_AHEAD = 7
const val MAX_DAYS_AHEAD = 14

/** One row in the preview: a [SuggestedSession] plus the display text it needs and whether the user still wants it. */
data class SuggestionItem(
    val session: SuggestedSession,
    val taskTitle: String,
    val subjectName: String?,
    val accepted: Boolean
)

sealed interface SmartPlanUiState {
    /** [selectedPresets] and [daysAhead] are pre-filled from last time, if any — see [AvailabilityRepository]. */
    data class Setup(val selectedPresets: Set<String>, val daysAhead: Int) : SmartPlanUiState
    data object Generating : SmartPlanUiState
    data class Preview(val items: List<SuggestionItem>, val unscheduled: List<UnscheduledRemainder>) : SmartPlanUiState
    /** No incomplete tasks, or no availability selected, or availability that fits nothing — distinguished only by [reason]'s text, no separate data needed. */
    data class NothingToSuggest(val reason: String) : SmartPlanUiState
    data object Committing : SmartPlanUiState
    data class Done(val addedCount: Int) : SmartPlanUiState
}

class SmartPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val availabilityRepository = app.availabilityRepository
    private val taskRepository = app.taskRepository
    private val subjectRepository = app.subjectRepository
    private val plannedSessionRepository = app.plannedSessionRepository
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow<SmartPlanUiState>(SmartPlanUiState.Setup(emptySet(), DEFAULT_DAYS_AHEAD))
    val uiState: StateFlow<SmartPlanUiState> = _uiState

    init {
        viewModelScope.launch {
            val storedBlocks = availabilityRepository.availability.first()
            if (storedBlocks.isNotEmpty()) {
                val matchingPresets = AVAILABILITY_PRESETS
                    .filter { preset -> preset.blocks.all { it in storedBlocks } }
                    .map { it.label }
                    .toSet()
                _uiState.value = SmartPlanUiState.Setup(matchingPresets, DEFAULT_DAYS_AHEAD)
            }
        }
    }

    /** Generates suggestions from [selectedPresets]/[daysAhead] against the app's real, current tasks and plans. Writes nothing. */
    fun generate(selectedPresets: Set<String>, daysAhead: Int) {
        val horizon = daysAhead.coerceIn(MIN_DAYS_AHEAD, MAX_DAYS_AHEAD)
        val availability = AVAILABILITY_PRESETS.filter { it.label in selectedPresets }.flatMap { it.blocks }
        _uiState.value = SmartPlanUiState.Generating

        viewModelScope.launch {
            availabilityRepository.setAvailability(availability)

            if (availability.isEmpty()) {
                _uiState.value = SmartPlanUiState.NothingToSuggest(
                    "Choose at least one time you're generally free to get suggestions."
                )
                return@launch
            }

            val tasks = taskRepository.allTasks().first().filter { !it.completed }
            if (tasks.isEmpty()) {
                _uiState.value = SmartPlanUiState.NothingToSuggest(
                    "There are no open tasks to schedule right now — add one from the Tasks tab first."
                )
                return@launch
            }

            val subjects = subjectRepository.allSubjects().first()
            val subjectNameById = subjects.associate { it.id to it.name }

            val today = LocalDate.now().toEpochDay()
            val existingPlans = plannedSessionRepository.sessionsSince(today).first()
            val alreadyPlannedMinutesByDay = existingPlans
                .groupBy { it.dateEpochDay }
                .mapValues { (_, plans) -> plans.sumOf { it.durationMinutes } }

            val dailyGoalMinutes = settingsRepository.dailyGoalMinutes.first()
            val maxDailyMinutes = dailyGoalMinutes.coerceAtLeast(30)

            val plannableTasks = tasks.map { task ->
                PlannableTask(
                    taskId = task.id,
                    title = task.title,
                    subjectId = task.subjectId,
                    priority = TaskPriority.entries.firstOrNull { it.name == task.priority } ?: TaskPriority.MEDIUM,
                    deadlineEpochDay = task.deadlineEpochDay,
                    estimatedDurationMinutes = task.estimatedDurationMinutes
                )
            }

            val result = app.planGenerator.generate(
                plannableTasks,
                SmartPlanRequest(
                    startEpochDay = today,
                    daysAhead = horizon,
                    availability = availability,
                    maxDailyMinutes = maxDailyMinutes,
                    alreadyPlannedMinutesByDay = alreadyPlannedMinutesByDay
                )
            )

            if (result.suggestions.isEmpty()) {
                _uiState.value = SmartPlanUiState.NothingToSuggest(
                    "Nothing fit your chosen times in the next $horizon days — try adding another availability window or a longer horizon."
                )
                return@launch
            }

            val taskById = tasks.associateBy { it.id }
            val items = result.suggestions.map { session ->
                val task = taskById[session.taskId]
                SuggestionItem(
                    session = session,
                    taskTitle = task?.title ?: "Untitled task",
                    subjectName = session.subjectId?.let { subjectNameById[it] },
                    accepted = true
                )
            }
            _uiState.value = SmartPlanUiState.Preview(items, result.unscheduled)
        }
    }

    /** Flips one suggestion's accepted state in place, keeping every other row untouched. */
    fun toggleAccepted(session: SuggestedSession) {
        val state = _uiState.value as? SmartPlanUiState.Preview ?: return
        _uiState.value = state.copy(
            items = state.items.map { if (it.session == session) it.copy(accepted = !it.accepted) else it }
        )
    }

    fun backToSetup(selectedPresets: Set<String>, daysAhead: Int) {
        _uiState.value = SmartPlanUiState.Setup(selectedPresets, daysAhead)
    }

    /**
     * Writes only the accepted suggestions, each via the same
     * `PlannedSessionRepository.createPlan` a manually created plan uses —
     * so every accepted suggestion becomes an ordinary planned session,
     * editable and deletable from the Planner exactly like one the user
     * typed in themselves. Declined suggestions are simply never written.
     */
    fun commit() {
        val state = _uiState.value as? SmartPlanUiState.Preview ?: return
        val accepted = state.items.filter { it.accepted }
        _uiState.value = SmartPlanUiState.Committing

        viewModelScope.launch {
            accepted.forEach { item ->
                plannedSessionRepository.createPlan(
                    dateEpochDay = item.session.dateEpochDay,
                    startMinuteOfDay = item.session.startMinuteOfDay,
                    durationMinutes = item.session.durationMinutes,
                    subjectId = item.session.subjectId,
                    taskId = item.session.taskId,
                    notes = null
                )
            }
            _uiState.value = SmartPlanUiState.Done(accepted.size)
        }
    }
}
