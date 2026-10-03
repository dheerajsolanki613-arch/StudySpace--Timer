package com.studyspace.timer.planning

/**
 * Phase 18 (Optional AI architecture). The one contract between "something
 * that proposes study sessions" and the preview-then-confirm UI in
 * `SmartPlanScreen`. Today the only implementation is [LocalPlanGenerator]
 * (Phase 17's deterministic, offline algorithm). A future provider —
 * whatever it is — plugs in by implementing this interface and returning the
 * same [SmartPlanResult]; it then automatically inherits everything that
 * already protects the user: suggestions are shown in a preview, each can be
 * declined, only accepted ones are written, and an accepted one is an
 * ordinary editable planned session.
 *
 * `suspend` on purpose even though [LocalPlanGenerator] never suspends: a
 * provider that does I/O must be callable without changing the ViewModel.
 */
fun interface PlanGenerator {
    suspend fun generate(tasks: List<PlannableTask>, request: SmartPlanRequest): SmartPlanResult
}

/** The default, and currently only, generator: Phase 17's pure algorithm. Works fully offline. */
object LocalPlanGenerator : PlanGenerator {
    override suspend fun generate(tasks: List<PlannableTask>, request: SmartPlanRequest): SmartPlanResult =
        SmartPlanner.suggest(tasks, request)
}
