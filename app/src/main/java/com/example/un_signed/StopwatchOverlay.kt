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
    // ── Restore persisted state (survives app kill) ────────────
    val initial = remember { FitDataRepository.loadStopwatchState() }
    val initialElapsed = remember(initial) {
        if (initial.running && initial.savedAtEpochMs > 0)
            initial.elapsedMsAtSave + (System.currentTimeMillis() - initial.savedAtEpochMs).coerceAtLeast(0L)
        else initial.elapsedMsAtSave
    }

    var isRunning    by remember { mutableStateOf(initial.running) }
    var elapsedMs    by remember { mutableStateOf(initialElapsed) }
    var laps         by remember { mutableStateOf(initial.laps) }
    var lastLapMs    by remember { mutableStateOf(initial.lastLapMs) }
    var pendingReset by remember { mutableStateOf(false) }

    // Plain array refs — no State overhead in the hot timing loop
    val startedAt   = remember { LongArray(1) { 0L } }
    val baseElapsed = remember { LongArray(1) { 0L } }

    fun persist() {
        FitDataRepository.saveStopwatchState(
            StopwatchPersistedState(
                laps = laps,
                elapsedMsAtSave = elapsedMs,
                running = isRunning,
                savedAtEpochMs = System.currentTimeMillis(),
                lastLapMs = lastLapMs
            )
        )
    }

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

    // Periodic safety-net save while running (every 5s) so a crash loses ≤ 5s
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (true) {
                delay(5_000L)
                persist()
            }
        } else {
            persist()   // save on every pause
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
    val secSmooth  = (elapsedMs % 60_000L) / 60_000.0                          // per minute
    val minSmooth  = (elapsedMs % 3_600_000L) / 3_600_000.0                    // per hour
    val hourSmooth = (elapsedMs % (12L * 3_600_000L)) / (12.0 * 3_600_000.0)  // per 12 hours
    val secInt     = ((elapsedMs / 1000L) % 60L).toInt()
    val minInt     = ((elapsedMs / 60_000L) % 60L).toInt()
    val hourInt    = ((elapsedMs / 3_600_000L) % 12L).toInt()
    val showStar   = elapsedMs >= 3_600_000L                                    // ≥ 1 hour

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { persist(); onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(310.dp)
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
                    fontFamily = NokiaFont,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Circular watch face ───────────────────────────
            StopwatchFace(
                secSmooth      = secSmooth,
                minSmooth      = minSmooth,
                hourSmooth     = hourSmooth,
                secInt         = secInt,
                minInt         = minInt,
                hourInt        = hourInt,
                isRunning      = isRunning,
                showGoldenStar = showStar,
                onPlayPause    = { isRunning = !isRunning; if (isRunning) pendingReset = false },
                fontFamily     = fontFamily
            )

            Spacer(Modifier.height(18.dp))

            // ── LAP | RESET | SKIP ────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SwBtn("LAP",   Color(0xFF23232F), Modifier.weight(1f), onClick = {
                    if (elapsedMs > 0L) {
                        val snap  = snapMs()
                        laps      = laps + LapEntry(laps.size + 1, snap - lastLapMs, snap)
                        lastLapMs = snap
                        persist()
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
                            persist()
                        } else {
                            // 2nd press: wipe all lap data
                            laps = emptyList(); pendingReset = false
                            FitDataRepository.clearStopwatchState()
                        }
                    },
                    fontFamily = BebasFont
                )
                SwBtn("SKIP",  Color(0xFF122A12), Modifier.weight(1f), onClick = { persist(); onSkip() }, fontFamily = BebasFont)
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
    minSmooth: Double,
    hourSmooth: Double,
    secInt: Int,
    minInt: Int,
    hourInt: Int,
    isRunning: Boolean,
    showGoldenStar: Boolean,
    onPlayPause: () -> Unit,
    fontFamily: FontFamily
) {
    val dialDp  = 230.dp
    val density = LocalDensity.current

    val dialPx      = with(density) { dialDp.toPx() }
    val cx          = dialPx / 2f
    val outerRadius = cx - with(density) { 5.dp.toPx() }
    val innerRadius = outerRadius * 0.60f
    val trackRadius = (outerRadius + innerRadius) / 2f
    val handlePx    = with(density) { 22.dp.toPx() }
    val ppPx        = with(density) { 17.dp.toPx() }
    val minBubblePx = with(density) { 18.dp.toPx() }
    val hourBubblePx = with(density) { 14.dp.toPx() }
    // Hours orbit just inside the outer chrome rim
    val hourTrackR  = (trackRadius + outerRadius) / 2f

    // Seconds hand sweeps smoothly around the dial
    val handleAngle = (-PI / 2.0 + secSmooth * 2.0 * PI).toFloat()
    val handleX     = cx + innerRadius * cos(handleAngle)
    val handleY     = cx + innerRadius * sin(handleAngle)

    // Play/pause floats diagonally opposite the seconds hand, inside inner circle
    val ppAngle = (handleAngle.toDouble() + PI).toFloat()
    val ppDist  = innerRadius * 0.50f
    val ppX     = cx + ppDist * cos(ppAngle)
    val ppY     = cx + ppDist * sin(ppAngle)

    // Minutes bubble orbits the middle of the silver ring once per hour
    val minAngle = (-PI / 2.0 + minSmooth * 2.0 * PI).toFloat()
    val minX     = cx + trackRadius * cos(minAngle)
    val minY     = cx + trackRadius * sin(minAngle)

    // Hours bubble orbits the outer half of the silver ring once per 12 hours
    val hourAngle = (-PI / 2.0 + hourSmooth * 2.0 * PI).toFloat()
    val hourX     = cx + hourTrackR * cos(hourAngle)
    val hourY     = cx + hourTrackR * sin(hourAngle)

    Box(modifier = Modifier.size(dialDp)) {

        // ── Main disc drawing ──────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(cx, cx)

            // ── 1. Large soft drop shadow ──────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(0.60f),
                        Color.Black.copy(0.28f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cx + 22.dp.toPx()),
                    radius = outerRadius + 30.dp.toPx()
                ),
                radius = outerRadius + 30.dp.toPx(),
                center = Offset(cx, cx + 22.dp.toPx())
            )

            // ── 2. Outer disc — off-centre light source ────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFEEEEEE), Color(0xFFCCCCCC), Color(0xFF9E9E9E)),
                    center = Offset(cx - outerRadius * 0.36f, cx - outerRadius * 0.38f),
                    radius = outerRadius * 1.55f
                ),
                radius = outerRadius, center = center
            )
            // Top-left specular blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(0.78f), Color.Transparent),
                    center = Offset(cx - outerRadius * 0.38f, cx - outerRadius * 0.42f),
                    radius = outerRadius * 0.50f
                ),
                radius = outerRadius, center = center
            )
            // Bottom-right shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(0.32f)),
                    center = Offset(cx + outerRadius * 0.30f, cx + outerRadius * 0.34f),
                    radius = outerRadius * 0.95f
                ),
                radius = outerRadius, center = center
            )

            // ── 3. Outer edge bevel arcs ───────────────────────
            val edgeR = outerRadius - 1.dp.toPx()
            val edgeBox = Size(edgeR * 2f, edgeR * 2f)
            val edgeTL  = Offset(cx - edgeR, cx - edgeR)
            // Bright arc — top-left (light hits the rim)
            drawArc(
                color = Color.White.copy(0.80f),
                startAngle = 195f, sweepAngle = 150f, useCenter = false,
                topLeft = edgeTL, size = edgeBox,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Dark arc — bottom-right (shadow side of rim)
            drawArc(
                color = Color(0xFF505050).copy(0.65f),
                startAngle = 15f, sweepAngle = 150f, useCenter = false,
                topLeft = edgeTL, size = edgeBox,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // ── 4. Chrome bevel ring — separates disc from hole ─
            val bevR   = innerRadius + 6.dp.toPx()
            val bevBox = Size(bevR * 2f, bevR * 2f)
            val bevTL  = Offset(cx - bevR, cx - bevR)
            drawCircle(color = Color(0xFF1A1A1A), radius = bevR, center = center)
            // Bright bevel highlight — top-left
            drawArc(
                color = Color(0xFFD8D8D8).copy(0.88f),
                startAngle = 200f, sweepAngle = 142f, useCenter = false,
                topLeft = bevTL, size = bevBox,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Dark bevel shadow — bottom-right
            drawArc(
                color = Color.Black.copy(0.72f),
                startAngle = 22f, sweepAngle = 142f, useCenter = false,
                topLeft = bevTL, size = bevBox,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // ── 5. Inner sunken disc ───────────────────────────
            drawCircle(color = Color(0xFF080808), radius = innerRadius, center = center)
            // Top inner shadow (light from above casts shadow into the recess)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(0.80f), Color.Transparent),
                    center = Offset(cx, cx - innerRadius * 0.48f),
                    radius = innerRadius * 0.72f
                ),
                radius = innerRadius, center = center
            )
            // Subtle bottom bounce light
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF1C1C1C)),
                    center = Offset(cx, cx + innerRadius * 0.52f),
                    radius = innerRadius * 0.55f
                ),
                radius = innerRadius, center = center
            )
            // Thin inner rim highlight at top (grazing light on the lip)
            drawArc(
                color = Color.White.copy(0.14f),
                startAngle = 192f, sweepAngle = 156f, useCenter = false,
                topLeft = Offset(cx - innerRadius + 1f, cx - innerRadius + 1f),
                size = Size((innerRadius - 1f) * 2f, (innerRadius - 1f) * 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // ── 6. Hours bubble — 3-D sphere (deep navy) ──────
            drawCircle(
                color = Color.Black.copy(0.50f),
                radius = hourBubblePx + 4f,
                center = Offset(hourX, hourY + 5f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3A5FA8), Color(0xFF162B6E), Color(0xFF080E2A)),
                    center = Offset(hourX - hourBubblePx * 0.26f, hourY - hourBubblePx * 0.30f),
                    radius = hourBubblePx * 1.15f
                ),
                radius = hourBubblePx, center = Offset(hourX, hourY)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8AABF0).copy(0.60f), Color.Transparent),
                    center = Offset(hourX - hourBubblePx * 0.30f, hourY - hourBubblePx * 0.34f),
                    radius = hourBubblePx * 0.40f
                ),
                radius = hourBubblePx, center = Offset(hourX, hourY)
            )

            // ── 7. Minutes bubble — 3-D sphere ────────────────
            drawCircle(
                color = Color.Black.copy(0.45f),
                radius = minBubblePx + 4f,
                center = Offset(minX, minY + 5f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9B5A26), Color(0xFF5C2E0D), Color(0xFF2A1005)),
                    center = Offset(minX - minBubblePx * 0.26f, minY - minBubblePx * 0.30f),
                    radius = minBubblePx * 1.15f
                ),
                radius = minBubblePx, center = Offset(minX, minY)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD4956A).copy(0.55f), Color.Transparent),
                    center = Offset(minX - minBubblePx * 0.30f, minY - minBubblePx * 0.34f),
                    radius = minBubblePx * 0.42f
                ),
                radius = minBubblePx, center = Offset(minX, minY)
            )

            // ── 7. Seconds bubble — 3-D sphere ────────────────
            drawCircle(
                color = Color.Black.copy(0.42f),
                radius = handlePx + 5f,
                center = Offset(handleX, handleY + 6f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFEEEEEE), Color(0xFFD4D4D4)),
                    center = Offset(handleX - handlePx * 0.26f, handleY - handlePx * 0.30f),
                    radius = handlePx * 1.15f
                ),
                radius = handlePx, center = Offset(handleX, handleY)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(0.88f), Color.Transparent),
                    center = Offset(handleX - handlePx * 0.34f, handleY - handlePx * 0.38f),
                    radius = handlePx * 0.40f
                ),
                radius = handlePx, center = Offset(handleX, handleY)
            )
        }

        // ── Hours text inside navy bubble ─────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset((hourX - hourBubblePx).roundToInt(), (hourY - hourBubblePx).roundToInt()) }
                .size(with(density) { (hourBubblePx * 2f).toDp() }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", hourInt),
                color = Color(0xFFABC4F5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = NokiaFont
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
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = NokiaFont
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
                color = Color(0xFF0A0A0A),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = NokiaFont
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
