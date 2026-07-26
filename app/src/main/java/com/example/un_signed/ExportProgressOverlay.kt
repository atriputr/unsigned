package com.example.un_signed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import java.time.LocalDate

@Composable
fun ExportProgressOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // Load all persisted data
    val sessions  = remember { FitDataRepository.loadSessions() }
    val subjects  = remember { FitDataRepository.loadSubjects() }
    val courses   = remember { FitDataRepository.loadCourses() }
    val practices = remember { FitDataRepository.loadPractices() }
    val habits    = remember { FitDataRepository.loadHabits() }
    val calTasks  = remember { FitDataRepository.loadCalendarTasks() }
    val junkHist  = remember { FitDataRepository.loadJunkHistory() }
    val today     = remember { LocalDate.now() }

    var selectedPeriod by remember { mutableIntStateOf(0) }

    val fromDate = when (selectedPeriod) {
        0 -> today.minusDays(6)
        1 -> today.minusDays(29)
        2 -> today.minusMonths(3)
        else -> today.minusYears(1)
    }

    val ss = remember(selectedPeriod) { AnalyticsEngine.computeSessionStats(sessions, fromDate, today) }
    val es = remember { AnalyticsEngine.computeEducationStats(subjects, courses, practices) }
    val cs = remember { AnalyticsEngine.computeCalendarStats(calTasks) }
    val hs = remember(selectedPeriod) { AnalyticsEngine.computeHealthStats(junkHist, fromDate, today) }

    val habitColors = listOf(
        Color(0xFFF4511E), Color(0xFFFB8C00), Color(0xFFF9A825),
        Color(0xFF43A047), Color(0xFF039BE5), Color(0xFF7B1FA2),
        Color(0xFFE53935), Color(0xFF00BCD4), Color(0xFF8BC34A)
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.93f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed header + period selector
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ANALYTICS",
                        color = Color.White, fontSize = 26.sp, fontFamily = titleFont,
                        style = TextStyle(shadow = Shadow(Color.White.copy(0.25f), blurRadius = 8f))
                    )
                    Text(
                        "✕", color = Color.White.copy(0.5f), fontSize = 22.sp,
                        modifier = Modifier.clickable { onClose() }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("7D", "30D", "3M", "1Y").forEachIndexed { idx, label ->
                        val sel = selectedPeriod == idx
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFF09e8ad) else Color.White.copy(0.1f))
                                .border(1.dp, if (sel) Color.Transparent else Color.White.copy(0.2f), RoundedCornerShape(8.dp))
                                .clickable { selectedPeriod = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label,
                                color = if (sel) Color.Black else Color.White,
                                fontSize = 14.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Scrollable stats
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

                // Time in app
                item {
                    ACard("TIME IN APP", titleFont) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            AMini("SESSIONS", "${ss.totalSessions}", contentFont)
                            AMini("TOTAL", fmtMin(ss.totalMinutes), contentFont)
                            AMini("AVG", fmtMin(ss.averageSessionMinutes.toLong()), contentFont)
                        }
                        ss.mostActiveDate?.let { d ->
                            Spacer(Modifier.height(8.dp))
                            Text("Most active day: $d", color = Color.White.copy(0.4f), fontSize = 11.sp, fontFamily = contentFont)
                        }
                    }
                }

                // Education
                item {
                    ACard("EDUCATION", titleFont) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val done = es.totalChaptersDone.toFloat()
                            val rem  = (es.totalChapters - es.totalChaptersDone).toFloat().coerceAtLeast(0f)
                            APie(listOf(done, rem), listOf(Color(0xFF09e8ad), Color.White.copy(0.1f)))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ARow("Subjects", "${es.completedSubjects} / ${subjects.size}", contentFont)
                                ARow("Courses", "${es.completedCourses} / ${courses.size}", contentFont)
                                ARow("Practices", "${es.completedPractices} / ${practices.size}", contentFont)
                                ARow("Chapters", "${es.totalChaptersDone} / ${es.totalChapters}", contentFont)
                            }
                        }
                    }
                }

                // Calendar tasks
                item {
                    ACard("CALENDAR TASKS", titleFont) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            APie(
                                listOf(cs.completedTasks.toFloat(), cs.pendingTasks.toFloat()),
                                listOf(Color(0xFF4FC3F7), Color.White.copy(0.1f))
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ARow("Total", "${cs.totalTasks}", contentFont)
                                ARow("Completed", "${cs.completedTasks}", contentFont)
                                ARow("Pending", "${cs.pendingTasks}", contentFont)
                                ARow("Rate", "${(cs.completionRate * 100).toInt()}%", contentFont)
                            }
                        }
                    }
                }

                // Habits
                if (habits.isNotEmpty()) {
                    item {
                        ACard("HABITS", titleFont) {
                            val total = habits.sumOf { it.count }.toFloat()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                APie(
                                    if (total > 0f) habits.map { it.count.toFloat() } else listOf(1f),
                                    if (total > 0f) habitColors else listOf(Color.White.copy(0.1f))
                                )
                                Column(Modifier.weight(1f)) {
                                    habits.forEachIndexed { i, habit ->
                                        val c = habitColors.getOrElse(i) { Color.White }
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            Arrangement.SpaceBetween, Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(c))
                                                Spacer(Modifier.width(6.dp))
                                                Text(habit.name, color = Color.White, fontSize = 13.sp, fontFamily = contentFont)
                                            }
                                            Text("${habit.count}x", color = c, fontSize = 14.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Health
                item {
                    ACard("HEALTH", titleFont) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            AMini("TOTAL JUNK", "${hs.totalJunkCount}", contentFont)
                            AMini("AVG / DAY", "${"%.1f".format(hs.averagePerDay)}", contentFont)
                        }
                        if (hs.history.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text("DAILY TREND", color = Color.White.copy(0.35f), fontSize = 10.sp, fontFamily = contentFont, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            val maxC = hs.history.maxOf { it.second }.coerceAtLeast(1)
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                hs.history.takeLast(14).forEach { (_, count) ->
                                    val frac = count.toFloat() / maxC
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        Box(
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .fillMaxHeight(frac.coerceAtLeast(0.04f))
                                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                                .background(Color(0xFFE41417).copy(alpha = 0.85f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Export buttons
                item {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF09e8ad))
                            .clickable {
                                val html = ReportGenerator.generate(
                                    sessions, subjects, courses, practices, habits,
                                    calTasks, junkHist, fromDate, today, selectedPeriod
                                )
                                ReportGenerator.shareReport(context, html)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GENERATE & SHARE REPORT", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = titleFont)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEBC174))
                            .clickable {
                                val html = ReportGenerator.generate(
                                    sessions, subjects, courses, practices, habits,
                                    calTasks, junkHist, fromDate, today, selectedPeriod
                                )
                                val ok = ReportGenerator.saveToDownloads(context, html)
                                Toast.makeText(context, if (ok) "Saved to Downloads" else "Save failed", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SAVE TO DOWNLOADS", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = titleFont)
                    }
                }
            }
        }
    }
}

// ── Private helper composables ─────────────────────────────────

@Composable
private fun ACard(title: String, titleFont: FontFamily, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.06f))
            .border(1.dp, Color.White.copy(0.11f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color(0xFF09e8ad), fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 3.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun APie(slices: List<Float>, colors: List<Color>, size: Dp = 80.dp) {
    val total = slices.sum()
    Canvas(modifier = Modifier.size(size)) {
        val r = minOf(this.size.width, this.size.height) / 2f
        if (total <= 0f) {
            drawCircle(Color.White.copy(0.08f))
            drawCircle(Color(0xFF0D0D1A), r * 0.40f)
            return@Canvas
        }
        var start = -90f
        slices.forEachIndexed { i, v ->
            if (v <= 0f) return@forEachIndexed
            val sweep = (v / total * 360f).coerceAtLeast(0.01f)
            drawArc(
                color = colors.getOrElse(i) { Color.Gray },
                startAngle = start, sweepAngle = sweep, useCenter = true,
                topLeft = Offset.Zero, size = this.size
            )
            start += sweep
        }
        drawCircle(Color(0xFF0D0D1A), r * 0.40f, Offset(r, r))
    }
}

@Composable
private fun AMini(label: String, value: String, font: FontFamily) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontFamily = font, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(0.4f), fontSize = 9.sp, fontFamily = font, letterSpacing = 1.sp)
    }
}

@Composable
private fun ARow(label: String, value: String, font: FontFamily) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(0.55f), fontSize = 13.sp, fontFamily = font)
        Text(value, color = Color.White, fontSize = 13.sp, fontFamily = font, fontWeight = FontWeight.Bold)
    }
}

private fun fmtMin(m: Long): String = when {
    m <= 0L -> "—"
    m < 60L -> "${m}m"
    else    -> "${m / 60}h ${m % 60}m"
}
