package com.studyspace.timer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studyspace.timer.data.repository.GoalProgress
import com.studyspace.timer.timer.formatDurationHoursMinutes

/**
 * A goal's progress as a labeled bar: "Xh Ym / target", a filled track, the
 * percentage, and time remaining — the layout the Study Goals spec asks
 * for (e.g. "3h 42m / 5h ... 74% ... 2h 18m remaining"), shared by the
 * daily and weekly cards on [com.studyspace.timer.screens.goals.GoalsScreen]
 * rather than each screen drawing its own bar.
 */
@Composable
fun GoalProgressCard(
    progress: GoalProgress,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    trailingContent: (@Composable () -> Unit)? = null
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                trailingContent?.invoke()
            }
            Text(
                text = "${formatDurationHoursMinutes(progress.currentMillis)} / ${formatDurationHoursMinutes(progress.targetMillis)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )
            ProgressTrack(fraction = progress.progressFraction, color = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.percent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = if (progress.isComplete) {
                        "Goal reached"
                    } else {
                        "${formatDurationHoursMinutes(progress.remainingMillis)} remaining"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
    }
}
