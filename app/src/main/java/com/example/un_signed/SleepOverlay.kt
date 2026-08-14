package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun SleepOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    onClose: () -> Unit
) {
    var allEntries by remember { mutableStateOf(FitDataRepository.loadSleepEntries()) }
    val today = remember { LocalDate.now() }

    // Sensible defaults: bedtime 23:00 yesterday, wake 07:00 today
    var bedH by remember { mutableStateOf(23) }
    var bedM by remember { mutableStateOf(0) }
    var wakeH by remember { mutableStateOf(7) }
    var wakeM by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf(3) }

    fun computeDurationMin(bH: Int, bM: Int, wH: Int, wM: Int): Int {
        var mins = (wH * 60 + wM) - (bH * 60 + bM)
        if (mins <= 0) mins += 24 * 60
        return mins
    }
    val durationMin = computeDurationMin(bedH, bedM, wakeH, wakeM)
    val durationHrs = durationMin / 60.0

    fun logSleep() {
        val zone = ZoneId.systemDefault()
        // bedtime is on previous day if it's late-night; otherwise same day as wake
        val bedDate = if (bedH >= 12) today.minusDays(1) else today
        val bedMs = LocalDateTime.of(bedDate, LocalTime.of(bedH, bedM)).atZone(zone).toInstant().toEpochMilli()
        val wakeMs = LocalDateTime.of(today, LocalTime.of(wakeH, wakeM)).atZone(zone).toInstant().toEpochMilli()
        val entry = SleepEntry(
            wakeDateIso = today.toString(),
            bedtimeMs = bedMs,
            wakeMs = wakeMs,
            quality = quality
        )
        val newList = allEntries.filter { it.wakeDateIso != today.toString() } + entry
        allEntries = newList
        FitDataRepository.saveSleepEntries(newList)
    }

    fun deleteEntry(entry: SleepEntry) {
        val newList = allEntries.filter { it.id != entry.id }
        allEntries = newList
        FitDataRepository.saveSleepEntries(newList)
    }

    val recent = allEntries.sortedByDescending { it.wakeDateIso }.take(7)
    val avgHrs = if (recent.isNotEmpty()) recent.sumOf { it.durationHours } / recent.size else 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1230).copy(alpha = 0.96f), Color(0xFF08051A).copy(alpha = 0.96f))))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFB19CFF).copy(alpha = 0.5f), Color.Transparent, Color(0xFF6A4EAA).copy(alpha = 0.3f))), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SLEEP",
                color = Color(0xFFE1D6FF),
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = Color(0xFFB19CFF), blurRadius = 12f))
            )
            Text(
                "Target: %.1f hr / night".format(profile.recommendedSleepHours),
                color = Color(0xFFB19CFF).copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(14.dp))

            // Bedtime picker
            TimeRow("BEDTIME", bedH, bedM, contentFont, Color(0xFF6A4EAA), Color(0xFFB19CFF)) { h, m -> bedH = h; bedM = m }

            Spacer(Modifier.height(8.dp))
            // Wake picker
            TimeRow("WAKE UP", wakeH, wakeM, contentFont, Color(0xFFDD8A55), Color(0xFFFFC48A)) { h, m -> wakeH = h; wakeM = m }

            Spacer(Modifier.height(14.dp))

            // Duration + status
            val targetMin = (profile.recommendedSleepHours * 60).toInt()
            val ratio = if (targetMin > 0) durationMin.toFloat() / targetMin else 0f
            val statusColor = when {
                ratio >= 0.95f && ratio <= 1.10f -> Color(0xFF8CD86A)
                ratio >= 0.85f -> Color(0xFFFFC48A)
                else -> Color(0xFFFF7770)
            }
            val statusText = when {
                ratio >= 0.95f && ratio <= 1.10f -> "on target"
                ratio > 1.10f -> "over target"
                ratio >= 0.85f -> "slightly short"
                else -> "under-slept"
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%d h %02d m".format(durationMin / 60, durationMin % 60),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontFamily = NokiaFont,
                    letterSpacing = 2.sp
                )
                Text(statusText.uppercase(), color = statusColor, fontSize = 10.sp, fontFamily = contentFont, letterSpacing = 3.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Quality slider (star row)
            Text("QUALITY", color = Color(0xFFB19CFF), fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                (1..5).forEach { star ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (star <= quality) Color(0xFF6A4EAA).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (star <= quality) Color(0xFFB19CFF) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .clickable { quality = star },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "★",
                            color = if (star <= quality) Color(0xFFFFC848) else Color.White.copy(alpha = 0.3f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Log button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF6A4EAA))
                    .border(1.dp, Color(0xFFB19CFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { logSleep() },
                contentAlignment = Alignment.Center
            ) {
                Text("LOG TONIGHT'S SLEEP", color = Color.White, fontSize = 13.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Recent
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LAST 7", color = Color(0xFFB19CFF), fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp)
                if (avgHrs > 0) Text("avg %.1f h".format(avgHrs), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = contentFont)
            }
            Spacer(Modifier.height(6.dp))
            if (recent.isEmpty()) {
                Text("no sleep logged", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(recent) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("%.1fh".format(entry.durationHours), color = Color(0xFFB19CFF), fontSize = 15.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.wakeDateIso, color = Color.White, fontSize = 12.sp, fontFamily = contentFont)
                                Text("quality " + "★".repeat(entry.quality), color = Color(0xFFFFC848).copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = contentFont)
                            }
                            Text("×", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp, modifier = Modifier.clickable { deleteEntry(entry) }.padding(horizontal = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("CLOSE", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    hour: Int,
    minute: Int,
    font: FontFamily,
    bg: Color,
    fg: Color,
    onChange: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = fg, fontSize = 10.sp, fontFamily = BebasFont, letterSpacing = 3.sp)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg.copy(alpha = 0.25f))
                .border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Hour rocker
            Rocker(hour, 23, font) { onChange(it, minute) }
            Text(":", color = Color.White, fontSize = 22.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
            // Minute rocker (step of 5)
            Rocker(minute, 59, font, step = 5) { onChange(hour, it) }
        }
    }
}

@Composable
private fun Rocker(value: Int, max: Int, font: FontFamily, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { onChange(((value - step) + (max + 1)) % (max + 1)) },
            contentAlignment = Alignment.Center
        ) { Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Text(
            "%02d".format(value),
            color = Color.White,
            fontSize = 22.sp,
            fontFamily = NokiaFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { onChange((value + step) % (max + 1)) },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}
