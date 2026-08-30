package com.example.un_signed

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One comparison card row. */
data class Comparison(
    val metric: String,               // "BMI"
    val userValueDisplay: String,     // "22.4"
    val referenceDisplay: String,     // "24.8 (global mean)"
    val band: GlobalNorms.Band,
    val bandLabel: String,            // "Healthy"
    val percentile: Int? = null,      // 0-100
    val percentileLabel: String? = null,
    val insight: String,              // one-liner takeaway
    val source: String                // "WHO 1997 · NCD-RisC 2016"
)

/** Groups related metrics for display. */
data class ComparisonSection(
    val title: String,
    val accentHex: Long,              // 0xFF... for display color
    val comparisons: List<Comparison>
)

object ComparisonEngine {

    fun buildAll(profile: UserProfile, prefs: AppPreferences): List<ComparisonSection> {
        val today = LocalDate.now()
        val snap  = InsightsEngine.snapshot(profile, today)
        val weightHistory = FitDataRepository.loadWeightEntries().sortedByDescending { it.dateIso }
        val latestWeightKg = weightHistory.firstOrNull()?.weightKg ?: profile.weightKg

        val sections = mutableListOf<ComparisonSection>()

        // ── BODY ────────────────────────────────────────────────
        val body = mutableListOf<Comparison>()
        if (profile.bmi > 0) {
            val band = GlobalNorms.bmiBand(profile.bmi)
            val pct = GlobalNorms.bmiPercentile(profile.bmi)
            body += Comparison(
                metric = "BMI",
                userValueDisplay = "%.1f".format(profile.bmi),
                referenceDisplay = "healthy 18.5–24.9 · global μ 24.8",
                band = band.band,
                bandLabel = band.label,
                percentile = pct,
                percentileLabel = GlobalNorms.percentileLabel(pct),
                insight = when (band.band) {
                    GlobalNorms.Band.Healthy   -> "You sit inside the WHO healthy band."
                    GlobalNorms.Band.Concern   -> "Slightly outside the healthy band — worth a check-up."
                    GlobalNorms.Band.AtRisk    -> "Outside healthy — consider consulting a clinician."
                    else                       -> "—"
                },
                source = "WHO 1997 · NCD-RisC 2016"
            )
        }
        if (latestWeightKg > 0 && profile.heightCm > 0) {
            val heightM = profile.heightCm / 100.0
            val idealMinKg = 18.5 * heightM * heightM
            val idealMaxKg = 24.9 * heightM * heightM
            body += Comparison(
                metric = "Weight vs Height",
                userValueDisplay = Units.displayWeight(latestWeightKg, prefs.weightUnit),
                referenceDisplay = "ideal ${Units.displayWeight(idealMinKg, prefs.weightUnit)}–${Units.displayWeight(idealMaxKg, prefs.weightUnit)}",
                band = when {
                    latestWeightKg in idealMinKg..idealMaxKg -> GlobalNorms.Band.Healthy
                    latestWeightKg > idealMaxKg && latestWeightKg < idealMaxKg * 1.15 -> GlobalNorms.Band.Concern
                    latestWeightKg < idealMinKg && latestWeightKg > idealMinKg * 0.85 -> GlobalNorms.Band.Concern
                    else -> GlobalNorms.Band.AtRisk
                },
                bandLabel = if (latestWeightKg in idealMinKg..idealMaxKg) "Ideal" else if (latestWeightKg > idealMaxKg) "Above ideal" else "Below ideal",
                insight = if (latestWeightKg in idealMinKg..idealMaxKg)
                    "Weight matches ideal range for your height."
                else if (latestWeightKg > idealMaxKg)
                    "About ${Units.displayWeight(latestWeightKg - idealMaxKg, prefs.weightUnit)} above ideal high."
                else
                    "About ${Units.displayWeight(idealMinKg - latestWeightKg, prefs.weightUnit)} below ideal low.",
                source = "WHO BMI band × your height"
            )
        }
        if (profile.tdee > 0) {
            body += Comparison(
                metric = "Daily Energy (TDEE)",
                userValueDisplay = "%.0f kcal".format(profile.tdee),
                referenceDisplay = "adults 1 600–2 800 kcal typical",
                band = GlobalNorms.Band.Healthy,
                bandLabel = "Personalised",
                insight = "Mifflin-St Jeor × ${profile.activityLevel.lowercase()} activity.",
                source = "Mifflin-St Jeor 1990"
            )
        }
        body += lifeContext(profile)
        sections += ComparisonSection("BODY", 0xFFEBC174, body)

        // ── HYDRATION ──────────────────────────────────────────
        val hydration = mutableListOf<Comparison>()
        val waterHistory = FitDataRepository.loadWaterHistory()
        val today7 = (0..6).map {
            val d = today.minusDays(it.toLong())
            val e = waterHistory[d.toString()]
            (e?.let { it.glassesConsumed * it.glassMl } ?: 0)
        }
        val avgMl = today7.average().toInt()
        val target = GlobalNorms.waterTargetMl(profile)
        val waterBand = GlobalNorms.waterBand(avgMl, target)
        hydration += Comparison(
            metric = "7-day avg intake",
            userValueDisplay = Units.displayVolume(avgMl, prefs.volumeUnit),
            referenceDisplay = "your target " + Units.displayVolume(target, prefs.volumeUnit),
            band = waterBand.band,
            bandLabel = waterBand.label,
            insight = when (waterBand.band) {
                GlobalNorms.Band.Excellent, GlobalNorms.Band.Healthy -> "Consistent hydration — keep it up."
                GlobalNorms.Band.Concern -> "About ${(target - avgMl).coerceAtLeast(0) / 250} more glasses/day to hit target."
                GlobalNorms.Band.AtRisk  -> "Chronic under-hydration hurts focus, skin, and metabolism."
                else -> "—"
            },
            source = "EFSA 2010 · IOM 2004 · 35 mL/kg rule"
        )
        val globalDrinkTarget = GlobalNorms.waterGlobalDrinkTargetMl(profile.gender)
        hydration += Comparison(
            metric = "Global drink target",
            userValueDisplay = Units.displayVolume(avgMl, prefs.volumeUnit),
            referenceDisplay = "${if (profile.gender == "Female") "women" else "men"} " + Units.displayVolume(globalDrinkTarget, prefs.volumeUnit),
            band = if (avgMl >= globalDrinkTarget) GlobalNorms.Band.Healthy else GlobalNorms.Band.Concern,
            bandLabel = if (avgMl >= globalDrinkTarget) "Above global" else "Below global",
            insight = "EFSA/IOM ${if (profile.gender == "Female") "female" else "male"} adult target from drinks alone.",
            source = "EFSA 2010"
        )
        sections += ComparisonSection("HYDRATION", 0xFF4EA8DE, hydration)

        // ── MOVEMENT ───────────────────────────────────────────
        val movement = mutableListOf<Comparison>()
        val todaySteps = FitDataRepository.loadFitnessSamples().firstOrNull { it.dateIso == today.toString() }
        if (todaySteps != null && todaySteps.source != "unavailable") {
            val stepsBand = GlobalNorms.stepsBand(todaySteps.steps)
            movement += Comparison(
                metric = "Steps · today",
                userValueDisplay = "${todaySteps.steps}",
                referenceDisplay = "global μ ${GlobalNorms.stepsGlobalMean()} steps/day",
                band = stepsBand.band,
                bandLabel = stepsBand.label,
                insight = when (stepsBand.band) {
                    GlobalNorms.Band.Excellent -> "Highly active — well above the global average."
                    GlobalNorms.Band.Healthy   -> "Active — above the global daily average."
                    GlobalNorms.Band.Concern   -> "Below the global average — a short walk helps."
                    GlobalNorms.Band.AtRisk    -> "Low movement today — inactivity is a major mortality risk factor."
                    else -> "—"
                },
                source = if (todaySteps.source == "health_connect") "Health Connect · Althoff Nature 2017" else "Device step sensor · Althoff Nature 2017"
            )
        }
        val exBand = GlobalNorms.exerciseWeekBand(snap.exerciseWeekMin)
        movement += Comparison(
            metric = "Exercise · this week",
            userValueDisplay = "${snap.exerciseWeekMin} min",
            referenceDisplay = "WHO 150–300 min · global μ ${GlobalNorms.exerciseGlobalMean().toInt()} min",
            band = exBand.band,
            bandLabel = exBand.label,
            insight = when (exBand.band) {
                GlobalNorms.Band.Excellent -> "You're in the top tier — only ~15% of adults exceed this."
                GlobalNorms.Band.Healthy   -> "You meet WHO minimum — better than ${((1 - GlobalNorms.exerciseGlobalTargetMet()) * 100).toInt()}% of adults globally."
                GlobalNorms.Band.Concern   -> "${150 - snap.exerciseWeekMin} more minutes hits the WHO baseline."
                GlobalNorms.Band.AtRisk    -> "Chronic inactivity is a top-10 mortality risk factor (Lee 2012 Lancet)."
                else -> "—"
            },
            source = "WHO 2020 · Guthold Lancet 2018"
        )
        // Activity sessions in past week (from ActivityTracker)
        val actsAll = FitDataRepository.loadActivitySessions()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val actWeek = actsAll.filter {
            val d = try { LocalDate.parse(it.dateIso) } catch (_: Exception) { null }
            d != null && !d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6))
        }
        val cyclingKm = actWeek.filter { it.activity == "cycling" }.mapNotNull { it.distanceKm }.sum()
        val walkingKm = actWeek.filter { it.activity == "walking" }.mapNotNull { it.distanceKm }.sum()
        if (cyclingKm > 0.0 || walkingKm > 0.0) {
            val totalKm = cyclingKm + walkingKm
            movement += Comparison(
                metric = "Distance covered (wk)",
                userValueDisplay = Units.displayDistance(totalKm, prefs.distanceUnit),
                referenceDisplay = "avg adult moves " + Units.displayDistance(24.0, prefs.distanceUnit) + "/wk (all modes)",
                band = when {
                    totalKm >= 40.0 -> GlobalNorms.Band.Excellent
                    totalKm >= 20.0 -> GlobalNorms.Band.Healthy
                    totalKm >= 8.0  -> GlobalNorms.Band.Concern
                    else            -> GlobalNorms.Band.AtRisk
                },
                bandLabel = if (totalKm >= 20.0) "Above avg" else "Below avg",
                insight = "Distance sums walking + cycling logged in Un-signed.",
                source = "Althoff Nature 2017"
            )
        }
        sections += ComparisonSection("MOVEMENT", 0xFF8CD86A, movement)

        // ── SLEEP ──────────────────────────────────────────────
        val sleep = mutableListOf<Comparison>()
        val target2 = GlobalNorms.sleepTargetRange(profile.ageYears.coerceAtLeast(1))
        val sBand = GlobalNorms.sleepBand(snap.sleepLastHours, profile.ageYears)
        sleep += Comparison(
            metric = "Last night",
            userValueDisplay = "%.1f h".format(snap.sleepLastHours),
            referenceDisplay = "your age needs ${target2.minH.toInt()}–${target2.maxH.toInt()} h · global μ %.1f h".format(GlobalNorms.sleepGlobalMean()),
            band = sBand.band,
            bandLabel = sBand.label,
            insight = when (sBand.band) {
                GlobalNorms.Band.Healthy   -> "Right in the NSF band for your age."
                GlobalNorms.Band.Concern   -> "Not far off — one earlier night fixes it."
                GlobalNorms.Band.AtRisk    -> "Chronic short-sleep is linked to reduced cognition + immunity."
                else -> "—"
            },
            source = "National Sleep Foundation 2015"
        )
        // 7-night average
        val sleepAll = FitDataRepository.loadSleepEntries()
        val sleep7 = sleepAll
            .filter { it.wakeDateIso >= today.minusDays(6).toString() && it.wakeDateIso <= today.toString() }
            .map { it.durationHours }
        if (sleep7.isNotEmpty()) {
            val avg7 = sleep7.average()
            sleep += Comparison(
                metric = "7-night average",
                userValueDisplay = "%.1f h".format(avg7),
                referenceDisplay = "target %.1f h".format(profile.recommendedSleepHours),
                band = when {
                    avg7 >= profile.recommendedSleepHours - 0.5 -> GlobalNorms.Band.Healthy
                    avg7 >= profile.recommendedSleepHours - 1.5 -> GlobalNorms.Band.Concern
                    else -> GlobalNorms.Band.AtRisk
                },
                bandLabel = if (avg7 >= profile.recommendedSleepHours - 0.5) "Consistent" else "Deficit building",
                insight = if (avg7 < profile.recommendedSleepHours - 0.5)
                    "Cumulative deficit ≈ %.1f h/week.".format((profile.recommendedSleepHours - avg7) * 7)
                else "Solid consistency — protects hormones + memory.",
                source = "NSF 2015"
            )
        }
        sections += ComparisonSection("SLEEP", 0xFFB19CFF, sleep)

        // ── MIND / FOCUS ────────────────────────────────────────
        val mind = mutableListOf<Comparison>()
        val focusToday = FitDataRepository.loadFocusSessions().filter {
            Instant.ofEpochMilli(it.startedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }.sumOf { it.totalFocusMinutes }
        val fBand = GlobalNorms.focusDailyBand(focusToday)
        mind += Comparison(
            metric = "Focused work today",
            userValueDisplay = "$focusToday min",
            referenceDisplay = "elite ceiling ≈ 240 min/day",
            band = fBand.band,
            bandLabel = fBand.label,
            insight = when (fBand.band) {
                GlobalNorms.Band.Excellent -> "You're operating at expert-practitioner intensity."
                GlobalNorms.Band.Healthy   -> "Above most knowledge workers' daily focused-time."
                GlobalNorms.Band.Concern   -> "Even one 25-min pomodoro compounds over weeks."
                else -> "Start a single focus block today."
            },
            source = "Newport 2016 · Ericsson deliberate-practice research"
        )
        // Skill practice total
        val skillMin = FitDataRepository.loadSkillItems().sumOf { it.totalMinutes }
        if (skillMin > 0) {
            mind += Comparison(
                metric = "Total skill practice",
                userValueDisplay = if (skillMin >= 60) "%.1f h".format(skillMin / 60.0) else "$skillMin min",
                referenceDisplay = "expertise threshold ≈ 10 000 h (Ericsson)",
                band = when {
                    skillMin >= 6000 -> GlobalNorms.Band.Excellent
                    skillMin >= 500  -> GlobalNorms.Band.Healthy
                    skillMin >= 60   -> GlobalNorms.Band.Concern
                    else             -> GlobalNorms.Band.AtRisk
                },
                bandLabel = when {
                    skillMin >= 6000 -> "Near mastery"
                    skillMin >= 500  -> "On the path"
                    else             -> "Building"
                },
                insight = "Compounding beats sprinting — 30 min/day = 180 h/year.",
                source = "Ericsson 1993"
            )
        }
        sections += ComparisonSection("MIND / FOCUS", 0xFFFF8A00, mind)

        // ── DIET / HABITS ──────────────────────────────────────
        val diet = mutableListOf<Comparison>()
        val jBand = GlobalNorms.junkBand(snap.junkToday)
        diet += Comparison(
            metric = "Junk today",
            userValueDisplay = "${snap.junkToday}",
            referenceDisplay = "US adults derive ~57% kcal from ultra-processed",
            band = jBand.band,
            bandLabel = jBand.label,
            insight = when {
                snap.junkToday == 0 && snap.streakJunkClean >= 3 ->
                    "🔥 ${snap.streakJunkClean}-day clean streak — huge for metabolic health."
                snap.junkToday >= 3 -> "3+ items in a day correlates with worse next-day energy."
                else -> "Keeping under 2 items/day is a good practical bar."
            },
            source = "BMJ 2019 (Monteiro et al.) · NHANES"
        )
        val weekAvg = snap.junkWeekAvg
        if (weekAvg > 0) {
            diet += Comparison(
                metric = "Weekly junk avg",
                userValueDisplay = "%.1f/day".format(weekAvg),
                referenceDisplay = "world avg 1–4/day",
                band = if (weekAvg < 1.0) GlobalNorms.Band.Healthy else if (weekAvg < 2.5) GlobalNorms.Band.Concern else GlobalNorms.Band.AtRisk,
                bandLabel = if (weekAvg < 1.0) "Low" else if (weekAvg < 2.5) "Mid" else "High",
                insight = "Rolling 7-day mean.",
                source = "NCHS / global comparisons"
            )
        }
        sections += ComparisonSection("DIET", 0xFFFF9B44, diet)

        return sections
    }

    private fun lifeContext(profile: UserProfile): Comparison {
        val pct = GlobalNorms.lifePercentLived(profile.ageYears, profile.gender)
        val life = GlobalNorms.lifeExpectancy(profile.gender)
        return Comparison(
            metric = "Life % lived (est.)",
            userValueDisplay = "%.1f%%".format(pct),
            referenceDisplay = "global expectancy %.1f yrs · %s".format(life, profile.gender.ifBlank { "adult" }),
            band = GlobalNorms.Band.Healthy,
            bandLabel = "Perspective",
            insight = "Use it as fuel — every % is time you still own.",
            source = "WHO 2023"
        )
    }
}
