package com.studyspace.timer.data.transfer

import com.studyspace.timer.data.PlannedSessionStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.ZoneId

/**
 * Phase 14 — Backup & Restore. A backup is a **full-fidelity replace**
 * artifact: unlike Phase 13's export (study history only, always additive on
 * import), a backup carries subjects, tasks, study sessions, planned
 * sessions, the weekly goal, and the DataStore-backed settings/theme
 * choices — everything this app stores except achievements (computed from
 * the above, never stored) and a custom "Personalize" gallery photo (a
 * private file on this device, deliberately left out of a portable JSON
 * backup rather than base64-inflating it into the file).
 *
 * A backup nests a `studyspace-export`-shaped object (see
 * [buildJsonExport]/[EXPORT_FORMAT_ID]) under `"studyData"` and reuses the
 * exact same, already-tested [parseJsonExport] for that section — see that
 * function's doc for why. Everything backup-specific (planned sessions,
 * goal, settings, theme) is parsed here, leniently: a corrupt or
 * hand-edited settings/theme block falls back to app defaults rather than
 * failing the whole restore, since losing a toggle is recoverable and losing
 * years of study history to a formatting slip is not.
 */

const val BACKUP_FORMAT_ID = "studyspace-backup"
const val BACKUP_FORMAT_VERSION = 1

// Small, local fallback defaults — deliberately not importing SettingsRepository/
// ReminderSettings here to keep this file dependency-free of the settings package,
// same layering as the rest of `data.transfer`. Source of truth for these values is
// SettingsRepository.DEFAULT_DAILY_GOAL_MINUTES and ReminderSettings.DEFAULT_DAILY_REMINDER_MINUTE.
private const val FALLBACK_DAILY_GOAL_MINUTES = 240
private const val FALLBACK_DAILY_REMINDER_MINUTE = 19 * 60
private const val FALLBACK_PALETTE_ID = "galaxy"
private const val FALLBACK_WALLPAPER_MODE = "default"

/**
 * A snapshot of the DataStore-backed preferences a backup carries. Read/written by the caller, not this class — see [BackupContent].
 * Phase 15 added [soundEnabled]/[vibrationEnabled]; both default to `true`
 * on parse if missing (e.g. a backup made before Phase 15 existed), matching
 * [com.studyspace.timer.settings.SettingsRepository]'s own defaults.
 */
data class SettingsSnapshot(
    val dailyGoalMinutes: Int,
    val timerCompletionAlerts: Boolean,
    val keepScreenOn: Boolean,
    val reduceMotion: Boolean,
    val plannedSessionReminders: Boolean,
    val upcomingSessionReminders: Boolean,
    val dailyReminder: Boolean,
    val dailyReminderMinuteOfDay: Int,
    val goalNotifications: Boolean,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

/**
 * @param wallpaperMode "default" or "built_in". A user's custom gallery photo
 *   is never captured here — see the class doc — so restoring a backup that
 *   had a custom photo selected falls back to the default background.
 * @param accentArgb Phase 15's accent-customization override, or `null` to
 *   use the selected palette's own accent — see
 *   [com.studyspace.timer.ui.theme.effectiveColorScheme].
 */
data class ThemeSnapshot(
    val paletteId: String,
    val wallpaperMode: String,
    val builtInWallpaperId: String?,
    val accentArgb: Int? = null
)

/** A planned session as a backup file represents it: subject/task by name, like [ImportedSession]. */
data class ImportedPlannedSession(
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val subjectName: String?,
    val taskTitle: String?,
    val taskSubjectName: String?,
    val notes: String?,
    val status: String,
    val createdAtEpochMillis: Long
)

/** Everything a validated backup file contains, ready to preview or restore. */
data class BackupContent(
    val studyData: ParsedImport,
    val plannedSessions: List<ImportedPlannedSession>,
    val weeklyGoalMinutes: Int?,
    val settings: SettingsSnapshot,
    val theme: ThemeSnapshot,
    /** Backup-specific rejections (bad planned sessions / goal) — on top of, not instead of, [studyData]'s own [ParsedImport.rejected]. */
    val rejected: List<RejectedRow>,
    val createdAtEpochMillis: Long
) {
    val totalRejected: Int get() = studyData.rejected.size + rejected.size
}

sealed interface BackupParseResult {
    data class Success(val content: BackupContent) : BackupParseResult

    /** The file as a whole can't be restored (wrong kind of file, damaged, too new); nothing was read from it. */
    data class Failure(val message: String) : BackupParseResult
}

/** Internal control flow for "this one entry is bad" — caught per entry, never escapes the parser. */
private class BackupRowRejected(val reason: String) : Exception(reason)

private fun rejectRow(reason: String): Nothing = throw BackupRowRejected(reason)

private fun JSONObject.textOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

/** Stricter than `optLong`: a present-but-non-numeric value (e.g. a string) is `null`, not silently `0`. */
private fun JSONObject.longOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) (opt(key) as? Number)?.toLong() else null

private fun JSONObject.intOrNull(key: String): Int? = longOrNull(key)?.toInt()

fun backupFileName(date: java.time.LocalDate): String = "studyspace-backup-$date.json"

/**
 * Builds the full backup JSON: a `studyspace-export`-shaped `studyData`
 * object (built by the existing, tested [buildJsonExport]) plus planned
 * sessions, the weekly goal, and the settings/theme snapshot.
 */
fun buildBackupJson(
    bundle: BackupExportBundle,
    settings: SettingsSnapshot,
    theme: ThemeSnapshot,
    nowEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val studyDataJson = JSONObject(buildJsonExport(bundle.studyData, nowEpochMillis, zone))

    val subjectNames = bundle.studyData.subjects.associate { it.id to it.name }
    val tasksById = bundle.studyData.tasks.associateBy { it.id }
    val plannedArray = JSONArray()
    bundle.plannedSessions.forEach { planned ->
        val task = planned.taskId?.let { tasksById[it] }
        plannedArray.put(
            JSONObject()
                .put("dateEpochDay", planned.dateEpochDay)
                .put("startMinuteOfDay", planned.startMinuteOfDay)
                .put("durationMinutes", planned.durationMinutes)
                .putOpt("subject", planned.subjectId?.let { subjectNames[it] })
                .putOpt("task", task?.title)
                .putOpt("taskSubject", task?.subjectId?.let { subjectNames[it] })
                .putOpt("notes", planned.notes)
                .put("status", planned.status)
                .put("createdAtEpochMillis", planned.createdAtEpochMillis)
        )
    }

    val settingsJson = JSONObject()
        .put("dailyGoalMinutes", settings.dailyGoalMinutes)
        .put("timerCompletionAlerts", settings.timerCompletionAlerts)
        .put("keepScreenOn", settings.keepScreenOn)
        .put("reduceMotion", settings.reduceMotion)
        .put("plannedSessionReminders", settings.plannedSessionReminders)
        .put("upcomingSessionReminders", settings.upcomingSessionReminders)
        .put("dailyReminder", settings.dailyReminder)
        .put("dailyReminderMinuteOfDay", settings.dailyReminderMinuteOfDay)
        .put("goalNotifications", settings.goalNotifications)
        .put("soundEnabled", settings.soundEnabled)
        .put("vibrationEnabled", settings.vibrationEnabled)

    val themeJson = JSONObject()
        .put("paletteId", theme.paletteId)
        .put("wallpaperMode", theme.wallpaperMode)
        .putOpt("builtInWallpaperId", theme.builtInWallpaperId)
        .putOpt("accentArgb", theme.accentArgb)

    return JSONObject()
        .put("format", BACKUP_FORMAT_ID)
        .put("version", BACKUP_FORMAT_VERSION)
        .put("app", "StudySpace Timer")
        .put("createdAtEpochMillis", nowEpochMillis)
        .put("timeZone", zone.id)
        .put("studyData", studyDataJson)
        .put("plannedSessions", plannedArray)
        .putOpt("weeklyGoalMinutes", bundle.weeklyGoalMinutes)
        .put("settings", settingsJson)
        .put("theme", themeJson)
        .toString(2)
}

/**
 * Parses and validates a backup file. Detects the format from content
 * (`format: "studyspace-backup"`), same reasoning as [parseStudyDataFile].
 * Study data (subjects/tasks/sessions) is validated exactly as strictly as a
 * Phase 13 import — see [parseJsonExport]. Planned sessions are validated
 * per-entry, with bad ones reported and skipped rather than failing the
 * whole restore. Settings/theme are read leniently: any missing or
 * unrecognised field falls back to a safe default instead of rejecting the
 * file, since a backup is still worth restoring even if one preference in it
 * can't be read.
 */
fun parseBackupFile(
    text: String,
    nowEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): BackupParseResult {
    val body = text.removePrefix("\uFEFF").trimStart()
    if (body.isEmpty()) return BackupParseResult.Failure("The file is empty.")

    val root = try {
        JSONObject(body)
    } catch (e: JSONException) {
        return BackupParseResult.Failure("This file isn't valid JSON, so it can't be a StudySpace backup.")
    }

    val format = root.optString("format")
    if (format == EXPORT_FORMAT_ID) {
        return BackupParseResult.Failure(
            "This is a StudySpace export file (study history only), not a full backup. Use \"Import\" instead, or make a backup from this screen."
        )
    }
    if (format != BACKUP_FORMAT_ID) {
        return BackupParseResult.Failure("This isn't a StudySpace backup file.")
    }

    val version = root.optInt("version", -1)
    if (version < 1) return BackupParseResult.Failure("This backup file has no valid format version.")
    if (version > BACKUP_FORMAT_VERSION) {
        return BackupParseResult.Failure(
            "This backup was made by a newer version of StudySpace (format version $version). Update the app to restore it."
        )
    }

    val studyDataObj = root.optJSONObject("studyData")
        ?: return BackupParseResult.Failure("This backup file is missing its study data section.")

    val parsedStudyData = when (val result = parseJsonExport(studyDataObj.toString(), nowEpochMillis, zone)) {
        is ParseResult.Failure -> return BackupParseResult.Failure("This backup's study data section is invalid: ${result.message}")
        is ParseResult.Success -> result.parsed
    }

    val rejected = mutableListOf<RejectedRow>()

    val plannedSessions = mutableListOf<ImportedPlannedSession>()
    root.optJSONArray("plannedSessions")?.let { array ->
        for (i in 0 until array.length()) {
            try {
                plannedSessions += parsePlannedSession(array.optJSONObject(i))
            } catch (e: BackupRowRejected) {
                rejected += RejectedRow("Planned session #${i + 1}", e.reason)
            }
        }
    }

    val weeklyGoalMinutes: Int? = if (root.has("weeklyGoalMinutes") && !root.isNull("weeklyGoalMinutes")) {
        val raw = root.intOrNull("weeklyGoalMinutes")
        if (raw != null && raw in 1..100_000) {
            raw
        } else {
            rejected += RejectedRow("Weekly goal", "Invalid target minutes (${raw ?: root.opt("weeklyGoalMinutes")})")
            null
        }
    } else {
        null
    }

    val settingsObj = root.optJSONObject("settings")
    val settings = SettingsSnapshot(
        dailyGoalMinutes = settingsObj?.optInt("dailyGoalMinutes", FALLBACK_DAILY_GOAL_MINUTES) ?: FALLBACK_DAILY_GOAL_MINUTES,
        timerCompletionAlerts = settingsObj?.optBoolean("timerCompletionAlerts", true) ?: true,
        keepScreenOn = settingsObj?.optBoolean("keepScreenOn", true) ?: true,
        reduceMotion = settingsObj?.optBoolean("reduceMotion", false) ?: false,
        plannedSessionReminders = settingsObj?.optBoolean("plannedSessionReminders", false) ?: false,
        upcomingSessionReminders = settingsObj?.optBoolean("upcomingSessionReminders", false) ?: false,
        dailyReminder = settingsObj?.optBoolean("dailyReminder", false) ?: false,
        dailyReminderMinuteOfDay = settingsObj?.optInt("dailyReminderMinuteOfDay", FALLBACK_DAILY_REMINDER_MINUTE)
            ?.takeIf { it in 0..1439 } ?: FALLBACK_DAILY_REMINDER_MINUTE,
        goalNotifications = settingsObj?.optBoolean("goalNotifications", false) ?: false,
        // Phase 15 additions — a pre-Phase-15 backup simply won't have these keys, so
        // the missing-key default (true, matching SettingsRepository's own default) applies.
        soundEnabled = settingsObj?.optBoolean("soundEnabled", true) ?: true,
        vibrationEnabled = settingsObj?.optBoolean("vibrationEnabled", true) ?: true
    )

    val themeObj = root.optJSONObject("theme")
    val theme = ThemeSnapshot(
        // Any unrecognised palette/wallpaper id is handled safely downstream by
        // AppPalettes.byId()/WallpaperCatalog.findById(), which already fall back
        // to the default rather than crashing, so no catalog lookup happens here.
        paletteId = themeObj?.textOrNull("paletteId") ?: FALLBACK_PALETTE_ID,
        wallpaperMode = themeObj?.textOrNull("wallpaperMode") ?: FALLBACK_WALLPAPER_MODE,
        builtInWallpaperId = themeObj?.textOrNull("builtInWallpaperId"),
        // Phase 15: absent on a pre-Phase-15 backup, or if the user never customized
        // the accent — null just means "use the palette's own accent", same as live.
        accentArgb = themeObj?.intOrNull("accentArgb")
    )

    return BackupParseResult.Success(
        BackupContent(
            studyData = parsedStudyData,
            plannedSessions = plannedSessions,
            weeklyGoalMinutes = weeklyGoalMinutes,
            settings = settings,
            theme = theme,
            rejected = rejected,
            createdAtEpochMillis = root.optLong("createdAtEpochMillis", nowEpochMillis)
        )
    )
}

private fun parsePlannedSession(obj: JSONObject?): ImportedPlannedSession {
    if (obj == null) rejectRow("Not a valid entry")

    val dateEpochDay = obj.longOrNull("dateEpochDay") ?: rejectRow("Missing or invalid date")
    val startMinuteOfDay = obj.intOrNull("startMinuteOfDay") ?: rejectRow("Missing or invalid start time")
    if (startMinuteOfDay !in 0..1439) rejectRow("Start time must be between 0 and 1439 minutes past midnight")
    val durationMinutes = obj.intOrNull("durationMinutes") ?: rejectRow("Missing or invalid duration")
    if (durationMinutes !in 1..1440) rejectRow("Duration must be between 1 and 1440 minutes")
    val createdAt = obj.longOrNull("createdAtEpochMillis") ?: rejectRow("Missing or invalid createdAtEpochMillis")

    val statusRaw = obj.textOrNull("status") ?: PlannedSessionStatus.PLANNED.name
    val status = PlannedSessionStatus.values().firstOrNull { it.name.equals(statusRaw, ignoreCase = true) }
        ?: rejectRow("Unknown status '$statusRaw'")

    return ImportedPlannedSession(
        dateEpochDay = dateEpochDay,
        startMinuteOfDay = startMinuteOfDay,
        durationMinutes = durationMinutes,
        subjectName = obj.textOrNull("subject"),
        taskTitle = obj.textOrNull("task"),
        taskSubjectName = obj.textOrNull("taskSubject"),
        notes = obj.textOrNull("notes"),
        status = status.name,
        createdAtEpochMillis = createdAt
    )
}
