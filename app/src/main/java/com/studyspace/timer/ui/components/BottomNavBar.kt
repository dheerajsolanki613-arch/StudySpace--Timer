package com.studyspace.timer.ui.components

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
import com.studyspace.timer.navigation.Screen

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
 * Premium bottom navigation bar. Highlights the currently active top-level
 * destination and delegates the actual navigation call to [onNavigate].
 *
 * Colors are theme-aware: the active tab's pill uses `primary` with
 * `onPrimary` content, which is exactly how the reference kawaii mockup's
 * lavender-pill-with-indigo-icon Home tab reads, and how every dark galaxy
 * palette's neon-accent-on-near-black reads too — same role mapping, per
 * palette values.
 */
@Composable
fun StudySpaceBottomNav(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = scheme.surface,
        contentColor = scheme.onSurface
    ) {
        bottomNavEntries.forEach { entry ->
            val selected = currentRoute == entry.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(entry.screen) },
                icon = { Icon(imageVector = entry.icon, contentDescription = entry.label) },
                label = { Text(entry.label) },
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
