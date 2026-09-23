package com.studyspace.timer.screens.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.repository.formatPlannedTimeRange
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.components.StatMiniCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Study Planner: today's planned sessions (with Start / Mark done / Cancel
 * actions and a planned/completed/missed count row) plus an "Upcoming"
 * section for future plans. "Start" navigates to the Timer screen's
 * Self-Study tab with the plan's subject/task pre-selected (reusing the
 * Phase-5 [com.studyspace.timer.timer.StopwatchTimerViewModel.selectSubject]/
 * `selectTask` machinery via [com.studyspace.timer.navigation.Screen.TimerFromPlan])
 * — see [PlannedSessionEntity]'s class doc for why the resulting session
 * isn't auto-linked back to this plan.
 */
@Composable
fun PlannerScreen(
    onStartPlan: (subjectId: Long?, taskId: Long?) -> Unit,
    viewModel: PlannerViewModel = viewModel()
) {
    val today by viewModel.today.collectAsState()
    val upcoming by viewModel.upcoming.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }
    val tasksById = remember(tasks) { tasks.associateBy { it.id } }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlan: PlannedSessionEntity? by remember { mutableStateOf(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(title = "Planner", actionLabel = "Add", onActionClick = { showAddDialog = true })
        }
        item { TodayCountsRow(today) }
        if (today.isEmpty()) {
            item { EmptyPlanCard(onAdd = { showAddDialog = true }) }
        } else {
            items(today, key = { it.id }) { plan ->
                PlannedSessionRow(
                    plan = plan,
                    subjectLabel = plan.subjectId?.let { subjectsById[it]?.let { s -> "${s.icon} ${s.name}" } },
                    taskLabel = plan.taskId?.let { tasksById[it]?.title },
                    dateLabel = null,
                    onClick = { editingPlan = plan },
                    onStart = { onStartPlan(plan.subjectId, plan.taskId) },
                    onMarkComplete = { viewModel.markCompleted(plan) },
                    onCancel = { viewModel.cancelPlan(plan) }
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item { SectionHeader(title = "Upcoming") }
            items(upcoming, key = { it.id }) { plan ->
                PlannedSessionRow(
                    plan = plan,
                    subjectLabel = plan.subjectId?.let { subjectsById[it]?.let { s -> "${s.icon} ${s.name}" } },
                    taskLabel = plan.taskId?.let { tasksById[it]?.title },
                    dateLabel = formatPlanDate(plan.dateEpochDay),
                    onClick = { editingPlan = plan },
                    onStart = null,
                    onMarkComplete = null,
                    onCancel = { viewModel.cancelPlan(plan) }
                )
            }
        }
    }

    if (showAddDialog) {
        PlannedSessionEditorDialog(
            title = "New plan",
            initialDateOffsetDays = 0L,
            initialStartMinuteOfDay = 16 * 60,
            initialDurationMinutes = 60,
            initialSubjectId = null,
            initialTaskId = null,
            initialNotes = null,
            subjects = subjects,
            tasks = tasks,
            onDismiss = { showAddDialog = false },
            onConfirm = { result ->
                viewModel.createPlan(
                    result.dateEpochDay, result.startMinuteOfDay, result.durationMinutes,
                    result.subjectId, result.taskId, result.notes
                )
                showAddDialog = false
            }
        )
    }

    editingPlan?.let { plan ->
        PlannedSessionEditorDialog(
            title = "Edit plan",
            initialDateOffsetDays = plan.dateEpochDay - LocalDate.now().toEpochDay(),
            initialStartMinuteOfDay = plan.startMinuteOfDay,
            initialDurationMinutes = plan.durationMinutes,
            initialSubjectId = plan.subjectId,
            initialTaskId = plan.taskId,
            initialNotes = plan.notes,
            subjects = subjects,
            tasks = tasks,
            onDismiss = { editingPlan = null },
            onConfirm = { result ->
                viewModel.updatePlan(
                    plan, result.dateEpochDay, result.startMinuteOfDay, result.durationMinutes,
                    result.subjectId, result.taskId, result.notes
                )
                editingPlan = null
            },
            onDelete = {
                viewModel.deletePlan(plan)
                editingPlan = null
            }
        )
    }
}

private fun formatPlanDate(dateEpochDay: Long): String {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val today = LocalDate.now()
    return when (dateEpochDay - today.toEpochDay()) {
        1L -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
}

@Composable
private fun TodayCountsRow(today: List<PlannedSessionEntity>) {
    val planned = today.count { it.status == PlannedSessionStatus.PLANNED.name }
    val completed = today.count { it.status == PlannedSessionStatus.COMPLETED.name }
    val missed = today.count { it.status == PlannedSessionStatus.MISSED.name }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatMiniCard(title = "Planned", value = planned.toString(), modifier = Modifier.weight(1f))
        StatMiniCard(title = "Completed", value = completed.toString(), modifier = Modifier.weight(1f))
        StatMiniCard(title = "Missed", value = missed.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EmptyPlanCard(onAdd: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd)) {
        Column {
            Text(
                text = "Nothing planned for today",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add a time block, optionally with a subject and task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PlannedSessionRow(
    plan: PlannedSessionEntity,
    subjectLabel: String?,
    taskLabel: String?,
    dateLabel: String?,
    onClick: () -> Unit,
    onStart: (() -> Unit)?,
    onMarkComplete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    val status = PlannedSessionStatus.entries.firstOrNull { it.name == plan.status } ?: PlannedSessionStatus.PLANNED
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val timeLine = buildString {
                        dateLabel?.let { append("$it  •  ") }
                        append(formatPlannedTimeRange(plan.startMinuteOfDay, plan.durationMinutes))
                    }
                    Text(
                        text = timeLine,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val subtitleParts = buildList {
                        subjectLabel?.let { add(it) }
                        taskLabel?.let { add(it) }
                        plan.notes?.let { add(it) }
                    }
                    if (subtitleParts.isNotEmpty()) {
                        Text(
                            text = subtitleParts.joinToString("  •  "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    text = statusLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(status)
                )
            }
            if (status == PlannedSessionStatus.PLANNED) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onStart != null) {
                        TextButton(onClick = onStart) { Text("Start") }
                    }
                    if (onMarkComplete != null) {
                        TextButton(onClick = onMarkComplete) { Text("Mark done") }
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

private fun statusLabel(status: PlannedSessionStatus): String = when (status) {
    PlannedSessionStatus.PLANNED -> "Planned"
    PlannedSessionStatus.COMPLETED -> "Done"
    PlannedSessionStatus.MISSED -> "Missed"
    PlannedSessionStatus.CANCELLED -> "Cancelled"
}

@Composable
private fun statusColor(status: PlannedSessionStatus) = when (status) {
    PlannedSessionStatus.PLANNED -> MaterialTheme.colorScheme.tertiary
    PlannedSessionStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    PlannedSessionStatus.MISSED -> MaterialTheme.colorScheme.error
    PlannedSessionStatus.CANCELLED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
}
