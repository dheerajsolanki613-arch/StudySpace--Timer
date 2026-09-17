package com.studyspace.timer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * One selectable app-wide color palette: a Material [ColorScheme] plus the
 * gradient colors used to preview it as a swatch on the Themes screen. All
 * three ship dark (galaxy aesthetic is dark-first by design); a palette
 * changes the accent/surface relationships, not the light/dark mode itself.
 */
data class AppPalette(
    val id: String,
    val label: String,
    val previewColors: List<Color>,
    val colorScheme: ColorScheme
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
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
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
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyStarWhite,
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
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
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
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
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
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyDeepSpace,
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
            onBackground = GalaxyStarWhite,
            onSurface = GalaxyStarWhite,
            onPrimary = GalaxyDeepSpace,
            onSecondary = GalaxyStarWhite,
        )
    )

    val all: List<AppPalette> = listOf(
        galaxy, nebulaBlue, twilightLavender, solarFlare, emeraldNova, crimsonNebula
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
