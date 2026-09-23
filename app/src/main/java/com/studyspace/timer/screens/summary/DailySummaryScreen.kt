package com.studyspace.timer.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.repository.DailySummary
import com.studyspace.timer.data.repository.dailySummaryDateLabel
import com.studyspace.timer.timer.formatDurationHoursMinutes
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.GlassCardAccent
import com.studyspace.timer.ui.components.GoalProgressCard
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.components.StatMiniCard
import java.time.LocalDate

/**
 * Phase 11 — Daily Summary. An on-demand, read-only recap of one day:
 * total study time, session count, subject count, goal progress, top
 * subject, tasks completed — exactly the fields the spec lists, each a real
 * number from stored data. "Optional" in the spec's sense: nothing pops it
 * up or notifies about it (a notification for it belongs to Phase 12's
 * opt-in reminders); the user opens it from Home when they want it.
 *
 * Previous/next-day arrows let the user look back at earlier days (the next
 * arrow is disabled on today). A day with nothing recorded says so plainly
 * and still shows honest zeros — no encouragement text, no comparison to
 * other days, no "score".
 */
@Composable
fun DailySummaryScreen(modifier: Modifier = Modifier, viewModel: DailySummaryViewModel = viewModel()) {
    val summary by viewModel.summary.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }
    val today = LocalDate.now()
    val isToday = summary.date == today

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title = "Daily Summary") }
        item {
            DayNavigator(
                label = dailySummaryDateLabel(summary.date, today),
                isToday = isToday,
                onPrevious = viewModel::showPreviousDay,
                onNext = viewModel::showNextDay,
                onToday = viewModel::showToday
            )
        }
        item { TotalCard(summary = summary, isToday = isToday) }
        if (!summary.hasActivity) {
            item { NoActivityNote(isToday = isToday) }
        }
        item { CountsRow(summary = summary) }
        item {
            TopSubjectCard(
                subject = summary.topSubject?.let { subjectsById[it.subjectId] },
                totalMillis = summary.topSubject?.totalMillis ?: 0L
            )
        }
        item { GoalProgressCard(progress = summary.goal) }
        if (!isToday) {
            item {
                Text(
                    text = "Measured against your current daily goal — past goal targets aren't stored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DayNavigator(
    label: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Previous day")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (!isToday) {
                TextButton(onClick = onToday) {
                    Text(text = "Back to today", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        // Disabled (not hidden) on today so the row's layout doesn't shift.
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun TotalCard(summary: DailySummary, isToday: Boolean) {
    GlassCardAccent(modifier = Modifier.fillMaxWidth(), accentColor = MaterialTheme.colorScheme.secondary) {
        Column {
            Text(
                text = if (isToday) "Total study time today" else "Total study time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = formatDurationHoursMinutes(summary.totalMillis),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NoActivityNote(isToday: Boolean) {
    Text(
        text = if (isToday) {
            "Nothing recorded yet today."
        } else {
            "No study sessions or completed tasks were recorded on this day."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
private fun CountsRow(summary: DailySummary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatMiniCard(title = "Sessions", value = summary.sessionCount.toString(), modifier = Modifier.weight(1f))
        StatMiniCard(title = "Subjects", value = summary.subjectCount.toString(), modifier = Modifier.weight(1f))
        StatMiniCard(title = "Tasks done", value = summary.tasksCompleted.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TopSubjectCard(subject: SubjectEntity?, totalMillis: Long) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "TOP SUBJECT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            if (subject == null) {
                Text(
                    text = "No sessions with a subject",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(subject.colorArgb).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = subject.icon, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                    Text(
                        text = formatDurationHoursMinutes(totalMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
