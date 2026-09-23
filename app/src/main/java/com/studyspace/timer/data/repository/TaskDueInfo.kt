package com.studyspace.timer.data.repository

/** How urgent a task's deadline is, for the Tasks list's due-date chip color/emphasis. */
enum class TaskDueUrgency { NONE, OVERDUE, DUE_TODAY, UPCOMING }

data class TaskDueInfo(val urgency: TaskDueUrgency, val label: String?)

/**
 * Pure — takes plain epoch-day longs (see [com.studyspace.timer.data.db.TaskEntity.deadlineEpochDay]'s
 * KDoc for why a day rather than a moment) so this needs no Android/time-zone
 * dependency and is covered directly by `TaskDueInfoTest`, same reasoning as
 * [SessionRepository.computeStreak].
 */
fun computeTaskDueInfo(deadlineEpochDay: Long?, todayEpochDay: Long): TaskDueInfo {
    if (deadlineEpochDay == null) return TaskDueInfo(TaskDueUrgency.NONE, null)
    val daysUntil = deadlineEpochDay - todayEpochDay
    return when {
        daysUntil < 0L -> TaskDueInfo(TaskDueUrgency.OVERDUE, "Overdue")
        daysUntil == 0L -> TaskDueInfo(TaskDueUrgency.DUE_TODAY, "Due today")
        daysUntil == 1L -> TaskDueInfo(TaskDueUrgency.UPCOMING, "Due tomorrow")
        else -> TaskDueInfo(TaskDueUrgency.UPCOMING, "Due in $daysUntil days")
    }
}
