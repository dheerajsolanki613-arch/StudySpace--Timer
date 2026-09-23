package com.studyspace.timer.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studyspace.timer.data.repository.SubjectRepository

/**
 * Create-or-edit dialog for a subject: name field, a preset emoji row, and
 * a preset accent-color row — used by [SubjectsScreen] (create) and Subject
 * Detail (rename), so there's exactly one place this UI is built.
 */
@Composable
fun SubjectEditorDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, colorArgb: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColorArgb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectRepository.ICON_PRESETS.forEach { icon ->
                        FilterChip(
                            selected = selectedIcon == icon,
                            onClick = { selectedIcon = icon },
                            label = { Text(icon) }
                        )
                    }
                }
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubjectRepository.COLOR_PRESETS.forEach { colorArgb ->
                        ColorSwatch(
                            color = Color(colorArgb),
                            selected = selectedColor == colorArgb,
                            onClick = { selectedColor = colorArgb }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * A tappable color circle. Selection is shown by making the swatch larger
 * plus a contrasting ring — a size change rather than only a color-based
 * indicator, since color alone isn't a reliable signal for every user
 * (the project's own accessibility rule elsewhere: "no information should
 * depend on color alone").
 */
@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val size = if (selected) 40.dp else 32.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}
