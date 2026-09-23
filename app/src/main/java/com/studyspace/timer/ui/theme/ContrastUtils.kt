package com.studyspace.timer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Picks a readable text/icon color for [background]: keeps [preferred] if
 * it already clears WCAG AA (4.5:1) against it, otherwise falls back to
 * whichever of pure white/near-black actually contrasts better against
 * that exact color — so a button (or, since Phase 15, a custom accent
 * color) never ships unreadable text, for any palette or color, including
 * ones added or chosen later.
 *
 * Originally private to `ui/components/Buttons.kt`; pulled out here (Phase
 * 15) so accent customization's [AppPalette.effectiveColorScheme] can reuse
 * the exact same rule rather than a second, possibly-drifting copy of it.
 */
fun contrastSafeContentColor(background: Color, preferred: Color): Color {
    fun contrastRatio(foreground: Color, bg: Color): Float {
        val l1 = foreground.luminance() + 0.05f
        val l2 = bg.luminance() + 0.05f
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    if (contrastRatio(preferred, background) >= 4.5f) return preferred

    val white = Color.White
    val nearBlack = Color(0xFF10101F)
    return if (contrastRatio(white, background) >= contrastRatio(nearBlack, background)) white else nearBlack
}
