package com.studyspace.timer.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftResolverTest {
    private val today = 100L
    private val subjects = listOf(KnownSubject(1, "Physics"), KnownSubject(2, "Maths"))
    private val tasks = listOf(
        KnownTask(10, "Mechanics", subjectId = 1),
        KnownTask(11, "Calculus", subjectId = 2),
        KnownTask(12, "Revision", subjectId = null)
    )

    private fun draft(
        date: Long = today + 1,
        start: Int? = 15 * 60,
        duration: Int = 60,
        subject: String? = null,
        task: String? = null,
        notes: String? = null
    ) = DraftSession(date, start, duration, subject, task, notes)

    private fun resolve(vararg drafts: DraftSession, subjects: List<KnownSubject> = this.subjects, tasks: List<KnownTask> = this.tasks) =
        DraftResolver.resolve(drafts.toList(), subjects, tasks, today)

    // ---------- happy path ----------

    @Test
    fun `a valid draft resolves subject and task by case-insensitive name`() {
        val r = resolve(draft(subject = "physics", task = "mechanics")).resolved.single()
        assertEquals(1L, r.subjectId)
        assertEquals(10L, r.taskId)
        assertTrue(r.warnings.isEmpty())
        assertEquals(15 * 60, r.startMinuteOfDay)
    }

    @Test
    fun `a missing start time gets the default`() {
        val r = resolve(draft(start = null)).resolved.single()
        assertEquals(DraftResolver.DEFAULT_START_MINUTE_OF_DAY, r.startMinuteOfDay)
    }

    @Test
    fun `no drafts gives an empty resolution`() {
        val r = resolve()
        assertTrue(r.resolved.isEmpty())
        assertTrue(r.rejected.isEmpty())
    }

    // ---------- rejection: impossible values are reported, never silently fixed ----------

    @Test
    fun `a zero or over-24-hour duration is rejected`() {
        assertEquals(2, resolve(draft(duration = 0), draft(duration = 1441)).rejected.size)
    }

    @Test
    fun `a session that would run past midnight is rejected`() {
        val r = resolve(draft(start = 23 * 60 + 30, duration = 60))
        assertEquals(0, r.resolved.size)
        assertTrue(r.rejected.single().reason.contains("midnight"))
    }

    @Test
    fun `a start time of 1440 is not a valid time of day`() {
        assertEquals(1, resolve(draft(start = 1440, duration = 1)).rejected.size)
    }

    @Test
    fun `a date in the past is rejected but today is accepted`() {
        val r = resolve(draft(date = today - 1), draft(date = today))
        assertEquals(1, r.rejected.size)
        assertEquals(1, r.resolved.size)
        assertEquals(today, r.resolved.single().dateEpochDay)
    }

    @Test
    fun `a date more than a year ahead is rejected but exactly a year is accepted`() {
        val r = resolve(
            draft(date = today + DraftResolver.MAX_DAYS_AHEAD + 1),
            draft(date = today + DraftResolver.MAX_DAYS_AHEAD)
        )
        assertEquals(1, r.rejected.size)
        assertEquals(1, r.resolved.size)
    }

    @Test
    fun `one bad draft does not stop the good ones`() {
        val r = resolve(draft(duration = 0), draft())
        assertEquals(1, r.resolved.size)
        assertEquals(1, r.rejected.size)
    }

    // ---------- linking: nothing is ever invented ----------

    @Test
    fun `an unknown subject is left unlinked with a warning, and the session is still offered`() {
        val r = resolve(draft(subject = "Astrology")).resolved.single()
        assertNull(r.subjectId)
        assertEquals(1, r.warnings.size)
        assertTrue(r.warnings.single().contains("No subject"))
    }

    @Test
    fun `an unknown task is left unlinked with a warning`() {
        val r = resolve(draft(task = "Nonexistent")).resolved.single()
        assertNull(r.taskId)
        assertTrue(r.warnings.single().contains("No task"))
    }

    @Test
    fun `a task with no named subject inherits the task's own subject`() {
        val r = resolve(draft(task = "Calculus")).resolved.single()
        assertEquals(11L, r.taskId)
        assertEquals(2L, r.subjectId)
    }

    @Test
    fun `a task belonging to a different subject than the one named is not linked`() {
        val r = resolve(draft(subject = "Physics", task = "Calculus")).resolved.single()
        assertNull(r.taskId)
        assertEquals(1L, r.subjectId)
        assertTrue(r.warnings.single().contains("different subject"))
    }

    @Test
    fun `a task with no subject at all can be linked under a named subject`() {
        val r = resolve(draft(subject = "Physics", task = "Revision")).resolved.single()
        assertEquals(12L, r.taskId)
        assertEquals(1L, r.subjectId)
    }

    @Test
    fun `two tasks with the same title are not guessed between`() {
        val dupTasks = listOf(KnownTask(20, "Essay", null), KnownTask(21, "Essay", null))
        val r = resolve(draft(task = "Essay"), tasks = dupTasks).resolved.single()
        assertNull(r.taskId)
        assertTrue(r.warnings.single().contains("More than one task"))
    }

    @Test
    fun `two subjects with the same name are not guessed between`() {
        val dupSubjects = listOf(KnownSubject(5, "Bio"), KnownSubject(6, "bio"))
        val r = resolve(draft(subject = "Bio"), subjects = dupSubjects).resolved.single()
        assertNull(r.subjectId)
        assertTrue(r.warnings.single().contains("More than one subject"))
    }

    @Test
    fun `resolved ids only ever come from the known lists`() {
        val r = resolve(draft(subject = "Physics", task = "Mechanics"), draft(subject = "Nope", task = "Nope")).resolved
        val knownSubjectIds = subjects.map { it.id }.toSet()
        val knownTaskIds = tasks.map { it.id }.toSet()
        assertTrue(r.all { it.subjectId == null || it.subjectId in knownSubjectIds })
        assertTrue(r.all { it.taskId == null || it.taskId in knownTaskIds })
    }

    // ---------- notes ----------

    @Test
    fun `notes are trimmed and blank notes become null`() {
        val r = resolve(draft(notes = "  bring calculator  "), draft(notes = "   ")).resolved
        assertEquals("bring calculator", r[0].notes)
        assertNull(r[1].notes)
    }
}
