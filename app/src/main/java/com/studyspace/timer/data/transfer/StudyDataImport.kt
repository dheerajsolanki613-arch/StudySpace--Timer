package com.studyspace.timer.data.transfer

import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import com.studyspace.timer.data.repository.SessionRepository
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * Phase 13 — importing a StudySpace export (CSV or JSON), in two pure steps
 * so each can be unit-tested and neither touches the database:
 *
 *  1. [parseStudyDataFile] — read + **validate** the text into a
 *     [ParsedImport]. Nothing is trusted: every row is checked, and a row
 *     that fails is *reported* in [ParsedImport.rejected] with its position
 *     and reason rather than silently dropped or, worse, saved.
 *  2. [planImport] — compare the valid rows against what's already in the
 *     app and decide exactly what would be added ([ImportPlan]), so the
 *     screen can show it and ask for confirmation *before* anything is
 *     written.
 *
 * **Import only ever adds.** It never edits, replaces or deletes an existing
 * session, subject or task — a re-import of your own export adds nothing
 * (every session is recognised as a duplicate). That is how "never overwrite
 * existing data without confirmation" is met: there is no path here that
 * overwrites, and even the additive write only happens after the user
 * confirms the previewed plan. Replacing everything with a saved copy is a
 * different operation and belongs to Phase 14's Backup & Restore.
 */

data class ImportedSubject(val name: String, val icon: String?, val colorArgb: Int?)

data class ImportedTask(
    val title: String,
    val chapter: String?,
    val topic: String?,
    val subjectName: String?,
    val priority: String,
    val deadlineEpochDay: Long?,
    val estimatedDurationMinutes: Int?,
    val completed: Boolean,
    val completedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long?
)

/**
 * @param type a [SessionType] name (already validated).
 * @param taskSubjectName the *task's* subject, which is what identifies the
 *   task together with [taskTitle]. JSON records it exactly; CSV has only the
 *   session's subject, so it stands in (see [TaskKeyResolver] for the
 *   fallback when that isn't exact).
 */
data class ImportedSession(
    val type: String,
    val label: String,
    val startEpochMillis: Long,
    val durationMillis: Long,
    val completedNaturally: Boolean,
    val dateEpochDay: Long,
    val subjectName: String?,
    val taskTitle: String?,
    val taskSubjectName: String?
)

/** A row/entry that failed validation. [where] is human-readable ("Row 7", "Session #3"). */
data class RejectedRow(val where: String, val reason: String)

data class ParsedImport(
    val format: TransferFormat,
    val subjects: List<ImportedSubject>,
    val tasks: List<ImportedTask>,
    val sessions: List<ImportedSession>,
    val rejected: List<RejectedRow>
)

sealed interface ParseResult {
    data class Success(val parsed: ParsedImport) : ParseResult

    /** The file as a whole can't be imported (wrong kind of file, damaged, too new); nothing was read from it. */
    data class Failure(val message: String) : ParseResult
}

/** Sanity bounds. Deliberately generous — they exist to reject garbage, not to police real data. */
object ImportLimits {
    const val MAX_SESSION_MILLIS = 24L * 60 * 60 * 1000
    const val FUTURE_TOLERANCE_MILLIS = 24L * 60 * 60 * 1000
    const val EARLIEST_START_MILLIS = 946_684_800_000L // 2000-01-01T00:00:00Z
    const val MAX_NAME_LENGTH = 100
    const val MAX_TITLE_LENGTH = 200
    const val MAX_LABEL_LENGTH = 100
    const val MAX_IMPORT_BYTES = 10 * 1024 * 1024
}

private const val DEFAULT_IMPORT_ICON = "📚"
private const val DEFAULT_IMPORT_COLOR = 0xFF7C4DFF.toInt()

/** Internal control flow for "this one row is bad" — caught per row, never escapes the parser. */
private class RowRejected(val reason: String) : Exception(reason)

private fun reject(reason: String): Nothing = throw RowRejected(reason)

// ---------------------------------------------------------------------------------------------
// Parsing
// ---------------------------------------------------------------------------------------------

/**
 * Detects the format from the *content* (a leading `{` is JSON, anything else
 * is treated as CSV) rather than the file name or MIME type — phones report
 * CSVs as `text/csv`, `application/vnd.ms-excel`, `text/plain` or
 * `application/octet-stream` almost at random.
 *
 * @param nowEpochMillis used to reject sessions dated in the future.
 * @param zone interprets the CSV's local Date + Start time (JSON carries exact instants).
 */
fun parseStudyDataFile(
    text: String,
    nowEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): ParseResult {
    val body = text.removePrefix("\uFEFF")
    val trimmed = body.trimStart()
    if (trimmed.isEmpty()) return ParseResult.Failure("The file is empty.")
    return when {
        trimmed.startsWith("{") -> parseJsonExport(trimmed, nowEpochMillis, zone)
        trimmed.startsWith("[") -> ParseResult.Failure("This isn't a StudySpace export file.")
        else -> parseCsvExport(body, nowEpochMillis, zone)
    }
}

private fun normalizeSessionType(raw: String?): String? {
    val key = raw?.trim()?.uppercase()?.replace(' ', '_')?.replace('-', '_') ?: return null
    return SessionType.values().firstOrNull { it.name == key }?.name
}

private fun sessionProblem(startMillis: Long, durationMillis: Long, nowMillis: Long): String? = when {
    durationMillis < SessionRepository.MIN_RECORDABLE_MILLIS -> "Duration is shorter than 10 seconds"
    durationMillis > ImportLimits.MAX_SESSION_MILLIS -> "Duration is longer than 24 hours"
    startMillis < ImportLimits.EARLIEST_START_MILLIS -> "Start time is before the year 2000"
    startMillis > nowMillis + ImportLimits.FUTURE_TOLERANCE_MILLIS -> "Start time is in the future"
    else -> null
}

private fun lengthProblem(value: String?, max: Int, what: String): String? =
    if (value != null && value.length > max) "$what is longer than $max characters" else null

private fun checkSessionText(label: String, subject: String?, task: String?, taskSubject: String?) {
    lengthProblem(label, ImportLimits.MAX_LABEL_LENGTH, "Label")?.let { reject(it) }
    lengthProblem(subject, ImportLimits.MAX_NAME_LENGTH, "Subject name")?.let { reject(it) }
    lengthProblem(taskSubject, ImportLimits.MAX_NAME_LENGTH, "Task's subject name")?.let { reject(it) }
    lengthProblem(task, ImportLimits.MAX_TITLE_LENGTH, "Task title")?.let { reject(it) }
}

private fun defaultLabel(type: String): String = SessionType.valueOf(type).displayLabel

// ---- CSV ------------------------------------------------------------------------------------

private fun parseCsvExport(text: String, now: Long, zone: ZoneId): ParseResult {
    val rows = try {
        parseCsv(text)
    } catch (e: IllegalArgumentException) {
        return ParseResult.Failure("This CSV file is damaged. ${e.message}")
    }
    if (rows.isEmpty()) return ParseResult.Failure("The file is empty.")

    val header = rows[0].map { it.trim().lowercase() }
    val index = HashMap<String, Int>()
    header.forEachIndexed { i, name -> index.putIfAbsent(name, i) }

    val required = listOf(CsvColumns.DATE, CsvColumns.START, CsvColumns.DURATION, CsvColumns.TYPE)
    val missing = required.filter { it.lowercase() !in index }
    if (missing.isNotEmpty()) {
        return ParseResult.Failure(
            "This doesn't look like a StudySpace export. Missing column(s): ${missing.joinToString(", ")}."
        )
    }

    fun cell(row: List<String>, column: String): String? =
        index[column.lowercase()]?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }

    val sessions = mutableListOf<ImportedSession>()
    val rejected = mutableListOf<RejectedRow>()

    for (i in 1 until rows.size) {
        val row = rows[i]
        if (row.all { it.isBlank() }) continue
        val where = "Row ${i + 1}"
        try {
            sessions += parseCsvSession(row, ::cell, now, zone)
        } catch (e: RowRejected) {
            rejected += RejectedRow(where, e.reason)
        }
    }
    return ParseResult.Success(ParsedImport(TransferFormat.CSV, emptyList(), emptyList(), sessions, rejected))
}

private fun parseCsvSession(
    row: List<String>,
    cell: (List<String>, String) -> String?,
    now: Long,
    zone: ZoneId
): ImportedSession {
    val date = cell(row, CsvColumns.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: reject("Invalid or missing date (expected YYYY-MM-DD)")
    val time = cell(row, CsvColumns.START)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        ?: reject("Invalid or missing start time (expected HH:MM:SS)")
    val seconds = cell(row, CsvColumns.DURATION)?.toLongOrNull()
        ?: reject("Invalid or missing duration (expected whole seconds)")
    val typeRaw = cell(row, CsvColumns.TYPE)
    val type = normalizeSessionType(typeRaw) ?: reject("Unknown session type '${typeRaw ?: ""}'")

    val completedRaw = cell(row, CsvColumns.COMPLETED)
    val completed = if (completedRaw == null) false else {
        parseYesNo(completedRaw) ?: reject("Invalid Completed value '$completedRaw' (expected yes or no)")
    }

    val subject = cell(row, CsvColumns.SUBJECT)?.let(::csvUnsafeText)
    val task = cell(row, CsvColumns.TASK)?.let(::csvUnsafeText)
    val label = cell(row, CsvColumns.LABEL)?.let(::csvUnsafeText) ?: defaultLabel(type)

    // Clamped before the ×1000 so an absurd value can't overflow into something that looks valid.
    val durationMillis = seconds.coerceIn(-1L, 200_000L) * 1000L
    val start = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    sessionProblem(start, durationMillis, now)?.let { reject(it) }
    checkSessionText(label, subject, task, subject)

    return ImportedSession(
        type = type,
        label = label,
        startEpochMillis = start,
        durationMillis = durationMillis,
        completedNaturally = completed,
        dateEpochDay = date.toEpochDay(),
        subjectName = subject,
        taskTitle = task,
        // CSV only has the session's subject; it stands in for the task's (TaskKeyResolver copes if it isn't exact).
        taskSubjectName = subject
    )
}

private fun parseYesNo(raw: String): Boolean? = when (raw.trim().lowercase()) {
    "yes", "y", "true", "1" -> true
    "no", "n", "false", "0" -> false
    else -> null
}

// ---- JSON -----------------------------------------------------------------------------------

private fun JSONObject.stringOrNull(key: String): String? =
    if (isNull(key)) null else opt(key) as? String

private fun JSONObject.longOrNull(key: String): Long? =
    if (isNull(key)) null else (opt(key) as? Number)?.toLong()

private fun JSONObject.boolOrNull(key: String): Boolean? =
    if (isNull(key)) null else opt(key) as? Boolean

private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * `internal`, not `private`: [StudyDataBackup]'s restore path nests a full
 * `studyspace-export`-shaped object (subjects/tasks/sessions) inside its own
 * `studyspace-backup` file and reuses this exact, already-tested parser for
 * that section rather than duplicating subject/task/session validation.
 */
internal fun parseJsonExport(text: String, now: Long, zone: ZoneId): ParseResult {
    val root = try {
        JSONObject(text)
    } catch (e: JSONException) {
        return ParseResult.Failure("This file isn't valid JSON, so it can't be a StudySpace export.")
    }
    if (root.optString("format") == BACKUP_FORMAT_ID) {
        return ParseResult.Failure(
            "This is a StudySpace backup file, not an export. Use \"Restore backup\" instead — it replaces all your data, so it lives in its own confirmation flow."
        )
    }
    if (root.optString("format") != EXPORT_FORMAT_ID) {
        return ParseResult.Failure("This isn't a StudySpace export file.")
    }
    val version = root.optInt("version", -1)
    if (version < 1) return ParseResult.Failure("This export file has no valid format version.")
    if (version > EXPORT_FORMAT_VERSION) {
        return ParseResult.Failure(
            "This file was made by a newer version of StudySpace (format version $version). Update the app to import it."
        )
    }
    val sessionArray = root.optJSONArray("sessions")
        ?: return ParseResult.Failure("This export file has no sessions list.")

    val rejected = mutableListOf<RejectedRow>()

    val subjects = mutableListOf<ImportedSubject>()
    root.optJSONArray("subjects")?.let { array ->
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            val name = obj?.stringOrNull("name").trimmedOrNull()
            when {
                name == null -> rejected += RejectedRow("Subject #${i + 1}", "Missing name")
                name.length > ImportLimits.MAX_NAME_LENGTH ->
                    rejected += RejectedRow("Subject #${i + 1}", "Name is longer than ${ImportLimits.MAX_NAME_LENGTH} characters")
                else -> subjects += ImportedSubject(
                    name = name,
                    icon = obj?.stringOrNull("icon").trimmedOrNull(),
                    colorArgb = obj?.longOrNull("color")?.toInt()
                )
            }
        }
    }

    val tasks = mutableListOf<ImportedTask>()
    root.optJSONArray("tasks")?.let { array ->
        for (i in 0 until array.length()) {
            try {
                tasks += parseJsonTask(array.optJSONObject(i))
            } catch (e: RowRejected) {
                rejected += RejectedRow("Task #${i + 1}", e.reason)
            }
        }
    }

    val sessions = mutableListOf<ImportedSession>()
    for (i in 0 until sessionArray.length()) {
        try {
            sessions += parseJsonSession(sessionArray.optJSONObject(i), now, zone)
        } catch (e: RowRejected) {
            rejected += RejectedRow("Session #${i + 1}", e.reason)
        }
    }
    return ParseResult.Success(ParsedImport(TransferFormat.JSON, subjects, tasks, sessions, rejected))
}

private fun parseJsonSession(obj: JSONObject?, now: Long, zone: ZoneId): ImportedSession {
    if (obj == null) reject("Not a valid entry")
    val typeRaw = obj.stringOrNull("type")
    val type = normalizeSessionType(typeRaw) ?: reject("Unknown session type '${typeRaw ?: ""}'")
    val start = obj.longOrNull("startEpochMillis") ?: reject("Missing start time (startEpochMillis)")
    val duration = obj.longOrNull("durationMillis") ?: reject("Missing duration (durationMillis)")

    val explicitDate = obj.stringOrNull("date")?.let { raw ->
        runCatching { LocalDate.parse(raw.trim()) }.getOrNull() ?: reject("Invalid date '$raw' (expected YYYY-MM-DD)")
    }
    val date = explicitDate ?: Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
    // The stored day may legitimately differ from the UTC day (time zones), but never by more than one.
    if (abs(date.toEpochDay() - Math.floorDiv(start, 86_400_000L)) > 1L) reject("Date doesn't match the start time")

    val label = obj.stringOrNull("label").trimmedOrNull() ?: defaultLabel(type)
    val subject = obj.stringOrNull("subject").trimmedOrNull()
    val task = obj.stringOrNull("task").trimmedOrNull()
    val taskSubject = obj.stringOrNull("taskSubject").trimmedOrNull()

    sessionProblem(start, duration, now)?.let { reject(it) }
    checkSessionText(label, subject, task, taskSubject)

    return ImportedSession(
        type = type,
        label = label,
        startEpochMillis = start,
        durationMillis = duration,
        completedNaturally = obj.boolOrNull("completedNaturally") ?: false,
        dateEpochDay = date.toEpochDay(),
        subjectName = subject,
        taskTitle = task,
        taskSubjectName = taskSubject
    )
}

private fun parseJsonTask(obj: JSONObject?): ImportedTask {
    if (obj == null) reject("Not a valid entry")
    val title = obj.stringOrNull("title").trimmedOrNull() ?: reject("Missing title")
    lengthProblem(title, ImportLimits.MAX_TITLE_LENGTH, "Title")?.let { reject(it) }
    val chapter = obj.stringOrNull("chapter").trimmedOrNull()
    val topic = obj.stringOrNull("topic").trimmedOrNull()
    val subject = obj.stringOrNull("subject").trimmedOrNull()
    lengthProblem(chapter, ImportLimits.MAX_NAME_LENGTH, "Chapter")?.let { reject(it) }
    lengthProblem(topic, ImportLimits.MAX_NAME_LENGTH, "Topic")?.let { reject(it) }
    lengthProblem(subject, ImportLimits.MAX_NAME_LENGTH, "Subject name")?.let { reject(it) }

    // Absent priority defaults to MEDIUM; a *present but unrecognised* one is an error, not a silent guess.
    val priorityRaw = obj.stringOrNull("priority").trimmedOrNull()
    val priority = if (priorityRaw == null) {
        TaskPriority.MEDIUM.name
    } else {
        TaskPriority.values().firstOrNull { it.name.equals(priorityRaw, ignoreCase = true) }?.name
            ?: reject("Unknown priority '$priorityRaw'")
    }

    val deadline = obj.stringOrNull("deadline").trimmedOrNull()?.let { raw ->
        (runCatching { LocalDate.parse(raw) }.getOrNull() ?: reject("Invalid deadline '$raw' (expected YYYY-MM-DD)")).toEpochDay()
    }
    val estimate = obj.longOrNull("estimatedMinutes")?.let { minutes ->
        if (minutes !in 1L..10_000L) reject("Estimated minutes must be between 1 and 10000")
        minutes.toInt()
    }
    val completed = obj.boolOrNull("completed") ?: false

    return ImportedTask(
        title = title,
        chapter = chapter,
        topic = topic,
        subjectName = subject,
        priority = priority,
        deadlineEpochDay = deadline,
        estimatedDurationMinutes = estimate,
        completed = completed,
        // A task that isn't complete never carries a completion time (mirrors applyTaskCompletion).
        completedAtEpochMillis = if (completed) obj.longOrNull("completedAtEpochMillis") else null,
        createdAtEpochMillis = obj.longOrNull("createdAtEpochMillis")
    )
}

// ---------------------------------------------------------------------------------------------
// Planning
// ---------------------------------------------------------------------------------------------

/** A subject the import would create because no subject with that name (ignoring case) exists yet. */
data class NewSubject(val name: String, val icon: String, val colorArgb: Int)

/**
 * Exactly what confirming the import would add. Everything else in the file
 * is either a [duplicateSessions] (already in the app), or in [rejected].
 *
 * @param sessionsWithoutTaskLink sessions that name a task which doesn't
 *   exist here (and isn't being created by this import), so they will be
 *   imported without a task link rather than dropped.
 */
data class ImportPlan(
    val format: TransferFormat,
    val newSubjects: List<NewSubject>,
    val newTasks: List<ImportedTask>,
    val newSessions: List<ImportedSession>,
    val duplicateSessions: Int,
    val sessionsWithoutTaskLink: Int,
    val rejected: List<RejectedRow>
) {
    /** False when there is nothing new to add (e.g. re-importing your own export). */
    val hasChanges: Boolean
        get() = newSubjects.isNotEmpty() || newTasks.isNotEmpty() || newSessions.isNotEmpty()
}

fun subjectKey(name: String): String = name.trim().lowercase()

fun taskKey(title: String, subjectName: String?): String =
    title.trim().lowercase() + "\u0000" + (subjectName?.let(::subjectKey) ?: "")

/** Sessions are identified by start, duration and type at *second* precision, since CSV can't carry milliseconds. */
fun sessionKey(startEpochMillis: Long, durationMillis: Long, type: String): String =
    "${startEpochMillis / 1000}|${durationMillis / 1000}|$type"

/**
 * Finds which known task a session refers to. Exact (title + task's subject)
 * first; failing that, if exactly one known task has that title, it's
 * unambiguous and is used (this is what makes CSV — which only knows the
 * *session's* subject — link correctly when the task's subject differs).
 * Two or more tasks sharing a title with no exact match is ambiguous, so the
 * session is left unlinked rather than guessed.
 */
class TaskKeyResolver(knownKeys: Collection<String>) {
    private val known: Set<String> = knownKeys.toSet()
    private val byTitle: Map<String, List<String>> = known.groupBy { it.substringBefore('\u0000') }

    fun resolve(title: String, subjectName: String?): String? {
        val exact = taskKey(title, subjectName)
        if (exact in known) return exact
        return byTitle[title.trim().lowercase()]?.singleOrNull()
    }
}

fun planImport(
    parsed: ParsedImport,
    existingSubjects: List<SubjectEntity>,
    existingTasks: List<TaskEntity>,
    existingSessions: List<StudySessionEntity>
): ImportPlan {
    val existingSubjectKeys = existingSubjects.map { subjectKey(it.name) }.toSet()
    val subjectNameById = existingSubjects.associate { it.id to it.name }
    val existingTaskKeys = existingTasks.map { taskKey(it.title, it.subjectId?.let { id -> subjectNameById[id] }) }.toSet()

    // Subjects to create: declared ones first (they carry an icon and colour), then any merely referenced by name.
    val newSubjects = LinkedHashMap<String, NewSubject>()
    fun wantSubject(name: String?, icon: String?, color: Int?) {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isEmpty()) return
        val key = subjectKey(trimmed)
        if (key in existingSubjectKeys || key in newSubjects) return
        newSubjects[key] = NewSubject(trimmed, icon ?: DEFAULT_IMPORT_ICON, color ?: DEFAULT_IMPORT_COLOR)
    }
    parsed.subjects.forEach { wantSubject(it.name, it.icon, it.colorArgb) }
    parsed.tasks.forEach { wantSubject(it.subjectName, null, null) }
    parsed.sessions.forEach {
        wantSubject(it.subjectName, null, null)
        wantSubject(it.taskSubjectName, null, null)
    }

    val newTasks = LinkedHashMap<String, ImportedTask>()
    parsed.tasks.forEach { task ->
        val key = taskKey(task.title, task.subjectName)
        if (key !in existingTaskKeys && key !in newTasks) newTasks[key] = task
    }

    val resolver = TaskKeyResolver(existingTaskKeys + newTasks.keys)
    val seen = existingSessions.map { sessionKey(it.startEpochMillis, it.durationMillis, it.type) }.toMutableSet()
    val newSessions = mutableListOf<ImportedSession>()
    var duplicates = 0
    var withoutLink = 0
    parsed.sessions.forEach { session ->
        // Also catches a duplicate that appears twice within the file itself.
        if (!seen.add(sessionKey(session.startEpochMillis, session.durationMillis, session.type))) {
            duplicates++
        } else {
            newSessions += session
            if (session.taskTitle != null && resolver.resolve(session.taskTitle, session.taskSubjectName) == null) withoutLink++
        }
    }

    return ImportPlan(
        format = parsed.format,
        newSubjects = newSubjects.values.toList(),
        newTasks = newTasks.values.toList(),
        newSessions = newSessions,
        duplicateSessions = duplicates,
        sessionsWithoutTaskLink = withoutLink,
        rejected = parsed.rejected
    )
}
