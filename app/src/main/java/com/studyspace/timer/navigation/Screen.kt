package com.studyspace.timer.navigation

/**
 * Single source of truth for every route in the app. Bottom-nav destinations
 * are the top-level ones (Home, Timer, Analytics, Themes, Settings); Pomodoro,
 * Focus Mode, Study Goals, Subjects, Tasks, Planner, Achievements, and
 * Daily Summary, and Data Management (reached from Settings) are
 * reached from Home's quick actions but aren't bottom-nav tabs themselves,
 * matching the approved dashboard concept. [SubjectDetail] takes a
 * `subjectId` path argument, reached only from [Subjects]'s list, never
 * from the bottom nav.
 * [TimerFromPlan] is a separate destination from [Timer] (not an optional
 * query arg on the same route) specifically so [Timer]'s existing
 * bottom-nav navigation/route-matching is untouched — see the Planner
 * Phase 6 session log entry in `PROJECT_STATE.md` for why a second route
 * was the lower-risk choice over adding optional args to `Timer` itself.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Timer : Screen("timer")
    data object Pomodoro : Screen("pomodoro")
    data object Focus : Screen("focus")
    data object Goals : Screen("goals")
    data object Subjects : Screen("subjects")
    data object SubjectDetail : Screen("subject_detail/{subjectId}") {
        fun createRoute(subjectId: Long) = "subject_detail/$subjectId"
    }
    data object Tasks : Screen("tasks")
    data object Planner : Screen("planner")
    data object Achievements : Screen("achievements")
    data object DailySummary : Screen("daily_summary")
    data object DataManagement : Screen("data_management")
    /**
     * Opens the Timer screen (Self-Study tab) with a subject/task
     * pre-selected via [com.studyspace.timer.timer.StopwatchTimerViewModel.selectSubject]/
     * `selectTask`. `-1L` is the "none" sentinel for either arg, since
     * `NavType.LongType` path segments can't be null.
     */
    data object TimerFromPlan : Screen("timer_from_plan/{subjectId}/{taskId}") {
        const val NONE = -1L
        fun createRoute(subjectId: Long?, taskId: Long?) =
            "timer_from_plan/${subjectId ?: NONE}/${taskId ?: NONE}"
    }
    /**
     * Phase 10 (Daily Dashboard): Home's "Quick Start" row jumps straight
     * to a specific Timer tab (0 = Self-Study/"Start Stopwatch", 2 =
     * Normal/"Start Custom Timer" — "Start Pomodoro" instead just opens
     * [Pomodoro] directly, since Pomodoro already has its own screen).
     * A third, separate route rather than extending [TimerFromPlan] or
     * [Timer] themselves — same reasoning as [TimerFromPlan] already
     * documents: touching an existing route's shape risks the bottom nav
     * or an existing deep-link, where a brand new route can't.
     */
    data object QuickStartTimer : Screen("quick_start_timer/{tab}") {
        fun createRoute(tab: Int) = "quick_start_timer/$tab"
    }
    data object Analytics : Screen("analytics")
    data object Themes : Screen("themes")
    data object Settings : Screen("settings")

    companion object {
        /** Destinations shown as bottom-navigation tabs. */
        val bottomNavScreens = listOf(Home, Timer, Analytics, Themes, Settings)
    }
}
