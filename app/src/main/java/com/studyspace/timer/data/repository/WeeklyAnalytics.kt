package com.studyspace.timer.data.repository

import com.studyspace.timer.data.SessionType
import java.time.LocalDate

/**
 * One day's total study time, for the Analytics weekly chart.
 *
 * @param date the calendar day this total covers (device zone).
 * @param totalMillis sum of every recorded session's [durationMillis]
 *   ([com.studyspace.timer.data.db.StudySessionEntity]) that started on
 *   [date]. Zero, not omitted, for a day with no sessions — the chart needs
 *   exactly 7 entries to draw 7 bars.
 */
data class DayTotal(
    val date: LocalDate,
    val totalMillis: Long
)

/**
 * Stage 7 analytics aggregate: the last 7 days' study time, bucketed two
 * ways from the same underlying query
 * ([com.studyspace.timer.data.db.StudySessionDao.sessionsSince]) so the
 * weekly bar chart and the per-mode breakdown cards always agree with each
 * other and with Home's "this week" total.
 *
 * @param dailyTotals exactly 7 entries, oldest (6 days ago) to newest
 *   (today), for the weekly bar chart. Days with no sessions are included
 *   with [DayTotal.totalMillis] = 0 rather than omitted.
 * @param typeTotals every [SessionType] present in the same 7-day window,
 *   mapped to its summed duration. A type with no sessions this week is
 *   simply absent from the map rather than present with 0 — callers that
 *   want to show all 5 modes regardless should default missing entries to
 *   0 themselves (see [com.studyspace.timer.screens.analytics.AnalyticsScreen]).
 */
data class WeeklyAnalytics(
    val dailyTotals: List<DayTotal> = emptyList(),
    val typeTotals: Map<SessionType, Long> = emptyMap()
)
