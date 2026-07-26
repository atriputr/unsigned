package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.*

@Composable
fun PracticeScreen(
    titleFont: FontFamily,
    contentFont: FontFamily,
    savedPractices: MutableList<Subject>,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    var showEntryOverlay         by remember { mutableStateOf(false) }
    var selectedPracticeForDetail by remember { mutableStateOf<Subject?>(null) }
    var practiceToAction         by remember { mutableStateOf<Subject?>(null) }
    var showPastPage             by remember { mutableStateOf(false) }

    // Calendar state owned here so it survives overlay switches
    var showRepeatOptions        by remember { mutableStateOf(false) }
    var showPracticeCalendar     by remember { mutableStateOf(false) }
    var practiceCalendarTasks    by remember { mutableStateOf<Map<LocalDate, List<CalendarTask>>>(emptyMap()) }
    var repeatInfoText           by remember { mutableStateOf("") }
    var savedDatesCount          by remember { mutableIntStateOf(0) }

    val activePractices = savedPractices.filter { subj ->
        subj.chapters.any { !it.isCompleted } || subj.chapters.isEmpty()
    }
    val completedPractices = savedPractices.filter { subj ->
        subj.chapters.isNotEmpty() && subj.chapters.all { it.isCompleted }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        if (!showPastPage) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PRACTICE MANAGEMENT",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))))
                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(14.dp))
                        .clickable { showEntryOverlay = true },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "PRACTICE CALENDAR",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontFamily = titleFont,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE41417).copy(alpha = 0.8f))
                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
                        .clickable { showPastPage = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PAST",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ACTIVE SAVED PRACTICES",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontFamily = titleFont,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activePractices) { practice ->
                        SubjectListItem(
                            subject = practice,
                            font = contentFont,
                            onLongClick = { practiceToAction = practice }
                        ) { selectedPracticeForDetail = practice }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BACK",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    fontFamily = titleFont,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAST PRACTICES",
                    color = Color(0xFF09e8ad),
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color(0xFF09e8ad).copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (completedPractices.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("NO COMPLETED PRACTICES YET", color = Color.White.copy(alpha = 0.3f), fontFamily = contentFont)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(completedPractices) { practice ->
                            SubjectListItem(practice, contentFont) {
                                selectedPracticeForDetail = practice
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { showPastPage = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("BACK TO MANAGEMENT", color = Color.White, fontSize = 16.sp, fontFamily = titleFont)
                }
            }
        }

        // Practice Detail entry overlay
        if (showEntryOverlay) {
            SubjectEntryOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                mainTitle = "PRACTICE DETAIL",
                labelName = "Practicing ℳ",
                buttonLabel = "PRACTICE TIME",
                showDurationField = true,
                showCalendarFlow = true,
                hideTwigsEntry = true,
                taskCount = savedDatesCount,           // drives the [N] counter
                repeatInfo = repeatInfoText,           // shows saved date list in summary
                onButtonClick = { showRepeatOptions = true },
                onRepeatSummaryClick = { showPracticeCalendar = true },
                onSaveAndExit = { name, chapters, duration ->
                    savedPractices.add(Subject(
                        name = name,
                        chapters = chapters,
                        durationMin = duration,
                        repeatInfo = repeatInfoText
                    ))
                    // reset for next entry
                    repeatInfoText = ""
                    savedDatesCount = 0
                    practiceCalendarTasks = emptyMap()
                    showEntryOverlay = false
                },
                onClose = { showEntryOverlay = false }
            )
        }

        // Repeat options: Custom / Weekly / Monthly / Daily Till
        if (showRepeatOptions) {
            RepeatOptionsOverlay(
                titleFont = titleFont,
                onSave = { info, tasksMap ->
                    repeatInfoText = info
                    if (tasksMap.isNotEmpty()) {
                        practiceCalendarTasks = tasksMap
                        savedDatesCount = tasksMap.size
                    } else {
                        savedDatesCount = if (info.isNotBlank()) 1 else 0
                    }
                    showRepeatOptions = false
                },
                onClose = { showRepeatOptions = false }
            )
        }

        // Calendar opens ON TOP of the entry overlay; SAVE returns here with data
        if (showPracticeCalendar) {
            GlassCalendarOverlay(
                allTasks = practiceCalendarTasks,
                onUpdateTasks = { date, tasks ->
                    val updated = practiceCalendarTasks.toMutableMap()
                    if (tasks.isEmpty()) updated.remove(date) else updated[date] = tasks
                    practiceCalendarTasks = updated
                },
                fontFamily = titleFont,
                onClose = { showPracticeCalendar = false },
                onSave = { savedTasks ->
                    practiceCalendarTasks = savedTasks
                    savedDatesCount = savedTasks.size   // number of dates with at least one task

                    // Build repeat summary: one line per date showing all task names
                    repeatInfoText = savedTasks.entries
                        .sortedBy { it.key }
                        .joinToString("\n") { (date, taskList) ->
                            val dateStr = "${date.dayOfMonth} " +
                                date.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()
                            val taskStr = taskList.joinToString(", ") { it.text }
                            "$dateStr → $taskStr"
                        }
                    showPracticeCalendar = false   // close calendar, land back on Practice Detail
                }
            )
        }

        if (selectedPracticeForDetail != null) {
            SubjectDetailOverlay(
                subject = selectedPracticeForDetail!!,
                titleFont = titleFont,
                contentFont = contentFont,
                onUpdateChapter = { chapterId, isChecked ->
                    val idx = savedPractices.indexOfFirst { it.id == selectedPracticeForDetail!!.id }
                    if (idx != -1) {
                        val current = savedPractices[idx]
                        savedPractices[idx] = current.copy(chapters = current.chapters.map {
                            if (it.id == chapterId) it.copy(isCompleted = isChecked) else it
                        })
                        selectedPracticeForDetail = savedPractices[idx]
                    }
                },
                onUpdateName = { newName ->
                    val idx = savedPractices.indexOfFirst { it.id == selectedPracticeForDetail!!.id }
                    if (idx != -1) {
                        savedPractices[idx] = savedPractices[idx].copy(name = newName)
                        selectedPracticeForDetail = savedPractices[idx]
                    }
                },
                onAddChapter = { chapterName ->
                    val idx = savedPractices.indexOfFirst { it.id == selectedPracticeForDetail!!.id }
                    if (idx != -1) {
                        savedPractices[idx] = savedPractices[idx].copy(
                            chapters = savedPractices[idx].chapters + Chapter(name = chapterName)
                        )
                        selectedPracticeForDetail = savedPractices[idx]
                    }
                },
                onRemoveChapter = { chapterId ->
                    val idx = savedPractices.indexOfFirst { it.id == selectedPracticeForDetail!!.id }
                    if (idx != -1) {
                        savedPractices[idx] = savedPractices[idx].copy(
                            chapters = savedPractices[idx].chapters.filter { it.id != chapterId }
                        )
                        selectedPracticeForDetail = savedPractices[idx]
                    }
                },
                onEditRepeat = { showRepeatOptions = true },
                onClose = { selectedPracticeForDetail = null }
            )
        }

        // Long-press action dialog
        if (practiceToAction != null) {
            val target = practiceToAction!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { practiceToAction = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(310.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0D0D1A))))
                        .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                        .clickable(enabled = false) {}
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WHAT DO YOU WANT, HUH!!?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = Shadow(color = Color.White.copy(0.4f), blurRadius = 10f)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = target.name.uppercase(),
                        color = Color(0xFFEBC174),
                        fontSize = 14.sp,
                        fontFamily = contentFont,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // DELETE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE41417).copy(0.85f))
                            .clickable {
                                savedPractices.removeAll { it.id == target.id }
                                practiceToAction = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DELETE", color = Color.White, fontSize = 16.sp,
                            fontFamily = titleFont, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // NOT NEEDED ANYMORE → mark all twigs done → moves to PAST tab
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF09e8ad).copy(0.15f))
                            .border(1.dp, Color(0xFF09e8ad).copy(0.55f), RoundedCornerShape(12.dp))
                            .clickable {
                                val idx = savedPractices.indexOfFirst { it.id == target.id }
                                if (idx != -1) {
                                    val p = savedPractices[idx]
                                    // Ensure at least one completed chapter so the PAST filter picks it up
                                    val doneChapters = p.chapters
                                        .map { it.copy(isCompleted = true) }
                                        .ifEmpty { listOf(Chapter(name = "—", isCompleted = true)) }
                                    savedPractices[idx] = p.copy(chapters = doneChapters)
                                }
                                practiceToAction = null
                                showPastPage = true   // navigate straight to PAST tab
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NOT NEEDED ANYMORE", color = Color(0xFF09e8ad), fontSize = 14.sp,
                            fontFamily = titleFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
