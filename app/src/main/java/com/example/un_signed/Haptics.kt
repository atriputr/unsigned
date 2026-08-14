package com.example.un_signed

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View

/** Global switch — flipped from AppPreferences on load. */
object Haptics {
    @Volatile var enabled: Boolean = true

    private fun vibrator(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) { null }

    fun tick(context: Context) {
        if (!enabled) return
        val v = vibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    fun click(context: Context) {
        if (!enabled) return
        val v = vibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    /** Use for milestone events (goal hit, timer done). */
    fun success(context: Context) {
        if (!enabled) return
        val v = vibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 30), -1)
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION") v.vibrate(longArrayOf(0, 30, 60, 30), -1)
            }
        } catch (_: Exception) {}
    }

    /** Fallback via View.performHapticFeedback when no Context but we have a View. */
    fun viewClick(view: View) {
        if (!enabled) return
        try { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }
}
