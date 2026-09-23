package com.studyspace.timer.screens.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.studyspace.timer.ui.components.StatMiniCard
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
    onOpenTimerTab: (Int) -> Unit,
    onOpenPomodoro: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenDailySummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val dailyGoalMillis by viewModel.dailyGoalMillis.collectAsState()
    val todaySubjectTotals by viewModel.todaySubjectTotals.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }
    val todaysPlans by viewModel.todaysPlans.collectAsState()
    val remainingTasks by viewModel.remainingTasks.collectAsState()
    val scheme = MaterialTheme.colorScheme

    val quickActions = listOf(
        QuickAction("Self-Study", "Untimed focus session", Icons.Filled.MenuBook, scheme.primary),
        QuickAction("Online Study", "Track a class or lecture", Icons.Filled.CloudQueue, scheme.secondary),
        QuickAction("Normal Timer", "Simple countdown", Icons.Filled.Timer, scheme.tertiary),
        QuickAction("Pomodoro", "Work / break cycles", Icons.Filled.Bolt, scheme.primaryContainer),
        QuickAction("Focus Mode", "Distraction-free session", Icons.Filled.CenterFocusStrong, scheme.secondary),
        QuickAction("Study Goals", "Daily & weekly targets", Icons.Filled.TrackChanges, scheme.tertiary),
        QuickAction("Subjects", "Manage & track by subject", Icons.Filled.Category, scheme.primary),
        QuickAction("Tasks", "To-dos with deadlines", Icons.Filled.Checklist, scheme.secondaryContainer),
        QuickAction("Planner", "Plan your study blocks", Icons.Filled.EditCalendar, scheme.tertiary),
        QuickAction("Achievements", "Streaks & unlocked badges", Icons.Filled.EmojiEvents, scheme.secondaryContainer),
        QuickAction("Daily Summary", "Review your day", Icons.Filled.Today, scheme.primary)
    )

    val onQuickActionClick: (String) -> Unit = { title ->
        when (title) {
            "Pomodoro" -> onOpenPomodoro()
            "Focus Mode" -> onOpenFocus()
            "Study Goals" -> onOpenGoals()
            "Subjects" -> onOpenSubjects()
            "Tasks" -> onOpenTasks()
            "Planner" -> onOpenPlanner()
            "Achievements" -> onOpenAchievements()
            "Daily Summary" -> onOpenDailySummary()
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
                item { SectionHeader(title = "Quick Start") }
                item {
                    QuickStartRow(
                        onStartStopwatch = onOpenTimer,
                        onStartPomodoro = onOpenPomodoro,
                        onStartCustomTimer = { onOpenTimerTab(2) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { SectionHeader(title = "Subject Breakdown", actionLabel = "See all", onActionClick = onOpenSubjects) }
                item { SubjectBreakdownSection(totals = todaySubjectTotals, subjectsById = subjectsById) }
                item { SectionHeader(title = "Today's Plan", actionLabel = "See all", onActionClick = onOpenPlanner) }
                item { TodaysPlanSection(plans = todaysPlans, subjectsById = subjectsById, onOpenPlanner = onOpenPlanner) }
                item { SectionHeader(title = "Tasks", actionLabel = "See all", onActionClick = onOpenTasks) }
                item { RemainingTasksSection(tasks = remainingTasks, onOpenTasks = onOpenTasks) }
                item { SectionHeader(title = "All Features") }
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
            item { SectionHeader(title = "Quick Start") }
            item {
                QuickStartRow(
                    onStartStopwatch = onOpenTimer,
                    onStartPomodoro = onOpenPomodoro,
                    onStartCustomTimer = { onOpenTimerTab(2) }
                )
            }
            item { SectionHeader(title = "Subject Breakdown", actionLabel = "See all", onActionClick = onOpenSubjects) }
            item { SubjectBreakdownSection(totals = todaySubjectTotals, subjectsById = subjectsById) }
            item { SectionHeader(title = "Today's Plan", actionLabel = "See all", onActionClick = onOpenPlanner) }
            item { TodaysPlanSection(plans = todaysPlans, subjectsById = subjectsById, onOpenPlanner = onOpenPlanner) }
            item { SectionHeader(title = "Tasks", actionLabel = "See all", onActionClick = onOpenTasks) }
            item { RemainingTasksSection(tasks = remainingTasks, onOpenTasks = onOpenTasks) }
            item { SectionHeader(title = "All Features") }
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
            value = "\uD83D\uDD25 ${stats.streakDays} days",
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            title = "This week",
            value = formatDurationHoursMinutes(stats.weekTotalMillis),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Phase 10 (Daily Dashboard) "Quick Start": the 3 single-tap actions the
 * spec calls out by name, distinct from the fuller "All Features" grid
 * below (which still has its own Self-Study/Pomodoro/Normal-Timer tiles —
 * kept, not removed, since existing navigation paths stay working per this
 * project's own rules; Quick Start is a faster on-ramp to the 3 most-used
 * of those 10, not a replacement for the rest).
 */
@Composable
private fun QuickStartRow(
    onStartStopwatch: () -> Unit,
    onStartPomodoro: () -> Unit,
    onStartCustomTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        com.studyspace.timer.ui.components.PrimaryButton(
            text = "Stopwatch",
            onClick = onStartStopwatch,
            icon = Icons.Filled.MenuBook,
            modifier = Modifier.weight(1f)
        )
        com.studyspace.timer.ui.components.PrimaryButton(
            text = "Pomodoro",
            onClick = onStartPomodoro,
            icon = Icons.Filled.Bolt,
            modifier = Modifier.weight(1f)
        )
        com.studyspace.timer.ui.components.PrimaryButton(
            text = "Custom",
            onClick = onStartCustomTimer,
            icon = Icons.Filled.Timer,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Phase 10 "Subject Breakdown": today's per-subject totals, reusing Phase
 * 8's [com.studyspace.timer.data.repository.SubjectTotal]/
 * [com.studyspace.timer.data.repository.computeSubjectTotals] rather than
 * inventing a second aggregation. Sessions with no subject attributed are
 * silently excluded here (unlike Analytics' Phase 8 "By Subject", which
 * shows an explicit "No subject" row) — Analytics is a full accounting of
 * every session, so an unattributed bucket belongs there; a home dashboard
 * card is a quick glance at *categorized* progress, and an "unattributed:
 * 42m" row would be more confusing than useful in that smaller, faster
 * context. The full, honest picture (including unattributed time) is one
 * tap away via this section's own "See all" link to Analytics/Subjects.
 */
@Composable
private fun SubjectBreakdownSection(
    totals: List<com.studyspace.timer.data.repository.SubjectTotal>,
    subjectsById: Map<Long, com.studyspace.timer.data.db.SubjectEntity>,
    modifier: Modifier = Modifier
) {
    val attributed = totals.filter { it.subjectId != null }
    if (attributed.isEmpty()) {
        GlassCard(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "No subject-tagged study time yet today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            attributed.forEach { total ->
                val subject = subjectsById[total.subjectId]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = subject?.let { "${it.icon} ${it.name}" } ?: "Untitled subject",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDurationHoursMinutes(total.totalMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Phase 10 "Today's Plan": up to 3 of today's planned blocks (Phase 6),
 * cheapest-effort dashboard glance rather than the Planner screen's full
 * management view — capped and pointing at "See all" (this section's
 * [SectionHeader] action, wired to [onOpenPlanner] by the caller) rather
 * than duplicating Planner's Start/Mark done/Cancel actions inline here.
 */
@Composable
private fun TodaysPlanSection(
    plans: List<com.studyspace.timer.data.db.PlannedSessionEntity>,
    subjectsById: Map<Long, com.studyspace.timer.data.db.SubjectEntity>,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (plans.isEmpty()) {
        GlassCard(
            modifier = modifier.fillMaxWidth().clickable(onClick = onOpenPlanner),
            content = {
                Text(
                    text = "Nothing planned for today — tap to add a study block.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plans.take(3).forEach { plan ->
            val subject = plan.subjectId?.let { subjectsById[it] }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = com.studyspace.timer.data.repository.formatPlannedTimeRange(
                                plan.startMinuteOfDay,
                                plan.durationMinutes
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        subject?.let {
                            Text(
                                text = "${it.icon} ${it.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
        if (plans.size > 3) {
            Text(
                text = "+${plans.size - 3} more today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Phase 10 "Tasks": up to 5 incomplete tasks
 * ([HomeViewModel.remainingTasks] already caps and sorts them), same
 * "dashboard glance, full management lives one tap away" shape as
 * [TodaysPlanSection] above.
 */
@Composable
private fun RemainingTasksSection(
    tasks: List<com.studyspace.timer.data.db.TaskEntity>,
    onOpenTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        GlassCard(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "No open tasks. Nice and clear.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tasks.forEach { task ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = task.priority.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    quickActions: List<QuickAction>,
    columns: Int,
    onClick: (String) -> Unit
) {
    // Height computed from the actual row count (rather than a fixed
    // constant) so an added/removed tile never clips or leaves dead space —
    // the fixed 300.dp this used to be was sized for 4 tiles/2 rows and
    // silently clipped the 5th (now 6th) tile's row. userScrollEnabled is
    // off since the grid is sized to fit its content exactly; the outer
    // LazyColumn (this grid's caller) handles scrolling the whole screen.
    val rows = (quickActions.size + columns - 1) / columns
    val gridHeight = (132 * rows + 12 * (rows - 1).coerceAtLeast(0)).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.height(gridHeight),
        userScrollEnabled = false,
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
