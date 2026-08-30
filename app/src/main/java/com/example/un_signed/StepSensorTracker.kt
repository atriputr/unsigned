package com.example.un_signed

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.LocalDate

/**
 * Fallback step source using the device's built-in TYPE_STEP_COUNTER sensor when Health Connect
 * is unavailable. TYPE_STEP_COUNTER reports a cumulative count since last boot, so we track a
 * per-day baseline the first time it's observed each day.
 */
object StepSensorTracker {

    private const val BASELINE_PREFIX = "step_baseline_"

    fun isAvailable(ctx: Context): Boolean {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    /** Registers a one-shot listener, reads the current cumulative count, updates today's FitnessSample, then unregisters. */
    fun sampleOnce(ctx: Context, onResult: (Int?) -> Unit) {
        if (!PermissionsManager.hasActivityRecognitionPermission(ctx)) {
            onResult(null)
            return
        }
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sm == null || sensor == null) {
            onResult(null)
            return
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                sm.unregisterListener(this)
                val cumulative = event.values.firstOrNull()?.toInt() ?: return onResult(null)
                val today = LocalDate.now().toString()
                val prefsKey = BASELINE_PREFIX + today
                val baseline = readBaseline(ctx, prefsKey) ?: run {
                    writeBaseline(ctx, prefsKey, cumulative)
                    cumulative
                }
                val steps = (cumulative - baseline).coerceAtLeast(0)
                FitDataRepository.upsertFitnessSample(FitnessSample(dateIso = today, steps = steps, source = "step_sensor"))
                onResult(steps)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun readBaseline(ctx: Context, key: String): Int? {
        val prefs = ctx.getSharedPreferences("step_sensor_prefs", Context.MODE_PRIVATE)
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    private fun writeBaseline(ctx: Context, key: String, value: Int) {
        ctx.getSharedPreferences("step_sensor_prefs", Context.MODE_PRIVATE).edit().putInt(key, value).apply()
    }
}
