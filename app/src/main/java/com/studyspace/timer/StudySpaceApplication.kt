package com.studyspace.timer

import android.app.Application
import com.studyspace.timer.data.db.AppDatabase
import com.studyspace.timer.data.repository.SessionRepository
import com.studyspace.timer.settings.SettingsRepository
import com.studyspace.timer.theme.PaletteRepository
import com.studyspace.timer.wallpaper.WallpaperRepository

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
 * accessibility preferences and the daily study goal. Same lazy, no-DI
 * pattern as the rest of the class.
 */
class StudySpaceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database.studySessionDao()) }
    val wallpaperRepository: WallpaperRepository by lazy { WallpaperRepository(this) }
    val paletteRepository: PaletteRepository by lazy { PaletteRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
