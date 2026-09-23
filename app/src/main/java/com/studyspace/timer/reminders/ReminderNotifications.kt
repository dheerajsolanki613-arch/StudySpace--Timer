package com.studyspace.timer.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studyspace.timer.MainActivity
import com.studyspace.timer.R

/**
 * Phase 12 — builds and posts the reminder/goal notifications. Three
 * channels, so each kind can be tuned or silenced independently in the
 * system's own notification settings on top of the in-app toggles:
 *
 *  - [STUDY_CHANNEL_ID] (`IMPORTANCE_DEFAULT`): planned-session start and the
 *    upcoming heads-up. Time-sensitive, so it's allowed to make a sound.
 *  - [DAILY_CHANNEL_ID] and [GOAL_CHANNEL_ID] (`IMPORTANCE_LOW`): the daily
 *    reminder and goal-reached notices are informational, never urgent, so
 *    they appear silently. A user who wants them louder can raise the
 *    channel in system settings.
 *
 * A channel's importance can't be changed by the app after creation (only by
 * the user), so these are chosen once, deliberately, here.
 *
 * Every post goes through [post], which does nothing when notifications are
 * disabled and swallows the `SecurityException` an API 33+ device throws
 * when `POST_NOTIFICATIONS` was denied — a denied permission degrades to "no
 * reminder", never a crash. Tapping any of these simply opens the app.
 *
 * Wording is plain and factual — no urgency, no streak language, no
 * "don't miss out" — in line with the spec's "do not pressure users to
 * study" rule for the gamification/notification phases.
 */
object ReminderNotifications {
    const val STUDY_CHANNEL_ID = "study_reminders_channel"
    const val DAILY_CHANNEL_ID = "daily_reminder_channel"
    const val GOAL_CHANNEL_ID = "goal_progress_channel"

    // One fixed id per kind; a per-plan *tag* (below) keeps two plans' notifications distinct.
    private const val ID_SESSION_START = 2001
    private const val ID_UPCOMING = 2002
    private const val ID_DAILY = 2003
    private const val ID_GOAL = 2004

    /** Idempotent — safe to call on every app start and before every post. */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        fun create(id: String, nameRes: Int, descRes: Int, importance: Int) {
            if (manager.getNotificationChannel(id) != null) return
            manager.createNotificationChannel(
                NotificationChannel(id, context.getString(nameRes), importance).apply {
                    description = context.getString(descRes)
                    setShowBadge(false)
                }
            )
        }
        create(STUDY_CHANNEL_ID, R.string.notif_channel_study, R.string.notif_channel_study_desc, NotificationManager.IMPORTANCE_DEFAULT)
        create(DAILY_CHANNEL_ID, R.string.notif_channel_daily, R.string.notif_channel_daily_desc, NotificationManager.IMPORTANCE_LOW)
        create(GOAL_CHANNEL_ID, R.string.notif_channel_goal, R.string.notif_channel_goal_desc, NotificationManager.IMPORTANCE_LOW)
    }

    fun notifySessionStart(context: Context, planId: Long, description: String, timeRange: String) {
        post(
            context, tag = "plan_start:$planId", id = ID_SESSION_START,
            channelId = STUDY_CHANNEL_ID, title = "Study session starting", text = "$description • $timeRange"
        )
    }

    fun notifyUpcoming(context: Context, planId: Long, minutesUntilStart: Int, description: String, timeRange: String) {
        post(
            context, tag = "plan_upcoming:$planId", id = ID_UPCOMING,
            channelId = STUDY_CHANNEL_ID, title = upcomingReminderTitle(minutesUntilStart), text = "$description • $timeRange"
        )
    }

    fun notifyDaily(context: Context) {
        post(
            context, tag = "daily", id = ID_DAILY,
            channelId = DAILY_CHANNEL_ID, title = "Daily study reminder",
            text = "Open StudySpace whenever you're ready to study."
        )
    }

    fun notifyGoalReached(context: Context, weekly: Boolean, totalText: String) {
        post(
            context, tag = if (weekly) "goal_weekly" else "goal_daily", id = ID_GOAL,
            channelId = GOAL_CHANNEL_ID,
            title = if (weekly) "Weekly goal reached" else "Daily goal reached",
            text = if (weekly) "$totalText over the last 7 days." else "$totalText studied today."
        )
    }

    private fun post(context: Context, tag: String, id: Int, channelId: String, title: String, text: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannels(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(
                if (channelId == STUDY_CHANNEL_ID) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .build()
        try {
            manager.notify(tag, id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check above and now — degrade to no notification.
        }
    }
}
