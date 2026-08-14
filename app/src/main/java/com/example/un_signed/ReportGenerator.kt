package com.example.un_signed

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ReportGenerator {

    fun generate(
        sessions: List<AppSession>,
        subjects: List<Subject>,
        courses: List<Subject>,
        practices: List<Subject>,
        habits: List<Habit>,
        calendarTasks: Map<LocalDate, List<CalendarTask>>,
        junkHistory: Map<String, Int>,
        from: LocalDate,
        to: LocalDate,
        periodIdx: Int
    ): String {
        val periodLabel = listOf("Last 7 Days", "Last 30 Days", "Last 3 Months", "Last Year")[periodIdx]
        val ss = AnalyticsEngine.computeSessionStats(sessions, from, to)
        val es = AnalyticsEngine.computeEducationStats(subjects, courses, practices)
        val cs = AnalyticsEngine.computeCalendarStats(calendarTasks)
        val hs = AnalyticsEngine.computeHealthStats(junkHistory, from, to)
        val now = java.time.LocalDateTime.now().toString().take(16).replace('T', ' ')

        // Extended health data
        val profile        = FitDataRepository.loadUserProfile()
        val waterHistory   = FitDataRepository.loadWaterHistory()
        val exerciseAll    = FitDataRepository.loadExerciseEntries()
        val sleepAll       = FitDataRepository.loadSleepEntries()
        val logsAll        = FitDataRepository.loadLogEntries()
        val skillsAll      = FitDataRepository.loadSkillItems()
        val activitiesAll  = FitDataRepository.loadActivitySessions()
        val periodDates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()

        val waterAvg = if (periodDates.isNotEmpty()) periodDates.map { waterHistory[it.toString()]?.let { w -> w.glassesConsumed * w.glassMl } ?: 0 }.average() else 0.0
        val exerciseTotal = exerciseAll.filter { dateInRange(it.dateIso, from, to) }.sumOf { it.minutes }
        val sleepInPeriod = sleepAll.filter { dateInRange(it.wakeDateIso, from, to) }
        val sleepAvgHrs = if (sleepInPeriod.isNotEmpty()) sleepInPeriod.map { it.durationHours }.average() else 0.0

        return buildString {
            appendLine("<!DOCTYPE html><html lang='en'><head>")
            appendLine("<meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>")
            appendLine("<title>Unsigned — Progress Report</title><style>${css()}</style></head>")
            appendLine("<body><div class='container'>")

            // Header
            appendLine("<div class='header'><h1>UN-SIGNED</h1>")
            appendLine("<div class='subtitle'>PROGRESS REPORT &mdash; ${periodLabel.uppercase()}</div>")
            appendLine("<div class='meta'>Generated $now &bull; $from to $to</div></div>")

            // Summary grid
            appendLine("<div class='summary-grid'>")
            appendLine(statCard(fmtMin(ss.totalMinutes), "Time in App"))
            appendLine(statCard("${ss.totalSessions}", "Sessions"))
            appendLine(statCard("${cs.completedTasks} / ${cs.totalTasks}", "Tasks Done"))
            appendLine(statCard("${es.totalChaptersDone} / ${es.totalChapters}", "Chapters Done"))
            appendLine("</div>")

            // Education
            appendLine("<div class='section'><div class='section-title'>Education</div><div class='chart-row'>")
            val eduDone = es.totalChaptersDone.toFloat()
            val eduRem = (es.totalChapters - es.totalChaptersDone).toFloat().coerceAtLeast(0f)
            appendLine("<div class='chart-wrap'>${donut(listOf("Done" to eduDone, "Remaining" to eduRem), listOf("#09e8ad","#1a2d40"))}<div class='chart-label'>Chapter Completion</div></div>")
            appendLine("<div class='stats-list'>")
            appendLine(row("Subjects", "${es.completedSubjects} / ${subjects.size} completed"))
            appendLine(row("Courses", "${es.completedCourses} / ${courses.size} completed"))
            appendLine(row("Practices", "${es.completedPractices} / ${practices.size} completed"))
            appendLine(row("Total Chapters", "${es.totalChaptersDone} of ${es.totalChapters} done"))
            appendLine("</div></div></div>")

            // Calendar
            appendLine("<div class='section'><div class='section-title'>Calendar Tasks</div><div class='chart-row'>")
            appendLine("<div class='chart-wrap'>${donut(listOf("Done" to cs.completedTasks.toFloat(), "Pending" to cs.pendingTasks.toFloat()), listOf("#4FC3F7","#1a2d40"))}<div class='chart-label'>Task Completion</div></div>")
            appendLine("<div class='stats-list'>")
            appendLine(row("Total Tasks", "${cs.totalTasks}"))
            appendLine(row("Completed", "${cs.completedTasks}"))
            appendLine(row("Pending", "${cs.pendingTasks}"))
            appendLine(row("Completion Rate", "${(cs.completionRate * 100).toInt()}%"))
            appendLine("</div></div></div>")

            // Habits
            if (habits.isNotEmpty()) {
                val habitColors = listOf("#F4511E","#FB8C00","#F9A825","#43A047","#039BE5","#7B1FA2","#E53935","#00BCD4","#8BC34A")
                appendLine("<div class='section'><div class='section-title'>Habits</div><div class='chart-row'>")
                appendLine("<div class='chart-wrap'>${donut(habits.map { it.name to it.count.toFloat() }, habitColors, showCenter = false)}<div class='chart-label'>Habit Distribution</div></div>")
                appendLine("<table class='h-table'><tr><th>Habit</th><th>Count</th></tr>")
                habits.forEachIndexed { i, h ->
                    val c = habitColors.getOrElse(i) { "#aaa" }
                    appendLine("<tr><td><span style='color:$c'>&#9679;</span> ${h.name}</td><td style='color:$c'>${h.count}</td></tr>")
                }
                appendLine("</table></div></div>")
            }

            // Health
            appendLine("<div class='section'><div class='section-title'>Health</div><div class='chart-row'>")
            appendLine("<div class='stats-list'>")
            appendLine(row("Period Junk Total", "${hs.totalJunkCount}"))
            appendLine(row("Average Per Day", "${"%.1f".format(hs.averagePerDay)}"))
            ss.mostActiveDate?.let { appendLine(row("Most Active Day", it)) }
            appendLine("</div>")
            if (hs.history.isNotEmpty()) {
                val maxH = hs.history.maxOf { it.second }.coerceAtLeast(1)
                appendLine("<div style='flex:1'>")
                appendLine("<div class='bar-label-top'>Junk Count Daily Trend</div>")
                appendLine("<div style='display:flex;align-items:flex-end;height:80px;gap:5px;border-bottom:1px solid #1a2d40;padding-bottom:4px'>")
                hs.history.takeLast(14).forEach { (date, count) ->
                    val h = (count.toFloat() / maxH * 64).toInt().coerceAtLeast(2)
                    appendLine("<div style='flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end'>")
                    appendLine("<div style='width:100%;height:${h}px;background:#E41417;border-radius:2px 2px 0 0;opacity:.85'></div>")
                    appendLine("<div style='font-size:8px;color:#3a5060;margin-top:3px;writing-mode:vertical-rl;transform:rotate(180deg)'>${date.takeLast(5)}</div>")
                    appendLine("</div>")
                }
                appendLine("</div></div>")
            }
            appendLine("</div></div>")

            // Sessions
            appendLine("<div class='section'><div class='section-title'>Session Detail</div><div class='stats-list'>")
            appendLine(row("Total Sessions", "${ss.totalSessions}"))
            appendLine(row("Total Time", fmtMin(ss.totalMinutes)))
            appendLine(row("Average Session", fmtMin(ss.averageSessionMinutes.toLong())))
            ss.mostActiveDate?.let { appendLine(row("Most Active Date", it)) }
            appendLine("</div></div>")

            // ── Health profile ─────────────────────────────
            if (profile.setupComplete) {
                appendLine("<div class='section'><div class='section-title'>Health Profile</div><div class='stats-list'>")
                appendLine(row("Name", profile.name))
                appendLine(row("Age", "${profile.ageYears}"))
                appendLine(row("Weight", "%.1f kg".format(profile.weightKg)))
                appendLine(row("Height", "%.1f cm".format(profile.heightCm)))
                appendLine(row("BMI", "%.1f (%s)".format(profile.bmi, profile.bmiCategory)))
                if (profile.bmr > 0) appendLine(row("BMR", "%.0f kcal/day".format(profile.bmr)))
                if (profile.tdee > 0) appendLine(row("TDEE", "%.0f kcal/day".format(profile.tdee)))
                appendLine(row("Activity Level", profile.activityLevel))
                appendLine("</div></div>")
            }

            // ── Water ─────────────────────────────────────
            if (waterHistory.isNotEmpty()) {
                appendLine("<div class='section'><div class='section-title'>Hydration</div><div class='chart-row'>")
                appendLine("<div class='stats-list'>")
                appendLine(row("Avg Daily Intake", "%.2f L".format(waterAvg / 1000.0)))
                val goalDays = periodDates.count {
                    val e = waterHistory[it.toString()] ?: return@count false
                    (e.glassesConsumed * e.glassMl) >= e.goalMl
                }
                appendLine(row("Days Goal Met", "$goalDays / ${periodDates.size}"))
                appendLine("</div>")
                appendLine("<div style='flex:1'>")
                appendLine("<div class='bar-label-top'>Water Daily Trend (ml)</div>")
                val trend = periodDates.takeLast(14).map { d -> d.toString() to ((waterHistory[d.toString()]?.let { it.glassesConsumed * it.glassMl }) ?: 0) }
                val maxMl = trend.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                appendLine("<div style='display:flex;align-items:flex-end;height:80px;gap:5px;border-bottom:1px solid #1a2d40;padding-bottom:4px'>")
                trend.forEach { (date, ml) ->
                    val h = (ml.toFloat() / maxMl * 64).toInt().coerceAtLeast(2)
                    appendLine("<div style='flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end'>")
                    appendLine("<div style='width:100%;height:${h}px;background:#4EA8DE;border-radius:2px 2px 0 0;opacity:.85'></div>")
                    appendLine("<div style='font-size:8px;color:#3a5060;margin-top:3px;writing-mode:vertical-rl;transform:rotate(180deg)'>${date.takeLast(5)}</div>")
                    appendLine("</div>")
                }
                appendLine("</div></div></div></div>")
            }

            // ── Exercise ──────────────────────────────────
            if (exerciseAll.isNotEmpty()) {
                appendLine("<div class='section'><div class='section-title'>Exercise</div><div class='stats-list'>")
                appendLine(row("Period Total", "$exerciseTotal min"))
                appendLine(row("Sessions", "${exerciseAll.count { dateInRange(it.dateIso, from, to) }}"))
                val weeklyAvg = if (periodDates.isNotEmpty()) exerciseTotal * 7.0 / periodDates.size else 0.0
                appendLine(row("Weekly Average", "%.0f min".format(weeklyAvg)))
                appendLine(row("WHO Target Ratio", "%.0f%%".format(weeklyAvg / 150.0 * 100.0)))
                appendLine("</div></div>")
            }

            // ── Sleep ─────────────────────────────────────
            if (sleepInPeriod.isNotEmpty()) {
                appendLine("<div class='section'><div class='section-title'>Sleep</div><div class='chart-row'>")
                appendLine("<div class='stats-list'>")
                appendLine(row("Average Duration", "%.2f h".format(sleepAvgHrs)))
                appendLine(row("Target", "%.1f h".format(profile.recommendedSleepHours)))
                appendLine(row("Nights Logged", "${sleepInPeriod.size}"))
                val avgQuality = sleepInPeriod.map { it.quality }.average()
                appendLine(row("Avg Quality", "%.1f / 5 ★".format(avgQuality)))
                appendLine("</div>")
                appendLine("<div style='flex:1'>")
                appendLine("<div class='bar-label-top'>Sleep Duration (hrs)</div>")
                val recent = sleepInPeriod.sortedBy { it.wakeDateIso }.takeLast(14)
                val maxHrs = (recent.maxOfOrNull { it.durationHours } ?: 10.0).coerceAtLeast(1.0)
                appendLine("<div style='display:flex;align-items:flex-end;height:80px;gap:5px;border-bottom:1px solid #1a2d40;padding-bottom:4px'>")
                recent.forEach { s ->
                    val h = (s.durationHours / maxHrs * 64).toInt().coerceAtLeast(2)
                    val color = if (s.durationHours >= profile.recommendedSleepHours - 1) "#B19CFF" else "#FF9066"
                    appendLine("<div style='flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end'>")
                    appendLine("<div style='width:100%;height:${h}px;background:$color;border-radius:2px 2px 0 0;opacity:.85'></div>")
                    appendLine("<div style='font-size:8px;color:#3a5060;margin-top:3px;writing-mode:vertical-rl;transform:rotate(180deg)'>${s.wakeDateIso.takeLast(5)}</div>")
                    appendLine("</div>")
                }
                appendLine("</div></div></div></div>")
            }

            // ── Meds / Suppliments / Severe / Diet ────────
            val logsInPeriod = logsAll.filter { dateInRange(it.dateIso, from, to) }
            if (logsInPeriod.isNotEmpty()) {
                appendLine("<div class='section'><div class='section-title'>Health Logs</div><div class='stats-list'>")
                listOf("meds", "suppliments", "severe", "diet").forEach { cat ->
                    val count = logsInPeriod.count { it.category == cat }
                    if (count > 0) appendLine(row(cat.replaceFirstChar { it.uppercase() }, "$count entries"))
                }
                appendLine("</div></div>")
            }

            // ── Skills ────────────────────────────────────
            if (skillsAll.isNotEmpty()) {
                appendLine("<div class='section'><div class='section-title'>Skill Practice</div><table class='h-table'><tr><th>Skill</th><th>Category</th><th>Minutes</th></tr>")
                skillsAll.sortedByDescending { it.totalMinutes }.forEach { s ->
                    appendLine("<tr><td>${s.name}</td><td style='color:#7aa5c0'>${s.category}</td><td>${s.totalMinutes}</td></tr>")
                }
                appendLine("</table></div>")
            }

            // ── Activities ────────────────────────────────
            val actsInPeriod = activitiesAll.filter { dateInRange(it.dateIso, from, to) }
            if (actsInPeriod.isNotEmpty()) {
                appendLine("<div class='section'><div class='section-title'>Activities</div><div class='stats-list'>")
                listOf("cycling", "yoga", "walking").forEach { act ->
                    val sessions = actsInPeriod.filter { it.activity == act }
                    if (sessions.isNotEmpty()) {
                        val mins = sessions.sumOf { it.durationMinutes }
                        val km = sessions.mapNotNull { it.distanceKm }.sum()
                        val extra = if (km > 0) " · %.1f km".format(km) else ""
                        appendLine(row(act.replaceFirstChar { it.uppercase() }, "${sessions.size} sess · $mins min$extra"))
                    }
                }
                appendLine("</div></div>")
            }

            appendLine("<div class='footer'>Generated by Unsigned App &bull; Your personal growth tracker</div>")
            appendLine("</div></body></html>")
        }
    }

    private fun dateInRange(iso: String, from: LocalDate, to: LocalDate): Boolean {
        val d = try { LocalDate.parse(iso) } catch (_: Exception) { return false }
        return !d.isBefore(from) && !d.isAfter(to)
    }

    fun shareReport(context: Context, html: String) {
        try {
            val dir = File(context.cacheDir, "reports").also { it.mkdirs() }
            val file = File(dir, "unsigned_report_${LocalDate.now()}.html")
            file.writeText(html)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveToDownloads(context: Context, html: String): Boolean = try {
        val fileName = "unsigned_report_${LocalDate.now()}.html"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/html")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(html.toByteArray()) } }
        uri != null
    } catch (e: Exception) { false }

    // ── SVG donut chart ──────────────────────────────────────────
    private fun donut(
        slices: List<Pair<String, Float>>,
        colors: List<String>,
        size: Int = 160,
        showCenter: Boolean = true
    ): String {
        val nz = slices.filter { it.second > 0f }
        if (nz.isEmpty()) return emptyDonut(size)
        val total = nz.sumOf { it.second.toDouble() }.toFloat()
        val cx = size / 2.0; val cy = size / 2.0
        val r = size / 2.0 - 6; val ir = r * 0.44

        val sb = StringBuilder("<svg width='$size' height='$size' viewBox='0 0 $size $size'>")
        if (nz.size == 1) {
            sb.append("<circle cx='${f(cx)}' cy='${f(cy)}' r='${f(r)}' fill='${colors[0]}'/>")
        } else {
            var angle = -PI / 2.0
            nz.forEachIndexed { i, (_, v) ->
                val sweep = v / total * 2 * PI
                val x1 = cx + r * cos(angle); val y1 = cy + r * sin(angle)
                val x2 = cx + r * cos(angle + sweep); val y2 = cy + r * sin(angle + sweep)
                val la = if (sweep > PI) 1 else 0
                val c = colors.getOrElse(i) { "#808080" }
                sb.append("<path d='M ${f(cx)},${f(cy)} L ${f(x1)},${f(y1)} A ${f(r)},${f(r)} 0 $la,1 ${f(x2)},${f(y2)} Z' fill='$c'/>")
                angle += sweep
            }
        }
        sb.append("<circle cx='${f(cx)}' cy='${f(cy)}' r='${f(ir)}' fill='#0c1824'/>")
        if (showCenter) {
            val pct = (nz[0].second / total * 100).toInt()
            sb.append("<text x='${f(cx)}' y='${f(cy - 5)}' text-anchor='middle' fill='#09e8ad' font-size='20' font-weight='bold' font-family='monospace'>$pct%</text>")
            sb.append("<text x='${f(cx)}' y='${f(cy + 14)}' text-anchor='middle' fill='#5a7a90' font-size='9' font-family='monospace'>${nz[0].first.take(10)}</text>")
        }
        sb.append("</svg>")
        val legend = buildString {
            append("<div class='legend'>")
            nz.forEachIndexed { i, (label, value) ->
                val c = colors.getOrElse(i) { "#808080" }
                append("<div class='legend-item'><span class='ldot' style='background:$c'></span><span>$label: ${value.toInt()}</span></div>")
            }
            append("</div>")
        }
        return sb.toString() + legend
    }

    private fun emptyDonut(size: Int): String {
        val c = size / 2.0; val r = size / 2.0 - 6; val ir = r * 0.44
        return "<svg width='$size' height='$size'><circle cx='${f(c)}' cy='${f(c)}' r='${f(r)}' fill='#1a2d40'/><circle cx='${f(c)}' cy='${f(c)}' r='${f(ir)}' fill='#0c1824'/></svg><div class='legend'><div class='legend-item' style='color:#3a5060'>No data yet</div></div>"
    }

    private fun f(d: Double) = "%.2f".format(d)
    private fun statCard(v: String, label: String) = "<div class='stat-card'><div class='stat-value'>$v</div><div class='stat-label'>${label.uppercase()}</div></div>"
    private fun row(label: String, value: String) = "<div class='stats-item'><span class='sil'>$label</span><span class='siv'>$value</span></div>"
    private fun fmtMin(m: Long) = when { m <= 0L -> "—"; m < 60L -> "${m}m"; else -> "${m / 60}h ${m % 60}m" }

    private fun css() = """
*{box-sizing:border-box;margin:0;padding:0}
body{background:#080b14;color:#e0e0e0;font-family:'Courier New','Lucida Console',monospace}
.container{max-width:860px;margin:0 auto;padding:30px 20px}
.header{text-align:center;padding:50px 20px;background:linear-gradient(180deg,#0d1a2e,#080b14);border-bottom:1px solid #1e3a5c;margin-bottom:36px;border-radius:16px 16px 0 0}
.header h1{font-size:48px;color:#09e8ad;letter-spacing:12px;text-shadow:0 0 20px rgba(9,232,173,.5)}
.subtitle{color:#7aa5c0;font-size:13px;letter-spacing:4px;margin-top:12px}
.meta{color:#4a6b80;font-size:11px;margin-top:6px}
.summary-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:14px;margin-bottom:32px}
.stat-card{background:linear-gradient(135deg,#0f1f35,#0a1520);border:1px solid #1e3a5c;border-radius:14px;padding:22px 16px;text-align:center}
.stat-value{font-size:28px;color:#09e8ad;font-weight:bold;letter-spacing:2px;text-shadow:0 0 10px rgba(9,232,173,.4)}
.stat-label{font-size:10px;color:#5a7a90;letter-spacing:3px;margin-top:8px;text-transform:uppercase}
.section{background:#0c1824;border:1px solid #1a2d40;border-radius:18px;padding:26px;margin-bottom:20px}
.section-title{font-size:13px;color:#EBC174;letter-spacing:5px;margin-bottom:18px;padding-bottom:10px;border-bottom:1px solid #1e2d3a;text-transform:uppercase}
.chart-row{display:flex;gap:24px;align-items:flex-start;flex-wrap:wrap}
.chart-wrap{text-align:center}
.chart-label{font-size:10px;color:#5a7a90;letter-spacing:2px;margin-top:8px;text-transform:uppercase}
.stats-list{flex:1;min-width:180px}
.stats-item{display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-bottom:1px solid #0f1820}
.stats-item:last-child{border-bottom:none}
.sil{color:#7aa5c0;font-size:12px}.siv{color:#e0e0e0;font-size:13px;font-weight:bold}
.legend{margin-top:10px}
.legend-item{display:flex;align-items:center;gap:8px;padding:2px 0;font-size:11px;color:#9ab5c5}
.ldot{width:10px;height:10px;border-radius:50%;flex-shrink:0}
.bar-label-top{font-size:10px;color:#5a7a90;letter-spacing:2px;margin-bottom:10px;text-transform:uppercase}
.h-table{width:100%;border-collapse:collapse;margin-top:4px}
.h-table th{font-size:10px;color:#5a7a90;letter-spacing:3px;text-align:left;padding:8px 0;border-bottom:1px solid #1a2d40}
.h-table td{padding:10px 6px;border-bottom:1px solid #0f1820;font-size:14px}
.h-table td:last-child{text-align:right;font-size:20px;font-weight:bold}
.footer{text-align:center;padding:30px;color:#2a4050;font-size:11px;letter-spacing:2px;margin-top:16px}
    """.trimIndent()
}
