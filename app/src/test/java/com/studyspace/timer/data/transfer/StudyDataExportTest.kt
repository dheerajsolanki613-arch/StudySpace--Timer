package com.studyspace.timer.data.transfer

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StudyDataExportTest {
    private val utc = ZoneId.of("UTC")

    private fun utcMillis(y: Int, m: Int, d: Int, h: Int, min: Int, sec: Int = 0): Long =
        java.time.LocalDateTime.of(y, m, d, h, min, sec).atZone(utc).toInstant().toEpochMilli()

    private val math = SubjectEntity(id = 1, name = "Mathematics", icon = "📐", colorArgb = 0x112233, createdAtEpochMillis = 0L)
    private val calculus = TaskEntity(
        id = 5, title = "Calculus", chapter = "Ch 3", topic = "Limits", subjectId = 1,
        priority = TaskPriority.HIGH.name, deadlineEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
        estimatedDurationMinutes = 90, completed = true, createdAtEpochMillis = 1_000L, completedAtEpochMillis = 2_000L
    )

    private fun session(
        start: Long,
        durationMillis: Long = 5_400_000L,
        type: SessionType = SessionType.POMODORO,
        label: String = "Calculus",
        completed: Boolean = true,
        subjectId: Long? = 1,
        taskId: Long? = 5
    ) = StudySessionEntity(
        type = type.name,
        label = label,
        startEpochMillis = start,
        durationMillis = durationMillis,
        completedNaturally = completed,
        dateEpochDay = java.time.Instant.ofEpochMilli(start).atZone(utc).toLocalDate().toEpochDay(),
        subjectId = subjectId,
        taskId = taskId
    )

    private fun bundle(vararg sessions: StudySessionEntity) =
        ExportBundle(listOf(math), listOf(calculus), sessions.toList())

    private val header = "Date,Start time,End time,Duration (seconds),Subject,Task,Session type,Label,Completed"

    // ---------- CSV ----------

    @Test
    fun `csv starts with a BOM, has the header, and uses CRLF`() {
        val csv = buildCsvExport(bundle(), utc)
        assertEquals("\uFEFF$header\r\n", csv)
    }

    @Test
    fun `csv row carries date, times, duration, subject, task, type, label and completion`() {
        val csv = buildCsvExport(bundle(session(utcMillis(2026, 9, 21, 16, 0))), utc)
        val row = csv.removePrefix("\uFEFF").split("\r\n")[1]
        assertEquals("2026-09-21,16:00:00,17:30:00,5400,Mathematics,Calculus,POMODORO,Calculus,yes", row)
    }

    @Test
    fun `csv quotes a label containing a comma`() {
        val csv = buildCsvExport(bundle(session(utcMillis(2026, 9, 21, 16, 0), label = "Calculus, ch. 3")), utc)
        assertTrue(csv.contains(",\"Calculus, ch. 3\","))
    }

    @Test
    fun `csv leaves subject and task blank when the session has neither, and marks not-completed as no`() {
        val csv = buildCsvExport(
            bundle(session(utcMillis(2026, 9, 21, 9, 0), subjectId = null, taskId = null, completed = false)),
            utc
        )
        assertEquals("2026-09-21,09:00:00,10:30:00,5400,,,POMODORO,Calculus,no", csv.removePrefix("\uFEFF").split("\r\n")[1])
    }

    @Test
    fun `csv guards text that a spreadsheet would run as a formula`() {
        val evil = SubjectEntity(id = 2, name = "=HYPERLINK(\"http://x\")", icon = "📚", colorArgb = 0, createdAtEpochMillis = 0L)
        val csv = buildCsvExport(
            ExportBundle(listOf(evil), emptyList(), listOf(session(utcMillis(2026, 9, 21, 9, 0), subjectId = 2, taskId = null, label = "+cmd"))),
            utc
        )
        val row = csv.removePrefix("\uFEFF").split("\r\n")[1]
        assertTrue("subject must be guarded", row.contains("\"'=HYPERLINK(\"\"http://x\"\")\""))
        assertTrue("label must be guarded", row.contains(",'+cmd,"))
    }

    @Test
    fun `csv lists sessions oldest first regardless of input order`() {
        val csv = buildCsvExport(
            bundle(session(utcMillis(2026, 9, 22, 9, 0)), session(utcMillis(2026, 9, 20, 9, 0))),
            utc
        )
        val rows = csv.removePrefix("\uFEFF").trim().split("\r\n")
        assertTrue(rows[1].startsWith("2026-09-20"))
        assertTrue(rows[2].startsWith("2026-09-22"))
    }

    @Test
    fun `csv works out the day and times in the given zone`() {
        val kolkata = ZoneId.of("Asia/Kolkata") // UTC+05:30
        val csv = buildCsvExport(bundle(session(utcMillis(2026, 9, 21, 22, 0))), kolkata)
        // 22:00 UTC is 03:30 the next day in Kolkata.
        assertTrue(csv.removePrefix("\uFEFF").split("\r\n")[1].startsWith("2026-09-22,03:30:00,05:00:00,"))
    }

    // ---------- JSON ----------

    @Test
    fun `json has the format header, subjects, tasks and sessions`() {
        val json = JSONObject(buildJsonExport(bundle(session(utcMillis(2026, 9, 21, 16, 0))), nowEpochMillis = 123L, zone = utc))
        assertEquals(EXPORT_FORMAT_ID, json.getString("format"))
        assertEquals(EXPORT_FORMAT_VERSION, json.getInt("version"))
        assertEquals(123L, json.getLong("exportedAtEpochMillis"))
        assertEquals("UTC", json.getString("timeZone"))
        assertEquals(1, json.getJSONArray("subjects").length())
        assertEquals(1, json.getJSONArray("tasks").length())
        assertEquals(1, json.getJSONArray("sessions").length())
    }

    @Test
    fun `json session keeps exact millis and resolves subject and task names`() {
        val start = utcMillis(2026, 9, 21, 16, 0) + 123L
        val json = JSONObject(buildJsonExport(bundle(session(start, durationMillis = 5_400_500L)), 0L, utc))
        val s = json.getJSONArray("sessions").getJSONObject(0)
        assertEquals(start, s.getLong("startEpochMillis"))
        assertEquals(5_400_500L, s.getLong("durationMillis"))
        assertEquals("2026-09-21", s.getString("date"))
        assertEquals("2026-09-21T16:00:00", s.getString("start"))
        assertEquals("POMODORO", s.getString("type"))
        assertEquals("Mathematics", s.getString("subject"))
        assertEquals("Calculus", s.getString("task"))
        assertEquals("Mathematics", s.getString("taskSubject"))
        assertTrue(s.getBoolean("completedNaturally"))
    }

    @Test
    fun `json omits subject and task keys when a session has none`() {
        val json = JSONObject(buildJsonExport(bundle(session(utcMillis(2026, 9, 21, 9, 0), subjectId = null, taskId = null)), 0L, utc))
        val s = json.getJSONArray("sessions").getJSONObject(0)
        assertFalse(s.has("subject"))
        assertFalse(s.has("task"))
        assertFalse(s.has("taskSubject"))
    }

    @Test
    fun `json task includes its details and optional fields only when present`() {
        val bare = TaskEntity(id = 6, title = "Read", priority = TaskPriority.LOW.name, createdAtEpochMillis = 7L)
        val json = JSONObject(buildJsonExport(ExportBundle(listOf(math), listOf(calculus, bare), emptyList()), 0L, utc))
        val tasks = json.getJSONArray("tasks")
        val full = tasks.getJSONObject(0)
        assertEquals("Calculus", full.getString("title"))
        assertEquals("Ch 3", full.getString("chapter"))
        assertEquals("Limits", full.getString("topic"))
        assertEquals("Mathematics", full.getString("subject"))
        assertEquals("HIGH", full.getString("priority"))
        assertEquals("2026-10-01", full.getString("deadline"))
        assertEquals(90, full.getInt("estimatedMinutes"))
        assertTrue(full.getBoolean("completed"))
        assertEquals(2_000L, full.getLong("completedAtEpochMillis"))
        val minimal = tasks.getJSONObject(1)
        assertFalse(minimal.has("chapter"))
        assertFalse(minimal.has("deadline"))
        assertFalse(minimal.has("completedAtEpochMillis"))
    }

    @Test
    fun `json export of empty data is still a valid export with empty lists`() {
        val json = JSONObject(buildJsonExport(ExportBundle(emptyList(), emptyList(), emptyList()), 0L, utc))
        assertEquals(0, json.getJSONArray("sessions").length())
        assertEquals(0, json.getJSONArray("subjects").length())
    }

    // ---------- file name ----------

    @Test
    fun `export file names carry the date and extension`() {
        assertEquals("studyspace-sessions-2026-09-21.csv", exportFileName(TransferFormat.CSV, LocalDate.of(2026, 9, 21)))
        assertEquals("studyspace-sessions-2026-09-21.json", exportFileName(TransferFormat.JSON, LocalDate.of(2026, 9, 21)))
    }
}
