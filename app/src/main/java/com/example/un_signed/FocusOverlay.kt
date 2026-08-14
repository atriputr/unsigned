package com.example.un_signed

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun FocusOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val persisted = remember { FitDataRepository.loadFocusState() }
    val skills = remember { FitDataRepository.loadSkillItems() }

    // Restore state: if it was running when killed, compute new remaining from wall-clock
    val restoredRemaining = remember(persisted) {
        if (!persisted.active) 0L
        else if (persisted.running)
            (persisted.remainingMsAtSave - (System.currentTimeMillis() - persisted.savedAtEpochMs)).coerceAtLeast(0L)
        else
            persisted.remainingMsAtSave
    }

    var focusMin      by remember { mutableStateOf(if (persisted.active) persisted.focusMinutes else 25) }
    var breakMin      by remember { mutableStateOf(if (persisted.active) persisted.breakMinutes else 5) }
    var cycleGoal     by remember { mutableStateOf(if (persisted.active) persisted.cycleGoal else 4) }
    var completedCycles by remember { mutableStateOf(persisted.completedCycles) }
    var phase         by remember { mutableStateOf(persisted.phase.ifBlank { "FOCUS" }) }
    var active        by remember { mutableStateOf(persisted.active) }
    var running       by remember { mutableStateOf(persisted.active && persisted.running && restoredRemaining > 0L) }
    var remainingMs   by remember { mutableStateOf(restoredRemaining) }
    var selectedSkillId by remember { mutableStateOf(persisted.skillId) }
    var selectedSkillName by remember { mutableStateOf(persisted.skillName) }

    val startedAt = remember { LongArray(1) { 0L } }
    val baseRemain = remember { LongArray(1) { 0L } }

    fun currentPhaseTotalMs(): Long = ((if (phase == "FOCUS") focusMin else breakMin) * 60_000L).coerceAtLeast(1L)

    fun persist() {
        FitDataRepository.saveFocusState(
            FocusTimerState(
                active = active,
                running = running,
                phase = phase,
                focusMinutes = focusMin,
                breakMinutes = breakMin,
                cycleGoal = cycleGoal,
                completedCycles = completedCycles,
                remainingMsAtSave = remainingMs,
                savedAtEpochMs = System.currentTimeMillis(),
                skillId = selectedSkillId,
                skillName = selectedSkillName
            )
        )
    }

    // Play alert once when phase hits zero
    var alerted by remember { mutableStateOf(false) }

    LaunchedEffect(running, phase) {
        if (running && remainingMs > 0L) {
            baseRemain[0] = remainingMs
            startedAt[0] = SystemClock.elapsedRealtime()
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt[0]
                remainingMs = (baseRemain[0] - elapsed).coerceAtLeast(0L)
                if (remainingMs == 0L) { running = false; break }
                delay(100L)
            }
        }
    }

    // On phase completion — advance
    LaunchedEffect(remainingMs, active) {
        if (active && remainingMs == 0L && !alerted) {
            alerted = true
            try {
                val mp = MediaPlayer.create(ctx, R.raw.timer_alert)
                mp?.setOnCompletionListener { it.release() }
                mp?.start()
            } catch (_: Exception) {}
            Haptics.success(ctx)
            if (phase == "FOCUS") {
                completedCycles += 1
                // Log the focus session immediately (in case user leaves)
                val all = FitDataRepository.loadFocusSessions().toMutableList()
                all.add(
                    FocusSession(
                        focusMinutes = focusMin,
                        breakMinutes = breakMin,
                        completedCycles = 1,
                        skillId = selectedSkillId,
                        skillName = selectedSkillName
                    )
                )
                FitDataRepository.saveFocusSessions(all)
                // Add minutes to linked skill
                selectedSkillId?.let { sid ->
                    val allSkills = FitDataRepository.loadSkillItems()
                    val s = allSkills.firstOrNull { it.id == sid }
                    if (s != null) {
                        val updated = s.copy(
                            totalMinutes = s.totalMinutes + focusMin,
                            sessions = s.sessions + SkillSession(durationMinutes = focusMin, notes = "Pomodoro")
                        )
                        FitDataRepository.saveSkillItems(allSkills.map { if (it.id == sid) updated else it })
                    }
                }

                if (completedCycles >= cycleGoal) {
                    // All done
                    active = false; running = false
                    phase = "FOCUS"
                    remainingMs = 0L
                    FitDataRepository.clearFocusState()
                } else {
                    phase = "BREAK"
                    remainingMs = breakMin * 60_000L
                    running = true
                }
            } else {
                phase = "FOCUS"
                remainingMs = focusMin * 60_000L
                running = true
            }
            alerted = false
            persist()
        }
    }

    // Safety-net save every 5s while running; save on pause
    LaunchedEffect(running, active) {
        if (active && running) { while (true) { delay(5_000L); persist() } }
        else persist()
    }

    val palette = LocalPalette.current
    val phaseAccent = if (phase == "FOCUS") Color(0xFFFF6B47) else Color(0xFF6ACBEA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                persist(); onClose()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF171422).copy(alpha = 0.97f), Color(0xFF08060E).copy(alpha = 0.97f)))
                )
                .border(1.5.dp, Brush.verticalGradient(listOf(phaseAccent.copy(alpha = 0.55f), Color.Transparent, phaseAccent.copy(alpha = 0.25f))), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (active) phase else "FOCUS SESSION",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(shadow = Shadow(color = phaseAccent, blurRadius = 12f))
            )
            Text(
                if (active) "Cycle ${completedCycles + (if (remainingMs > 0) 1 else 0)} / $cycleGoal"
                else "Pomodoro method · $focusMin min focus / $breakMin min break",
                color = phaseAccent.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            if (active) {
                // ── Active session ─────────────────────────
                val totalMs = currentPhaseTotalMs()
                val progress = 1f - (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                CircleTimer(
                    label = fmtTime(remainingMs),
                    progress = progress,
                    accent = phaseAccent
                )

                Spacer(Modifier.height(14.dp))

                // Cycle dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(cycleGoal) { i ->
                        val filled = i < completedCycles
                        Box(
                            modifier = Modifier
                                .size(if (filled) 12.dp else 10.dp)
                                .clip(CircleShape)
                                .background(if (filled) phaseAccent else Color.White.copy(alpha = 0.15f))
                                .border(1.dp, if (filled) phaseAccent else Color.White.copy(alpha = 0.25f), CircleShape)
                        )
                    }
                }

                if (selectedSkillName.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("→ ${selectedSkillName}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pause / Resume
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(phaseAccent)
                            .clickable {
                                running = !running
                                Haptics.click(ctx); persist()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (running) "PAUSE" else "RESUME", color = Color.Black, fontSize = 12.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    // Skip phase
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                remainingMs = 0L
                                Haptics.click(ctx); persist()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SKIP", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    // Stop
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A0F0F))
                            .border(1.dp, Color(0xFFFF3030).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                active = false; running = false; remainingMs = 0L
                                completedCycles = 0; phase = "FOCUS"
                                FitDataRepository.clearFocusState()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("STOP", color = Color(0xFFFF6666), fontSize = 12.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                }
            } else {
                // ── Configuration ──────────────────────────
                ConfigRow("FOCUS", focusMin, "min", listOf(15, 20, 25, 30, 45, 50), contentFont, phaseAccent) { focusMin = it }
                Spacer(Modifier.height(6.dp))
                ConfigRow("BREAK", breakMin, "min", listOf(3, 5, 10, 15), contentFont, Color(0xFF6ACBEA)) { breakMin = it }
                Spacer(Modifier.height(6.dp))
                ConfigRow("CYCLES", cycleGoal, "×", listOf(2, 3, 4, 5, 6), contentFont, Color(0xFF9B7EEA)) { cycleGoal = it }

                // Skill picker
                if (skills.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("LINK TO SKILL (optional)", color = phaseAccent, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        item {
                            SkillChip("— none —", selectedSkillId == null, contentFont) {
                                selectedSkillId = null; selectedSkillName = ""
                            }
                        }
                        items(skills) { s ->
                            SkillChip(s.name, selectedSkillId == s.id, contentFont) {
                                selectedSkillId = s.id; selectedSkillName = s.name
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(phaseAccent)
                        .clickable {
                            phase = "FOCUS"
                            completedCycles = 0
                            remainingMs = focusMin * 60_000L
                            active = true; running = true
                            Haptics.click(ctx); persist()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("BEGIN FOCUS", color = Color.Black, fontSize = 15.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                }

                // Today's summary
                val todaySessions = remember {
                    FitDataRepository.loadFocusSessions().filter {
                        Instant.ofEpochMilli(it.startedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
                    }
                }
                if (todaySessions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val minutesToday = todaySessions.sumOf { it.totalFocusMinutes }
                    val weekMinutes = FitDataRepository.loadFocusSessions()
                        .filter {
                            val d = Instant.ofEpochMilli(it.startedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
                            !d.isBefore(LocalDate.now().with(DayOfWeek.MONDAY))
                        }.sumOf { it.totalFocusMinutes }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        StatBadge("TODAY", "${minutesToday}m", contentFont)
                        StatBadge("THIS WEEK", "${weekMinutes}m", contentFont)
                        StatBadge("CYCLES", "${todaySessions.sumOf { it.completedCycles }}", contentFont)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("CLOSE", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { persist(); onClose() })
        }
    }
}

@Composable
private fun ConfigRow(label: String, current: Int, suffix: String, options: List<Int>, font: FontFamily, accent: Color, onPick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = accent, fontSize = 10.sp, fontFamily = BebasFont, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            Text("$current $suffix", color = Color.White, fontSize = 15.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { opt ->
                val sel = current == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (sel) accent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .clickable { onPick(opt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("$opt", color = Color.White.copy(alpha = if (sel) 1f else 0.65f), fontSize = 12.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SkillChip(name: String, selected: Boolean, font: FontFamily, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) OrangeFire.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) OrangeFire else Color.White.copy(alpha = 0.15f), RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name.uppercase(), color = Color.White.copy(alpha = if (selected) 1f else 0.7f), fontSize = 11.sp, fontFamily = font, letterSpacing = 1.sp)
    }
}

@Composable
private fun StatBadge(label: String, value: String, font: FontFamily) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = BebasFont, letterSpacing = 2.sp)
    }
}

@Composable
private fun CircleTimer(label: String, progress: Float, accent: Color) {
    val dialDp = 180.dp
    val density = LocalDensity.current
    val dialPx = with(density) { dialDp.toPx() }
    val cx = dialPx / 2f
    val ringR = cx - with(density) { 14.dp.toPx() }

    Box(modifier = Modifier.size(dialDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(cx, cx)
            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - ringR, cx - ringR),
                size = Size(ringR * 2f, ringR * 2f)
            )
            // Progress
            val sweep = progress * 360f
            listOf(18f to 0.15f, 10f to 0.35f).forEach { (w, a) ->
                drawArc(
                    color = accent.copy(alpha = a),
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    style = Stroke(w.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(cx - ringR, cx - ringR),
                    size = Size(ringR * 2f, ringR * 2f)
                )
            }
            drawArc(
                color = accent,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - ringR, cx - ringR),
                size = Size(ringR * 2f, ringR * 2f)
            )
        }
        Text(
            label,
            color = Color.White,
            fontSize = 32.sp,
            fontFamily = NokiaFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            style = TextStyle(shadow = Shadow(color = accent, blurRadius = 16f))
        )
    }
}

private fun fmtTime(ms: Long): String {
    val m = ms / 60_000L
    val s = (ms % 60_000L) / 1_000L
    return String.format("%02d:%02d", m, s)
}
