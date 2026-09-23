package com.studyspace.timer.screens.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.repository.AdvancedStats
import com.studyspace.timer.data.repository.AnalyticsRange
import com.studyspace.timer.data.repository.PlannedVsActualAnalytics
import com.studyspace.timer.data.repository.ProductivityPatterns
import com.studyspace.timer.data.repository.SubjectTotal
import com.studyspace.timer.data.repository.WeeklyAnalytics
import com.studyspace.timer.data.repository.computeAdvancedStats
import com.studyspace.timer.data.repository.computePlannedVsActual
import com.studyspace.timer.data.repository.computeProductivityPatterns
import com.studyspace.timer.data.repository.computeSubjectTotals
import com.studyspace.timer.data.repository.sessionsInRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

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
 *
 * Phase 7 (Planned vs Actual): [plannedVsActual] combines the last 30 days
 * of planned blocks and recorded sessions — reusing
 * [com.studyspace.timer.data.repository.SessionRepository.sessionsSince]/
 * [com.studyspace.timer.data.repository.PlannedSessionRepository.sessionsSince]
 * (the same "one wider query, filter in Kotlin" shape [weeklyAnalytics]
 * already established) — through the pure, unit-tested
 * `computePlannedVsActual`.
 *
 * Phase 8 (Advanced Analytics): [selectedRange] drives [advancedStats]/
 * [subjectTotals]/[productivityPatterns], all three derived from one shared
 * [filteredSessions] flow — [com.studyspace.timer.data.repository.SessionRepository.allSessions]
 * fetched once, unbounded, then range-sliced in Kotlin via
 * [com.studyspace.timer.data.repository.sessionsInRange] — rather than each
 * of the three re-filtering independently, so all three always agree with
 * each other about exactly which sessions are "in range" for the
 * currently-selected [AnalyticsRange].
 */
class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.sessionRepository

    val weeklyAnalytics: StateFlow<WeeklyAnalytics> = repository.weeklyAnalytics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyAnalytics())

    val plannedVsActual: StateFlow<PlannedVsActualAnalytics> = combine(
        app.plannedSessionRepository.sessionsSince(LocalDate.now().minusDays(29).toEpochDay()),
        repository.sessionsSince(LocalDate.now().minusDays(29).toEpochDay())
    ) { planned, actual ->
        computePlannedVsActual(planned, actual, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannedVsActualAnalytics())

    private val _selectedRange = MutableStateFlow(AnalyticsRange.LAST_7_DAYS)
    val selectedRange: StateFlow<AnalyticsRange> = _selectedRange.asStateFlow()

    fun selectRange(range: AnalyticsRange) {
        _selectedRange.value = range
    }

    val subjects: StateFlow<List<SubjectEntity>> = app.subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filteredSessions = combine(repository.allSessions(), _selectedRange) { sessions, range ->
        sessionsInRange(sessions, range, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val advancedStats: StateFlow<AdvancedStats> = combine(filteredSessions, _selectedRange) { sessions, range ->
        computeAdvancedStats(sessions, range, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvancedStats())

    val subjectTotals: StateFlow<List<SubjectTotal>> = filteredSessions
        .map { computeSubjectTotals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val productivityPatterns: StateFlow<ProductivityPatterns> = combine(filteredSessions, _selectedRange) { sessions, range ->
        computeProductivityPatterns(sessions, range, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductivityPatterns())
}
