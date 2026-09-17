package com.studyspace.timer.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.timer.StopwatchTimerViewModel
import com.studyspace.timer.timer.TimerRunState
import com.studyspace.timer.timer.formatTimerDuration
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.theme.GalaxyMutedLavender
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyStarWhite

/**
 * Focus Mode screen: a distraction-free open-ended session, backed by the
 * same [StopwatchTimerViewModel] used for Self-Study/Online Study, keyed
 * separately so it tracks its own elapsed time independently of them.
 *
 * This project deliberately never implements actual distraction blocking
 * via Accessibility Services or cross-app automation (see project rules) —
 * this screen is purely an honest, quiet timer, not an enforcement tool.
 */
@Composable
fun FocusScreen(modifier: Modifier = Modifier) {
    val viewModel: StopwatchTimerViewModel = viewModel(key = "focus_timer")
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Focus Mode",
            style = MaterialTheme.typography.headlineMedium,
            color = GalaxyMutedLavender
        )

        GlassCard(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CenterFocusStrong,
                    contentDescription = null,
                    tint = GalaxyNeonCyan,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = formatTimerDuration(state.elapsedMillis),
                    style = MaterialTheme.typography.headlineLarge,
                    color = GalaxyStarWhite
                )
                Text(
                    text = when (state.runState) {
                        TimerRunState.IDLE -> "Ready for a distraction-free session"
                        TimerRunState.RUNNING -> "Focusing…"
                        TimerRunState.PAUSED -> "Paused"
                        TimerRunState.COMPLETED -> "Session ended"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalaxyStarWhite.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(20.dp))

                when (state.runState) {
                    TimerRunState.IDLE, TimerRunState.COMPLETED -> PrimaryButton(
                        text = "Enter Focus Mode",
                        onClick = { viewModel.start("Focus Mode", SessionType.FOCUS_MODE) }
                    )
                    TimerRunState.RUNNING -> PrimaryButton(
                        text = "Pause",
                        onClick = { viewModel.pause() }
                    )
                    TimerRunState.PAUSED -> PrimaryButton(
                        text = "Resume",
                        onClick = { viewModel.resume() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    text = "End session",
                    onClick = { viewModel.reset() },
                    enabled = state.runState != TimerRunState.IDLE
                )
            }
        }
    }
}
