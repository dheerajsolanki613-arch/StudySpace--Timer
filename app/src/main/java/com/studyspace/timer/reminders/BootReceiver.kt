package com.studyspace.timer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyspace.timer.StudySpaceApplication
import kotlinx.coroutines.launch

/**
 * Re-aims the reminder alarm after a device reboot (Android clears every
 * alarm on reboot) or after the app itself is updated. This is the only
 * reason the app holds `RECEIVE_BOOT_COMPLETED`: without it, a reminder set
 * for tomorrow would silently vanish if the phone restarted overnight. It
 * does nothing else — no service is started and no notification is posted.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as StudySpaceApplication
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                ReminderScheduler.reschedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
