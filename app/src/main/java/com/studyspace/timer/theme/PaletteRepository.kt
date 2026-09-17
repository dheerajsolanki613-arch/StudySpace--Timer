package com.studyspace.timer.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studyspace.timer.ui.theme.AppPalettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

/**
 * Persists which [com.studyspace.timer.ui.theme.AppPalette] the user picked
 * on the Themes screen, so the choice survives app restarts. Separate
 * DataStore file from [com.studyspace.timer.wallpaper.WallpaperRepository]
 * since palette and wallpaper are independent choices that happen to live on
 * the same screen.
 */
class PaletteRepository(private val context: Context) {

    private object Keys {
        val PALETTE_ID = stringPreferencesKey("palette_id")
    }

    val selectedPaletteId: Flow<String> = context.themeDataStore.data.map { prefs ->
        prefs[Keys.PALETTE_ID] ?: AppPalettes.galaxy.id
    }

    suspend fun setPalette(id: String) {
        context.themeDataStore.edit { prefs -> prefs[Keys.PALETTE_ID] = id }
    }
}
