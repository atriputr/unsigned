package com.example.un_signed

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

// WHO recommended weekly minimum: 150 min moderate OR 75 min vigorous
private const val WEEKLY_TARGET_MIN = 150

@Composable
fun ExerciseOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    val today = remember { LocalDate.now() }
    var allEntries by remember { mutableStateOf(FitDataRepository.loadExerciseEntries()) }
    var minutesInput by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("General") }
    var intensity by remember { mutableStateOf("Moderate") }

    // Week starts Monday
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekEntries = allEntries.filter {
        val d = try { LocalDate.parse(it.dateIso) } catch (_: Exception) { null }
        d != null && !d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6))
    }
    val weekTotal = weekEntries.sumOf { it.minutes }
    val weekPercent = (weekTotal.toFloat() / WEEKLY_TARGET_MIN).coerceIn(0f, 1.5f)

    fun addEntry(min: Int) {
        if (min <= 0) return
        val newList = allEntries + ExerciseEntry(
            minutes = min,
            activity = activity,
            intensity = intensity,
            dateIso = today.toString()
        )
        allEntries = newList
        FitDataRepository.saveExerciseEntries(newList)
    }

    fun deleteEntry(entry: ExerciseEntry) {
        val newList = allEntries.filter { it.id != entry.id }
        allEntries = newList
        FitDataRepository.saveExerciseEntries(newList)
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
                .background(Brush.verticalGradient(listOf(Color(0xFF1C2418).copy(alpha = 0.96f), Color(0xFF0A0F08).copy(alpha = 0.96f))))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFF8CD86A).copy(alpha = 0.5f), Color.Transparent, Color(0xFF4A9B2E).copy(alpha = 0.3f))), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "EXERCISE",
                color = Color(0xFFCCE9BC),
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = Color(0xFF8CD86A), blurRadius = 12f))
            )
            Text(
                "WHO recommends 150 min / week",
                color = Color(0xFF8CD86A).copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(14.dp))

            // Weekly progress ring
            Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val r = cx - 12f
                    // Track
                    drawArc(
                        color = Color(0xFF223321),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(14f, cap = StrokeCap.Round),
                        topLeft = Offset(cx - r, cx - r),
                        size = Size(r * 2f, r * 2f)
                    )
                    // Filled portion
                    val sweep = (weekPercent * 360f).coerceAtMost(360f)
                    if (sweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color(0xFF4A9B2E), Color(0xFF8CD86A), Color(0xFFCCE9BC))),
                            startAngle = -90f, sweepAngle = sweep, useCenter = false,
                            style = Stroke(14f, cap = StrokeCap.Round),
                            topLeft = Offset(cx - r, cx - r),
                            size = Size(r * 2f, r * 2f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$weekTotal", color = Color.White, fontSize = 34.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                    Text("of $WEEKLY_TARGET_MIN min", color = Color(0xFF8CD86A).copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = contentFont, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quick add row
            Text("QUICK ADD", color = Color(0xFF8CD86A), fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 15, 30, 45, 60).forEach { m ->
                    QuickAddPill("+${m}m", Modifier.weight(1f)) { addEntry(m) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Manual entry row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = minutesInput,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) minutesInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontFamily = contentFont, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color(0xFF8CD86A).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (minutesInput.isEmpty()) Text("min", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp, fontFamily = contentFont)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4A9B2E))
                        .clickable {
                            minutesInput.toIntOrNull()?.let { if (it > 0) { addEntry(it); minutesInput = "" } }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ ADD ENTRY", color = Color.White, fontSize = 13.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(6.dp))
            // Intensity pills
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Light", "Moderate", "Vigorous").forEach { i ->
                    val sel = intensity == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) Color(0xFF4A9B2E).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (sel) Color(0xFF8CD86A) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable { intensity = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(i.uppercase(), color = Color.White.copy(alpha = if (sel) 1f else 0.6f), fontSize = 10.sp, fontFamily = contentFont, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Recent entries (this week)
            Text("THIS WEEK", color = Color(0xFF8CD86A), fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(6.dp))
            if (weekEntries.isEmpty()) {
                Text("no sessions yet", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(weekEntries.reversed()) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${entry.minutes}m", color = Color(0xFF8CD86A), fontSize = 15.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.activity, color = Color.White, fontSize = 12.sp, fontFamily = contentFont)
                                Text("${entry.dateIso} · ${entry.intensity}", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = contentFont)
                            }
                            Text("×", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp, modifier = Modifier.clickable { deleteEntry(entry) }.padding(horizontal = 4.dp))
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
private fun QuickAddPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF223321))
            .border(1.dp, Color(0xFF8CD86A).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFFCCE9BC), fontSize = 12.sp, fontFamily = BebasFont, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}
