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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** Reusable text-log overlay driven by category. Handles Meds / Suppliments / Severe / Diet. */
@Composable
fun LogEntryOverlay(
    category: String,      // "meds" | "suppliments" | "severe" | "diet"
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    val title = when (category) {
        "meds" -> "MEDS LOG"
        "suppliments" -> "SUPPLIMENTS"
        "severe" -> "SEVERE"
        "diet" -> "DIET"
        else -> category.uppercase()
    }
    val accent = when (category) {
        "meds" -> Color(0xFF6ACBEA)
        "suppliments" -> Color(0xFF6DEBB4)
        "severe" -> Color(0xFFFF6666)
        "diet" -> Color(0xFFFFAB4A)
        else -> OrangeFire
    }
    val bgTop = when (category) {
        "meds" -> Color(0xFF0E1E2A).copy(alpha = 0.96f)
        "suppliments" -> Color(0xFF0A241C).copy(alpha = 0.96f)
        "severe" -> Color(0xFF2A0E0E).copy(alpha = 0.96f)
        "diet" -> Color(0xFF2A1A0A).copy(alpha = 0.96f)
        else -> Color(0xFF1A1A28).copy(alpha = 0.96f)
    }

    var allEntries by remember { mutableStateOf(FitDataRepository.loadLogEntries()) }
    var note by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf(defaultSubcategory(category)) }
    var severity by remember { mutableStateOf(3) }

    val myEntries = allEntries.filter { it.category == category }
        .sortedByDescending { it.timestamp }
    val today = LocalDate.now().toString()
    val todayEntries = myEntries.filter { it.dateIso == today }

    fun save() {
        if (note.isBlank()) return
        val entry = LogEntry(
            category = category,
            subcategory = subcategory,
            note = note.trim(),
            severity = if (category == "severe") severity else 0
        )
        val newList = allEntries + entry
        allEntries = newList
        FitDataRepository.saveLogEntries(newList)
        note = ""
    }

    fun delete(e: LogEntry) {
        val newList = allEntries.filter { it.id != e.id }
        allEntries = newList
        FitDataRepository.saveLogEntries(newList)
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
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(bgTop, Color(0xFF06060A).copy(alpha = 0.96f))))
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
                style = TextStyle(shadow = Shadow(color = accent, blurRadius = 10f))
            )
            Text(
                helperText(category),
                color = accent.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Subcategory row (chips)
            val chips = subcategoriesFor(category)
            if (chips.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { chip ->
                        val sel = subcategory == chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f))
                                .border(1.dp, if (sel) accent else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable { subcategory = chip },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chip.uppercase(), color = Color.White.copy(alpha = if (sel) 1f else 0.65f), fontSize = 11.sp, fontFamily = contentFont, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Severity slider — only for "severe"
            if (category == "severe") {
                Text("SEVERITY  $severity/10", color = accent, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..10).forEach { level ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (level <= severity)
                                        lerpColor(Color(0xFFFFC848), Color(0xFFFF3020), (level - 1) / 9f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .clickable { severity = level }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Note field
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = contentFont),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp, max = 120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (note.isEmpty()) Text(placeholderFor(category), color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp, fontFamily = contentFont)
                    inner()
                }
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.85f))
                    .border(1.dp, accent, RoundedCornerShape(12.dp))
                    .clickable { save() },
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE ENTRY", color = Color.Black, fontSize = 13.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TODAY (${todayEntries.size})", color = accent, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp)
                Text("all: ${myEntries.size}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = contentFont)
            }
            Spacer(Modifier.height(6.dp))

            if (myEntries.isEmpty()) {
                Text("no entries yet", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(myEntries) { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (e.subcategory.isNotBlank() || (category == "severe" && e.severity > 0)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (e.subcategory.isNotBlank())
                                            Text(e.subcategory.uppercase(), color = accent, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                                        if (category == "severe" && e.severity > 0)
                                            Text("severity ${e.severity}", color = Color(0xFFFF6666), fontSize = 10.sp, fontFamily = contentFont)
                                    }
                                }
                                Text(e.note, color = Color.White, fontSize = 13.sp, fontFamily = contentFont)
                                Text(formatTime(e.timestamp), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = contentFont)
                            }
                            Text("×", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp, modifier = Modifier.clickable { delete(e) }.padding(horizontal = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("CLOSE", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

private fun defaultSubcategory(cat: String): String = when (cat) {
    "diet" -> "breakfast"
    else -> ""
}

private fun subcategoriesFor(cat: String): List<String> = when (cat) {
    "meds" -> listOf("morning", "afternoon", "night")
    "suppliments" -> listOf("morning", "post-workout", "night")
    "diet" -> listOf("breakfast", "lunch", "dinner", "snack")
    "severe" -> listOf("headache", "gut", "fatigue", "other")
    else -> emptyList()
}

private fun placeholderFor(cat: String): String = when (cat) {
    "meds" -> "e.g. Vitamin D 1000 IU"
    "suppliments" -> "e.g. Whey protein 30g"
    "severe" -> "Describe symptoms…"
    "diet" -> "What did you eat?"
    else -> "Note…"
}

private fun helperText(cat: String): String = when (cat) {
    "meds" -> "Log every dose you take."
    "suppliments" -> "Track your daily suppliments."
    "severe" -> "Chronic symptoms with severity 1-10."
    "diet" -> "Log meals to see patterns."
    else -> ""
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(ts))

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tc = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tc,
        green = a.green + (b.green - a.green) * tc,
        blue = a.blue + (b.blue - a.blue) * tc,
        alpha = 1f
    )
}
