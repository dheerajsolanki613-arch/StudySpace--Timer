package com.studyspace.timer.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.repository.AdvancedStats
import com.studyspace.timer.data.repository.AnalyticsRange
import com.studyspace.timer.data.repository.DayTotal
import com.studyspace.timer.data.repository.PlannedVsActual
import com.studyspace.timer.data.repository.PlannedVsActualAnalytics
import com.studyspace.timer.data.repository.ProductivityPatterns
import com.studyspace.timer.data.repository.SubjectTotal
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.util.centeredContentWidth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Analytics dashboard (Stage 7): weekly bar chart and per-mode breakdown are
 * real, driven by [AnalyticsViewModel] →
 * [com.studyspace.timer.data.repository.SessionRepository.weeklyAnalytics]
 * (Room). An empty week still renders — flat bars and "0m" cards — since
 * that's the honest state for a new install, not an error.
 *
 * Responsive layout pass: this screen previously stayed a single
 * [LazyColumn] regardless of orientation (only the breakdown grid's column
 * count reacted to landscape), which on a short landscape phone forced a
 * fixed-height chart card and full stack of section headers to fight over
 * very little vertical space. It now follows the same
 * [BoxWithConstraints]-measured pattern as
 * [com.studyspace.timer.screens.home.HomeScreen]:
 *  - **Portrait / narrow**: one scrolling column, capped to
 *    [centeredContentWidth] on anything wider than a phone, chart card at
 *    its original comfortable height.
 *  - **Landscape / wide**: a two-pane [Row] — the weekly chart in a left
 *    pane, the session breakdown in a right pane — so the screen uses its
 *    spare *width* instead of stacking everything into scarce *height*.
 *    Each pane scrolls independently as a safety net for unusually short
 *    heights, and the chart's own height is measured from the pane rather
 *    than hardcoded, so it never claims more room than is actually there.
 */
@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier, viewModel: AnalyticsViewModel = viewModel()) {
    val analytics by viewModel.weeklyAnalytics.collectAsState()
    val plannedVsActual by viewModel.plannedVsActual.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val advancedStats by viewModel.advancedStats.collectAsState()
    val subjectTotals by viewModel.subjectTotals.collectAsState()
    val productivityPatterns by viewModel.productivityPatterns.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    item { SectionHeader(title = "This Week") }
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            WeeklyBarChart(
                                dailyTotals = analytics.dailyTotals,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { SectionHeader(title = "Session Breakdown") }
                    item { SessionBreakdown(typeTotals = analytics.typeTotals, columns = 2) }
                    item { SectionHeader(title = "Planned vs Actual") }
                    item { PlannedVsActualSection(analytics = plannedVsActual) }
                    item { SectionHeader(title = "Advanced Analytics") }
                    item {
                        RangeSelectorRow(selected = selectedRange, onSelect = viewModel::selectRange)
                    }
                    item { OverviewStatsGrid(stats = advancedStats, columns = 2) }
                    item { SectionHeader(title = "By Subject") }
                    item { SubjectBreakdownList(totals = subjectTotals, subjectsById = subjectsById) }
                    item { SectionHeader(title = "Productivity Patterns") }
                    item { ProductivityPatternsCard(patterns = productivityPatterns) }
                }
            }
        } else {
            val contentWidth: Dp = centeredContentWidth(maxWidth)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.width(contentWidth)
                    )
                }

                item { SectionHeader(title = "This Week", modifier = Modifier.width(contentWidth)) }

                item {
                    GlassCard(modifier = Modifier.width(contentWidth)) {
                        WeeklyBarChart(
                            dailyTotals = analytics.dailyTotals,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }

                item { SectionHeader(title = "Session Breakdown", modifier = Modifier.width(contentWidth)) }

                item {
                    SessionBreakdown(
                        typeTotals = analytics.typeTotals,
                        columns = 2,
                        modifier = Modifier.width(contentWidth)
                    )
                }

                item { SectionHeader(title = "Planned vs Actual", modifier = Modifier.width(contentWidth)) }

                item {
                    PlannedVsActualSection(
                        analytics = plannedVsActual,
                        modifier = Modifier.width(contentWidth)
                    )
                }

                item { SectionHeader(title = "Advanced Analytics", modifier = Modifier.width(contentWidth)) }

                item {
                    RangeSelectorRow(
                        selected = selectedRange,
                        onSelect = viewModel::selectRange,
                        modifier = Modifier.width(contentWidth)
                    )
                }

                item {
                    OverviewStatsGrid(
                        stats = advancedStats,
                        columns = 2,
                        modifier = Modifier.width(contentWidth)
                    )
                }

                item { SectionHeader(title = "By Subject", modifier = Modifier.width(contentWidth)) }

                item {
                    SubjectBreakdownList(
                        totals = subjectTotals,
                        subjectsById = subjectsById,
                        modifier = Modifier.width(contentWidth)
                    )
                }

                item { SectionHeader(title = "Productivity Patterns", modifier = Modifier.width(contentWidth)) }

                item {
                    ProductivityPatternsCard(
                        patterns = productivityPatterns,
                        modifier = Modifier.width(contentWidth)
                    )
                }
            }
        }
    }
}

/**
 * Per-mode breakdown grid, factored out of [AnalyticsScreen] so both the
 * portrait single-column layout and the landscape right-hand pane can share
 * it with just a different [columns] count and width constraint.
 */
@Composable
private fun SessionBreakdown(
    typeTotals: Map<SessionType, Long>,
    columns: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SessionType.entries.chunked(columns).forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTypes.forEach { type ->
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text(
                                text = formatDurationHoursMinutes(typeTotals[type] ?: 0L),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = type.displayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                // Pad a short last row so every card keeps equal width.
                repeat(columns - rowTypes.size) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Phase 7 (Planned vs Actual): three stacked comparison cards (Today / This
 * week / This month) rather than a side-by-side row of three — a row of
 * three cards each carrying three numbers (planned/actual/completion) is
 * too dense for a phone-width column, the same "stack, don't cram
 * horizontally" call [SessionBreakdown]'s `columns` parameter exists to let
 * callers make per-layout.
 */
@Composable
private fun PlannedVsActualSection(analytics: PlannedVsActualAnalytics, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlannedVsActualCard(label = "Today", data = analytics.today)
        PlannedVsActualCard(label = "This week", data = analytics.week)
        PlannedVsActualCard(label = "This month", data = analytics.month)
    }
}

@Composable
private fun PlannedVsActualCard(label: String, data: PlannedVsActual, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlannedVsActualStat(title = "Planned", value = formatDurationHoursMinutes(data.plannedMillis))
                PlannedVsActualStat(title = "Actual", value = formatDurationHoursMinutes(data.actualMillis))
                PlannedVsActualStat(
                    title = "Completion",
                    value = if (data.plannedMillis > 0L) "${data.completionPercent}%" else "—",
                    valueColor = if (data.completionPercent >= 100 && data.plannedMillis > 0L) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun PlannedVsActualStat(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

/**
 * Phase 8 range picker — [AnalyticsRange] entries as [FilterChip]s, same
 * horizontal-scroll-chip-row pattern the rest of this app already uses for
 * preset pickers (`TaskEditorDialog`'s deadline chips,
 * `PlannedSessionEditorDialog`'s date/time/duration chips). Selecting a
 * range re-filters [AnalyticsViewModel.advancedStats]/`subjectTotals`/
 * `productivityPatterns` together (all three derive from the same
 * `filteredSessions` flow) — it does **not** affect the "This Week" chart/
 * breakdown above or the "Planned vs Actual" cards above that, which stay
 * on their own fixed windows (7 days and 30 days respectively) regardless
 * of this selector, since those sections were built in earlier phases with
 * their own explicit range semantics already documented and it would be a
 * bigger, riskier change to retrofit them onto a shared selector than to
 * simply scope this one to the new Phase 8 sections it was built for.
 */
@Composable
private fun RangeSelectorRow(
    selected: AnalyticsRange,
    onSelect: (AnalyticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(range.label) }
            )
        }
    }
}

/** Phase 8 "Statistics" section: an 8-stat grid for [AdvancedStats] over the selected [AnalyticsRange]. */
@Composable
private fun OverviewStatsGrid(stats: AdvancedStats, columns: Int, modifier: Modifier = Modifier) {
    val entries = listOf(
        "Total time" to formatDurationHoursMinutes(stats.totalMillis),
        "Sessions" to stats.sessionCount.toString(),
        "Average session" to formatDurationHoursMinutes(stats.averageSessionMillis),
        "Longest session" to formatDurationHoursMinutes(stats.longestSessionMillis),
        "Completed Pomodoros" to stats.completedPomodoros.toString(),
        "Completion rate" to "${stats.completionRatePercent}%",
        "Daily average" to formatDurationHoursMinutes(stats.dailyAverageMillis),
        "Weekly average" to (stats.weeklyAverageMillis?.let { formatDurationHoursMinutes(it) } ?: "—")
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.chunked(columns).forEach { rowEntries ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowEntries.forEach { (title, value) ->
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                repeat(columns - rowEntries.size) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Phase 8 "Subject analytics" section: one row per [SubjectTotal] within
 * the selected range, widest first. An unattributed total
 * (`subjectId == null`) is labeled "No subject" rather than dropped — see
 * [com.studyspace.timer.data.repository.computeSubjectTotals]'s KDoc.
 */
@Composable
private fun SubjectBreakdownList(
    totals: List<SubjectTotal>,
    subjectsById: Map<Long, com.studyspace.timer.data.db.SubjectEntity>,
    modifier: Modifier = Modifier
) {
    if (totals.isEmpty()) {
        GlassCard(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "No sessions in this range yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        totals.forEach { total ->
            val subject = total.subjectId?.let { subjectsById[it] }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subject?.let { "${it.icon} ${it.name}" } ?: "No subject",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDurationHoursMinutes(total.totalMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * Phase 8 "Productivity patterns" card. Every line renders "Not enough
 * data yet" rather than a fabricated pattern when [ProductivityPatterns]'s
 * corresponding field is `null` — see that class's KDoc for why blank/zero
 * is the honest state here, not an error to work around.
 */
@Composable
private fun ProductivityPatternsCard(patterns: ProductivityPatterns, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProductivityPatternRow("Most productive day", patterns.mostProductiveDayLabel ?: "Not enough data yet")
            ProductivityPatternRow("Most productive time", patterns.mostProductiveHourRangeLabel ?: "Not enough data yet")
            ProductivityPatternRow("Study consistency", "${patterns.consistencyPercent}% of days in range")
            ProductivityPatternRow(
                "Session completion rate",
                "${patterns.completionRatePercent}% (same metric as \"Completion rate\" above)"
            )
        }
    }
}

@Composable
private fun ProductivityPatternRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Simple bar chart, one bar per [DayTotal], drawn on a bare [Canvas] rather
 * than pulling in a charting library — the project has stayed
 * dependency-light throughout. Bar heights are relative to the tallest day
 * in [dailyTotals]; an all-zero week draws flat minimum-height bars rather
 * than invisible ones, so the chart doesn't look broken on a fresh install.
 */
@Composable
private fun WeeklyBarChart(dailyTotals: List<DayTotal>, modifier: Modifier = Modifier) {
    val maxMillis = (dailyTotals.maxOfOrNull { it.totalMillis } ?: 0L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val trackStrokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (dailyTotals.isEmpty()) return@Canvas

            val barCount = dailyTotals.size
            val gap = size.width * 0.04f
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            val minBarHeight = 4.dp.toPx()

            dailyTotals.forEachIndexed { index, day ->
                val fraction = day.totalMillis.toFloat() / maxMillis.toFloat()
                val barHeight = (size.height * fraction).coerceAtLeast(minBarHeight)
                val left = index * (barWidth + gap)
                val top = size.height - barHeight
                val color = if (day.totalMillis > 0L) barColor else trackStrokeColor

                // Faint full-height track drawn first, so a 0-minute day is
                // still legible as a column once the (minimum-height) bar
                // is drawn on top of it.
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth * 0.25f, barWidth * 0.25f),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.25f, barWidth * 0.25f)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            dailyTotals.forEach { day ->
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
