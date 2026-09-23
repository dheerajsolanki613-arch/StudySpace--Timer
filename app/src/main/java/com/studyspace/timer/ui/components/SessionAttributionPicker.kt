package com.studyspace.timer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity

/**
 * Optional per-session attribution shown by every timer mode's setup UI
 * (Self-Study, Online Study, Normal Timer, Pomodoro, Focus Mode) while
 * idle/in setup — a custom label, an optional subject, and an optional
 * task, added in Phase 5 ("timer integration") to finally give Subjects
 * (Phase 3) and Tasks (Phase 4) a way to actually accumulate sessions
 * instead of only ever showing zero.
 *
 * [tasks] is expected to already be pre-filtered by the caller's ViewModel
 * (incomplete only, and by [selectedSubjectId] if one is chosen) — this
 * composable just renders whatever list it's given and hides the task row
 * entirely when that list is empty, rather than showing a pointless
 * "None"-only row.
 */
@Composable
fun SessionAttributionPicker(
    subjects: List<SubjectEntity>,
    tasks: List<TaskEntity>,
    selectedSubjectId: Long?,
    selectedTaskId: Long?,
    customLabel: String,
    onSubjectSelected: (Long?) -> Unit,
    onTaskSelected: (Long?) -> Unit,
    onLabelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = customLabel,
            onValueChange = onLabelChange,
            label = { Text("Session label (optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (subjects.isNotEmpty()) {
            FieldLabel("Subject")
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSubjectId == null,
                    onClick = { onSubjectSelected(null) },
                    label = { Text("None") }
                )
                subjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubjectId == subject.id,
                        onClick = { onSubjectSelected(subject.id) },
                        label = { Text("${subject.icon} ${subject.name}") }
                    )
                }
            }
        }

        if (tasks.isNotEmpty()) {
            FieldLabel("Task")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTaskId == null,
                    onClick = { onTaskSelected(null) },
                    label = { Text("None") }
                )
                tasks.forEach { task ->
                    FilterChip(
                        selected = selectedTaskId == task.id,
                        onClick = { onTaskSelected(task.id) },
                        label = { Text(task.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Narrows a task list to one subject, or leaves it alone if no subject is
 * selected — the exact filtering [SessionAttributionPicker]'s doc says its
 * `tasks` param expects. Shared here so every call site (Self-Study/Online
 * Study, Normal Timer, Pomodoro, Focus Mode) filters the same way.
 */
fun List<TaskEntity>.filteredForSubject(subjectId: Long?): List<TaskEntity> =
    if (subjectId == null) this else filter { it.subjectId == subjectId }
