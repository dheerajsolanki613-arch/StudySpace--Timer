package com.studyspace.timer.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.repository.StudyStats
import com.studyspace.timer.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Backs [HomeScreen]'s real numbers (Stage 5) — today's total, session
 * count, this-week total, streak, and recent activity — all read from Room
 * via [com.studyspace.timer.data.repository.SessionRepository] instead of
 * the static placeholders Stage 2 shipped with.
 *
 * `WhileSubscribed(5_000)` on both flows means the underlying Room queries
 * stay alive briefly across quick navigation away/back (e.g. switching bottom
 * nav tabs) instead of restarting from empty every time, while still
 * stopping once nobody's observing for good.
 *
 * Stage 8: [dailyGoalMillis] reads the real, user-configurable goal from
 * [SettingsRepository] instead of `HomeScreen.kt`'s old fixed
 * `DAILY_GOAL_MILLIS` constant. Default (240 min / 4h) is identical to that
 * old constant, so a user who never opens Settings sees no change.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudySpaceApplication).sessionRepository
    private val settingsRepository = application.settingsRepository

    val stats: StateFlow<StudyStats> = repository.studyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyStats())

    val recentSessions: StateFlow<List<StudySessionEntity>> = repository.recentSessions(limit = 6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyGoalMillis: StateFlow<Long> = settingsRepository.dailyGoalMinutes
        .map { minutes -> minutes * 60_000L }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsRepository.DEFAULT_DAILY_GOAL_MINUTES * 60_000L
        )
}

