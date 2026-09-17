package com.studyspace.timer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.navigation.StudySpaceNavHost
import com.studyspace.timer.screens.settings.SettingsViewModel
import com.studyspace.timer.screens.themes.ThemesViewModel
import com.studyspace.timer.service.TimerForegroundService
import com.studyspace.timer.timer.ActiveTimerSession
import com.studyspace.timer.ui.theme.LocalReduceMotion
import com.studyspace.timer.ui.theme.StudySpaceTimerTheme

/**
 * Single-activity entry point. Stage 2 wired the real NavHost + bottom
 * navigation + galaxy dashboard here, replacing the Stage 1 placeholder.
 *
 * Stage 4: [StudySpaceTimerApp] now also observes [ActiveTimerSession] and,
 * the moment a timer becomes active, (a) starts [TimerForegroundService] so
 * it keeps running/notifying in the background, and (b) requests the
 * runtime `POST_NOTIFICATIONS` permission on API 33+ — requested at that
 * moment rather than on every app launch, since asking before the user has
 * even started a timer would be requesting a permission with no immediate
 * purpose visible to them.
 *
 * Stage 8: also reads [SettingsViewModel] (same Activity-scoped instance
 * the Settings screen writes to) for two app-wide preferences:
 *  - "Keep screen on during sessions" — applied via `LocalView.current
 *    .keepScreenOn`, kept true only while [activeSession] is non-null *and*
 *    the setting is on, and always cleared when neither holds, so the
 *    display goes back to normal sleep behavior the moment a timer stops.
 *  - "Reduce motion" — provided app-wide via [LocalReduceMotion] so any
 *    animated composable (currently just
 *    [com.studyspace.timer.ui.components.ProgressRing]) can read it without
 *    every call site needing to thread a parameter through.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudySpaceTimerApp()
        }
    }
}

@Composable
fun StudySpaceTimerApp() {
    val context = LocalContext.current
    val activeSession by ActiveTimerSession.active.collectAsState()
    var notificationPermissionRequested by remember { mutableStateOf(false) }

    // Same Activity-scoped ThemesViewModel instance the Themes screen writes
    // to — reading the palette here is what makes a palette change apply to
    // every screen's MaterialTheme, not just the Themes screen itself.
    val themesViewModel: ThemesViewModel = viewModel()
    val paletteId by themesViewModel.paletteId.collectAsState()

    // Stage 8: same Activity-scoped SettingsViewModel instance the Settings
    // screen reads from and writes to.
    val settingsViewModel: SettingsViewModel = viewModel()
    val keepScreenOnEnabled by settingsViewModel.keepScreenOnEnabled.collectAsState()
    val reduceMotion by settingsViewModel.reduceMotionEnabled.collectAsState()

    val view = LocalView.current
    LaunchedEffect(activeSession != null, keepScreenOnEnabled) {
        view.keepScreenOn = activeSession != null && keepScreenOnEnabled
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* If denied, the foreground service still runs and the timer keeps
           counting -- it just won't show a notification, which is a
           degraded but honest fallback rather than a broken one. */ }

    LaunchedEffect(activeSession != null) {
        if (activeSession != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionRequested) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionRequested = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerForegroundService::class.java)
            )
        }
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        StudySpaceTimerTheme(paletteId = paletteId) {
            Surface(modifier = Modifier.fillMaxSize()) {
                StudySpaceNavHost()
            }
        }
    }
}
