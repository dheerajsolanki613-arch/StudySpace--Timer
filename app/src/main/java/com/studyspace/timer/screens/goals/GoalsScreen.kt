package com.studyspace.timer.screens.goals

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.repository.GoalRepository
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.GoalProgressCard
import com.studyspace.timer.ui.components.SectionHeader

/**
 * Study Goals screen: today's progress against the existing Settings daily
 * goal, the new weekly goal (editable here), and an honest placeholder for
 * per-subject goals — those need the Subject system, a later phase, so
 * this doesn't ship a button that can't do anything yet (see the project's
 * rule against placeholder controls).
 *
 * Reached from Home's "Study Goals" quick action rather than a bottom-nav
 * tab, same pattern as Pomodoro/Focus — it's a destination, not a
 * top-level section of the app.
 */
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = viewModel()) {
    val dailyProgress by viewModel.dailyGoalProgress.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()
    val weeklyProgress by viewModel.weeklyGoalProgress.collectAsState()
    var showWeeklyPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionHeader(title = "Study Goals") }
        item {
            GoalProgressCard(
                progress = dailyProgress,
                accentColor = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            SectionHeader(
                title = "Weekly Goal",
                actionLabel = if (weeklyGoal != null) "Edit" else "Set goal",
                onActionClick = { showWeeklyPicker = true }
            )
        }
        item {
            if (weeklyProgress != null) {
                GoalProgressCard(
                    progress = weeklyProgress!!,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            } else {
                EmptyWeeklyGoalCard(onSetGoal = { showWeeklyPicker = true })
            }
        }
        item { SectionHeader(title = "Subject Goals") }
        item { SubjectGoalsComingSoonCard() }
    }

    if (showWeeklyPicker) {
        WeeklyGoalPickerDialog(
            currentHours = weeklyGoal?.targetMinutes?.div(60),
            onDismiss = { showWeeklyPicker = false },
            onConfirm = { hours ->
                viewModel.setWeeklyGoalHours(hours)
                showWeeklyPicker = false
            },
            onClear = if (weeklyGoal != null) {
                {
                    viewModel.clearWeeklyGoal()
                    showWeeklyPicker = false
                }
            } else null
        )
    }
}

@Composable
private fun EmptyWeeklyGoalCard(onSetGoal: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Icon(imageVector = Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(
                text = "No weekly goal set",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Set a weekly target and this card will track your rolling 7-day total against it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            TextButton(onClick = onSetGoal) {
                Text(text = "Set weekly goal", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun SubjectGoalsComingSoonCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Coming with Subjects",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Per-subject targets (e.g. Mathematics — 10h/week) unlock once the Subject system is built — a session needs a subject to attach a goal to.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun WeeklyGoalPickerDialog(
    currentHours: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onClear: (() -> Unit)?
) {
    var selectedHours by remember { mutableStateOf(currentHours ?: GoalRepository.WEEKLY_GOAL_PRESET_HOURS[3]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weekly study goal") },
        text = {
            Column {
                Text(
                    text = "Hours per week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalRepository.WEEKLY_GOAL_PRESET_HOURS.forEach { hours ->
                        FilterChip(
                            selected = selectedHours == hours,
                            onClick = { selectedHours = hours },
                            label = { Text("${hours}h") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHours) }) {
                Text("Save")
            }
        },
        dismissButton = {
            if (onClear != null) {
                TextButton(onClick = onClear) { Text("Remove goal") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
