package com.studyspace.timer.screens.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.repository.Achievement
import com.studyspace.timer.data.repository.StreakSummary
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.components.StatMiniCard

/**
 * Phase 9 — Streaks & Achievements. Two sections: a top-line numbers card
 * (current/best streak, weekly consistency, then a totals row) and a list
 * of the 8 spec achievements, each a fact-stated card with no urgency
 * language, streak-shaming, or "don't break the chain" framing — the spec
 * explicitly warns against pressuring continuous studying, and describing
 * an unlocked/locked state plainly (title + one descriptive sentence, no
 * exclamation points, no "keep it up!") is how that's honored here rather
 * than only in the data layer.
 */
@Composable
fun AchievementsScreen(modifier: Modifier = Modifier, viewModel: AchievementsViewModel = viewModel()) {
    val streakSummary by viewModel.streakSummary.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title = "Streaks & Achievements") }
        item { StreakCard(summary = streakSummary) }
        item { TotalsRow(summary = streakSummary) }
        item { SectionHeader(title = "Achievements") }
        items(achievements, key = { it.id.name }) { achievement ->
            AchievementCard(achievement = achievement)
        }
    }
}

@Composable
private fun StreakCard(summary: StreakSummary, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = "\uD83D\uDD25 ${summary.currentStreakDays} day${if (summary.currentStreakDays == 1) "" else "s"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Current streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Best: ${summary.bestStreakDays} day${if (summary.bestStreakDays == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${summary.weeklyConsistencyPercent}% of the last 7 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsRow(summary: StreakSummary, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatMiniCard(
            title = "Total focused",
            value = formatDurationHoursMinutes(summary.totalFocusedMillis),
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            title = "Sessions",
            value = summary.completedSessionCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            title = "Tasks done",
            value = summary.completedTaskCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val contentAlpha = if (achievement.unlocked) 1f else 0.5f
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = if (achievement.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                contentDescription = if (achievement.unlocked) "Unlocked" else "Locked",
                tint = if (achievement.unlocked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
            )
            Column {
                Text(
                    text = achievement.id.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = achievement.id.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha * 0.8f)
                )
            }
        }
    }
}
