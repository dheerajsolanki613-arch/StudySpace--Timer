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

/**
 * Visual shell for a timer screen: large time readout + label + row of action
 * buttons. Text colors read `onSurface` (not a fixed `Galaxy*` constant) so
 * they stay legible against whichever card fill the active palette gives
 * [GlassCardAccent] — espresso-brown on Kawaii Pastel's cream card, star-white
 * on every dark galaxy palette's glass card.
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
        accentColor = MaterialTheme.colorScheme.secondary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
