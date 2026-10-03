package com.studyspace.timer.ai

/** A subject the resolver may link to. A projection, not the Room entity — this package has no database dependency. */
data class KnownSubject(val id: Long, val name: String)

data class KnownTask(val id: Long, val title: String, val subjectId: Long?)

/** A [DraftSession] that passed validation and has ids where the app has matches. [warnings] explain any link that couldn't be made. */
data class ResolvedDraft(
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val subjectId: Long?,
    val taskId: Long?,
    val notes: String?,
    val warnings: List<String>
)

data class RejectedDraft(val draft: DraftSession, val reason: String)

data class DraftResolution(val resolved: List<ResolvedDraft>, val rejected: List<RejectedDraft>)

/**
 * Phase 18. The checkpoint every assistant output must pass before it can
 * become a preview row. An assistant is treated like any other untrusted
 * input source (same stance as Phase 13's import validation): it may
 * hallucinate a date in the past, a 40-hour session, or a subject that
 * doesn't exist, and none of that may reach the planner.
 *
 * Rules, all deterministic and pure so they're unit-tested:
 *  - A draft with an impossible duration, start time, or date is **rejected
 *    with a reason** (never silently "fixed"), so the person sees what was
 *    dropped.
 *  - Subjects/tasks are matched by trimmed, case-insensitive name against
 *    what the person actually has. **Nothing is ever created** to satisfy a
 *    draft: an unknown subject or task just leaves that link empty with a
 *    warning, and the session is still offered (the person can edit it).
 *  - An ambiguous match (two tasks with the same title and nothing to
 *    disambiguate) is left unlinked rather than guessed.
 */
object DraftResolver {
    const val DEFAULT_START_MINUTE_OF_DAY = 18 * 60
    const val MAX_DAYS_AHEAD = 365
    private const val MINUTES_PER_DAY = 24 * 60

    fun resolve(
        drafts: List<DraftSession>,
        subjects: List<KnownSubject>,
        tasks: List<KnownTask>,
        todayEpochDay: Long,
        defaultStartMinuteOfDay: Int = DEFAULT_START_MINUTE_OF_DAY
    ): DraftResolution {
        val resolved = mutableListOf<ResolvedDraft>()
        val rejected = mutableListOf<RejectedDraft>()

        for (draft in drafts) {
            val reason = validate(draft, todayEpochDay, defaultStartMinuteOfDay)
            if (reason != null) {
                rejected += RejectedDraft(draft, reason)
                continue
            }
            resolved += link(draft, subjects, tasks, draft.startMinuteOfDay ?: defaultStartMinuteOfDay)
        }
        return DraftResolution(resolved, rejected)
    }

    private fun validate(draft: DraftSession, todayEpochDay: Long, defaultStart: Int): String? {
        if (draft.durationMinutes !in 1..MINUTES_PER_DAY) {
            return "Duration must be between 1 minute and 24 hours."
        }
        val start = draft.startMinuteOfDay ?: defaultStart
        if (start !in 0 until MINUTES_PER_DAY) return "Start time isn't a valid time of day."
        if (start + draft.durationMinutes > MINUTES_PER_DAY) return "Session would run past midnight."
        if (draft.dateEpochDay < todayEpochDay) return "Date is in the past."
        if (draft.dateEpochDay > todayEpochDay + MAX_DAYS_AHEAD) return "Date is more than a year ahead."
        return null
    }

    private fun link(draft: DraftSession, subjects: List<KnownSubject>, tasks: List<KnownTask>, start: Int): ResolvedDraft {
        val warnings = mutableListOf<String>()

        var subjectId: Long? = null
        val subjectName = draft.subjectName?.trim()?.takeIf { it.isNotEmpty() }
        if (subjectName != null) {
            val matches = subjects.filter { it.name.trim().equals(subjectName, ignoreCase = true) }
            when (matches.size) {
                1 -> subjectId = matches.single().id
                0 -> warnings += "No subject named \"$subjectName\" — added without a subject."
                else -> warnings += "More than one subject is named \"$subjectName\" — added without a subject."
            }
        }

        var taskId: Long? = null
        val taskTitle = draft.taskTitle?.trim()?.takeIf { it.isNotEmpty() }
        if (taskTitle != null) {
            val byTitle = tasks.filter { it.title.trim().equals(taskTitle, ignoreCase = true) }
            // When a subject was resolved, a task belonging to a *different* subject isn't a match;
            // a task with no subject at all still is.
            val candidates = if (subjectId != null) byTitle.filter { it.subjectId == subjectId || it.subjectId == null } else byTitle
            when {
                candidates.size == 1 -> {
                    taskId = candidates.single().id
                    if (subjectId == null) subjectId = candidates.single().subjectId
                }
                candidates.size > 1 -> warnings += "More than one task is named \"$taskTitle\" — added without a task."
                byTitle.isNotEmpty() -> warnings += "The task \"$taskTitle\" belongs to a different subject — added without a task."
                else -> warnings += "No task named \"$taskTitle\" — added without a task."
            }
        }

        return ResolvedDraft(
            dateEpochDay = draft.dateEpochDay,
            startMinuteOfDay = start,
            durationMinutes = draft.durationMinutes,
            subjectId = subjectId,
            taskId = taskId,
            notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
            warnings = warnings
        )
    }
}
