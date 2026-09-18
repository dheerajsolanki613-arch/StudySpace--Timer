package com.studyspace.timer.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * App-wide "is Strict Focus Mode currently locking navigation" flag.
 *
 * Same plain-singleton shape as [ActiveTimerSession] and for the same
 * reason: the thing that needs to react to this (the bottom nav bar and
 * the system back gesture, both wired up in
 * [com.studyspace.timer.navigation.StudySpaceNavHost]) lives above and
 * outside any single screen's ViewModel, so a `StateFlow` on a singleton
 * object is the simplest reliable shared source of truth — a reliable
 * shared session manager, not just a visual "disable this button" flag
 * scoped to one composable, which is what actually stops someone from
 * tapping through to another tab while a lock is meant to be held.
 *
 * Ownership uses the same id-token pattern as [ActiveTimerSession.publish]/
 * [ActiveTimerSession.clear]: whoever calls [lock] gets back an id, and
 * [unlock] only clears the flag if that same id is still the current
 * holder — so a stale/late call from an old, already-superseded session
 * can't accidentally unlock a newer one. [forceUnlock] deliberately skips
 * that check; it exists only as the last-resort safety net
 * [FocusModeViewModel] calls from `onCleared()`, so the app can never be
 * left permanently locked just because the ViewModel holding the id was
 * torn down before it could call [unlock] itself.
 */
object FocusLockController {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var currentHolderId: String? = null

    /** Engages the lock and returns an id the caller must hold onto and
     *  pass back to [unlock]. */
    fun lock(): String {
        val id = UUID.randomUUID().toString()
        currentHolderId = id
        _isLocked.value = true
        return id
    }

    /** No-op unless [id] still matches the current holder. */
    fun unlock(id: String?) {
        if (id != null && currentHolderId == id) {
            currentHolderId = null
            _isLocked.value = false
        }
    }

    /** Unconditional release, regardless of holder — see the class doc's
     *  note on why this exists as a distinct, deliberately-unsafe escape
     *  hatch rather than something reached during normal operation. */
    fun forceUnlock() {
        currentHolderId = null
        _isLocked.value = false
    }
}
