package com.example.un_signed

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.util.*

class ProfileSelectionActivity : AppCompatActivity() {

    private lateinit var tvClock: ComposeView
    private lateinit var pbYearProgress: ProgressBar
    private lateinit var tvYearPercent: TextView
    private lateinit var composeOverlay: ComposeView
    private val timeHandler = Handler(Looper.getMainLooper())

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
        composeOverlay = findViewById(R.id.composeOverlay)

        val nokiaFont = FontFamily(Font(R.font.nokia_kokia))
        val jerseyFont = FontFamily(Font(R.font.jersey_10_charted_regular))

        tvClock.setContent {
            NixieClock(fontFamily = jerseyFont, onClick = {})
        }

        findViewById<View>(R.id.btnIdealProfile).setOnClickListener {
            showGlassOverlay(nokiaFont, jerseyFont)
        }

        findViewById<View>(R.id.btnCustomProfile).setOnClickListener {
            // Custom Action
        }

        findViewById<View>(R.id.btnExportProgress).setOnClickListener {
            // Export Action
        }

        val onYearProgressClick = View.OnClickListener {
            showCalendarOverlay()
        }
        pbYearProgress.setOnClickListener(onYearProgressClick)
        tvYearPercent.setOnClickListener(onYearProgressClick)

        timeHandler.post(updateTimeRunnable)
    }

    private fun showCalendarOverlay() {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            GlassCalendarOverlay(onClose = { composeOverlay.visibility = View.GONE })
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
