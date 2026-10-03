package com.studyspace.timer.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 21: JVM tests for [TimerEngine] using coroutines-test virtual time as
 * the injected clock. Tick interval is the default 200ms, so ticks land at
 * t = 0, 200, 400, ...; every test calls advanceTimeBy(n) then runCurrent() so
 * a tick scheduled exactly at the boundary has run.
 *
 * Written and hand-traced, NOT executed (no JDK/Gradle run available when
 * written) — CI is the first real run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private fun TestScope.engine(onCompleted: () -> Unit = {}) =
        TimerEngine(scope = backgroundScope, clock = { currentTime }, onCompleted = onCompleted)

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun startsStopwatchRunningAtZero() = runTest {
        val e = engine()
        e.start()
        runCurrent()
        val s = e.state.value
        assertEquals(TimerRunState.RUNNING, s.runState)
        assertEquals(TimerDirection.COUNT_UP, s.direction)
        assertEquals(0L, s.elapsedMillis)
        assertEquals(0L, s.remainingMillis)
    }

    @Test
    fun elapsedFollowsClock() = runTest {
        val e = engine()
        e.start()
        advance(1000)
        assertEquals(1000L, e.state.value.elapsedMillis)
    }

    @Test
    fun pauseFreezesElapsed() = runTest {
        val e = engine()
        e.start()
        advance(1000)
        e.pause()
        assertEquals(TimerRunState.PAUSED, e.state.value.runState)
        assertEquals(1000L, e.state.value.elapsedMillis)
        advance(5000)
        assertEquals(1000L, e.state.value.elapsedMillis)
    }

    @Test
    fun resumeExcludesPausedTime() = runTest {
        val e = engine()
        e.start()
        advance(1000)
        e.pause()
        advance(5000)
        e.resume()
        advance(600)
        assertEquals(TimerRunState.RUNNING, e.state.value.runState)
        assertEquals(1600L, e.state.value.elapsedMillis)
    }

    @Test
    fun pauseUsesClockNotTickCount() = runTest {
        // A delayed/missed tick must not cause drift: elapsed comes from the clock.
        var now = 0L
        val e = TimerEngine(scope = backgroundScope, clock = { now })
        e.start()
        runCurrent()
        now = 3000L // time passes with no tick running
        e.pause()
        assertEquals(3000L, e.state.value.elapsedMillis)
    }

    @Test
    fun pauseWhenIdleAndResumeWhenRunningAreNoOps() = runTest {
        val e = engine()
        e.pause()
        assertTrue(e.state.value.isIdle)
        e.resume()
        assertTrue(e.state.value.isIdle)
        e.start()
        runCurrent()
        e.resume() // already running
        assertTrue(e.state.value.isRunning)
    }

    @Test
    fun countdownReportsRemainingAndProgress() = runTest {
        val e = engine()
        e.start(targetMillis = 1000L)
        advance(600)
        val s = e.state.value
        assertEquals(TimerDirection.COUNT_DOWN, s.direction)
        assertEquals(600L, s.elapsedMillis)
        assertEquals(400L, s.remainingMillis)
        assertEquals(0.6f, s.progress, 0.0001f)
    }

    @Test
    fun countdownCompletesOnceAndClamps() = runTest {
        var completions = 0
        val e = engine { completions++ }
        e.start(targetMillis = 1000L)
        advance(1000)
        val s = e.state.value
        assertTrue(s.isCompleted)
        assertEquals(1000L, s.elapsedMillis)
        assertEquals(0L, s.remainingMillis)
        assertEquals(1f, s.progress, 0.0001f)
        assertEquals(1, completions)
        advance(5000)
        assertEquals(1, completions)
        assertEquals(1000L, e.state.value.elapsedMillis)
    }

    @Test
    fun stopwatchNeverCompletes() = runTest {
        var completions = 0
        val e = engine { completions++ }
        e.start()
        advance(60_000)
        assertTrue(e.state.value.isRunning)
        assertEquals(0, completions)
    }

    @Test
    fun resetReturnsToIdleAndStopsTicking() = runTest {
        val e = engine()
        e.start()
        advance(1000)
        e.reset()
        assertTrue(e.state.value.isIdle)
        assertEquals(0L, e.state.value.elapsedMillis)
        advance(2000)
        assertTrue(e.state.value.isIdle)
        assertEquals(0L, e.state.value.elapsedMillis)
    }

    @Test
    fun startWhileRunningRestartsFromZero() = runTest {
        val e = engine()
        e.start()
        advance(1000)
        e.start()
        assertEquals(0L, e.state.value.elapsedMillis)
        advance(400)
        assertEquals(400L, e.state.value.elapsedMillis)
    }

    @Test
    fun uiStateEdgeCases() {
        val up = TimerUiState(direction = TimerDirection.COUNT_UP, elapsedMillis = 5000L)
        assertEquals(0L, up.remainingMillis)
        assertEquals(0f, up.progress, 0f)
        val over = TimerUiState(
            direction = TimerDirection.COUNT_DOWN, elapsedMillis = 2000L, targetMillis = 1000L
        )
        assertEquals(0L, over.remainingMillis)
        assertEquals(1f, over.progress, 0f)
        assertFalse(over.isRunning)
    }
}
