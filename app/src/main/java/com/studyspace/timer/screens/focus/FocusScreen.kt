package com.studyspace.timer.screens.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.timer.FocusDurationInput
import com.studyspace.timer.timer.FocusModeViewModel
import com.studyspace.timer.timer.FocusStage
import com.studyspace.timer.timer.TimerRunState
import com.studyspace.timer.timer.TimerUiState
import com.studyspace.timer.timer.formatTimerDuration
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.GlassCardAccent
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.ProgressRing
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.SessionAttributionPicker
import com.studyspace.timer.ui.components.filteredForSubject
import com.studyspace.timer.ui.util.centeredContentWidth

/**
 * Strict Focus Mode screen. Three UI states driven by [FocusModeViewModel.stage]:
 *
 * - [FocusStage.SETUP] — duration picker (preset chips + a custom
 *   minutes/seconds stepper), validated by
 *   [com.studyspace.timer.timer.FocusDuration] before a confirmation
 *   dialog shows the exact selected time and locks in the start.
 * - [FocusStage.ACTIVE] — large countdown + progress ring, a locked-state
 *   badge, Pause/Resume, and the one deliberate way out
 *   ([FocusModeViewModel.emergencyExit]) behind its own confirmation
 *   dialog. The system back gesture is intercepted here via [BackHandler]
 *   and redirected into that same confirmation instead of silently
 *   leaving the screen. [com.studyspace.timer.timer.FocusLockController]
 *   is what [com.studyspace.timer.navigation.StudySpaceNavHost] and
 *   [com.studyspace.timer.ui.components.StudySpaceBottomNav] read to keep
 *   the bottom nav from doing the same over on that side.
 * - [FocusStage.COMPLETED] — a clear "Focus Session Completed" screen
 *   before returning to setup for another round.
 *
 * This does not, and per this project's locked-in decisions never will,
 * block other apps or system functions — see
 * [com.studyspace.timer.timer.FocusLockController]'s doc comment. The lock
 * only ever applies to navigation inside this app, and
 * [FocusModeViewModel.emergencyExit] always remains reachable.
 */
@Composable
fun FocusScreen(modifier: Modifier = Modifier) {
    val viewModel: FocusModeViewModel = viewModel()
    val stage by viewModel.stage.collectAsState()
    val state by viewModel.state.collectAsState()
    val durationInput by viewModel.durationInput.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val customLabel by viewModel.customLabel.collectAsState()

    var showStartConfirm by remember { mutableStateOf(false) }
    var showEmergencyExitConfirm by remember { mutableStateOf(false) }

    // Redirect the system back gesture into the same confirmation the
    // in-app "Emergency Exit" button uses, rather than letting it silently
    // pop back to whatever screen was underneath -- see the class doc.
    BackHandler(enabled = stage == FocusStage.ACTIVE) {
        showEmergencyExitConfirm = true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val contentWidth = centeredContentWidth(maxWidth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .width(contentWidth)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Focus Mode",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )

                when (stage) {
                    FocusStage.SETUP -> SetupContent(
                        durationInput = durationInput,
                        validationError = validationError,
                        presetMinutes = FocusModeViewModel.PRESET_MINUTES,
                        onSelectPreset = viewModel::selectPresetMinutes,
                        onCustomChange = viewModel::updateCustomDuration,
                        subjects = subjects,
                        tasks = tasks.filteredForSubject(selectedSubjectId),
                        selectedSubjectId = selectedSubjectId,
                        selectedTaskId = selectedTaskId,
                        customLabel = customLabel,
                        onSubjectSelected = viewModel::selectSubject,
                        onTaskSelected = viewModel::selectTask,
                        onLabelChange = viewModel::setCustomLabel,
                        onStartRequested = { showStartConfirm = true }
                    )
                    FocusStage.ACTIVE -> ActiveContent(
                        state = state,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onEmergencyExitRequested = { showEmergencyExitConfirm = true }
                    )
                    FocusStage.COMPLETED -> CompletedContent(
                        onDone = viewModel::acknowledgeCompletion
                    )
                }
            }
        }
    }

    if (showStartConfirm) {
        val totalMillis = durationInput.minutes * 60_000L + durationInput.seconds * 1_000L
        AlertDialog(
            onDismissRequest = { showStartConfirm = false },
            title = { Text("Start Focus Mode?") },
            text = {
                Text(
                    "You're about to start a ${formatTimerDuration(totalMillis)} focus " +
                        "session. Navigation will lock to this screen until it finishes -- " +
                        "you can still leave early with the Emergency Exit option if you need to."
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = "Start Focus",
                    fillWidth = false,
                    onClick = {
                        showStartConfirm = false
                        viewModel.confirmAndStart()
                    }
                )
            },
            dismissButton = {
                SecondaryButton(text = "Cancel", fillWidth = false, onClick = { showStartConfirm = false })
            }
        )
    }

    if (showEmergencyExitConfirm) {
        AlertDialog(
            onDismissRequest = { showEmergencyExitConfirm = false },
            title = { Text("Exit Focus Session?") },
            text = {
                Text(
                    "This ends your focus session early. Your progress so far will still be " +
                        "saved to your study history, but this session won't count as completed."
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = "Emergency Exit",
                    fillWidth = false,
                    onClick = {
                        showEmergencyExitConfirm = false
                        viewModel.emergencyExit()
                    }
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = "Keep Focusing",
                    fillWidth = false,
                    onClick = { showEmergencyExitConfirm = false }
                )
            }
        )
    }
}

@Composable
private fun SetupContent(
    durationInput: FocusDurationInput,
    validationError: String?,
    presetMinutes: List<Int>,
    onSelectPreset: (Int) -> Unit,
    onCustomChange: (Int, Int) -> Unit,
    subjects: List<SubjectEntity>,
    tasks: List<TaskEntity>,
    selectedSubjectId: Long?,
    selectedTaskId: Long?,
    customLabel: String,
    onSubjectSelected: (Long?) -> Unit,
    onTaskSelected: (Long?) -> Unit,
    onLabelChange: (String) -> Unit,
    onStartRequested: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Choose your focus duration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Once started, navigation locks to this screen until time's up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presetMinutes.forEach { minutes ->
                    val selected = durationInput.minutes == minutes && durationInput.seconds == 0
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(minutes) },
                        label = { Text("${minutes}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Custom duration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DurationStepper(
                    label = "min",
                    value = durationInput.minutes,
                    onDecrement = {
                        onCustomChange((durationInput.minutes - 1).coerceAtLeast(0), durationInput.seconds)
                    },
                    onIncrement = {
                        onCustomChange((durationInput.minutes + 1).coerceAtMost(180), durationInput.seconds)
                    }
                )
                DurationStepper(
                    label = "sec",
                    value = durationInput.seconds,
                    onDecrement = {
                        onCustomChange(durationInput.minutes, (durationInput.seconds - 5).coerceAtLeast(0))
                    },
                    onIncrement = {
                        onCustomChange(durationInput.minutes, (durationInput.seconds + 5).coerceAtMost(55))
                    }
                )
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SessionAttributionPicker(
                subjects = subjects,
                tasks = tasks,
                selectedSubjectId = selectedSubjectId,
                selectedTaskId = selectedTaskId,
                customLabel = customLabel,
                onSubjectSelected = onSubjectSelected,
                onTaskSelected = onTaskSelected,
                onLabelChange = onLabelChange
            )

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = "Start Focus", onClick = onStartRequested)
        }
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease $label")
            }
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onIncrement) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase $label")
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ActiveContent(
    state: TimerUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEmergencyExitRequested: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LockedBadge()

        GlassCardAccent(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ProgressRing(
                    progress = state.progress,
                    size = 180,
                    strokeWidth = 12,
                    centerLabel = formatTimerDuration(state.remainingMillis),
                    animate = false
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (state.runState) {
                        TimerRunState.PAUSED -> "Paused"
                        else -> "Focusing…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (state.isRunning) {
                    PrimaryButton(text = "Pause", onClick = onPause)
                } else {
                    PrimaryButton(text = "Resume", onClick = onResume)
                }

                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(text = "Emergency Exit", onClick = onEmergencyExitRequested)
            }
        }
    }
}

@Composable
private fun LockedBadge() {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = "Focus Locked",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun CompletedContent(onDone: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .height(48.dp)
            )
            Text(
                text = "Focus Session Completed",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Great work. That session has been saved to your study history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = "Done", onClick = onDone)
        }
    }
}
