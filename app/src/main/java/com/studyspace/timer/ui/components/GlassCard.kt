package com.studyspace.timer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studyspace.timer.ui.theme.GalaxyGlassSurface
import com.studyspace.timer.ui.theme.GalaxyMutedLavender
import com.studyspace.timer.ui.theme.GalaxyTwilightPurple

/**
 * Base "neon-glass" card used throughout the galaxy UI: translucent fill,
 * soft gradient, and a subtle lavender hairline border. Purely presentational —
 * wraps [content] with consistent padding/shape so screens don't repeat it.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    contentPadding: Int = 16,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GalaxyGlassSurface,
                        GalaxyTwilightPurple.copy(alpha = 0.55f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = GalaxyMutedLavender.copy(alpha = 0.25f),
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
    accentColor: Color = GalaxyMutedLavender,
    cornerRadius: Int = 24,
    contentPadding: Int = 20,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.22f),
                        GalaxyGlassSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(contentPadding.dp)
    ) {
        content()
    }
}
