package com.studyspace.timer.screens.planner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.data.repository.PlannedSessionRepository
import com.studyspace.timer.ui.components.filteredForSubject
import java.time.LocalDate

/** A completed edit from [PlannedSessionEditorDialog]'s onConfirm. */
data class PlannedSessionEditorResult(
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val subjectId: Long?,
    val taskId: Long?,
    val notes: String?
)

/**
 * Create-or-edit dialog for a planned study block, used by [PlannerScreen]
 * (create) and its edit flow. Date/start-time/duration are all fixed preset
 * chips ([PlannedSessionRepository]'s `*_PRESETS`) — same lower-risk choice
 * `TaskEditorDialog`'s deadline chips already made rather than a calendar/
 * time picker widget. Subject/task chips reuse the same
 * [filteredForSubject] helper `SessionAttributionPicker` uses, so picking a
 * subject narrows the task list to that subject's tasks, and picking a task
 * auto-selects its subject.
 */
@Composable
fun PlannedSessionEditorDialog(
    title: String,
    initialDateOffsetDays: Long,
    initialStartMinuteOfDay: Int,
    initialDurationMinutes: Int,
    initialSubjectId: Long?,
    initialTaskId: Long?,
    initialNotes: String?,
    subjects: List<SubjectEntity>,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onConfirm: (PlannedSessionEditorResult) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var selectedDateOffset by remember { mutableStateOf(initialDateOffsetDays) }
    var selectedStartMinute by remember { mutableStateOf(initialStartMinuteOfDay) }
    var selectedDuration by remember { mutableStateOf(initialDurationMinutes) }
    var selectedSubjectId by remember { mutableStateOf(initialSubjectId) }
    var selectedTaskId by remember { mutableStateOf(initialTaskId) }
    var notes by remember { mutableStateOf(initialNotes.orEmpty()) }

    val visibleTasks = remember(tasks, selectedSubjectId) { tasks.filteredForSubject(selectedSubjectId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                FieldLabel("Date")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlannedSessionRepository.DATE_OFFSET_PRESETS.forEach { (label, offset) ->
                        FilterChip(
                            selected = selectedDateOffset == offset,
                            onClick = { selectedDateOffset = offset },
                            label = { Text(label) }
                        )
                    }
                }

                FieldLabel("Start time")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlannedSessionRepository.START_TIME_PRESETS.forEach { (label, minute) ->
                        FilterChip(
                            selected = selectedStartMinute == minute,
                            onClick = { selectedStartMinute = minute },
                            label = { Text(label) }
                        )
                    }
                }

                FieldLabel("Duration")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlannedSessionRepository.DURATION_PRESETS.forEach { (label, minutes) ->
                        FilterChip(
                            selected = selectedDuration == minutes,
                            onClick = { selectedDuration = minutes },
                            label = { Text(label) }
                        )
                    }
                }

                FieldLabel("Subject")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = {
                            selectedSubjectId = null
                            selectedTaskId = null
                        },
                        label = { Text("None") }
                    )
                    subjects.forEach { subject ->
                        FilterChip(
                            selected = selectedSubjectId == subject.id,
                            onClick = { selectedSubjectId = subject.id },
                            label = { Text("${subject.icon} ${subject.name}") }
                        )
                    }
                }

                if (visibleTasks.isNotEmpty()) {
                    FieldLabel("Task")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTaskId == null,
                            onClick = { selectedTaskId = null },
                            label = { Text("None") }
                        )
                        visibleTasks.forEach { task ->
                            FilterChip(
                                selected = selectedTaskId == task.id,
                                onClick = {
                                    selectedTaskId = task.id
                                    selectedSubjectId = task.subjectId ?: selectedSubjectId
                                },
                                label = { Text(task.title) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        PlannedSessionEditorResult(
                            dateEpochDay = LocalDate.now().toEpochDay() + selectedDateOffset,
                            startMinuteOfDay = selectedStartMinute,
                            durationMinutes = selectedDuration,
                            subjectId = selectedSubjectId,
                            taskId = selectedTaskId,
                            notes = notes
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
