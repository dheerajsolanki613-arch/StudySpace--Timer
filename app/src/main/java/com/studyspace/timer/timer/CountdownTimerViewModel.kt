package com.studyspace.timer.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.service.TimerNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Countdown timer backing the "Normal Timer" mode. The duration is picked
 * from a fixed preset list before starting (custom arbitrary durations are
 * a Stage 8/settings-era refinement); once running it behaves like any
 * other [TimerEngine]-backed timer.
 *
 * Stage 4: on [start], publishes itself to [ActiveTimerSession] the same
 * way [StopwatchTimerViewModel] does, so a running Normal Timer keeps
 * counting and stays controllable from the background-service notification.
 *
 * Stage 5: records a [com.studyspace.timer.data.db.StudySessionEntity] two
 * ways — [onTimerCompleted] when the countdown reaches zero on its own
 * (`completedNaturally = true`), or from [reset] if the user stops it early
 * while running/paused with some progress made (`completedNaturally =
 * false`). A [reset] called *after* natural completion (the "Reset" button
 * shown once the countdown hits zero) does not double-save, since
 * [onTimerCompleted] already recorded that run.
 *
 * Stage 8: [onTimerCompleted] also posts a one-shot completion notification
 * via [TimerNotifications.notifyCompletion], gated by the "Timer completion
 * alerts" Settings toggle.
 */
class CountdownTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.sessionRepository
    private val settingsRepository = app.settingsRepository

    private val engine = TimerEngine(scope = viewModelScope, onCompleted = { onTimerCompleted() })
    val state = engine.state

    private val _selectedDurationMillis = MutableStateFlow(DEFAULT_DURATION_MILLIS)
    val selectedDurationMillis: StateFlow<Long> = _selectedDurationMillis.asStateFlow()

    private var activeSessionId: String? = null
    private var sessionStartEpochMillis: Long = 0L

    /** Only takes effect while idle, mirroring what the UI enables. */
    fun selectDuration(millis: Long) {
        if (state.value.isIdle) {
            _selectedDurationMillis.value = millis
        }
    }

    fun start() {
        sessionStartEpochMillis = System.currentTimeMillis()
        engine.start(targetMillis = _selectedDurationMillis.value)
        activeSessionId = ActiveTimerSession.publish(
            label = "Normal Timer",
            state = state,
            onPause = ::pause,
            onResume = ::resume,
            onStop = ::reset
        )
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()

    fun reset() {
        val current = engine.state.value
        if ((current.isRunning || current.isPaused) && current.elapsedMillis > 0L) {
            saveSession(durationMillis = current.elapsedMillis, completedNaturally = false)
        }
        engine.reset()
        clearActiveSession()
    }

    private fun onTimerCompleted() {
        saveSession(durationMillis = engine.state.value.elapsedMillis, completedNaturally = true)
        clearActiveSession()
        maybeNotifyCompletion()
    }

    /** Stage 8: posts a one-shot completion alert if the user has that
     *  Settings toggle on. See [TimerNotifications.notifyCompletion] for why
     *  this is called directly here rather than observed from the service. */
    private fun maybeNotifyCompletion() {
        viewModelScope.launch {
            if (settingsRepository.timerCompletionAlertsEnabled.first()) {
                TimerNotifications.notifyCompletion(getApplication(), "Normal Timer")
            }
        }
    }

    private fun saveSession(durationMillis: Long, completedNaturally: Boolean) {
        val startedAt = sessionStartEpochMillis
        viewModelScope.launch {
            repository.recordSession(
                type = SessionType.NORMAL_TIMER,
                label = "Normal Timer",
                startEpochMillis = startedAt,
                durationMillis = durationMillis,
                completedNaturally = completedNaturally
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

    companion object {
        val PRESET_MINUTES = listOf(5, 10, 15, 25, 45, 60)
        const val DEFAULT_DURATION_MILLIS = 25 * 60 * 1000L
    }
}
