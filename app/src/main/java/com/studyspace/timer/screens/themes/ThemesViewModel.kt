package com.studyspace.timer.screens.themes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.ui.theme.AppPalettes
import com.studyspace.timer.wallpaper.BuiltInWallpaper
import com.studyspace.timer.wallpaper.WallpaperSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Themes screen's two independent pickers:
 *  - Wallpaper (Default / bundled images / "Personalize" gallery photo), via
 *    [StudySpaceApplication.wallpaperRepository].
 *  - Palette (Material color scheme), via [StudySpaceApplication.paletteRepository].
 *
 * Also read from [com.studyspace.timer.MainActivity] (same Activity-scoped
 * instance, since Compose's `viewModel()` returns the same object for the
 * same owner) so the selected palette can be applied to the whole app's
 * [com.studyspace.timer.ui.theme.StudySpaceTimerTheme] wrapper, not just the
 * Themes screen itself.
 */
class ThemesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudySpaceApplication
    private val wallpaperRepository = app.wallpaperRepository
    private val paletteRepository = app.paletteRepository

    val selection: StateFlow<WallpaperSelection> = wallpaperRepository.selection.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WallpaperSelection.Default
    )

    val paletteId: StateFlow<String> = paletteRepository.selectedPaletteId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppPalettes.galaxy.id
    )

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    fun selectDefault() {
        viewModelScope.launch { wallpaperRepository.selectDefault() }
    }

    fun selectBuiltIn(wallpaper: BuiltInWallpaper) {
        viewModelScope.launch { wallpaperRepository.selectBuiltIn(wallpaper) }
    }

    /** Called with the Uri returned by the system Photo Picker, if any was picked. */
    fun onGalleryImagePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val success = wallpaperRepository.selectCustomFromGallery(uri)
            if (!success) {
                _importError.value = "Couldn't use that photo. Please try a different one."
            }
        }
    }

    /** Explicitly removes the personalized photo and reverts to the default background. */
    fun removeCustomPhoto() {
        viewModelScope.launch { wallpaperRepository.clearCustom() }
    }

    fun dismissImportError() {
        _importError.value = null
    }

    fun selectPalette(id: String) {
        viewModelScope.launch { paletteRepository.setPalette(id) }
    }
}
