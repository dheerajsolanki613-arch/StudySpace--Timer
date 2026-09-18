package com.studyspace.timer.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyspace.timer.navigation.Screen
import com.studyspace.timer.ui.util.isLandscape

private data class BottomNavEntry(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavEntries = listOf(
    BottomNavEntry(Screen.Home, "Home", Icons.Filled.Home),
    BottomNavEntry(Screen.Timer, "Timer", Icons.Filled.Timer),
    BottomNavEntry(Screen.Analytics, "Analytics", Icons.Filled.Analytics),
    BottomNavEntry(Screen.Themes, "Themes", Icons.Filled.Palette),
    BottomNavEntry(Screen.Settings, "Settings", Icons.Filled.Settings)
)

/**
 * Width below which showing all 5 labels alongside icons gets cramped
 * enough to risk overlap/truncation — e.g. a phone in landscape with a
 * short edge under ~360dp, or any device with enlarged system font scale.
 * Below it, labels hide and only icons + selection indicator show; each
 * item still reports its label via `contentDescription` for accessibility,
 * so nothing is lost, just not drawn as text.
 */
private val COMPACT_LABEL_THRESHOLD = 360.dp

/** Material3's default [NavigationBar] height (used in portrait). */
private val PORTRAIT_BAR_HEIGHT = 80.dp

/**
 * Landscape height: Material3's default 80dp is sized for a portrait
 * phone's generous bottom inset and was never adjusted for the much
 * shorter total height a phone has in landscape, which is the "bottom
 * navigation is too tall" bug — nearly a quarter of a ~360dp-tall
 * landscape window otherwise goes to the bar alone. 56dp still comfortably
 * fits a 24dp icon plus touch padding.
 */
private val LANDSCAPE_BAR_HEIGHT = 56.dp

/**
 * Bottom navigation bar. [NavigationBar] already distributes its
 * [NavigationBarItem] children with equal weight/spacing on its own — that
 * was never the bug. What could overlap is icon+label *within* one item on
 * a narrow bar (5 destinations is a lot), so this measures the actual
 * available width with [BoxWithConstraints] and drops to icon-only below
 * [COMPACT_LABEL_THRESHOLD] instead of letting text wrap or get clipped.
 *
 * Height is explicitly set (rather than left at Material3's default) and
 * switches with [isLandscape]: [PORTRAIT_BAR_HEIGHT] normally,
 * [LANDSCAPE_BAR_HEIGHT] in landscape, where vertical space is scarce and
 * labels are usually already hidden by the width check above.
 *
 * Colors are theme-aware: the active tab's pill uses `primary` with
 * `onPrimary` content, matching both the kawaii mockup's lavender-pill/
 * indigo-icon Home tab and every dark galaxy palette's neon-accent look —
 * same role mapping, per-palette values.
 *
 * `alwaysShowLabel` tracks `showLabels` rather than being hardcoded `true`:
 * when labels are hidden (narrow bar), each item has no `label` composable
 * at all, so `alwaysShowLabel = true` would otherwise leave Material3
 * reserving layout space for a label slot that's never drawn — the actual
 * cause of icons sitting slightly off-center/misaligned in icon-only mode.
 * Tying the two together keeps icon-only items laid out purely around the
 * icon, and keeps every label visible (not just the selected item's) once
 * there's room to show them.
 */
@Composable
fun StudySpaceBottomNav(
    currentRoute: String?,
    locked: Boolean = false,
    onNavigate: (Screen) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val barHeight = if (isLandscape()) LANDSCAPE_BAR_HEIGHT else PORTRAIT_BAR_HEIGHT
    BoxWithConstraints {
        val showLabels = maxWidth >= COMPACT_LABEL_THRESHOLD
        NavigationBar(
            containerColor = scheme.surface,
            contentColor = scheme.onSurface,
            modifier = Modifier.height(barHeight)
        ) {
            bottomNavEntries.forEach { entry ->
                val selected = currentRoute == entry.screen.route
                NavigationBarItem(
                    selected = selected,
                    // Strict Focus Mode's navigation lock: while
                    // [com.studyspace.timer.timer.FocusLockController] is
                    // engaged, every tab -- including the currently
                    // selected one -- is disabled, so a tap can't pop the
                    // Focus screen off the back stack via bottom nav. The
                    // system back gesture is separately intercepted inside
                    // `FocusScreen` itself; this is the other half of "the
                    // locked state must be controlled by a reliable shared
                    // state ... not just visual button disabling" -- both
                    // read the same [FocusLockController.isLocked] source
                    // of truth rather than each keeping its own flag.
                    enabled = !locked,
                    onClick = { onNavigate(entry.screen) },
                    icon = { Icon(imageVector = entry.icon, contentDescription = entry.label) },
                    label = if (showLabels) {
                        {
                            Text(
                                text = entry.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        null
                    },
                    alwaysShowLabel = showLabels,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.onPrimary,
                        selectedTextColor = scheme.onPrimary,
                        unselectedIconColor = scheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = scheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = scheme.primary
                    )
                )
            }
        }
    }
}
