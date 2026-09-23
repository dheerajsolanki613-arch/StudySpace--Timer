package com.studyspace.timer.data.repository

/**
 * Pure minute-of-day formatting/math for [com.studyspace.timer.data.db.PlannedSessionEntity]
 * — no Android dependency, same reasoning as [computeTaskDueInfo]/`GoalProgress`,
 * unit-tested directly.
 */

/** "9:00 AM", "1:30 PM", etc. from a 0-1439 minute-of-day value. */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val wrapped = ((minuteOfDay % 1440) + 1440) % 1440
    val hour24 = wrapped / 60
    val minute = wrapped % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "%d:%02d %s".format(hour12, minute, amPm)
}

/**
 * "9:00 AM – 10:30 AM" for a plan's window. If the plan runs past midnight
 * (`startMinuteOfDay + durationMinutes >= 1440` — an evening session with a
 * long duration), the end time is still formatted as a wall-clock time on
 * the *next* day, with a "+1d" suffix so it doesn't read as a same-day time
 * earlier than the start time.
 */
fun formatPlannedTimeRange(startMinuteOfDay: Int, durationMinutes: Int): String {
    val endMinuteOfDay = startMinuteOfDay + durationMinutes
    val endLabel = formatMinuteOfDay(endMinuteOfDay)
    val suffix = if (endMinuteOfDay >= 1440) " (+1d)" else ""
    return "${formatMinuteOfDay(startMinuteOfDay)} – $endLabel$suffix"
}

/** "1h 30m", "45m" — for a plan's duration chip/label. */
fun formatPlannedDuration(durationMinutes: Int): String {
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
