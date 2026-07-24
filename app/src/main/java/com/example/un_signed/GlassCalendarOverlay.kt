package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.*

data class CalendarTask(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

@Composable
fun GlassCalendarOverlay(onClose: () -> Unit) {
    val today = LocalDate.now()
    var displayYear by remember { mutableStateOf(today.year) }
    var displayMonth by remember { mutableStateOf(today.monthValue) }
    var selectedDate by remember { mutableStateOf(today) }
    var allTasks by remember { mutableStateOf<Map<LocalDate, List<CalendarTask>>>(emptyMap()) }
    var addingTask by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }

    val yearMonth = YearMonth.of(displayYear, displayMonth)
    val prevYearMonth = yearMonth.minusMonths(1)
    val nextYearMonth = yearMonth.plusMonths(1)
    val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1 // Mon=0 … Sun=6
    val prevMonthDays = prevYearMonth.lengthOfMonth()

    val cells = buildList<LocalDate> {
        for (i in firstDayOffset - 1 downTo 0)
            add(LocalDate.of(prevYearMonth.year, prevYearMonth.month, prevMonthDays - i))
        for (day in 1..yearMonth.lengthOfMonth())
            add(LocalDate.of(displayYear, displayMonth, day))
        var next = 1
        while (size < 42) add(LocalDate.of(nextYearMonth.year, nextYearMonth.month, next++))
    }

    val selectedTasks = allTasks[selectedDate] ?: emptyList()
    val monthName = yearMonth.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF252535).copy(alpha = 0.93f), Color(0xFF1A1A2A).copy(alpha = 0.93f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(16.dp)
        ) {

            // ── LEFT: calendar grid ──────────────────────────
            Column(modifier = Modifier.weight(1.6f)) {

                // Header: year nav + month nav
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$displayYear", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    CalNavArrow("<") { displayYear-- }
                    Spacer(Modifier.width(2.dp))
                    CalNavArrow(">") { displayYear++ }
                    Spacer(Modifier.width(14.dp))
                    Text(monthName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    CalNavArrow("<") {
                        if (displayMonth == 1) { displayYear--; displayMonth = 12 } else displayMonth--
                    }
                    Spacer(Modifier.width(2.dp))
                    CalNavArrow(">") {
                        if (displayMonth == 12) { displayYear++; displayMonth = 1 } else displayMonth++
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                Spacer(Modifier.height(8.dp))

                // Day-of-week headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 6-row calendar grid
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val isCurrentMonth = date.monthValue == displayMonth && date.year == displayYear
                            val isToday = date == today
                            val isSelected = date == selectedDate
                            val hasTasks = allTasks[date]?.isNotEmpty() == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> Color.White.copy(alpha = 0.22f)
                                            isToday    -> Color.White.copy(alpha = 0.08f)
                                            else       -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = Color.White.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(
                                        "${date.dayOfMonth}",
                                        color = when {
                                            isToday        -> Color.White
                                            isCurrentMonth -> Color.White.copy(alpha = 0.85f)
                                            else           -> Color.White.copy(alpha = 0.28f)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasTasks) {
                                        Spacer(Modifier.height(1.dp))
                                        Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF90CAF9)))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // ── RIGHT: selected day detail ────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    selectedDate.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp
                )
                Text(
                    "${selectedDate.dayOfMonth}",
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Thin,
                    lineHeight = 58.sp
                )
                Text(
                    selectedDate.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.getDefault()),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
                Spacer(Modifier.height(10.dp))

                Text("Tasks", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))

                // Task list
                selectedTasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.09f))
                            .clickable {
                                val updated = selectedTasks.map {
                                    if (it.id == task.id) it.copy(isDone = !it.isDone) else it
                                }
                                allTasks = allTasks + (selectedDate to updated)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (task.isDone) Color.White.copy(alpha = 0.75f) else Color.Transparent)
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isDone) Text("✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(task.text, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (addingTask) {
                    BasicTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newTaskText.isNotBlank()) {
                                allTasks = allTasks + (selectedDate to selectedTasks + CalendarTask(text = newTaskText.trim()))
                                newTaskText = ""
                            }
                            addingTask = false
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        decorationBox = { inner ->
                            if (newTaskText.isEmpty()) Text("Task name…", color = Color.White.copy(alpha = 0.38f), fontSize = 13.sp)
                            inner()
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            "Cancel",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { addingTask = false; newTaskText = "" }.padding(4.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Add",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (newTaskText.isNotBlank()) {
                                    allTasks = allTasks + (selectedDate to selectedTasks + CalendarTask(text = newTaskText.trim()))
                                    newTaskText = ""
                                }
                                addingTask = false
                            }.padding(4.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                            .clickable { addingTask = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Add Task", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalNavArrow(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
