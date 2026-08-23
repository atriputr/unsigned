package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import java.util.*

@Composable
fun CourseScreen(
    titleFont: FontFamily,
    contentFont: FontFamily,
    savedCourses: MutableList<Subject>, // Reusing the Subject data class as template is exact same
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    var showEntryOverlay by remember { mutableStateOf(false) }
    var selectedCourseForDetail by remember { mutableStateOf<Subject?>(null) }
    var showPastPage by remember { mutableStateOf(false) }

    val activeCourses = savedCourses.filter { subj ->
        subj.chapters.any { !it.isCompleted } || subj.chapters.isEmpty()
    }
    val completedCourses = savedCourses.filter { subj ->
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
                    text = LocalStrings.current.courseManagement,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // ENTER COURSE Button
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
                        text = LocalStrings.current.enterCourse,
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
                        text = LocalStrings.current.past,
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
                    text = LocalStrings.current.activeSavedCourses,
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
                    items(activeCourses, key = { it.id }) { course ->
                        SubjectListItem(course, contentFont) {
                            selectedCourseForDetail = course
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = LocalStrings.current.back,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    fontFamily = titleFont,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        } else {
            // PAST COURSES PAGE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LocalStrings.current.past,
                    color = Color(0xFF09e8ad),
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color(0xFF09e8ad).copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (completedCourses.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(LocalStrings.current.noCompletedCoursesYet, color = Color.White.copy(alpha = 0.3f), fontFamily = contentFont)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(completedCourses, key = { it.id }) { course ->
                            SubjectListItem(course, contentFont) {
                                selectedCourseForDetail = course
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
                    Text(LocalStrings.current.backToManagement, color = Color.White, fontSize = 16.sp, fontFamily = titleFont)
                }
            }
        }

        if (showEntryOverlay) {
            SubjectEntryOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                mainTitle = "WHAT IT'S ABOUT!",
                showDurationField = true,
                onSaveAndExit = { name, chapters, duration ->
                    savedCourses.add(Subject(name = name, chapters = chapters, durationMin = duration))
                    showEntryOverlay = false
                },
                onClose = { showEntryOverlay = false }
            )
        }

        if (selectedCourseForDetail != null) {
            SubjectDetailOverlay(
                subject = selectedCourseForDetail!!,
                titleFont = titleFont,
                contentFont = contentFont,
                onUpdateChapter = { chapterId, isChecked ->
                    val idx = savedCourses.indexOfFirst { it.id == selectedCourseForDetail!!.id }
                    if (idx != -1) {
                        val current = savedCourses[idx]
                        val updated = current.chapters.map {
                            if (it.id == chapterId) it.copy(isCompleted = isChecked) else it
                        }
                        savedCourses[idx] = current.copy(chapters = updated)
                        selectedCourseForDetail = savedCourses[idx]
                    }
                },
                onClose = { selectedCourseForDetail = null }
            )
        }
    }
}
