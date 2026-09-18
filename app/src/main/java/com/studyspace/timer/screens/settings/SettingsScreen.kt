package com.studyspace.timer.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.settings.SettingsRepository
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyMutedLavender
import com.studyspace.timer.ui.theme.GalaxyStarWhite

/**
 * Settings screen, Stage 8: every row below reads from and writes to
 * [SettingsViewModel] (DataStore-backed via [SettingsRepository]) — none of
 * it is a disabled placeholder anymore. What each row actually does:
 *  - "Timer completion alerts" gates a distinct, one-shot sound
 *    notification posted whenever a Normal Timer countdown or any Pomodoro
 *    phase (work or break) finishes — separate from the always-silent
 *    ongoing progress notification from Stage 4. See
 *    [com.studyspace.timer.service.TimerNotifications.notifyCompletion].
 *  - "Keep screen on during sessions" applies `View.keepScreenOn` for as
 *    long as a timer is actively running — see
 *    [com.studyspace.timer.MainActivity].
 *  - "Reduce motion" is read via [com.studyspace.timer.ui.theme.LocalReduceMotion]
 *    by the one place this app animates ([com.studyspace.timer.ui.components.ProgressRing]).
 *  - Daily goal replaces `HomeScreen.kt`'s old fixed 4h constant; picked
 *    from a preset list the same way Pomodoro/Normal Timer durations are.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val completionAlerts by viewModel.timerCompletionAlertsEnabled.collectAsState()
    val keepScreenOn by viewModel.keepScreenOnEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = GalaxyMutedLavender
            )
        }

        item { SectionHeader(title = "Notifications") }
        item {
            SettingsSwitchRow(
                title = "Timer completion alerts",
                subtitle = "Play a sound when a session or Pomodoro work phase ends",
                checked = completionAlerts,
                onCheckedChange = viewModel::setTimerCompletionAlertsEnabled
            )
        }

        item { SectionHeader(title = "Timer") }
        item {
            SettingsSwitchRow(
                title = "Keep screen on during sessions",
                subtitle = "Prevents the display from sleeping while a timer is running",
                checked = keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOnEnabled
            )
        }
        item {
            DailyGoalCard(
                selectedMinutes = dailyGoalMinutes,
                onSelect = viewModel::setDailyGoalMinutes
            )
        }

        item { SectionHeader(title = "Accessibility") }
        item {
            SettingsSwitchRow(
                title = "Reduce motion",
                subtitle = "Skip the easing animation on progress rings; values update instantly",
                checked = reduceMotion,
                onCheckedChange = viewModel::setReduceMotionEnabled
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wallpaper and color palette live on the Themes tab. Every setting above is saved automatically and applies immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalaxyStarWhite.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = GalaxyStarWhite)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalaxyStarWhite.copy(alpha = 0.55f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { contentDescription = title },
                colors = SwitchDefaults.colors(checkedTrackColor = GalaxyNeonCyan)
            )
        }
    }
}

@Composable
private fun DailyGoalCard(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = "Daily study goal", style = MaterialTheme.typography.bodyLarge, color = GalaxyStarWhite)
            Text(
                text = "Used for the progress ring on Home",
                style = MaterialTheme.typography.bodyMedium,
                color = GalaxyStarWhite.copy(alpha = 0.55f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsRepository.DAILY_GOAL_PRESET_MINUTES.forEach { minutes ->
                    FilterChip(
                        selected = selectedMinutes == minutes,
                        onClick = { onSelect(minutes) },
                        label = { Text(goalLabel(minutes)) }
                    )
                }
            }
        }
    }
}

private fun goalLabel(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}m"
}
