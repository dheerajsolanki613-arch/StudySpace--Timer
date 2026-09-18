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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.repository.DayTotal
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.theme.GalaxyMutedLavender
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyStarWhite
import java.time.format.TextStyle
import java.util.Locale

/**
 * Analytics dashboard (Stage 7): weekly bar chart and per-mode breakdown are
 * now real, driven by [AnalyticsViewModel] →
 * [com.studyspace.timer.data.repository.SessionRepository.weeklyAnalytics]
 * (Room), replacing Stage 2's "no chart data yet" placeholder and hardcoded
 * "0h" cards. An empty week still renders — flat bars and "0m" cards — since
 * that's the honest state for a new install, not an error.
 */
@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier, viewModel: AnalyticsViewModel = viewModel()) {
    val analytics by viewModel.weeklyAnalytics.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                color = GalaxyMutedLavender
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
                SessionType.entries.chunked(2).forEach { rowTypes ->
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
                                        color = GalaxyStarWhite
                                    )
                                    Text(
                                        text = type.displayLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GalaxyStarWhite.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        // Odd count (5 types): pad the last row so both cards keep equal width.
                        if (rowTypes.size == 1) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple 7-bar vertical chart, one bar per [DayTotal], drawn on a bare
 * [Canvas] rather than pulling in a charting library — the project has
 * stayed dependency-light throughout, and 7 rounded rectangles don't need
 * one. Bar heights are relative to the tallest day in [dailyTotals]; an
 * all-zero week draws 7 flat minimum-height bars rather than 7 invisible
 * ones, so the chart doesn't look broken on a fresh install.
 */
@Composable
private fun WeeklyBarChart(dailyTotals: List<DayTotal>, modifier: Modifier = Modifier) {
    val maxMillis = (dailyTotals.maxOfOrNull { it.totalMillis } ?: 0L).coerceAtLeast(1L)

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
                val color = if (day.totalMillis > 0L) GalaxyNeonCyan else GalaxyStarWhite.copy(alpha = 0.15f)

                // Faint full-height track drawn first, so a 0-minute day is
                // still legible as a column once the (minimum-height) bar
                // is drawn on top of it.
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f),
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
                    color = GalaxyStarWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
