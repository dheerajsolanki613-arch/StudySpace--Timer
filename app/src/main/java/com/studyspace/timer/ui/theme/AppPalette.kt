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

    // Phase 15 — see the comment above the color constants in Color.kt for why
    // these four exist: calmer, lower-saturation alternatives to the neon
    // Galaxy family, for people who want the same app with less visual noise.

    val deepSpace = AppPalette(
        id = "deep_space",
        label = "Deep Space",
        previewColors = listOf(DeepSpaceBackground, DeepSpaceSurface, DeepSpaceBlue),
        colorScheme = darkColorScheme(
            primary = DeepSpaceBlue,
            secondary = DeepSpaceCyan,
            tertiary = DeepSpaceSlate,
            background = DeepSpaceBackground,
            surface = DeepSpaceSurface,
            surfaceVariant = DeepSpaceSurface,
            outline = DeepSpaceOutline,
            onBackground = DeepSpaceText,
            onSurface = DeepSpaceText,
            onSurfaceVariant = DeepSpaceText,
            onPrimary = DeepSpaceBackground,
            onSecondary = DeepSpaceBackground,
            onTertiary = DeepSpaceBackground,
            primaryContainer = DeepSpaceBlue,
            onPrimaryContainer = DeepSpaceBackground,
        )
    )

    val midnight = AppPalette(
        id = "midnight",
        label = "Midnight",
        previewColors = listOf(MidnightBackground, MidnightSurface, MidnightIndigo),
        colorScheme = darkColorScheme(
            primary = MidnightIndigo,
            secondary = MidnightGray,
            tertiary = MidnightSlate,
            background = MidnightBackground,
            surface = MidnightSurface,
            surfaceVariant = MidnightSurface,
            outline = MidnightOutline,
            onBackground = MidnightText,
            onSurface = MidnightText,
            onSurfaceVariant = MidnightText,
            onPrimary = MidnightBackground,
            onSecondary = MidnightBackground,
            onTertiary = MidnightText,
            primaryContainer = MidnightIndigo,
            onPrimaryContainer = MidnightBackground,
        )
    )

    val minimalDark = AppPalette(
        id = "minimal_dark",
        label = "Minimal Dark",
        previewColors = listOf(MinimalDarkBackground, MinimalDarkSurface, MinimalDarkBlue),
        colorScheme = darkColorScheme(
            primary = MinimalDarkBlue,
            secondary = MinimalDarkGray,
            tertiary = MinimalDarkPaleGray,
            background = MinimalDarkBackground,
            surface = MinimalDarkSurface,
            surfaceVariant = MinimalDarkSurface,
            outline = MinimalDarkOutline,
            onBackground = MinimalDarkText,
            onSurface = MinimalDarkText,
            onSurfaceVariant = MinimalDarkText,
            onPrimary = MinimalDarkBackground,
            onSecondary = MinimalDarkBackground,
            onTertiary = MinimalDarkBackground,
            primaryContainer = MinimalDarkBlue,
            onPrimaryContainer = MinimalDarkBackground,
        )
    )

    val minimalLight = AppPalette(
        id = "minimal_light",
        label = "Light",
        previewColors = listOf(MinimalLightBackground, MinimalLightSurface, MinimalLightBlue),
        isDark = false,
        colorScheme = lightColorScheme(
            primary = MinimalLightBlue,
            secondary = MinimalLightGray,
            tertiary = MinimalLightPaleBlue,
            background = MinimalLightBackground,
            surface = MinimalLightSurface,
            surfaceVariant = MinimalLightSurface,
            outline = MinimalLightOutline,
            onBackground = MinimalLightText,
            onSurface = MinimalLightText,
            onSurfaceVariant = MinimalLightText,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = MinimalLightText,
            primaryContainer = MinimalLightBlue,
            onPrimaryContainer = Color.White,
        )
    )

    val all: List<AppPalette> = listOf(
        galaxy, nebulaBlue, twilightLavender, solarFlare, emeraldNova, crimsonNebula, kawaiiPastel,
        deepSpace, midnight, minimalDark, minimalLight
    )

    fun byId(id: String?): AppPalette = all.find { it.id == id } ?: galaxy
}

/**
 * Phase 15 — Accent customization. When [accentArgb] is set, returns a copy
 * of [AppPalette.colorScheme] with just the accent-carrying roles (primary,
 * primaryContainer, secondary) swapped for the custom color; background,
 * surface, text and outline are left exactly as the base palette defined
 * them, so a custom accent personalizes the app without ever touching the
 * colors "Do not sacrifice readability for visual effects" is really about.
 * `onPrimary`/`onSecondary`/`onPrimaryContainer` are recomputed via
 * [contrastSafeContentColor] rather than guessed, since — unlike a
 * hand-picked palette color — a user-chosen hue can't be pre-verified for
 * contrast; this applies the same safety net
 * [com.studyspace.timer.ui.components.PrimaryButton] already relies on, at
 * the source instead of only at the button.
 */
fun AppPalette.effectiveColorScheme(accentArgb: Int?): ColorScheme {
    if (accentArgb == null) return colorScheme
    val accent = Color(accentArgb)
    val onAccent = contrastSafeContentColor(background = accent, preferred = colorScheme.onPrimary)
    return colorScheme.copy(
        primary = accent,
        primaryContainer = accent,
        secondary = accent,
        onPrimary = onAccent,
        onSecondary = onAccent,
        onPrimaryContainer = onAccent
    )
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
