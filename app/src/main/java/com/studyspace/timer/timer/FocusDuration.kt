package com.studyspace.timer.timer

/**
 * Validation for Focus Mode's custom minutes/seconds duration picker.
 * Deliberately a plain object with no Android dependency (no
 * `SystemClock`, no `Application`) — this is the one piece of Focus Mode's
 * "validate the duration before starting" / "prevent invalid or zero-
 * duration sessions" requirement that's worth pulling out of
 * [FocusModeViewModel] on its own, since keeping it framework-free is what
 * lets it run as a fast local JUnit test (see
 * `FocusDurationTest`) in an environment with no Android SDK/emulator —
 * the same constraint this whole project already builds around (see
 * `PROJECT_STATE.md`).
 *
 * [validate] is also the single choke point every path into
 * [FocusModeViewModel.confirmAndStart] goes through — nothing reaches
 * [TimerEngine.start] with an unvalidated duration, which is what actually
 * prevents a corrupted/zero/negative input from ever starting a locked
 * session that could strand the user.
 */
object FocusDuration {
    /** Shown as quick-pick chips before a custom value is dialed in. */
    val PRESET_MINUTES = listOf(15, 25, 45, 60)

    const val MIN_TOTAL_SECONDS = 1
    const val MAX_MINUTES = 180

    sealed class ValidationResult {
        data class Valid(val totalMillis: Long) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    /**
     * [minutes]/[seconds] come straight from stepper UI state that itself
     * can't go negative or let seconds reach 60 (see `DurationStepper` in
     * `FocusScreen.kt`), but this still re-checks both bounds rather than
     * trusting the caller — the whole point of having one validation choke
     * point is that it doesn't assume its input is already clean.
     */
    fun validate(minutes: Int, seconds: Int): ValidationResult {
        if (minutes < 0 || seconds < 0 || seconds > 59) {
            return ValidationResult.Invalid("Enter a valid focus duration.")
        }
        if (minutes > MAX_MINUTES) {
            return ValidationResult.Invalid("Focus sessions are capped at $MAX_MINUTES minutes.")
        }
        val totalMillis = minutes * 60_000L + seconds * 1_000L
        return if (totalMillis < MIN_TOTAL_SECONDS * 1_000L) {
            ValidationResult.Invalid("Focus duration can't be zero.")
        } else {
            ValidationResult.Valid(totalMillis)
        }
    }
}
