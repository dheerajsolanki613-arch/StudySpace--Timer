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
 * Which screen Focus Mode's UI should show. Not derived purely from
 * [TimerUiState.runState] because the *setup* screen (duration picker,
 * before a session has ever started) and the *completion* screen (right
 * after one ends) both count as "not running" but need to be shown as
 * distinct screens rather than collapsing back to the same setup UI the
 * instant a countdown reaches [TimerRunState.COMPLETED].
 */
enum class FocusStage { SETUP, ACTIVE, COMPLETED }

/** In-progress duration picked on the setup screen, before it's validated
 *  and turned into a countdown target by [FocusDuration.validate]. */
data class FocusDurationInput(val minutes: Int, val seconds: Int)

/**
 * Strict Focus Mode: a countdown session built on the same [TimerEngine]
 * every other timer mode uses (Self-Study/Online Study/Normal/Pomodoro),
 * plus [FocusLockController] engagement while it's running/paused so the
 * rest of the app — bottom nav, the system back gesture — locks onto this
 * screen until the timer finishes naturally or the user deliberately
 * confirms [emergencyExit]. See [com.studyspace.timer.screens.focus.FocusScreen]
 * for where that lock is actually surfaced, and
 * [com.studyspace.timer.navigation.StudySpaceNavHost] for where it's
 * enforced against navigation.
 *
 * Survives recomposition and configuration changes exactly the way
 * [CountdownTimerViewModel] does: this class is an `AndroidViewModel`
 * living in `viewModelScope`, and [TimerEngine] uses
 * `SystemClock.elapsedRealtime()` — not anything tied to a particular
 * Activity instance — as its source of truth, so rotating the device
 * mid-session doesn't reset or re-time anything.
 *
 * Process death / device restart is a known, explicit limitation shared by
 * every timer mode in this app (see `PROJECT_STATE.md`'s "Known gaps" —
 * a session is only written to Room when it *ends*, and nothing persists
 * an in-progress start timestamp to disk anywhere yet). This class doesn't
 * change that; building real disk-backed mid-session recovery would mean
 * touching [ActiveTimerSession] and
 * [com.studyspace.timer.service.TimerForegroundService] for every mode,
 * not just Focus — out of scope for this feature and not something to
 * silently half-implement. What Focus Mode specifically *does* guard
 * against instead: invalid/corrupted duration input never reaching
 * [TimerEngine.start] (see [FocusDuration.validate], the one choke point
 * [confirmAndStart] goes through), and the lock never surviving longer
 * than a live ViewModel that can actually clear it (see [onCleared]'s
 * [FocusLockController.forceUnlock] safety net) — between those two, a
 * crash or a killed process can leave a session's *time* unrecovered (same
 * as any other mode today) but can never leave the user's *navigation*
 * permanently stuck.
 */
class FocusModeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.sessionRepository
    private val settingsRepository = app.settingsRepository

    private val engine = TimerEngine(scope = viewModelScope, onCompleted = { onTimerCompleted() })
    val state: StateFlow<TimerUiState> = engine.state

    private val _stage = MutableStateFlow(FocusStage.SETUP)
    val stage: StateFlow<FocusStage> = _stage.asStateFlow()

    private val _durationInput = MutableStateFlow(FocusDurationInput(minutes = DEFAULT_MINUTES, seconds = 0))
    val durationInput: StateFlow<FocusDurationInput> = _durationInput.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private var activeSessionId: String? = null
    private var lockId: String? = null
    private var sessionStartEpochMillis: Long = 0L

    /** Applies a preset chip. Only takes effect in [FocusStage.SETUP],
     *  mirroring [CountdownTimerViewModel.selectDuration]'s idle-only
     *  guard — you can't quietly change a session's length once it (or its
     *  completion screen) is already showing. */
    fun selectPresetMinutes(minutes: Int) {
        if (_stage.value != FocusStage.SETUP) return
        _durationInput.value = FocusDurationInput(minutes = minutes, seconds = 0)
        _validationError.value = null
    }

    /** Applies a custom minutes/seconds value from the stepper UI. Same
     *  setup-only guard as [selectPresetMinutes]. */
    fun updateCustomDuration(minutes: Int, seconds: Int) {
        if (_stage.value != FocusStage.SETUP) return
        _durationInput.value = FocusDurationInput(minutes = minutes, seconds = seconds)
        _validationError.value = null
    }

    /**
     * Validates the current [durationInput] via [FocusDuration.validate]
     * and starts the countdown (engaging the lock) only if it passes;
     * otherwise publishes a message through [validationError] and starts
     * nothing. This is the single path the UI's "Start Focus" confirmation
     * calls, and the only way [TimerEngine.start] is ever reached from this
     * class — see the class doc.
     */
    fun confirmAndStart() {
        if (_stage.value != FocusStage.SETUP) return
        val input = _durationInput.value
        when (val result = FocusDuration.validate(input.minutes, input.seconds)) {
            is FocusDuration.ValidationResult.Invalid -> _validationError.value = result.reason
            is FocusDuration.ValidationResult.Valid -> {
                _validationError.value = null
                beginLockedSession(result.totalMillis)
            }
        }
    }

    private fun beginLockedSession(targetMillis: Long) {
        sessionStartEpochMillis = System.currentTimeMillis()
        engine.start(targetMillis = targetMillis)
        _stage.value = FocusStage.ACTIVE
        activeSessionId = ActiveTimerSession.publish(
            label = "Focus Mode",
            state = state,
            onPause = ::pause,
            onResume = ::resume,
            // The persistent notification's "Stop" action is a second,
            // system-level way out of a locked session -- it must behave
            // exactly like the in-app emergency exit (save partial
            // progress, clear the lock), not a silent kill, so the user is
            // never stuck even if they never see the in-app dialog.
            onStop = ::emergencyExit
        )
        lockId = FocusLockController.lock()
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()

    /**
     * The one deliberate way out of a locked session before it finishes on
     * its own. Always safe to call regardless of current [stage]/engine
     * state — the UI's confirmation dialog (see `FocusScreen`) calls this
     * unconditionally once the user confirms, without first checking
     * whether there's technically anything running to exit.
     */
    fun emergencyExit() {
        val current = engine.state.value
        if ((current.isRunning || current.isPaused) && current.elapsedMillis > 0L) {
            saveSession(durationMillis = current.elapsedMillis, completedNaturally = false)
        }
        engine.reset()
        clearActiveSession()
        clearLock()
        _stage.value = FocusStage.SETUP
    }

    private fun onTimerCompleted() {
        saveSession(durationMillis = engine.state.value.elapsedMillis, completedNaturally = true)
        clearActiveSession()
        clearLock()
        _stage.value = FocusStage.COMPLETED
        maybeNotifyCompletion()
    }

    /** Called from the completion screen's "Done" button to return to a
     *  fresh [FocusStage.SETUP] for another session. */
    fun acknowledgeCompletion() {
        engine.reset()
        _stage.value = FocusStage.SETUP
    }

    /** Stage 8-style completion alert, same pattern as every other timer
     *  mode's `maybeNotifyCompletion` — gated by the same "Timer
     *  completion alerts" Settings toggle. */
    private fun maybeNotifyCompletion() {
        viewModelScope.launch {
            if (settingsRepository.timerCompletionAlertsEnabled.first()) {
                TimerNotifications.notifyCompletion(getApplication(), "Focus Mode")
            }
        }
    }

    private fun saveSession(durationMillis: Long, completedNaturally: Boolean) {
        val startedAt = sessionStartEpochMillis
        viewModelScope.launch {
            repository.recordSession(
                type = SessionType.FOCUS_MODE,
                label = "Focus Mode",
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

    private fun clearLock() {
        FocusLockController.unlock(lockId)
        lockId = null
    }

    /** Safety net, not the normal path: normal completion/[emergencyExit]
     *  already clear the lock themselves. This only fires if the ViewModel
     *  is torn down (e.g. the process is killed) while [lockId] is still
     *  held, so [FocusLockController] can never keep reporting "locked"
     *  with no ViewModel left alive to ever release it. */
    override fun onCleared() {
        clearActiveSession()
        if (lockId != null) {
            FocusLockController.forceUnlock()
            lockId = null
        }
        super.onCleared()
    }

    companion object {
        const val DEFAULT_MINUTES = 25
        val PRESET_MINUTES = FocusDuration.PRESET_MINUTES
    }
}
