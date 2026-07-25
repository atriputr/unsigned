package com.example.un_signed

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun TimerPickerOverlay(
    fontFamily: FontFamily,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    var hours   by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var unit    by remember { mutableStateOf(1) }

    // Countdown phase state
    var timerActive by remember { mutableStateOf(false) }
    var isRunning   by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableStateOf(0L) }
    var totalMs     by remember { mutableStateOf(0L) }

    val startedAt   = remember { LongArray(1) { 0L } }
    val baseRemain  = remember { LongArray(1) { 0L } }
    var restartKey  by remember { mutableStateOf(0) }

    LaunchedEffect(isRunning, restartKey) {
        if (isRunning && remainingMs > 0L) {
            baseRemain[0] = remainingMs
            startedAt[0]  = SystemClock.elapsedRealtime()
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt[0]
                remainingMs = (baseRemain[0] - elapsed).coerceAtLeast(0L)
                if (remainingMs == 0L) { isRunning = false; break }
                delay(16L)
            }
        }
    }

    // Play alert sound exactly once when the timer hits zero
    var soundTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(timerActive, remainingMs) {
        if (timerActive && remainingMs == 0L && !soundTriggered) {
            soundTriggered = true
            val mp = MediaPlayer.create(context, R.raw.timer_alert)
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
        }
        if (!timerActive) soundTriggered = false
    }

    fun fmtCountdown(ms: Long): String {
        val h = ms / 3_600_000L
        val m = (ms % 3_600_000L) / 60_000L
        val s = (ms % 60_000L) / 1_000L
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
               else       String.format("%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1E2E).copy(alpha = 0.97f), Color(0xFF12121A).copy(alpha = 0.97f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "TIMER",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = BebasFont
            )

            Spacer(Modifier.height(10.dp))

            if (!timerActive) {
                // ── PICKER PHASE ──────────────────────────────────────

                Text(
                    text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Thin,
                    fontFamily = fontFamily,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("H", "M", "S").forEachIndexed { idx, label ->
                        val selected = unit == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (selected) OrangeFire else Color.Transparent)
                                .clickable { unit = idx }
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.Black else Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                val currentValue = when (unit) { 0 -> hours; 1 -> minutes; else -> seconds }
                val maxValue     = if (unit == 0) 23 else 59

                CircularDialPicker(
                    value    = currentValue,
                    maxValue = maxValue,
                    onSet    = {
                        val t = hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L
                        if (t > 0L) {
                            totalMs = t; remainingMs = t
                            timerActive = true; isRunning = true
                        }
                    }
                ) { newVal ->
                    when (unit) {
                        0 -> hours = newVal
                        1 -> when {
                            minutes > 50 && newVal < 10 -> { hours = (hours + 1).coerceAtMost(12); minutes = newVal }
                            minutes < 10 && newVal > 50 -> { if (hours > 0) { hours -= 1; minutes = newVal } }
                            else -> minutes = newVal
                        }
                        2 -> seconds = newVal
                    }
                }

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
                        .clickable { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Skip", color = Color.White.copy(alpha = 0.55f), fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = BebasFont)
                }

            } else {
                // ── COUNTDOWN PHASE ───────────────────────────────────

                val fraction = if (totalMs > 0L) remainingMs.toFloat() / totalMs.toFloat() else 0f
                val done     = remainingMs == 0L

                TimerCountdownRing(
                    fraction   = fraction,
                    label      = fmtCountdown(remainingMs),
                    fontFamily = fontFamily,
                    done       = done
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PAUSE / RESUME
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (done) Color.White.copy(alpha = 0.08f) else OrangeFire)
                            .clickable(enabled = !done) { isRunning = !isRunning },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isRunning) "PAUSE" else "RESUME",
                            color = if (done) Color.White.copy(alpha = 0.3f) else Color.Black,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
                            fontFamily = BebasFont
                        )
                    }
                    // RESTART — resets to full duration and starts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF3A0A0A))
                            .border(1.dp, Color(0xFFFF1C1C).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable {
                                remainingMs = totalMs
                                isRunning   = true
                                restartKey++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("RESTART", color = Color(0xFFFF1C1C), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = BebasFont)
                    }
                    // STOP — back to picker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
                            .clickable {
                                isRunning = false; timerActive = false
                                remainingMs = 0L; totalMs = 0L
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("STOP", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = BebasFont)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Skip", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, fontFamily = BebasFont)
                }
            }
        }
    }
}

@Composable
private fun TimerCountdownRing(
    fraction: Float,
    label: String,
    fontFamily: FontFamily,
    done: Boolean
) {
    val dialDp  = 210.dp
    val density = LocalDensity.current
    val dialPx  = with(density) { dialDp.toPx() }
    val cx      = dialPx / 2f
    val ringR   = cx - with(density) { 14.dp.toPx() }

    val neonRed = Color(0xFFFF1C1C)

    // Hand sweeps clockwise from 12 o'clock as time counts down
    val handAngle  = (-PI / 2.0 + (1.0 - fraction.toDouble()) * 2.0 * PI).toFloat()
    val handLen    = ringR * 0.78f
    val hX         = cx + handLen * cos(handAngle)
    val hY         = cx + handLen * sin(handAngle)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(dialDp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(cx, cx)

                // ── Outer glow bloom (multiple stroke passes) ──
                listOf(14f to 0.04f, 9f to 0.06f, 5f to 0.09f, 2.5f to 0.14f).forEach { (w, a) ->
                    drawCircle(
                        color = neonRed.copy(alpha = a),
                        radius = ringR, center = center,
                        style = Stroke(width = w.dp.toPx())
                    )
                }
                // Crisp ring
                drawCircle(color = neonRed, radius = ringR, center = center, style = Stroke(1.8.dp.toPx()))

                // ── Tick marks ─────────────────────────────────
                for (i in 0 until 60) {
                    val a      = (-PI / 2.0 + i.toDouble() / 60.0 * 2.0 * PI).toFloat()
                    val isLong = i % 5 == 0
                    val oR     = ringR - 2.dp.toPx()
                    val iR     = oR - if (isLong) 14.dp.toPx() else 6.dp.toPx()
                    val cosA   = cos(a);  val sinA = sin(a)
                    // Tick glow
                    if (isLong) drawLine(
                        color = neonRed.copy(alpha = 0.30f),
                        start = Offset(cx + oR * cosA, cx + oR * sinA),
                        end   = Offset(cx + iR * cosA, cx + iR * sinA),
                        strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round
                    )
                    drawLine(
                        color = neonRed.copy(alpha = if (isLong) 1f else 0.55f),
                        start = Offset(cx + oR * cosA, cx + oR * sinA),
                        end   = Offset(cx + iR * cosA, cx + iR * sinA),
                        strokeWidth = if (isLong) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // ── Hand glow layers ───────────────────────────
                listOf(10f to 0.04f, 6f to 0.09f, 3.5f to 0.18f).forEach { (w, a) ->
                    drawLine(
                        color = neonRed.copy(alpha = a),
                        start = center, end = Offset(hX, hY),
                        strokeWidth = w.dp.toPx(), cap = StrokeCap.Round
                    )
                }
                // Hand core — white
                drawLine(
                    color = Color.White,
                    start = center, end = Offset(hX, hY),
                    strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round
                )

                // ── Center dot ─────────────────────────────────
                drawCircle(color = neonRed.copy(alpha = 0.20f), radius = 13.dp.toPx(), center = center)
                drawCircle(color = neonRed.copy(alpha = 0.45f), radius =  7.dp.toPx(), center = center)
                drawCircle(color = Color.White,                  radius =  3.5.dp.toPx(), center = center)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Neon red time label below clock
        Text(
            text  = if (done) "DONE" else label,
            color = neonRed,
            fontSize   = if (!done && label.length > 5) 24.sp else 28.sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = neonRed, blurRadius = 20f)
            )
        )
    }
}

@Composable
fun CircularDialPicker(
    value: Int,
    maxValue: Int,
    onSet: () -> Unit = {},
    onValueChange: (Int) -> Unit
) {
    val dialDp = 210.dp
    val density = LocalDensity.current

    val dialPx       = with(density) { dialDp.toPx() }
    val cx           = dialPx / 2f
    val outerRadius  = cx - with(density) { 8.dp.toPx() }
    val innerRadius  = outerRadius * 0.56f
    val trackRadius  = (outerRadius + innerRadius) / 2f
    val handlePx     = with(density) { 21.dp.toPx() }
    val arcStroke    = (outerRadius - innerRadius) * 0.72f

    val handleAngle  = (-PI / 2.0 + value.toDouble() / (maxValue + 1).toDouble() * 2.0 * PI).toFloat()
    val handleX      = cx + trackRadius * cos(handleAngle)
    val handleY      = cx + trackRadius * sin(handleAngle)

    fun updateFromPos(x: Float, y: Float) {
        val dx = x - cx
        val dy = y - cx
        var a = atan2(dy.toDouble(), dx.toDouble()) + PI / 2.0
        if (a < 0.0) a += 2.0 * PI
        onValueChange(((a / (2.0 * PI)) * (maxValue + 1)).toInt().coerceIn(0, maxValue))
    }

    Box(modifier = Modifier.size(dialDp)) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxValue) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            updateFromPos(pos.x, pos.y)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            val center = Offset(cx, cx)

            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF3A3A50), Color(0xFF22222E)), center = center),
                radius = outerRadius, center = center
            )
            drawCircle(color = Color.White.copy(alpha = 0.1f), radius = outerRadius, center = center, style = Stroke(width = 1.5.dp.toPx()))

            val sweep = (value.toFloat() / (maxValue + 1).toFloat()) * 360f
            if (sweep > 0f) {
                drawArc(
                    color = OrangeFire, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    style = Stroke(width = arcStroke, cap = StrokeCap.Round),
                    topLeft = Offset(cx - trackRadius, cx - trackRadius), size = Size(trackRadius * 2f, trackRadius * 2f)
                )
                drawArc(
                    color = OrangeFire.copy(alpha = 0.25f), startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    style = Stroke(width = arcStroke + 6.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(cx - trackRadius, cx - trackRadius), size = Size(trackRadius * 2f, trackRadius * 2f)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF1C1C28), Color(0xFF0E0E18)), center = center),
                radius = innerRadius, center = center
            )
            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = innerRadius, center = center, style = Stroke(width = 1.dp.toPx()))

            val tickStart = Offset(cx, cx - outerRadius + 6.dp.toPx())
            val tickEnd   = Offset(cx, cx - innerRadius - 4.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.3f), tickStart, tickEnd, strokeWidth = 2.dp.toPx())

            drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = handlePx + 2f, center = Offset(handleX, handleY + 3f))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color(0xFFDDDDDD)), center = Offset(handleX, handleY), radius = handlePx),
                radius = handlePx, center = Offset(handleX, handleY)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset((handleX - handlePx).roundToInt(), (handleY - handlePx).roundToInt()) }
                .size(with(density) { (handlePx * 2f).toDp() }),
            contentAlignment = Alignment.Center
        ) {
            Text(String.format("%02d", value), color = Color(0xFF1A1A1A), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White, Color(0xFFE8E8E8))))
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSet() },
            contentAlignment = Alignment.Center
        ) {
            Text("SET", color = Color(0xFF1A1A1A), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        }
    }
}
