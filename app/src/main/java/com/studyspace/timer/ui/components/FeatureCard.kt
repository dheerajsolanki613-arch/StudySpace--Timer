package com.studyspace.timer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyspace.timer.ui.theme.GalaxyStarWhite

/**
 * Quick-action tile used on the Home dashboard grid (Self-Study, Online Study,
 * Normal Timer, Pomodoro, Focus Mode). Each tile has its own accent color so
 * the dashboard reads as "colorful illustrated icons" rather than one flat list.
 *
 * Stage 8 accessibility pass, two real fixes (not new decoration):
 *  - Tap feedback was previously suppressed (`indication = null` with a
 *    manually supplied interaction source); switched to the default
 *    `clickable(onClick = onClick)` overload, which draws the standard
 *    Material ripple via `LocalIndication`, same as every other clickable
 *    surface in this app.
 *  - The icon's `contentDescription` duplicated the visible title text right
 *    below it, so a screen reader announced the same word twice. The icon is
 *    now marked decorative (`contentDescription = null`) and the whole card
 *    carries one merged description ("title. subtitle") instead.
 */
@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" }
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor
                )
            }
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = GalaxyStarWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalaxyStarWhite.copy(alpha = 0.65f)
                )
            }
        }
    }
}
