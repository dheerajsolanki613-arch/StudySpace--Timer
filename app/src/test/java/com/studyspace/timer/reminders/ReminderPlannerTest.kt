package com.studyspace.timer.reminders

import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.settings.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderPlannerTest {
    private val utc = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 9, 21)

    private val allOff = ReminderSettings()
    private val startOnly = ReminderSettings(plannedSessionReminders = true)
    private val upcomingOnly = ReminderSettings(upcomingSessionReminders = true)
    private val both = ReminderSettings(plannedSessionReminders = true, upcomingSessionReminders = true)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0) =
        date.atTime(hour, minute).atZone(utc).toInstant().toEpochMilli()

    private fun plan(
        id: Long,
        date: LocalDate = today,
        hour: Int,
        minute: Int = 0,
        status: PlannedSessionStatus = PlannedSessionStatus.PLANNED
    ) = PlannedSessionEntity(
        id = id,
        dateEpochDay = date.toEpochDay(),
        startMinuteOfDay = hour * 60 + minute,
        durationMinutes = 60,
        status = status.name,
        createdAtEpochMillis = 0L
    )

    private fun next(now: Long, settings: ReminderSettings, plans: List<PlannedSessionEntity> = emptyList()) =
        computeNextReminder(now, settings, plans, utc)

    // ---------- computeNextReminder ----------

    @Test
    fun `nothing enabled means no reminder even with upcoming plans`() {
        assertNull(next(at(today, 8), allOff, listOf(plan(1, hour = 16))))
    }

    @Test
    fun `no plans and no daily reminder means no reminder`() {
        assertNull(next(at(today, 8), both))
    }

    @Test
    fun `start reminder fires at the plan's start time`() {
        val result = next(at(today, 8), startOnly, listOf(plan(1, hour = 16, minute = 30)))
        assertEquals(ScheduledReminder(ReminderKind.SESSION_START, at(today, 16, 30), 1L), result)
    }

    @Test
    fun `upcoming reminder fires ten minutes before the start`() {
        val result = next(at(today, 8), upcomingOnly, listOf(plan(1, hour = 16)))
        assertEquals(ScheduledReminder(ReminderKind.UPCOMING_SESSION, at(today, 15, 50), 1L), result)
    }

    @Test
    fun `with both on the heads-up comes first, then the start once the heads-up has passed`() {
        val plans = listOf(plan(1, hour = 16))
        assertEquals(ReminderKind.UPCOMING_SESSION, next(at(today, 8), both, plans)?.kind)
        assertEquals(ReminderKind.SESSION_START, next(at(today, 15, 55), both, plans)?.kind)
    }

    @Test
    fun `a heads-up that is already in the past is skipped, never sent late`() {
        // Plan starts in 3 minutes: the 10-minute heads-up is gone, the start reminder still fires.
        val result = next(at(today, 15, 57), both, listOf(plan(1, hour = 16)))
        assertEquals(ReminderKind.SESSION_START, result?.kind)
    }

    @Test
    fun `a plan that has already started produces nothing`() {
        assertNull(next(at(today, 16, 1), both, listOf(plan(1, hour = 16))))
    }

    @Test
    fun `only planned sessions produce reminders`() {
        val plans = listOf(
            plan(1, hour = 16, status = PlannedSessionStatus.COMPLETED),
            plan(2, hour = 17, status = PlannedSessionStatus.MISSED),
            plan(3, hour = 18, status = PlannedSessionStatus.CANCELLED)
        )
        assertNull(next(at(today, 8), both, plans))
    }

    @Test
    fun `the soonest reminder wins across several plans and days`() {
        val plans = listOf(
            plan(1, date = today.plusDays(1), hour = 9),
            plan(2, hour = 20),
            plan(3, hour = 14)
        )
        assertEquals(3L, next(at(today, 8), startOnly, plans)?.planId)
    }

    @Test
    fun `a plan on a later day is reminded on that day`() {
        val tomorrow = today.plusDays(1)
        val result = next(at(today, 22), startOnly, listOf(plan(1, date = tomorrow, hour = 9)))
        assertEquals(at(tomorrow, 9), result?.triggerAtEpochMillis)
    }

    // ---------- daily reminder ----------

    @Test
    fun `daily reminder is today's occurrence when it is still ahead`() {
        val settings = ReminderSettings(dailyReminder = true, dailyReminderMinuteOfDay = 19 * 60)
        val result = next(at(today, 10), settings)
        assertEquals(ScheduledReminder(ReminderKind.DAILY, at(today, 19), null), result)
    }

    @Test
    fun `daily reminder rolls to tomorrow once today's time has passed`() {
        val settings = ReminderSettings(dailyReminder = true, dailyReminderMinuteOfDay = 19 * 60)
        assertEquals(at(today.plusDays(1), 19), next(at(today, 19, 1), settings)?.triggerAtEpochMillis)
    }

    @Test
    fun `daily reminder exactly at the reminder time rolls to tomorrow, never re-fires`() {
        val settings = ReminderSettings(dailyReminder = true, dailyReminderMinuteOfDay = 19 * 60)
        assertEquals(at(today.plusDays(1), 19), next(at(today, 19), settings)?.triggerAtEpochMillis)
    }

    @Test
    fun `an earlier plan reminder beats the daily reminder and vice versa`() {
        val settings = ReminderSettings(plannedSessionReminders = true, dailyReminder = true, dailyReminderMinuteOfDay = 19 * 60)
        assertEquals(ReminderKind.SESSION_START, next(at(today, 8), settings, listOf(plan(1, hour = 16)))?.kind)
        assertEquals(ReminderKind.DAILY, next(at(today, 8), settings, listOf(plan(1, hour = 21)))?.kind)
    }

    // ---------- time zones ----------

    @Test
    fun `plan start is wall-clock time in the given zone`() {
        val kolkata = ZoneId.of("Asia/Kolkata") // UTC+05:30
        val p = plan(1, hour = 9)
        assertEquals(at(today, 9), plannedSessionStartMillis(p, utc))
        // 09:00 IST is 03:30 UTC.
        assertEquals(at(today, 3, 30), plannedSessionStartMillis(p, kolkata))
    }

    @Test
    fun `a plan keeps its wall-clock time across a daylight saving change`() {
        val newYork = ZoneId.of("America/New_York")
        val beforeDst = LocalDate.of(2026, 3, 7) // EST, UTC-5
        val afterDst = LocalDate.of(2026, 3, 9)  // EDT, UTC-4 (clocks changed Mar 8, 2026)
        val a = plannedSessionStartMillis(plan(1, date = beforeDst, hour = 9), newYork)
        val b = plannedSessionStartMillis(plan(2, date = afterDst, hour = 9), newYork)
        assertEquals(beforeDst.atTime(14, 0).atZone(utc).toInstant().toEpochMilli(), a) // 9:00 EST = 14:00 UTC
        assertEquals(afterDst.atTime(13, 0).atZone(utc).toInstant().toEpochMilli(), b)  // 9:00 EDT = 13:00 UTC
    }

    // ---------- remindersDueAt ----------

    @Test
    fun `two plans starting together are both due when their shared alarm fires`() {
        val plans = listOf(plan(1, hour = 16), plan(2, hour = 16), plan(3, hour = 17))
        val due = remindersDueAt(at(today, 16), startOnly, plans, utc)
        assertEquals(setOf(1L, 2L), due.map { it.planId }.toSet())
        assertTrue(due.all { it.kind == ReminderKind.SESSION_START })
    }

    @Test
    fun `a plan start and the daily reminder at the same instant are both due`() {
        val settings = ReminderSettings(plannedSessionReminders = true, dailyReminder = true, dailyReminderMinuteOfDay = 18 * 60)
        val due = remindersDueAt(at(today, 18), settings, listOf(plan(1, hour = 18)), utc)
        assertEquals(setOf(ReminderKind.SESSION_START, ReminderKind.DAILY), due.map { it.kind }.toSet())
    }

    @Test
    fun `due lookup uses the scheduled instant, so a heads-up is matched to the right plan`() {
        val due = remindersDueAt(at(today, 15, 50), upcomingOnly, listOf(plan(1, hour = 16), plan(2, hour = 17)), utc)
        assertEquals(listOf(1L), due.map { it.planId })
        assertEquals(ReminderKind.UPCOMING_SESSION, due.single().kind)
    }

    @Test
    fun `a reminder switched off or a plan since cancelled is no longer due`() {
        val trigger = at(today, 16)
        assertTrue(remindersDueAt(trigger, allOff, listOf(plan(1, hour = 16)), utc).isEmpty())
        assertTrue(remindersDueAt(trigger, startOnly, listOf(plan(1, hour = 16, status = PlannedSessionStatus.CANCELLED)), utc).isEmpty())
    }

    @Test
    fun `an unrelated instant has nothing due`() {
        assertTrue(remindersDueAt(at(today, 12), both, listOf(plan(1, hour = 16)), utc).isEmpty())
    }

    // ---------- minutesUntil / text ----------

    @Test
    fun `minutes until rounds up and is not positive once started`() {
        val start = at(today, 16)
        assertEquals(10, minutesUntil(start, start - 10 * 60_000L))
        assertEquals(10, minutesUntil(start, start - 9 * 60_000L - 20_000L)) // 9m20s away reads 10
        assertEquals(1, minutesUntil(start, start - 1_000L))
        assertEquals(0, minutesUntil(start, start))
        assertTrue(minutesUntil(start, start + 5 * 60_000L) <= 0)
    }

    @Test
    fun `upcoming title is singular for one minute`() {
        assertEquals("Starting in 1 minute", upcomingReminderTitle(1))
        assertEquals("Starting in 10 minutes", upcomingReminderTitle(10))
    }

    @Test
    fun `plan description combines subject and task, with a plain fallback`() {
        val subject = SubjectEntity(id = 1, name = "Mathematics", icon = "📐", colorArgb = 0, createdAtEpochMillis = 0L)
        val task = TaskEntity(title = "Calculus", priority = TaskPriority.MEDIUM.name, createdAtEpochMillis = 0L)
        assertEquals("📐 Mathematics · Calculus", describePlannedSession(subject, task))
        assertEquals("📐 Mathematics", describePlannedSession(subject, null))
        assertEquals("Calculus", describePlannedSession(null, task))
        assertEquals("Planned study session", describePlannedSession(null, null))
    }

    // ---------- shouldNotifyGoalReached ----------

    private val target = 5 * 60 * 60_000L

    @Test
    fun `goal notice fires on the crossing`() {
        assertTrue(shouldNotifyGoalReached(target - 60_000L, target + 60_000L, target, null, 100L, 1))
        assertTrue("landing exactly on the target counts", shouldNotifyGoalReached(target - 60_000L, target, target, null, 100L, 1))
    }

    @Test
    fun `goal notice stays silent below the goal and once it is already met`() {
        assertFalse(shouldNotifyGoalReached(0L, target - 1L, target, null, 100L, 1))
        // Already past the goal before this session: not a crossing, so no repeat notice.
        assertFalse(shouldNotifyGoalReached(target + 1L, target + 600_000L, target, null, 100L, 1))
    }

    @Test
    fun `goal notice is once per day for the daily goal`() {
        assertFalse(shouldNotifyGoalReached(target - 1L, target + 1L, target, lastNotifiedEpochDay = 100L, todayEpochDay = 100L, minDaysBetween = 1))
        assertTrue(shouldNotifyGoalReached(target - 1L, target + 1L, target, lastNotifiedEpochDay = 99L, todayEpochDay = 100L, minDaysBetween = 1))
    }

    @Test
    fun `weekly goal notice is at most once per seven days`() {
        assertFalse(shouldNotifyGoalReached(target - 1L, target + 1L, target, lastNotifiedEpochDay = 95L, todayEpochDay = 100L, minDaysBetween = 7))
        assertTrue(shouldNotifyGoalReached(target - 1L, target + 1L, target, lastNotifiedEpochDay = 93L, todayEpochDay = 100L, minDaysBetween = 7))
    }

    @Test
    fun `no goal notice for a missing or zero target`() {
        assertFalse(shouldNotifyGoalReached(0L, 10_000L, 0L, null, 100L, 1))
        assertFalse(shouldNotifyGoalReached(0L, 10_000L, -1L, null, 100L, 1))
    }

    // ---------- ReminderSettings ----------

    @Test
    fun `every reminder defaults to off and the presets stop at 9 PM`() {
        val defaults = ReminderSettings()
        assertFalse(defaults.plannedSessionReminders)
        assertFalse(defaults.upcomingSessionReminders)
        assertFalse(defaults.dailyReminder)
        assertFalse(defaults.goalNotifications)
        assertFalse(defaults.anyScheduledReminderEnabled)
        assertTrue(ReminderSettings.DAILY_TIME_PRESET_MINUTES.max() <= 21 * 60)
        assertTrue(ReminderSettings.DEFAULT_DAILY_REMINDER_MINUTE in ReminderSettings.DAILY_TIME_PRESET_MINUTES)
    }
}
