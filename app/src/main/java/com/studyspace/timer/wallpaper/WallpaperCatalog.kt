package com.studyspace.timer.wallpaper

import com.studyspace.timer.R

/**
 * One entry in the built-in wallpaper set (Stage 6 groundwork, added early at
 * the user's request alongside the "personalize from gallery" feature).
 *
 * [id] is the stable string stored in DataStore — it must never change once
 * shipped, or existing users' saved selection would silently fall back to
 * the default background. The drawable resource itself can be swapped freely.
 */
data class BuiltInWallpaper(
    val id: String,
    val label: String,
    val drawableRes: Int
)

/**
 * The fixed set of bundled wallpapers (res/drawable-nodpi/wallpaper_XX.webp).
 * These are the user's own curated reference images, compressed to WebP and
 * bundled as app assets — not fetched or generated at runtime.
 */
object WallpaperCatalog {
    val builtIns: List<BuiltInWallpaper> = listOf(
        BuiltInWallpaper("wallpaper_01", "Crimson Spires", R.drawable.wallpaper_01),
        BuiltInWallpaper("wallpaper_02", "Violet Skyline", R.drawable.wallpaper_02),
        BuiltInWallpaper("wallpaper_03", "Nebula Drift", R.drawable.wallpaper_03),
        BuiltInWallpaper("wallpaper_04", "Deep Orbit", R.drawable.wallpaper_04),
        BuiltInWallpaper("wallpaper_05", "Aurora Path", R.drawable.wallpaper_05),
        BuiltInWallpaper("wallpaper_06", "Starfall", R.drawable.wallpaper_06),
        BuiltInWallpaper("wallpaper_07", "Lunar Glow", R.drawable.wallpaper_07),
        BuiltInWallpaper("wallpaper_08", "Cosmic Tide", R.drawable.wallpaper_08),
        BuiltInWallpaper("wallpaper_09", "Twilight Peak", R.drawable.wallpaper_09),
        BuiltInWallpaper("wallpaper_10", "Nightfall City", R.drawable.wallpaper_10),
        BuiltInWallpaper("wallpaper_11", "Violet Horizon", R.drawable.wallpaper_11),
        BuiltInWallpaper("wallpaper_12", "Galaxy Bloom", R.drawable.wallpaper_12),
        BuiltInWallpaper("wallpaper_13", "Ember Sky", R.drawable.wallpaper_13),
        BuiltInWallpaper("wallpaper_14", "Silent Cosmos", R.drawable.wallpaper_14),
        BuiltInWallpaper("wallpaper_15", "Distant Stars", R.drawable.wallpaper_15),
        BuiltInWallpaper("wallpaper_16", "Astral Dusk", R.drawable.wallpaper_16)
    )

    fun findById(id: String?): BuiltInWallpaper? = builtIns.find { it.id == id }
}
