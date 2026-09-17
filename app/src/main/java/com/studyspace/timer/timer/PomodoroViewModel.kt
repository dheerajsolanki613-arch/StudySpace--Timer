package com.studyspace.timer.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.service.TimerNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which part of the Pomodoro cycle is currently active. */
enum class PomodoroPhase { WORK, SHORT_BREAK, LONG_BREAK }

data class PomodoroUiState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val completedWorkSessions: Int = 0,
    val timer: TimerUiState = TimerUiState()
)

/**
 * Work/break cycle manager built on the shared [TimerEngine]. Defaults to
 * 25m work / 5m short break / 15m long break every 4th completed work
 * session (Stage 3); Stage 7 adds [selectDurationMinutes] so each phase's
 * length is user-selectable from a preset list while idle — the cycling
 * logic itself (which phase comes next, every-4th-session long break)
 * doesn't change. Duration choices live in memory only for now — surviving
 * an app restart needs DataStore, which is Stage 8's job.
 *
 * When a phase's countdown completes, [advanceToNextPhase] runs and the
 * engine resets to idle for the new phase — the user starts the next phase
 * explicitly rather than it auto-starting, so a completed Pomodoro session
 * never silently keeps running unattended.
 *
 * Stage 4: [start] publishes to [ActiveTimerSession] with a label reflecting
 * the *current* phase ("Pomodoro — Work", "Pomodoro — Short Break", etc.).
 * Because a phase completing already returns the engine to idle (see
 * above), the active session is cleared at that point too — the background
 * notification correctly disappears between phases until the user taps
 * Start for the next one, rather than implying a phase is running when it
 * isn't.
 *
 * Stage 5: only **Work** phases are ever recorded as study sessions — break
 * time isn't study time, so [onPhaseCompleted] and [reset] both skip saving
 * during Short/Long Break regardless of elapsed time. A Work phase is saved
 * two ways: [onPhaseCompleted] when it finishes on its own
 * (`completedNaturally = true`, full 25m), or from [reset] if the user
 * stops mid-Work with some progress made (`completedNaturally = false`,
 * partial duration).
 *
 * Stage 8: [onPhaseCompleted] also posts a one-shot completion notification
 * (via [TimerNotifications.notifyCompletion]) for *every* finished phase —
 * Work and both break types — gated by the "Timer completion alerts"
 * Settings toggle. This is separate from, and unaffected by, the
 * Work-only session-recording rule described above.
 */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudySpaceApplication).sessionRepository
    private val settingsRepository = application.settingsRepository

    private val engine: TimerEngine = TimerEngine(
        scope = viewModelScope,
        onCompleted = { onPhaseCompleted() }
    )

    private val _phase = MutableStateFlow(PomodoroPhase.WORK)
    private val _completedWorkSessions = MutableStateFlow(0)

    // Stage 7: user-selectable durations per phase, in-memory only for now
    // (persisting the choice across app restarts is Stage 8's DataStore
    // settings job — this is the cycling/engine half of "custom durations").
    // Defaults match the Stage 3 fixed values so behavior is unchanged
    // until the user actually picks something different.
    private val _workMinutes = MutableStateFlow(DEFAULT_WORK_MINUTES)
    private val _shortBreakMinutes = MutableStateFlow(DEFAULT_SHORT_BREAK_MINUTES)
    private val _longBreakMinutes = MutableStateFlow(DEFAULT_LONG_BREAK_MINUTES)
    val workMinutes: StateFlow<Int> = _workMinutes
    val shortBreakMinutes: StateFlow<Int> = _shortBreakMinutes
    val longBreakMinutes: StateFlow<Int> = _longBreakMinutes

    private var activeSessionId: String? = null
    private var sessionStartEpochMillis: Long = 0L

    val uiState: StateFlow<PomodoroUiState> = combine(
        engine.state, _phase, _completedWorkSessions
    ) { timer, phase, completed ->
        PomodoroUiState(phase = phase, completedWorkSessions = completed, timer = timer)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PomodoroUiState())

    /**
     * Changes the duration of whichever phase [phase] names. Only takes
     * effect while idle, mirroring [com.studyspace.timer.timer.CountdownTimerViewModel.selectDuration] —
     * a running/paused phase keeps its already-started length. Applies from
     * the *next* time that phase is entered, so changing e.g. Short Break
     * mid-Work-session is safe and doesn't retroactively alter anything.
     */
    fun selectDurationMinutes(phase: PomodoroPhase, minutes: Int) {
        if (!engine.state.value.isIdle) return
        when (phase) {
            PomodoroPhase.WORK -> _workMinutes.value = minutes
            PomodoroPhase.SHORT_BREAK -> _shortBreakMinutes.value = minutes
            PomodoroPhase.LONG_BREAK -> _longBreakMinutes.value = minutes
        }
    }

    fun start() {
        sessionStartEpochMillis = System.currentTimeMillis()
        engine.start(targetMillis = durationFor(_phase.value))
        activeSessionId = ActiveTimerSession.publish(
            label = "Pomodoro \u2014 ${phaseLabel(_phase.value)}",
            state = engine.state,
            onPause = ::pause,
            onResume = ::resume,
            onStop = ::reset
        )
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()

    /** Stops the current phase and returns the whole cycle to Work #1. */
    fun reset() {
        val current = engine.state.value
        if (_phase.value == PomodoroPhase.WORK && (current.isRunning || current.isPaused) && current.elapsedMillis > 0L) {
            saveWorkSession(durationMillis = current.elapsedMillis, completedNaturally = false)
        }
        engine.reset()
        _phase.value = PomodoroPhase.WORK
        _completedWorkSessions.value = 0
        clearActiveSession()
    }

    private fun onPhaseCompleted() {
        val finishedPhase = _phase.value
        if (finishedPhase == PomodoroPhase.WORK) {
            saveWorkSession(durationMillis = durationFor(PomodoroPhase.WORK), completedNaturally = true)
        }
        advanceToNextPhase()
        engine.reset()
        clearActiveSession()
        maybeNotifyCompletion(finishedPhase)
    }

    /** Stage 8: posts a one-shot completion alert (if enabled in Settings)
     *  whenever *any* phase ends, not just Work — a Pomodoro user needs to
     *  know a break finished just as much as a work session, since the app
     *  deliberately never auto-starts the next phase (see class doc). See
     *  [TimerNotifications.notifyCompletion] for why this is called
     *  directly here rather than observed from the background service. */
    private fun maybeNotifyCompletion(finishedPhase: PomodoroPhase) {
        viewModelScope.launch {
            if (settingsRepository.timerCompletionAlertsEnabled.first()) {
                TimerNotifications.notifyCompletion(getApplication(), "Pomodoro \u2014 ${phaseLabel(finishedPhase)}")
            }
        }
    }

    private fun saveWorkSession(durationMillis: Long, completedNaturally: Boolean) {
        val startedAt = sessionStartEpochMillis
        viewModelScope.launch {
            repository.recordSession(
                type = SessionType.POMODORO,
                label = "Pomodoro \u2014 Work",
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

    private fun advanceToNextPhase() {
        if (_phase.value == PomodoroPhase.WORK) {
            val completed = _completedWorkSessions.value + 1
            _completedWorkSessions.value = completed
            _phase.value = if (completed % SESSIONS_PER_LONG_BREAK == 0) {
                PomodoroPhase.LONG_BREAK
            } else {
                PomodoroPhase.SHORT_BREAK
            }
        } else {
            _phase.value = PomodoroPhase.WORK
        }
    }

    private fun durationFor(phase: PomodoroPhase): Long = when (phase) {
        PomodoroPhase.WORK -> _workMinutes.value * 60_000L
        PomodoroPhase.SHORT_BREAK -> _shortBreakMinutes.value * 60_000L
        PomodoroPhase.LONG_BREAK -> _longBreakMinutes.value * 60_000L
    }

    private fun phaseLabel(phase: PomodoroPhase): String = when (phase) {
        PomodoroPhase.WORK -> "Work"
        PomodoroPhase.SHORT_BREAK -> "Short Break"
        PomodoroPhase.LONG_BREAK -> "Long Break"
    }

    companion object {
        const val DEFAULT_WORK_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val SESSIONS_PER_LONG_BREAK = 4

        // Stage 7: preset choices shown as chips per phase while idle. Kept
        // as short, sensible lists rather than a free-entry field — an
        // arbitrary-value entry UI is a Stage 8/settings-era refinement,
        // consistent with how CountdownTimerViewModel.PRESET_MINUTES works
        // for the Normal Timer.
        val WORK_PRESET_MINUTES = listOf(15, 20, 25, 30, 45, 60)
        val SHORT_BREAK_PRESET_MINUTES = listOf(3, 5, 10, 15)
        val LONG_BREAK_PRESET_MINUTES = listOf(10, 15, 20, 30)
    }
}
