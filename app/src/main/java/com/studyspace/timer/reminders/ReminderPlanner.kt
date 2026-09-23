package com.studyspace.timer.reminders

import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.settings.ReminderSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Phase 12 — pure scheduling logic for reminders. No Android dependency, so
 * every rule below is covered by a fast local JUnit test
 * (`ReminderPlannerTest`), same shape as [com.studyspace.timer.data.repository.computeDailySummary]
 * and friends. The Android side ([ReminderScheduler], [ReminderReceiver])
 * only translates these results into an `AlarmManager` alarm and a posted
 * notification.
 *
 * **Scheduling model: one alarm, always the *next* reminder.** Rather than
 * registering an alarm per plan (unbounded, and every plan edit would have
 * to find and cancel its own), the app keeps a single pending alarm aimed at
 * whichever reminder is soonest. When it fires, the receiver posts whatever
 * is due and then reschedules the next one; any change to a plan or a
 * setting reschedules too (see [ReminderScheduler.startObserving]). That
 * keeps the alarm state trivially consistent with the database.
 */

/** Minutes before a planned session's start that the "upcoming" heads-up fires. */
const val UPCOMING_LEAD_MINUTES = 10

enum class ReminderKind {
    /** "Starting in N minutes" — [UPCOMING_LEAD_MINUTES] before a planned session. */
    UPCOMING_SESSION,

    /** "Study session starting" — at a planned session's start time. */
    SESSION_START,

    /** The once-a-day reminder. */
    DAILY
}

/**
 * @param triggerAtEpochMillis the wall-clock instant this reminder is *meant*
 *   to fire. Inexact alarms can be delivered later than this, so the receiver
 *   matches due reminders against this scheduled instant, never against the
 *   time it happened to be delivered.
 * @param planId the [PlannedSessionEntity] this is about; `null` for [ReminderKind.DAILY].
 */
data class ScheduledReminder(
    val kind: ReminderKind,
    val triggerAtEpochMillis: Long,
    val planId: Long?
)

/**
 * The instant a plan starts, in [zone]. Built from a local date + wall-clock
 * time (not "midnight plus N minutes"), so a plan at 9:00 stays at 9:00 on a
 * daylight-saving-change day instead of drifting by an hour.
 */
fun plannedSessionStartMillis(plan: PlannedSessionEntity, zone: ZoneId): Long =
    LocalDate.ofEpochDay(plan.dateEpochDay)
        .atTime(LocalTime.of(plan.startMinuteOfDay / 60, plan.startMinuteOfDay % 60))
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

/** The instant the daily reminder falls on [date] in [zone]. */
fun dailyReminderMillis(date: LocalDate, minuteOfDay: Int, zone: ZoneId): Long =
    date.atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)).atZone(zone).toInstant().toEpochMilli()

/** The next daily-reminder instant strictly after [nowEpochMillis]: today's if still ahead, otherwise tomorrow's. */
fun nextDailyReminderMillis(nowEpochMillis: Long, minuteOfDay: Int, zone: ZoneId): Long {
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
    val todays = dailyReminderMillis(today, minuteOfDay, zone)
    return if (todays > nowEpochMillis) todays else dailyReminderMillis(today.plusDays(1), minuteOfDay, zone)
}

/**
 * The single soonest reminder strictly after [nowEpochMillis], or `null` if
 * nothing is enabled or nothing is upcoming.
 *
 * Only [PlannedSessionStatus.PLANNED] plans produce reminders — a completed,
 * missed or cancelled plan never does. A reminder whose time has already
 * passed is dropped, never sent late: e.g. if a session was created 3
 * minutes before it starts, the 10-minute heads-up is simply skipped and the
 * start-time reminder still fires.
 */
fun computeNextReminder(
    nowEpochMillis: Long,
    settings: ReminderSettings,
    plans: List<PlannedSessionEntity>,
    zone: ZoneId = ZoneId.systemDefault()
): ScheduledReminder? {
    val candidates = mutableListOf<ScheduledReminder>()

    plans.filter { it.status == PlannedSessionStatus.PLANNED.name }.forEach { plan ->
        val start = plannedSessionStartMillis(plan, zone)
        if (settings.plannedSessionReminders) {
            candidates += ScheduledReminder(ReminderKind.SESSION_START, start, plan.id)
        }
        if (settings.upcomingSessionReminders) {
            candidates += ScheduledReminder(ReminderKind.UPCOMING_SESSION, start - UPCOMING_LEAD_MINUTES * 60_000L, plan.id)
        }
    }
    if (settings.dailyReminder) {
        candidates += ScheduledReminder(
            ReminderKind.DAILY,
            nextDailyReminderMillis(nowEpochMillis, settings.dailyReminderMinuteOfDay, zone),
            planId = null
        )
    }

    return candidates
        .filter { it.triggerAtEpochMillis > nowEpochMillis }
        .minWithOrNull(
            compareBy<ScheduledReminder>(
                { it.triggerAtEpochMillis },
                { it.kind.ordinal },
                { it.planId ?: -1L }
            )
        )
}

/**
 * Every reminder that was scheduled for exactly [triggerAtEpochMillis]. The
 * alarm carries its scheduled instant, and this recovers *all* reminders
 * sharing it — two plans that start at the same time, or a plan that starts
 * at the daily-reminder time — so none is lost when the single alarm fires
 * once for the group. Reads the *current* settings and plans, so a reminder
 * whose toggle was switched off, or whose plan was since cancelled or
 * completed, quietly produces nothing.
 */
fun remindersDueAt(
    triggerAtEpochMillis: Long,
    settings: ReminderSettings,
    plans: List<PlannedSessionEntity>,
    zone: ZoneId = ZoneId.systemDefault()
): List<ScheduledReminder> {
    val due = mutableListOf<ScheduledReminder>()

    plans.filter { it.status == PlannedSessionStatus.PLANNED.name }.forEach { plan ->
        val start = plannedSessionStartMillis(plan, zone)
        if (settings.plannedSessionReminders && start == triggerAtEpochMillis) {
            due += ScheduledReminder(ReminderKind.SESSION_START, start, plan.id)
        }
        if (settings.upcomingSessionReminders && start - UPCOMING_LEAD_MINUTES * 60_000L == triggerAtEpochMillis) {
            due += ScheduledReminder(ReminderKind.UPCOMING_SESSION, triggerAtEpochMillis, plan.id)
        }
    }
    if (settings.dailyReminder) {
        val date = Instant.ofEpochMilli(triggerAtEpochMillis).atZone(zone).toLocalDate()
        if (dailyReminderMillis(date, settings.dailyReminderMinuteOfDay, zone) == triggerAtEpochMillis) {
            due += ScheduledReminder(ReminderKind.DAILY, triggerAtEpochMillis, planId = null)
        }
    }
    return due
}

/**
 * Whole minutes from [nowEpochMillis] until [startEpochMillis], rounded *up*
 * (so "9 minutes 20 seconds away" reads "10", not "9"); zero or negative
 * once the session has started. An inexact alarm can be delivered late, so
 * the heads-up states the real remaining time, and the receiver skips it
 * entirely once that reaches zero.
 */
fun minutesUntil(startEpochMillis: Long, nowEpochMillis: Long): Int =
    (Math.floorDiv(startEpochMillis - nowEpochMillis + 59_999L, 60_000L)).toInt()

fun upcomingReminderTitle(minutesUntilStart: Int): String =
    if (minutesUntilStart == 1) "Starting in 1 minute" else "Starting in $minutesUntilStart minutes"

/** "📐 Mathematics · Calculus", or "Planned study session" when the plan has neither a subject nor a task. */
fun describePlannedSession(subject: SubjectEntity?, task: TaskEntity?): String {
    val parts = listOfNotNull(subject?.let { "${it.icon} ${it.name}" }, task?.title)
    return if (parts.isEmpty()) "Planned study session" else parts.joinToString(" · ")
}

/**
 * Whether a goal-reached notification should be posted now.
 *
 * It fires only on the *crossing* — the session just recorded moved the
 * total from below [targetMillis] to at-or-above it — so further sessions
 * after the goal is already met stay silent. On top of that, at most one per
 * [minDaysBetween] days (1 for the daily goal, 7 for the weekly goal, whose
 * window rolls and could otherwise cross again on consecutive days), tracked
 * via [lastNotifiedEpochDay].
 */
fun shouldNotifyGoalReached(
    beforeMillis: Long,
    afterMillis: Long,
    targetMillis: Long,
    lastNotifiedEpochDay: Long?,
    todayEpochDay: Long,
    minDaysBetween: Int
): Boolean {
    if (targetMillis <= 0L) return false
    val crossed = beforeMillis < targetMillis && afterMillis >= targetMillis
    if (!crossed) return false
    val last = lastNotifiedEpochDay ?: return true
    return todayEpochDay - last >= minDaysBetween
}
