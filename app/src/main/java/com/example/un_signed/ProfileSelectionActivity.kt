package com.example.un_signed

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
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

        tvClock = findViewById(R.id.tvNixieClock)
        pbYearProgress = findViewById(R.id.pbYearProgress)
        tvYearPercent = findViewById(R.id.tvYearPercent)
        cvUpcomingEvents = findViewById(R.id.cvUpcomingEvents)
        composeOverlay = findViewById(R.id.composeOverlay)

        val nokiaFont = FontFamily(Font(R.font.nokia_kokia))
        val jerseyFont = FontFamily(Font(R.font.jersey_10_charted_regular))
        val bebasFont = FontFamily(Font(R.font.bebas_neue))

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
                allTasks = allCalendarTasks,
                fontFamily = jerseyFont,
                onEventClick = { date -> showCalendarOverlay(date) }
            )
        }

        findViewById<View>(R.id.btnIdealProfile).setOnClickListener {
            showGlassOverlay(nokiaFont, jerseyFont)
        }

        findViewById<View>(R.id.btnCustomProfile).setOnClickListener {
            showCustomProfileOverlay(nokiaFont, bebasFont)
        }

        findViewById<View>(R.id.btnExportProgress).setOnClickListener {
            showExportOverlay(nokiaFont, jerseyFont)
        }

        val onYearProgressClick = View.OnClickListener {
            showCalendarOverlay()
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

    private fun showCalendarOverlay(initialDate: LocalDate = LocalDate.now()) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            GlassCalendarOverlay(
                allTasks = allCalendarTasks,
                onUpdateTasks = { date, tasks ->
                    if (tasks.isEmpty()) allCalendarTasks.remove(date)
                    else allCalendarTasks[date] = tasks
                },
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
                onClose = {
                    composeOverlay.visibility = View.GONE
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
        timeHandler.removeCallbacks(updateTimeRunnable)
        super.onDestroy()
    }
}
