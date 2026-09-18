package com.studyspace.timer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * One selectable app-wide color palette: a Material [ColorScheme] plus the
 * gradient colors used both to preview it as a swatch on the Themes screen
 * and to paint the app-wide [com.studyspace.timer.ui.components.AppBackground]
 * when the wallpaper mode is [com.studyspace.timer.wallpaper.WallpaperSelection.Default].
 *
 * Every palette used to ship dark (galaxy aesthetic was dark-first by
 * design). [isDark] now distinguishes that from the light, cream-and-pastel
 * "Kawaii Pastel" palette, so components that need different *styling* (not
 * just different colors) — [com.studyspace.timer.ui.components.GlassCard]'s
 * translucent-glass-vs-solid-card fill being the main one — can branch on it
 * instead of assuming dark.
 */
data class AppPalette(
    val id: String,
    val label: String,
    val previewColors: List<Color>,
    val colorScheme: ColorScheme,
    val isDark: Boolean = true
)

object AppPalettes {
    val galaxy = AppPalette(
        id = "galaxy",
        label = "Galaxy (default)",
        previewColors = listOf(GalaxyDeepSpace, GalaxyTwilightPurple, GalaxyNeonPink),
        colorScheme = darkColorScheme(
            primary = GalaxyNeonPink,
            secondary = GalaxyNeonCyan,
            tertiary = GalaxyMutedLavender,
            background = GalaxyDeepSpace,
            surface = GalaxyTwilightPurple,
            surfaceVariant = GalaxyTwilightPurple,
            outline = GalaxyMutedLavender,
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onSurfaceVariant = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
            onTertiary = GalaxyDeepSpace,
            primaryContainer = GalaxyNeonPink,
            onPrimaryContainer = GalaxyDeepSpace,
        )
    )

    val nebulaBlue = AppPalette(
        id = "nebula_blue",
        label = "Nebula Blue",
        previewColors = listOf(GalaxyDeepSpace, GalaxyTwilightPurple, GalaxyNeonCyan),
        colorScheme = darkColorScheme(
            primary = GalaxyNeonCyan,
            secondary = GalaxyNebulaBlue,
            tertiary = GalaxyMutedLavender,
            background = GalaxyDeepSpace,
            surface = GalaxyNebulaBlue.copy(alpha = 0.55f).compositeOverDeepSpace(),
            surfaceVariant = GalaxyNebulaBlue.copy(alpha = 0.55f).compositeOverDeepSpace(),
            outline = GalaxyMutedLavender,
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onSurfaceVariant = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyStarWhite,
            onTertiary = GalaxyDeepSpace,
            primaryContainer = GalaxyNeonCyan,
            onPrimaryContainer = GalaxyDeepSpace,
        )
    )

    val twilightLavender = AppPalette(
        id = "twilight_lavender",
        label = "Twilight Lavender",
        previewColors = listOf(GalaxyTwilightPurple, GalaxyMutedLavender, GalaxyNeonPink),
        colorScheme = darkColorScheme(
            primary = GalaxyMutedLavender,
            secondary = GalaxyNeonPink,
            tertiary = GalaxyNeonCyan,
            background = GalaxyTwilightPurple,
            surface = GalaxyDeepSpace,
            surfaceVariant = GalaxyDeepSpace,
            outline = GalaxyMutedLavender,
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onSurfaceVariant = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
            onTertiary = GalaxyDeepSpace,
            primaryContainer = GalaxyMutedLavender,
            onPrimaryContainer = GalaxyDeepSpace,
        )
    )

    val solarFlare = AppPalette(
        id = "solar_flare",
        label = "Solar Flare",
        previewColors = listOf(GalaxyDeepSpace, GalaxySolarRose, GalaxySolarOrange),
        colorScheme = darkColorScheme(
            primary = GalaxySolarOrange,
            secondary = GalaxySolarRose,
            tertiary = GalaxyMutedLavender,
            background = GalaxyDeepSpace,
            surface = GalaxyTwilightPurple,
            surfaceVariant = GalaxyTwilightPurple,
            outline = GalaxyMutedLavender,
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onSurfaceVariant = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
            onTertiary = GalaxyDeepSpace,
            primaryContainer = GalaxySolarOrange,
            onPrimaryContainer = GalaxyDeepSpace,
        )
    )

    val emeraldNova = AppPalette(
        id = "emerald_nova",
        label = "Emerald Nova",
        previewColors = listOf(GalaxyDeepSpace, GalaxyEmeraldTeal, GalaxyEmeraldGreen),
        colorScheme = darkColorScheme(
            primary = GalaxyEmeraldGreen,
            secondary = GalaxyEmeraldTeal,
            tertiary = GalaxyMutedLavender,
            background = GalaxyDeepSpace,
            surface = GalaxyTwilightPurple,
            surfaceVariant = GalaxyTwilightPurple,
            outline = GalaxyMutedLavender,
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onSurfaceVariant = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
            onTertiary = GalaxyDeepSpace,
            primaryContainer = GalaxyEmeraldGreen,
            onPrimaryContainer = GalaxyDeepSpace,
        )
    )

    val crimsonNebula = AppPalette(
        id = "crimson_nebula",
        label = "Crimson Nebula",
        previewColors = listOf(GalaxyTwilightPurple, GalaxyCrimsonMagenta, GalaxyCrimsonRed),
        colorScheme = darkColorScheme(
            primary = GalaxyCrimsonRed,
            secondary = GalaxyCrimsonMagenta,
            tertiary = GalaxyNeonCyan,
            background = GalaxyDeepSpace,
            surface = GalaxyTwilightPurple,
            surfaceVariant = GalaxyTwilightPurple,
            outline = GalaxyNeonCyan,
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onSurfaceVariant = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyStarWhite,
            onTertiary = GalaxyDeepSpace,
            primaryContainer = GalaxyCrimsonRed,
            onPrimaryContainer = GalaxyDeepSpace,
        )
    )

    /**
     * Light, cream-and-pastel palette matching the kawaii mascot reference
     * mockup: soft lavender-to-cream sunset gradient behind an opaque cream
     * card, espresso-brown text, a periwinkle-lavender accent (Focus Mode
     * pill, active bottom-nav tab), and a deep mocha primary-action color
     * (Start/Pause buttons) — see the Kawaii* constants in Color.kt for the
     * exact values pulled from the reference image.
     */
    val kawaiiPastel = AppPalette(
        id = "kawaii_pastel",
        label = "Kawaii Pastel",
        previewColors = listOf(KawaiiLavenderSky, KawaiiRose, KawaiiPeach, KawaiiBackground),
        isDark = false,
        colorScheme = lightColorScheme(
            primary = KawaiiLavender,
            secondary = KawaiiTerracotta,
            tertiary = KawaiiChickYellow,
            background = KawaiiBackground,
            surface = KawaiiCream,
            surfaceVariant = KawaiiRose,
            outline = KawaiiRose,
            onBackground = KawaiiEspresso,
            onSurface = KawaiiEspresso,
            onSurfaceVariant = KawaiiEspresso,
            onPrimary = KawaiiIndigo,
            onSecondary = Color.White,
            onTertiary = KawaiiEspresso,
            primaryContainer = KawaiiMocha,
            onPrimaryContainer = Color.White,
        )
    )

    val all: List<AppPalette> = listOf(
        galaxy, nebulaBlue, twilightLavender, solarFlare, emeraldNova, crimsonNebula, kawaiiPastel
    )

    fun byId(id: String?): AppPalette = all.find { it.id == id } ?: galaxy
}

/** Flattens a translucent color onto [GalaxyDeepSpace] so it's opaque enough to use as a surface color. */
private fun Color.compositeOverDeepSpace(): Color {
    val bg = GalaxyDeepSpace
    val a = this.alpha
    return Color(
        red = red * a + bg.red * (1 - a),
        green = green * a + bg.green * (1 - a),
        blue = blue * a + bg.blue * (1 - a),
        alpha = 1f
    )
}
