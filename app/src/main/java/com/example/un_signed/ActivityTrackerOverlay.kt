package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

/** Reusable activity tracker for Cycling / Yoga / Walking. */
@Composable
fun ActivityTrackerOverlay(
    activity: String,            // "cycling" | "yoga" | "walking"
    titleFont: FontFamily,
    contentFont: FontFamily,
    prefs: AppPreferences = AppPreferences(),
    onClose: () -> Unit
) {
    val title = activity.uppercase()
    val supportsDistance = activity == "cycling" || activity == "walking"
    val accent = when (activity) {
        "cycling" -> Color(0xFF6ACBEA)
        "yoga" -> Color(0xFFB19CFF)
        "walking" -> Color(0xFF8CD86A)
        else -> OrangeFire
    }
    val subtitle = when (activity) {
        "cycling" -> "Kilometres & minutes ridden"
        "yoga" -> "Practice sessions"
        "walking" -> "Kilometres walked · steps counted"
        else -> ""
    }

    val today = remember { LocalDate.now() }
    var allSessions by remember { mutableStateOf(FitDataRepository.loadActivitySessions()) }
    var minutesInput by remember { mutableStateOf("") }
    var distanceInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val mySessions = allSessions.filter { it.activity == activity }
        .sortedByDescending { it.timestamp }
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekSessions = mySessions.filter {
        val d = try { LocalDate.parse(it.dateIso) } catch (_: Exception) { null }
        d != null && !d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6))
    }
    val weekMinutes = weekSessions.sumOf { it.durationMinutes }
    val weekKm = weekSessions.mapNotNull { it.distanceKm }.sum()
    val distSuffix = Units.distanceSuffix(prefs.distanceUnit)

    fun addSession() {
        val mins = minutesInput.toIntOrNull() ?: return
        if (mins <= 0) return
        val km: Double? = if (supportsDistance) {
            distanceInput.toDoubleOrNull()?.let { Units.distanceToKm(it, prefs.distanceUnit) }
        } else null
        val session = ActivitySession(
            activity = activity,
            dateIso = today.toString(),
            durationMinutes = mins,
            distanceKm = km,
            notes = notes.trim()
        )
        val newList = allSessions + session
        allSessions = newList
        FitDataRepository.saveActivitySessions(newList)
        minutesInput = ""; distanceInput = ""; notes = ""
    }

    fun deleteSession(s: ActivitySession) {
        val newList = allSessions.filter { it.id != s.id }
        allSessions = newList
        FitDataRepository.saveActivitySessions(newList)
    }

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
                .heightIn(max = 660.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF14181F).copy(alpha = 0.96f), Color(0xFF06080B).copy(alpha = 0.96f))))
                .border(1.5.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent, accent.copy(alpha = 0.25f))), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = accent, blurRadius = 12f))
            )
            Text(subtitle, color = accent.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = contentFont, letterSpacing = 1.sp)

            Spacer(Modifier.height(14.dp))

            // Weekly stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("THIS WEEK", "$weekMinutes min", contentFont, accent, Modifier.weight(1f))
                if (supportsDistance) StatTile("DISTANCE", Units.displayDistance(weekKm, prefs.distanceUnit), contentFont, accent, Modifier.weight(1f))
                StatTile("SESSIONS", "${weekSessions.size}", contentFont, accent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            // Entry form
            Text("ADD SESSION", color = accent, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldBox(minutesInput, "min", contentFont, accent, KeyboardType.Number, Modifier.weight(1f)) {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) minutesInput = it
                }
                if (supportsDistance) {
                    FieldBox(distanceInput, distSuffix, contentFont, accent, KeyboardType.Decimal, Modifier.weight(1f)) {
                        if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,2})?$"))) distanceInput = it
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = notes,
                onValueChange = { notes = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = contentFont),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (notes.isEmpty()) Text("Notes (optional)", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp, fontFamily = contentFont)
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.85f))
                    .clickable { addSession() },
                contentAlignment = Alignment.Center
            ) {
                Text("+ LOG SESSION", color = Color.Black, fontSize = 13.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(14.dp))

            Text("RECENT", color = accent, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(6.dp))
            if (mySessions.isEmpty()) {
                Text("no sessions logged", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(mySessions.take(20)) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${s.durationMinutes}m", color = accent, fontSize = 14.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                            if (s.distanceKm != null) {
                                Spacer(Modifier.width(6.dp))
                                Text("· " + Units.displayDistance(s.distanceKm, prefs.distanceUnit), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontFamily = contentFont)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.dateIso, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = contentFont)
                                if (s.notes.isNotBlank()) Text(s.notes, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = contentFont, maxLines = 1)
                            }
                            Text("×", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp, modifier = Modifier.clickable { deleteSession(s) }.padding(horizontal = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("CLOSE", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, font: FontFamily, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = accent.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = BebasFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 16.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FieldBox(
    value: String,
    placeholder: String,
    font: FontFamily,
    accent: Color,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = font, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 15.sp, fontFamily = font)
                inner()
            }
        }
    )
}
