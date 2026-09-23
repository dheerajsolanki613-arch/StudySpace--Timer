package com.studyspace.timer.data

/**
 * Lifecycle state of a [com.studyspace.timer.data.db.PlannedSessionEntity].
 * Stored as [name] (plain string column), same convention as the other
 * enums in this package.
 */
enum class PlannedSessionStatus {
    /** Created, its time window hasn't passed yet (or is in progress). */
    PLANNED,
    /** Linked to a real [com.studyspace.timer.data.db.StudySessionEntity] the user completed for it. */
    COMPLETED,
    /** Its end time passed with no linked session and the user didn't cancel it. */
    MISSED,
    /** User explicitly removed/cancelled it rather than letting it go missed. */
    CANCELLED
}
