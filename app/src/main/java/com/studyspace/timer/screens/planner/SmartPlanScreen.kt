package com.studyspace.timer.screens.planner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.repository.formatPlannedTimeRange
import com.studyspace.timer.planning.SuggestedSession
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.SectionHeader

/**
 * Phase 17 (Smart Study Planning). Pushed from [PlannerScreen]'s "Suggest a
 * schedule" button. This screen never writes a planned session on its own —
 * [SmartPlanViewModel.generate] only ever produces an in-memory preview
 * ([SmartPlanUiState.Preview]), which the person can edit by unchecking any
 * row they don't want, before [SmartPlanViewModel.commit] writes just the
 * accepted ones. Once written, an accepted suggestion is an ordinary
 * `PlannedSessionEntity`: editable and deletable from the Planner exactly
 * like a plan the person typed in by hand, satisfying the "users must be
 * able to edit the generated plan" requirement without this screen needing
 * its own separate editor.
 */
@Composable
fun SmartPlanScreen(onDone: () -> Unit, viewModel: SmartPlanViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title = "Suggest a schedule") }

        when (val s = state) {
            is SmartPlanUiState.Setup -> setupContent(s, onGenerate = viewModel::generate)
            SmartPlanUiState.Generating -> item { BusyRow("Working out a schedule that fits…") }
            is SmartPlanUiState.Preview -> previewContent(
                state = s,
                onToggle = viewModel::toggleAccepted,
                onBack = { viewModel.backToSetup(emptySet(), DEFAULT_DAYS_AHEAD) },
                onCommit = viewModel::commit
            )
            is SmartPlanUiState.NothingToSuggest -> item {
                NothingToSuggestCard(s.reason, onBack = { viewModel.backToSetup(emptySet(), DEFAULT_DAYS_AHEAD) })
            }
            SmartPlanUiState.Committing -> item { BusyRow("Adding to your planner…") }
            is SmartPlanUiState.Done -> item { DoneCard(s.addedCount, onDone = onDone) }
        }
    }
}

private fun LazyListScope.setupContent(
    state: SmartPlanUiState.Setup,
    onGenerate: (Set<String>, Int) -> Unit
) {
    item {
        Text(
            text = "This looks at your open tasks — their deadlines, priority, and estimated time — and suggests " +
                "sessions that fit the times below. It's a starting point, not a perfect plan: review the " +
                "suggestions and add only the ones you want.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
    item {
        var selectedPresets by remember { mutableStateOf(state.selectedPresets) }
        var daysAhead by remember { mutableStateOf(state.daysAhead) }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "When are you generally free?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVAILABILITY_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = preset.label in selectedPresets,
                            onClick = {
                                selectedPresets = if (preset.label in selectedPresets) {
                                    selectedPresets - preset.label
                                } else {
                                    selectedPresets + preset.label
                                }
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }

                Text(text = "How many days ahead?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14).forEach { days ->
                        FilterChip(
                            selected = daysAhead == days,
                            onClick = { daysAhead = days },
                            label = { Text("$days days") }
                        )
                    }
                }

                PrimaryButton(text = "Generate suggestions", onClick = { onGenerate(selectedPresets, daysAhead) })
            }
        }
    }
}

private fun LazyListScope.previewContent(
    state: SmartPlanUiState.Preview,
    onToggle: (SuggestedSession) -> Unit,
    onBack: () -> Unit,
    onCommit: () -> Unit
) {
    item {
        val acceptedCount = state.items.count { it.accepted }
        Text(
            text = "$acceptedCount of ${state.items.size} suggested — uncheck any you don't want before adding them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
    items(state.items, key = { it.session }) { item ->
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.accepted, onCheckedChange = { onToggle(item.session) })
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(text = item.taskTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    val subjectPrefix = item.subjectName?.let { "$it • " } ?: ""
                    Text(
                        text = "$subjectPrefix${formatPlanDate(item.session.dateEpochDay)}, " +
                            formatPlannedTimeRange(item.session.startMinuteOfDay, item.session.durationMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    if (state.unscheduled.isNotEmpty()) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Didn't fully fit:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    state.unscheduled.forEach { remainder ->
                        Text(
                            text = "${remainder.title} — ${formatPlannedDurationShort(remainder.unscheduledMinutes)} left over",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    item {
        SecondaryButton(text = "Start over", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
    item {
        val acceptedCount = state.items.count { it.accepted }
        PrimaryButton(
            text = if (acceptedCount > 0) "Add $acceptedCount to my planner" else "Add to my planner",
            enabled = acceptedCount > 0,
            onClick = onCommit
        )
    }
}

private fun formatPlannedDurationShort(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

@Composable
private fun NothingToSuggestCard(reason: String, onBack: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Nothing to suggest yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            SecondaryButton(text = "Back", onClick = onBack)
        }
    }
}

@Composable
private fun DoneCard(addedCount: Int, onDone: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (addedCount > 0) "Added $addedCount session${if (addedCount == 1) "" else "s"} to your planner" else "Nothing was added",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You can edit or remove any of them from the Planner, exactly like a plan you'd add yourself.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            PrimaryButton(text = "Back to Planner", onClick = onDone)
        }
    }
}

@Composable
private fun BusyRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
