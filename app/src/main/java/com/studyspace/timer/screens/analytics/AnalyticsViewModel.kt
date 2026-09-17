package com.studyspace.timer.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.repository.WeeklyAnalytics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Backs [AnalyticsScreen]'s weekly bar chart and per-mode breakdown cards
 * (Stage 7) with real numbers from
 * [com.studyspace.timer.data.repository.SessionRepository.weeklyAnalytics],
 * replacing the Stage 2 "no chart data yet" placeholder and the hardcoded
 * "0h" breakdown cards.
 *
 * Same `WhileSubscribed(5_000)` pattern as [com.studyspace.timer.screens.home.HomeViewModel]:
 * the underlying Room query survives brief unsubscription (switching bottom
 * nav tabs and back) instead of restarting from empty every time.
 */
class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudySpaceApplication).sessionRepository

    val weeklyAnalytics: StateFlow<WeeklyAnalytics> = repository.weeklyAnalytics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyAnalytics())
}
