package com.studyspace.timer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Small labeled-number card ("Sessions" / "3", "Streak" / "7 days", ...).
 * Originally private to `HomeScreen.kt`'s stats row; made a shared
 * component (Subject phase) so Subject Detail's stats grid uses the exact
 * same look instead of a second copy of the same three lines of Compose.
 */
@Composable
fun StatMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
