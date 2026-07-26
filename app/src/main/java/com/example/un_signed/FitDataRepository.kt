package com.example.un_signed

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.util.UUID

data class AppSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

data class LectureState(
    val name: String? = null,
    val progress: Float = 0f,
    val topics: List<LectureTopic> = emptyList(),
    val durationMin: Int = 0
)

object FitDataRepository {
    private lateinit var fitDir: File
    private val gson = GsonBuilder().serializeNulls().create()

    fun init(context: Context) {
        fitDir = File(context.filesDir, "fitdata").also { it.mkdirs() }
    }

    private fun file(name: String) = File(fitDir, name)

    private fun saveJson(fileName: String, data: Any) {
        try { file(fileName).writeText(gson.toJson(data)) } catch (e: Exception) { /* ignore */ }
    }

    private inline fun <reified T> loadJson(fileName: String, default: T): T = try {
        val text = file(fileName).takeIf { it.exists() }?.readText() ?: return default
        val type = object : TypeToken<T>() {}.type
        gson.fromJson<T>(text, type) ?: default
    } catch (e: Exception) { default }

    // ── Education ──────────────────────────────────────────────
    fun saveSubjects(s: List<Subject>) = saveJson("subjects.json", s)
    fun loadSubjects(): List<Subject> = loadJson("subjects.json", emptyList<Subject>())

    fun saveCourses(c: List<Subject>) = saveJson("courses.json", c)
    fun loadCourses(): List<Subject> = loadJson("courses.json", emptyList<Subject>())

    fun savePractices(p: List<Subject>) = saveJson("practices.json", p)
    fun loadPractices(): List<Subject> = loadJson("practices.json", emptyList<Subject>())

    // ── Lecture ────────────────────────────────────────────────
    fun saveLectureState(s: LectureState) = saveJson("lecture_state.json", s)
    fun loadLectureState(): LectureState = loadJson("lecture_state.json", LectureState())

    // ── Habits ─────────────────────────────────────────────────
    fun saveHabits(h: List<Habit>) = saveJson("habits.json", h)
    fun loadHabits(): List<Habit> = loadJson("habits.json", emptyList<Habit>())

    // ── Health ─────────────────────────────────────────────────
    fun saveJunkEntry(date: LocalDate, count: Int) {
        val history = loadJunkHistory().toMutableMap()
        if (count == 0) history.remove(date.toString()) else history[date.toString()] = count
        saveJson("junk_history.json", history)
    }
    fun loadJunkHistory(): Map<String, Int> = loadJson("junk_history.json", emptyMap<String, Int>())

    // ── Calendar ───────────────────────────────────────────────
    fun saveCalendarTasks(tasks: Map<LocalDate, List<CalendarTask>>) {
        saveJson("calendar_tasks.json", tasks.mapKeys { it.key.toString() })
    }
    fun loadCalendarTasks(): Map<LocalDate, List<CalendarTask>> {
        val raw: Map<String, List<CalendarTask>> = loadJson("calendar_tasks.json", emptyMap<String, List<CalendarTask>>())
        return try { raw.mapKeys { LocalDate.parse(it.key) } } catch (e: Exception) { emptyMap() }
    }

    // ── Custom Profiles ────────────────────────────────────────
    fun saveCustomProfiles(p: List<CustomProfile>) = saveJson("custom_profiles.json", p)
    fun loadCustomProfiles(): List<CustomProfile> = loadJson("custom_profiles.json", emptyList<CustomProfile>())

    // ── Sessions ───────────────────────────────────────────────
    fun recordSessionStart(): String {
        val sessions = loadSessions().toMutableList()
        val session = AppSession()
        sessions.add(session)
        saveJson("sessions.json", sessions)
        return session.id
    }

    fun recordSessionEnd(id: String) {
        val sessions = loadSessions().toMutableList()
        val idx = sessions.indexOfFirst { it.id == id }
        if (idx >= 0) {
            sessions[idx] = sessions[idx].copy(endTime = System.currentTimeMillis())
            saveJson("sessions.json", sessions)
        }
    }

    fun loadSessions(): List<AppSession> = loadJson("sessions.json", emptyList<AppSession>())
}
