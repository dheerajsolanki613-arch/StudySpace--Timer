package com.studyspace.timer.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.SessionType
import kotlinx.coroutines.launch

/**
 * Open-ended count-up timer used by Self-Study, Online Study, and Focus
 * Mode. Each screen/tab requests its own instance via a distinct
 * `viewModel(key = ...)` call, so switching tabs never resets or shares
 * another mode's progress, and ticking continues even while a different
 * tab is on screen (the engine lives in `viewModelScope`, not tied to
 * whether this composable is currently being drawn).
 *
 * Stage 4: on [start], this publishes itself to [ActiveTimerSession] so
 * [com.studyspace.timer.service.TimerForegroundService] can keep it
 * visible/controllable from a notification while the app is backgrounded.
 * [label] is supplied by the caller per screen ("Self-Study", "Online
 * Study", "Focus Mode") since one generic ViewModel class backs all three.
 *
 * Stage 5: also takes [SessionType] so [reset] can save a
 * [com.studyspace.timer.data.db.StudySessionEntity] via the shared
 * [com.studyspace.timer.data.repository.SessionRepository]. Stopwatches have
 * no target to "complete", so every save from this class is recorded with
 * `completedNaturally = false` — the elapsed time is real regardless of
 * whether the user meant to stop there, but it's never a "reached the goal"
 * moment the way a countdown or Pomodoro work phase finishing is.
 */
class StopwatchTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudySpaceApplication).sessionRepository

    private val engine = TimerEngine(scope = viewModelScope)
    val state = engine.state

    private var activeSessionId: String? = null
    private var sessionStartEpochMillis: Long = 0L
    private var currentLabel: String = "Study Session"
    private var currentType: SessionType = SessionType.SELF_STUDY

    fun start(label: String = "Study Session", type: SessionType = SessionType.SELF_STUDY) {
        currentLabel = label
        currentType = type
        sessionStartEpochMillis = System.currentTimeMillis()
        engine.start(targetMillis = 0L)
        activeSessionId = ActiveTimerSession.publish(
            label = label,
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
    }

    private fun saveSession(durationMillis: Long) {
        val type = currentType
        val label = currentLabel
        val startedAt = sessionStartEpochMillis
        viewModelScope.launch {
            repository.recordSession(
                type = type,
                label = label,
                startEpochMillis = startedAt,
                durationMillis = durationMillis,
                completedNaturally = false
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
