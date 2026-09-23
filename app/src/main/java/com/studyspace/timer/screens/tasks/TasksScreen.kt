package com.studyspace.timer.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.data.repository.TaskDueUrgency
import com.studyspace.timer.data.repository.computeTaskDueInfo
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import java.time.LocalDate

/**
 * Tasks list: every task across all subjects, incomplete first ordered by
 * soonest deadline (see [com.studyspace.timer.data.db.TaskDao.allTasks]),
 * tap a row to edit it, checkbox to toggle complete, "+ Add" to create one.
 * No "start timer from this task" action here yet — that needs the timer
 * screens to accept a task/subject to attribute the session to, which is a
 * later phase's job (see `PROJECT_STATE.md`'s Phase 4 entry); a button that
 * couldn't actually do that would be a placeholder control, which this
 * project's rules rule out.
 */
@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask: TaskEntity? by remember { mutableStateOf(null) }
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Tasks",
                actionLabel = "Add",
                onActionClick = { showAddDialog = true }
            )
        }
        if (tasks.isEmpty()) {
            item { EmptyTasksCard(onAdd = { showAddDialog = true }) }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    subject = task.subjectId?.let { subjectsById[it] },
                    todayEpochDay = todayEpochDay,
                    onToggleComplete = { viewModel.setCompleted(task, !task.completed) },
                    onClick = { editingTask = task }
                )
            }
        }
    }

    if (showAddDialog) {
        TaskEditorDialog(
            title = "New task",
            initialTitle = "",
            initialChapter = null,
            initialTopic = null,
            initialSubjectId = null,
            initialPriority = TaskPriority.MEDIUM,
            initialDeadlineEpochDay = null,
            initialEstimatedDurationMinutes = null,
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { result ->
                viewModel.createTask(
                    result.title, result.chapter, result.topic, result.subjectId,
                    result.priority, result.deadlineEpochDay, result.estimatedDurationMinutes
                )
                showAddDialog = false
            }
        )
    }

    editingTask?.let { task ->
        TaskEditorDialog(
            title = "Edit task",
            initialTitle = task.title,
            initialChapter = task.chapter,
            initialTopic = task.topic,
            initialSubjectId = task.subjectId,
            initialPriority = TaskPriority.entries.firstOrNull { it.name == task.priority } ?: TaskPriority.MEDIUM,
            initialDeadlineEpochDay = task.deadlineEpochDay,
            initialEstimatedDurationMinutes = task.estimatedDurationMinutes,
            subjects = subjects,
            onDismiss = { editingTask = null },
            onConfirm = { result ->
                viewModel.updateTask(
                    task, result.title, result.chapter, result.topic, result.subjectId,
                    result.priority, result.deadlineEpochDay, result.estimatedDurationMinutes
                )
                editingTask = null
            },
            onDelete = {
                viewModel.deleteTask(task)
                editingTask = null
            }
        )
    }
}

@Composable
private fun EmptyTasksCard(onAdd: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
    ) {
        Column {
            Text(
                text = "No tasks yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add a task, optionally under a subject, with a priority and deadline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    subject: SubjectEntity?,
    todayEpochDay: Long,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val dueInfo = computeTaskDueInfo(task.deadlineEpochDay, todayEpochDay)
    val priority = TaskPriority.entries.firstOrNull { it.name == task.priority }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggleComplete() })
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                val subtitleParts = buildList {
                    subject?.let { add("${it.icon} ${it.name}") }
                    priority?.let { add(it.displayLabel) }
                    dueInfo.label?.let { add(it) }
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dueInfo.urgency == TaskDueUrgency.OVERDUE && !task.completed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}
