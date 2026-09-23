package com.studyspace.timer.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Open-ended count-up timer used by Self-Study and Online Study. Each
 * screen/tab requests its own instance via a distinct `viewModel(key = ...)`
 * call, so switching tabs never resets or shares another mode's progress,
 * and ticking continues even while a different tab is on screen (the engine
 * lives in `viewModelScope`, not tied to whether this composable is
 * currently being drawn). Focus Mode has its own
 * [com.studyspace.timer.timer.FocusModeViewModel] rather than sharing this
 * class, since it also needs [FocusLockController] engagement this class
 * has no reason to know about.
 *
 * Stage 4: on [start], this publishes itself to [ActiveTimerSession] so
 * [com.studyspace.timer.service.TimerForegroundService] can keep it
 * visible/controllable from a notification while the app is backgrounded.
 * [label] is supplied by the caller per screen ("Self-Study", "Online
 * Study") since one generic ViewModel class backs both.
 *
 * Stage 5: also takes [SessionType] so [reset] can save a
 * [com.studyspace.timer.data.db.StudySessionEntity] via the shared
 * [com.studyspace.timer.data.repository.SessionRepository]. Stopwatches have
 * no target to "complete", so every save from this class is recorded with
 * `completedNaturally = false` — the elapsed time is real regardless of
 * whether the user meant to stop there, but it's never a "reached the goal"
 * moment the way a countdown or Pomodoro work phase finishing is.
 *
 * Phase 5 (timer integration): [subjects]/[tasks] back an optional
 * attribution picker the screen shows while idle (see
 * [com.studyspace.timer.ui.components.SessionAttributionPicker]) —
 * [selectSubject]/[selectTask]/[setCustomLabel] only take effect while the
 * engine is idle, same guard every other idle-only setting in this app
 * uses (e.g. [CountdownTimerViewModel.selectDuration]). Selecting a task
 * also selects that task's own subject, since a task attributed to Physics
 * shouldn't silently end up filed under a different subject; changing the
 * subject away from a selected task's subject clears the task instead of
 * leaving a now-inconsistent pairing. Both clear back to "none" once a
 * session is saved, so the next session starts from a blank picker rather
 * than quietly reusing the last one's attribution.
 */
class StopwatchTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.sessionRepository

    private val engine = TimerEngine(scope = viewModelScope)
    val state = engine.state

    val subjects: StateFlow<List<SubjectEntity>> = app.subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = app.taskRepository.allTasks()
        .map { list -> list.filterNot { it.completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId: StateFlow<Long?> = _selectedSubjectId.asStateFlow()

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    val selectedTaskId: StateFlow<Long?> = _selectedTaskId.asStateFlow()

    private val _customLabel = MutableStateFlow("")
    val customLabel: StateFlow<String> = _customLabel.asStateFlow()

    private var activeSessionId: String? = null
    private var sessionStartEpochMillis: Long = 0L
    private var currentLabel: String = "Study Session"
    private var currentType: SessionType = SessionType.SELF_STUDY
    private var currentSubjectId: Long? = null
    private var currentTaskId: Long? = null

    fun selectSubject(subjectId: Long?) {
        if (!state.value.isIdle) return
        _selectedSubjectId.value = subjectId
        val selectedTask = tasks.value.find { it.id == _selectedTaskId.value }
        if (selectedTask != null && selectedTask.subjectId != subjectId) {
            _selectedTaskId.value = null
        }
    }

    fun selectTask(taskId: Long?) {
        if (!state.value.isIdle) return
        _selectedTaskId.value = taskId
        if (taskId != null) {
            tasks.value.find { it.id == taskId }?.subjectId?.let { _selectedSubjectId.value = it }
        }
    }

    fun setCustomLabel(text: String) {
        if (state.value.isIdle) _customLabel.value = text
    }

    fun start(label: String = "Study Session", type: SessionType = SessionType.SELF_STUDY) {
        currentLabel = _customLabel.value.ifBlank { label }
        currentType = type
        currentSubjectId = _selectedSubjectId.value
        currentTaskId = _selectedTaskId.value
        sessionStartEpochMillis = System.currentTimeMillis()
        engine.start(targetMillis = 0L)
        activeSessionId = ActiveTimerSession.publish(
            label = currentLabel,
            state = state,
            onPause = ::pause,
            onResume = ::resume,
            onStop = ::reset
        )
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()

    fun reset() {
        val elapsed = engine.state.value.elapsedMillis
        if (elapsed > 0L) {
            saveSession(elapsed)
        }
        engine.reset()
        clearActiveSession()
        _selectedSubjectId.value = null
        _selectedTaskId.value = null
        _customLabel.value = ""
    }

    private fun saveSession(durationMillis: Long) {
        val type = currentType
        val label = currentLabel
        val startedAt = sessionStartEpochMillis
        val subjectId = currentSubjectId
        val taskId = currentTaskId
        viewModelScope.launch {
            repository.recordSession(
                type = type,
                label = label,
                startEpochMillis = startedAt,
                durationMillis = durationMillis,
                completedNaturally = false,
                subjectId = subjectId,
                taskId = taskId
            )
        }
    }

    private fun clearActiveSession() {
        activeSessionId?.let { ActiveTimerSession.clear(it) }
        activeSessionId = null
    }

    override fun onCleared() {
        clearActiveSession()
        super.onCleared()
    }
}
