package com.studyspace.timer.navigation

/**
 * Single source of truth for every route in the app. Bottom-nav destinations
 * are the top-level ones (Home, Timer, Analytics, Themes, Settings); Pomodoro
 * and Focus Mode are reached from Home's quick actions but aren't bottom-nav
 * tabs themselves, matching the approved dashboard concept.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Timer : Screen("timer")
    data object Pomodoro : Screen("pomodoro")
    data object Focus : Screen("focus")
    data object Analytics : Screen("analytics")
    data object Themes : Screen("themes")
    data object Settings : Screen("settings")

    companion object {
        /** Destinations shown as bottom-navigation tabs. */
        val bottomNavScreens = listOf(Home, Timer, Analytics, Themes, Settings)
    }
}
