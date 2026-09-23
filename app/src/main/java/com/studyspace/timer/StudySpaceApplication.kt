package com.studyspace.timer

import android.app.Application
import com.studyspace.timer.data.db.AppDatabase
import com.studyspace.timer.data.repository.GoalRepository
import com.studyspace.timer.data.repository.PlannedSessionRepository
import com.studyspace.timer.data.repository.SessionRepository
import com.studyspace.timer.data.repository.SubjectRepository
import com.studyspace.timer.data.repository.TaskRepository
import com.studyspace.timer.data.transfer.DataTransferRepository
import com.studyspace.timer.reminders.GoalNotifier
import com.studyspace.timer.reminders.ReminderNotifications
import com.studyspace.timer.reminders.ReminderScheduler
import com.studyspace.timer.settings.SettingsRepository
import com.studyspace.timer.theme.PaletteRepository
import com.studyspace.timer.wallpaper.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * App-wide holder for the Room database and [SessionRepository], lazily
 * created on first access. No DI framework (Hilt/Koin) is used — the project
 * has stayed deliberately dependency-light, and one Application-scoped
 * holder is enough for the single repository Stage 5 introduces.
 *
 * ViewModels reach this via `(application as StudySpaceApplication)` inside
 * an `AndroidViewModel` — see [com.studyspace.timer.timer.StopwatchTimerViewModel],
 * [com.studyspace.timer.timer.CountdownTimerViewModel],
 * [com.studyspace.timer.timer.PomodoroViewModel], and
 * [com.studyspace.timer.screens.home.HomeViewModel]. Compose's default
 * `viewModel()` factory already knows how to construct an `AndroidViewModel`
 * (it falls back to `ViewModelProvider.AndroidViewModelFactory` when the
 * `LocalViewModelStoreOwner` is backed by an Activity), so no custom
 * ViewModelProvider.Factory needed to be added for this.
 *
 * [wallpaperRepository] backs the wallpaper/personalize feature: it persists
 * the selected background (built-in or a gallery photo copied into private
 * storage) via DataStore. [paletteRepository] similarly persists the chosen
 * color palette. [settingsRepository] (Stage 8) persists notification/timer/
 * accessibility preferences and the daily study goal. [goalRepository]
 * (Study Goals) persists the weekly goal via Room. [subjectRepository]
 * (Subjects) persists user-created subjects via Room. [taskRepository]
 * (Tasks) persists tasks via Room. [plannedSessionRepository] (Study
 * Planner) persists planned study blocks via Room. Same lazy, no-DI pattern
 * as the rest of the class.
 *
 * Phase 12 (Notifications & Reminders): [applicationScope] is a process-wide
 * scope for work that must outlive any one screen — re-aiming the reminder
 * alarm ([ReminderScheduler.startObserving], started in [onCreate], which
 * also runs when the OS starts the process just to deliver an alarm or a
 * boot event) and the post-save goal notification ([GoalNotifier]). It uses a
 * [SupervisorJob] so one failed task can't cancel the rest.
 */
class StudySpaceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            database.studySessionDao(),
            onSessionRecorded = { session -> GoalNotifier.onSessionRecorded(this@StudySpaceApplication, applicationScope, session) }
        )
    }
    val goalRepository: GoalRepository by lazy { GoalRepository(database.studyGoalDao()) }
    val subjectRepository: SubjectRepository by lazy { SubjectRepository(database.subjectDao()) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val plannedSessionRepository: PlannedSessionRepository by lazy { PlannedSessionRepository(database.plannedSessionDao()) }
    val wallpaperRepository: WallpaperRepository by lazy { WallpaperRepository(this) }
    val paletteRepository: PaletteRepository by lazy { PaletteRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val dataTransferRepository: DataTransferRepository by lazy { DataTransferRepository(database) }

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannels(this)
        ReminderScheduler.startObserving(this, applicationScope)
    }
}
