package com.studyspace.timer.reminders

import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.timer.formatDurationHoursMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Phase 12 — the "goal progress" notification. Called by
 * [com.studyspace.timer.data.repository.SessionRepository] right after a
 * session is saved, so it fires at the moment a goal is actually reached
 * rather than on a timer. The work runs on the application scope so saving
 * a session never waits on it.
 *
 * Only ever posts a "goal *reached*" notice — no "halfway there" or
 * "almost there" nudges, which would be exactly the kind of extra,
 * pressure-adding notification the spec asks to avoid. Whether it posts is
 * decided by [shouldNotifyGoalReached]: only on the crossing itself, at most
 * once per day for the daily goal and once per 7 days for the weekly goal.
 *
 * The weekly goal uses the same rolling 7-day window Home and the Goals
 * screen already call "this week" (see `StudyStats.weekTotalMillis`), not a
 * calendar week, so all three places agree on what "reached" means.
 */
object GoalNotifier {

    fun onSessionRecorded(app: StudySpaceApplication, scope: CoroutineScope, session: StudySessionEntity) {
        scope.launch {
            try {
                check(app, session)
            } catch (_: Exception) {
                // Best-effort: a failed goal notice must never affect session saving or anything else.
            }
        }
    }

    private suspend fun check(app: StudySpaceApplication, session: StudySessionEntity) {
        val settingsRepo = app.settingsRepository
        if (!settingsRepo.reminderSettings.first().goalNotifications) return

        val todayEpochDay = LocalDate.now().toEpochDay()
        val weekSessions = app.sessionRepository.sessionsSince(todayEpochDay - 6).first()

        // Daily goal — only meaningful for a session that belongs to today (one that started before
        // midnight and ended after it is credited to the day it started, per StudySessionEntity).
        if (session.dateEpochDay == todayEpochDay) {
            val target = settingsRepo.dailyGoalMinutes.first() * 60_000L
            val after = weekSessions.filter { it.dateEpochDay == todayEpochDay }.sumOf { it.durationMillis }
            val before = after - session.durationMillis
            if (shouldNotifyGoalReached(before, after, target, settingsRepo.lastGoalNotifiedDay(weekly = false), todayEpochDay, minDaysBetween = 1)) {
                settingsRepo.setLastGoalNotifiedDay(weekly = false, epochDay = todayEpochDay)
                ReminderNotifications.notifyGoalReached(app, weekly = false, totalText = formatDurationHoursMinutes(after))
            }
        }

        // Weekly goal — rolling 7 days, and only if the user has set one.
        val weeklyTarget = (app.goalRepository.weeklyGoal().first()?.targetMinutes ?: 0) * 60_000L
        if (weeklyTarget > 0L && session.dateEpochDay >= todayEpochDay - 6) {
            val after = weekSessions.sumOf { it.durationMillis }
            val before = after - session.durationMillis
            if (shouldNotifyGoalReached(before, after, weeklyTarget, settingsRepo.lastGoalNotifiedDay(weekly = true), todayEpochDay, minDaysBetween = 7)) {
                settingsRepo.setLastGoalNotifiedDay(weekly = true, epochDay = todayEpochDay)
                ReminderNotifications.notifyGoalReached(app, weekly = true, totalText = formatDurationHoursMinutes(after))
            }
        }
    }
}
