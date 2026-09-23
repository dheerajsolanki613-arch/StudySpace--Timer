package com.studyspace.timer.data.repository

import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCompletionTest {

    private fun task(completed: Boolean = false, completedAt: Long? = null) = TaskEntity(
        id = 1L,
        title = "Practice questions",
        priority = TaskPriority.HIGH.name,
        completed = completed,
        createdAtEpochMillis = 100L,
        completedAtEpochMillis = completedAt
    )

    @Test
    fun `completing an open task stamps the completion time`() {
        val result = applyTaskCompletion(task(), completed = true, nowEpochMillis = 5_000L)
        assertTrue(result.completed)
        assertEquals(5_000L, result.completedAtEpochMillis)
    }

    @Test
    fun `completing an already-completed task keeps the original time`() {
        val result = applyTaskCompletion(task(completed = true, completedAt = 1_000L), completed = true, nowEpochMillis = 9_000L)
        assertEquals(1_000L, result.completedAtEpochMillis)
    }

    @Test
    fun `re-opening a task clears the completion time`() {
        val result = applyTaskCompletion(task(completed = true, completedAt = 1_000L), completed = false, nowEpochMillis = 9_000L)
        assertFalse(result.completed)
        assertNull(result.completedAtEpochMillis)
    }

    @Test
    fun `re-completing after a re-open gets a fresh time`() {
        val reopened = applyTaskCompletion(task(completed = true, completedAt = 1_000L), completed = false, nowEpochMillis = 2_000L)
        val recompleted = applyTaskCompletion(reopened, completed = true, nowEpochMillis = 3_000L)
        assertEquals(3_000L, recompleted.completedAtEpochMillis)
    }

    @Test
    fun `completion never changes any other field`() {
        val original = task()
        val result = applyTaskCompletion(original, completed = true, nowEpochMillis = 5_000L)
        assertEquals(original.copy(completed = true, completedAtEpochMillis = 5_000L), result)
    }

    @Test
    fun `a legacy completed task with no time gets one only if completed again`() {
        // Legacy rows (completed before times were recorded) stay null until the user re-opens and re-completes them.
        val legacy = task(completed = true, completedAt = null)
        assertNull(legacy.completedAtEpochMillis)
        val reopened = applyTaskCompletion(legacy, completed = false, nowEpochMillis = 2_000L)
        assertNull(reopened.completedAtEpochMillis)
        assertEquals(3_000L, applyTaskCompletion(reopened, completed = true, nowEpochMillis = 3_000L).completedAtEpochMillis)
    }
}
