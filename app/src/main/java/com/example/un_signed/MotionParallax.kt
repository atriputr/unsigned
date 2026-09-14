package com.example.un_signed

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.PI
import kotlin.math.abs

/**
 * Sensor-driven parallax tilt for the Glass theme.
 *
 * Reads the accelerometer (or gravity sensor if available) via a low-pass filter to isolate
 * the gravity vector, then converts it into a normalized [-1..1] Offset that Compose can
 * consume as a `State<Offset>`. Callers multiply that by whatever pixel magnitude they want.
 *
 * Rotation-aware — the returned offset is corrected for the current display rotation so
 * tilting the phone right *always* pushes content right regardless of landscape/portrait.
 */
object MotionParallax {

    /**
     * Composable helper that subscribes to the accelerometer while composed, and exposes the
     * current tilt as a `State<Offset>` in the range roughly [-1..1] × [-1..1].
     *
     *  · Positive x  →  device tilted so its right edge is lower
     *  · Positive y  →  device tilted so its bottom edge is lower (back)
     *
     * @param smoothing 0.0 = no smoothing (jittery), 1.0 = no motion. 0.85 is a nice default.
     * @param maxTiltDeg tilt angle at which the returned offset saturates to ±1. 30° works well.
     */
    @Composable
    fun rememberTilt(
        smoothing: Float = 0.85f,
        maxTiltDeg: Float = 30f
    ): State<Offset> {
        val ctx = LocalContext.current
        val view = LocalView.current
        val tiltState = remember { mutableStateOf(Offset.Zero) }

        DisposableEffect(ctx) {
            val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            // Prefer TYPE_GRAVITY (pre-filtered) if available, fall back to accelerometer.
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            // Low-pass state for accelerometer path
            val gravity = FloatArray(3)
            val alpha = (1f - smoothing.coerceIn(0f, 1f)).coerceAtLeast(0.02f)

            // Map tilt angle → normalized offset. sin(maxTiltDeg) is the accel value at saturation.
            val satAccel = kotlin.math.sin(maxTiltDeg * PI.toFloat() / 180f) * 9.81f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val vals = event.values
                    val x: Float
                    val y: Float
                    if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                        x = vals[0]
                        y = vals[1]
                    } else {
                        // Isolate gravity from linear accel via low-pass filter.
                        gravity[0] = gravity[0] + alpha * (vals[0] - gravity[0])
                        gravity[1] = gravity[1] + alpha * (vals[1] - gravity[1])
                        x = gravity[0]
                        y = gravity[1]
                    }

                    // Correct for display rotation so "tilt right" always means +x on screen.
                    val rotation = view.display?.rotation ?: Surface.ROTATION_0
                    val (rx, ry) = when (rotation) {
                        Surface.ROTATION_90  -> Pair( y, -x)
                        Surface.ROTATION_180 -> Pair(-x, -y)
                        Surface.ROTATION_270 -> Pair(-y,  x)
                        else                 -> Pair( x,  y)
                    }

                    // Phone tilted right → gravity.x becomes NEGATIVE (points left toward ground).
                    // We want positive-x when tilted right, so invert.
                    val nx = (-rx / satAccel).coerceIn(-1f, 1f)
                    // Phone tilted forward (top away) → gravity.y INCREASES (points down toward feet).
                    // Positive-y downward on screen when top tilts away.
                    val ny = ( ry / satAccel).coerceIn(-1f, 1f)

                    // Dead-zone very small values so a resting phone doesn't drift.
                    val dx = if (abs(nx) < 0.02f) 0f else nx
                    val dy = if (abs(ny) < 0.02f) 0f else ny

                    tiltState.value = Offset(dx, dy)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (sensor != null && sensorManager != null) {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            }

            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }

        return tiltState
    }
}
