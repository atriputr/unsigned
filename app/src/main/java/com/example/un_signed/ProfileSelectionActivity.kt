package com.example.un_signed

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.time.LocalDate
import java.util.*

class ProfileSelectionActivity : AppCompatActivity() {

    private lateinit var tvClock: ComposeView
    private lateinit var pbYearProgress: ProgressBar
    private lateinit var tvYearPercent: TextView
    private lateinit var cvUpcomingEvents: ComposeView
    private lateinit var composeOverlay: ComposeView
    private val timeHandler = Handler(Looper.getMainLooper())
    
    // State for Custom Profiles
    private val savedCustomProfiles = mutableStateListOf<CustomProfile>()
    
    // State for Calendar Tasks
    private val allCalendarTasks = mutableStateMapOf<LocalDate, List<CalendarTask>>()

    // Active lecture state (drives the home-screen banner)
    private val activeLectureName        = mutableStateOf<String?>(null)
    private val activeLectureProgress    = mutableStateOf(0f)
    private val activeLectureTopics      = mutableStateListOf<LectureTopic>()
    private val activeLectureDurationMin = mutableStateOf(0)
    
    // State for Health
    private val savedJunkCount = mutableStateOf(0)

    // State for Habits
    private val savedHabits = mutableStateListOf<Habit>()

    // State for Subjects
    private val savedSubjects = mutableStateListOf<Subject>()
    private val savedCourses = mutableStateListOf<Subject>()
    private val savedPractices = mutableStateListOf<Subject>()
    private var currentSessionId = ""

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()

            updateYearProgress(now)

            timeHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_selection)

        FitDataRepository.init(this)
        currentSessionId = FitDataRepository.recordSessionStart()

        // Restore persisted data
        savedSubjects.addAll(FitDataRepository.loadSubjects())
        savedCourses.addAll(FitDataRepository.loadCourses())
        savedPractices.addAll(FitDataRepository.loadPractices())
        savedHabits.addAll(FitDataRepository.loadHabits())
        savedCustomProfiles.addAll(FitDataRepository.loadCustomProfiles())
        allCalendarTasks.putAll(FitDataRepository.loadCalendarTasks())
        val lectureState = FitDataRepository.loadLectureState()
        activeLectureName.value = lectureState.name
        activeLectureProgress.value = lectureState.progress
        activeLectureTopics.addAll(lectureState.topics)
        activeLectureDurationMin.value = lectureState.durationMin
        savedJunkCount.value = FitDataRepository.loadJunkHistory()[LocalDate.now().toString()] ?: 0

        tvClock = findViewById(R.id.tvNixieClock)
        pbYearProgress = findViewById(R.id.pbYearProgress)
        tvYearPercent = findViewById(R.id.tvYearPercent)
        cvUpcomingEvents = findViewById(R.id.cvUpcomingEvents)
        composeOverlay = findViewById(R.id.composeOverlay)

        val bebasFont = FontFamily(Font(R.font.bebas_neue))
        val jerseyFont = FontFamily(Font(R.font.jersey_10_charted_regular))

        tvClock.setContent {
            NixieClock(
                fontFamily = jerseyFont,
                onClick = {},
                onTimerTap      = { advanceMode -> showTimerOverlay(jerseyFont, advanceMode) },
                onStopwatchTap  = { advanceMode -> showStopwatchOverlay(jerseyFont, advanceMode) }
            )
        }

        cvUpcomingEvents.setContent {
            UpcomingEventsList(
                allTasks              = allCalendarTasks,
                fontFamily            = bebasFont,
                onEventClick          = { date -> showCalendarOverlay(date, bebasFont) },
                activeLectureName     = activeLectureName.value,
                activeLectureProgress = activeLectureProgress.value,
                onLectureClick        = {
                    composeOverlay.visibility = View.VISIBLE
                    showLectureOverlay(
                        bebasFont, bebasFont,
                        onBack = { composeOverlay.visibility = View.GONE }
                    )
                }
            )
        }

        findViewById<View>(R.id.btnIdealProfile).setOnClickListener {
            showGlassOverlay(bebasFont, bebasFont)
        }

        findViewById<View>(R.id.btnCustomProfile).setOnClickListener {
            showCustomProfileOverlay(bebasFont, bebasFont)
        }

        findViewById<View>(R.id.btnExportProgress).setOnClickListener {
            showExportOverlay(bebasFont, bebasFont)
        }

        val onYearProgressClick = View.OnClickListener {
            showCalendarOverlay(fontFamily = bebasFont)
        }
        pbYearProgress.setOnClickListener(onYearProgressClick)
        tvYearPercent.setOnClickListener(onYearProgressClick)

        timeHandler.post(updateTimeRunnable)
    }

    private fun showStopwatchOverlay(fontFamily: FontFamily, advanceMode: () -> Unit) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            StopwatchOverlay(
                fontFamily = fontFamily,
                onSkip = {
                    composeOverlay.visibility = View.GONE
                    advanceMode()
                },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showTimerOverlay(fontFamily: FontFamily, advanceMode: () -> Unit) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            TimerPickerOverlay(
                fontFamily = fontFamily,
                onSkip = {
                    composeOverlay.visibility = View.GONE
                    advanceMode()
                },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showCalendarOverlay(initialDate: LocalDate = LocalDate.now(), fontFamily: FontFamily) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            GlassCalendarOverlay(
                allTasks = allCalendarTasks,
                onUpdateTasks = { date, tasks ->
                    if (tasks.isEmpty()) allCalendarTasks.remove(date)
                    else allCalendarTasks[date] = tasks
                    FitDataRepository.saveCalendarTasks(allCalendarTasks.toMap())
                },
                fontFamily = fontFamily,
                initialDate = initialDate,
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showGlassOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            GlassDialogContent(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onEducationClick = {
                    showEducationOverlay(titleFont, buttonFont)
                },
                onHealthClick = {
                    showHealthOverlay(titleFont, buttonFont)
                },
                onSkillClick = {
                    showSkillOverlay(titleFont, buttonFont)
                },
                onPeaceClick = {
                    showPeaceOverlay(titleFont, buttonFont)
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showEducationOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            EducationOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onLectureClick = {
                    showLectureOverlay(titleFont, buttonFont)
                },
                onSubjectClick = {
                    showSubjectScreen(titleFont, buttonFont)
                },
                onCourseClick = {
                    showCourseScreen(titleFont, buttonFont)
                },
                onPracticeClick = {
                    showPracticeScreen(titleFont, buttonFont)
                },
                onBack = {
                    showGlassOverlay(titleFont, buttonFont)
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showSubjectScreen(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            LaunchedEffect(savedSubjects.toList()) {
                FitDataRepository.saveSubjects(savedSubjects.toList())
            }
            SubjectScreen(
                titleFont = titleFont,
                contentFont = buttonFont,
                savedSubjects = savedSubjects,
                onBack = { showEducationOverlay(titleFont, buttonFont) },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showCourseScreen(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            LaunchedEffect(savedCourses.toList()) {
                FitDataRepository.saveCourses(savedCourses.toList())
            }
            CourseScreen(
                titleFont = titleFont,
                contentFont = buttonFont,
                savedCourses = savedCourses,
                onBack = { showEducationOverlay(titleFont, buttonFont) },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showPracticeScreen(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            LaunchedEffect(savedPractices.toList()) {
                FitDataRepository.savePractices(savedPractices.toList())
            }
            PracticeScreen(
                titleFont = titleFont,
                contentFont = buttonFont,
                savedPractices = savedPractices,
                onBack = { showEducationOverlay(titleFont, buttonFont) },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showLectureOverlay(
        titleFont: FontFamily,
        buttonFont: FontFamily,
        onBack: () -> Unit = { showEducationOverlay(titleFont, buttonFont) }
    ) {
        composeOverlay.setContent {
            LectureScreen(
                titleFont        = titleFont,
                contentFont      = buttonFont,
                initialName      = activeLectureName.value ?: "",
                initialDurationMin = activeLectureDurationMin.value,
                initialTopics    = activeLectureTopics.toList(),
                onBack           = onBack,
                onClose          = { composeOverlay.visibility = View.GONE },
                onSaveAndBegin   = { name, totalMin, topics, progress ->
                    if (progress >= 1f) {
                        activeLectureName.value     = null
                        activeLectureProgress.value = 0f
                        activeLectureTopics.clear()
                        activeLectureDurationMin.value = 0
                        FitDataRepository.saveLectureState(LectureState())
                    } else {
                        val n = name.ifBlank { "LECTURE" }
                        activeLectureName.value     = n
                        activeLectureProgress.value = progress
                        activeLectureTopics.clear()
                        activeLectureTopics.addAll(topics)
                        activeLectureDurationMin.value = totalMin
                        FitDataRepository.saveLectureState(LectureState(n, progress, topics, totalMin))
                    }
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showHealthOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            HealthOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                junkCount = savedJunkCount.value,
                onJunkCountChange = { newCount ->
                    savedJunkCount.value = newCount
                    FitDataRepository.saveJunkEntry(LocalDate.now(), newCount)
                },
                onMedsClick = {
                    showMedsOverlay(titleFont, buttonFont)
                },
                onBack = {
                    showGlassOverlay(titleFont, buttonFont)
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showMedsOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            MedsOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onBack = {
                    showHealthOverlay(titleFont, buttonFont)
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showSkillOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            SkillOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onBack = {
                    showGlassOverlay(titleFont, buttonFont)
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showPeaceOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        composeOverlay.setContent {
            PeaceOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onHabitsClick = {
                    showHabitOverlay(titleFont, buttonFont)
                },
                onBack = {
                    showGlassOverlay(titleFont, buttonFont)
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showHabitOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        composeOverlay.setContent {
            HabitOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                savedHabits = savedHabits,
                onSave = { name ->
                    savedHabits.add(Habit(name = name))
                    FitDataRepository.saveHabits(savedHabits.toList())
                },
                onUpdateCount = { index, newCount ->
                    if (index in savedHabits.indices) {
                        savedHabits[index] = savedHabits[index].copy(count = newCount.coerceAtLeast(0))
                        FitDataRepository.saveHabits(savedHabits.toList())
                    }
                },
                onClose = {
                    showPeaceOverlay(titleFont, contentFont)
                }
            )
        }
    }

    private fun showCustomProfileOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            CustomProfileOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                savedProfiles = savedCustomProfiles,
                onSave = { name, duration, type ->
                    savedCustomProfiles.add(CustomProfile(name = name, duration = duration, type = type))
                    FitDataRepository.saveCustomProfiles(savedCustomProfiles.toList())
                },
                onDelete = { profile ->
                    savedCustomProfiles.remove(profile)
                    FitDataRepository.saveCustomProfiles(savedCustomProfiles.toList())
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showExportOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            ExportProgressOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun updateYearProgress(now: Calendar) {
        val year = now.get(Calendar.YEAR)
        val startOfYear = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfYear = Calendar.getInstance().apply {
            set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val totalMs = endOfYear.timeInMillis - startOfYear.timeInMillis
        val elapsedMs = now.timeInMillis - startOfYear.timeInMillis

        val progress = (elapsedMs.toDouble() / totalMs.toDouble() * 100)
        pbYearProgress.progress = progress.toInt()
        tvYearPercent.text = String.format(Locale.getDefault(), "YEAR PROGRESS: %.6f%%", progress)
    }

    override fun onDestroy() {
        FitDataRepository.recordSessionEnd(currentSessionId)
        timeHandler.removeCallbacks(updateTimeRunnable)
        super.onDestroy()
    }
}
