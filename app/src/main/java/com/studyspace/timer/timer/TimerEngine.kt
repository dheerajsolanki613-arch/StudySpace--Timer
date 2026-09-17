package com.studyspace.timer.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Whether a [TimerEngine] counts up indefinitely or down to a target. */
enum class TimerDirection { COUNT_UP, COUNT_DOWN }

/** Lifecycle state of a running timer. */
enum class TimerRunState { IDLE, RUNNING, PAUSED, COMPLETED }

/**
 * Immutable snapshot of a timer at a point in time.
 *
 * @param elapsedMillis time actually spent running (pause time is excluded).
 * @param targetMillis for [TimerDirection.COUNT_DOWN], the duration being
 *   counted down from; 0 for [TimerDirection.COUNT_UP] (unbounded stopwatch).
 */
data class TimerUiState(
    val direction: TimerDirection = TimerDirection.COUNT_UP,
    val runState: TimerRunState = TimerRunState.IDLE,
    val elapsedMillis: Long = 0L,
    val targetMillis: Long = 0L
) {
    /** Milliseconds left for a countdown; 0 for count-up timers. */
    val remainingMillis: Long
        get() = if (direction == TimerDirection.COUNT_DOWN) {
            (targetMillis - elapsedMillis).coerceAtLeast(0L)
        } else {
            0L
        }

    /** 0f..1f fraction complete; only meaningful for COUNT_DOWN. */
    val progress: Float
        get() = if (direction == TimerDirection.COUNT_DOWN && targetMillis > 0L) {
            (elapsedMillis.toFloat() / targetMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isRunning: Boolean get() = runState == TimerRunState.RUNNING
    val isPaused: Boolean get() = runState == TimerRunState.PAUSED
    val isCompleted: Boolean get() = runState == TimerRunState.COMPLETED
    val isIdle: Boolean get() = runState == TimerRunState.IDLE
}

/**
 * Drift-resistant timer core shared by every timer mode and Pomodoro phase
 * in the app (Self-Study, Online Study, Normal, Pomodoro, Focus Mode).
 *
 * Uses [SystemClock.elapsedRealtime] — monotonic and immune to wall-clock or
 * timezone changes — as the source of truth for elapsed time, rather than
 * accumulating fixed per-tick increments. This means the displayed time
 * stays accurate even if an individual tick is delayed (e.g. by a GC pause
 * or a slow recomposition), instead of silently drifting behind.
 *
 * Exactly one coroutine [Job] runs at a time: [start] and [resume] always
 * cancel any prior job before launching a new one, so two tickers can never
 * run concurrently for the same engine.
 *
 * This class is intentionally NOT a ViewModel — it's owned and driven by one
 * (see [StopwatchTimerViewModel], [CountdownTimerViewModel], and
 * [PomodoroViewModel]), using that ViewModel's `viewModelScope` so the
 * ticking coroutine is cancelled automatically in `onCleared()`. It has no
 * knowledge of Android lifecycle beyond that — surviving app backgrounding
 * or process death is a Stage 4 (foreground service) concern, not this
 * engine's.
 */
class TimerEngine(
    private val scope: CoroutineScope,
    private val tickIntervalMillis: Long = 200L,
    private val onCompleted: () -> Unit = {}
) {
    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var runStartRealtime: Long = 0L
    private var accumulatedMillis: Long = 0L

    /**
     * Begin a fresh run from zero. Pass [targetMillis] > 0 to run as a
     * countdown; 0 (the default) runs as an open-ended stopwatch.
     */
    fun start(targetMillis: Long = 0L) {
        tickJob?.cancel()
        accumulatedMillis = 0L
        runStartRealtime = SystemClock.elapsedRealtime()
        _state.value = TimerUiState(
            direction = if (targetMillis > 0L) TimerDirection.COUNT_DOWN else TimerDirection.COUNT_UP,
            runState = TimerRunState.RUNNING,
            elapsedMillis = 0L,
            targetMillis = targetMillis
        )
        launchTicker()
    }

    /** Freeze the timer at its current elapsed time. No-op unless running. */
    fun pause() {
        val current = _state.value
        if (!current.isRunning) return
        tickJob?.cancel()
        accumulatedMillis = currentElapsed()
        _state.value = current.copy(runState = TimerRunState.PAUSED, elapsedMillis = accumulatedMillis)
    }

    /** Continue from where [pause] left off. No-op unless paused. */
    fun resume() {
        val current = _state.value
        if (!current.isPaused) return
        runStartRealtime = SystemClock.elapsedRealtime()
        _state.value = current.copy(runState = TimerRunState.RUNNING)
        launchTicker()
    }

    /** Stop and clear back to a fresh idle state at zero. */
    fun reset() {
        tickJob?.cancel()
        accumulatedMillis = 0L
        _state.value = TimerUiState()
    }

    private fun currentElapsed(): Long =
        accumulatedMillis + (SystemClock.elapsedRealtime() - runStartRealtime)

    private fun launchTicker() {
        tickJob = scope.launch {
            while (isActive) {
                val elapsed = currentElapsed()
                val current = _state.value
                if (current.direction == TimerDirection.COUNT_DOWN && elapsed >= current.targetMillis) {
                    _state.value = current.copy(
                        runState = TimerRunState.COMPLETED,
                        elapsedMillis = current.targetMillis
                    )
                    onCompleted()
                    break
                }
                _state.value = current.copy(elapsedMillis = elapsed)
                delay(tickIntervalMillis)
            }
        }
    }
}
