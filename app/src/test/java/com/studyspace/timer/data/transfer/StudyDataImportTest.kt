package com.studyspace.timer.data.transfer

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StudyDataImportTest {
    private val utc = ZoneId.of("UTC")

    private fun utcMillis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, sec: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min, sec).atZone(utc).toInstant().toEpochMilli()

    /** "Now" for every test: 2026-09-21 12:00 UTC. */
    private val now = utcMillis(2026, 9, 21, 12)

    private val header = "Date,Start time,End time,Duration (seconds),Subject,Task,Session type,Label,Completed"

    private fun csv(vararg rows: String) = (listOf(header) + rows).joinToString("\n")

    private fun parse(text: String) = parseStudyDataFile(text, now, utc)

    private fun success(text: String): ParsedImport {
        val result = parse(text)
        assertTrue("expected success but got $result", result is ParseResult.Success)
        return (result as ParseResult.Success).parsed
    }

    private fun failure(text: String): String {
        val result = parse(text)
        assertTrue("expected failure but got $result", result is ParseResult.Failure)
        return (result as ParseResult.Failure).message
    }

    private val goodRow = "2026-09-20,16:00:00,17:00:00,3600,Physics,,SELF_STUDY,Mechanics,yes"

    // ======================= CSV: valid =======================

    @Test
    fun `a valid csv row becomes a session with everything mapped`() {
        val s = success(csv(goodRow)).sessions.single()
        assertEquals("SELF_STUDY", s.type)
        assertEquals("Mechanics", s.label)
        assertEquals(utcMillis(2026, 9, 20, 16), s.startEpochMillis)
        assertEquals(3_600_000L, s.durationMillis)
        assertTrue(s.completedNaturally)
        assertEquals(LocalDate.of(2026, 9, 20).toEpochDay(), s.dateEpochDay)
        assertEquals("Physics", s.subjectName)
        assertNull(s.taskTitle)
    }

    @Test
    fun `csv parsing tolerates BOM, CRLF, blank lines, any column order, case and extra columns`() {
        val text = "\uFEFFduration (SECONDS),SESSION TYPE,date,Start Time,Extra\r\n" +
            "\r\n" +
            "1800,pomodoro,2026-09-20,09:30,ignored\r\n"
        val parsed = success(text)
        assertEquals(1, parsed.sessions.size)
        assertEquals("POMODORO", parsed.sessions[0].type)
        assertEquals(utcMillis(2026, 9, 20, 9, 30), parsed.sessions[0].startEpochMillis)
        assertTrue(parsed.rejected.isEmpty())
    }

    @Test
    fun `optional csv columns can be absent and label falls back to the type's name`() {
        val parsed = success("Date,Start time,Duration (seconds),Session type\n2026-09-20,10:00:00,600,Self-Study")
        val s = parsed.sessions.single()
        assertEquals("SELF_STUDY", s.type)
        assertEquals("Self-Study", s.label)
        assertFalse(s.completedNaturally)
        assertNull(s.subjectName)
    }

    @Test
    fun `friendly session type spellings are accepted`() {
        listOf("Online Study" to "ONLINE_STUDY", "focus mode" to "FOCUS_MODE", "NORMAL_TIMER" to "NORMAL_TIMER").forEach { (raw, expected) ->
            val s = success(csv("2026-09-20,10:00:00,10:10:00,600,,,$raw,x,no")).sessions.single()
            assertEquals(expected, s.type)
        }
    }

    @Test
    fun `csv import undoes the export's formula guard`() {
        val s = success(csv("2026-09-20,10:00:00,10:10:00,600,'=SUM(A1),,POMODORO,'+cmd,no")).sessions.single()
        assertEquals("=SUM(A1)", s.subjectName)
        assertEquals("+cmd", s.label)
    }

    @Test
    fun `csv start time is interpreted in the given zone`() {
        val kolkata = ZoneId.of("Asia/Kolkata")
        val result = parseStudyDataFile(csv("2026-09-20,09:00:00,09:10:00,600,,,POMODORO,x,no"), now, kolkata)
        val s = (result as ParseResult.Success).parsed.sessions.single()
        assertEquals(utcMillis(2026, 9, 20, 3, 30), s.startEpochMillis)
        assertEquals(LocalDate.of(2026, 9, 20).toEpochDay(), s.dateEpochDay)
    }

    @Test
    fun `a session within the future tolerance is accepted`() {
        // 8 hours after "now": tolerated (clock skew / time zones), unlike 4 days.
        assertEquals(1, success(csv("2026-09-21,20:00:00,20:10:00,600,,,POMODORO,x,no")).sessions.size)
    }

    // ======================= CSV: invalid rows =======================

    private fun rejectionFor(row: String): RejectedRow = success(csv(row)).rejected.single()

    @Test
    fun `each kind of bad csv row is rejected with its row number and a reason`() {
        assertTrue(rejectionFor("20-09-2026,16:00:00,,3600,,,SELF_STUDY,x,no").reason.contains("date"))
        assertTrue(rejectionFor("2026-09-20,4pm,,3600,,,SELF_STUDY,x,no").reason.contains("start time"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,abc,,,SELF_STUDY,x,no").reason.contains("duration"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,3600.5,,,SELF_STUDY,x,no").reason.contains("duration"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,,,,SELF_STUDY,x,no").reason.contains("duration"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,3600,,,Napping,x,no").reason.contains("Unknown session type"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,3600,,,SELF_STUDY,x,maybe").reason.contains("Completed"))
    }

    @Test
    fun `out-of-range values are rejected`() {
        assertTrue(rejectionFor("2026-09-20,16:00:00,,5,,,SELF_STUDY,x,no").reason.contains("shorter than 10 seconds"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,-30,,,SELF_STUDY,x,no").reason.contains("shorter than 10 seconds"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,90000,,,SELF_STUDY,x,no").reason.contains("longer than 24 hours"))
        assertTrue(rejectionFor("2026-09-20,16:00:00,,99999999999999999,,,SELF_STUDY,x,no").reason.contains("longer than 24 hours"))
        assertTrue(rejectionFor("2026-09-25,16:00:00,,3600,,,SELF_STUDY,x,no").reason.contains("future"))
        assertTrue(rejectionFor("1999-12-31,16:00:00,,3600,,,SELF_STUDY,x,no").reason.contains("before the year 2000"))
    }

    @Test
    fun `over-long text is rejected`() {
        val long = "x".repeat(ImportLimits.MAX_LABEL_LENGTH + 1)
        assertTrue(rejectionFor("2026-09-20,16:00:00,,3600,,,SELF_STUDY,$long,no").reason.contains("Label"))
    }

    @Test
    fun `bad rows are reported but do not stop good rows, and row numbers count the header`() {
        val parsed = success(csv(goodRow, "not-a-date,16:00:00,,3600,,,SELF_STUDY,x,no", goodRow.replace("2026-09-20", "2026-09-19")))
        assertEquals(2, parsed.sessions.size)
        assertEquals(1, parsed.rejected.size)
        assertEquals("Row 3", parsed.rejected[0].where) // header is row 1, so the second data row is row 3
    }

    @Test
    fun `a row with too few columns is rejected rather than crashing`() {
        val parsed = success(csv("2026-09-20"))
        assertEquals(0, parsed.sessions.size)
        assertEquals(1, parsed.rejected.size)
    }

    // ======================= CSV: whole-file failures =======================

    @Test
    fun `empty and whitespace-only files fail with a clear message`() {
        assertTrue(failure("").contains("empty"))
        assertTrue(failure("   \n\n  ").contains("empty"))
        assertTrue(failure("\uFEFF").contains("empty"))
    }

    @Test
    fun `a csv missing required columns fails and names them`() {
        val message = failure("Foo,Bar\n1,2")
        assertTrue(message.contains("Missing column"))
        assertTrue(message.contains("Date"))
        assertTrue(message.contains("Duration (seconds)"))
    }

    @Test
    fun `a damaged csv with an unclosed quote fails instead of importing garbage`() {
        assertTrue(failure("$header\n2026-09-20,16:00:00,,3600,\"Physics,,SELF_STUDY,x,no").contains("damaged"))
    }

    @Test
    fun `a csv with only a header is valid and has nothing to import`() {
        val parsed = success(header)
        assertTrue(parsed.sessions.isEmpty())
        assertTrue(parsed.rejected.isEmpty())
        assertFalse(planImport(parsed, emptyList(), emptyList(), emptyList()).hasChanges)
    }

    // ======================= JSON =======================

    private val t0 = utcMillis(2026, 9, 20, 10)

    private fun jsonSession(type: String = "POMODORO", start: Long = t0, duration: Long = 1_500_000L, extra: String = "") =
        """{"type":"$type","startEpochMillis":$start,"durationMillis":$duration$extra}"""

    private fun json(sessions: String = jsonSession(), version: Int = 1, format: String = EXPORT_FORMAT_ID, extra: String = "") =
        """{"format":"$format","version":$version$extra,"sessions":[$sessions]}"""

    @Test
    fun `a minimal json export parses, deriving the day from the start time`() {
        val s = success(json()).sessions.single()
        assertEquals("POMODORO", s.type)
        assertEquals(t0, s.startEpochMillis)
        assertEquals(1_500_000L, s.durationMillis)
        assertEquals(LocalDate.of(2026, 9, 20).toEpochDay(), s.dateEpochDay)
        assertFalse(s.completedNaturally)
    }

    @Test
    fun `json keeps an explicit stored day and all optional fields`() {
        val extra = ""","date":"2026-09-20","label":"Limits","completedNaturally":true,"subject":"Math","task":"Calc","taskSubject":"Math" """
        val s = success(json(jsonSession(extra = extra))).sessions.single()
        assertEquals("Limits", s.label)
        assertTrue(s.completedNaturally)
        assertEquals("Math", s.subjectName)
        assertEquals("Calc", s.taskTitle)
        assertEquals("Math", s.taskSubjectName)
    }

    @Test
    fun `json subjects and tasks are parsed with their details`() {
        val extra = ""","subjects":[{"name":"Math","icon":"📐","color":-16776961}],""" +
            """"tasks":[{"title":"Calc","chapter":"Ch1","subject":"Math","priority":"high","deadline":"2026-10-01","estimatedMinutes":45,"completed":true,"completedAtEpochMillis":5,"createdAtEpochMillis":4}]"""
        val parsed = success(json(extra = extra))
        assertEquals(ImportedSubject("Math", "📐", -16776961), parsed.subjects.single())
        val t = parsed.tasks.single()
        assertEquals("Calc", t.title)
        assertEquals("HIGH", t.priority)
        assertEquals(LocalDate.of(2026, 10, 1).toEpochDay(), t.deadlineEpochDay)
        assertEquals(45, t.estimatedDurationMinutes)
        assertTrue(t.completed)
        assertEquals(5L, t.completedAtEpochMillis)
    }

    @Test
    fun `a task that is not completed drops any completion time, and a missing priority defaults`() {
        val extra = ""","tasks":[{"title":"Calc","completed":false,"completedAtEpochMillis":99}]"""
        val t = success(json(extra = extra)).tasks.single()
        assertNull(t.completedAtEpochMillis)
        assertEquals(TaskPriority.MEDIUM.name, t.priority)
    }

    @Test
    fun `invalid json entries are rejected individually with their position`() {
        val sessions = listOf(
            jsonSession(),
            jsonSession(type = "NAPPING"),
            """{"type":"POMODORO","durationMillis":1500000}""",            // no start
            """{"type":"POMODORO","startEpochMillis":$t0}""",              // no duration
            jsonSession(duration = 1_000L),                                 // too short
            jsonSession(extra = ""","date":"2026-01-01" """),              // day doesn't match the start
            jsonSession(extra = ""","date":"garbage" """),
            "42"                                                            // not an object
        ).joinToString(",")
        val parsed = success(json(sessions))
        assertEquals(1, parsed.sessions.size)
        assertEquals(7, parsed.rejected.size)
        assertEquals("Session #2", parsed.rejected[0].where)
        assertTrue(parsed.rejected[0].reason.contains("Unknown session type"))
        assertTrue(parsed.rejected.any { it.reason.contains("Date doesn't match") })
    }

    @Test
    fun `invalid subjects and tasks are rejected without failing the file`() {
        val extra = ""","subjects":[{"icon":"x"},{"name":"Ok"}],"tasks":[{"priority":"HIGH"},{"title":"T","priority":"URGENT"},{"title":"Fine"}]"""
        val parsed = success(json(extra = extra))
        assertEquals(listOf("Ok"), parsed.subjects.map { it.name })
        assertEquals(listOf("Fine"), parsed.tasks.map { it.title })
        assertEquals(listOf("Subject #1", "Task #1", "Task #2"), parsed.rejected.map { it.where })
        assertTrue(parsed.rejected[2].reason.contains("Unknown priority"))
    }

    @Test
    fun `whole-file json problems fail cleanly`() {
        assertTrue(failure("{ this is not json").contains("valid JSON"))
        assertTrue(failure("[1,2,3]").contains("isn't a StudySpace export"))
        assertTrue(failure("""{"hello":"world"}""").contains("isn't a StudySpace export"))
        assertTrue(failure(json(version = 2)).contains("newer version"))
        assertTrue(failure("""{"format":"$EXPORT_FORMAT_ID","sessions":[]}""").contains("version"))
        assertTrue(failure("""{"format":"$EXPORT_FORMAT_ID","version":1}""").contains("sessions"))
    }

    @Test
    fun `an export with no sessions is valid and empty`() {
        val parsed = success(json(sessions = ""))
        assertTrue(parsed.sessions.isEmpty())
        assertFalse(planImport(parsed, emptyList(), emptyList(), emptyList()).hasChanges)
    }

    // ======================= planning =======================

    private fun subject(id: Long, name: String) = SubjectEntity(id = id, name = name, icon = "📚", colorArgb = 0, createdAtEpochMillis = 0L)

    private fun task(id: Long, title: String, subjectId: Long? = null) =
        TaskEntity(id = id, title = title, subjectId = subjectId, priority = TaskPriority.MEDIUM.name, createdAtEpochMillis = 0L)

    private fun existingSession(start: Long, duration: Long = 1_500_000L, type: String = "POMODORO") = StudySessionEntity(
        type = type, label = "x", startEpochMillis = start, durationMillis = duration,
        completedNaturally = true, dateEpochDay = 0L
    )

    private fun imported(
        start: Long = t0,
        duration: Long = 1_500_000L,
        subject: String? = null,
        task: String? = null,
        taskSubject: String? = subject
    ) = ImportedSession("POMODORO", "x", start, duration, true, 0L, subject, task, taskSubject)

    private fun parsedOf(
        sessions: List<ImportedSession> = emptyList(),
        subjects: List<ImportedSubject> = emptyList(),
        tasks: List<ImportedTask> = emptyList(),
        rejected: List<RejectedRow> = emptyList()
    ) = ParsedImport(TransferFormat.JSON, subjects, tasks, sessions, rejected)

    private fun importedTask(title: String, subject: String? = null) = ImportedTask(
        title, null, null, subject, TaskPriority.MEDIUM.name, null, null, false, null, null
    )

    @Test
    fun `new sessions into an empty app are all added`() {
        val plan = planImport(parsedOf(listOf(imported(t0), imported(t0 + 3_600_000L))), emptyList(), emptyList(), emptyList())
        assertEquals(2, plan.newSessions.size)
        assertEquals(0, plan.duplicateSessions)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun `a session already in the app is skipped as a duplicate`() {
        val plan = planImport(parsedOf(listOf(imported(t0))), emptyList(), emptyList(), listOf(existingSession(t0)))
        assertEquals(0, plan.newSessions.size)
        assertEquals(1, plan.duplicateSessions)
        assertFalse(plan.hasChanges)
    }

    @Test
    fun `duplicates are recognised at second precision, so millisecond noise does not defeat them`() {
        val plan = planImport(
            parsedOf(listOf(imported(t0, duration = 1_500_000L))),
            emptyList(), emptyList(),
            listOf(existingSession(t0 + 437L, duration = 1_500_812L))
        )
        assertEquals(1, plan.duplicateSessions)
    }

    @Test
    fun `same start and duration but a different type is a different session`() {
        val plan = planImport(parsedOf(listOf(imported(t0))), emptyList(), emptyList(), listOf(existingSession(t0, type = "SELF_STUDY")))
        assertEquals(1, plan.newSessions.size)
    }

    @Test
    fun `a duplicate that appears twice inside the file is only added once`() {
        val plan = planImport(parsedOf(listOf(imported(t0), imported(t0))), emptyList(), emptyList(), emptyList())
        assertEquals(1, plan.newSessions.size)
        assertEquals(1, plan.duplicateSessions)
    }

    @Test
    fun `an existing subject matches by name ignoring case and is not recreated`() {
        val plan = planImport(parsedOf(listOf(imported(subject = "  PHYSICS "))), listOf(subject(1, "physics")), emptyList(), emptyList())
        assertTrue(plan.newSubjects.isEmpty())
    }

    @Test
    fun `a new subject uses its declared icon and colour, or defaults when only referenced`() {
        val plan = planImport(
            parsedOf(
                sessions = listOf(imported(subject = "Math"), imported(t0 + 1_000_000L, subject = "Chem")),
                subjects = listOf(ImportedSubject("Math", "📐", 0x123456))
            ),
            emptyList(), emptyList(), emptyList()
        )
        assertEquals(listOf("Math", "Chem"), plan.newSubjects.map { it.name })
        assertEquals("📐", plan.newSubjects[0].icon)
        assertEquals(0x123456, plan.newSubjects[0].colorArgb)
        assertEquals("📚", plan.newSubjects[1].icon)
    }

    @Test
    fun `a subject named by several rows is created only once`() {
        val plan = planImport(parsedOf(listOf(imported(t0, subject = "Math"), imported(t0 + 5_000_000L, subject = "math"))), emptyList(), emptyList(), emptyList())
        assertEquals(1, plan.newSubjects.size)
    }

    @Test
    fun `tasks that already exist are not duplicated, new ones are added`() {
        val plan = planImport(
            parsedOf(tasks = listOf(importedTask("Calculus", "Math"), importedTask("Algebra", "Math"))),
            listOf(subject(1, "Math")), listOf(task(5, "calculus", subjectId = 1)), emptyList()
        )
        assertEquals(listOf("Algebra"), plan.newTasks.map { it.title })
    }

    @Test
    fun `the same task title under different subjects are different tasks`() {
        val plan = planImport(
            parsedOf(tasks = listOf(importedTask("Revise", "Math"), importedTask("Revise", "Physics"))),
            emptyList(), emptyList(), emptyList()
        )
        assertEquals(2, plan.newTasks.size)
    }

    @Test
    fun `a session's task is linked when it exists in the app, and counted as unlinked when it does not`() {
        val plan = planImport(
            parsedOf(listOf(imported(t0, subject = "Math", task = "Calculus"), imported(t0 + 5_000_000L, subject = "Math", task = "Ghost"))),
            listOf(subject(1, "Math")), listOf(task(5, "Calculus", subjectId = 1)), emptyList()
        )
        assertEquals(2, plan.newSessions.size) // the unlinked one is still imported, not dropped
        assertEquals(1, plan.sessionsWithoutTaskLink)
    }

    @Test
    fun `a task created by the same import counts as linkable`() {
        val plan = planImport(
            parsedOf(sessions = listOf(imported(subject = "Math", task = "Calculus")), tasks = listOf(importedTask("Calculus", "Math"))),
            emptyList(), emptyList(), emptyList()
        )
        assertEquals(0, plan.sessionsWithoutTaskLink)
    }

    @Test
    fun `task lookup falls back to a unique title when the subject differs, but not when ambiguous`() {
        val existing = listOf(task(1, "Calculus", subjectId = 1))
        val subjects = listOf(subject(1, "Math"), subject(2, "Physics"))
        // CSV knows only the session's subject ("Physics"); the task's own subject is Math, but the title is unique.
        val unique = planImport(parsedOf(listOf(imported(subject = "Physics", task = "Calculus"))), subjects, existing, emptyList())
        assertEquals(0, unique.sessionsWithoutTaskLink)

        val ambiguous = planImport(
            parsedOf(listOf(imported(subject = "Chemistry", task = "Read"))),
            subjects + subject(3, "Chemistry"),
            listOf(task(1, "Read", subjectId = 1), task(2, "Read", subjectId = 2)),
            emptyList()
        )
        assertEquals(1, ambiguous.sessionsWithoutTaskLink)
    }

    @Test
    fun `rejected entries are carried through to the plan`() {
        val rejected = listOf(RejectedRow("Row 2", "bad"))
        assertEquals(rejected, planImport(parsedOf(rejected = rejected), emptyList(), emptyList(), emptyList()).rejected)
    }

    // ======================= round trips (export -> import) =======================

    private val math = subject(1, "Mathematics")
    private val calculus = task(5, "Calculus", subjectId = 1)

    private fun realSession(start: Long, durationMillis: Long, subjectId: Long? = 1, taskId: Long? = 5) = StudySessionEntity(
        type = SessionType.POMODORO.name, label = "Calculus", startEpochMillis = start, durationMillis = durationMillis,
        completedNaturally = true,
        dateEpochDay = Instant.ofEpochMilli(start).atZone(utc).toLocalDate().toEpochDay(),
        subjectId = subjectId, taskId = taskId
    )

    private val sessions = listOf(
        realSession(utcMillis(2026, 9, 18, 9, 0) + 123L, 1_500_456L),
        realSession(utcMillis(2026, 9, 19, 14, 30), 3_600_000L, subjectId = null, taskId = null),
        realSession(utcMillis(2026, 9, 20, 20, 15, 30), 45_000L)
    )
    private val bundle = ExportBundle(listOf(math), listOf(calculus), sessions)

    @Test
    fun `json round trip preserves every session exactly`() {
        val parsed = success(buildJsonExport(bundle, now, utc))
        assertTrue(parsed.rejected.isEmpty())
        assertEquals(sessions.map { it.startEpochMillis }.sorted(), parsed.sessions.map { it.startEpochMillis })
        assertEquals(sessions.sortedBy { it.startEpochMillis }.map { it.durationMillis }, parsed.sessions.map { it.durationMillis })
        assertEquals(sessions.sortedBy { it.startEpochMillis }.map { it.dateEpochDay }, parsed.sessions.map { it.dateEpochDay })
    }

    @Test
    fun `re-importing your own json export into the same data adds nothing`() {
        val parsed = success(buildJsonExport(bundle, now, utc))
        val plan = planImport(parsed, listOf(math), listOf(calculus), sessions)
        assertEquals(3, plan.duplicateSessions)
        assertTrue(plan.newSessions.isEmpty())
        assertTrue(plan.newSubjects.isEmpty())
        assertTrue(plan.newTasks.isEmpty())
        assertFalse(plan.hasChanges)
    }

    @Test
    fun `re-importing your own csv export adds nothing, even with sub-second precision in the data`() {
        val parsed = success(buildCsvExport(bundle, utc))
        assertTrue(parsed.rejected.isEmpty())
        val plan = planImport(parsed, listOf(math), listOf(calculus), sessions)
        assertEquals(3, plan.duplicateSessions)
        assertFalse(plan.hasChanges)
    }

    @Test
    fun `importing a json export into a fresh app recreates subjects, tasks and links`() {
        val parsed = success(buildJsonExport(bundle, now, utc))
        val plan = planImport(parsed, emptyList(), emptyList(), emptyList())
        assertEquals(3, plan.newSessions.size)
        assertEquals(listOf("Mathematics"), plan.newSubjects.map { it.name })
        assertEquals(listOf("Calculus"), plan.newTasks.map { it.title })
        assertEquals(0, plan.sessionsWithoutTaskLink)
    }

    @Test
    fun `importing a csv export into a fresh app creates subjects and imports unlinked task sessions`() {
        val parsed = success(buildCsvExport(bundle, utc))
        val plan = planImport(parsed, emptyList(), emptyList(), emptyList())
        assertEquals(3, plan.newSessions.size)
        assertEquals(listOf("Mathematics"), plan.newSubjects.map { it.name })
        assertTrue(plan.newTasks.isEmpty()) // CSV has no task details to recreate tasks from
        // Two of the three sessions name the task; with no task to link to they're imported without one, not dropped.
        assertEquals(2, plan.sessionsWithoutTaskLink)
    }

    @Test
    fun `text that needed the formula guard survives a csv round trip`() {
        val evil = subject(2, "=cmd|' /C calc'!A0")
        val b = ExportBundle(listOf(evil), emptyList(), listOf(realSession(utcMillis(2026, 9, 18, 9), 600_000L, subjectId = 2, taskId = null)))
        val parsed = success(buildCsvExport(b, utc))
        assertEquals("=cmd|' /C calc'!A0", parsed.sessions.single().subjectName)
    }
}
