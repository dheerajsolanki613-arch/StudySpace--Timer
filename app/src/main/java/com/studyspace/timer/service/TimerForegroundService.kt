package com.studyspace.timer.service

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.studyspace.timer.timer.ActiveTimerSession
import com.studyspace.timer.timer.TimerRunState
import com.studyspace.timer.timer.formatTimerDuration
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps exactly one timer session alive and visible in a notification while
 * the app is backgrounded, and lets the user Pause/Resume/Stop it from
 * there.
 *
 * This service does NOT own a [com.studyspace.timer.timer.TimerEngine]
 * itself — it only observes whichever one [ActiveTimerSession] currently
 * points at (published by the owning ViewModel; see that class's doc for
 * why background tracking is single-session by design) and mirrors its
 * state into an ongoing notification. Started via
 * `ContextCompat.startForegroundService` from `MainActivity` the moment a
 * session becomes active; stops itself the moment [ActiveTimerSession]
 * clears (timer reset, phase completed, or ViewModel cleared).
 *
 * `START_NOT_STICKY`: if the OS kills this service under memory pressure,
 * it should NOT restart itself with a null intent and no active session to
 * show — that would be exactly the kind of fabricated/misleading state this
 * project's rules warn against. A genuinely running timer's own ViewModel
 * (while its process is alive) or the user reopening the app is what
 * re-establishes an active session; this service only ever mirrors that.
 *
 * Known limitation (documented per project rule, not silently glossed
 * over): if Android kills the whole app *process* — not just this service —
 * while a timer is running, the in-memory [com.studyspace.timer.timer.TimerEngine]
 * state is lost with it, same as before Stage 4. A true "resume exact
 * elapsed time after process death" would need the session's start
 * timestamp persisted to disk (Room, arriving in Stage 5) and the engine
 * itself rehydrated from that on next launch — out of scope for "add a
 * foreground service + notification," and not implemented here.
 */
class TimerForegroundService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        TimerNotifications.ensureChannel(this)

        // Post a minimal placeholder immediately so startForeground() is
        // called well within the OS's post-startForegroundService window,
        // even before the first real state arrives from ActiveTimerSession.
        startForegroundCompat(
            TimerNotifications.build(
                context = this,
                label = "StudySpace Timer",
                timeText = "00:00",
                statusText = "Starting\u2026",
                isPaused = false
            )
        )

        lifecycleScope.launch {
            ActiveTimerSession.active
                .flatMapLatest { info ->
                    if (info == null) {
                        flowOf(null)
                    } else {
                        info.state.map { timerState -> info to timerState }
                    }
                }
                .collectLatest { pair ->
                    if (pair == null) {
                        ServiceCompat.stopForeground(this@TimerForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collectLatest
                    }
                    val (info, timerState) = pair
                    val timeText = formatTimerDuration(
                        if (timerState.targetMillis > 0L) timerState.remainingMillis else timerState.elapsedMillis
                    )
                    val statusText = when (timerState.runState) {
                        TimerRunState.RUNNING -> "In progress"
                        TimerRunState.PAUSED -> "Paused"
                        TimerRunState.COMPLETED -> "Completed"
                        TimerRunState.IDLE -> "Ready"
                    }
                    startForegroundCompat(
                        TimerNotifications.build(
                            context = this@TimerForegroundService,
                            label = info.label,
                            timeText = timeText,
                            statusText = statusText,
                            isPaused = timerState.isPaused
                        )
                    )
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PAUSE -> ActiveTimerSession.active.value?.onPause?.invoke()
            ACTION_RESUME -> ActiveTimerSession.active.value?.onResume?.invoke()
            ACTION_STOP -> ActiveTimerSession.active.value?.onStop?.invoke()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                TimerNotifications.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(TimerNotifications.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.studyspace.timer.action.PAUSE"
        const val ACTION_RESUME = "com.studyspace.timer.action.RESUME"
        const val ACTION_STOP = "com.studyspace.timer.action.STOP"
    }
}
