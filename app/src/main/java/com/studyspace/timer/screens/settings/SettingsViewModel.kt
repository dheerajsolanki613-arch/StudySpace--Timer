package com.studyspace.timer.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
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

    fun setTimerCompletionAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTimerCompletionAlertsEnabled(enabled) }
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
