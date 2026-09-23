package com.studyspace.timer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.repository.formatPlannedTimeRange
import com.studyspace.timer.settings.ReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Phase 12 — the Android side of reminder scheduling: keeps exactly one
 * `AlarmManager` alarm pointed at the next reminder (see the model described
 * in `ReminderPlanner.kt`) and handles it when it fires.
 *
 * **Inexact on purpose.** Uses [AlarmManager.setAndAllowWhileIdle], which
 * needs no special permission. Android documents that such an alarm never
 * fires *before* its trigger time, but may fire later — on Android 12+ up to
 * an hour later when battery-saving restrictions such as Doze apply. Exact
 * alarms would need the user to grant "Alarms & reminders" special access,
 * a heavier ask than a study reminder warrants. The Settings screen says so
 * plainly, and the receiver copes with late delivery (the heads-up states
 * the real minutes remaining and is dropped once the session has started).
 *
 * The alarm is re-derived from the database and settings on every relevant
 * change ([startObserving]), at app start, after a reboot ([BootReceiver] —
 * the OS clears all alarms on reboot), and after each firing.
 */
object ReminderScheduler {
    private const val REQUEST_CODE = 4001
    const val EXTRA_TRIGGER_AT = "reminder_trigger_at"

    private fun alarmPendingIntent(context: Context, triggerAt: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_TRIGGER_AT, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Watches the reminder settings and the planned sessions and re-aims the
     * alarm on every change. Started once from
     * [com.studyspace.timer.StudySpaceApplication.onCreate], so it also runs
     * in a process the OS started just to deliver an alarm or a boot event.
     */
    fun startObserving(app: StudySpaceApplication, scope: CoroutineScope) {
        scope.launch {
            combine(
                app.settingsRepository.reminderSettings,
                app.plannedSessionRepository.sessionsSince(LocalDate.now().toEpochDay())
            ) { settings, plans -> settings to plans }
                .collectLatest { (settings, plans) -> aimAlarm(app, settings, plans) }
        }
    }

    /** One-shot re-derive from current state — used after the alarm fires and after boot. */
    suspend fun reschedule(app: StudySpaceApplication) {
        val settings = app.settingsRepository.reminderSettings.first()
        val plans = app.plannedSessionRepository.sessionsSince(LocalDate.now().toEpochDay()).first()
        aimAlarm(app, settings, plans)
    }

    private fun aimAlarm(context: Context, settings: ReminderSettings, plans: List<PlannedSessionEntity>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val next = computeNextReminder(System.currentTimeMillis(), settings, plans)
        if (next == null) {
            // Nothing enabled/upcoming: cancel whatever alarm might be pending. A PendingIntent
            // is matched on everything but extras, so any trigger value cancels the same one.
            alarmManager.cancel(alarmPendingIntent(context, 0L))
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.triggerAtEpochMillis,
                alarmPendingIntent(context, next.triggerAtEpochMillis)
            )
        }
    }

    /**
     * Posts every reminder that was due at [triggerAt], then re-aims the alarm
     * at whatever comes next. Best-effort: a failure posting one reminder
     * must never stop the next one from being scheduled.
     */
    suspend fun handleAlarm(app: StudySpaceApplication, triggerAt: Long) {
        try {
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val settings = app.settingsRepository.reminderSettings.first()
            val todayEpochDay = LocalDate.now(zone).toEpochDay()
            val plans = app.plannedSessionRepository.sessionsSince(todayEpochDay - 1).first()
            val due = remindersDueAt(triggerAt, settings, plans, zone)

            if (due.isNotEmpty()) {
                val subjectsById = app.subjectRepository.allSubjects().first().associateBy { it.id }
                val tasksById = app.taskRepository.allTasks().first().associateBy { it.id }

                due.forEach { reminder ->
                    val plan = plans.firstOrNull { it.id == reminder.planId }
                    when (reminder.kind) {
                        ReminderKind.SESSION_START -> if (plan != null && plan.status == PlannedSessionStatus.PLANNED.name) {
                            val end = plannedSessionStartMillis(plan, zone) + plan.durationMinutes * 60_000L
                            // A session whose whole window has passed (very late delivery) is not worth announcing.
                            if (now < end) {
                                ReminderNotifications.notifySessionStart(
                                    app, plan.id,
                                    describePlannedSession(plan.subjectId?.let { subjectsById[it] }, plan.taskId?.let { tasksById[it] }),
                                    formatPlannedTimeRange(plan.startMinuteOfDay, plan.durationMinutes)
                                )
                            }
                        }
                        ReminderKind.UPCOMING_SESSION -> if (plan != null && plan.status == PlannedSessionStatus.PLANNED.name) {
                            val minutes = minutesUntil(plannedSessionStartMillis(plan, zone), now)
                            // Delivered after the session already started: the start-time reminder covers it.
                            if (minutes > 0) {
                                ReminderNotifications.notifyUpcoming(
                                    app, plan.id, minutes,
                                    describePlannedSession(plan.subjectId?.let { subjectsById[it] }, plan.taskId?.let { tasksById[it] }),
                                    formatPlannedTimeRange(plan.startMinuteOfDay, plan.durationMinutes)
                                )
                            }
                        }
                        ReminderKind.DAILY -> {
                            // Skip on a day the user has already studied — the reminder has nothing left to do.
                            val studiedToday = app.sessionRepository.sessionsSince(todayEpochDay).first().isNotEmpty()
                            if (!studiedToday) ReminderNotifications.notifyDaily(app)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Reminders are best-effort; fall through to rescheduling below.
        }
        reschedule(app)
    }
}
