package com.studyspace.timer.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.repository.DayTotal
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.util.isLandscape
import java.time.format.TextStyle
import java.util.Locale

/**
 * Analytics dashboard (Stage 7): weekly bar chart and per-mode breakdown are
 * real, driven by [AnalyticsViewModel] →
 * [com.studyspace.timer.data.repository.SessionRepository.weeklyAnalytics]
 * (Room). An empty week still renders — flat bars and "0m" cards — since
 * that's the honest state for a new install, not an error.
 *
 * Theme/rotation update: chart bars and breakdown cards read
 * `MaterialTheme.colorScheme` instead of fixed `Galaxy*` colors, and the
 * breakdown grid uses 3 columns in landscape (vs. 2 in portrait) since
 * there's more horizontal room and 5 session types divide more evenly.
 */
@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier, viewModel: AnalyticsViewModel = viewModel()) {
    val analytics by viewModel.weeklyAnalytics.collectAsState()
    val breakdownColumns = if (isLandscape()) 3 else 2

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        .height(160.dp)
                )
            }
        }

        item { SectionHeader(title = "Session Breakdown") }

        item {
            val typeTotals = analytics.typeTotals
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionType.entries.chunked(breakdownColumns).forEach { rowTypes ->
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
                        repeat(breakdownColumns - rowTypes.size) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
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
