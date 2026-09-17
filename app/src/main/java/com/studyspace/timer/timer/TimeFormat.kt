package com.studyspace.timer.timer

import java.util.Locale

/**
 * Formats a millisecond duration as a clock string: "MM:SS" under an hour,
 * "H:MM:SS" once it reaches one. Used for every timer display in the app so
 * formatting stays identical across Self-Study, Online Study, Normal,
 * Pomodoro, and Focus Mode.
 */
fun formatTimerDuration(millis: Long): String {
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Formats a millisecond duration as "Xh Ym" (dashboard/stats style, distinct
 * from [formatTimerDuration]'s running-clock "MM:SS"). "0m" for a zero
 * duration, "Xh" alone when the minutes component is exactly zero.
 */
fun formatDurationHoursMinutes(millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }
}

/**
 * Short relative-time label for a past [epochMillis] against [nowMillis]
 * (defaults to [System.currentTimeMillis]) — "Just now", "23m ago", "3h
 * ago", "Yesterday", or "N days ago" — used for "Recent Activity" entries.
 */
fun formatRelativeTime(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffMinutes = (nowMillis - epochMillis) / 60_000L
    return when {
        diffMinutes < 1L -> "Just now"
        diffMinutes < 60L -> "${diffMinutes}m ago"
        diffMinutes < 24 * 60L -> "${diffMinutes / 60L}h ago"
        diffMinutes < 2 * 24 * 60L -> "Yesterday"
        else -> "${diffMinutes / (24 * 60L)} days ago"
    }
}

