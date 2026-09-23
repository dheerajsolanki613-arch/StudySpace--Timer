package com.studyspace.timer.data.transfer

import androidx.room.withTransaction
import com.studyspace.timer.data.GoalScope
import com.studyspace.timer.data.db.AppDatabase
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.StudyGoalEntity
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import kotlinx.coroutines.flow.first

/** What an import actually wrote, for the confirmation message. */
data class ImportResult(
    val sessionsAdded: Int,
    val duplicatesSkipped: Int,
    val subjectsCreated: Int,
    val tasksCreated: Int,
    val rejected: Int
)

/** Everything a full backup carries out of the database (Phase 14). Settings/theme live in DataStore, not here — the ViewModel reads those separately and combines them with this into the backup file. */
data class BackupExportBundle(
    val studyData: ExportBundle,
    val plannedSessions: List<PlannedSessionEntity>,
    val weeklyGoalMinutes: Int?
)

/** What a restore actually wrote, for the confirmation message. */
data class RestoreResult(
    val sessionsRestored: Int,
    val subjectsRestored: Int,
    val tasksRestored: Int,
    val plannedSessionsRestored: Int,
    val weeklyGoalRestored: Boolean,
    val itemsSkipped: Int
)

/** The subject/task id lookup a set of inserted rows can be linked against, keyed the same way [subjectKey]/[taskKey] key existing rows. */
private data class InsertedKeys(val subjectIdByKey: Map<String, Long>, val taskIdByKey: Map<String, Long>)

/**
 * Phase 13 — the database side of export/import. All the *decisions* live in
 * the pure, unit-tested [planImport] / [buildCsvExport] / [buildJsonExport];
 * this class only reads the existing rows and performs the writes the plan
 * calls for.
 *
 * Imported sessions are inserted straight through the DAOs, deliberately
 * **not** via `SessionRepository.recordSession`: history being restored must
 * not fire a "goal reached" notification for every day it happens to cross a
 * goal, and it must not be re-filtered by the live-recording rules (the
 * importer already validated each row against the same 10-second minimum).
 *
 * Phase 14 — [restoreBackup] adds a second, distinct write path: **replace**
 * rather than **add**. It deletes every row this class owns and reinserts
 * exactly what the backup file contains, in one transaction. It deliberately
 * reuses [ImportedSubject]/[ImportedTask]/[ImportedSession] and the same
 * name-based linking ([subjectKey]/[taskKey]/[TaskKeyResolver]) that
 * [applyImport] already relies on, via [insertParsedData] — restoring into an
 * empty database is exactly "import everything against nothing existing".
 */
class DataTransferRepository(private val database: AppDatabase) {

    private data class Existing(
        val subjects: List<SubjectEntity>,
        val tasks: List<TaskEntity>,
        val sessions: List<StudySessionEntity>
    )

    private suspend fun loadExisting() = Existing(
        subjects = database.subjectDao().allSubjects().first(),
        tasks = database.taskDao().allTasks().first(),
        sessions = database.studySessionDao().allSessions().first()
    )

    suspend fun loadExportBundle(): ExportBundle {
        val existing = loadExisting()
        return ExportBundle(existing.subjects, existing.tasks, existing.sessions)
    }

    /** Read-only: works out what importing [parsed] would add, without writing anything. */
    suspend fun previewImport(parsed: ParsedImport): ImportPlan {
        val existing = loadExisting()
        return planImport(parsed, existing.subjects, existing.tasks, existing.sessions)
    }

    /**
     * Re-plans against the database as it is *now* (so a duplicate can't slip
     * in if data changed since the preview), then writes the subjects, tasks
     * and sessions in one transaction — either all of it is saved or none.
     * Only ever inserts.
     */
    suspend fun applyImport(parsed: ParsedImport, nowEpochMillis: Long): ImportResult {
        val existing = loadExisting()
        val plan = planImport(parsed, existing.subjects, existing.tasks, existing.sessions)

        database.withTransaction {
            insertParsedData(plan, existing, nowEpochMillis)
        }

        return ImportResult(
            sessionsAdded = plan.newSessions.size,
            duplicatesSkipped = plan.duplicateSessions,
            subjectsCreated = plan.newSubjects.size,
            tasksCreated = plan.newTasks.size,
            rejected = plan.rejected.size
        )
    }

    /** Everything Room needs to write a full backup file: study data plus planned sessions and the weekly goal. */
    suspend fun loadBackupExportBundle(): BackupExportBundle {
        val existing = loadExisting()
        val plannedSessions = database.plannedSessionDao().allPlannedSessions().first()
        val weeklyGoalMinutes = database.studyGoalDao().goalForScope(GoalScope.WEEKLY.name).first()?.targetMinutes
        return BackupExportBundle(
            studyData = ExportBundle(existing.subjects, existing.tasks, existing.sessions),
            plannedSessions = plannedSessions,
            weeklyGoalMinutes = weeklyGoalMinutes
        )
    }

    /**
     * Replaces every subject, task, study session, planned session and study
     * goal with exactly what [content] contains — the opposite of
     * [applyImport]'s "only ever add". Everything this class owns is deleted
     * and reinserted in one transaction, so either the whole restore lands or
     * (on any failure) none of it does and the prior data is untouched.
     *
     * Settings and theme are **not** touched here — they live in DataStore,
     * not Room, and are restored separately by the caller (see
     * `DataManagementViewModel.restoreBackup`) once this transaction commits.
     */
    suspend fun restoreBackup(content: BackupContent, nowEpochMillis: Long): RestoreResult {
        var plannedRestored = 0
        var goalRestored = false

        database.withTransaction {
            // Children before parents. With `ON DELETE SET_NULL` foreign keys the
            // order doesn't affect correctness, but it keeps the transaction from
            // ever holding a study_sessions/planned_sessions row that points at a
            // subject/task id that no longer exists, even momentarily.
            database.studySessionDao().deleteAll()
            database.plannedSessionDao().deleteAll()
            database.taskDao().deleteAll()
            database.subjectDao().deleteAll()
            database.studyGoalDao().deleteAll()

            val empty = Existing(emptyList(), emptyList(), emptyList())
            val plan = planImport(content.studyData, empty.subjects, empty.tasks, empty.sessions)
            val keys = insertParsedData(plan, empty, nowEpochMillis)

            val resolver = TaskKeyResolver(keys.taskIdByKey.keys)
            content.plannedSessions.forEach { planned ->
                val linkedTaskKey = planned.taskTitle?.let { title -> resolver.resolve(title, planned.taskSubjectName) }
                database.plannedSessionDao().insert(
                    PlannedSessionEntity(
                        dateEpochDay = planned.dateEpochDay,
                        startMinuteOfDay = planned.startMinuteOfDay,
                        durationMinutes = planned.durationMinutes,
                        subjectId = planned.subjectName?.let { name -> keys.subjectIdByKey[subjectKey(name)] },
                        taskId = linkedTaskKey?.let { key -> keys.taskIdByKey[key] },
                        notes = planned.notes,
                        status = planned.status,
                        createdAtEpochMillis = planned.createdAtEpochMillis
                    )
                )
                plannedRestored++
            }

            content.weeklyGoalMinutes?.let { minutes ->
                database.studyGoalDao().upsert(
                    StudyGoalEntity(
                        scope = GoalScope.WEEKLY.name,
                        subjectId = null,
                        targetMinutes = minutes,
                        createdAtEpochMillis = nowEpochMillis
                    )
                )
                goalRestored = true
            }
        }

        return RestoreResult(
            sessionsRestored = content.studyData.sessions.size,
            subjectsRestored = content.studyData.subjects.size,
            tasksRestored = content.studyData.tasks.size,
            plannedSessionsRestored = plannedRestored,
            weeklyGoalRestored = goalRestored,
            itemsSkipped = content.studyData.rejected.size + content.rejected.size
        )
    }

    /**
     * Inserts every subject/task/session [plan] calls new, linking each
     * session to a subject/task by name exactly as [applyImport] always has.
     * [baseline] supplies the ids of rows that already exist (non-empty for
     * [applyImport], always empty for [restoreBackup] since it deletes first)
     * so a session can link to either a pre-existing or a newly-inserted row.
     * Must run inside the caller's `withTransaction` block.
     */
    private suspend fun insertParsedData(plan: ImportPlan, baseline: Existing, nowEpochMillis: Long): InsertedKeys {
        val subjectDao = database.subjectDao()
        val taskDao = database.taskDao()
        val sessionDao = database.studySessionDao()

        val subjectIdByKey = HashMap<String, Long>()
        baseline.subjects.forEach { subjectIdByKey[subjectKey(it.name)] = it.id }
        plan.newSubjects.forEach { subject ->
            subjectIdByKey[subjectKey(subject.name)] = subjectDao.insert(
                SubjectEntity(
                    name = subject.name,
                    icon = subject.icon,
                    colorArgb = subject.colorArgb,
                    createdAtEpochMillis = nowEpochMillis
                )
            )
        }

        val subjectNameById = baseline.subjects.associate { it.id to it.name }
        val taskIdByKey = HashMap<String, Long>()
        baseline.tasks.forEach { task ->
            taskIdByKey[taskKey(task.title, task.subjectId?.let { id -> subjectNameById[id] })] = task.id
        }
        plan.newTasks.forEach { task ->
            taskIdByKey[taskKey(task.title, task.subjectName)] = taskDao.insert(
                TaskEntity(
                    title = task.title,
                    chapter = task.chapter,
                    topic = task.topic,
                    subjectId = task.subjectName?.let { name -> subjectIdByKey[subjectKey(name)] },
                    priority = task.priority,
                    deadlineEpochDay = task.deadlineEpochDay,
                    estimatedDurationMinutes = task.estimatedDurationMinutes,
                    completed = task.completed,
                    createdAtEpochMillis = task.createdAtEpochMillis ?: nowEpochMillis,
                    completedAtEpochMillis = if (task.completed) task.completedAtEpochMillis else null
                )
            )
        }

        val resolver = TaskKeyResolver(taskIdByKey.keys)
        plan.newSessions.forEach { session ->
            val linkedTaskKey = session.taskTitle?.let { title -> resolver.resolve(title, session.taskSubjectName) }
            sessionDao.insert(
                StudySessionEntity(
                    type = session.type,
                    label = session.label,
                    startEpochMillis = session.startEpochMillis,
                    durationMillis = session.durationMillis,
                    completedNaturally = session.completedNaturally,
                    dateEpochDay = session.dateEpochDay,
                    subjectId = session.subjectName?.let { name -> subjectIdByKey[subjectKey(name)] },
                    taskId = linkedTaskKey?.let { key -> taskIdByKey[key] }
                )
            )
        }

        return InsertedKeys(subjectIdByKey, taskIdByKey)
    }
}
