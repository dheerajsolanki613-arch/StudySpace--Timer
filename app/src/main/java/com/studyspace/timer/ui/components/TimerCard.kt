package com.studyspace.timer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyStarWhite

/**
 * Visual shell for a timer screen: large time readout + label + row of action
 * buttons. Stage 2 only renders a static/placeholder time; Stage 3 replaces
 * [timeText] and the button callbacks with real timer engine state.
 */
@Composable
fun TimerCard(
    label: String,
    timeText: String,
    modifier: Modifier = Modifier,
    statusText: String = "Ready to start",
    actions: @Composable () -> Unit = {}
) {
    GlassCardAccent(
        modifier = modifier.fillMaxWidth(),
        accentColor = GalaxyNeonCyan
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = GalaxyStarWhite.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineLarge,
                color = GalaxyStarWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = GalaxyStarWhite.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                actions()
            }
        }
    }
}
