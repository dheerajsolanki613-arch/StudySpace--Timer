package com.studyspace.timer.data

/**
 * Which timer mode produced a recorded [com.studyspace.timer.data.db.StudySessionEntity].
 * Stored in the database as [name] (a plain string column) via
 * [com.studyspace.timer.data.db.StudySessionEntity.type] rather than a Room
 * TypeConverter/enum column, so a future renamed/added mode can't silently
 * break deserialization of old rows.
 */
enum class SessionType(val displayLabel: String) {
    SELF_STUDY("Self-Study"),
    ONLINE_STUDY("Online Study"),
    NORMAL_TIMER("Normal Timer"),
    POMODORO("Pomodoro"),
    FOCUS_MODE("Focus Mode")
}
