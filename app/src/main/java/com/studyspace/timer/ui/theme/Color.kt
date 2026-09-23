package com.studyspace.timer.ui.theme

import androidx.compose.ui.graphics.Color

// Galaxy / twilight palette — refined further in Stage 6 (theme system).
val GalaxyDeepSpace = Color(0xFF0B0B2E)
val GalaxyTwilightPurple = Color(0xFF1A1440)
val GalaxyNebulaBlue = Color(0xFF2E3192)
val GalaxyNeonPink = Color(0xFFFF6EC7)
val GalaxyNeonCyan = Color(0xFF5CE1E6)
val GalaxyStarWhite = Color(0xFFF5F5FF)
val GalaxyMutedLavender = Color(0xFFB8AEE0)
val GalaxyGlassSurface = Color(0x33FFFFFF) // translucent glass-card fill

// Extra accent colors for the additional palettes in ui/theme/AppPalette.kt.
// Backgrounds/surfaces are reused from the set above so every palette stays
// within the same dark "galaxy" depth — only the accent hues change.
val GalaxySolarOrange = Color(0xFFFF9E5E)
val GalaxySolarRose = Color(0xFFFF5C8A)
val GalaxyEmeraldGreen = Color(0xFF4DE8A0)
val GalaxyEmeraldTeal = Color(0xFF2FB8B8)
val GalaxyCrimsonRed = Color(0xFFFF4D6D)
val GalaxyCrimsonMagenta = Color(0xFFD946B8)

// Kawaii Pastel palette — the one light (non-"galaxy") palette in the app,
// matching the reference mockup: a soft lavender-to-cream sunset gradient
// behind a cream card with espresso-brown text, a periwinkle-lavender accent,
// and a terracotta secondary accent. See AppPalette.kt's `kawaiiPastel` for
// how these compose into the actual ColorScheme.
val KawaiiCream = Color(0xFFFDF6ED)          // card fill / lightest surface
val KawaiiBackground = Color(0xFFF3E4D0)     // base background (bottom of the sunset gradient)
val KawaiiPeach = Color(0xFFF6D9BE)          // gradient stop 3
val KawaiiRose = Color(0xFFF0C3C4)           // gradient stop 2 / card border
val KawaiiLavenderSky = Color(0xFFD6C6EC)    // gradient stop 1 (top of sky)
val KawaiiLavender = Color(0xFFC3B4EA)       // primary accent (Focus Mode pill, active nav)
val KawaiiIndigo = Color(0xFF5B4B8A)         // on-lavender text/icon
val KawaiiEspresso = Color(0xFF5A3A2A)       // primary text (headings, timer digits)
val KawaiiMocha = Color(0xFF6B4038)          // primary-action button fill (Pause/Start)
val KawaiiTaupe = Color(0xFF8B6F5C)          // secondary/muted text
val KawaiiTerracotta = Color(0xFFD98878)     // secondary accent (mug, warm highlights)
val KawaiiChickYellow = Color(0xFFF5D576)    // tertiary accent

// Phase 15 — four additional palettes, deliberately more restrained than the
// neon Galaxy family: less saturated accents, higher text contrast, aimed at
// people who want the same app but a calmer, more "focus room" look. Each
// still keeps a cosmic name/spirit, but none of them lean on hot neon pink or
// cyan the way Galaxy/Nebula Blue/Crimson Nebula do.

// Deep Space — a calmer, cooler dark theme: soft indigo-blue instead of neon.
val DeepSpaceBackground = Color(0xFF0B0E1A)
val DeepSpaceSurface = Color(0xFF141A2E)
val DeepSpaceOutline = Color(0xFF2E3652)
val DeepSpaceBlue = Color(0xFF6C8CFF)
val DeepSpaceCyan = Color(0xFF4FD1E8)
val DeepSpaceSlate = Color(0xFF8892B0)
val DeepSpaceText = Color(0xFFE3E6F0)

// Midnight — near-black/OLED-friendly, almost grayscale but for one indigo accent.
val MidnightBackground = Color(0xFF000000)
val MidnightSurface = Color(0xFF0D0D10)
val MidnightOutline = Color(0xFF2A2A30)
val MidnightIndigo = Color(0xFF7C93FF)
val MidnightGray = Color(0xFFB8BCC8)
val MidnightSlate = Color(0xFF5A5F73)
val MidnightText = Color(0xFFF2F2F5)

// Minimal Dark — the most restrained dark option: neutral charcoal (no blue/
// purple tint), muted single-hue accent, built for low visual noise and high
// text contrast rather than atmosphere. Feeds directly into Phase 16
// (Accessibility) as the "give me the plainest readable dark theme" choice.
val MinimalDarkBackground = Color(0xFF1A1B1E)
val MinimalDarkSurface = Color(0xFF232427)
val MinimalDarkOutline = Color(0xFF3C3D40)
val MinimalDarkBlue = Color(0xFF8AB4F8)
val MinimalDarkGray = Color(0xFFA8ABB3)
val MinimalDarkPaleGray = Color(0xFFC7C9CE)
val MinimalDarkText = Color(0xFFEDEDEF)

// Minimal Light — a plain, neutral light theme (distinct from the whimsical,
// warm-toned Kawaii Pastel): near-white background, near-black text, one
// muted blue accent. The "just give me a normal light mode" option.
val MinimalLightBackground = Color(0xFFFAFAFA)
val MinimalLightSurface = Color(0xFFFFFFFF)
val MinimalLightOutline = Color(0xFFDDDDE1)
val MinimalLightBlue = Color(0xFF3B5BDB)
val MinimalLightGray = Color(0xFF495057)
val MinimalLightPaleBlue = Color(0xFF748FFC)
val MinimalLightText = Color(0xFF1A1B1E)
