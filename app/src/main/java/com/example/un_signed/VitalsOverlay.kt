package com.example.un_signed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class VitalsTab { BP, GLUCOSE }

private val GLUCOSE_CONTEXTS = listOf("Fasting", "PostMeal", "Random", "Bedtime")

private fun statusColor(status: String, palette: ThemePalette): Color = when (status) {
    "ok" -> palette.success
    "watch" -> Color(0xFFFFC848)
    "bad" -> Color(0xFFFF6666)
    "critical" -> palette.danger
    "low" -> Color(0xFF6ACBEA)
    else -> palette.faint
}

@Composable
fun VitalsOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(VitalsTab.BP) }
    var bpEntries by remember { mutableStateOf(FitDataRepository.loadBpReadings()) }
    var glucoseEntries by remember { mutableStateOf(FitDataRepository.loadGlucoseReadings()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.borderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "VITALS",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = OrangeFire.copy(alpha = 0.4f), blurRadius = 10f))
            )
            Text(
                "BP & glucose — long-term monitoring",
                color = palette.subtle,
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // ── Tab switcher ────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TabPill("BLOOD PRESSURE", tab == VitalsTab.BP, titleFont, palette, Modifier.weight(1f)) {
                    Haptics.tick(ctx); tab = VitalsTab.BP
                }
                TabPill("GLUCOSE", tab == VitalsTab.GLUCOSE, titleFont, palette, Modifier.weight(1f)) {
                    Haptics.tick(ctx); tab = VitalsTab.GLUCOSE
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    VitalsTab.BP -> BpSection(
                        entries = bpEntries,
                        titleFont = titleFont,
                        contentFont = contentFont,
                        palette = palette,
                        onAdd = { entry ->
                            FitDataRepository.addBpReading(entry)
                            bpEntries = FitDataRepository.loadBpReadings()
                            Haptics.success(ctx)
                        },
                        onDelete = { id ->
                            FitDataRepository.deleteBpReading(id)
                            bpEntries = FitDataRepository.loadBpReadings()
                            Haptics.tick(ctx)
                        }
                    )
                    VitalsTab.GLUCOSE -> GlucoseSection(
                        entries = glucoseEntries,
                        titleFont = titleFont,
                        contentFont = contentFont,
                        palette = palette,
                        onAdd = { entry ->
                            FitDataRepository.addGlucoseReading(entry)
                            glucoseEntries = FitDataRepository.loadGlucoseReadings()
                            Haptics.success(ctx)
                        },
                        onDelete = { id ->
                            FitDataRepository.deleteGlucoseReading(id)
                            glucoseEntries = FitDataRepository.loadGlucoseReadings()
                            Haptics.tick(ctx)
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "CLOSE",
                color = palette.faint,
                fontSize = 14.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

// ── BP section ─────────────────────────────────────────────
@Composable
private fun BpSection(
    entries: List<BpReading>,
    titleFont: FontFamily,
    contentFont: FontFamily,
    palette: ThemePalette,
    onAdd: (BpReading) -> Unit,
    onDelete: (String) -> Unit
) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val latest = entries.lastOrNull()

    // Headline card
    if (latest != null) {
        HeadlineCard(
            big = "${latest.systolic}/${latest.diastolic}",
            unit = "mmHg",
            category = latest.category,
            status = latest.status,
            secondary = if (latest.pulse > 0) "${latest.pulse} bpm · ${formatWhen(latest.timestamp)}" else formatWhen(latest.timestamp),
            titleFont = titleFont,
            contentFont = contentFont,
            palette = palette
        )
    } else {
        EmptyHint("no BP readings yet — enter your first below", contentFont, palette)
    }

    // Chart
    if (entries.size >= 2) {
        BpChart(entries.takeLast(30), palette)
    }

    // Entry form
    SectionHeader("NEW READING", titleFont, palette)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumberField(systolic, "SYS", contentFont, palette, Modifier.weight(1f), maxLen = 3) { systolic = it }
        Box(modifier = Modifier.width(10.dp).align(Alignment.CenterVertically)) {
            Text("/", color = palette.subtle, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
        NumberField(diastolic, "DIA", contentFont, palette, Modifier.weight(1f), maxLen = 3) { diastolic = it }
        NumberField(pulse, "BPM", contentFont, palette, Modifier.weight(1f), maxLen = 3) { pulse = it }
    }
    Spacer(Modifier.height(4.dp))
    NoteField(note, contentFont, palette) { note = it }

    val sys = systolic.toIntOrNull() ?: 0
    val dia = diastolic.toIntOrNull() ?: 0
    val valid = sys in 60..260 && dia in 30..200
    LogButton(enabled = valid, label = "+ LOG READING", titleFont = titleFont, palette = palette) {
        onAdd(BpReading(
            systolic = sys,
            diastolic = dia,
            pulse = pulse.toIntOrNull() ?: 0,
            notes = note
        ))
        systolic = ""; diastolic = ""; pulse = ""; note = ""
    }

    // Preview classification
    if (sys > 0 && dia > 0) {
        val preview = BpReading(systolic = sys, diastolic = dia)
        ClassifyPreview(preview.category, preview.status, contentFont, palette)
    }

    // History list
    if (entries.isNotEmpty()) {
        SectionHeader("HISTORY · ${entries.size}", titleFont, palette)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(entries.reversed(), key = { it.id }) { e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.chipBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${e.systolic}/${e.diastolic}",
                        color = palette.onSurface,
                        fontSize = 14.sp,
                        fontFamily = NokiaFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatWhen(e.timestamp), color = palette.subtle, fontSize = 11.sp, fontFamily = contentFont)
                        Text(
                            if (e.pulse > 0) "${e.pulse} bpm · ${e.category}" else e.category,
                            color = statusColor(e.status, palette),
                            fontSize = 10.sp,
                            fontFamily = contentFont
                        )
                    }
                    Text("×", color = palette.faint, fontSize = 18.sp, modifier = Modifier.clickable { onDelete(e.id) }.padding(horizontal = 4.dp))
                }
            }
        }
    }

    // Reference legend
    RangeLegend(
        title = "BP RANGES (AHA)",
        rows = listOf(
            "Normal" to "<120 / <80",
            "Elevated" to "120–129 / <80",
            "Stage 1" to "130–139 / 80–89",
            "Stage 2" to "≥140 / ≥90",
            "Crisis" to "≥180 / ≥120"
        ),
        titleFont = titleFont,
        contentFont = contentFont,
        palette = palette
    )
}

// ── Glucose section ────────────────────────────────────────
@Composable
private fun GlucoseSection(
    entries: List<GlucoseReading>,
    titleFont: FontFamily,
    contentFont: FontFamily,
    palette: ThemePalette,
    onAdd: (GlucoseReading) -> Unit,
    onDelete: (String) -> Unit
) {
    var mg by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("Random") }
    var note by remember { mutableStateOf("") }

    val latest = entries.lastOrNull()

    if (latest != null) {
        HeadlineCard(
            big = "${latest.mgPerDl}",
            unit = "mg/dL",
            category = latest.category,
            status = latest.status,
            secondary = "${latest.context} · ${formatWhen(latest.timestamp)}",
            titleFont = titleFont,
            contentFont = contentFont,
            palette = palette
        )
    } else {
        EmptyHint("no glucose readings yet — enter your first below", contentFont, palette)
    }

    if (entries.size >= 2) {
        GlucoseChart(entries.takeLast(30), palette)
    }

    SectionHeader("NEW READING", titleFont, palette)

    // Context selector
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GLUCOSE_CONTEXTS.forEach { c ->
            TabPill(c.uppercase(), context == c, contentFont, palette, Modifier.weight(1f), sizeSp = 10) {
                context = c
            }
        }
    }
    Spacer(Modifier.height(4.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NumberField(mg, "mg/dL", contentFont, palette, Modifier.weight(1f), maxLen = 3) { mg = it }
    }
    Spacer(Modifier.height(4.dp))
    NoteField(note, contentFont, palette) { note = it }

    val mgVal = mg.toIntOrNull() ?: 0
    val valid = mgVal in 20..600
    LogButton(enabled = valid, label = "+ LOG READING", titleFont = titleFont, palette = palette) {
        onAdd(GlucoseReading(mgPerDl = mgVal, context = context, notes = note))
        mg = ""; note = ""
    }

    if (mgVal > 0) {
        val preview = GlucoseReading(mgPerDl = mgVal, context = context)
        ClassifyPreview(preview.category, preview.status, contentFont, palette)
    }

    if (entries.isNotEmpty()) {
        SectionHeader("HISTORY · ${entries.size}", titleFont, palette)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(entries.reversed(), key = { it.id }) { e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.chipBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${e.mgPerDl}",
                        color = palette.onSurface,
                        fontSize = 14.sp,
                        fontFamily = NokiaFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${e.context} · ${formatWhen(e.timestamp)}",
                            color = palette.subtle,
                            fontSize = 11.sp,
                            fontFamily = contentFont
                        )
                        Text(
                            e.category,
                            color = statusColor(e.status, palette),
                            fontSize = 10.sp,
                            fontFamily = contentFont
                        )
                    }
                    Text("×", color = palette.faint, fontSize = 18.sp, modifier = Modifier.clickable { onDelete(e.id) }.padding(horizontal = 4.dp))
                }
            }
        }
    }

    RangeLegend(
        title = "GLUCOSE RANGES (ADA · mg/dL)",
        rows = listOf(
            "Fasting Normal" to "<100",
            "Fasting Pre-diabetic" to "100–125",
            "Fasting Diabetic" to "≥126",
            "Post-meal Normal" to "<140",
            "Post-meal Diabetic" to "≥200",
            "Low (Hypo)" to "<70"
        ),
        titleFont = titleFont,
        contentFont = contentFont,
        palette = palette
    )
}

// ── Shared bits ────────────────────────────────────────────

@Composable
private fun HeadlineCard(
    big: String,
    unit: String,
    category: String,
    status: String,
    secondary: String,
    titleFont: FontFamily,
    contentFont: FontFamily,
    palette: ThemePalette
) {
    val color = statusColor(status, palette)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                big,
                color = palette.onSurface,
                fontSize = 40.sp,
                fontFamily = NokiaFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                " $unit",
                color = palette.subtle,
                fontSize = 12.sp,
                fontFamily = contentFont,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            category.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            secondary,
            color = palette.faint,
            fontSize = 10.sp,
            fontFamily = contentFont,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun EmptyHint(text: String, contentFont: FontFamily, palette: ThemePalette) {
    Text(
        text,
        color = palette.faint,
        fontSize = 13.sp,
        fontFamily = contentFont,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

@Composable
private fun ClassifyPreview(category: String, status: String, contentFont: FontFamily, palette: ThemePalette) {
    val color = statusColor(status, palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("STATUS", color = palette.subtle, fontSize = 9.sp, fontFamily = contentFont, letterSpacing = 2.sp)
        Spacer(Modifier.width(8.dp))
        Text(category, color = color, fontSize = 12.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(text: String, titleFont: FontFamily, palette: ThemePalette) {
    Text(
        text,
        color = palette.accentPrimary,
        fontSize = 10.sp,
        fontFamily = titleFont,
        letterSpacing = 3.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    font: FontFamily,
    palette: ThemePalette,
    modifier: Modifier = Modifier,
    sizeSp: Int = 11,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OrangeFire.copy(alpha = 0.35f) else palette.chipBg)
            .border(1.dp, if (selected) OrangeFire else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) palette.onSurface else palette.subtle,
            fontSize = sizeSp.sp,
            fontFamily = font,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    hint: String,
    font: FontFamily,
    palette: ThemePalette,
    modifier: Modifier = Modifier,
    maxLen: Int = 3,
    onChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = { v -> if (v.length <= maxLen && v.all { it.isDigit() }) onChange(v) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = palette.onSurface, fontSize = 20.sp, fontFamily = NokiaFont, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(OrangeFire),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.fieldBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) Text(hint, color = palette.faint, fontSize = 12.sp, fontFamily = font, letterSpacing = 1.sp)
                inner()
            }
        }
    )
}

@Composable
private fun NoteField(value: String, font: FontFamily, palette: ThemePalette, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = palette.onSurface, fontSize = 13.sp, fontFamily = font),
        cursorBrush = SolidColor(OrangeFire),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.fieldBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) Text("note (optional)", color = palette.faint, fontSize = 12.sp, fontFamily = font, fontStyle = FontStyle.Italic)
                inner()
            }
        }
    )
}

@Composable
private fun LogButton(enabled: Boolean, label: String, titleFont: FontFamily, palette: ThemePalette, onClick: () -> Unit) {
    val bg = if (enabled) OrangeFire.copy(alpha = 0.9f) else palette.chipBg
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (enabled) OrangeFire else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) Color.Black else palette.faint,
            fontSize = 13.sp,
            fontFamily = titleFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun RangeLegend(
    title: String,
    rows: List<Pair<String, String>>,
    titleFont: FontFamily,
    contentFont: FontFamily,
    palette: ThemePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.divider, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, color = palette.accentSecondary, fontSize = 9.sp, fontFamily = titleFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        rows.forEach { (k, v) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, color = palette.subtle, fontSize = 11.sp, fontFamily = contentFont)
                Text(v, color = palette.onSurface, fontSize = 11.sp, fontFamily = NokiaFont)
            }
        }
    }
}

// ── Charts ─────────────────────────────────────────────────

@Composable
private fun BpChart(entries: List<BpReading>, palette: ThemePalette) {
    val sys = entries.map { it.systolic.toFloat() }
    val dia = entries.map { it.diastolic.toFloat() }
    val minV = (dia.min() - 5).coerceAtLeast(40f)
    val maxV = (sys.max() + 5).coerceAtMost(260f)
    ChartBox(palette, min = minV, max = maxV) { w, h ->
        drawTwoSeries(sys, dia, min = minV, max = maxV, w = w, h = h,
            colorA = Color(0xFFE85D5D), colorB = Color(0xFF6ACBEA))
    }
}

@Composable
private fun GlucoseChart(entries: List<GlucoseReading>, palette: ThemePalette) {
    val vals = entries.map { it.mgPerDl.toFloat() }
    val minV = (vals.min() - 10).coerceAtLeast(20f)
    val maxV = (vals.max() + 10).coerceAtMost(600f)
    ChartBox(palette, min = minV, max = maxV) { w, h ->
        drawOneSeries(vals, min = minV, max = maxV, w = w, h = h, color = OrangeFire)
    }
}

@Composable
private fun ChartBox(
    palette: ThemePalette,
    min: Float,
    max: Float,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { draw(size.width, size.height) }
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("%.0f".format(max), color = palette.faint, fontSize = 9.sp, fontFamily = NokiaFont)
            Text("%.0f".format(min), color = palette.faint, fontSize = 9.sp, fontFamily = NokiaFont)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOneSeries(
    values: List<Float>, min: Float, max: Float, w: Float, h: Float, color: Color
) {
    if (values.size < 2) return
    val range = (max - min).coerceAtLeast(1f)
    val stepX = w / (values.size - 1)
    val pts = values.mapIndexed { i, v ->
        val ny = ((v - min) / range).coerceIn(0f, 1f)
        Offset(i * stepX, h * (1f - ny))
    }
    for (i in 0 until pts.size - 1) {
        drawLine(color = color, start = pts[i], end = pts[i + 1], strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
    pts.forEach { p -> drawCircle(color = color, radius = 3.5f, center = p) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTwoSeries(
    a: List<Float>, b: List<Float>, min: Float, max: Float, w: Float, h: Float,
    colorA: Color, colorB: Color
) {
    drawOneSeries(a, min, max, w, h, colorA)
    drawOneSeries(b, min, max, w, h, colorB)
}

// ── util ───────────────────────────────────────────────────
private val WHEN_FORMATTER = DateTimeFormatter.ofPattern("MMM d · HH:mm")

private fun formatWhen(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDateTime().format(WHEN_FORMATTER)
