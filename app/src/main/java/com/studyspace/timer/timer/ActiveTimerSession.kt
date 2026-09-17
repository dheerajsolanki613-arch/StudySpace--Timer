package com.studyspace.timer.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * App-wide holder for "the one timer session the background foreground
 * service currently cares about."
 *
 * Stage 4 assumption, made explicit: only one timer session is tracked for
 * background/notification purposes at a time, even though several
 * [TimerEngine]-backed ViewModels can technically exist at once (e.g. the
 * Timer screen's three tabs). Whichever one most recently called [publish]
 * (i.e. was most recently started) is "the" active session. This matches
 * how the app is actually used — one study session at a time — rather than
 * inventing multi-session background tracking that nothing in the UI
 * surfaces anyway.
 *
 * This is a plain singleton, not a ViewModel or a Service. It holds no
 * coroutine and does no ticking of its own — it only republishes whichever
 * engine's `StateFlow<TimerUiState>` is currently active, plus the control
 * callbacks needed to drive it from outside that ViewModel (namely, from
 * [com.studyspace.timer.service.TimerForegroundService]'s notification
 * actions, which have no other way to reach a specific ViewModel instance).
 */
object ActiveTimerSession {

    data class Info(
        val id: String,
        val label: String,
        val state: StateFlow<TimerUiState>,
        val onPause: () -> Unit,
        val onResume: () -> Unit,
        val onStop: () -> Unit,
        /** Wall-clock time [publish] was called. Used only for the
         *  best-effort notification text if the process dies and the
         *  service's own onCreate re-observes a stale value momentarily;
         *  NOT used for actual elapsed-time math (the engine's
         *  SystemClock.elapsedRealtime()-based state remains the source of
         *  truth for that). */
        val startedAtEpochMillis: Long
    )

    private val _active = MutableStateFlow<Info?>(null)
    val active: StateFlow<Info?> = _active.asStateFlow()

    /**
     * Called by a [TimerEngine]-owning ViewModel when it starts a fresh
     * run. Returns a session id the caller must hold onto and pass back to
     * [clear] — this lets [clear] refuse to wipe out a *different*, newer
     * session that became active after the caller's own session ended.
     */
    fun publish(
        label: String,
        state: StateFlow<TimerUiState>,
        onPause: () -> Unit,
        onResume: () -> Unit,
        onStop: () -> Unit
    ): String {
        val id = UUID.randomUUID().toString()
        _active.value = Info(
            id = id,
            label = label,
            state = state,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            startedAtEpochMillis = System.currentTimeMillis()
        )
        return id
    }

    /** No-ops unless [id] still matches the currently active session. */
    fun clear(id: String) {
        if (_active.value?.id == id) {
            _active.value = null
        }
    }
}
