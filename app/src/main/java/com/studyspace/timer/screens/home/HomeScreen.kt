package com.studyspace.timer.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.timer.formatRelativeTime
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.GlassCardAccent
import com.studyspace.timer.ui.components.ProgressRing
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.util.isLandscape

private data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color
)

/**
 * Home dashboard: greeting, today's study progress, quick-action grid for the
 * five timer modes, and a recent-activity section.
 *
 * Stage 5: today's time, session count, this-week total, streak, and recent
 * activity are real numbers from Room via [HomeViewModel]. Stage 8: the
 * daily goal is user-configurable (Settings tab), read from
 * [HomeViewModel.dailyGoalMillis].
 *
 * Theme/rotation update: every color below reads from `MaterialTheme.colorScheme`
 * (set by the active [com.studyspace.timer.ui.theme.AppPalette]) instead of
 * fixed `Galaxy*` constants, and the whole screen has two layouts — a single
 * scrolling column in portrait, a two-pane row (hero/stats left, quick
 * actions + recent activity right) in landscape — chosen once via
 * [isLandscape] rather than duplicating the screen.
 */
@Composable
fun HomeScreen(
    onOpenTimer: () -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenFocus: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val dailyGoalMillis by viewModel.dailyGoalMillis.collectAsState()
    val scheme = MaterialTheme.colorScheme

    val quickActions = listOf(
        QuickAction("Self-Study", "Untimed focus session", Icons.Filled.MenuBook, scheme.primary),
        QuickAction("Online Study", "Track a class or lecture", Icons.Filled.CloudQueue, scheme.secondary),
        QuickAction("Normal Timer", "Simple countdown", Icons.Filled.Timer, scheme.tertiary),
        QuickAction("Pomodoro", "Work / break cycles", Icons.Filled.Bolt, scheme.primaryContainer),
        QuickAction("Focus Mode", "Distraction-free session", Icons.Filled.CenterFocusStrong, scheme.secondary)
    )

    val onQuickActionClick: (String) -> Unit = { title ->
        when (title) {
            "Pomodoro" -> onOpenPomodoro()
            "Focus Mode" -> onOpenFocus()
            else -> onOpenTimer()
        }
    }

    if (isLandscape()) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { GreetingBlock() }
                item { TodayProgressCard(stats = stats, dailyGoalMillis = dailyGoalMillis) }
                item { StatsRow(stats = stats) }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { SectionHeader(title = "Quick Actions") }
                item {
                    QuickActionsGrid(
                        quickActions = quickActions,
                        columns = 2,
                        onClick = onQuickActionClick
                    )
                }
                item { SectionHeader(title = "Recent Activity") }
                recentActivityItems(recentSessions)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { GreetingBlock() }
            item { TodayProgressCard(stats = stats, dailyGoalMillis = dailyGoalMillis) }
            item { StatsRow(stats = stats) }
            item { SectionHeader(title = "Quick Actions") }
            item {
                QuickActionsGrid(
                    quickActions = quickActions,
                    columns = 2,
                    onClick = onQuickActionClick
                )
            }
            item { SectionHeader(title = "Recent Activity") }
            recentActivityItems(recentSessions)
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun GreetingBlock() {
    Column {
        Text(
            text = "Good to see you \uD83D\uDC4B",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Let's make today count.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun TodayProgressCard(
    stats: com.studyspace.timer.data.repository.StudyStats,
    dailyGoalMillis: Long
) {
    val progress = (stats.todayTotalMillis.toFloat() / dailyGoalMillis.toFloat()).coerceIn(0f, 1f)
    GlassCardAccent(modifier = Modifier.fillMaxWidth(), accentColor = MaterialTheme.colorScheme.secondary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Today's study time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = formatDurationHoursMinutes(stats.todayTotalMillis),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Goal: ${formatDurationHoursMinutes(dailyGoalMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            ProgressRing(
                progress = progress,
                size = 84,
                strokeWidth = 8,
                centerLabel = "${(progress * 100).toInt()}%"
            )
        }
    }
}

@Composable
private fun StatsRow(stats: com.studyspace.timer.data.repository.StudyStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatMiniCard(
            title = "Sessions",
            value = stats.todaySessionCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            title = "Streak",
            value = "${stats.streakDays} days",
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            title = "This week",
            value = formatDurationHoursMinutes(stats.weekTotalMillis),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionsGrid(
    quickActions: List<QuickAction>,
    columns: Int,
    onClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.height(300.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quickActions) { action ->
            com.studyspace.timer.ui.components.FeatureCard(
                title = action.title,
                subtitle = action.subtitle,
                icon = action.icon,
                accentColor = action.accent,
                onClick = { onClick(action.title) }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recentActivityItems(
    recentSessions: List<StudySessionEntity>
) {
    if (recentSessions.isEmpty()) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    NoSessionsText()
                }
            }
        }
    } else {
        items(recentSessions) { session ->
            RecentSessionRow(session)
        }
    }
}

@Composable
private fun NoSessionsText() {
    Text(
        text = "No sessions yet",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
    Text(
        text = "Start a timer above and it'll show up here once you finish or stop it.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
}

@Composable
private fun StatMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
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

@Composable
private fun RecentSessionRow(session: StudySessionEntity, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatRelativeTime(session.startEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = formatDurationHoursMinutes(session.durationMillis),
                style = MaterialTheme.typography.titleMedium,
                color = if (session.completedNaturally) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                }
            )
        }
    }
}
