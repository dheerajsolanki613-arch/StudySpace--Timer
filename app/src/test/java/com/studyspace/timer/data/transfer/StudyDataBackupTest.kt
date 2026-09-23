package com.studyspace.timer.data.transfer

import com.studyspace.timer.data.PlannedSessionStatus
import com.studyspace.timer.data.SessionType
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.PlannedSessionEntity
import com.studyspace.timer.data.db.StudySessionEntity
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class StudyDataBackupTest {
    private val utc = ZoneId.of("UTC")
    private val now = 10_000_000L

    private val math = SubjectEntity(id = 1, name = "Mathematics", icon = "📐", colorArgb = 0x112233, createdAtEpochMillis = 0L)
    private val calculus = TaskEntity(
        id = 5, title = "Calculus", chapter = null, topic = null, subjectId = 1,
        priority = TaskPriority.HIGH.name, deadlineEpochDay = null,
        estimatedDurationMinutes = null, completed = false, createdAtEpochMillis = 1_000L, completedAtEpochMillis = null
    )
    private val session = StudySessionEntity(
        id = 9, type = SessionType.POMODORO.name, label = "Calculus", startEpochMillis = 5_000_000L,
        durationMillis = 5_400_000L, completedNaturally = true, dateEpochDay = 57, subjectId = 1, taskId = 5
    )
    private val plannedSession = PlannedSessionEntity(
        id = 3, dateEpochDay = 100, startMinuteOfDay = 960, durationMinutes = 60,
        subjectId = 1, taskId = 5, notes = "Bring calculator", status = PlannedSessionStatus.PLANNED.name,
        createdAtEpochMillis = 2_000L
    )

    private val settings = SettingsSnapshot(
        dailyGoalMinutes = 300, timerCompletionAlerts = false, keepScreenOn = false, reduceMotion = true,
        plannedSessionReminders = true, upcomingSessionReminders = true, dailyReminder = true,
        dailyReminderMinuteOfDay = 480, goalNotifications = true
    )
    private val theme = ThemeSnapshot(paletteId = "twilight", wallpaperMode = "built_in", builtInWallpaperId = "nebula-1")

    private fun bundle(weeklyGoalMinutes: Int? = 1200) = BackupExportBundle(
        studyData = ExportBundle(listOf(math), listOf(calculus), listOf(session)),
        plannedSessions = listOf(plannedSession),
        weeklyGoalMinutes = weeklyGoalMinutes
    )

    // ---------- round trip ----------

    @Test
    fun `built backup parses back with matching study data, planned sessions, goal, settings and theme`() {
        val json = buildBackupJson(bundle(), settings, theme, now, utc)
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        val content = result.content

        assertEquals(1, content.studyData.subjects.size)
        assertEquals("Mathematics", content.studyData.subjects.single().name)
        assertEquals(1, content.studyData.tasks.size)
        assertEquals("Calculus", content.studyData.tasks.single().title)
        assertEquals(1, content.studyData.sessions.size)
        assertEquals(0, content.studyData.rejected.size)

        val planned = content.plannedSessions.single()
        assertEquals(100L, planned.dateEpochDay)
        assertEquals(960, planned.startMinuteOfDay)
        assertEquals(60, planned.durationMinutes)
        assertEquals("Mathematics", planned.subjectName)
        assertEquals("Calculus", planned.taskTitle)
        assertEquals("Mathematics", planned.taskSubjectName)
        assertEquals("Bring calculator", planned.notes)
        assertEquals(PlannedSessionStatus.PLANNED.name, planned.status)

        assertEquals(1200, content.weeklyGoalMinutes)
        assertEquals(settings, content.settings)
        assertEquals(theme, content.theme)
        assertEquals(0, content.totalRejected)
    }

    @Test
    fun `weekly goal is absent in both directions when there is none`() {
        val json = buildBackupJson(bundle(weeklyGoalMinutes = null), settings, theme, now, utc)
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertNull(result.content.weeklyGoalMinutes)
    }

    // ---------- format detection ----------

    @Test
    fun `an export file is rejected with a message pointing at Import instead`() {
        val exportJson = buildJsonExport(ExportBundle(listOf(math), listOf(calculus), listOf(session)), now, utc)
        val result = parseBackupFile(exportJson, now, utc) as BackupParseResult.Failure
        assertTrue(result.message.contains("Import"))
    }

    @Test
    fun `a backup file fed to the import parser is rejected with a message pointing at Restore instead`() {
        val json = buildBackupJson(bundle(), settings, theme, now, utc)
        val result = parseStudyDataFile(json, now) as ParseResult.Failure
        assertTrue(result.message.contains("Restore backup"))
    }

    @Test
    fun `garbage text is rejected without throwing`() {
        val result = parseBackupFile("not json at all", now, utc)
        assertTrue(result is BackupParseResult.Failure)
    }

    @Test
    fun `a newer format version is rejected`() {
        val json = buildBackupJson(bundle(), settings, theme, now, utc)
            .replace("\"version\": 1", "\"version\": 99")
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Failure
        assertTrue(result.message.contains("newer version"))
    }

    // ---------- planned-session validation ----------

    @Test
    fun `a planned session with an out-of-range start time is rejected but the rest of the backup still parses`() {
        val badPlan = plannedSession.copy(startMinuteOfDay = 1500)
        val json = buildBackupJson(bundle().copy(plannedSessions = listOf(plannedSession, badPlan)), settings, theme, now, utc)
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertEquals(1, result.content.plannedSessions.size)
        assertEquals(1, result.content.rejected.size)
        assertEquals(1, result.content.totalRejected)
    }

    @Test
    fun `a planned session with an unknown status is rejected`() {
        val json = buildBackupJson(bundle(), settings, theme, now, utc)
            .replace("\"status\": \"PLANNED\"", "\"status\": \"NOT_A_REAL_STATUS\"")
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertEquals(0, result.content.plannedSessions.size)
        assertEquals(1, result.content.rejected.size)
    }

    // ---------- weekly goal validation ----------

    @Test
    fun `an out-of-range weekly goal is dropped and reported rather than failing the whole file`() {
        val json = buildBackupJson(bundle(weeklyGoalMinutes = 500), settings, theme, now, utc)
            .replace("\"weeklyGoalMinutes\": 500", "\"weeklyGoalMinutes\": -5")
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertNull(result.content.weeklyGoalMinutes)
        assertEquals(1, result.content.rejected.size)
    }

    @Test
    fun `a custom accent color round trips through the backup`() {
        val accentedTheme = theme.copy(accentArgb = 0xFFFFC107.toInt())
        val json = buildBackupJson(bundle(), settings, accentedTheme, now, utc)
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertEquals(accentedTheme.accentArgb, result.content.theme.accentArgb)
    }

    @Test
    fun `a pre-Phase-15 backup with no sound, vibration or accent fields still restores, defaulting sound and vibration on`() {
        // Simulates a backup written before Phase 15 existed: no soundEnabled/vibrationEnabled
        // keys in "settings", no accentArgb key in "theme".
        val json = org.json.JSONObject(buildBackupJson(bundle(), settings, theme, now, utc)).apply {
            getJSONObject("settings").remove("soundEnabled")
            getJSONObject("settings").remove("vibrationEnabled")
            getJSONObject("theme").remove("accentArgb")
        }.toString()
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertEquals(true, result.content.settings.soundEnabled)
        assertEquals(true, result.content.settings.vibrationEnabled)
        assertNull(result.content.theme.accentArgb)
    }

    // ---------- settings/theme leniency ----------

    @Test
    fun `missing settings and theme sections fall back to safe defaults instead of failing`() {
        val json = org.json.JSONObject(buildBackupJson(bundle(), settings, theme, now, utc))
            .apply { remove("settings"); remove("theme") }
            .toString()
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        // Defaults exist and are self-consistent; the exact values are an implementation
        // detail, but the file must still parse successfully rather than fail outright.
        assertTrue(result.content.settings.dailyGoalMinutes > 0)
        assertTrue(result.content.theme.paletteId.isNotBlank())
    }

    @Test
    fun `a custom wallpaper is never round-tripped as built_in`() {
        // ThemeSnapshot itself has no "custom" mode by design (see its class doc) —
        // the caller building it must already have mapped Custom to "default".
        val customLikeTheme = theme.copy(wallpaperMode = "default", builtInWallpaperId = null)
        val json = buildBackupJson(bundle(), settings, customLikeTheme, now, utc)
        val result = parseBackupFile(json, now, utc) as BackupParseResult.Success
        assertEquals("default", result.content.theme.wallpaperMode)
        assertNull(result.content.theme.builtInWallpaperId)
    }
}
