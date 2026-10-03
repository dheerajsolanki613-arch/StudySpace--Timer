package com.studyspace.timer.screens.subjects

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.repository.SubjectStats
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.ui.components.GlassCardAccent
import com.studyspace.timer.ui.components.StatMiniCard
import androidx.compose.ui.graphics.Color

/**
 * One subject's detail screen: a colored header card (icon, name, edit
 * button) and the stats grid the spec asks for (total, today, week, month,
 * sessions, average session, streak). Rename/delete both go through
 * [SubjectEditorDialog] behind the header's edit button.
 */
@Composable
fun SubjectDetailScreen(subjectId: Long, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SubjectDetailViewModel = viewModel(
        factory = SubjectDetailViewModelFactory(application, subjectId)
    )
    val subject by viewModel.subject.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    val current = subject
    if (current == null) {
        // Either still loading, or the subject was just deleted — either
        // way there's nothing to show, so hand control back rather than
        // rendering a header for data that doesn't exist.
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCardAccent(
                modifier = Modifier.fillMaxWidth(),
                accentColor = Color(current.colorArgb)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = current.icon, style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = current.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit subject")
                    }
                }
            }
        }
        item { StatsGrid(stats) }
    }

    if (showEditDialog) {
        SubjectEditorDialog(
            title = "Edit subject",
            initialName = current.name,
            initialIcon = current.icon,
            initialColorArgb = current.colorArgb,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, icon, colorArgb ->
                viewModel.rename(name, icon, colorArgb)
                showEditDialog = false
            },
            onDelete = {
                showEditDialog = false
                viewModel.delete(onDeleted = onBack)
            }
        )
    }
}

@Composable
private fun StatsGrid(stats: SubjectStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatMiniCard(title = "Total", value = formatDurationHoursMinutes(stats.totalMillis), modifier = Modifier.weight(1f))
            StatMiniCard(title = "Today", value = formatDurationHoursMinutes(stats.todayMillis), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatMiniCard(title = "This week", value = formatDurationHoursMinutes(stats.weekMillis), modifier = Modifier.weight(1f))
            StatMiniCard(title = "This month", value = formatDurationHoursMinutes(stats.monthMillis), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatMiniCard(title = "Sessions", value = stats.sessionCount.toString(), modifier = Modifier.weight(1f))
            StatMiniCard(title = "Avg session", value = formatDurationHoursMinutes(stats.averageSessionMillis), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatMiniCard(title = "Streak", value = "${stats.streakDays} days", modifier = Modifier.weight(1f))
        }
    }
}
