package com.studyspace.timer.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.timer.CountdownTimerViewModel
import com.studyspace.timer.timer.StopwatchTimerViewModel
import com.studyspace.timer.timer.TimerRunState
import com.studyspace.timer.timer.formatTimerDuration
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.TimerCard
import com.studyspace.timer.ui.util.isLandscape

private val timerModes = listOf("Self-Study", "Online Study", "Normal")

/**
 * Timer screen: Self-Study and Online Study are open-ended stopwatches;
 * Normal is a countdown from a user-selected preset. Each tab keeps its own
 * ViewModel instance (via distinct `viewModel(key = ...)` calls), obtained
 * unconditionally every recomposition regardless of [selectedTab], so a
 * timer keeps counting even while a different tab is showing and switching
 * tabs never resets or shares another mode's progress. That's also what
 * keeps the running timer intact across a rotation: the ViewModel isn't
 * scoped to this composable's position on screen, so relaying out for
 * landscape doesn't touch it.
 *
 * In landscape, the tab content is centered with a bounded max width
 * instead of stretching the card and buttons edge-to-edge on a wide screen.
 */
@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val landscape = isLandscape()

    val selfStudyViewModel: StopwatchTimerViewModel = viewModel(key = "self_study_timer")
    val onlineStudyViewModel: StopwatchTimerViewModel = viewModel(key = "online_study_timer")
    val normalViewModel: CountdownTimerViewModel = viewModel(key = "normal_timer")

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Timer",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = if (landscape) Modifier.widthIn(max = 480.dp) else Modifier.fillMaxWidth()
        ) {
            timerModes.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) }
                )
            }
        }

        Column(
            modifier = (if (landscape) Modifier.widthIn(max = 480.dp) else Modifier.fillMaxWidth())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> StopwatchTimerContent(
                    label = "Self-Study Timer",
                    type = SessionType.SELF_STUDY,
                    viewModel = selfStudyViewModel
                )
                1 -> StopwatchTimerContent(
                    label = "Online Study Timer",
                    type = SessionType.ONLINE_STUDY,
                    viewModel = onlineStudyViewModel
                )
                else -> CountdownTimerContent(viewModel = normalViewModel)
            }
        }
    }
}

@Composable
private fun StopwatchTimerContent(
    label: String,
    type: SessionType,
    viewModel: StopwatchTimerViewModel
) {
    val state by viewModel.state.collectAsState()

    TimerCard(
        label = label,
        timeText = formatTimerDuration(state.elapsedMillis),
        statusText = when (state.runState) {
            TimerRunState.IDLE -> "Ready to start"
            TimerRunState.RUNNING -> "Studying…"
            TimerRunState.PAUSED -> "Paused"
            TimerRunState.COMPLETED -> "Completed"
        },
        actions = {
            when (state.runState) {
                TimerRunState.IDLE, TimerRunState.COMPLETED -> PrimaryButton(
                    text = "Start",
                    onClick = { viewModel.start(label, type) },
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
        }
    )

    SecondaryButton(
        text = "Stop & Reset",
        onClick = { viewModel.reset() },
        icon = Icons.Filled.Refresh,
        enabled = state.runState != TimerRunState.IDLE
    )
}

@Composable
private fun CountdownTimerContent(viewModel: CountdownTimerViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedDuration by viewModel.selectedDurationMillis.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.isIdle) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CountdownTimerViewModel.PRESET_MINUTES) { minutes ->
                    val millis = minutes * 60_000L
                    FilterChip(
                        selected = selectedDuration == millis,
                        onClick = { viewModel.selectDuration(millis) },
                        label = { Text("${minutes}m") }
                    )
                }
            }
        }

        TimerCard(
            label = "Normal Timer",
            timeText = formatTimerDuration(
                if (state.isIdle) selectedDuration else state.remainingMillis
            ),
            statusText = when (state.runState) {
                TimerRunState.IDLE -> "Ready to start"
                TimerRunState.RUNNING -> "Counting down…"
                TimerRunState.PAUSED -> "Paused"
                TimerRunState.COMPLETED -> "Time's up!"
            },
            actions = {
                when (state.runState) {
                    TimerRunState.IDLE -> PrimaryButton(
                        text = "Start",
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
                    TimerRunState.COMPLETED -> PrimaryButton(
                        text = "Reset",
                        onClick = { viewModel.reset() },
                        icon = Icons.Filled.Refresh
                    )
                }
            }
        )

        SecondaryButton(
            text = "Reset",
            onClick = { viewModel.reset() },
            icon = Icons.Filled.Refresh,
            enabled = state.runState != TimerRunState.IDLE
        )
    }
}
