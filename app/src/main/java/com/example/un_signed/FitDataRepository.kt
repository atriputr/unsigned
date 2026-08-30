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

    // ── User profile ───────────────────────────────────────────
    fun saveUserProfile(p: UserProfile) = saveJson("user_profile.json", p)
    fun loadUserProfile(): UserProfile = loadJson("user_profile.json", UserProfile())

    // ── Weather cache ──────────────────────────────────────────
    fun saveWeatherCache(w: WeatherData) = saveJson("weather_cache.json", w)
    fun loadWeatherCache(): WeatherData = loadJson("weather_cache.json", WeatherData())

    // ── Water history (per day) ────────────────────────────────
    fun loadWaterHistory(): Map<String, WaterDailyLog> =
        loadJson("water_history.json", emptyMap<String, WaterDailyLog>())
    fun saveWaterEntry(date: LocalDate, log: WaterDailyLog) {
        val h = loadWaterHistory().toMutableMap()
        if (log.glassesConsumed == 0) h.remove(date.toString())
        else h[date.toString()] = log.copy(date = date.toString())
        saveJson("water_history.json", h)
    }
    fun loadWaterEntry(date: LocalDate): WaterDailyLog? =
        loadWaterHistory()[date.toString()]

    // ── Exercise ───────────────────────────────────────────────
    fun saveExerciseEntries(entries: List<ExerciseEntry>) =
        saveJson("exercise.json", entries)
    fun loadExerciseEntries(): List<ExerciseEntry> =
        loadJson("exercise.json", emptyList<ExerciseEntry>())

    // ── Sleep ──────────────────────────────────────────────────
    fun saveSleepEntries(entries: List<SleepEntry>) =
        saveJson("sleep.json", entries)
    fun loadSleepEntries(): List<SleepEntry> =
        loadJson("sleep.json", emptyList<SleepEntry>())

    // ── Log entries (meds/suppliments/severe/diet) ─────────────
    fun saveLogEntries(entries: List<LogEntry>) =
        saveJson("log_entries.json", entries)
    fun loadLogEntries(): List<LogEntry> =
        loadJson("log_entries.json", emptyList<LogEntry>())

    // ── Skills (hobby/minor/major) ─────────────────────────────
    fun saveSkillItems(items: List<SkillItem>) =
        saveJson("skills.json", items)
    fun loadSkillItems(): List<SkillItem> =
        loadJson("skills.json", emptyList<SkillItem>())

    // ── Activities (cycling/yoga/walking) ──────────────────────
    fun saveActivitySessions(sessions: List<ActivitySession>) =
        saveJson("activities.json", sessions)
    fun loadActivitySessions(): List<ActivitySession> =
        loadJson("activities.json", emptyList<ActivitySession>())

    // ── Timer / Stopwatch persisted state ──────────────────────
    fun saveTimerState(s: TimerPersistedState) = saveJson("timer_state.json", s)
    fun loadTimerState(): TimerPersistedState = loadJson("timer_state.json", TimerPersistedState())
    fun clearTimerState() = saveJson("timer_state.json", TimerPersistedState())

    fun saveStopwatchState(s: StopwatchPersistedState) = saveJson("stopwatch_state.json", s)
    fun loadStopwatchState(): StopwatchPersistedState = loadJson("stopwatch_state.json", StopwatchPersistedState())
    fun clearStopwatchState() = saveJson("stopwatch_state.json", StopwatchPersistedState())

    // ── App preferences (units, theme, haptics) ────────────────
    fun saveAppPreferences(p: AppPreferences) = saveJson("app_prefs.json", p)
    fun loadAppPreferences(): AppPreferences = loadJson("app_prefs.json", AppPreferences())

    // ── Weight history ─────────────────────────────────────────
    fun saveWeightEntries(list: List<WeightEntry>) = saveJson("weight_log.json", list)
    fun loadWeightEntries(): List<WeightEntry> = loadJson("weight_log.json", emptyList<WeightEntry>())

    // ── Focus sessions (pomodoro history) ──────────────────────
    fun saveFocusSessions(list: List<FocusSession>) = saveJson("focus_sessions.json", list)
    fun loadFocusSessions(): List<FocusSession> = loadJson("focus_sessions.json", emptyList<FocusSession>())

    fun saveFocusState(s: FocusTimerState) = saveJson("focus_state.json", s)
    fun loadFocusState(): FocusTimerState = loadJson("focus_state.json", FocusTimerState())
    fun clearFocusState() = saveJson("focus_state.json", FocusTimerState())

    // ── Sleep session in progress ──────────────────────────────
    fun saveSleepSession(s: SleepSessionState) = saveJson("sleep_session.json", s)
    fun loadSleepSession(): SleepSessionState = loadJson("sleep_session.json", SleepSessionState())
    fun clearSleepSession() = saveJson("sleep_session.json", SleepSessionState())

    // ── Fitness samples (Health Connect / step sensor) ─────────
    fun saveFitnessSamples(list: List<FitnessSample>) = saveJson("fitness_samples.json", list)
    fun loadFitnessSamples(): List<FitnessSample> = loadJson("fitness_samples.json", emptyList<FitnessSample>())
    fun upsertFitnessSample(sample: FitnessSample) {
        val list = loadFitnessSamples().toMutableList()
        val idx = list.indexOfFirst { it.dateIso == sample.dateIso }
        if (idx >= 0) list[idx] = sample else list.add(sample)
        saveFitnessSamples(list)
    }

    // ── Junk log (product-catalogued) ──────────────────────────
    fun saveJunkLogEntries(entries: List<JunkLogEntry>) = saveJson("junk_log.json", entries)
    fun loadJunkLogEntries(): List<JunkLogEntry> = loadJson("junk_log.json", emptyList<JunkLogEntry>())
    fun addJunkLogEntry(entry: JunkLogEntry) {
        val list = loadJunkLogEntries() + entry
        saveJunkLogEntries(list)
    }
}
