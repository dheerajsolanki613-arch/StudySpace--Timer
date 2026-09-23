package com.studyspace.timer.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

/**
 * Persists the Stage 8 settings: notification behavior, timer behavior, and
 * an accessibility preference, via DataStore Preferences — same pattern as
 * [com.studyspace.timer.theme.PaletteRepository] and
 * [com.studyspace.timer.wallpaper.WallpaperRepository]. A separate DataStore
 * file (`app_settings`) since none of these are theme/wallpaper choices.
 *
 * Every value has a sensible default that matches the app's pre-Stage-8
 * behavior exactly, so installing this stage doesn't silently change
 * anything for a user who never opens Settings:
 *  - Completion alerts: on (the app already showed a silent "Completed"
 *    status in the ongoing notification; this adds a distinct one-shot
 *    sound, so defaulting it on is what a user would expect a timer app to
 *    do out of the box).
 *  - Keep screen on: on (matches the common assumption that a running
 *    on-screen timer won't have the display sleep on it).
 *  - Reduce motion: off (the small amount of motion this app has — see
 *    [com.studyspace.timer.ui.theme.LocalReduceMotion] — is the default;
 *    this is an explicit opt-in accessibility preference, not a correction).
 *  - Daily goal: 240 minutes (4h), identical to the fixed constant
 *    `HomeScreen.kt` used before Stage 8 made it user-configurable.
 *
 * Phase 12 (Notifications & Reminders) adds [reminderSettings] — four
 * opt-in notification toggles plus a daily-reminder time, all defaulting to
 * **off** (see [ReminderSettings]) — and two small bookkeeping values,
 * [lastGoalNotifiedDay]/[setLastGoalNotifiedDay], that stop a goal
 * notification from repeating. Those two are not user settings; they live
 * in this same DataStore file only because it's the app's one existing
 * key-value store and they're a handful of bytes.
 *
 * Phase 14 (Backup & Restore) adds [lastBackupEpochMillis] — bookkeeping,
 * not a user setting, same reasoning as above.
 *
 * Phase 15 (Themes & Customization) adds [soundEnabled]/[vibrationEnabled] —
 * both default **on**, independent of [timerCompletionAlertsEnabled]; see
 * [com.studyspace.timer.service.CompletionFeedback] for what they actually
 * trigger and why they're separate from the notification toggle.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val TIMER_COMPLETION_ALERTS = booleanPreferencesKey("timer_completion_alerts")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val PLANNED_SESSION_REMINDERS = booleanPreferencesKey("planned_session_reminders")
        val UPCOMING_SESSION_REMINDERS = booleanPreferencesKey("upcoming_session_reminders")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute_of_day")
        val GOAL_NOTIFICATIONS = booleanPreferencesKey("goal_notifications")
        val LAST_DAILY_GOAL_NOTIFIED_DAY = longPreferencesKey("last_daily_goal_notified_day")
        val LAST_WEEKLY_GOAL_NOTIFIED_DAY = longPreferencesKey("last_weekly_goal_notified_day")
        val LAST_BACKUP_EPOCH_MILLIS = longPreferencesKey("last_backup_epoch_millis")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    /** When the most recent backup finished writing, for the Data screen's "Last backup: …" line. Null if none yet. */
    val lastBackupEpochMillis: Flow<Long?> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_EPOCH_MILLIS]
    }

    suspend fun setLastBackupEpochMillis(epochMillis: Long) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.LAST_BACKUP_EPOCH_MILLIS] = epochMillis }
    }

    val timerCompletionAlertsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.TIMER_COMPLETION_ALERTS] ?: true
    }

    /**
     * Phase 15 — independent of [timerCompletionAlertsEnabled] (the system
     * notification): this controls a short in-app tone the app itself plays
     * via [com.studyspace.timer.service.CompletionFeedback] the instant a
     * timer/focus session/Pomodoro phase finishes, so it fires even if
     * notifications are otherwise off. Defaults on, matching pre-Phase-15
     * behavior as closely as an entirely new sound can (there was no sound
     * at all before; on is what a timer app's user would expect).
     */
    val soundEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SOUND_ENABLED] ?: true
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.SOUND_ENABLED] = enabled }
    }

    /** Phase 15 — same independence from notifications as [soundEnabled], via the same [com.studyspace.timer.service.CompletionFeedback] call. */
    val vibrationEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.VIBRATION_ENABLED] ?: true
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.VIBRATION_ENABLED] = enabled }
    }

    val keepScreenOnEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.KEEP_SCREEN_ON] ?: true
    }

    val reduceMotionEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.REDUCE_MOTION] ?: false
    }

    val dailyGoalMinutes: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.DAILY_GOAL_MINUTES] ?: DEFAULT_DAILY_GOAL_MINUTES
    }

    val reminderSettings: Flow<ReminderSettings> = context.settingsDataStore.data.map { prefs ->
        ReminderSettings(
            plannedSessionReminders = prefs[Keys.PLANNED_SESSION_REMINDERS] ?: false,
            upcomingSessionReminders = prefs[Keys.UPCOMING_SESSION_REMINDERS] ?: false,
            dailyReminder = prefs[Keys.DAILY_REMINDER] ?: false,
            dailyReminderMinuteOfDay = prefs[Keys.DAILY_REMINDER_MINUTE] ?: ReminderSettings.DEFAULT_DAILY_REMINDER_MINUTE,
            goalNotifications = prefs[Keys.GOAL_NOTIFICATIONS] ?: false
        )
    }

    suspend fun setPlannedSessionReminders(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.PLANNED_SESSION_REMINDERS] = enabled }
    }

    suspend fun setUpcomingSessionReminders(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.UPCOMING_SESSION_REMINDERS] = enabled }
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.DAILY_REMINDER] = enabled }
    }

    suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.DAILY_REMINDER_MINUTE] = minuteOfDay }
    }

    suspend fun setGoalNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.GOAL_NOTIFICATIONS] = enabled }
    }

    /** Epoch day the daily (or, if [weekly], weekly) goal notification was last posted, or `null` if never. */
    suspend fun lastGoalNotifiedDay(weekly: Boolean): Long? =
        context.settingsDataStore.data.first()[if (weekly) Keys.LAST_WEEKLY_GOAL_NOTIFIED_DAY else Keys.LAST_DAILY_GOAL_NOTIFIED_DAY]

    suspend fun setLastGoalNotifiedDay(weekly: Boolean, epochDay: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[if (weekly) Keys.LAST_WEEKLY_GOAL_NOTIFIED_DAY else Keys.LAST_DAILY_GOAL_NOTIFIED_DAY] = epochDay
        }
    }

    suspend fun setTimerCompletionAlertsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.TIMER_COMPLETION_ALERTS] = enabled }
    }

    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setReduceMotionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.DAILY_GOAL_MINUTES] = minutes }
    }

    companion object {
        const val DEFAULT_DAILY_GOAL_MINUTES = 240
        /** Preset chips shown on the Settings screen, mirroring how
         *  Pomodoro/Normal Timer durations are picked from short lists
         *  rather than free-entry fields. */
        val DAILY_GOAL_PRESET_MINUTES = listOf(60, 120, 180, 240, 360, 480)
    }
}
