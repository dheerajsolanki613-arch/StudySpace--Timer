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
