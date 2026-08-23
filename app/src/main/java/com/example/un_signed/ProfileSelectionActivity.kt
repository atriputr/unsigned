package com.example.un_signed

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

class ProfileSelectionActivity : AppCompatActivity() {

    private lateinit var tvClock: ComposeView
    private lateinit var pbYearProgress: ProgressBar
    private lateinit var tvYearPercent: TextView
    private lateinit var cvUpcomingEvents: ComposeView
    private lateinit var composeOverlay: ComposeView
    private lateinit var bgThemeTint: View
    private lateinit var cvHomeSkin: ComposeView
    private lateinit var cvStatusBar: ComposeView
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

    // User profile (loaded on init)
    private val userProfile = mutableStateOf(UserProfile())
    // App preferences (units, theme, haptics)
    private val appPrefs = mutableStateOf(AppPreferences())
    // Sleep session in progress (for quick-action button)
    private val sleepSession = mutableStateOf(SleepSessionState())
    // Water glasses today (for quick-action button)
    private val waterGlassesToday = mutableStateOf(0)

    // Update state
    private val pendingUpdate = mutableStateOf<UpdateInfo?>(null)
    private lateinit var updateManager: UpdateManager

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
            if (id != -1L) {
                // When download is done, show install prompt
                updateManager.installDownloadedApk(id)
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* silent: WeatherService already falls back to IP if denied */ }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()

            updateYearProgress(now)

            timeHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
        userProfile.value = FitDataRepository.loadUserProfile()
        appPrefs.value = FitDataRepository.loadAppPreferences()
        sleepSession.value = FitDataRepository.loadSleepSession()
        waterGlassesToday.value = FitDataRepository.loadWaterEntry(LocalDate.now())?.glassesConsumed ?: 0
        Haptics.enabled = appPrefs.value.hapticsEnabled

        tvClock = findViewById(R.id.tvNixieClock)
        pbYearProgress = findViewById(R.id.pbYearProgress)
        tvYearPercent = findViewById(R.id.tvYearPercent)
        cvUpcomingEvents = findViewById(R.id.cvUpcomingEvents)
        composeOverlay = findViewById(R.id.composeOverlay)
        bgThemeTint = findViewById(R.id.bgThemeTint)
        cvHomeSkin  = findViewById(R.id.cvHomeSkin)
        cvStatusBar = findViewById(R.id.cvStatusBar)
        applyThemeTint()

        // Check for updates
        updateManager = UpdateManager(this)
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(downloadReceiver, filter)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val info = updateManager.checkForUpdate()
            if (info != null) {
                pendingUpdate.value = info
                updateManager.showUpdateNotification(info)
                showUpdateOverlay(info, updateManager)
            }
        }

        val bebasFont = FontFamily(Font(R.font.bebas_neue))
        val jerseyFont = FontFamily(Font(R.font.jersey_10_charted_regular))

        tvClock.setContent {
            AppThemeProvider(appPrefs.value.theme, appPrefs.value.languageCode) {
                NixieClock(
                    fontFamily = jerseyFont,
                    onClick = {},
                    onTimerTap      = { advanceMode -> showTimerOverlay(jerseyFont, advanceMode) },
                    onStopwatchTap  = { advanceMode -> showStopwatchOverlay(jerseyFont, advanceMode) }
                )
            }
        }

        cvUpcomingEvents.setContent {
            AppThemeProvider(appPrefs.value.theme, appPrefs.value.languageCode) {
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

        // Back button: close overlay if open, else default (exit)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (composeOverlay.visibility == View.VISIBLE) {
                    composeOverlay.visibility = View.GONE
                    composeOverlay.setContent { }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // First-run onboarding: force profile setup before anything else
        if (!userProfile.value.setupComplete) {
            showProfileOverlay(bebasFont, bebasFont, isOnboarding = true)
        }

        // Silently request coarse location once so weather can be accurate (IP fallback otherwise)
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val permsToRequest = mutableListOf<String>()
        if (!granted) permsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permsToRequest.isNotEmpty()) {
            locationPermissionLauncher.launch(permsToRequest.toTypedArray())
        }
    }

    /** Applies the current theme tint + refreshes the home skin (used both on init and whenever quick-state changes). */
    private fun applyThemeTint() {
        val themeName = appPrefs.value.theme
        val palette   = AppPalettes.byName(themeName)
        tvYearPercent.setTextColor(palette.onSurface.toArgb())

        pbYearProgress.progressTintList = android.content.res.ColorStateList.valueOf(palette.accentPrimary.toArgb())
        pbYearProgress.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(palette.divider.toArgb())

        cvStatusBar.setContent {
            AppThemeProvider(themeName, appPrefs.value.languageCode) { StatusBarBox() }
        }

        // In every theme (including DARK) the Compose home-skin fully replaces the baked
        // bg_select_profile.png — so we always draw an opaque tint over it. This is what
        // lets us keep the text + icons as editable Kotlin strings instead of a PNG.
        bgThemeTint.setBackgroundColor(0xFF000000.toInt())

        val bebasFont = FontFamily(Font(R.font.bebas_neue))
        cvHomeSkin.visibility = View.VISIBLE
        cvHomeSkin.setContent {
            AppThemeProvider(themeName, appPrefs.value.languageCode) {
                // Compute derived state
                val waterEntry = FitDataRepository.loadWaterEntry(LocalDate.now())
                val goalMl = waterEntry?.goalMl ?: WaterGoal.compute(userProfile.value, FitDataRepository.loadWeatherCache().takeIf { it.isValid })
                val glassMl = waterEntry?.glassMl ?: 250
                val targetGlasses = ((goalMl + glassMl - 1) / glassMl).coerceAtLeast(1)

                HomeSkin(
                    themeName = themeName,
                    titleFont = bebasFont,
                    quickState = HomeQuickState(
                        sleepActive = sleepSession.value.active,
                        sleepDisturbances = sleepSession.value.disturbanceCount,
                        junkCountToday = savedJunkCount.value,
                        waterGlassesToday = waterGlassesToday.value,
                        waterTargetGlasses = targetGlasses
                    ),
                    quickCallbacks = HomeQuickCallbacks(
                        onSleepBegin     = { onSleepBegin() },
                        onSleepDisturbed = { onSleepDisturbed() },
                        onSleepEnd       = { onSleepEnd() },
                        onSleepManage    = { showSleepManageOverlay() },
                        onJunkIncrement    = { onJunkChange(+1) },
                        onJunkDecrement    = { onJunkChange(-1) },
                        onJunkOpenDetailed = { showJunkLogOverlay() },
                        onWaterIncrement   = { onWaterChange(+1) },
                        onWaterDecrement   = { onWaterChange(-1) },
                        onWaterReset       = { onWaterReset() }
                    )
                )
            }
        }
    }

    // ── Quick action handlers ────────────────────────────────────
    private fun onSleepBegin() {
        if (sleepSession.value.active) return
        Haptics.click(this)
        val newState = SleepSessionState(active = true, startedAtEpochMs = System.currentTimeMillis(), disturbanceCount = 0)
        sleepSession.value = newState
        FitDataRepository.saveSleepSession(newState)
        applyThemeTint()
    }

    private fun onSleepDisturbed() {
        val s = sleepSession.value
        if (!s.active) return
        Haptics.tick(this)
        val newState = s.copy(disturbanceCount = s.disturbanceCount + 1)
        sleepSession.value = newState
        FitDataRepository.saveSleepSession(newState)
        applyThemeTint()
    }

    private fun onSleepEnd() {
        val s = sleepSession.value
        if (!s.active) return
        Haptics.success(this)
        val endMs = System.currentTimeMillis()
        if (endMs > s.startedAtEpochMs) {
            val entry = SleepEntry(
                wakeDateIso = LocalDate.now().toString(),
                bedtimeMs = s.startedAtEpochMs,
                wakeMs = endMs,
                disturbances = s.disturbanceCount
            )
            val all = FitDataRepository.loadSleepEntries().filter { it.wakeDateIso != entry.wakeDateIso } + entry
            FitDataRepository.saveSleepEntries(all)
        }
        sleepSession.value = SleepSessionState()
        FitDataRepository.clearSleepSession()
        applyThemeTint()
    }

    private fun onJunkChange(delta: Int) {
        Haptics.tick(this)
        val newCount = (savedJunkCount.value + delta).coerceAtLeast(0)
        savedJunkCount.value = newCount
        FitDataRepository.saveJunkEntry(LocalDate.now(), newCount)
        applyThemeTint()
    }

    private fun showJunkLogOverlay() {
        Haptics.click(this)
        val bebasFont = FontFamily(Font(R.font.bebas_neue))
        setThemedContent {
            JunkLogOverlay(
                titleFont = bebasFont,
                contentFont = bebasFont,
                profile = userProfile.value,
                onSaved = { _ ->
                    // Also bump the simple daily junk counter so streaks/insights stay in sync
                    val newCount = savedJunkCount.value + 1
                    savedJunkCount.value = newCount
                    FitDataRepository.saveJunkEntry(LocalDate.now(), newCount)
                    Haptics.success(this@ProfileSelectionActivity)
                    applyThemeTint()
                },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun onWaterChange(delta: Int) {
        Haptics.tick(this)
        val today = LocalDate.now()
        val existing = FitDataRepository.loadWaterEntry(today)
        val goalMl = existing?.goalMl ?: WaterGoal.compute(userProfile.value, FitDataRepository.loadWeatherCache().takeIf { it.isValid })
        val glassMl = existing?.glassMl ?: 250
        val newGlasses = ((existing?.glassesConsumed ?: 0) + delta).coerceAtLeast(0)
        waterGlassesToday.value = newGlasses
        FitDataRepository.saveWaterEntry(
            today,
            WaterDailyLog(
                date = today.toString(),
                glassesConsumed = newGlasses,
                glassMl = glassMl,
                goalMl = goalMl,
                temperatureC = existing?.temperatureC
            )
        )
        applyThemeTint()
    }

    private fun onWaterReset() {
        Haptics.success(this)
        val today = LocalDate.now()
        waterGlassesToday.value = 0
        // Passing count=0 to saveWaterEntry causes it to remove the entry entirely (see repo impl)
        FitDataRepository.saveWaterEntry(today, WaterDailyLog(date = today.toString(), glassesConsumed = 0))
        applyThemeTint()
    }

    private fun showSleepManageOverlay() {
        val bebasFont = FontFamily(Font(R.font.bebas_neue))
        setThemedContent {
            SleepManageOverlay(
                titleFont = bebasFont,
                contentFont = bebasFont,
                activeSession = sleepSession.value,
                onDiscardActive = {
                    sleepSession.value = SleepSessionState()
                    FitDataRepository.clearSleepSession()
                    applyThemeTint()
                },
                onDeleteEntry = { entry ->
                    val remaining = FitDataRepository.loadSleepEntries().filter { it.id != entry.id }
                    FitDataRepository.saveSleepEntries(remaining)
                },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    /** Wrap any overlay content in the active theme so every child composable can read LocalPalette and LocalStrings. */
    private fun setThemedContent(makeVisible: Boolean = true, content: @Composable () -> Unit) {
        if (makeVisible) composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            AppThemeProvider(appPrefs.value.theme, appPrefs.value.languageCode) { content() }
        }
    }

    private fun showUpdateOverlay(info: UpdateInfo, updateManager: UpdateManager) {
        composeOverlay.visibility = View.VISIBLE
        composeOverlay.setContent {
            UpdateOverlay(
                info = info,
                onUpdate = {
                    updateManager.downloadUpdate(info)
                    composeOverlay.visibility = View.GONE
                },
                onClose = {
                    composeOverlay.visibility = View.GONE
                }
            )
        }
    }

    private fun showProfileOverlay(
        titleFont: FontFamily,
        contentFont: FontFamily,
        isOnboarding: Boolean = false
    ) {
        setThemedContent {
            UserProfileOverlay(
                    titleFont = titleFont,
                    contentFont = contentFont,
                    existing = userProfile.value,
                    prefs = appPrefs.value,
                    onPrefsChange = { p ->
                        appPrefs.value = p
                        Haptics.enabled = p.hapticsEnabled
                        FitDataRepository.saveAppPreferences(p)
                    },
                    isOnboarding = isOnboarding,
                    onSave = { p ->
                        userProfile.value = p
                        FitDataRepository.saveUserProfile(p)
                        composeOverlay.visibility = View.GONE
                    },
                    onClose = { if (!isOnboarding) composeOverlay.visibility = View.GONE }
                )
        }
    }

    private fun showWaterOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
            WaterOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                profile = userProfile.value,
                prefs = appPrefs.value,
                onClose = { showHealthOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showExerciseOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
            ExerciseOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                onClose = { showHealthOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showLogOverlay(category: String, titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
            LogEntryOverlay(
                category = category,
                titleFont = titleFont,
                contentFont = contentFont,
                onClose = { showMedsOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showSkillListOverlay(category: String, titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
            SkillListOverlay(
                category = category,
                titleFont = titleFont,
                contentFont = contentFont,
                onClose = { showSkillOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showActivityOverlay(activity: String, titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
            ActivityTrackerOverlay(
                activity = activity,
                titleFont = titleFont,
                contentFont = contentFont,
                prefs = appPrefs.value,
                onClose = { showPeaceOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showSleepOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
            SleepOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                profile = userProfile.value,
                onClose = { showPeaceOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showBriefingOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            DailyBriefingOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                profile = userProfile.value,
                prefs = appPrefs.value,
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showFocusOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            FocusOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showCompareOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            CompareToGlobeOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                profile = userProfile.value,
                prefs = appPrefs.value,
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showTipsOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            RecommendationsOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                profile = userProfile.value,
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showSettingsOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            SettingsOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                prefs = appPrefs.value,
                onPrefsChange = { p ->
                    val themeChanged = p.theme != appPrefs.value.theme
                    appPrefs.value = p
                    Haptics.enabled = p.hapticsEnabled
                    FitDataRepository.saveAppPreferences(p)
                    if (themeChanged) {
                        applyThemeTint()
                        showSettingsOverlay(titleFont, contentFont)
                    }
                },
                onEditProfile = { showProfileOverlay(titleFont, contentFont, isOnboarding = false) },
                onWeightLog   = { showWeightLogOverlay(titleFont, contentFont) },
                onCheckUpdate = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val info = updateManager.checkForUpdate()
                        if (info != null) {
                            pendingUpdate.value = info
                            showUpdateOverlay(info, updateManager)
                        } else {
                            Toast.makeText(this@ProfileSelectionActivity, "App is up to date", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShowUpdateLog = {
                    val info = pendingUpdate.value
                    if (info != null) {
                        showUpdateOverlay(info, updateManager)
                    } else {
                        Toast.makeText(this@ProfileSelectionActivity, "No update log available", Toast.LENGTH_SHORT).show()
                    }
                },
                onChangeLanguage = { showLanguagePicker(titleFont, contentFont) },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showLanguagePicker(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            LanguagePickerOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                onLanguageSelect = { code ->
                    val updated = appPrefs.value.copy(languageCode = code)
                    appPrefs.value = updated
                    FitDataRepository.saveAppPreferences(updated)
                    applyThemeTint()
                    // Re-open settings to show updated language
                    showSettingsOverlay(titleFont, contentFont)
                },
                onClose = { showSettingsOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showWeightLogOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent {
            WeightLogOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                profile = userProfile.value,
                prefs = appPrefs.value,
                onProfileWeightChange = { newKg ->
                    val updated = userProfile.value.copy(weightKg = newKg)
                    userProfile.value = updated
                    FitDataRepository.saveUserProfile(updated)
                },
                onClose = { showSettingsOverlay(titleFont, contentFont) }
            )
        }
    }

    private fun showStopwatchOverlay(fontFamily: FontFamily, advanceMode: () -> Unit) {
        setThemedContent {
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
        setThemedContent {
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
        setThemedContent {
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
        setThemedContent {
            GlassDialogContent(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onEducationClick = { showEducationOverlay(titleFont, buttonFont) },
                onHealthClick    = { showHealthOverlay(titleFont, buttonFont) },
                onSkillClick     = { showSkillOverlay(titleFont, buttonFont) },
                onPeaceClick     = { showPeaceOverlay(titleFont, buttonFont) },
                onFocusClick     = { showFocusOverlay(titleFont, buttonFont) },
                onSettingsClick  = { showSettingsOverlay(titleFont, buttonFont) },
                onBriefingClick  = { showBriefingOverlay(titleFont, buttonFont) },
                onCompareClick   = { showCompareOverlay(titleFont, buttonFont) },
                onProfileClick   = { showProfileOverlay(titleFont, buttonFont, isOnboarding = false) },
                onTipsClick      = { showTipsOverlay(titleFont, buttonFont) },
                onClose = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showEducationOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        setThemedContent(makeVisible = false) {
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
        setThemedContent(makeVisible = false) {
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
        setThemedContent(makeVisible = false) {
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
        setThemedContent(makeVisible = false) {
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
        setThemedContent(makeVisible = false) {
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
        setThemedContent {
            HealthOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                junkCount = savedJunkCount.value,
                onJunkCountChange = { newCount ->
                    savedJunkCount.value = newCount
                    FitDataRepository.saveJunkEntry(LocalDate.now(), newCount)
                },
                onWaterClick    = { showWaterOverlay(titleFont, buttonFont) },
                onExerciseClick = { showExerciseOverlay(titleFont, buttonFont) },
                onMedsClick     = { showMedsOverlay(titleFont, buttonFont) },
                onBack          = { showGlassOverlay(titleFont, buttonFont) },
                onClose         = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showMedsOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        setThemedContent {
            MedsOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onMedsLogClick     = { showLogOverlay("meds", titleFont, buttonFont) },
                onSupplimentsClick = { showLogOverlay("suppliments", titleFont, buttonFont) },
                onSevereClick      = { showLogOverlay("severe", titleFont, buttonFont) },
                onDietClick        = { showLogOverlay("diet", titleFont, buttonFont) },
                onBack             = { showHealthOverlay(titleFont, buttonFont) },
                onClose            = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showSkillOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        setThemedContent {
            SkillOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onHobbyClick = { showSkillListOverlay("hobby", titleFont, buttonFont) },
                onMinorClick = { showSkillListOverlay("minor", titleFont, buttonFont) },
                onMajorClick = { showSkillListOverlay("major", titleFont, buttonFont) },
                onBack       = { showGlassOverlay(titleFont, buttonFont) },
                onClose      = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showPeaceOverlay(titleFont: FontFamily, buttonFont: FontFamily) {
        setThemedContent {
            PeaceOptionsOverlay(
                titleFont = titleFont,
                buttonFont = buttonFont,
                onCyclingClick = { showActivityOverlay("cycling", titleFont, buttonFont) },
                onYogaClick    = { showActivityOverlay("yoga", titleFont, buttonFont) },
                onWalkingClick = { showActivityOverlay("walking", titleFont, buttonFont) },
                onSleepClick   = { showSleepOverlay(titleFont, buttonFont) },
                onHabitsClick  = { showHabitOverlay(titleFont, buttonFont) },
                onBack         = { showGlassOverlay(titleFont, buttonFont) },
                onClose        = { composeOverlay.visibility = View.GONE }
            )
        }
    }

    private fun showHabitOverlay(titleFont: FontFamily, contentFont: FontFamily) {
        setThemedContent(makeVisible = false) {
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
        setThemedContent {
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
        setThemedContent {
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
        val yearLabel = Localization.getStrings(appPrefs.value.languageCode).yearProgress
        tvYearPercent.text = String.format(Locale.getDefault(), "%s: %.6f%%", yearLabel, progress)
    }

    override fun onDestroy() {
        FitDataRepository.recordSessionEnd(currentSessionId)
        timeHandler.removeCallbacks(updateTimeRunnable)
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
