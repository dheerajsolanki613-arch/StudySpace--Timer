package com.studyspace.timer.planning

import com.studyspace.timer.data.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Phase 17 (Smart Study Planning). This is a **suggestion engine**, not a
 * scheduler that knows the user's real life: it only ever sees what this app
 * knows about — incomplete tasks (with their deadline, priority, and
 * estimated duration) and the free time windows the user tells it about
 * ([AvailabilityBlock] — this app has no calendar integration, so "available"
 * is only ever what the user explicitly says, never inferred). Its output
 * ([SuggestedSession] list) is never written to the database directly; every
 * caller must route it through the same preview-then-edit-then-confirm flow
 * a person uses for a manually created plan (see
 * `screens/planner/SmartPlanScreen.kt`), and nothing here prevents editing or
 * deleting a suggestion once accepted — it becomes an ordinary
 * `PlannedSessionEntity` like any other.
 *
 * The algorithm is a straightforward greedy heuristic (earliest-deadline
 * first, then priority, one chunk per task per day so studying spreads out
 * rather than crams), not a claim of optimality. [SmartPlanResult.unscheduled]
 * is reported honestly when a task's estimated time doesn't fit the
 * available horizon, rather than silently dropping it or overflowing a day
 * past [SmartPlanRequest.maxDailyMinutes] to force it in — that cap exists
 * specifically so this feature can never suggest an unhealthy, back-to-back
 * single day of studying, matching the same "don't pressure continuous
 * studying" principle Phase 9's achievements already follow.
 */

/** A recurring weekly window the user says they're generally free, e.g. weekday evenings. */
data class AvailabilityBlock(
    val dayOfWeek: DayOfWeek,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
) {
    init {
        require(startMinuteOfDay in 0..1439) { "startMinuteOfDay out of range: $startMinuteOfDay" }
        require(endMinuteOfDay in 0..1440) { "endMinuteOfDay out of range: $endMinuteOfDay" }
        require(endMinuteOfDay > startMinuteOfDay) { "endMinuteOfDay ($endMinuteOfDay) must be after startMinuteOfDay ($startMinuteOfDay)" }
    }

    val durationMinutes: Int get() = endMinuteOfDay - startMinuteOfDay
}

/** An incomplete task as the planner needs to see it — a projection, not the DB entity, so this file has no Room dependency. */
data class PlannableTask(
    val taskId: Long,
    val title: String,
    val subjectId: Long?,
    val priority: TaskPriority,
    /** Epoch day the task is due, or null for no deadline. A task past its deadline is the caller's concern to filter out — this class doesn't know "today". */
    val deadlineEpochDay: Long?,
    /** Minutes estimated for the whole task, or null to use [SmartPlanner.DEFAULT_CHUNK_MINUTES] as a single-session guess. */
    val estimatedDurationMinutes: Int?
)

/** One suggested addition to the planner. Carries [taskId] so the caller can label/link it; nothing here is written to the database yet. */
data class SuggestedSession(
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val taskId: Long,
    val subjectId: Long?
)

/** A task whose estimated time didn't fully fit within the horizon/deadline/daily cap — reported, not silently dropped. */
data class UnscheduledRemainder(
    val taskId: Long,
    val title: String,
    val unscheduledMinutes: Int
)

data class SmartPlanResult(
    val suggestions: List<SuggestedSession>,
    val unscheduled: List<UnscheduledRemainder>
)

/**
 * @param startEpochDay the first day to consider (usually today).
 * @param daysAhead how many days, starting at [startEpochDay], to plan across.
 * @param availability the user's recurring weekly free-time windows. Empty means nothing can be suggested.
 * @param maxDailyMinutes a hard cap per day, regardless of how much [availability] offers that day —
 *   the "don't suggest an unhealthy cram day" guardrail. Callers typically pass the user's own daily
 *   study goal, or a sane default if none is set.
 * @param alreadyPlannedMinutesByDay total minutes already planned or spent studying on each day
 *   (epoch day to minutes), so a day that's already busy has less room suggested for it. This is a
 *   total, not exact time slots — see the class doc on why exact overlap-checking isn't attempted.
 */
data class SmartPlanRequest(
    val startEpochDay: Long,
    val daysAhead: Int,
    val availability: List<AvailabilityBlock>,
    val maxDailyMinutes: Int,
    val alreadyPlannedMinutesByDay: Map<Long, Int> = emptyMap()
)

object SmartPlanner {
    const val DEFAULT_CHUNK_MINUTES = 60
    const val MAX_CHUNK_MINUTES = 120
    const val MIN_CHUNK_MINUTES = 20

    private class DayState(val dateEpochDay: Long, val blocks: MutableList<IntRange>, var capacityMinutes: Int)

    private fun priorityRank(priority: TaskPriority): Int = when (priority) {
        TaskPriority.HIGH -> 0
        TaskPriority.MEDIUM -> 1
        TaskPriority.LOW -> 2
    }

    /**
     * Suggests where to fit [tasks] into [request]'s available time. Never
     * mutates either argument. Deterministic for the same input (ties broken
     * by task order), so this is straightforward to unit test and to reason
     * about — no randomness, no external calls.
     */
    fun suggest(tasks: List<PlannableTask>, request: SmartPlanRequest): SmartPlanResult {
        if (request.daysAhead <= 0 || request.availability.isEmpty() || tasks.isEmpty() || request.maxDailyMinutes <= 0) {
            return SmartPlanResult(emptyList(), emptyList())
        }

        val orderedTasks = tasks.sortedWith(
            compareBy<PlannableTask> { it.deadlineEpochDay == null }
                .thenBy { it.deadlineEpochDay ?: Long.MAX_VALUE }
                .thenBy { priorityRank(it.priority) }
        )

        val days = (0 until request.daysAhead).map { offset ->
            val dateEpochDay = request.startEpochDay + offset
            val dow = LocalDate.ofEpochDay(dateEpochDay).dayOfWeek
            val blocksForDay = request.availability
                .filter { it.dayOfWeek == dow }
                .map { it.startMinuteOfDay..it.endMinuteOfDay }
                .sortedBy { it.first }
                .toMutableList()
            val totalAvailable = blocksForDay.sumOf { it.last - it.first }
            val alreadyUsed = request.alreadyPlannedMinutesByDay[dateEpochDay] ?: 0
            val capacity = (totalAvailable - alreadyUsed).coerceIn(0, request.maxDailyMinutes)
            DayState(dateEpochDay, blocksForDay, capacity)
        }

        val suggestions = mutableListOf<SuggestedSession>()
        val unscheduled = mutableListOf<UnscheduledRemainder>()

        for (task in orderedTasks) {
            var remaining = (task.estimatedDurationMinutes ?: DEFAULT_CHUNK_MINUTES).coerceAtLeast(MIN_CHUNK_MINUTES)
            val deadline = task.deadlineEpochDay

            for (day in days) {
                if (remaining <= 0) break
                if (deadline != null && day.dateEpochDay > deadline) continue
                if (day.capacityMinutes < MIN_CHUNK_MINUTES || day.blocks.isEmpty()) continue

                // At most one chunk per task per day, so a task with lots of time
                // spreads across several days rather than filling one day alone.
                val chunk = minOf(remaining, MAX_CHUNK_MINUTES, day.capacityMinutes)
                if (chunk < MIN_CHUNK_MINUTES) continue

                val blockIndex = day.blocks.indexOfFirst { (it.last - it.first) >= chunk }
                if (blockIndex == -1) continue

                val block = day.blocks[blockIndex]
                val start = block.first
                suggestions += SuggestedSession(
                    dateEpochDay = day.dateEpochDay,
                    startMinuteOfDay = start,
                    durationMinutes = chunk,
                    taskId = task.taskId,
                    subjectId = task.subjectId
                )

                val shrunk = (start + chunk)..block.last
                if (shrunk.last - shrunk.first < MIN_CHUNK_MINUTES) {
                    day.blocks.removeAt(blockIndex)
                } else {
                    day.blocks[blockIndex] = shrunk
                }
                day.capacityMinutes -= chunk
                remaining -= chunk
            }

            if (remaining > 0) {
                unscheduled += UnscheduledRemainder(task.taskId, task.title, remaining)
            }
        }

        return SmartPlanResult(
            suggestions = suggestions.sortedWith(compareBy({ it.dateEpochDay }, { it.startMinuteOfDay })),
            unscheduled = unscheduled
        )
    }
}
