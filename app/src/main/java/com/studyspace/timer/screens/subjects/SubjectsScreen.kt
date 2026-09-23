package com.studyspace.timer.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.repository.SubjectRepository
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.SectionHeader

/**
 * Subjects list: every subject the user has created, tap to open its
 * detail/stats screen, "+ Add" to create a new one via
 * [SubjectEditorDialog]. Renaming/deleting happens on the detail screen,
 * not here, so this list stays a simple browse-and-create surface.
 */
@Composable
fun SubjectsScreen(
    onOpenSubject: (Long) -> Unit,
    viewModel: SubjectsViewModel = viewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Subjects",
                actionLabel = "Add",
                onActionClick = { showAddDialog = true }
            )
        }
        if (subjects.isEmpty()) {
            item { EmptySubjectsCard(onAdd = { showAddDialog = true }) }
        } else {
            items(subjects, key = { it.id }) { subject ->
                SubjectRow(subject = subject, onClick = { onOpenSubject(subject.id) })
            }
        }
    }

    if (showAddDialog) {
        SubjectEditorDialog(
            title = "New subject",
            initialName = "",
            initialIcon = SubjectRepository.ICON_PRESETS.first(),
            initialColorArgb = SubjectRepository.COLOR_PRESETS.first(),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, icon, colorArgb ->
                viewModel.addSubject(name, icon, colorArgb)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptySubjectsCard(onAdd: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
    ) {
        Column {
            Text(
                text = "No subjects yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add a subject (like Mathematics or Physics) to start tracking study time by subject.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SubjectRow(subject: SubjectEntity, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val accent = Color(subject.colorArgb)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = subject.icon, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
