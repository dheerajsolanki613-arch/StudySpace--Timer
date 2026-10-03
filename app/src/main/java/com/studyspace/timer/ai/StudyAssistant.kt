package com.studyspace.timer.ai

import com.studyspace.timer.planning.PlannableTask
import com.studyspace.timer.planning.SmartPlanRequest
import com.studyspace.timer.planning.SmartPlanResult

/**
 * Phase 18 — architecture for *optional* assistance, with **no AI service,
 * no network code, and no new dependency added**. The app has no `INTERNET`
 * permission and no HTTP library today (audited before writing this), so a
 * networked provider cannot be introduced by accident: someone would have to
 * add the permission and a client deliberately, and [StudyAssistant.requiresNetwork]
 * exists so the UI can then disclose that before the first use.
 *
 * Design rules any implementation must follow (also why the types below look
 * the way they do):
 *  1. **Optional, capability-gated.** The app never assumes an assistant
 *     exists. UI shows an assistant-backed entry point only when
 *     `capability in assistant.capabilities` ([StudyAssistant.supports]); with
 *     the default [OfflineStudyAssistant] that is never, so nothing appears —
 *     there are deliberately no dead buttons waiting for a backend.
 *  2. **Data-minimal.** A provider receives only the request object it is
 *     handed — plain values, never Room entities, never a repository, never
 *     the whole database. Subjects and tasks are passed as names/titles.
 *  3. **Drafts, never writes.** Every result is a *draft*. Nothing an
 *     assistant returns is persisted; drafts go through the same
 *     preview → decline-any → confirm flow as Phase 17, and become ordinary
 *     editable rows only when the person accepts them.
 *  4. **Failure is a normal outcome.** [AssistantResult.Unavailable] and
 *     [AssistantResult.Failed] must leave the app fully usable; every feature
 *     works without an assistant.
 */
enum class AiCapability {
    /** Propose planned sessions from tasks + availability (same output as [com.studyspace.timer.planning.PlanGenerator]). */
    PLAN_GENERATION,

    /** Turn a week's descriptive statistics into a short written summary. */
    WEEKLY_SUMMARY,

    /** Split one task into smaller steps. */
    TASK_BREAKDOWN,

    /** Group a subject's topic names into chapters. */
    TOPIC_ORGANIZATION,

    /** Turn free text like "plan 3 hours of physics tomorrow" into [DraftSession]s. */
    NATURAL_LANGUAGE_INPUT
}

sealed interface AssistantResult<out T> {
    data class Success<out T>(val value: T) : AssistantResult<T>

    /** This assistant doesn't offer the requested capability (or none is configured). Not an error. */
    data object Unavailable : AssistantResult<Nothing>

    /** The capability exists but this attempt failed (e.g. a provider that needs a network had none). [message] is user-presentable. */
    data class Failed(val message: String) : AssistantResult<Nothing>
}

/** "Plan 3 hours of physics tomorrow" → [text]. Only names are shared, never ids or full task records. */
data class NaturalLanguageRequest(
    val text: String,
    val todayEpochDay: Long,
    val knownSubjectNames: List<String>,
    val knownTaskTitles: List<String>
)

/**
 * An unvalidated, unresolved proposal. References subjects/tasks by *name*
 * because an assistant can't and shouldn't know database ids. Must pass
 * through [DraftResolver] before it can be shown as a preview row.
 */
data class DraftSession(
    val dateEpochDay: Long,
    /** Null when the text didn't say when; [DraftResolver] fills a default. */
    val startMinuteOfDay: Int?,
    val durationMinutes: Int,
    val subjectName: String?,
    val taskTitle: String?,
    val notes: String?
)

data class TaskBreakdownRequest(val taskTitle: String, val subjectName: String?, val estimatedMinutes: Int?)

/** A proposed sub-step. Becomes a real task only if the person accepts it. */
data class DraftTask(val title: String, val estimatedMinutes: Int?)

data class TopicOrganizationRequest(val subjectName: String, val topicNames: List<String>)

data class TopicGroup(val chapterName: String, val topicNames: List<String>)

/** Descriptive numbers only — the same kind of figures Analytics already shows. No session-level records. */
data class WeeklySummaryRequest(
    val totalMinutes: Int,
    val sessionCount: Int,
    val minutesBySubjectName: Map<String, Int>,
    val daysStudied: Int
)

interface StudyAssistant {
    val displayName: String

    /** True if using this assistant sends data off the device. The UI must disclose this before first use. */
    val requiresNetwork: Boolean

    val capabilities: Set<AiCapability>

    fun supports(capability: AiCapability): Boolean = capability in capabilities

    // Every operation defaults to Unavailable so a provider overrides only what it actually offers,
    // and adding a capability later can never break an existing implementation.

    suspend fun generatePlan(tasks: List<PlannableTask>, request: SmartPlanRequest): AssistantResult<SmartPlanResult> =
        AssistantResult.Unavailable

    suspend fun summarizeWeek(request: WeeklySummaryRequest): AssistantResult<String> = AssistantResult.Unavailable

    suspend fun breakDownTask(request: TaskBreakdownRequest): AssistantResult<List<DraftTask>> = AssistantResult.Unavailable

    suspend fun organizeTopics(request: TopicOrganizationRequest): AssistantResult<List<TopicGroup>> = AssistantResult.Unavailable

    suspend fun parseNaturalLanguage(request: NaturalLanguageRequest): AssistantResult<List<DraftSession>> =
        AssistantResult.Unavailable
}

/**
 * The only assistant that exists: offers nothing, needs no network, and is
 * what makes "the whole app works without AI" true by construction rather
 * than by convention.
 */
object OfflineStudyAssistant : StudyAssistant {
    override val displayName = "Offline (no assistant)"
    override val requiresNetwork = false
    override val capabilities: Set<AiCapability> = emptySet()
}
