package com.studyspace.timer.data.transfer

import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Phase 13 — building export files. Pure functions over plain entity lists
 * (the repository does the database reads), so the exact bytes of each format
 * are pinned down by unit tests.
 *
 * Two formats, for two jobs:
 *  - **CSV** — one row per session, for opening in a spreadsheet. Columns:
 *    Date, Start time, End time, Duration (seconds), Subject, Task, Session
 *    type, Label, Completed. Human-readable local times; times are only
 *    second-precise and the day is worked out in the exporting device's
 *    time zone.
 *  - **JSON** — the full-fidelity form: sessions with exact millisecond
 *    times and their original day, plus the subjects and tasks. Best for
 *    moving data between devices or keeping a faithful copy.
 *
 * Both are *study history*, per the spec. Planned sessions, goals,
 * achievements and settings are not in these files — full-app backup and
 * restore is Phase 14's job.
 */

enum class TransferFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json")
}

const val EXPORT_FORMAT_ID = "studyspace-export"
const val EXPORT_FORMAT_VERSION = 1

object CsvColumns {
    const val DATE = "Date"
    const val START = "Start time"
    const val END = "End time"
    const val DURATION = "Duration (seconds)"
    const val SUBJECT = "Subject"
    const val TASK = "Task"
    const val TYPE = "Session type"
    const val LABEL = "Label"
    const val COMPLETED = "Completed"

    val ALL = listOf(DATE, START, END, DURATION, SUBJECT, TASK, TYPE, LABEL, COMPLETED)
}

/** Everything an export needs, read from the database in one go. */
data class ExportBundle(
    val subjects: List<SubjectEntity>,
    val tasks: List<TaskEntity>,
    val sessions: List<StudySessionEntity>
)

fun exportFileName(format: TransferFormat, date: LocalDate): String =
    "studyspace-sessions-$date.${format.extension}"

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * CSV export. Starts with a UTF-8 byte-order mark so Excel reads accented
 * subject names correctly (other tools ignore it, and the importer strips
 * it); rows end in CRLF per RFC 4180. Sessions are oldest-first. User-typed
 * text goes through [csvSafeText] so a subject called `=SUM(...)` can't run
 * as a formula when the file is opened in a spreadsheet.
 */
fun buildCsvExport(bundle: ExportBundle, zone: ZoneId = ZoneId.systemDefault()): String {
    val subjectNames = bundle.subjects.associate { it.id to it.name }
    val taskTitles = bundle.tasks.associate { it.id to it.title }

    val out = StringBuilder("\uFEFF")
    out.append(CsvColumns.ALL.joinToString(",") { csvEscape(it) }).append("\r\n")

    bundle.sessions.sortedBy { it.startEpochMillis }.forEach { session ->
        val start = Instant.ofEpochMilli(session.startEpochMillis).atZone(zone)
        val end = start.plus(Duration.ofMillis(session.durationMillis))
        val fields = listOf(
            start.toLocalDate().toString(),
            TIME_FORMAT.format(start),
            TIME_FORMAT.format(end),
            (session.durationMillis / 1000).toString(),
            csvSafeText(session.subjectId?.let { subjectNames[it] }.orEmpty()),
            csvSafeText(session.taskId?.let { taskTitles[it] }.orEmpty()),
            session.type,
            csvSafeText(session.label),
            if (session.completedNaturally) "yes" else "no"
        )
        out.append(fields.joinToString(",") { csvEscape(it) }).append("\r\n")
    }
    return out.toString()
}

/** JSON export — see the class comment above for what it contains and how it differs from CSV. */
fun buildJsonExport(
    bundle: ExportBundle,
    nowEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val subjectNames = bundle.subjects.associate { it.id to it.name }
    val tasksById = bundle.tasks.associateBy { it.id }

    val subjects = JSONArray()
    bundle.subjects.forEach { subject ->
        subjects.put(
            JSONObject()
                .put("name", subject.name)
                .put("icon", subject.icon)
                .put("color", subject.colorArgb)
        )
    }

    val tasks = JSONArray()
    bundle.tasks.forEach { task ->
        tasks.put(
            JSONObject()
                .put("title", task.title)
                .putOpt("chapter", task.chapter)
                .putOpt("topic", task.topic)
                .putOpt("subject", task.subjectId?.let { subjectNames[it] })
                .put("priority", task.priority)
                .putOpt("deadline", task.deadlineEpochDay?.let { LocalDate.ofEpochDay(it).toString() })
                .putOpt("estimatedMinutes", task.estimatedDurationMinutes)
                .put("completed", task.completed)
                .putOpt("completedAtEpochMillis", task.completedAtEpochMillis)
                .put("createdAtEpochMillis", task.createdAtEpochMillis)
        )
    }

    val sessions = JSONArray()
    bundle.sessions.sortedBy { it.startEpochMillis }.forEach { session ->
        val start = Instant.ofEpochMilli(session.startEpochMillis).atZone(zone).toLocalDateTime().withNano(0)
        val end = start.plus(Duration.ofMillis(session.durationMillis))
        val task = session.taskId?.let { tasksById[it] }
        sessions.put(
            JSONObject()
                .put("type", session.type)
                .put("label", session.label)
                .put("date", LocalDate.ofEpochDay(session.dateEpochDay).toString())
                // Readable local times for humans; the importer ignores these and uses the exact millis below.
                .put("start", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(start))
                .put("end", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(end))
                .put("startEpochMillis", session.startEpochMillis)
                .put("durationMillis", session.durationMillis)
                .put("completedNaturally", session.completedNaturally)
                .putOpt("subject", session.subjectId?.let { subjectNames[it] })
                .putOpt("task", task?.title)
                .putOpt("taskSubject", task?.subjectId?.let { subjectNames[it] })
        )
    }

    return JSONObject()
        .put("format", EXPORT_FORMAT_ID)
        .put("version", EXPORT_FORMAT_VERSION)
        .put("app", "StudySpace Timer")
        .put("exportedAtEpochMillis", nowEpochMillis)
        .put("timeZone", zone.id)
        .put("subjects", subjects)
        .put("tasks", tasks)
        .put("sessions", sessions)
        .toString(2)
}
