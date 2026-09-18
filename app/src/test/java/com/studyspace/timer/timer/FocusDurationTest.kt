package com.studyspace.timer.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FocusDuration] has no Android dependency by design (see its class doc),
 * specifically so this suite can run as a fast local JUnit test with no
 * SDK/emulator -- the same constraint this whole project builds around
 * (see `PROJECT_STATE.md`). These tests cover the "validate the duration
 * before starting" / "prevent invalid or zero-duration sessions" spec
 * directly against the one choke point [FocusModeViewModel.confirmAndStart]
 * goes through.
 */
class FocusDurationTest {

    @Test
    fun `zero duration is invalid`() {
        val result = FocusDuration.validate(minutes = 0, seconds = 0)
        assertTrue(result is FocusDuration.ValidationResult.Invalid)
    }

    @Test
    fun `negative minutes is invalid`() {
        val result = FocusDuration.validate(minutes = -1, seconds = 0)
        assertTrue(result is FocusDuration.ValidationResult.Invalid)
    }

    @Test
    fun `negative seconds is invalid`() {
        val result = FocusDuration.validate(minutes = 5, seconds = -1)
        assertTrue(result is FocusDuration.ValidationResult.Invalid)
    }

    @Test
    fun `seconds of 60 or more is invalid`() {
        val result = FocusDuration.validate(minutes = 5, seconds = 60)
        assertTrue(result is FocusDuration.ValidationResult.Invalid)
    }

    @Test
    fun `duration over the maximum is invalid`() {
        val result = FocusDuration.validate(minutes = FocusDuration.MAX_MINUTES + 1, seconds = 0)
        assertTrue(result is FocusDuration.ValidationResult.Invalid)
    }

    @Test
    fun `a valid preset duration converts minutes to milliseconds`() {
        val result = FocusDuration.validate(minutes = 25, seconds = 0)
        assertTrue(result is FocusDuration.ValidationResult.Valid)
        assertEquals(25 * 60_000L, (result as FocusDuration.ValidationResult.Valid).totalMillis)
    }

    @Test
    fun `a valid custom duration combines minutes and seconds`() {
        val result = FocusDuration.validate(minutes = 1, seconds = 30)
        assertTrue(result is FocusDuration.ValidationResult.Valid)
        assertEquals(90_000L, (result as FocusDuration.ValidationResult.Valid).totalMillis)
    }

    @Test
    fun `one second is the smallest valid duration`() {
        val result = FocusDuration.validate(minutes = 0, seconds = 1)
        assertTrue(result is FocusDuration.ValidationResult.Valid)
        assertEquals(1_000L, (result as FocusDuration.ValidationResult.Valid).totalMillis)
    }

    @Test
    fun `the maximum duration itself is valid`() {
        val result = FocusDuration.validate(minutes = FocusDuration.MAX_MINUTES, seconds = 0)
        assertTrue(result is FocusDuration.ValidationResult.Valid)
    }
}
