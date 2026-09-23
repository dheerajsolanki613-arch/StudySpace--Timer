package com.studyspace.timer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GoalProgress] has no Android dependency by design (see its class doc),
 * same reasoning as [com.studyspace.timer.timer.FocusDurationTest] — covers
 * the percent/remaining/completion math the Goals screen's cards render
 * directly, with no SDK/emulator needed.
 */
class GoalProgressTest {

    @Test
    fun `midway progress computes expected percent and remaining`() {
        // 3h42m of a 5h goal, matching the spec's own worked example.
        val progress = GoalProgress(
            label = "Today",
            currentMillis = (3 * 60 + 42) * 60_000L,
            targetMillis = 5 * 60 * 60_000L
        )
        assertEquals(74, progress.percent)
        assertEquals((1 * 60 + 18) * 60_000L, progress.remainingMillis)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `zero target never divides by zero and reports zero progress`() {
        val progress = GoalProgress(label = "Weekly", currentMillis = 60_000L, targetMillis = 0L)
        assertEquals(0f, progress.progressFraction)
        assertEquals(0, progress.percent)
        assertEquals(0L, progress.remainingMillis)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `meeting the target exactly is complete with zero remaining`() {
        val progress = GoalProgress(label = "Today", currentMillis = 240 * 60_000L, targetMillis = 240 * 60_000L)
        assertEquals(100, progress.percent)
        assertEquals(0L, progress.remainingMillis)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `overshooting the target clamps percent at 100 and stays complete`() {
        val progress = GoalProgress(label = "Today", currentMillis = 300 * 60_000L, targetMillis = 240 * 60_000L)
        assertEquals(100, progress.percent)
        assertEquals(0L, progress.remainingMillis)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `zero current progress is zero percent and not complete`() {
        val progress = GoalProgress(label = "Today", currentMillis = 0L, targetMillis = 240 * 60_000L)
        assertEquals(0, progress.percent)
        assertEquals(240 * 60_000L, progress.remainingMillis)
        assertFalse(progress.isComplete)
    }
}
