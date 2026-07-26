package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.*

@Composable
fun RepeatOptionsOverlay(
    titleFont: FontFamily,
    onSave: (String, Map<LocalDate, List<CalendarTask>>) -> Unit,
    onClose: () -> Unit
) {
    var currentView by remember { mutableStateOf("MAIN") }
    var selectedRepeatInfo by remember { mutableStateOf("") }
    var dailyTillTasksMap by remember { mutableStateOf<Map<LocalDate, List<CalendarTask>>>(emptyMap()) }

    when (currentView) {
        "CUSTOM" -> {
            GlassCalendarOverlay(
                allTasks = emptyMap(),
                onUpdateTasks = { _, _ -> },
                fontFamily = titleFont,
                onClose = {
                    selectedRepeatInfo = "Custom dates selected"
                    currentView = "MAIN"
                }
            )
            return
        }
        "DAILY_TILL" -> {
            DailyTillFlow(
                titleFont = titleFont,
                onSave = { info, tasksMap ->
                    selectedRepeatInfo = info
                    dailyTillTasksMap = tasksMap
                    currentView = "MAIN"
                },
                onClose = { currentView = "MAIN" }
            )
            return
        }
    }

    // Panel views: MAIN, WEEKLY, MONTHLY
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0D0D1A))))
                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentView) {
                "MAIN" -> {
                    Text(
                        text = "REPEAT OPTIONS",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = titleFont,
                        fontStyle = FontStyle.Italic,
                        style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    RepeatMenuButton("CUSTOM", titleFont) { currentView = "CUSTOM" }
                    Spacer(modifier = Modifier.height(12.dp))
                    RepeatMenuButton("WEEKLY", titleFont) { currentView = "WEEKLY" }
                    Spacer(modifier = Modifier.height(12.dp))
                    RepeatMenuButton("MONTHLY", titleFont) { currentView = "MONTHLY" }
                    Spacer(modifier = Modifier.height(12.dp))
                    RepeatMenuButton("DAILY TILL", titleFont) { currentView = "DAILY_TILL" }

                    if (selectedRepeatInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = selectedRepeatInfo,
                            color = Color(0xFF09e8ad),
                            fontSize = 13.sp,
                            fontFamily = titleFont
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF09e8ad).copy(alpha = 0.8f))
                            .clickable { onSave(selectedRepeatInfo, dailyTillTasksMap) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SAVE", color = Color.Black, fontSize = 18.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
                    }
                }

                "WEEKLY" -> {
                    Text("WEEKLY REPEAT", color = Color.White, fontSize = 22.sp, fontFamily = titleFont)
                    Spacer(Modifier.height(20.dp))
                    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    val selectedDays = remember { mutableStateListOf<String>() }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        days.chunked(4).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { day ->
                                    val isSelected = day in selectedDays
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFEBC174) else Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, if (isSelected) Color(0xFFEBC174) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .clickable { if (isSelected) selectedDays.remove(day) else selectedDays.add(day) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(day, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontFamily = titleFont)
                                    }
                                }
                                if (row.size < 4) Spacer(Modifier.weight((4 - row.size).toFloat()))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    RepeatMenuButton("SAVE WEEKLY", titleFont) {
                        selectedRepeatInfo = "Weekly: ${selectedDays.joinToString(", ")}"
                        currentView = "MAIN"
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("BACK", color = Color.White.copy(0.4f), fontSize = 14.sp, fontFamily = titleFont,
                        modifier = Modifier.clickable { currentView = "MAIN" })
                }

                "MONTHLY" -> {
                    Text("MONTHLY REPEAT", color = Color.White, fontSize = 22.sp, fontFamily = titleFont)
                    Spacer(Modifier.height(20.dp))
                    val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                    val selectedMonths = remember { mutableStateListOf<String>() }

                    LazyColumn(modifier = Modifier.height(260.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(months.chunked(3)) { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { month ->
                                    val isSelected = month in selectedMonths
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(45.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFEBC174) else Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, if (isSelected) Color(0xFFEBC174) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .clickable { if (isSelected) selectedMonths.remove(month) else selectedMonths.add(month) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(month, color = if (isSelected) Color.Black else Color.White, fontSize = 14.sp, fontFamily = titleFont)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    RepeatMenuButton("SAVE MONTHLY", titleFont) {
                        selectedRepeatInfo = "Monthly: ${selectedMonths.joinToString(", ")}"
                        currentView = "MAIN"
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("BACK", color = Color.White.copy(0.4f), fontSize = 14.sp, fontFamily = titleFont,
                        modifier = Modifier.clickable { currentView = "MAIN" })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAILY TILL flow  (3 steps)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DailyTillFlow(
    titleFont: FontFamily,
    onSave: (String, Map<LocalDate, List<CalendarTask>>) -> Unit,
    onClose: () -> Unit
) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    // navigation
    var step by remember { mutableIntStateOf(1) }
    var displayYear by remember { mutableIntStateOf(today.year) }
    var displayMonth by remember { mutableIntStateOf(today.monthValue) }

    // step-1 data
    var startDate by remember { mutableStateOf(today) }
    var taskName by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    // step-2 data
    var endDate by remember { mutableStateOf(today) }

    // step-3 data
    var highlightedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var showOffDaysPicker by remember { mutableStateOf(false) }

    // calendar cells
    val yearMonth = YearMonth.of(displayYear, displayMonth)
    val prevYM = yearMonth.minusMonths(1)
    val nextYM = yearMonth.plusMonths(1)
    val offset = yearMonth.atDay(1).dayOfWeek.value - 1
    val prevLen = prevYM.lengthOfMonth()
    val cells = buildList<LocalDate> {
        for (i in offset - 1 downTo 0) add(LocalDate.of(prevYM.year, prevYM.month, prevLen - i))
        for (d in 1..yearMonth.lengthOfMonth()) add(LocalDate.of(displayYear, displayMonth, d))
        var n = 1; while (size < 42) add(LocalDate.of(nextYM.year, nextYM.month, n++))
    }

    // Off-days picker shown full-screen on top
    if (showOffDaysPicker) {
        OffDaysPicker(
            titleFont = titleFont,
            onSave = { offDays ->
                highlightedDates = highlightedDates.filter { it.dayOfWeek !in offDays }.toSet()
                showOffDaysPicker = false
            },
            onClose = { showOffDaysPicker = false }
        )
        return
    }

    // Time picker shown full-screen on top
    if (showTimePicker) {
        TimeDiscPicker(
            titleFont = titleFont,
            contentFont = titleFont,
            onConfirm = { h, m, isAm, _, _ ->
                val dh = if (h == 0) 12 else h
                taskTime = String.format(Locale.getDefault(), "%02d:%02d %s", dh, m, if (isAm) "AM" else "PM")
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0D0D1A))))
                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Title ────────────────────────────────────────────────
            Text(
                text = when (step) {
                    1 -> "SELECT START DATE"
                    2 -> "SELECT END DATE"
                    else -> "DAILY TILL SUMMARY"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // ── Month / Year navigation ───────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$displayYear", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = titleFont)
                Spacer(Modifier.width(4.dp))
                DtNavArrow("<", titleFont) { displayYear-- }
                DtNavArrow(">", titleFont) { displayYear++ }
                Spacer(Modifier.width(10.dp))
                Text(
                    yearMonth.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()),
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = titleFont
                )
                Spacer(Modifier.width(4.dp))
                DtNavArrow("<", titleFont) {
                    if (displayMonth == 1) { displayYear--; displayMonth = 12 } else displayMonth--
                }
                DtNavArrow(">", titleFont) {
                    if (displayMonth == 12) { displayYear++; displayMonth = 1 } else displayMonth++
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(0.18f))
            Spacer(Modifier.height(6.dp))

            // ── Day-of-week headers ──────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { lbl ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(lbl, color = Color.White.copy(0.45f), fontSize = 13.sp, fontFamily = titleFont)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            // ── Calendar grid ─────────────────────────────────────────
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val isCurrentMonth = date.monthValue == displayMonth && date.year == displayYear
                        val isPast = date.isBefore(today)
                        val isStart = date == startDate
                        val isEnd = step >= 2 && date == endDate && endDate != startDate
                        val inRange = step == 3 && date in highlightedDates

                        val bgColor = when {
                            inRange   -> Color(0xFF09e8ad).copy(0.28f)
                            isEnd     -> Color(0xFF09e8ad).copy(0.22f)
                            isStart && step >= 2 -> Color(0xFFEBC174).copy(0.28f)
                            step == 1 && date == startDate -> Color.White.copy(0.18f)
                            else      -> Color.Transparent
                        }
                        val borderColor = when {
                            inRange   -> Color(0xFF09e8ad).copy(0.7f)
                            isEnd     -> Color(0xFF09e8ad).copy(0.8f)
                            isStart && step >= 2 -> Color(0xFFEBC174).copy(0.8f)
                            step == 1 && date == startDate -> Color.White.copy(0.4f)
                            else      -> Color.Transparent
                        }
                        val clickable = !isPast && isCurrentMonth && step < 3 &&
                            (step == 1 || (step == 2 && date.isAfter(startDate)))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                .clickable(enabled = clickable) {
                                    when (step) {
                                        1 -> startDate = date
                                        2 -> endDate = date
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${date.dayOfMonth}",
                                color = when {
                                    isPast || !isCurrentMonth -> Color.White.copy(0.18f)
                                    inRange  -> Color(0xFF09e8ad)
                                    isEnd    -> Color(0xFF09e8ad)
                                    isStart && step >= 2 -> Color(0xFFEBC174)
                                    else     -> Color.White.copy(0.85f)
                                },
                                fontSize = 13.sp,
                                fontFamily = titleFont
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(0.15f))
            Spacer(Modifier.height(12.dp))

            // ── Step-specific content ────────────────────────────────
            when (step) {
                // ── STEP 1: pick date, name, time ───────────────────
                1 -> {
                    Text("NAME", color = Color.White.copy(0.5f), fontSize = 12.sp, fontFamily = titleFont,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = titleFont),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.07f))
                            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        decorationBox = { inner ->
                            if (taskName.isEmpty()) Text("Enter name...", color = Color.White.copy(0.25f), fontSize = 16.sp, fontFamily = titleFont)
                            inner()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.07f))
                            .border(1.dp, Color(0xFFEBC174).copy(0.4f), RoundedCornerShape(8.dp))
                            .clickable { showTimePicker = true }
                            .padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            if (taskTime.isBlank()) "SET TIME  (tap)" else taskTime,
                            color = if (taskTime.isBlank()) Color.White.copy(0.3f) else Color(0xFFEBC174),
                            fontSize = 16.sp, fontFamily = titleFont
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    val step1Ready = taskName.isNotBlank() && taskTime.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (step1Ready) Color(0xFF09e8ad).copy(0.85f) else Color.White.copy(0.08f))
                            .clickable(enabled = step1Ready) {
                                step = 2
                                endDate = startDate
                                displayYear = startDate.year
                                displayMonth = startDate.monthValue
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SAVE & NEXT",
                            color = if (step1Ready) Color.Black else Color.White.copy(0.3f),
                            fontSize = 15.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("BACK", color = Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = titleFont,
                        modifier = Modifier.clickable { onClose() })
                }

                // ── STEP 2: pick end date, locked name/time ──────────
                2 -> {
                    // Locked name + time display
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.04f))
                                .border(1.dp, Color.White.copy(0.10f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(taskName, color = Color.White.copy(0.45f), fontSize = 14.sp, fontFamily = titleFont)
                        }
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.04f))
                                .border(1.dp, Color.White.copy(0.10f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(taskTime, color = Color.White.copy(0.45f), fontSize = 14.sp, fontFamily = titleFont)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Start: ${startDate.format(fmt)}    End: ${if (endDate.isAfter(startDate)) endDate.format(fmt) else "—"}",
                        color = Color(0xFFEBC174),
                        fontSize = 13.sp,
                        fontFamily = titleFont
                    )

                    Spacer(Modifier.height(12.dp))

                    val step2Ready = endDate.isAfter(startDate)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (step2Ready) Color(0xFF09e8ad).copy(0.85f) else Color.White.copy(0.08f))
                            .clickable(enabled = step2Ready) {
                                // Fill every date in the range
                                val range = mutableSetOf<LocalDate>()
                                var d = startDate
                                while (!d.isAfter(endDate)) { range.add(d); d = d.plusDays(1) }
                                highlightedDates = range
                                step = 3
                                // Jump calendar to startDate month for review
                                displayYear = startDate.year
                                displayMonth = startDate.monthValue
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SAVE",
                            color = if (step2Ready) Color.Black else Color.White.copy(0.3f),
                            fontSize = 15.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("BACK", color = Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = titleFont,
                        modifier = Modifier.clickable { step = 1 })
                }

                // ── STEP 3: review + off days + final save ───────────
                3 -> {
                    Text(
                        "${startDate.format(fmt)}  →  ${endDate.format(fmt)}  •  ${highlightedDates.size} days",
                        color = Color(0xFF09e8ad),
                        fontSize = 13.sp,
                        fontFamily = titleFont
                    )

                    Spacer(Modifier.height(12.dp))

                    // OFF DAYS button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE41417).copy(0.12f))
                            .border(1.dp, Color(0xFFE41417).copy(0.55f), RoundedCornerShape(12.dp))
                            .clickable { showOffDaysPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OFF DAYS", color = Color(0xFFE41417), fontSize = 15.sp,
                            fontFamily = titleFont, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Final SAVE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF09e8ad).copy(0.85f))
                            .clickable {
                                val info = "Daily: ${startDate.format(fmt)} → ${endDate.format(fmt)} · $taskName @ $taskTime · ${highlightedDates.size} days"
                                val tasksMap = highlightedDates.associateWith { date ->
                                    listOf(CalendarTask(text = "$taskName @ $taskTime"))
                                }
                                onSave(info, tasksMap)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SAVE", color = Color.Black, fontSize = 15.sp,
                            fontFamily = titleFont, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("BACK", color = Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = titleFont,
                        modifier = Modifier.clickable { step = 2 })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OFF DAYS picker — weekday multi-select
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OffDaysPicker(
    titleFont: FontFamily,
    onSave: (Set<DayOfWeek>) -> Unit,
    onClose: () -> Unit
) {
    val days = listOf(
        "MON" to DayOfWeek.MONDAY,
        "TUE" to DayOfWeek.TUESDAY,
        "WED" to DayOfWeek.WEDNESDAY,
        "THU" to DayOfWeek.THURSDAY,
        "FRI" to DayOfWeek.FRIDAY,
        "SAT" to DayOfWeek.SATURDAY,
        "SUN" to DayOfWeek.SUNDAY
    )
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(310.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0D0D1A))))
                .border(1.5.dp, Color(0xFFE41417).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "OFF DAYS",
                color = Color(0xFFE41417),
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = Color(0xFFE41417).copy(0.4f), blurRadius = 8f)),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Chosen days will be removed\nfrom the highlighted range",
                color = Color.White.copy(0.4f),
                fontSize = 12.sp,
                fontFamily = titleFont,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                days.chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, dow) ->
                            val sel = dow in selectedDays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Color(0xFFE41417).copy(0.28f) else Color.White.copy(0.05f))
                                    .border(1.dp, if (sel) Color(0xFFE41417) else Color.White.copy(0.14f), RoundedCornerShape(10.dp))
                                    .clickable { if (sel) selectedDays.remove(dow) else selectedDays.add(dow) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (sel) Color(0xFFE41417) else Color.White.copy(0.65f),
                                    fontSize = 12.sp,
                                    fontFamily = titleFont,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(0.12f))
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE41417).copy(0.8f))
                    .clickable { onSave(selectedDays.toSet()) },
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE", color = Color.White, fontSize = 16.sp,
                    fontFamily = titleFont, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            Text("CANCEL", color = Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = titleFont,
                modifier = Modifier.clickable { onClose() })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RepeatMenuButton(text: String, font: FontFamily, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = font,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
private fun DtNavArrow(symbol: String, font: FontFamily, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(0.10f))
            .border(1.dp, Color.White.copy(0.20f), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = font)
    }
}
