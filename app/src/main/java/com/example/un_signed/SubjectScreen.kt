package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import java.util.*

data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCompleted: Boolean = false
)

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val chapters: List<Chapter> = emptyList(),
    val durationMin: Int = 0,
    val repeatInfo: String = ""
)

@Composable
fun SubjectScreen(
    titleFont: FontFamily,
    contentFont: FontFamily,
    savedSubjects: MutableList<Subject>,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    var showEntryOverlay by remember { mutableStateOf(false) }
    var selectedSubjectForDetail by remember { mutableStateOf<Subject?>(null) }
    var showPastPage by remember { mutableStateOf(false) }

    val activeSubjects = savedSubjects.filter { subj ->
        subj.chapters.any { !it.isCompleted } || subj.chapters.isEmpty()
    }
    val completedSubjects = savedSubjects.filter { subj ->
        subj.chapters.isNotEmpty() && subj.chapters.all { it.isCompleted }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        if (!showPastPage) {
            // MAIN SUBJECT SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SUBJECT MANAGEMENT",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // ENTER SUB Button
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
                        text = "ENTER SUB",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontFamily = titleFont,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PAST Button
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
                    text = "ACTIVE SAVED SUBJECTS",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontFamily = titleFont,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Active Subjects List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeSubjects, key = { it.id }) { subject ->
                        SubjectListItem(subject, contentFont) {
                            selectedSubjectForDetail = subject
                        }
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
            // PAST SUBJECTS PAGE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAST SUBJECTS",
                    color = Color(0xFF09e8ad),
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color(0xFF09e8ad).copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (completedSubjects.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("NO COMPLETED SUBJECTS YET", color = Color.White.copy(alpha = 0.3f), fontFamily = contentFont)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(completedSubjects, key = { it.id }) { subject ->
                            SubjectListItem(subject, contentFont) {
                                selectedSubjectForDetail = subject
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

        // ENTRY OVERLAY
        if (showEntryOverlay) {
            SubjectEntryOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                onSaveAndExit = { name, chapters, duration ->
                    savedSubjects.add(Subject(name = name, chapters = chapters, durationMin = duration))
                    showEntryOverlay = false
                },
                onClose = { showEntryOverlay = false }
            )
        }

        // DETAIL OVERLAY (Checkbox checklist)
        if (selectedSubjectForDetail != null) {
            SubjectDetailOverlay(
                subject = selectedSubjectForDetail!!,
                titleFont = titleFont,
                contentFont = contentFont,
                onUpdateChapter = { chapterId, isChecked ->
                    val subjectIdx = savedSubjects.indexOfFirst { it.id == selectedSubjectForDetail!!.id }
                    if (subjectIdx != -1) {
                        val currentSubject = savedSubjects[subjectIdx]
                        val updatedChapters = currentSubject.chapters.map {
                            if (it.id == chapterId) it.copy(isCompleted = isChecked) else it
                        }
                        savedSubjects[subjectIdx] = currentSubject.copy(chapters = updatedChapters)
                        selectedSubjectForDetail = savedSubjects[subjectIdx]
                    }
                },
                onClose = { selectedSubjectForDetail = null }
            )
        }
    }
}

@Composable
fun SubjectListItem(
    subject: Subject,
    font: FontFamily,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val completedCount = subject.chapters.count { it.isCompleted }
    val total = subject.chapters.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = subject.name.uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = font,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount / $total TWIGS DONE",
                    color = Color(0xFFEBC174).copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontFamily = font
                )
            }
            Text(
                text = ">",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 22.sp,
                fontFamily = font
            )
        }
    }
}

@Composable
fun SubjectEntryOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onSaveAndExit: (String, List<Chapter>, Int) -> Unit,
    onClose: () -> Unit,
    mainTitle: String = "NEW SUBJECT",
    labelName: String = "BRANCH NAME",
    buttonLabel: String = "ENTER TWIGS",
    showDurationField: Boolean = false,
    showCalendarFlow: Boolean = false,
    hideTwigsEntry: Boolean = false,
    onButtonClick: (() -> Unit)? = null,
    onRepeatSummaryClick: (() -> Unit)? = null,
    repeatInfo: String = "",
    taskCount: Int? = null          // when set, overrides the chapters count in the button label
) {
    var subjectName by remember { mutableStateOf("") }
    var durationMin by remember { mutableStateOf(0) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var nameStepDone by remember { mutableStateOf(false) }
    var showEventsSummary by remember { mutableStateOf(false) }
    
    val chapters = remember { mutableStateListOf<Chapter>() }
    var isEnteringChapters by remember { mutableStateOf(false) }
    var chapterIdx by remember { mutableStateOf(0) }

    GlassCard(
        modifier = Modifier.heightIn(max = 650.dp),
        onClose = onClose
    ) {
        if (!isEnteringChapters) {
            Text(
                text = mainTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = titleFont,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // i. Enter subject name
            Text(labelName, color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp, fontFamily = titleFont, modifier = Modifier.align(Alignment.Start))
            BasicTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontFamily = contentFont),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (subjectName.isEmpty()) Text("Enter name...", color = Color.White.copy(alpha = 0.2f), fontSize = 22.sp, fontFamily = contentFont)
                    inner()
                }
            )

            if (!nameStepDone) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBC174).copy(alpha = 0.8f))
                        .clickable(enabled = subjectName.isNotBlank()) {
                            nameStepDone = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MOVE FURTHER",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(if (subjectName.isNotBlank()) 1f else 0.4f)
                    )
                }
            }

            if (nameStepDone) {
                Spacer(modifier = Modifier.height(20.dp))

                if (showDurationField) {
                    // --- DISPLAY BOX FOR REPEAT INFO ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "REPEAT SUMMARY",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontFamily = titleFont
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                .clickable(enabled = repeatInfo.isNotBlank() && onRepeatSummaryClick != null) {
                                    onRepeatSummaryClick?.invoke()
                                }
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Text(
                                text = if (repeatInfo.isNotBlank()) repeatInfo else "E.G. 10:30 AM",
                                color = if (repeatInfo.isBlank()) Color.White.copy(alpha = 0.22f) else Color.White,
                                fontSize = 16.sp,
                                fontFamily = contentFont
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ii. Enter chapters button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable {
                            if (onButtonClick != null) {
                                onButtonClick()
                            } else {
                                if (chapters.isEmpty()) chapters.add(Chapter(name = ""))
                                isEnteringChapters = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$buttonLabel [${taskCount ?: chapters.count { it.name.isNotBlank() }}]",
                        color = Color(0xFFEBC174),
                        fontSize = 16.sp,
                        fontFamily = titleFont
                    )
                }

                // --- CHAPTERS LIST (Mid View) ---
                val filledChapters = chapters.filter { it.name.isNotBlank() }
                if (filledChapters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filledChapters.size, key = { idx -> filledChapters[idx].id }) { idx ->
                            val chapter = filledChapters[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = chapter.isCompleted,
                                    onCheckedChange = { checked ->
                                        val originalIdx = chapters.indexOf(chapter)
                                        if (originalIdx != -1) {
                                            chapters[originalIdx] = chapter.copy(isCompleted = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF09e8ad),
                                        uncheckedColor = Color.White.copy(alpha = 0.3f),
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chapter.name,
                                    color = if (chapter.isCompleted) Color(0xFF09e8ad) else Color.White,
                                    fontSize = 16.sp,
                                    fontFamily = contentFont
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // iii. Save & Exit
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF09e8ad).copy(alpha = 0.8f))
                        .clickable {
                            if (subjectName.isNotBlank()) {
                                onSaveAndExit(subjectName, chapters.filter { it.name.isNotBlank() }.toList(), durationMin)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE & EXIT", color = Color.Black, fontSize = 18.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            if (!hideTwigsEntry) {
                Text(
                    text = "TWIGS ${chapterIdx + 1}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = titleFont
                )
                Text(
                    text = "${chapterIdx + 1} of ${chapters.size}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontFamily = contentFont
                )

                Spacer(modifier = Modifier.height(20.dp))

                BasicTextField(
                    value = chapters.getOrElse(chapterIdx) { Chapter(name = "") }.name,
                    onValueChange = { v ->
                        while (chapters.size <= chapterIdx) chapters.add(Chapter(name = ""))
                        chapters[chapterIdx] = chapters[chapterIdx].copy(name = v)
                    },
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = contentFont),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color(0xFF09e8ad).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (chapters.getOrElse(chapterIdx) { Chapter(name = "") }.name.isEmpty()) {
                            Text("Twig Name...", color = Color.White.copy(alpha = 0.2f), fontSize = 18.sp, fontFamily = contentFont)
                        }
                        inner()
                    }
                )

                Spacer(modifier = Modifier.height(30.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hideTwigsEntry) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chapterIdx > 0) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                            .border(1.dp, Color.White.copy(alpha = if (chapterIdx > 0) 0.2f else 0.05f), RoundedCornerShape(10.dp))
                            .clickable(enabled = chapterIdx > 0) { chapterIdx-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("← PREV", color = Color.White.copy(alpha = if (chapterIdx > 0) 1f else 0.2f), fontSize = 12.sp, fontFamily = titleFont)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEBC174).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFFEBC174), RoundedCornerShape(10.dp))
                        .clickable { isEnteringChapters = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE", color = Color(0xFFEBC174), fontSize = 14.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
                }

                if (!hideTwigsEntry) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF09e8ad).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF09e8ad), RoundedCornerShape(10.dp))
                            .clickable {
                                if (chapterIdx == chapters.size - 1) chapters.add(Chapter(name = ""))
                                chapterIdx++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NEXT →", color = Color(0xFF09e8ad), fontSize = 12.sp, fontFamily = titleFont)
                    }
                }
            }
        }
    }

    if (showCalendarPicker) {
        GlassCalendarOverlay(
            allTasks = emptyMap(),
            onUpdateTasks = { _, _ -> },
            fontFamily = titleFont,
            initialDate = selectedDate,
            onClose = { 
                showCalendarPicker = false 
                showDurationPicker = true // Show time wheel after date
            }
        )
    }

    if (showDurationPicker) {
        DurationClockPicker(
            initialMinutes = durationMin,
            titleFont = titleFont,
            contentFont = contentFont,
            onConfirm = { totalMin ->
                durationMin = totalMin
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
    }

    if (showEventsSummary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PRACTICE CALENDAR", color = Color.White, fontSize = 24.sp, fontFamily = titleFont)
                Spacer(Modifier.height(20.dp))
                
                // Show a calendar view
                GlassCalendarOverlay(
                    allTasks = emptyMap(), // This would show highlighted events
                    onUpdateTasks = { _, _ -> },
                    fontFamily = titleFont,
                    onClose = { showEventsSummary = false }
                )
            }
        }
    }
}

@Composable
fun SubjectDetailOverlay(
    subject: Subject,
    titleFont: FontFamily,
    contentFont: FontFamily,
    onUpdateChapter: (String, Boolean) -> Unit,
    onClose: () -> Unit,
    onUpdateName: ((String) -> Unit)? = null,
    onAddChapter: ((String) -> Unit)? = null,
    onRemoveChapter: ((String) -> Unit)? = null,
    onEditRepeat: (() -> Unit)? = null
) {
    val editable = onUpdateName != null
    var editedName by remember(subject.id) { mutableStateOf(subject.name) }
    var newTwigText by remember { mutableStateOf("") }

    GlassCard(
        modifier = Modifier.heightIn(max = 640.dp),
        onClose = onClose
    ) {
        // ── Name: editable or static ─────────────────────────
        if (editable) {
            BasicTextField(
                value = editedName,
                onValueChange = { editedName = it },
                textStyle = TextStyle(
                    color = Color(0xFFEBC174),
                    fontSize = 22.sp,
                    fontFamily = titleFont,
                    shadow = Shadow(color = Color(0xFFEBC174).copy(alpha = 0.4f), blurRadius = 8f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEBC174).copy(alpha = 0.06f))
                    .border(1.dp, Color(0xFFEBC174).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (editedName.isEmpty()) Text(
                        "Practice name…",
                        color = Color(0xFFEBC174).copy(0.3f),
                        fontSize = 22.sp,
                        fontFamily = titleFont
                    )
                    inner()
                }
            )
            Text(
                "TAP NAME TO EDIT",
                color = Color(0xFFEBC174).copy(0.35f),
                fontSize = 10.sp,
                fontFamily = contentFont,
                modifier = Modifier.padding(top = 3.dp)
            )
        } else {
            Text(
                text = subject.name.uppercase(),
                color = Color(0xFFEBC174),
                fontSize = 24.sp,
                fontFamily = titleFont,
                style = TextStyle(shadow = Shadow(color = Color(0xFFEBC174).copy(alpha = 0.4f), blurRadius = 8f))
            )
            Text(
                text = "TWIG PROGRESS",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(10.dp))

        // ── Repeat summary (if saved) ────────────────────────
        if (subject.repeatInfo.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "REPEAT SUMMARY",
                    color = Color.White.copy(0.45f),
                    fontSize = 11.sp,
                    fontFamily = titleFont
                )
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.05f))
                        .border(
                            1.dp,
                            if (onEditRepeat != null) Color(0xFFEBC174).copy(0.4f)
                            else Color.White.copy(0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .then(if (onEditRepeat != null) Modifier.clickable { onEditRepeat() } else Modifier)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = subject.repeatInfo,
                        color = Color.White.copy(0.8f),
                        fontSize = 13.sp,
                        fontFamily = contentFont
                    )
                }
                if (onEditRepeat != null) {
                    Text(
                        "TAP TO EDIT SCHEDULE",
                        color = Color(0xFFEBC174).copy(0.35f),
                        fontSize = 10.sp,
                        fontFamily = contentFont,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── Twig list ─────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subject.chapters, key = { it.id }) { chapter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onUpdateChapter(chapter.id, !chapter.isCompleted) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = chapter.isCompleted,
                        onCheckedChange = { onUpdateChapter(chapter.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF09e8ad),
                            uncheckedColor = Color.White.copy(alpha = 0.3f),
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = chapter.name,
                        color = if (chapter.isCompleted) Color(0xFF09e8ad) else Color.White,
                        fontSize = 15.sp,
                        fontFamily = contentFont,
                        modifier = Modifier.weight(1f)
                    )
                    if (editable && onRemoveChapter != null) {
                        Text(
                            text = "✕",
                            color = Color(0xFFE41417).copy(0.7f),
                            fontSize = 14.sp,
                            fontFamily = titleFont,
                            modifier = Modifier
                                .clickable { onRemoveChapter(chapter.id) }
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        // ── Add twig row (edit mode only) ────────────────────
        if (editable && onAddChapter != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = newTwigText,
                    onValueChange = { newTwigText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = contentFont),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.06f))
                        .border(1.dp, Color(0xFF09e8ad).copy(0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    decorationBox = { inner ->
                        if (newTwigText.isEmpty()) Text(
                            "New twig name…",
                            color = Color.White.copy(0.22f),
                            fontSize = 14.sp,
                            fontFamily = contentFont
                        )
                        inner()
                    },
                    singleLine = true
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF09e8ad).copy(0.8f))
                        .clickable {
                            if (newTwigText.isNotBlank()) {
                                onAddChapter(newTwigText.trim())
                                newTwigText = ""
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ADD", color = Color.Black, fontSize = 13.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Action button ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (editable) Color(0xFF09e8ad).copy(0.85f) else Color.White.copy(alpha = 0.1f))
                .clickable {
                    if (editable) onUpdateName?.invoke(editedName.trim().ifBlank { subject.name })
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (editable) "SAVE & CLOSE" else "DONE",
                color = if (editable) Color.Black else Color.White,
                fontSize = 16.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
