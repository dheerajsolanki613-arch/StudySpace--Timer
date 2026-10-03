package com.studyspace.timer.ai

import com.studyspace.timer.planning.LocalPlanGenerator
import com.studyspace.timer.planning.PlannableTask
import com.studyspace.timer.planning.SmartPlanRequest
import com.studyspace.timer.planning.SmartPlanner
import com.studyspace.timer.planning.AvailabilityBlock
import com.studyspace.timer.data.TaskPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StudyAssistantTest {
    private val emptyPlanRequest = SmartPlanRequest(startEpochDay = 0, daysAhead = 1, availability = emptyList(), maxDailyMinutes = 60)

    // ---------- the default: offline, offers nothing ----------

    @Test
    fun `the offline assistant needs no network and supports nothing`() {
        assertFalse(OfflineStudyAssistant.requiresNetwork)
        assertTrue(OfflineStudyAssistant.capabilities.isEmpty())
        assertTrue(AiCapability.values().none { OfflineStudyAssistant.supports(it) })
    }

    @Test
    fun `every operation on the offline assistant is Unavailable, not an error`() = runTest {
        val a = OfflineStudyAssistant
        assertEquals(AssistantResult.Unavailable, a.generatePlan(emptyList(), emptyPlanRequest))
        assertEquals(AssistantResult.Unavailable, a.summarizeWeek(WeeklySummaryRequest(0, 0, emptyMap(), 0)))
        assertEquals(AssistantResult.Unavailable, a.breakDownTask(TaskBreakdownRequest("t", null, null)))
        assertEquals(AssistantResult.Unavailable, a.organizeTopics(TopicOrganizationRequest("s", emptyList())))
        assertEquals(AssistantResult.Unavailable, a.parseNaturalLanguage(NaturalLanguageRequest("x", 0, emptyList(), emptyList())))
    }

    // ---------- a future provider can be partial ----------

    private val partialProvider = object : StudyAssistant {
        override val displayName = "Fake summarizer"
        override val requiresNetwork = true
        override val capabilities = setOf(AiCapability.WEEKLY_SUMMARY)
        override suspend fun summarizeWeek(request: WeeklySummaryRequest): AssistantResult<String> =
            AssistantResult.Success("You studied ${request.totalMinutes} minutes.")
    }

    @Test
    fun `a provider overriding one capability leaves every other one Unavailable`() = runTest {
        assertEquals(AssistantResult.Success("You studied 90 minutes."), partialProvider.summarizeWeek(WeeklySummaryRequest(90, 2, emptyMap(), 1)))
        assertEquals(AssistantResult.Unavailable, partialProvider.breakDownTask(TaskBreakdownRequest("t", null, null)))
        assertEquals(AssistantResult.Unavailable, partialProvider.parseNaturalLanguage(NaturalLanguageRequest("x", 0, emptyList(), emptyList())))
    }

    @Test
    fun `supports reflects exactly the declared capabilities`() {
        assertTrue(partialProvider.supports(AiCapability.WEEKLY_SUMMARY))
        assertFalse(partialProvider.supports(AiCapability.PLAN_GENERATION))
        assertTrue(partialProvider.requiresNetwork)
    }

    // ---------- the plan-generation seam ----------

    @Test
    fun `LocalPlanGenerator returns exactly what SmartPlanner returns`() = runTest {
        val monday = LocalDate.of(2026, 10, 5).also { check(it.dayOfWeek == DayOfWeek.MONDAY) }.toEpochDay()
        val tasks = listOf(PlannableTask(1, "Task", null, TaskPriority.HIGH, null, 60))
        val request = SmartPlanRequest(monday, 3, DayOfWeek.values().map { AvailabilityBlock(it, 18 * 60, 20 * 60) }, 240)
        assertEquals(SmartPlanner.suggest(tasks, request), LocalPlanGenerator.generate(tasks, request))
    }
}
