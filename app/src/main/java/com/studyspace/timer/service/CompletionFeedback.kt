package com.studyspace.timer.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.delay

/**
 * Phase 15 (Themes & Customization) — the "Sound" and "Vibration" Settings
 * toggles. Deliberately **separate** from [TimerNotifications.notifyCompletion]
 * and its "Timer completion alerts" toggle: that posts a system notification
 * (whose own channel sound/vibration Android controls, not this app, once
 * the channel exists); this plays a short tone and/or vibrates *directly*,
 * so it's felt immediately and still works if the user has turned
 * notifications off but still wants the in-app cue. Both effects degrade
 * silently (never crash) if the platform denies the resource — e.g. no audio
 * output available, or a device with no vibrator — since a missed cue is a
 * minor inconvenience, not something worth surfacing as an error.
 */
object CompletionFeedback {
    private const val TONE_DURATION_MS = 250
    private const val VIBRATION_DURATION_MS = 200L

    /** Called once per finished timer/focus session/Pomodoro phase — see each ViewModel's `maybeNotifyCompletion`. */
    suspend fun play(context: Context, soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) playTone()
        if (vibrationEnabled) vibrate(context)
    }

    private suspend fun playTone() {
        val toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: RuntimeException) {
            // No audio output track available right now — nothing to fall back to, just skip the tone.
            return
        }
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS)
            delay(TONE_DURATION_MS.toLong())
        } finally {
            toneGenerator.release()
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = vibratorFor(context) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibratorFor(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
