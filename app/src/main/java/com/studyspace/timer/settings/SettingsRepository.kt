package com.studyspace.timer.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val TIMER_COMPLETION_ALERTS = booleanPreferencesKey("timer_completion_alerts")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
    }

    val timerCompletionAlertsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.TIMER_COMPLETION_ALERTS] ?: true
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
