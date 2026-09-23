package com.studyspace.timer.data.repository

/**
 * A goal's progress at a point in time — used for both the existing daily
 * goal (target from [com.studyspace.timer.settings.SettingsRepository]) and
 * the new weekly goal (target from
 * [com.studyspace.timer.data.db.StudyGoalEntity]), so the Goals screen
 * renders both with the same card and the same math.
 *
 * Has no Android dependency by design, same reasoning as
 * [com.studyspace.timer.timer.FocusDuration] — lets the percent/remaining
 * math be covered by a fast local JUnit test with no SDK/emulator.
 */
data class GoalProgress(
    val label: String,
    val currentMillis: Long,
    val targetMillis: Long
) {
    /** 0f..1f, even if [currentMillis] overshoots [targetMillis]. */
    val progressFraction: Float
        get() = if (targetMillis <= 0L) 0f else (currentMillis.toFloat() / targetMillis.toFloat()).coerceIn(0f, 1f)

    val percent: Int
        get() = Math.round(progressFraction * 100f)

    /** Never negative — 0 once the goal is met or passed. */
    val remainingMillis: Long
        get() = (targetMillis - currentMillis).coerceAtLeast(0L)

    val isComplete: Boolean
        get() = targetMillis > 0L && currentMillis >= targetMillis
}
