package com.studyspace.timer.wallpaper

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

private val Context.wallpaperDataStore by preferencesDataStore(name = "wallpaper_settings")

/** What the app background should currently render. */
sealed class WallpaperSelection {
    /** The base galaxy gradient defined in Theme.kt — no image. */
    data object Default : WallpaperSelection()

    /** One of the bundled [WallpaperCatalog] images. */
    data class BuiltIn(val wallpaper: BuiltInWallpaper) : WallpaperSelection()

    /** A photo the user picked from their own device gallery. */
    data class Custom(val filePath: String) : WallpaperSelection()
}

/**
 * Persists the user's chosen background (Stage 6 groundwork) and handles
 * importing a "Personalize" photo picked from the device gallery.
 *
 * Gallery photos are copied into the app's private files directory rather
 * than keeping the picked content:// URI directly. The Android Photo Picker
 * only guarantees read access to that URI until the next device restart, so
 * a copy is what makes the chosen wallpaper survive across app restarts and
 * reboots. Only one custom photo is kept at a time; picking a new one
 * replaces and deletes the previous file. No broad gallery/storage
 * permission is requested anywhere in this flow — the Photo Picker grants
 * access to just the single image the user selects.
 */
class WallpaperRepository(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("wallpaper_mode") // "default" | "built_in" | "custom"
        val BUILT_IN_ID = stringPreferencesKey("wallpaper_built_in_id")
        val CUSTOM_PATH = stringPreferencesKey("wallpaper_custom_path")
    }

    val selection: Flow<WallpaperSelection> = context.wallpaperDataStore.data.map { prefs ->
        when (prefs[Keys.MODE]) {
            "built_in" -> {
                val builtIn = WallpaperCatalog.findById(prefs[Keys.BUILT_IN_ID])
                if (builtIn != null) WallpaperSelection.BuiltIn(builtIn) else WallpaperSelection.Default
            }
            "custom" -> {
                val path = prefs[Keys.CUSTOM_PATH]
                if (path != null && File(path).exists()) {
                    WallpaperSelection.Custom(path)
                } else {
                    WallpaperSelection.Default
                }
            }
            else -> WallpaperSelection.Default
        }
    }

    suspend fun selectDefault() {
        context.wallpaperDataStore.edit { it[Keys.MODE] = "default" }
    }

    suspend fun selectBuiltIn(wallpaper: BuiltInWallpaper) {
        context.wallpaperDataStore.edit { prefs ->
            prefs[Keys.MODE] = "built_in"
            prefs[Keys.BUILT_IN_ID] = wallpaper.id
        }
    }

    /**
     * Copies [uri] (from the system Photo Picker) into private app storage
     * and saves it as the active custom wallpaper. Returns true on success.
     */
    suspend fun selectCustomFromGallery(uri: Uri): Boolean {
        val previousPath = currentCustomPathOrNull()
        val targetFile = File(wallpaperDir(), "custom_wallpaper_${System.currentTimeMillis()}.jpg")
        val copied = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }

        if (!copied || !targetFile.exists() || targetFile.length() == 0L) {
            targetFile.delete()
            return false
        }

        context.wallpaperDataStore.edit { prefs ->
            prefs[Keys.MODE] = "custom"
            prefs[Keys.CUSTOM_PATH] = targetFile.absolutePath
        }

        if (previousPath != null && previousPath != targetFile.absolutePath) {
            File(previousPath).delete()
        }
        return true
    }

    private suspend fun currentCustomPathOrNull(): String? =
        context.wallpaperDataStore.data.firstOrNull()?.get(Keys.CUSTOM_PATH)

    /**
     * Explicitly removes the personalized gallery photo: deletes the private
     * copy on disk and falls back to the default gradient. Distinct from
     * [selectDefault], which just switches the active mode — this also frees
     * the stored file, so the app doesn't keep an old personal photo around
     * once the user says they're done with it.
     */
    suspend fun clearCustom() {
        val path = currentCustomPathOrNull()
        context.wallpaperDataStore.edit { prefs ->
            prefs[Keys.MODE] = "default"
            prefs.remove(Keys.CUSTOM_PATH)
        }
        if (path != null) {
            File(path).delete()
        }
    }

    private fun wallpaperDir(): File =
        File(context.filesDir, "wallpapers").apply { mkdirs() }
}
