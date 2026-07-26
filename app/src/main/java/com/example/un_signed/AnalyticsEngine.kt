package com.example.un_signed

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AnalyticsEngine {

    data class SessionStats(
        val totalSessions: Int,
        val totalMinutes: Long,
        val averageSessionMinutes: Float,
        val mostActiveDate: String?
    )

    data class EducationStats(
        val activeSubjects: Int, val completedSubjects: Int,
        val activeCourses: Int, val completedCourses: Int,
        val activePractices: Int, val completedPractices: Int,
        val totalChaptersDone: Int, val totalChapters: Int
    )

    data class CalendarStats(
        val totalTasks: Int,
        val completedTasks: Int,
        val pendingTasks: Int,
        val completionRate: Float
    )

    data class HealthStats(
        val totalJunkCount: Int,
        val averagePerDay: Float,
        val history: List<Pair<String, Int>>
    )

    fun computeSessionStats(sessions: List<AppSession>, from: LocalDate, to: LocalDate): SessionStats {
        val zone = ZoneId.systemDefault()
        val fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val filtered = sessions.filter { it.startTime >= fromMs && it.startTime < toMs && it.endTime != null }
        val totalMs = filtered.sumOf { it.endTime!! - it.startTime }
        val totalMin = totalMs / 60_000L
        val avgMin = if (filtered.isNotEmpty()) totalMs.toFloat() / filtered.size / 60_000f else 0f

        val byDate = filtered.groupBy {
            Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate().toString()
        }
        return SessionStats(
            totalSessions = filtered.size,
            totalMinutes = totalMin,
            averageSessionMinutes = avgMin,
            mostActiveDate = byDate.maxByOrNull { it.value.size }?.key
        )
    }

    fun computeEducationStats(subjects: List<Subject>, courses: List<Subject>, practices: List<Subject>): EducationStats {
        fun active(list: List<Subject>) = list.count { s -> s.chapters.isEmpty() || s.chapters.any { !it.isCompleted } }
        fun completed(list: List<Subject>) = list.count { s -> s.chapters.isNotEmpty() && s.chapters.all { it.isCompleted } }
        val all = subjects + courses + practices
        return EducationStats(
            activeSubjects = active(subjects), completedSubjects = completed(subjects),
            activeCourses = active(courses), completedCourses = completed(courses),
            activePractices = active(practices), completedPractices = completed(practices),
            totalChaptersDone = all.sumOf { s -> s.chapters.count { it.isCompleted } },
            totalChapters = all.sumOf { it.chapters.size }
        )
    }

    fun computeCalendarStats(tasks: Map<LocalDate, List<CalendarTask>>): CalendarStats {
        val all = tasks.values.flatten()
        val done = all.count { it.isDone }
        val total = all.size
        return CalendarStats(
            totalTasks = total,
            completedTasks = done,
            pendingTasks = total - done,
            completionRate = if (total > 0) done.toFloat() / total else 0f
        )
    }

    fun computeHealthStats(history: Map<String, Int>, from: LocalDate, to: LocalDate): HealthStats {
        val fromStr = from.toString(); val toStr = to.toString()
        val sorted = history.entries
            .filter { it.key >= fromStr && it.key <= toStr }
            .sortedBy { it.key }
            .map { it.key to it.value }
        val total = sorted.sumOf { it.second }
        return HealthStats(
            totalJunkCount = total,
            averagePerDay = if (sorted.isNotEmpty()) total.toFloat() / sorted.size else 0f,
            history = sorted
        )
    }
}
