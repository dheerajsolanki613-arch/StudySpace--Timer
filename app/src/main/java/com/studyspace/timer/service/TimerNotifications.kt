package com.studyspace.timer.service

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
 * Builds and manages the single ongoing notification for whichever timer
 * [ActiveTimerSession] currently points at. Kept separate from
 * [TimerForegroundService] itself so the service class stays focused on
 * lifecycle/state wiring rather than notification-building boilerplate.
 */
object TimerNotifications {
    const val CHANNEL_ID = "active_timer_channel"
    const val NOTIFICATION_ID = 1001

    /** Stage 8: a second, separate channel for the "Timer completion alerts"
     *  setting — see [buildCompletion]. Kept as its own channel (not reused
     *  from [CHANNEL_ID]) because the two need different importance: the
     *  ongoing progress notification must stay silent on every tick, while a
     *  completion alert is a one-shot event the user has asked to be
     *  audibly notified about. Once created, a channel's importance can only
     *  be changed by the user in system settings (Android does not let apps
     *  alter it later) — if the "alerts" toggle is later turned off in this
     *  app's own Settings screen, that's handled by simply not posting to
     *  this channel, not by trying to mute the channel itself. */
    const val COMPLETION_CHANNEL_ID = "timer_completion_channel"
    const val COMPLETION_NOTIFICATION_ID = 1002

    /** Uses IMPORTANCE_LOW: visible and persistent, but silent — an ongoing
     *  study timer shouldn't buzz or ping on every tick or state change. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_timer),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notif_channel_timer_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
            if (manager.getNotificationChannel(COMPLETION_CHANNEL_ID) == null) {
                val completionChannel = NotificationChannel(
                    COMPLETION_CHANNEL_ID,
                    context.getString(R.string.notif_channel_completion),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notif_channel_completion_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(completionChannel)
            }
        }
    }

    fun build(
        context: Context,
        label: String,
        timeText: String,
        statusText: String,
        isPaused: Boolean
    ): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = NotificationCompat.Action(
            0,
            if (isPaused) "Resume" else "Pause",
            serviceActionIntent(context, if (isPaused) TimerForegroundService.ACTION_RESUME else TimerForegroundService.ACTION_PAUSE)
        )
        val stopAction = NotificationCompat.Action(
            0,
            "Stop",
            serviceActionIntent(context, TimerForegroundService.ACTION_STOP)
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(label)
            .setContentText("$timeText \u2022 $statusText")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    /**
     * Stage 8: one-shot notification for the "Timer completion alerts"
     * setting. Posted (not `setOngoing`, so it can be swiped away) via
     * [android.app.NotificationManager.notify] with [COMPLETION_NOTIFICATION_ID]
     * — a different id from [NOTIFICATION_ID] so it doesn't collide with or
     * get overwritten by the ongoing progress notification. Uses the
     * channel's own default sound (no explicit `setSound`/`setVibrate` call)
     * rather than a custom pattern, since `IMPORTANCE_DEFAULT` already
     * grants the system default notification sound without needing the
     * `VIBRATE` permission this app deliberately doesn't request.
     */
    fun buildCompletion(context: Context, label: String): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("$label complete")
            .setContentText("Nice work — tap to see your progress.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(contentIntent)
            .build()
    }

    /**
     * Posts [buildCompletion] if notification permission is actually
     * granted, doing nothing otherwise. Called directly from
     * [com.studyspace.timer.timer.CountdownTimerViewModel] and
     * [com.studyspace.timer.timer.PomodoroViewModel] when their "Timer
     * completion alerts" check passes — deliberately *not* wired through
     * [TimerForegroundService] observing [com.studyspace.timer.timer.ActiveTimerSession]
     * for the COMPLETED state, since that session is cleared in the same
     * call frame a completion is recorded (see those ViewModels'
     * `clearActiveSession()`), which would make the service's observation
     * of the transient COMPLETED state a race rather than a reliable
     * trigger. Calling it directly from the exact place completion is
     * already detected is simpler and correct by construction.
     *
     * `NotificationManagerCompat.notify` can throw `SecurityException` on
     * API 33+ if `POST_NOTIFICATIONS` was denied (Stage 4 requests it, but
     * the user may have declined) — caught here so a denied permission
     * degrades to "no alert" rather than crashing the app.
     */
    fun notifyCompletion(context: Context, label: String) {
        ensureChannel(context)
        try {
            NotificationManagerCompat.from(context)
                .notify(COMPLETION_NOTIFICATION_ID, buildCompletion(context, label))
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted -- degrade silently, same
            // fallback philosophy as the ongoing notification in
            // TimerForegroundService.
        }
    }

    private fun serviceActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, TimerForegroundService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
