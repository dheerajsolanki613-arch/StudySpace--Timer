package com.studyspace.timer.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.settings.ReminderSettings
import com.studyspace.timer.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs [SettingsScreen] with real, persisted values (Stage 8) instead of
 * the disabled Stage 2 placeholder switches. Also read from
 * [com.studyspace.timer.MainActivity] (same Activity-scoped instance) so
 * "keep screen on" and "reduce motion" can be applied app-wide rather than
 * only on the Settings screen itself — same pattern Stage 6 used for
 * [com.studyspace.timer.screens.themes.ThemesViewModel] and the palette.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudySpaceApplication).settingsRepository

    val timerCompletionAlertsEnabled: StateFlow<Boolean> =
        repository.timerCompletionAlertsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Phase 15 — independent of [timerCompletionAlertsEnabled]; see [SettingsRepository.soundEnabled]. */
    val soundEnabled: StateFlow<Boolean> =
        repository.soundEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Phase 15 — independent of [timerCompletionAlertsEnabled]; see [SettingsRepository.vibrationEnabled]. */
    val vibrationEnabled: StateFlow<Boolean> =
        repository.vibrationEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val keepScreenOnEnabled: StateFlow<Boolean> =
        repository.keepScreenOnEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val reduceMotionEnabled: StateFlow<Boolean> =
        repository.reduceMotionEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val dailyGoalMinutes: StateFlow<Int> =
        repository.dailyGoalMinutes.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_DAILY_GOAL_MINUTES
        )

    /** Phase 12: the opt-in reminder toggles + daily-reminder time; everything defaults to off. */
    val reminderSettings: StateFlow<ReminderSettings> =
        repository.reminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    fun setPlannedSessionReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setPlannedSessionReminders(enabled) }
    }

    fun setUpcomingSessionReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setUpcomingSessionReminders(enabled) }
    }

    fun setDailyReminder(enabled: Boolean) {
        viewModelScope.launch { repository.setDailyReminder(enabled) }
    }

    fun setDailyReminderMinuteOfDay(minuteOfDay: Int) {
        viewModelScope.launch { repository.setDailyReminderMinuteOfDay(minuteOfDay) }
    }

    fun setGoalNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setGoalNotifications(enabled) }
    }

    fun setTimerCompletionAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTimerCompletionAlertsEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setKeepScreenOnEnabled(enabled) }
    }

    fun setReduceMotionEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setReduceMotionEnabled(enabled) }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDailyGoalMinutes(minutes) }
    }
}
