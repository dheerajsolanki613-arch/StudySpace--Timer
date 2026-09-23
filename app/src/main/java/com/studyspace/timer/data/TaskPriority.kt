package com.studyspace.timer.data

/**
 * Priority level for a [com.studyspace.timer.data.db.TaskEntity]. Stored as
 * [name] (a plain string column, same convention as [SessionType]) rather
 * than an ordinal or a Room TypeConverter, so reordering or renaming a
 * member later can't silently corrupt old rows.
 */
enum class TaskPriority(val displayLabel: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}
