package com.studyspace.timer.screens.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyspace.timer.data.transfer.BackupContent
import com.studyspace.timer.data.transfer.ImportPlan
import com.studyspace.timer.data.transfer.ImportResult
import com.studyspace.timer.data.transfer.RestoreResult
import com.studyspace.timer.data.transfer.TransferFormat
import com.studyspace.timer.data.transfer.backupFileName
import com.studyspace.timer.data.transfer.exportFileName
import com.studyspace.timer.ui.components.GlassCard
import com.studyspace.timer.ui.components.PrimaryButton
import com.studyspace.timer.ui.components.SecondaryButton
import com.studyspace.timer.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val MAX_LISTED = 5
private val BACKUP_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

/**
 * Phase 13 (Export & Import of study history) and Phase 14 (Backup &
 * Restore), both reached from Settings → Data and both living on this one
 * screen since they're two views of the same underlying data-portability
 * feature.
 *
 * Export writes a CSV (for spreadsheets) or JSON (every detail) to a file the
 * user picks with the system's "save as" dialog. Import reads a file the user
 * picks, validates it, and shows exactly what it would add — *before*
 * anything is saved — behind an explicit confirm button. Import only ever
 * adds; it never changes or removes anything already in the app (see
 * `StudyDataImport.kt`).
 *
 * Backup writes *everything* the app stores (study data, planned sessions,
 * the weekly goal, and settings/theme) to one JSON file. Restore is the
 * opposite of Import: it **replaces** every subject, task, session, planned
 * session and goal with exactly what the backup contains. Because that's
 * destructive, restoring asks twice — once via the preview's warning text,
 * once via an explicit "yes, replace everything" confirmation dialog — before
 * [DataManagementViewModel.confirmRestore] ever runs (see `StudyDataBackup.kt`).
 *
 * Every state has a visible representation: no data to export, reading a
 * file, a preview (with rejected entries listed, not hidden), importing,
 * done, and failure — each with a plain-text explanation, never color alone.
 */
@Composable
fun DataManagementScreen(modifier: Modifier = Modifier, viewModel: DataManagementViewModel = viewModel()) {
    val sessionCount by viewModel.sessionCount.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val lastBackupEpochMillis by viewModel.lastBackupEpochMillis.collectAsState()

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TransferFormat.CSV.mimeType)
    ) { uri -> if (uri != null) viewModel.export(TransferFormat.CSV, uri) }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TransferFormat.JSON.mimeType)
    ) { uri -> if (uri != null) viewModel.export(TransferFormat.JSON, uri) }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readImportFile(uri)
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TransferFormat.JSON.mimeType)
    ) { uri -> if (uri != null) viewModel.createBackup(uri) }
    val restoreOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readBackupFile(uri)
    }

    val exportBusy = exportState is ExportUiState.Working
    val importBusy = importState is ImportUiState.Reading || importState is ImportUiState.Importing
    val backupBusy = backupState is BackupUiState.Working
    val restoreBusy = restoreState is RestoreUiState.Reading || restoreState is RestoreUiState.Restoring

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title = "Export study history") }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (sessionCount == 0) {
                            "There are no recorded sessions to export yet."
                        } else {
                            "${countText(sessionCount, "session")} recorded. Each row has the date, start and end time, duration, subject, task and session type."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    PrimaryButton(
                        text = "Export as CSV",
                        enabled = sessionCount > 0 && !exportBusy,
                        onClick = { csvLauncher.launch(exportFileName(TransferFormat.CSV, LocalDate.now())) }
                    )
                    Text(
                        text = "CSV opens in spreadsheet apps such as Excel and Google Sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    SecondaryButton(
                        text = "Export as JSON",
                        enabled = sessionCount > 0 && !exportBusy,
                        onClick = { jsonLauncher.launch(exportFileName(TransferFormat.JSON, LocalDate.now())) }
                    )
                    Text(
                        text = "JSON keeps every detail, including your subjects and tasks. It's the best choice for moving your history to another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        item {
            when (val state = exportState) {
                ExportUiState.Idle -> Unit
                ExportUiState.Working -> BusyRow("Saving file…")
                is ExportUiState.Done -> MessageCard("Export complete", state.message, onDismiss = viewModel::dismissExport)
                is ExportUiState.Failed -> MessageCard("Export failed", state.message, onDismiss = viewModel::dismissExport)
            }
        }

        item { SectionHeader(title = "Import study history") }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Import sessions from a StudySpace CSV or JSON export. You'll see exactly what will be added before anything is saved. Import only adds: nothing already in the app is changed or removed, and sessions you already have are skipped.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    SecondaryButton(
                        text = "Choose a file…",
                        enabled = !importBusy,
                        onClick = { openLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
        }
        item {
            when (val state = importState) {
                ImportUiState.Idle -> Unit
                ImportUiState.Reading -> BusyRow("Reading and checking the file…")
                ImportUiState.Importing -> BusyRow("Importing…")
                is ImportUiState.Preview -> ImportPreviewCard(
                    plan = state.plan,
                    onConfirm = viewModel::confirmImport,
                    onCancel = viewModel::dismissImport
                )
                is ImportUiState.Done -> ImportDoneCard(result = state.result, onDismiss = viewModel::dismissImport)
                is ImportUiState.Failed -> MessageCard("Couldn't import", state.message, onDismiss = viewModel::dismissImport)
            }
        }

        item { SectionHeader(title = "Backup & restore") }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "A backup saves everything: your subjects, tasks, study history, planner, weekly goal, and theme. It doesn't include a custom photo background, since that's a private file already on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = lastBackupText(lastBackupEpochMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    PrimaryButton(
                        text = "Create backup",
                        enabled = !backupBusy,
                        onClick = { backupLauncher.launch(backupFileName(LocalDate.now())) }
                    )
                }
            }
        }
        item {
            when (val state = backupState) {
                BackupUiState.Idle -> Unit
                BackupUiState.Working -> BusyRow("Saving backup…")
                is BackupUiState.Done -> MessageCard("Backup complete", state.message, onDismiss = viewModel::dismissBackup)
                is BackupUiState.Failed -> MessageCard("Backup failed", state.message, onDismiss = viewModel::dismissBackup)
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Restoring a backup replaces everything currently in the app with what's in the file. You'll see exactly what it contains, and be asked to confirm, before anything changes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    SecondaryButton(
                        text = "Choose a backup file…",
                        enabled = !restoreBusy,
                        onClick = { restoreOpenLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
        }
        item {
            when (val state = restoreState) {
                RestoreUiState.Idle -> Unit
                RestoreUiState.Reading -> BusyRow("Reading and checking the backup…")
                RestoreUiState.Restoring -> BusyRow("Restoring…")
                is RestoreUiState.Preview -> RestorePreviewCard(
                    content = state.content,
                    currentSessionCount = state.currentSessionCount,
                    onConfirm = viewModel::confirmRestore,
                    onCancel = viewModel::dismissRestore
                )
                is RestoreUiState.Done -> RestoreDoneCard(result = state.result, onDismiss = viewModel::dismissRestore)
                is RestoreUiState.Failed -> MessageCard("Couldn't restore", state.message, onDismiss = viewModel::dismissRestore)
            }
        }
    }
}

@Composable
private fun BusyRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun MessageCard(title: String, message: String, onDismiss: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            SecondaryButton(text = "OK", onClick = onDismiss)
        }
    }
}

@Composable
private fun ImportPreviewCard(plan: ImportPlan, onConfirm: () -> Unit, onCancel: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ready to import (${plan.format.name} file)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            previewLines(plan).forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            if (plan.rejected.isNotEmpty()) {
                Text(
                    text = "${countText(plan.rejected.size, "entry", "entries")} can't be imported and will be left out:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
                plan.rejected.take(MAX_LISTED).forEach { rejected ->
                    Text(
                        text = "${rejected.where}: ${rejected.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                if (plan.rejected.size > MAX_LISTED) {
                    Text(
                        text = "…and ${plan.rejected.size - MAX_LISTED} more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = "Nothing that's already in the app will be changed or deleted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
            PrimaryButton(
                text = if (plan.hasChanges) importButtonLabel(plan) else "Nothing new to import",
                enabled = plan.hasChanges,
                onClick = onConfirm
            )
            SecondaryButton(text = "Cancel", onClick = onCancel)
        }
    }
}

@Composable
private fun ImportDoneCard(result: ImportResult, onDismiss: () -> Unit) {
    val lines = buildList {
        add("Added ${countText(result.sessionsAdded, "session")}.")
        if (result.duplicatesSkipped > 0) add("Skipped ${countText(result.duplicatesSkipped, "session")} you already had.")
        if (result.subjectsCreated > 0) add("Created ${countText(result.subjectsCreated, "subject")}.")
        if (result.tasksCreated > 0) add("Created ${countText(result.tasksCreated, "task")}.")
        if (result.rejected > 0) add("Left out ${countText(result.rejected, "entry", "entries")} that couldn't be imported.")
    }
    MessageCard(title = "Import complete", message = lines.joinToString("\n"), onDismiss = onDismiss)
}

private fun importButtonLabel(plan: ImportPlan): String =
    if (plan.newSessions.isNotEmpty()) "Import ${countText(plan.newSessions.size, "session")}" else "Import"

/** The plain-language "here is what will happen" lines for the preview. */
private fun previewLines(plan: ImportPlan): List<String> = buildList {
    add("${countText(plan.newSessions.size, "new session")} to add")
    if (plan.duplicateSessions > 0) {
        add("${countText(plan.duplicateSessions, "session")} already in the app (skipped)")
    }
    if (plan.newSubjects.isNotEmpty()) {
        val names = plan.newSubjects.take(MAX_LISTED).joinToString(", ") { it.name }
        val more = plan.newSubjects.size - MAX_LISTED
        add("New ${if (plan.newSubjects.size == 1) "subject" else "subjects"} to create: $names" + if (more > 0) " and $more more" else "")
    }
    if (plan.newTasks.isNotEmpty()) {
        add("${countText(plan.newTasks.size, "new task")} to add")
    }
    if (plan.sessionsWithoutTaskLink > 0) {
        add("For ${countText(plan.sessionsWithoutTaskLink, "session")}, the task they refer to isn't in the app, so they'll be added without a task")
    }
}

private fun lastBackupText(epochMillis: Long?): String =
    if (epochMillis == null) {
        "No backup has been made on this device yet."
    } else {
        "Last backup: " + BACKUP_TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }

/**
 * Restore's preview, distinct from [ImportPreviewCard] in one deliberate way:
 * this is a **replace**, so the primary button doesn't restore directly — it
 * opens [ConfirmReplaceDialog], a second, explicit "yes, replace everything"
 * step, before [onConfirm] (`DataManagementViewModel.confirmRestore`) ever runs.
 */
@Composable
private fun RestorePreviewCard(
    content: BackupContent,
    currentSessionCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Backup ready to restore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This backup contains ${countText(content.studyData.sessions.size, "session")}, " +
                    "${countText(content.studyData.subjects.size, "subject")}, " +
                    "${countText(content.studyData.tasks.size, "task")} and " +
                    "${countText(content.plannedSessions.size, "planned session")}" +
                    (content.weeklyGoalMinutes?.let { ", plus a weekly goal" } ?: "") + ".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (currentSessionCount > 0) {
                Text(
                    text = "You currently have ${countText(currentSessionCount, "session")} recorded. " +
                        "Restoring will replace all of it — not add to it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (content.totalRejected > 0) {
                Text(
                    text = "${countText(content.totalRejected, "entry", "entries")} in the file can't be restored and will be left out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "This replaces every subject, task, session, planned session and goal currently in the app. It can't be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            PrimaryButton(text = "Restore backup…", onClick = { showConfirmDialog = true })
            SecondaryButton(text = "Cancel", onClick = onCancel)
        }
    }

    if (showConfirmDialog) {
        ConfirmReplaceDialog(
            onConfirm = {
                showConfirmDialog = false
                onConfirm()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun ConfirmReplaceDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace everything?") },
        text = {
            Text("This deletes all current subjects, tasks, study history, planned sessions and your goal, and replaces them with the backup. This can't be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Yes, replace everything") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RestoreDoneCard(result: RestoreResult, onDismiss: () -> Unit) {
    val lines = buildList {
        add("Restored ${countText(result.sessionsRestored, "session")}, ${countText(result.subjectsRestored, "subject")} and ${countText(result.tasksRestored, "task")}.")
        add("Restored ${countText(result.plannedSessionsRestored, "planned session")}.")
        if (result.weeklyGoalRestored) add("Weekly goal restored.")
        if (result.itemsSkipped > 0) add("Left out ${countText(result.itemsSkipped, "entry", "entries")} that couldn't be restored.")
    }
    MessageCard(title = "Restore complete", message = lines.joinToString("\n"), onDismiss = onDismiss)
}
