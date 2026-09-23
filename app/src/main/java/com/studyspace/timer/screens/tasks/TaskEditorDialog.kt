package com.studyspace.timer.screens.tasks

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
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.SubjectEntity
import java.time.LocalDate

/** A completed edit from [TaskEditorDialog]'s onConfirm. */
data class TaskEditorResult(
    val title: String,
    val chapter: String?,
    val topic: String?,
    val subjectId: Long?,
    val priority: TaskPriority,
    val deadlineEpochDay: Long?,
    val estimatedDurationMinutes: Int?
)

private val DEADLINE_PRESETS: List<Pair<String, Long?>> = listOf(
    "None" to null,
    "Today" to 0L,
    "Tomorrow" to 1L,
    "In 3 days" to 3L,
    "In 1 week" to 7L,
    "In 2 weeks" to 14L
)

private val DURATION_PRESETS: List<Pair<String, Int?>> = listOf(
    "None" to null,
    "15m" to 15,
    "25m" to 25,
    "45m" to 45,
    "60m" to 60,
    "90m" to 90
)

/**
 * Create-or-edit dialog for a task, used by [TasksScreen] (create) and its
 * edit flow (same composable, initial values supplied). Deadline and
 * estimated duration are chosen from fixed preset chips — same
 * lower-risk-than-a-calendar-widget choice
 * [com.studyspace.timer.screens.goals.GoalsScreen]'s weekly-goal picker
 * already made, rather than introducing this project's first use of
 * Material3's (still-experimental-at-this-library-version) `DatePicker`.
 */
@Composable
fun TaskEditorDialog(
    title: String,
    initialTitle: String,
    initialChapter: String?,
    initialTopic: String?,
    initialSubjectId: Long?,
    initialPriority: TaskPriority,
    initialDeadlineEpochDay: Long?,
    initialEstimatedDurationMinutes: Int?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (TaskEditorResult) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var taskTitle by remember { mutableStateOf(initialTitle) }
    var chapter by remember { mutableStateOf(initialChapter.orEmpty()) }
    var topic by remember { mutableStateOf(initialTopic.orEmpty()) }
    var selectedSubjectId by remember { mutableStateOf(initialSubjectId) }
    var selectedPriority by remember { mutableStateOf(initialPriority) }
    // Stored as an offset from "today" (in days) rather than an absolute
    // epoch day, so re-opening this dialog on a later day still highlights
    // the right preset chip (e.g. a task due "in 3 days" still shows that
    // chip selected the next time it's edited, even though the absolute
    // deadlineEpochDay is now only 2 days out).
    var selectedDeadlineOffset by remember {
        mutableStateOf(initialDeadlineEpochDay?.let { it - LocalDate.now().toEpochDay() })
    }
    var selectedDuration by remember { mutableStateOf(initialEstimatedDurationMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(modifier = Modifier.padding(bottom = 12.dp)) {
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("Chapter (optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Topic (optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                }

                FieldLabel("Subject")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = { selectedSubjectId = null },
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

                FieldLabel("Priority")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.displayLabel) }
                        )
                    }
                }

                FieldLabel("Deadline")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DEADLINE_PRESETS.forEach { (label, offset) ->
                        FilterChip(
                            selected = selectedDeadlineOffset == offset,
                            onClick = { selectedDeadlineOffset = offset },
                            label = { Text(label) }
                        )
                    }
                }

                FieldLabel("Estimated duration")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DURATION_PRESETS.forEach { (label, minutes) ->
                        FilterChip(
                            selected = selectedDuration == minutes,
                            onClick = { selectedDuration = minutes },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        TaskEditorResult(
                            title = taskTitle.trim(),
                            chapter = chapter,
                            topic = topic,
                            subjectId = selectedSubjectId,
                            priority = selectedPriority,
                            deadlineEpochDay = selectedDeadlineOffset?.let { LocalDate.now().toEpochDay() + it },
                            estimatedDurationMinutes = selectedDuration
                        )
                    )
                },
                enabled = taskTitle.isNotBlank()
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
