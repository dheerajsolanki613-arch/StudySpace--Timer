package com.studyspace.timer.settings

/**
 * Phase 12 — the user's opt-ins for scheduled/event notifications. Plain
 * data with no Android dependency so the scheduling logic in
 * [com.studyspace.timer.reminders.computeNextReminder] can be unit-tested
 * against it directly.
 *
 * **Every flag defaults to off.** The app never notified for any of these
 * before, so updating must not suddenly start pinging existing users; each
 * is an explicit opt-in from Settings (which is also the natural moment to
 * ask for the notification permission on Android 13+). The one pre-existing
 * notification setting — "Timer completion alerts" — is unchanged and lives
 * on [SettingsRepository] with its old default of on.
 *
 * @param plannedSessionReminders notify when a planned study session is due to start.
 * @param upcomingSessionReminders a heads-up [com.studyspace.timer.reminders.UPCOMING_LEAD_MINUTES]
 *   minutes before a planned session starts.
 * @param dailyReminder a single reminder per day at [dailyReminderMinuteOfDay];
 *   skipped on a day the user has already studied.
 * @param goalNotifications notify once when the daily goal, or the weekly
 *   goal, is reached.
 */
data class ReminderSettings(
    val plannedSessionReminders: Boolean = false,
    val upcomingSessionReminders: Boolean = false,
    val dailyReminder: Boolean = false,
    val dailyReminderMinuteOfDay: Int = DEFAULT_DAILY_REMINDER_MINUTE,
    val goalNotifications: Boolean = false
) {
    /** True if any reminder that needs an alarm scheduled is on. */
    val anyScheduledReminderEnabled: Boolean
        get() = plannedSessionReminders || upcomingSessionReminders || dailyReminder

    companion object {
        const val DEFAULT_DAILY_REMINDER_MINUTE = 19 * 60

        /**
         * Preset reminder times offered in Settings: 7 AM through 9 PM.
         * Deliberately nothing later than 9 PM — the app shouldn't nudge
         * anyone to study late at night (spec: don't encourage unhealthy
         * over-studying or sleep deprivation).
         */
        val DAILY_TIME_PRESET_MINUTES: List<Int> = listOf(7, 9, 12, 15, 18, 19, 20, 21).map { it * 60 }
    }
}
