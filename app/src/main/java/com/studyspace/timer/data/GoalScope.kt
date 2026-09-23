package com.studyspace.timer.data

/**
 * What a [com.studyspace.timer.data.db.StudyGoalEntity] measures progress
 * against. Stored as [name] (plain string column), same convention as
 * [SessionType] and [TaskPriority].
 *
 * Deliberately does **not** include a `DAILY` member: the app's single
 * overall daily goal already exists as
 * [com.studyspace.timer.settings.SettingsRepository.dailyGoalMinutes]
 * (DataStore, Stage 8) and stays there — see
 * [com.studyspace.timer.data.db.StudyGoalEntity]'s KDoc for why this table
 * only covers goals that value doesn't, rather than duplicating it.
 */
enum class GoalScope {
    WEEKLY,
    SUBJECT
}
