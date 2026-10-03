package com.studyspace.timer.planning

import com.studyspace.timer.data.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SmartPlannerTest {
    // Monday, chosen arbitrarily as a fixed, known day-of-week anchor for every test.
    private val monday: Long = LocalDate.of(2026, 10, 5).also {
        check(it.dayOfWeek == DayOfWeek.MONDAY)
    }.toEpochDay()

    private fun task(
        id: Long = 1,
        title: String = "Task $id",
        subjectId: Long? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        deadlineEpochDay: Long? = null,
        estimatedDurationMinutes: Int? = 60
    ) = PlannableTask(id, title, subjectId, priority, deadlineEpochDay, estimatedDurationMinutes)

    private fun everyDayEvenings(start: Int = 18 * 60, end: Int = 21 * 60): List<AvailabilityBlock> =
        DayOfWeek.values().map { AvailabilityBlock(it, start, end) }

    // ---------- degenerate inputs ----------

    @Test
    fun `no tasks yields no suggestions`() {
        val result = SmartPlanner.suggest(emptyList(), SmartPlanRequest(monday, 7, everyDayEvenings(), 240))
        assertEquals(0, result.suggestions.size)
        assertEquals(0, result.unscheduled.size)
    }

    @Test
    fun `no availability yields no suggestions and every task is fully unscheduled`() {
        val result = SmartPlanner.suggest(listOf(task()), SmartPlanRequest(monday, 7, emptyList(), 240))
        assertEquals(0, result.suggestions.size)
        assertEquals(1, result.unscheduled.size)
    }

    @Test
    fun `zero days ahead yields nothing`() {
        val result = SmartPlanner.suggest(listOf(task()), SmartPlanRequest(monday, 0, everyDayEvenings(), 240))
        assertEquals(0, result.suggestions.size)
    }

    // ---------- basic placement ----------

    @Test
    fun `a task that fits in one evening is scheduled once, at the start of that block`() {
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 45)),
            SmartPlanRequest(monday, 7, everyDayEvenings(), 240)
        )
        assertEquals(1, result.suggestions.size)
        val s = result.suggestions.single()
        assertEquals(monday, s.dateEpochDay)
        assertEquals(18 * 60, s.startMinuteOfDay)
        assertEquals(45, s.durationMinutes)
        assertEquals(0, result.unscheduled.size)
    }

    @Test
    fun `a null estimate defaults to DEFAULT_CHUNK_MINUTES`() {
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = null)),
            SmartPlanRequest(monday, 7, everyDayEvenings(), 240)
        )
        assertEquals(SmartPlanner.DEFAULT_CHUNK_MINUTES, result.suggestions.single().durationMinutes)
    }

    @Test
    fun `a long task spreads across multiple days, one chunk per day, rather than one long day`() {
        // 3-hour evening block, MAX_CHUNK_MINUTES caps a single session at 120, so a
        // 240-minute task needs at least 2 days even though one day's block could fit it.
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 240)),
            SmartPlanRequest(monday, 7, everyDayEvenings(), 300)
        )
        assertEquals(0, result.unscheduled.size)
        assertTrue("expected at least 2 sessions to spread a 240-minute task", result.suggestions.size >= 2)
        assertTrue(result.suggestions.all { it.durationMinutes <= SmartPlanner.MAX_CHUNK_MINUTES })
        // every suggestion is on a distinct day (at most one chunk per task per day)
        assertEquals(result.suggestions.size, result.suggestions.map { it.dateEpochDay }.distinct().size)
        assertEquals(240, result.suggestions.sumOf { it.durationMinutes })
    }

    // ---------- deadlines ----------

    @Test
    fun `a task is never scheduled after its deadline`() {
        val deadline = monday + 2
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 600, deadlineEpochDay = deadline)),
            SmartPlanRequest(monday, 14, everyDayEvenings(), 240)
        )
        assertTrue(result.suggestions.all { it.dateEpochDay <= deadline })
    }

    @Test
    fun `a task whose deadline leaves too little room is reported as unscheduled, not overflowed past the deadline`() {
        // Only 2 days before the deadline, each capped at 60 minutes -> 120 minutes
        // available, but the task needs 300. The other 180 must be reported, not
        // silently scheduled after the deadline.
        val deadline = monday + 1
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 300, deadlineEpochDay = deadline)),
            SmartPlanRequest(monday, 14, everyDayEvenings(18 * 60, 19 * 60), maxDailyMinutes = 60)
        )
        assertTrue(result.suggestions.all { it.dateEpochDay <= deadline })
        assertEquals(1, result.unscheduled.size)
        assertEquals(300 - result.suggestions.sumOf { it.durationMinutes }, result.unscheduled.single().unscheduledMinutes)
    }

    @Test
    fun `a task with no deadline can still be scheduled anywhere in the horizon`() {
        val result = SmartPlanner.suggest(
            listOf(task(deadlineEpochDay = null, estimatedDurationMinutes = 60)),
            SmartPlanRequest(monday, 7, everyDayEvenings(), 240)
        )
        assertEquals(1, result.suggestions.size)
    }

    // ---------- ordering: deadline and priority ----------

    @Test
    fun `a task with a sooner deadline is scheduled before one with a later deadline when they compete for the same limited day`() {
        val soon = task(id = 1, title = "Soon", deadlineEpochDay = monday, estimatedDurationMinutes = 60)
        val later = task(id = 2, title = "Later", deadlineEpochDay = monday + 5, estimatedDurationMinutes = 60)
        // Only one 60-minute slot exists on Monday itself; only one of the two competing
        // tasks can land there, and it must be the one whose deadline is that day.
        val result = SmartPlanner.suggest(
            listOf(later, soon),
            SmartPlanRequest(monday, 1, everyDayEvenings(18 * 60, 19 * 60), maxDailyMinutes = 60)
        )
        assertEquals(1, result.suggestions.size)
        assertEquals(1L, result.suggestions.single().taskId)
    }

    @Test
    fun `with no deadlines, a HIGH priority task is scheduled before a LOW priority one when they compete for the same limited day`() {
        val low = task(id = 1, title = "Low", priority = TaskPriority.LOW, estimatedDurationMinutes = 60)
        val high = task(id = 2, title = "High", priority = TaskPriority.HIGH, estimatedDurationMinutes = 60)
        val result = SmartPlanner.suggest(
            listOf(low, high),
            SmartPlanRequest(monday, 1, everyDayEvenings(18 * 60, 19 * 60), maxDailyMinutes = 60)
        )
        assertEquals(1, result.suggestions.size)
        assertEquals(2L, result.suggestions.single().taskId)
    }

    // ---------- capacity guardrails ----------

    @Test
    fun `maxDailyMinutes caps a day even when the availability block itself is larger`() {
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 180)),
            SmartPlanRequest(monday, 1, everyDayEvenings(18 * 60, 22 * 60), maxDailyMinutes = 60)
        )
        assertEquals(60, result.suggestions.sumOf { it.durationMinutes })
        assertEquals(120, result.unscheduled.single().unscheduledMinutes)
    }

    @Test
    fun `already-planned minutes reduce a day's remaining capacity`() {
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 60)),
            SmartPlanRequest(
                monday, 1, everyDayEvenings(18 * 60, 19 * 60), maxDailyMinutes = 60,
                alreadyPlannedMinutesByDay = mapOf(monday to 60)
            )
        )
        assertEquals(0, result.suggestions.size)
        assertEquals(60, result.unscheduled.single().unscheduledMinutes)
    }

    @Test
    fun `a day with no matching day-of-week availability is skipped entirely`() {
        // Only Saturday/Sunday availability; Monday-start, 3-day horizon covers Mon/Tue/Wed only.
        val weekendOnly = listOf(
            AvailabilityBlock(DayOfWeek.SATURDAY, 10 * 60, 12 * 60),
            AvailabilityBlock(DayOfWeek.SUNDAY, 10 * 60, 12 * 60)
        )
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 60)),
            SmartPlanRequest(monday, 3, weekendOnly, maxDailyMinutes = 240)
        )
        assertEquals(0, result.suggestions.size)
        assertEquals(60, result.unscheduled.single().unscheduledMinutes)
    }

    @Test
    fun `a block smaller than MIN_CHUNK_MINUTES is never used`() {
        val tinyBlock = listOf(AvailabilityBlock(DayOfWeek.MONDAY, 18 * 60, 18 * 60 + SmartPlanner.MIN_CHUNK_MINUTES - 1))
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 60)),
            SmartPlanRequest(monday, 1, tinyBlock, maxDailyMinutes = 240)
        )
        assertEquals(0, result.suggestions.size)
    }

    // ---------- determinism & purity ----------

    @Test
    fun `calling suggest twice with the same input gives the same result`() {
        val tasks = listOf(task(id = 1, estimatedDurationMinutes = 90), task(id = 2, estimatedDurationMinutes = 45))
        val request = SmartPlanRequest(monday, 5, everyDayEvenings(), 240)
        val first = SmartPlanner.suggest(tasks, request)
        val second = SmartPlanner.suggest(tasks, request)
        assertEquals(first, second)
    }

    @Test
    fun `suggestions never exceed a single availability block's time range`() {
        val result = SmartPlanner.suggest(
            listOf(task(estimatedDurationMinutes = 60)),
            SmartPlanRequest(monday, 1, everyDayEvenings(18 * 60, 19 * 60), maxDailyMinutes = 240)
        )
        val s = result.suggestions.single()
        assertTrue(s.startMinuteOfDay >= 18 * 60)
        assertTrue(s.startMinuteOfDay + s.durationMinutes <= 19 * 60)
    }

    @Test
    fun `two tasks on the same day get non-overlapping time slots`() {
        val result = SmartPlanner.suggest(
            listOf(task(id = 1, estimatedDurationMinutes = 60), task(id = 2, estimatedDurationMinutes = 60)),
            SmartPlanRequest(monday, 1, everyDayEvenings(18 * 60, 20 * 60), maxDailyMinutes = 240)
        )
        assertEquals(2, result.suggestions.size)
        val (a, b) = result.suggestions.sortedBy { it.startMinuteOfDay }
        assertTrue(a.startMinuteOfDay + a.durationMinutes <= b.startMinuteOfDay)
    }
}
