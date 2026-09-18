package com.studyspace.timer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Base card used throughout the app: theme-aware fill, soft gradient, and a
 * hairline border. Purely presentational — wraps [content] with consistent
 * padding/shape so screens don't repeat it.
 *
 * Reads every color from `MaterialTheme.colorScheme` (set by the active
 * [com.studyspace.timer.ui.theme.AppPalette]) instead of the fixed `Galaxy*`
 * constants this used to hardcode, so switching palette actually re-colors
 * every card in the app, not just the Themes screen's own preview swatches.
 *
 * The dark "galaxy" palettes and the light "Kawaii Pastel" palette want
 * visibly different card *styling*, not just different colors — a
 * translucent neon-glass fill reads as "space" on a dark background, but
 * the same translucency on the pastel sunset background would look muddy
 * rather than the reference's solid cream card with a soft rose border. So
 * this branches on the color scheme's background luminance via [isDark]:
 * translucent gradient fill for dark palettes, a near-opaque solid fill for
 * light ones.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    contentPadding: Int = 16,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.isDark()

    val fillColors = if (dark) {
        listOf(scheme.surface.copy(alpha = 0.35f), scheme.surfaceVariant.copy(alpha = 0.55f))
    } else {
        listOf(scheme.surface.copy(alpha = 0.98f), scheme.surface.copy(alpha = 0.94f))
    }
    val borderColor = if (dark) scheme.outline.copy(alpha = 0.25f) else scheme.outline.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(brush = Brush.verticalGradient(colors = fillColors))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(contentPadding.dp)
    ) {
        content()
    }
}

/** Slightly more prominent variant for hero/dashboard-header cards. */
@Composable
fun GlassCardAccent(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.tertiary,
    cornerRadius: Int = 24,
    contentPadding: Int = 20,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.isDark()

    val fillColors = if (dark) {
        listOf(accentColor.copy(alpha = 0.22f), scheme.surface.copy(alpha = 0.35f))
    } else {
        listOf(accentColor.copy(alpha = 0.14f), scheme.surface.copy(alpha = 0.98f))
    }
    val borderAlpha = if (dark) 0.4f else 0.55f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(brush = Brush.linearGradient(colors = fillColors))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(contentPadding.dp)
    ) {
        content()
    }
}

/**
 * Whether this color scheme is one of the dark "galaxy" palettes rather
 * than a light one like Kawaii Pastel. Derived from background luminance
 * (instead of threading [com.studyspace.timer.ui.theme.AppPalette.isDark]
 * through every card call site) so any composable with access to
 * `MaterialTheme.colorScheme` can branch on it directly.
 */
internal fun ColorScheme.isDark(): Boolean = background.luminance() < 0.5f
