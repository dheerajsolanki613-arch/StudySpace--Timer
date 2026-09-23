package com.studyspace.timer.data.repository

import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.TaskDao
import com.studyspace.timer.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for [TaskEntity] storage — same role
 * [SubjectRepository]/[GoalRepository] play for their own tables.
 */
class TaskRepository(private val dao: TaskDao) {

    fun allTasks(): Flow<List<TaskEntity>> = dao.allTasks()

    fun tasksForSubject(subjectId: Long): Flow<List<TaskEntity>> = dao.tasksForSubject(subjectId)

    suspend fun createTask(
        title: String,
        chapter: String?,
        topic: String?,
        subjectId: Long?,
        priority: TaskPriority,
        deadlineEpochDay: Long?,
        estimatedDurationMinutes: Int?
    ): Long = dao.insert(
        TaskEntity(
            title = title.trim(),
            chapter = chapter?.trim()?.ifBlank { null },
            topic = topic?.trim()?.ifBlank { null },
            subjectId = subjectId,
            priority = priority.name,
            deadlineEpochDay = deadlineEpochDay,
            estimatedDurationMinutes = estimatedDurationMinutes,
            completed = false,
            createdAtEpochMillis = System.currentTimeMillis()
        )
    )

    suspend fun updateTask(
        existing: TaskEntity,
        title: String,
        chapter: String?,
        topic: String?,
        subjectId: Long?,
        priority: TaskPriority,
        deadlineEpochDay: Long?,
        estimatedDurationMinutes: Int?
    ) {
        dao.update(
            existing.copy(
                title = title.trim(),
                chapter = chapter?.trim()?.ifBlank { null },
                topic = topic?.trim()?.ifBlank { null },
                subjectId = subjectId,
                priority = priority.name,
                deadlineEpochDay = deadlineEpochDay,
                estimatedDurationMinutes = estimatedDurationMinutes
            )
        )
    }

    suspend fun setCompleted(task: TaskEntity, completed: Boolean) {
        dao.update(applyTaskCompletion(task, completed, System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: TaskEntity) = dao.delete(task)
}

/**
 * Returns [task] with its completion state and [TaskEntity.completedAtEpochMillis]
 * kept consistent (Phase 11). Pure — [nowEpochMillis] is passed in rather than
 * read from the clock — so it is covered by a fast local JUnit test.
 *
 *  - Completing an incomplete task stamps [nowEpochMillis].
 *  - Completing a task that is *already* complete keeps its original
 *    timestamp (a stale double-tap must not move "completed today" to a
 *    later day).
 *  - Re-opening a task clears the timestamp, so a task that is not complete
 *    can never be counted as completed on any day.
 */
fun applyTaskCompletion(task: TaskEntity, completed: Boolean, nowEpochMillis: Long): TaskEntity =
    if (completed) {
        task.copy(completed = true, completedAtEpochMillis = task.completedAtEpochMillis ?: nowEpochMillis)
    } else {
        task.copy(completed = false, completedAtEpochMillis = null)
    }
