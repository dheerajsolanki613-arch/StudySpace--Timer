package com.studyspace.timer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyspace.timer.StudySpaceApplication
import kotlinx.coroutines.launch

/**
 * Receives the single reminder alarm (see [ReminderScheduler]) and hands off
 * to [ReminderScheduler.handleAlarm]. Uses `goAsync()` so the database reads
 * that decide what to post can finish off the main thread without the
 * process being torn down mid-way; `finish()` is always reached.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val triggerAt = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER_AT, -1L)
        if (triggerAt < 0L) return
        val app = context.applicationContext as StudySpaceApplication
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                ReminderScheduler.handleAlarm(app, triggerAt)
            } finally {
                pending.finish()
            }
        }
    }
}
