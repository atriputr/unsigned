package com.example.un_signed

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Dual-handle time dial.
 *   Light handle (gray)  → hours   0-11  (12-hour face, one full rotation = 12 h)
 *   Dark  handle (brown) → minutes 0-59
 *
 * Hours  ball orbits the inner-disc/ring boundary.
 * Minutes ball orbits the ring midpoint (further out).
 */
@Composable
fun DualHandleTimeDial(
    hours: Int,
    minutes: Int,
    onValueChange: (hours: Int, minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 280.dp,
    contentFont: FontFamily
) {
    val density = LocalDensity.current
    val view    = LocalView.current

    // ── Geometry (all in px, derived once from dialSize) ─────────────────
    val totalR    = with(density) { dialSize.toPx() / 2f }
    val innerFrac = 0.54f                          // inner disc = 54% of total radius (ring ~46%)
    val innerR    = totalR * innerFrac             // inner disc radius
    val ringT     = totalR - innerR                // ring thickness
    val orbitH    = innerR                         // hours:   inner-disc edge
    val orbitM    = innerR + ringT * 0.88f         // minutes: pushed toward outer ring edge
    val ballR     = totalR * 0.195f                // handle radius (~19.5% of totalR)
    val ballRDp   = with(density) { ballR.toDp() }

    // ── Angles: 0° = 12-o'clock, clockwise ───────────────────────────────
    var hourAngle    by remember { mutableStateOf(hours   / 12f * 360f) }
    var minuteAngle  by remember { mutableStateOf(minutes / 60f * 360f) }
    var draggingHour by remember { mutableStateOf(true) }
    var lastH        by remember { mutableStateOf(hours) }
    var lastM        by remember { mutableStateOf(minutes) }

    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    // ── Text overlay offsets (recomputed on every angle change) ──────────
    val hRad  = Math.toRadians(hourAngle.toDouble())
    val hOffX = with(density) { (orbitH * sin(hRad).toFloat()).toDp() }
    val hOffY = with(density) { (-orbitH * cos(hRad).toFloat()).toDp() }

    val mRad  = Math.toRadians(minuteAngle.toDouble())
    val mOffX = with(density) { (orbitM * sin(mRad).toFloat()).toDp() }
    val mOffY = with(density) { (-orbitM * cos(mRad).toFloat()).toDp() }

    // ── Root box – shadow only, NO clip so handles can straddle the edge ─
    Box(
        modifier = modifier
            .size(dialSize)
            .shadow(
                elevation    = 18.dp,
                shape        = CircleShape,
                clip         = false,
                ambientColor = Color.Black.copy(alpha = 0.09f),
                spotColor    = Color.Black.copy(alpha = 0.12f)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        fun ballPos(deg: Float, orbit: Float): Offset {
                            val r = Math.toRadians(deg.toDouble())
                            return Offset(
                                totalR + orbit * sin(r).toFloat(),
                                totalR - orbit * cos(r).toFloat()
                            )
                        }
                        val hp = ballPos(hourAngle,   orbitH)
                        val mp = ballPos(minuteAngle, orbitM)
                        draggingHour =
                            hypot(pos.x - hp.x, pos.y - hp.y) <=
                            hypot(pos.x - mp.x, pos.y - mp.y)
                    },
                    onDrag = { change, _ ->
                        val dx  = change.position.x - totalR
                        val dy  = change.position.y - totalR
                        val deg = ((Math.toDegrees(atan2(dx, -dy).toDouble()) + 360) % 360).toFloat()
                        if (draggingHour) {
                            hourAngle = deg
                            val newH = (deg / 360f * 12).toInt().coerceIn(0, 11)
                            if (newH != lastH) { lastH = newH; tick(); onValueChange(newH, lastM) }
                        } else {
                            minuteAngle = deg
                            val newM = (deg / 360f * 60).toInt().coerceIn(0, 59)
                            if (newM != lastM) { lastM = newM; tick(); onValueChange(lastH, newM) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {

        // ── Canvas: ring + disc + handle circles ──────────────────────────
        Canvas(modifier = Modifier.size(dialSize)) {
            val c    = Offset(size.width / 2f, size.height / 2f)
            val r    = size.width / 2f
            val iR   = r * innerFrac
            val rT2  = r - iR
            val bR   = r * 0.195f

            // Outer ring — warm light-gray, soft top-left light source
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFF6F6F6), Color(0xFFD8D8D8)),
                    start  = Offset(c.x * 0.20f, c.y * 0.20f),
                    end    = Offset(c.x * 1.80f,  c.y * 1.80f)
                ),
                radius = r,
                center = c
            )

            // Inner disc — near-black, faint radial highlight upper-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF282828), Color(0xFF1A1A1A)),
                    center = Offset(c.x * 0.60f, c.y * 0.60f),
                    radius = iR * 1.35f
                ),
                radius = iR,
                center = c
            )

            // ── Hours handle (dark brown) ─────────────────────────────────
            val hR2 = Math.toRadians(hourAngle.toDouble())
            val hC  = Offset(c.x + iR * sin(hR2).toFloat(), c.y - iR * cos(hR2).toFloat())

            // shadow blob
            drawCircle(Color.Black.copy(alpha = 0.13f), bR + 7f, hC + Offset(1f, 4f))
            // fill — flat dark umber
            drawCircle(Color(0xFF3E2B1E), bR, hC)

            // ── Minutes handle (off-white) ────────────────────────────────
            val mOrb = iR + rT2 * 0.88f
            val mR2  = Math.toRadians(minuteAngle.toDouble())
            val mC   = Offset(c.x + mOrb * sin(mR2).toFloat(), c.y - mOrb * cos(mR2).toFloat())

            // shadow blob
            drawCircle(Color.Black.copy(alpha = 0.13f), bR + 7f, mC + Offset(1f, 4f))
            // fill — off-white with subtle directional gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF9F9F9), Color(0xFFE2E2E2)),
                    center = mC - Offset(bR * 0.22f, bR * 0.22f),
                    radius = bR * 1.1f
                ),
                radius = bR,
                center = mC
            )
        }

        // ── Hours label (light text on dark handle) ───────────────────────
        Box(
            modifier = Modifier
                .size(ballRDp * 2)
                .offset(hOffX, hOffY),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = String.format("%02d", hours),
                color      = Color(0xFFF2F2F2),
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = contentFont
            )
        }

        // ── Minutes label (dark text on light handle) ────────────────────
        Box(
            modifier = Modifier
                .size(ballRDp * 2)
                .offset(mOffX, mOffY),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = String.format("%02d", minutes),
                color      = Color(0xFF1E1E1E),
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = contentFont
            )
        }
    }
}

// ── Overlay wrapper (keeps same call-site signature) ─────────────────────────
@Composable
fun DurationClockPicker(
    initialMinutes: Int = 450,   // 7 h 30 m → hours at ~7-o'clock, minutes at ~6-o'clock
    titleFont: FontFamily,
    contentFont: FontFamily,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hours   by remember { mutableStateOf((initialMinutes / 60).coerceIn(0, 11)) }
    var minutes by remember { mutableStateOf(initialMinutes % 60) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(enabled = false) {}
        ) {
            DualHandleTimeDial(
                hours         = hours,
                minutes       = minutes,
                onValueChange = { h, m -> hours = h; minutes = m },
                dialSize      = 280.dp,
                contentFont   = contentFont
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text       = "${hours * 60 + minutes} MIN",
                color      = Color.White.copy(alpha = 0.92f),
                fontSize   = 22.sp,
                fontFamily = contentFont,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFF9500))
                    .clickable { onConfirm(hours * 60 + minutes) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "SET",
                    color      = Color.Black,
                    fontSize   = 20.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text       = "DRAG HANDLES AROUND THE RING",
                color      = Color.White.copy(alpha = 0.28f),
                fontSize   = 11.sp,
                fontFamily = contentFont
            )
        }
    }
}
