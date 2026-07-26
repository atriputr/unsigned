package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.*

val TASK_COLORS = listOf(
    Color(0xFF90CAF9),
    Color(0xFF09e8ad),
    Color(0xFFEBC174),
    Color(0xFFF4511E),
    Color(0xFF43A047),
    Color(0xFFB39DDB),
    Color(0xFF80DEEA),
    Color(0xFFE41417),
)

data class CalendarTask(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false,
    val colorIndex: Int = 0
)

@Composable
fun GlassCalendarOverlay(
    allTasks: Map<LocalDate, List<CalendarTask>>,
    onUpdateTasks: (LocalDate, List<CalendarTask>) -> Unit,
    fontFamily: FontFamily,
    initialDate: LocalDate = LocalDate.now(),
    onClose: () -> Unit,
    // When provided, SAVE button delivers the full task map back to the caller
    onSave: ((Map<LocalDate, List<CalendarTask>>) -> Unit)? = null
) {
    val today = LocalDate.now()
    var displayYear    by remember { mutableStateOf(initialDate.year) }
    var displayMonth   by remember { mutableStateOf(initialDate.monthValue) }
    var selectedDate   by remember { mutableStateOf(initialDate) }
    var addingTask     by remember { mutableStateOf(false) }
    var newTaskText    by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var taskToDelete   by remember { mutableStateOf<CalendarTask?>(null) }
    var lastSavedTaskName by remember { mutableStateOf("") }

    val localTasks = remember {
        mutableStateMapOf<LocalDate, List<CalendarTask>>().also { m ->
            allTasks.forEach { m[it.key] = it.value }
        }
    }

    fun commitTasks(date: LocalDate, tasks: List<CalendarTask>) {
        if (tasks.isEmpty()) localTasks.remove(date) else localTasks[date] = tasks
        onUpdateTasks(date, tasks)
    }

    fun extractName(raw: String): String =
        if (" @ " in raw) raw.substringBefore(" @ ").trim() else raw.trim()

    val yearMonth      = YearMonth.of(displayYear, displayMonth)
    val prevYearMonth  = yearMonth.minusMonths(1)
    val nextYearMonth  = yearMonth.plusMonths(1)
    val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1
    val prevMonthDays  = prevYearMonth.lengthOfMonth()

    val cells = buildList<LocalDate> {
        for (i in firstDayOffset - 1 downTo 0)
            add(LocalDate.of(prevYearMonth.year, prevYearMonth.month, prevMonthDays - i))
        for (day in 1..yearMonth.lengthOfMonth())
            add(LocalDate.of(displayYear, displayMonth, day))
        var next = 1
        while (size < 42) add(LocalDate.of(nextYearMonth.year, nextYearMonth.month, next++))
    }

    val selectedTasks = localTasks[selectedDate] ?: emptyList()
    val monthName = yearMonth.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())

    // All tasks sorted by date for the agenda strip
    val agendaEntries = remember(localTasks.entries.map { it.key to it.value }) {
        localTasks.entries.sortedBy { it.key }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        // Main card — vertically scrollable so the agenda can extend below
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF252535).copy(0.93f), Color(0xFF1A1A2A).copy(0.93f))))
                .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(20.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Calendar + right panel side by side ──────────────
            Row(modifier = Modifier.fillMaxWidth()) {

                // ── LEFT: calendar grid ──────────────────────────
                Column(modifier = Modifier.weight(1.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$displayYear", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                        Spacer(Modifier.width(4.dp))
                        CalNavArrow("<", fontFamily) { displayYear-- }
                        Spacer(Modifier.width(2.dp))
                        CalNavArrow(">", fontFamily) { displayYear++ }
                        Spacer(Modifier.width(14.dp))
                        Text(monthName, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                        Spacer(Modifier.width(4.dp))
                        CalNavArrow("<", fontFamily) {
                            if (displayMonth == 1) { displayYear--; displayMonth = 12 } else displayMonth--
                        }
                        Spacer(Modifier.width(2.dp))
                        CalNavArrow(">", fontFamily) {
                            if (displayMonth == 12) { displayYear++; displayMonth = 1 } else displayMonth++
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(0.18f))
                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(label, color = Color.White.copy(0.5f), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, fontFamily = fontFamily)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                val isCurrentMonth = date.monthValue == displayMonth && date.year == displayYear
                                val isToday    = date == today
                                val isSelected = date == selectedDate
                                val isPast     = date.isBefore(today)
                                val cellTasks  = localTasks[date]
                                val hasTasks   = cellTasks?.isNotEmpty() == true
                                val firstColor = cellTasks?.firstOrNull()
                                    ?.let { TASK_COLORS.getOrElse(it.colorIndex) { TASK_COLORS[0] } }
                                    ?: TASK_COLORS[0]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            when {
                                                isSelected && hasTasks -> firstColor.copy(alpha = 0.28f)
                                                isSelected             -> Color.White.copy(alpha = 0.20f)
                                                isToday                -> Color.White.copy(alpha = 0.07f)
                                                else                   -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected && hasTasks) firstColor.copy(alpha = 0.7f)
                                                    else Color.White.copy(alpha = 0.38f),
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                        .clickable(enabled = !isPast) {
                                            selectedDate = date
                                            addingTask = false
                                            newTaskText = lastSavedTaskName
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(
                                            "${date.dayOfMonth}",
                                            color = when {
                                                isPast         -> Color.White.copy(alpha = 0.18f)
                                                isToday        -> Color.White
                                                isCurrentMonth -> Color.White.copy(alpha = 0.85f)
                                                else           -> Color.White.copy(alpha = 0.22f)
                                            },
                                            fontSize = 20.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = fontFamily
                                        )
                                        if (hasTasks && !isPast) {
                                            Spacer(Modifier.height(1.dp))
                                            Box(Modifier.size(4.dp).clip(CircleShape).background(firstColor))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // end left column

                Spacer(Modifier.width(12.dp))

                // ── RIGHT: selected-day detail ───────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(0.07f))
                        .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        selectedDate.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()),
                        color = Color.White.copy(0.70f), fontSize = 22.sp, fontFamily = fontFamily
                    )
                    Text(
                        "${selectedDate.dayOfMonth}",
                        color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Thin, lineHeight = 52.sp, fontFamily = fontFamily
                    )
                    Text(
                        selectedDate.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.getDefault()),
                        color = Color.White.copy(0.70f), fontSize = 21.sp, fontFamily = fontFamily
                    )

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(0.20f))
                    Spacer(Modifier.height(8.dp))

                    val pastSelected = selectedDate.isBefore(today)
                    if (!pastSelected) {
                        if (addingTask) {
                            BasicTextField(
                                value = newTaskText,
                                onValueChange = { newTaskText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 21.sp, fontFamily = fontFamily),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newTaskText.isNotBlank()) {
                                        lastSavedTaskName = extractName(newTaskText)
                                        commitTasks(selectedDate, selectedTasks + CalendarTask(text = newTaskText.trim()))
                                        newTaskText = lastSavedTaskName
                                    }
                                    addingTask = false
                                }),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(0.12f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                decorationBox = { inner ->
                                    if (newTaskText.isEmpty()) Text("Task name…", color = Color.White.copy(0.35f), fontSize = 21.sp, fontFamily = fontFamily)
                                    inner()
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(
                                    "Cancel", color = Color.White.copy(0.45f), fontSize = 19.sp, fontFamily = fontFamily,
                                    modifier = Modifier.clickable { addingTask = false; newTaskText = lastSavedTaskName }.padding(4.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Set time", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily,
                                    modifier = Modifier.clickable { if (newTaskText.isNotBlank()) showTimePicker = true }.padding(4.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(0.09f))
                                    .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(8.dp))
                                    .clickable { addingTask = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+ Add Task", color = Color.White.copy(0.60f), fontSize = 21.sp, fontFamily = fontFamily)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (selectedTasks.isNotEmpty()) {
                        Text(
                            "TASKS", color = Color.White.copy(0.35f), fontSize = 17.sp,
                            fontFamily = fontFamily, letterSpacing = 2.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        // Plain Column — no LazyColumn inside a vertically scrollable parent
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            selectedTasks.forEach { task ->
                                val taskColor = TASK_COLORS.getOrElse(task.colorIndex) { TASK_COLORS[0] }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(taskColor.copy(alpha = 0.12f))
                                        .border(1.dp, taskColor.copy(alpha = 0.40f), RoundedCornerShape(7.dp))
                                        .pointerInput(task.id, task.colorIndex) {
                                            detectTapGestures(
                                                onTap = {
                                                    val next = (task.colorIndex + 1) % TASK_COLORS.size
                                                    commitTasks(selectedDate, selectedTasks.map {
                                                        if (it.id == task.id) it.copy(colorIndex = next) else it
                                                    })
                                                },
                                                onLongPress = { taskToDelete = task }
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(taskColor))
                                    Spacer(Modifier.width(7.dp))
                                    Text(task.text, color = taskColor, fontSize = 19.sp, fontFamily = fontFamily, modifier = Modifier.weight(1f), maxLines = 2)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF09e8ad).copy(alpha = 0.85f))
                            .clickable {
                                if (onSave != null) onSave(localTasks.toMap())
                                else onClose()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SAVE", color = Color.Black, fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                    }
                } // end right column
            } // end Row

            // ── ALL TASKS agenda strip (below calendar) ──────────
            if (agendaEntries.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(0.18f))
                Spacer(Modifier.height(12.dp))

                Text(
                    "ALL TASKS",
                    color = Color.White.copy(0.40f), fontSize = 17.sp,
                    fontFamily = fontFamily, letterSpacing = 3.sp
                )
                Spacer(Modifier.height(10.dp))

                agendaEntries.forEach { (date, tasks) ->
                    if (tasks.isEmpty()) return@forEach

                    // Date header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(0.08f))
                                .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${date.dayOfMonth} ${date.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()} ${date.year}",
                                color = Color.White.copy(0.65f),
                                fontSize = 17.sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.10f))
                    }

                    // Task chips for this date
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        tasks.forEach { task ->
                            val taskColor = TASK_COLORS.getOrElse(task.colorIndex) { TASK_COLORS[0] }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(taskColor.copy(alpha = 0.10f))
                                    .border(1.dp, taskColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedDate = date
                                        displayYear = date.year
                                        displayMonth = date.monthValue
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(taskColor))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    task.text,
                                    color = taskColor,
                                    fontSize = 17.sp,
                                    fontFamily = fontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        } // end scrollable card Column

        // ── Time picker overlay ───────────────────────────────────
        if (showTimePicker) {
            TimeDiscPicker(
                titleFont = fontFamily,
                contentFont = fontFamily,
                onConfirm = { h, m, isAm, repeat, repeatInfo ->
                    val displayHour = if (h == 0) 12 else h
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, if (isAm) "AM" else "PM")
                    val repeatSuffix = if (repeat && repeatInfo.isNotBlank()) " [$repeatInfo]" else ""
                    val taskName = newTaskText.trim()
                    val taskWithTime = "$taskName @ $timeStr$repeatSuffix"
                    lastSavedTaskName = taskName
                    commitTasks(selectedDate, selectedTasks + CalendarTask(text = taskWithTime))
                    newTaskText = lastSavedTaskName
                    showTimePicker = false
                    addingTask = false
                },
                onDismiss = { showTimePicker = false }
            )
        }

        // ── Delete confirmation dialog ────────────────────────────
        if (taskToDelete != null) {
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                containerColor = Color(0xFF1A1A2A),
                title = {
                    Text("DELETE TASK", color = Color(0xFFE41417), fontFamily = fontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Are you sure you want to delete\n\"${taskToDelete!!.text.take(40)}\"?",
                        color = Color.White.copy(0.75f), fontFamily = fontFamily, fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE41417))
                            .clickable {
                                val del = taskToDelete!!
                                commitTasks(selectedDate, selectedTasks.filter { it.id != del.id })
                                taskToDelete = null
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("YES", color = Color.White, fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.08f))
                            .border(1.dp, Color.White.copy(0.25f), RoundedCornerShape(8.dp))
                            .clickable { taskToDelete = null }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NO", color = Color.White, fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun CalNavArrow(symbol: String, fontFamily: FontFamily, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(0.10f))
            .border(1.dp, Color.White.copy(0.20f), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
    }
}
