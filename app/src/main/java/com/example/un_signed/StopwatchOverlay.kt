package com.example.un_signed

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlin.math.*

data class LapEntry(val number: Int, val splitMs: Long, val absoluteMs: Long)

@Composable
fun StopwatchOverlay(
    fontFamily: FontFamily,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    var isRunning    by remember { mutableStateOf(false) }
    var elapsedMs    by remember { mutableStateOf(0L) }
    var laps         by remember { mutableStateOf(listOf<LapEntry>()) }
    var lastLapMs    by remember { mutableStateOf(0L) }
    var pendingReset by remember { mutableStateOf(false) }

    // Plain array refs — no State overhead in the hot timing loop
    val startedAt   = remember { LongArray(1) { 0L } }
    val baseElapsed = remember { LongArray(1) { 0L } }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            baseElapsed[0] = elapsedMs
            startedAt[0]   = SystemClock.elapsedRealtime()
            while (true) {
                elapsedMs = baseElapsed[0] + (SystemClock.elapsedRealtime() - startedAt[0])
                delay(16L)
            }
        }
    }

    // Captures exact ms at the instant of the call — no polling lag
    fun snapMs(): Long =
        if (isRunning) baseElapsed[0] + (SystemClock.elapsedRealtime() - startedAt[0])
        else elapsedMs

    fun fmt(ms: Long): String {
        val min   = ms / 60000L
        val sec   = (ms % 60000L) / 1000L
        val centi = (ms % 1000L) / 10L
        return String.format("%02d:%02d.%02d", min, sec, centi)
    }

    // Values derived from elapsed time for the watch face
    val secSmooth = (elapsedMs % 60_000L) / 60_000.0      // 0.0–1.0 continuous
    val secInt    = ((elapsedMs / 1000L) % 60L).toInt()   // 0–59 integer
    val minInt    = ((elapsedMs / 60_000L) % 60L).toInt() // 0–59 minutes
    val showStar  = elapsedMs >= 3_600_000L                // ≥ 1 hour

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
                        listOf(Color(0xFF1A1A28).copy(alpha = 0.97f), Color(0xFF0E0E18).copy(alpha = 0.97f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Title ─────────────────────────────────────────
            Text(
                "STOPWATCH",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = BebasFont
            )

            Spacer(Modifier.height(10.dp))

            // ── LED matrix time display ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF080812)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sp = 8.dp.toPx()
                    val r  = 1.dp.toPx()
                    var y = sp / 2f
                    while (y < size.height) {
                        var x = sp / 2f
                        while (x < size.width) {
                            drawCircle(Color.White.copy(alpha = 0.045f), r, Offset(x, y))
                            x += sp
                        }
                        y += sp
                    }
                }
                Text(
                    text = fmt(elapsedMs),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontFamily = fontFamily,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Circular watch face ───────────────────────────
            StopwatchFace(
                secSmooth    = secSmooth,
                secInt       = secInt,
                minInt       = minInt,
                isRunning    = isRunning,
                showGoldenStar = showStar,
                onPlayPause  = { isRunning = !isRunning; if (isRunning) pendingReset = false },
                fontFamily   = fontFamily
            )

            Spacer(Modifier.height(18.dp))

            // ── LAP | RESET | SKIP ────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SwBtn("LAP",   Color(0xFF23232F), Modifier.weight(1f), onClick = {
                    if (elapsedMs > 0L) {
                        val snap  = snapMs()
                        laps      = laps + LapEntry(laps.size + 1, snap - lastLapMs, snap)
                        lastLapMs = snap
                    }
                }, fontFamily = BebasFont)
                SwBtn(
                    "RESET",
                    if (pendingReset) Color(0xFF6B1010) else Color(0xFF2A1212),
                    Modifier.weight(1f),
                    onClick = {
                        if (!pendingReset) {
                            // 1st press: stop + reset ring to 0, keep laps + total display
                            isRunning = false; elapsedMs = 0L
                            startedAt[0] = 0L; baseElapsed[0] = 0L
                            lastLapMs = 0L; pendingReset = true
                        } else {
                            // 2nd press: wipe all lap data
                            laps = emptyList(); pendingReset = false
                        }
                    },
                    fontFamily = BebasFont
                )
                SwBtn("SKIP",  Color(0xFF122A12), Modifier.weight(1f), onClick = onSkip, fontFamily = BebasFont)
            }

            // ── Laps (last 4, newest first) ───────────────────
            if (laps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    laps.takeLast(4).reversed().forEach { lap ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Lap ${lap.number}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontFamily = fontFamily
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    fmt(lap.splitMs),
                                    color = OrangeFire,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = fontFamily
                                )
                                Text(
                                    fmt(lap.absoluteMs),
                                    color = Color.White.copy(alpha = 0.32f),
                                    fontSize = 11.sp,
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopwatchFace(
    secSmooth: Double,
    secInt: Int,
    minInt: Int,
    isRunning: Boolean,
    showGoldenStar: Boolean,
    onPlayPause: () -> Unit,
    fontFamily: FontFamily
) {
    val dialDp  = 200.dp
    val density = LocalDensity.current

    val dialPx      = with(density) { dialDp.toPx() }
    val cx          = dialPx / 2f
    val outerRadius = cx - with(density) { 5.dp.toPx() }
    val innerRadius = outerRadius * 0.60f
    val trackRadius = (outerRadius + innerRadius) / 2f
    val handlePx    = with(density) { 22.dp.toPx() }
    val ppPx        = with(density) { 17.dp.toPx() }
    val minBubblePx = with(density) { 18.dp.toPx() }

    // Seconds hand sweeps smoothly around the dial
    val handleAngle = (-PI / 2.0 + secSmooth * 2.0 * PI).toFloat()
    val handleX     = cx + innerRadius * cos(handleAngle)
    val handleY     = cx + innerRadius * sin(handleAngle)

    // Play/pause floats diagonally opposite the seconds hand, inside inner circle
    val ppAngle = (handleAngle.toDouble() + PI).toFloat()
    val ppDist  = innerRadius * 0.50f
    val ppX     = cx + ppDist * cos(ppAngle)
    val ppY     = cx + ppDist * sin(ppAngle)

    // Minutes bubble fixed at 6 o'clock position in the ring
    val minX = cx
    val minY = cx + trackRadius

    Box(modifier = Modifier.size(dialDp)) {

        // ── Main disc drawing ──────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(cx, cx)

            // Drop shadow under disc
            drawCircle(
                color = Color.Black.copy(alpha = 0.30f),
                radius = outerRadius + 6.dp.toPx(),
                center = Offset(cx, cx + 6.dp.toPx())
            )

            // Outer disc — light silver/gray
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF6F6F6), Color(0xFFDADADA), Color(0xFFC2C2C2)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFFAAAAAA),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner black circle
            drawCircle(color = Color(0xFF0D0D0D), radius = innerRadius, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Minutes bubble — brown, fixed 6 o'clock in ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6B3A18), Color(0xFF3A1C08)),
                    center = Offset(minX, minY),
                    radius = minBubblePx
                ),
                radius = minBubblePx,
                center = Offset(minX, minY)
            )

            // Seconds hand bubble shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = handlePx + 3f,
                center = Offset(handleX, handleY + 4f)
            )
            // Seconds hand bubble — white
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE6E6E6)),
                    center = Offset(handleX, handleY),
                    radius = handlePx
                ),
                radius = handlePx,
                center = Offset(handleX, handleY)
            )
        }

        // ── Minutes text inside brown bubble ──────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset((minX - minBubblePx).roundToInt(), (minY - minBubblePx).roundToInt()) }
                .size(with(density) { (minBubblePx * 2f).toDp() }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", minInt),
                color = Color(0xFFD4956A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = fontFamily
            )
        }

        // ── Seconds value inside white bubble ─────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset((handleX - handlePx).roundToInt(), (handleY - handlePx).roundToInt()) }
                .size(with(density) { (handlePx * 2f).toDp() }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", secInt),
                color = Color(0xFF111111),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = fontFamily
            )
        }

        // ── Play/pause — white circle, black icon ─────────────
        val ppSizeDp = with(density) { (ppPx * 2f).toDp() }
        Box(
            modifier = Modifier
                .offset { IntOffset((ppX - ppPx).roundToInt(), (ppY - ppPx).roundToInt()) }
                .size(ppSizeDp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ppSizeDp * 0.55f)) {
                if (isRunning) {
                    // ⏸ two black rounded bars
                    val bw  = size.width * 0.26f
                    val bh  = size.height * 0.62f
                    val gap = size.width * 0.20f
                    val x0  = (size.width - 2 * bw - gap) / 2f
                    val y0  = (size.height - bh) / 2f
                    drawRoundRect(Color.Black, Offset(x0, y0),            Size(bw, bh), CornerRadius(bw / 2f))
                    drawRoundRect(Color.Black, Offset(x0 + bw + gap, y0), Size(bw, bh), CornerRadius(bw / 2f))
                } else {
                    // ▶ black triangle
                    drawPath(
                        Path().apply {
                            moveTo(size.width * 0.30f, size.height * 0.10f)
                            lineTo(size.width * 0.94f, size.height * 0.50f)
                            lineTo(size.width * 0.30f, size.height * 0.90f)
                            close()
                        },
                        Color.Black
                    )
                }
            }
        }

        // ── Golden star at center after 1 hour ────────────────
        if (showGoldenStar) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text("✳", color = Color(0xFFFFD700), fontSize = 22.sp, fontWeight = FontWeight.Light, fontFamily = fontFamily)
            }
        }
    }
}

@Composable
private fun SwBtn(label: String, bg: Color, modifier: Modifier = Modifier, onClick: () -> Unit, fontFamily: FontFamily) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = fontFamily)
    }
}
