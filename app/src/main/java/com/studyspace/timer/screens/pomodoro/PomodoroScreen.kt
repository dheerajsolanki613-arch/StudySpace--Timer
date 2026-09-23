package com.studyspace.timer.screens.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.timer.PomodoroPhase
import com.studyspace.timer.timer.PomodoroViewModel
import com.studyspace.timer.timer.TimerRunState
import com.studyspace.timer.timer.formatTimerDuration
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.ProgressRing
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.SessionAttributionPicker
import com.studyspace.timer.ui.components.filteredForSubject
import com.studyspace.timer.ui.util.centeredContentWidth

/**
 * Pomodoro screen wired to [PomodoroViewModel]: real work/break cycling
 * (default 25m work / 5m short break / 15m long break, every 4th completed
 * work session), driven by the same [com.studyspace.timer.timer.TimerEngine]
 * core as every other timer mode. A duration-preset picker (shown while
 * idle, for whichever phase is up next) sits on top of the same engine and
 * cycling logic — see [PomodoroViewModel.selectDurationMinutes].
 *
 * Ring color uses `primary` for Work and `secondary` for either break,
 * pulled from the active palette instead of fixed neon pink/cyan.
 *
 * Responsive layout: same [BoxWithConstraints] + [centeredContentWidth]
 * rule as [com.studyspace.timer.screens.timer.TimerScreen] and
 * [com.studyspace.timer.screens.focus.FocusScreen] — a single centered,
 * capped-width column on wide/landscape screens, full width on portrait
 * phones, scrollable so a short landscape height reflows instead of
 * clipping the ring/buttons.
 */
@Composable
fun PomodoroScreen(modifier: Modifier = Modifier) {
    val viewModel: PomodoroViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val workMinutes by viewModel.workMinutes.collectAsState()
    val shortBreakMinutes by viewModel.shortBreakMinutes.collectAsState()
    val longBreakMinutes by viewModel.longBreakMinutes.collectAsState()
    val sessionsPerLongBreak by viewModel.sessionsPerLongBreak.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val customLabel by viewModel.customLabel.collectAsState()
    val timer = uiState.timer

    val phaseLabel = when (uiState.phase) {
        PomodoroPhase.WORK -> "Work session"
        PomodoroPhase.SHORT_BREAK -> "Short break"
        PomodoroPhase.LONG_BREAK -> "Long break"
    }
    val ringColor = if (uiState.phase == PomodoroPhase.WORK) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val selectedMinutesForCurrentPhase = when (uiState.phase) {
        PomodoroPhase.WORK -> workMinutes
        PomodoroPhase.SHORT_BREAK -> shortBreakMinutes
        PomodoroPhase.LONG_BREAK -> longBreakMinutes
    }
    val displayMillis = if (timer.isRunning || timer.isPaused) {
        timer.remainingMillis
    } else {
        selectedMinutesForCurrentPhase * 60_000L
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pomodoro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProgressRing(
                            progress = timer.progress,
                            size = 180,
                            strokeWidth = 14,
                            progressColor = ringColor,
                            centerLabel = formatTimerDuration(displayMillis),
                            centerSubLabel = phaseLabel,
                            // Already a live, ~200ms-ticking value -- see ProgressRing's
                            // doc comment for why easing would lag rather than smooth it.
                            animate = false
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Session ${uiState.completedWorkSessions + 1} • " +
                                "Work ${workMinutes}m / Break ${shortBreakMinutes}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (timer.isIdle) {
                            DurationPresetPicker(
                                presets = presetsFor(uiState.phase),
                                selectedMinutes = selectedMinutesForCurrentPhase,
                                onSelect = { minutes -> viewModel.selectDurationMinutes(uiState.phase, minutes) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SessionsPerLongBreakPicker(
                                selected = sessionsPerLongBreak,
                                onSelect = viewModel::selectSessionsPerLongBreak
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SessionAttributionPicker(
                                subjects = subjects,
                                tasks = tasks.filteredForSubject(selectedSubjectId),
                                selectedSubjectId = selectedSubjectId,
                                selectedTaskId = selectedTaskId,
                                customLabel = customLabel,
                                onSubjectSelected = viewModel::selectSubject,
                                onTaskSelected = viewModel::selectTask,
                                onLabelChange = viewModel::setCustomLabel
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        when (timer.runState) {
                            TimerRunState.IDLE, TimerRunState.COMPLETED -> PrimaryButton(
                                text = "Start $phaseLabel",
                                onClick = { viewModel.start() },
                                icon = Icons.Filled.PlayArrow
                            )
                            TimerRunState.RUNNING -> PrimaryButton(
                                text = "Pause",
                                onClick = { viewModel.pause() },
                                icon = Icons.Filled.Pause
                            )
                            TimerRunState.PAUSED -> PrimaryButton(
                                text = "Resume",
                                onClick = { viewModel.resume() },
                                icon = Icons.Filled.PlayArrow
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(
                            text = "Reset cycle",
                            onClick = { viewModel.reset() },
                            icon = Icons.Filled.Refresh
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationPresetPicker(
    presets: List<Int>,
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Duration",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onSelect(minutes) },
                    label = { Text("${minutes}m") }
                )
            }
        }
    }
}

@Composable
private fun SessionsPerLongBreakPicker(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Long break every",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PomodoroViewModel.SESSIONS_PER_LONG_BREAK_PRESETS) { count ->
                FilterChip(
                    selected = selected == count,
                    onClick = { onSelect(count) },
                    label = { Text("${count} sessions") }
                )
            }
        }
    }
}

private fun presetsFor(phase: PomodoroPhase): List<Int> = when (phase) {
    PomodoroPhase.WORK -> PomodoroViewModel.WORK_PRESET_MINUTES
    PomodoroPhase.SHORT_BREAK -> PomodoroViewModel.SHORT_BREAK_PRESET_MINUTES
    PomodoroPhase.LONG_BREAK -> PomodoroViewModel.LONG_BREAK_PRESET_MINUTES
}
