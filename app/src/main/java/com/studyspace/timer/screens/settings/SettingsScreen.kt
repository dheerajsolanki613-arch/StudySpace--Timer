package com.studyspace.timer.screens.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.repository.formatMinuteOfDay
import com.studyspace.timer.reminders.UPCOMING_LEAD_MINUTES
import com.studyspace.timer.settings.ReminderSettings
import com.studyspace.timer.settings.SettingsRepository
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.util.isLandscape

/**
 * Settings screen, Stage 8: every row below reads from and writes to
 * [SettingsViewModel] (DataStore-backed via [SettingsRepository]) — none of
 * it is a disabled placeholder. What each row actually does:
 *  - "Timer completion alerts" gates a one-shot **notification** posted
 *    whenever a Normal Timer countdown or any Pomodoro phase (work or
 *    break) finishes — separate from the always-silent ongoing progress
 *    notification from Stage 4. See
 *    [com.studyspace.timer.service.TimerNotifications.notifyCompletion].
 *  - "Sound"/"Vibration" (Phase 15) are independent of that notification:
 *    they trigger a short in-app tone/vibration directly, so they still
 *    fire even with system notifications off. See
 *    [com.studyspace.timer.service.CompletionFeedback].
 *  - "Keep screen on during sessions" applies `View.keepScreenOn` for as
 *    long as a timer is actively running — see
 *    [com.studyspace.timer.MainActivity].
 *  - "Reduce motion" is read via [com.studyspace.timer.ui.theme.LocalReduceMotion]
 *    by the one place this app animates ([com.studyspace.timer.ui.components.ProgressRing]).
 *  - Daily goal replaces `HomeScreen.kt`'s old fixed 4h constant; picked
 *    from a preset list the same way Pomodoro/Normal Timer durations are.
 *  - Phase 12 adds the reminder toggles under "Notifications": planned-session
 *    reminders, an upcoming-session heads-up, a daily reminder (with a time
 *    picker), and goal-reached notices. All default off and are scheduled by
 *    [com.studyspace.timer.reminders.ReminderScheduler]. Turning one on asks
 *    for the notification permission right there on Android 13+ (the moment
 *    the user has just said they want notifications), and if notifications
 *    are blocked at the system level a card explains that and links to the
 *    system notification settings, re-checked whenever the screen resumes.
 *
 * Theme/rotation update: text and switch colors read `MaterialTheme.colorScheme`,
 * and the list is width-constrained and centered in landscape rather than
 * stretching each row's title/subtitle across the full screen width.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenDataManagement: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val completionAlerts by viewModel.timerCompletionAlertsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val keepScreenOn by viewModel.keepScreenOnEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val reminders by viewModel.reminderSettings.collectAsState()
    val landscape = isLandscape()

    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    // Re-check on every resume: the user may have just changed it in system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // The choice is always saved; the permission is only *asked for* when something was just turned on and
    // notifications aren't currently allowed. If the user declines, the card below says why nothing will show.
    val onReminderToggle: (Boolean, (Boolean) -> Unit) -> Unit = { enabled, save ->
        save(enabled)
        if (enabled && !notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        item { SectionHeader(title = "Notifications", modifier = rowWidth(landscape)) }
        if (!notificationsAllowed) {
            item {
                NotificationsBlockedCard(
                    onOpenSettings = { openSystemNotificationSettings(context) },
                    modifier = rowWidth(landscape)
                )
            }
        }
        item {
            SettingsSwitchRow(
                title = "Timer completion alerts",
                subtitle = "Show a notification when a session or Pomodoro phase ends",
                checked = completionAlerts,
                onCheckedChange = viewModel::setTimerCompletionAlertsEnabled,
                modifier = rowWidth(landscape)
            )
        }
        item {
            SettingsSwitchRow(
                title = "Sound",
                subtitle = "Play a short in-app tone when a session or Pomodoro phase ends",
                checked = soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled,
                modifier = rowWidth(landscape)
            )
        }
        item {
            SettingsSwitchRow(
                title = "Vibration",
                subtitle = "Vibrate when a session or Pomodoro phase ends",
                checked = vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
                modifier = rowWidth(landscape)
            )
        }
        item {
            SettingsSwitchRow(
                title = "Planned session reminders",
                subtitle = "Notify when a planned study session is due to start",
                checked = reminders.plannedSessionReminders,
                onCheckedChange = { onReminderToggle(it, viewModel::setPlannedSessionReminders) },
                modifier = rowWidth(landscape)
            )
        }
        item {
            SettingsSwitchRow(
                title = "Upcoming session heads-up",
                subtitle = "Notify ${UPCOMING_LEAD_MINUTES} minutes before a planned session starts",
                checked = reminders.upcomingSessionReminders,
                onCheckedChange = { onReminderToggle(it, viewModel::setUpcomingSessionReminders) },
                modifier = rowWidth(landscape)
            )
        }
        item {
            DailyReminderCard(
                enabled = reminders.dailyReminder,
                selectedMinuteOfDay = reminders.dailyReminderMinuteOfDay,
                onEnabledChange = { onReminderToggle(it, viewModel::setDailyReminder) },
                onSelectTime = viewModel::setDailyReminderMinuteOfDay,
                modifier = rowWidth(landscape)
            )
        }
        item {
            SettingsSwitchRow(
                title = "Goal reached",
                subtitle = "A quiet notice when you reach your daily or weekly goal — once, and only when reached",
                checked = reminders.goalNotifications,
                onCheckedChange = { onReminderToggle(it, viewModel::setGoalNotifications) },
                modifier = rowWidth(landscape)
            )
        }
        item {
            Text(
                text = "Reminders use battery-friendly timing, so they can arrive a few minutes late — occasionally longer when the phone is in battery saver.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = rowWidth(landscape)
            )
        }

        item { SectionHeader(title = "Timer", modifier = rowWidth(landscape)) }
        item {
            SettingsSwitchRow(
                title = "Keep screen on during sessions",
                subtitle = "Prevents the display from sleeping while a timer is running",
                checked = keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOnEnabled,
                modifier = rowWidth(landscape)
            )
        }
        item {
            DailyGoalCard(
                selectedMinutes = dailyGoalMinutes,
                onSelect = viewModel::setDailyGoalMinutes,
                modifier = rowWidth(landscape)
            )
        }

        item { SectionHeader(title = "Accessibility", modifier = rowWidth(landscape)) }
        item {
            SettingsSwitchRow(
                title = "Reduce motion",
                subtitle = "Skip the easing animation on progress rings; values update instantly",
                checked = reduceMotion,
                onCheckedChange = viewModel::setReduceMotionEnabled,
                modifier = rowWidth(landscape)
            )
        }

        item { SectionHeader(title = "Data", modifier = rowWidth(landscape)) }
        item {
            GlassCard(modifier = rowWidth(landscape)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Export your study history as CSV or JSON, import an export, or back up and restore everything.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    SecondaryButton(text = "Export & import data", onClick = onOpenDataManagement)
                }
            }
        }

        item {
            GlassCard(modifier = rowWidth(landscape)) {
                Text(
                    text = "Wallpaper and color palette live on the Themes tab. Every setting above is saved automatically and applies immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun rowWidth(landscape: Boolean): Modifier =
    if (landscape) Modifier.widthIn(max = 560.dp).fillMaxWidth() else Modifier.fillMaxWidth()

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { contentDescription = title },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun DailyGoalCard(selectedMinutes: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = "Daily study goal",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Used for the progress ring on Home",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
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

@Composable
private fun NotificationsBlockedCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = "Notifications are turned off",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Alerts and reminders won't appear until notifications are allowed for StudySpace in your phone's settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            TextButton(onClick = onOpenSettings) {
                Text(text = "Open notification settings", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

/**
 * The daily-reminder switch plus, only while it's on, a row of time presets
 * (7 AM–9 PM; nothing later, by design — see [ReminderSettings.DAILY_TIME_PRESET_MINUTES]).
 * The chip row scrolls horizontally so eight presets never overflow a narrow
 * screen or a large font size.
 */
@Composable
private fun DailyReminderCard(
    enabled: Boolean,
    selectedMinuteOfDay: Int,
    onEnabledChange: (Boolean) -> Unit,
    onSelectTime: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = "Daily study reminder"
    GlassCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "One reminder a day at a time you choose — skipped on days you've already studied",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics { contentDescription = title },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
                )
            }
            if (enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderSettings.DAILY_TIME_PRESET_MINUTES.forEach { minuteOfDay ->
                        FilterChip(
                            selected = selectedMinuteOfDay == minuteOfDay,
                            onClick = { onSelectTime(minuteOfDay) },
                            label = { Text(formatMinuteOfDay(minuteOfDay)) }
                        )
                    }
                }
            }
        }
    }
}

/** Opens this app's page in the system notification settings; falls back to the general app-details page if that's unavailable. */
private fun openSystemNotificationSettings(context: android.content.Context) {
    val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(notificationSettings)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:${context.packageName}"))
            )
        } catch (_: ActivityNotFoundException) {
            // No settings screen to open on this device; the card's text still tells the user what to do.
        }
    }
}

private fun goalLabel(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}m"
}
