package com.studyspace.timer.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studyspace.timer.screens.analytics.AnalyticsScreen
import com.studyspace.timer.screens.focus.FocusScreen
import com.studyspace.timer.screens.home.HomeScreen
import com.studyspace.timer.screens.pomodoro.PomodoroScreen
import com.studyspace.timer.screens.settings.SettingsScreen
import com.studyspace.timer.screens.themes.ThemesScreen
import com.studyspace.timer.screens.themes.ThemesViewModel
import com.studyspace.timer.screens.timer.TimerScreen
import com.studyspace.timer.ui.components.AppBackground
import com.studyspace.timer.ui.components.StudySpaceBottomNav
import com.studyspace.timer.ui.theme.AppPalettes
import com.studyspace.timer.wallpaper.WallpaperSelection

/**
 * Top-level scaffold: app-wide wallpaper background + bottom nav + NavHost.
 * Bottom-nav taps use launchSingleTop and restore/save state so switching
 * tabs doesn't rebuild each screen from scratch or stack up duplicate
 * back-stack entries.
 *
 * The wallpaper (default gradient / built-in / personalized gallery photo)
 * is read once here, from [ThemesViewModel], and painted behind a fully
 * transparent [Scaffold] so every screen shares the same background instead
 * of each one drawing its own — the same source of truth the Themes screen
 * writes to when the user picks a wallpaper or personalizes with a gallery photo.
 */
@Composable
fun StudySpaceNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val themesViewModel: ThemesViewModel = viewModel()
    val wallpaperSelection by themesViewModel.selection.collectAsState(initial = WallpaperSelection.Default)
    val paletteId by themesViewModel.paletteId.collectAsState(initial = AppPalettes.galaxy.id)

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(selection = wallpaperSelection, palette = AppPalettes.byId(paletteId))

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                StudySpaceBottomNav(currentRoute = currentRoute) { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenTimer = { navController.navigate(Screen.Timer.route) },
                        onOpenPomodoro = { navController.navigate(Screen.Pomodoro.route) },
                        onOpenFocus = { navController.navigate(Screen.Focus.route) }
                    )
                }
                composable(Screen.Timer.route) { TimerScreen() }
                composable(Screen.Pomodoro.route) { PomodoroScreen() }
                composable(Screen.Focus.route) { FocusScreen() }
                composable(Screen.Analytics.route) { AnalyticsScreen() }
                composable(Screen.Themes.route) { ThemesScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}
