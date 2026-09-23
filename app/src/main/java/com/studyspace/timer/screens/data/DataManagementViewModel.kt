package com.studyspace.timer.screens.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.transfer.BackupContent
import com.studyspace.timer.data.transfer.BackupParseResult
import com.studyspace.timer.data.transfer.ImportLimits
import com.studyspace.timer.data.transfer.ImportPlan
import com.studyspace.timer.data.transfer.ImportResult
import com.studyspace.timer.data.transfer.ParseResult
import com.studyspace.timer.data.transfer.ParsedImport
import com.studyspace.timer.data.transfer.RestoreResult
import com.studyspace.timer.data.transfer.SettingsSnapshot
import com.studyspace.timer.data.transfer.ThemeSnapshot
import com.studyspace.timer.data.transfer.TransferFormat
import com.studyspace.timer.data.transfer.buildBackupJson
import com.studyspace.timer.data.transfer.buildCsvExport
import com.studyspace.timer.data.transfer.buildJsonExport
import com.studyspace.timer.data.transfer.parseBackupFile
import com.studyspace.timer.data.transfer.parseStudyDataFile
import com.studyspace.timer.reminders.ReminderScheduler
import com.studyspace.timer.wallpaper.WallpaperCatalog
import com.studyspace.timer.wallpaper.WallpaperSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Working : ExportUiState
    data class Done(val message: String) : ExportUiState
    data class Failed(val message: String) : ExportUiState
}

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Reading : ImportUiState

    /** A file was read and validated; nothing has been written. [parsed] is kept so confirming can apply it. */
    data class Preview(val plan: ImportPlan, val parsed: ParsedImport) : ImportUiState
    data object Importing : ImportUiState
    data class Done(val result: ImportResult) : ImportUiState
    data class Failed(val message: String) : ImportUiState
}

/** Phase 14 — mirrors [ExportUiState] but for the full backup file. */
sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Working : BackupUiState
    data class Done(val message: String) : BackupUiState
    data class Failed(val message: String) : BackupUiState
}

/**
 * Phase 14 — mirrors [ImportUiState]'s "read → preview → confirm" shape, but
 * restoring **replaces** everything rather than adding to it, so the preview
 * state carries an explicit warning the screen must show before the confirm
 * button does anything irreversible.
 */
sealed interface RestoreUiState {
    data object Idle : RestoreUiState
    data object Reading : RestoreUiState

    /** A backup was read and validated; nothing has been written yet. */
    data class Preview(val content: BackupContent, val currentSessionCount: Int) : RestoreUiState
    data object Restoring : RestoreUiState
    data class Done(val result: RestoreResult) : RestoreUiState
    data class Failed(val message: String) : RestoreUiState
}

/**
 * Backs [DataManagementScreen] (Phase 13). File access goes through the
 * Storage Access Framework: the system file picker hands this class a `Uri`
 * for exactly the file the user chose, so the app needs no storage
 * permission at all and can't read anything else.
 *
 * Importing is a two-step flow by design: [readImportFile] parses, validates
 * and *previews* (writing nothing), and only [confirmImport] — reached from
 * an explicit button on the preview — writes.
 */
class DataManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val repository = app.dataTransferRepository
    private val resolver get() = getApplication<Application>().contentResolver

    val sessionCount: StateFlow<Int> = app.sessionRepository.allSessions()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState

    private val _restoreState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val restoreState: StateFlow<RestoreUiState> = _restoreState

    val lastBackupEpochMillis: StateFlow<Long?> = app.settingsRepository.lastBackupEpochMillis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun export(format: TransferFormat, uri: Uri) {
        _exportState.value = ExportUiState.Working
        viewModelScope.launch {
            _exportState.value = try {
                val count = withContext(Dispatchers.IO) {
                    val bundle = repository.loadExportBundle()
                    val content = when (format) {
                        TransferFormat.CSV -> buildCsvExport(bundle)
                        TransferFormat.JSON -> buildJsonExport(bundle, System.currentTimeMillis())
                    }
                    // "wt" truncates, so re-saving over an existing file can't leave stale bytes at the end.
                    val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file for writing.")
                    stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                    bundle.sessions.size
                }
                ExportUiState.Done("Exported ${countText(count, "session")} as ${format.name}.")
            } catch (e: Exception) {
                ExportUiState.Failed("Export failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun readImportFile(uri: Uri) {
        _importState.value = ImportUiState.Reading
        viewModelScope.launch {
            _importState.value = try {
                val text = withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: throw IOException("Couldn't open the file.")
                    val bytes = stream.use { readCapped(it, ImportLimits.MAX_IMPORT_BYTES) }
                    bytes?.toString(Charsets.UTF_8)
                }
                if (text == null) {
                    ImportUiState.Failed("This file is larger than ${ImportLimits.MAX_IMPORT_BYTES / (1024 * 1024)} MB, which is far bigger than any StudySpace export. It wasn't imported.")
                } else {
                    when (val result = withContext(Dispatchers.Default) { parseStudyDataFile(text, System.currentTimeMillis()) }) {
                        is ParseResult.Failure -> ImportUiState.Failed(result.message)
                        is ParseResult.Success -> ImportUiState.Preview(repository.previewImport(result.parsed), result.parsed)
                    }
                }
            } catch (e: Exception) {
                ImportUiState.Failed("Couldn't read the file: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun confirmImport() {
        val preview = _importState.value as? ImportUiState.Preview ?: return
        _importState.value = ImportUiState.Importing
        viewModelScope.launch {
            _importState.value = try {
                ImportUiState.Done(repository.applyImport(preview.parsed, System.currentTimeMillis()))
            } catch (e: Exception) {
                // The write is one transaction, so a failure here means nothing was saved.
                ImportUiState.Failed("Import failed and nothing was saved: ${e.message ?: "unknown error"}")
            }
        }
    }

    /** Back to idle from a preview, a result, or an error. Never touches data. */
    fun dismissImport() {
        if (_importState.value !is ImportUiState.Reading && _importState.value !is ImportUiState.Importing) {
            _importState.value = ImportUiState.Idle
        }
    }

    fun dismissExport() {
        if (_exportState.value !is ExportUiState.Working) _exportState.value = ExportUiState.Idle
    }

    /** Everything on-device that a backup needs and Room doesn't already hold: the DataStore settings/theme snapshot. */
    private suspend fun currentSettingsSnapshot(): SettingsSnapshot {
        val reminders = app.settingsRepository.reminderSettings.first()
        return SettingsSnapshot(
            dailyGoalMinutes = app.settingsRepository.dailyGoalMinutes.first(),
            timerCompletionAlerts = app.settingsRepository.timerCompletionAlertsEnabled.first(),
            keepScreenOn = app.settingsRepository.keepScreenOnEnabled.first(),
            reduceMotion = app.settingsRepository.reduceMotionEnabled.first(),
            plannedSessionReminders = reminders.plannedSessionReminders,
            upcomingSessionReminders = reminders.upcomingSessionReminders,
            dailyReminder = reminders.dailyReminder,
            dailyReminderMinuteOfDay = reminders.dailyReminderMinuteOfDay,
            goalNotifications = reminders.goalNotifications,
            soundEnabled = app.settingsRepository.soundEnabled.first(),
            vibrationEnabled = app.settingsRepository.vibrationEnabled.first()
        )
    }

    private suspend fun currentThemeSnapshot(): ThemeSnapshot {
        val wallpaper = app.wallpaperRepository.selection.first()
        return ThemeSnapshot(
            paletteId = app.paletteRepository.selectedPaletteId.first(),
            // A custom gallery photo isn't captured in a backup (see StudyDataBackup.kt's class doc),
            // so it's recorded as "default" rather than as a mode restoring can't actually reproduce.
            wallpaperMode = if (wallpaper is WallpaperSelection.BuiltIn) "built_in" else "default",
            builtInWallpaperId = (wallpaper as? WallpaperSelection.BuiltIn)?.wallpaper?.id,
            accentArgb = app.paletteRepository.customAccentArgb.first()
        )
    }

    /** Writes a full backup (study data + planned sessions + goal + settings + theme) to [uri] as JSON. */
    fun createBackup(uri: Uri) {
        _backupState.value = BackupUiState.Working
        viewModelScope.launch {
            _backupState.value = try {
                val now = System.currentTimeMillis()
                val bundle = repository.loadBackupExportBundle()
                val settings = currentSettingsSnapshot()
                val theme = currentThemeSnapshot()
                val json = buildBackupJson(bundle, settings, theme, now)
                withContext(Dispatchers.IO) {
                    val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file for writing.")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                app.settingsRepository.setLastBackupEpochMillis(now)
                BackupUiState.Done(
                    "Backup saved: ${countText(bundle.studyData.sessions.size, "session")}, " +
                        "${countText(bundle.studyData.subjects.size, "subject")}, " +
                        "${countText(bundle.studyData.tasks.size, "task")}, " +
                        "${countText(bundle.plannedSessions.size, "planned session")}."
                )
            } catch (e: Exception) {
                BackupUiState.Failed("Backup failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun dismissBackup() {
        if (_backupState.value !is BackupUiState.Working) _backupState.value = BackupUiState.Idle
    }

    /** Reads and validates a backup file, then shows a preview. Nothing is written until [confirmRestore]. */
    fun readBackupFile(uri: Uri) {
        _restoreState.value = RestoreUiState.Reading
        viewModelScope.launch {
            _restoreState.value = try {
                val text = withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: throw IOException("Couldn't open the file.")
                    val bytes = stream.use { readCapped(it, ImportLimits.MAX_IMPORT_BYTES) }
                    bytes?.toString(Charsets.UTF_8)
                }
                if (text == null) {
                    RestoreUiState.Failed("This file is larger than ${ImportLimits.MAX_IMPORT_BYTES / (1024 * 1024)} MB, which is far bigger than any StudySpace backup. It wasn't read.")
                } else {
                    when (val result = withContext(Dispatchers.Default) { parseBackupFile(text, System.currentTimeMillis()) }) {
                        is BackupParseResult.Failure -> RestoreUiState.Failed(result.message)
                        is BackupParseResult.Success -> RestoreUiState.Preview(result.content, sessionCount.value)
                    }
                }
            } catch (e: Exception) {
                RestoreUiState.Failed("Couldn't read the file: ${e.message ?: "unknown error"}")
            }
        }
    }

    /**
     * Writes the previewed backup, **replacing** every subject, task, study
     * session, planned session and goal in one transaction (see
     * `DataTransferRepository.restoreBackup`), then restores settings/theme
     * and re-aims the reminder alarm so it reflects the restored plans and
     * preferences. Only reachable from an explicit confirm after the caller
     * has shown the destructive-replace warning — this function itself does
     * not ask again.
     */
    fun confirmRestore() {
        val preview = _restoreState.value as? RestoreUiState.Preview ?: return
        _restoreState.value = RestoreUiState.Restoring
        viewModelScope.launch {
            _restoreState.value = try {
                val content = preview.content
                val result = repository.restoreBackup(content, System.currentTimeMillis())

                app.settingsRepository.setDailyGoalMinutes(content.settings.dailyGoalMinutes)
                app.settingsRepository.setTimerCompletionAlertsEnabled(content.settings.timerCompletionAlerts)
                app.settingsRepository.setKeepScreenOnEnabled(content.settings.keepScreenOn)
                app.settingsRepository.setReduceMotionEnabled(content.settings.reduceMotion)
                app.settingsRepository.setPlannedSessionReminders(content.settings.plannedSessionReminders)
                app.settingsRepository.setUpcomingSessionReminders(content.settings.upcomingSessionReminders)
                app.settingsRepository.setDailyReminder(content.settings.dailyReminder)
                app.settingsRepository.setDailyReminderMinuteOfDay(content.settings.dailyReminderMinuteOfDay)
                app.settingsRepository.setGoalNotifications(content.settings.goalNotifications)
                app.settingsRepository.setSoundEnabled(content.settings.soundEnabled)
                app.settingsRepository.setVibrationEnabled(content.settings.vibrationEnabled)

                app.paletteRepository.setPalette(content.theme.paletteId)
                if (content.theme.accentArgb != null) {
                    app.paletteRepository.setCustomAccent(content.theme.accentArgb)
                } else {
                    app.paletteRepository.clearCustomAccent()
                }
                if (content.theme.wallpaperMode == "built_in") {
                    val builtIn = WallpaperCatalog.findById(content.theme.builtInWallpaperId)
                    if (builtIn != null) app.wallpaperRepository.selectBuiltIn(builtIn) else app.wallpaperRepository.selectDefault()
                } else {
                    app.wallpaperRepository.selectDefault()
                }

                ReminderScheduler.reschedule(app)

                RestoreUiState.Done(result)
            } catch (e: Exception) {
                // The database write is one transaction, so a failure here means the prior data is still intact.
                RestoreUiState.Failed("Restore failed and nothing was changed: ${e.message ?: "unknown error"}")
            }
        }
    }

    /** Cancels out of a preview, a result, or an error without writing anything — safe at any of those points since nothing is written until [confirmRestore] runs. */
    fun dismissRestore() {
        if (_restoreState.value !is RestoreUiState.Reading && _restoreState.value !is RestoreUiState.Restoring) {
            _restoreState.value = RestoreUiState.Idle
        }
    }

    /** Reads at most [max] bytes; returns `null` if the stream holds more, instead of loading it all into memory. */
    private fun readCapped(input: InputStream, max: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > max) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}

internal fun countText(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"
