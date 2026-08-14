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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun WeightLogOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    prefs: AppPreferences,
    onProfileWeightChange: (Double) -> Unit,   // update profile's weightKg + persist
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    var entries by remember { mutableStateOf(FitDataRepository.loadWeightEntries().sortedBy { it.dateIso }) }
    var input by remember { mutableStateOf("") }

    val unit = prefs.weightUnit
    val latest = entries.lastOrNull()
    val first  = entries.firstOrNull()
    val heightM = profile.heightCm / 100.0

    fun bmi(kg: Double): Double = if (heightM > 0) kg / (heightM * heightM) else 0.0

    fun addEntry() {
        val raw = input.toDoubleOrNull() ?: return
        val kg = if (unit == "lb") Units.lbToKg(raw) else raw
        if (kg !in 20.0..300.0) return
        val today = LocalDate.now().toString()
        val without = entries.filter { it.dateIso != today }
        val newList = (without + WeightEntry(dateIso = today, weightKg = kg))
            .sortedBy { it.dateIso }
        entries = newList
        FitDataRepository.saveWeightEntries(newList)
        onProfileWeightChange(kg)
        input = ""
    }

    fun deleteEntry(id: String) {
        val newList = entries.filter { it.id != id }.sortedBy { it.dateIso }
        entries = newList
        FitDataRepository.saveWeightEntries(newList)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 660.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.borderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "WEIGHT LOG",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = OrangeFire.copy(alpha = 0.4f), blurRadius = 10f))
            )
            Text(
                "Track long-term body composition",
                color = palette.subtle,
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Headline metric
            if (latest != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Units.displayWeight(latest.weightKg, unit),
                        color = palette.onSurface,
                        fontSize = 34.sp,
                        fontFamily = NokiaFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    val bmiVal = bmi(latest.weightKg)
                    if (bmiVal > 0) {
                        Text(
                            "BMI ${Units.formatBmi(bmiVal)}  ·  ${bmiCategoryOf(bmiVal)}",
                            color = palette.subtle,
                            fontSize = 12.sp,
                            fontFamily = contentFont,
                            letterSpacing = 1.sp
                        )
                    }
                    if (first != null && first.id != latest.id) {
                        val diffKg = latest.weightKg - first.weightKg
                        val sign = if (diffKg > 0) "+" else if (diffKg < 0) "−" else ""
                        val diffDisplay = Units.displayWeight(kotlin.math.abs(diffKg), unit).replace("-", "")
                        val color = when {
                            diffKg > 0.05 -> Color(0xFFFF9B44)
                            diffKg < -0.05 -> Color(0xFF8CD86A)
                            else -> palette.subtle
                        }
                        Text(
                            "$sign $diffDisplay since ${first.dateIso}",
                            color = color,
                            fontSize = 11.sp,
                            fontFamily = contentFont,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                Text("no entries yet — log your first weight below", color = palette.faint, fontSize = 13.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
            }

            Spacer(Modifier.height(12.dp))

            // Chart
            if (entries.size >= 2) {
                WeightChart(entries, unit, palette)
                Spacer(Modifier.height(10.dp))
            }

            // Input row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = input,
                    onValueChange = { if (it.matches(Regex("^\\d{0,4}(\\.\\d{0,1})?$"))) input = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(color = palette.onSurface, fontSize = 20.sp, fontFamily = contentFont, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    cursorBrush = SolidColor(OrangeFire),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.fieldBg)
                        .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (input.isEmpty()) Text(Units.weightSuffix(unit), color = palette.faint, fontSize = 16.sp, fontFamily = contentFont)
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
                        .background(OrangeFire.copy(alpha = 0.9f))
                        .border(1.dp, OrangeFire, RoundedCornerShape(10.dp))
                        .clickable { addEntry() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ LOG TODAY", color = Color.Black, fontSize = 13.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Entries list
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ENTRIES", color = OrangeFire, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp)
                Text("${entries.size}", color = palette.subtle, fontSize = 10.sp, fontFamily = contentFont)
            }
            Spacer(Modifier.height(6.dp))
            if (entries.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(entries.reversed(), key = { it.id }) { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.chipBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(Units.displayWeight(e.weightKg, unit), color = palette.onSurface, fontSize = 14.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(e.dateIso, color = palette.subtle, fontSize = 11.sp, fontFamily = contentFont)
                                if (heightM > 0) Text("BMI ${Units.formatBmi(bmi(e.weightKg))}", color = palette.faint, fontSize = 10.sp, fontFamily = contentFont)
                            }
                            Text("×", color = palette.faint, fontSize = 18.sp, modifier = Modifier.clickable { deleteEntry(e.id) }.padding(horizontal = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("CLOSE", color = palette.faint, fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>, unit: String, palette: ThemePalette) {
    val displayVals = entries.map { if (unit == "lb") Units.kgToLb(it.weightKg) else it.weightKg }
    val minV = displayVals.min()
    val maxV = displayVals.max()
    val range = (maxV - minV).coerceAtLeast(0.5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val n = displayVals.size
            if (n < 2) return@Canvas
            val stepX = w / (n - 1)
            val pts = displayVals.mapIndexed { i, v ->
                val ny = ((v - minV) / range).toFloat()
                Offset(i * stepX, h * (1f - ny))
            }
            // Guide line at midpoint
            drawLine(
                color = palette.divider,
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 1f
            )
            // Line path
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color = OrangeFire,
                    start = pts[i], end = pts[i + 1],
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
            // Points
            pts.forEach { p ->
                drawCircle(color = OrangeFire, radius = 3.5f, center = p)
                drawCircle(color = palette.surfaceBot, radius = 1.5f, center = p)
            }
        }
        // Y-axis labels
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("%.1f".format(maxV), color = palette.faint, fontSize = 9.sp, fontFamily = NokiaFont)
            Text("%.1f".format(minV), color = palette.faint, fontSize = 9.sp, fontFamily = NokiaFont)
        }
    }
}

private fun bmiCategoryOf(bmi: Double): String = when {
    bmi < 18.5 -> "Underweight"
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> "Overweight"
    else -> "Obese"
}
