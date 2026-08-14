package com.example.un_signed

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Curated evidence-based benchmarks for the "Compare Me To Globe" feature.
 * All figures reference publicly available research or standards bodies.
 *
 * Everything here is deterministic + offline — no API calls, no cost, no privacy leaks.
 * When distributions are needed we approximate with normals fitted to published mean/SD.
 *
 * Sources cited inline in each function; consolidated list at the bottom.
 */
object GlobalNorms {

    // ── Bands ──────────────────────────────────────────────────
    enum class Band { Excellent, Healthy, Concern, AtRisk, Unknown }

    data class BandInfo(val band: Band, val label: String)

    // ── BMI (WHO 1997/2000) ────────────────────────────────────
    // Cut-offs: <18.5 underweight | 18.5-24.9 normal | 25-29.9 overweight | ≥30 obese
    // Global adult mean BMI ≈ 24.8, SD ≈ 4.8 (NCD-RisC pooled, 2016 Lancet)
    fun bmiBand(bmi: Double): BandInfo = when {
        bmi <= 0 -> BandInfo(Band.Unknown, "—")
        bmi < 16.0 -> BandInfo(Band.AtRisk, "Severely underweight")
        bmi < 18.5 -> BandInfo(Band.Concern, "Underweight")
        bmi < 25.0 -> BandInfo(Band.Healthy, "Healthy")
        bmi < 30.0 -> BandInfo(Band.Concern, "Overweight")
        bmi < 35.0 -> BandInfo(Band.AtRisk, "Obese I")
        bmi < 40.0 -> BandInfo(Band.AtRisk, "Obese II")
        else       -> BandInfo(Band.AtRisk, "Obese III")
    }

    /** Approximate global percentile for adult BMI. Uses μ=24.8, σ=4.8 (NCD-RisC 2016). */
    fun bmiPercentile(bmi: Double): Int = percentile(bmi, mean = 24.8, sd = 4.8)

    // ── Sleep — NSF 2015 by age ────────────────────────────────
    data class SleepRange(val minH: Double, val maxH: Double)
    fun sleepTargetRange(ageYears: Int): SleepRange = when {
        ageYears < 3    -> SleepRange(14.0, 17.0)
        ageYears < 6    -> SleepRange(10.0, 13.0)
        ageYears < 14   -> SleepRange(9.0, 11.0)
        ageYears < 18   -> SleepRange(8.0, 10.0)
        ageYears < 26   -> SleepRange(7.0, 9.0)
        ageYears < 65   -> SleepRange(7.0, 9.0)
        else            -> SleepRange(7.0, 8.0)
    }
    /** Global adult mean sleep ≈ 6.8h (Sleep Cycle 2019 aggregate over 941k users, 50 countries). */
    fun sleepGlobalMean(): Double = 6.8

    fun sleepBand(hours: Double, ageYears: Int): BandInfo {
        val r = sleepTargetRange(ageYears)
        return when {
            hours <= 0.0 -> BandInfo(Band.Unknown, "—")
            hours in r.minH..r.maxH -> BandInfo(Band.Healthy, "On target")
            hours > r.maxH + 1.0 -> BandInfo(Band.Concern, "Over-slept")
            hours > r.maxH -> BandInfo(Band.Healthy, "Slightly high")
            hours >= r.minH - 1.0 -> BandInfo(Band.Concern, "Slightly short")
            hours >= r.minH - 2.0 -> BandInfo(Band.AtRisk, "Short sleep")
            else -> BandInfo(Band.AtRisk, "Chronic deficit")
        }
    }

    // ── Physical activity — WHO 2020 ───────────────────────────
    // Adults 18-64: 150-300 min moderate (or 75-150 vigorous) per week + 2 strength sessions
    // 27.5% of adults globally meet minimum (WHO Lancet 2018, Guthold et al.)
    fun exerciseWeekBand(minutes: Int): BandInfo = when {
        minutes < 0 -> BandInfo(Band.Unknown, "—")
        minutes >= 300 -> BandInfo(Band.Excellent, "Highly active")
        minutes >= 150 -> BandInfo(Band.Healthy, "Meets WHO target")
        minutes >= 75  -> BandInfo(Band.Concern, "Below target")
        minutes >  0   -> BandInfo(Band.AtRisk, "Minimal activity")
        else           -> BandInfo(Band.AtRisk, "Sedentary")
    }
    fun exerciseGlobalMean(): Double = 108.0   // rough weekly mean (WHO)
    fun exerciseGlobalTargetMet(): Double = 0.275  // 27.5% adults hit target

    // ── Water intake (EFSA 2010 + IOM 2004) ───────────────────
    // Total water (all sources): adult men 3.7L/day, women 2.7L/day (~20% from food)
    // Water from drinks: men ~2.5L, women ~2.0L
    // Medical rule of thumb: 35 mL per kg body weight
    fun waterTargetMl(profile: UserProfile): Int {
        val baseline = profile.baseWaterMl.coerceAtLeast(1500)
        return baseline
    }
    fun waterGlobalDrinkTargetMl(gender: String): Int =
        if (gender.equals("Female", ignoreCase = true)) 2000 else 2500

    fun waterBand(consumedMl: Int, targetMl: Int): BandInfo = when {
        targetMl <= 0 -> BandInfo(Band.Unknown, "—")
        consumedMl >= targetMl -> BandInfo(Band.Excellent, "Well hydrated")
        consumedMl >= targetMl * 0.80 -> BandInfo(Band.Healthy, "On track")
        consumedMl >= targetMl * 0.55 -> BandInfo(Band.Concern, "Under-hydrated")
        else -> BandInfo(Band.AtRisk, "Chronically low")
    }

    // ── Steps — modern research bands ──────────────────────────
    // 5k = sedentary threshold, 7.5k = strong health signal (Paluch 2022 Lancet meta),
    // 10k = classic aspirational, ≥12k = elite active
    // Global 111-country mean ≈ 4,961 steps/day (Althoff et al. 2017 Nature)
    fun stepsBand(steps: Int): BandInfo = when {
        steps < 0 -> BandInfo(Band.Unknown, "—")
        steps >= 10000 -> BandInfo(Band.Excellent, "Highly active")
        steps >= 7500  -> BandInfo(Band.Healthy, "Active")
        steps >= 5000  -> BandInfo(Band.Concern, "Low active")
        else           -> BandInfo(Band.AtRisk, "Sedentary")
    }
    fun stepsGlobalMean(): Int = 4961

    // ── Screen time — DataReportal Digital 2024 ────────────────
    // Global avg total screen: 6h 40m/day (400 min).  Social media alone 2h 24m.
    fun screenBand(minutes: Int): BandInfo = when {
        minutes < 0 -> BandInfo(Band.Unknown, "—")
        minutes < 120 -> BandInfo(Band.Excellent, "Very intentional")
        minutes < 240 -> BandInfo(Band.Healthy, "Below global avg")
        minutes < 400 -> BandInfo(Band.Concern, "Near global avg")
        minutes < 600 -> BandInfo(Band.AtRisk, "Above avg")
        else          -> BandInfo(Band.AtRisk, "Very high")
    }
    fun screenGlobalMeanMin(): Int = 400

    // ── Reading (books/year) — Pew Research 2019 ───────────────
    // US adult mean = 12/yr, median = 4/yr, 24% read 0 in past year
    fun readingBand(booksPerYear: Int): BandInfo = when {
        booksPerYear < 0 -> BandInfo(Band.Unknown, "—")
        booksPerYear >= 24 -> BandInfo(Band.Excellent, "Voracious reader")
        booksPerYear >= 12 -> BandInfo(Band.Healthy, "Above global avg")
        booksPerYear >= 4  -> BandInfo(Band.Concern, "Global median")
        booksPerYear >= 1  -> BandInfo(Band.Concern, "Below average")
        else -> BandInfo(Band.AtRisk, "Not reading")
    }

    // ── Deep-work / focus hours per day (Newport / Ericsson) ──
    // Deliberate practice ceiling ~4 hrs/day. Most knowledge workers <2 hrs of true focus.
    fun focusDailyBand(minutes: Int): BandInfo = when {
        minutes < 0 -> BandInfo(Band.Unknown, "—")
        minutes >= 180 -> BandInfo(Band.Excellent, "Elite focus")
        minutes >= 90  -> BandInfo(Band.Healthy, "Strong focus")
        minutes >= 25  -> BandInfo(Band.Concern, "Some focus")
        else -> BandInfo(Band.AtRisk, "Minimal deep work")
    }

    // ── Junk / ultra-processed food (per day) — NHANES + BMJ 2019 ──
    // US adults derive ~57% daily calories from ultra-processed foods (BMJ 2019)
    // World avg junk-food servings/day varies 1-4
    fun junkBand(count: Int): BandInfo = when {
        count <= 0 -> BandInfo(Band.Excellent, "No junk today")
        count == 1 -> BandInfo(Band.Healthy, "Occasional treat")
        count == 2 -> BandInfo(Band.Concern, "Getting frequent")
        count in 3..4 -> BandInfo(Band.AtRisk, "Heavy")
        else -> BandInfo(Band.AtRisk, "Very high")
    }

    // ── Sitting / sedentary time (hrs/day) — WHO 2020 ──────────
    // >8 hrs/day sitting → elevated all-cause mortality; global avg ≈ 6 hrs.
    fun sittingBand(hours: Double): BandInfo = when {
        hours < 0.0 -> BandInfo(Band.Unknown, "—")
        hours < 4.0 -> BandInfo(Band.Excellent, "Very active")
        hours < 6.0 -> BandInfo(Band.Healthy, "Global avg")
        hours < 8.0 -> BandInfo(Band.Concern, "Above avg")
        else -> BandInfo(Band.AtRisk, "Sedentary risk")
    }

    // ── Life-expectancy context (WHO 2023) — used for "years lived" perspective ──
    // Global mean 73.4 yrs (male 70.8, female 76.0)
    fun lifeExpectancy(gender: String): Double =
        if (gender.equals("Female", ignoreCase = true)) 76.0 else 70.8

    fun lifePercentLived(ageYears: Int, gender: String): Double {
        val expected = lifeExpectancy(gender)
        return if (expected > 0) (ageYears.toDouble() / expected * 100.0).coerceIn(0.0, 100.0) else 0.0
    }

    // ── Utility ────────────────────────────────────────────────
    /** Standard-normal CDF for percentile-lookups. */
    private fun phi(z: Double): Double {
        // Abramowitz–Stegun 7.1.26 approximation for erf
        val sign = if (z < 0) -1 else 1
        val x = kotlin.math.abs(z) / sqrt(2.0)
        val t = 1.0 / (1.0 + 0.3275911 * x)
        val a1 = 0.254829592; val a2 = -0.284496736; val a3 = 1.421413741
        val a4 = -1.453152027; val a5 = 1.061405429
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-x * x)
        return 0.5 * (1.0 + sign * y)
    }
    fun percentile(value: Double, mean: Double, sd: Double): Int =
        (phi((value - mean) / sd) * 100.0).toInt().coerceIn(0, 100)

    /** Human-readable percentile suffix. */
    fun percentileLabel(p: Int): String = when {
        p >= 95 -> "top 5%"
        p >= 85 -> "top 15%"
        p >= 65 -> "above avg"
        p in 35..64 -> "near avg"
        p in 15..34 -> "below avg"
        else -> "bottom 15%"
    }

    // ── Consolidated source list (used by comparison overlay footer) ─
    val sources: List<String> = listOf(
        "WHO Global BMI Ranges (1997/2000)",
        "NCD-RisC · Lancet 2016 · pooled BMI trends",
        "National Sleep Foundation · Sleep Duration Recommendations (2015)",
        "Sleep Cycle Corp · 2019 aggregate of 941 k users, 50 countries",
        "WHO Physical Activity Guidelines (2020)",
        "Guthold et al · Lancet Global Health 2018 · activity in 168 countries",
        "EFSA · Dietary reference values for water (2010)",
        "IOM · Dietary Reference Intakes for Water (2004)",
        "Althoff et al · Nature 2017 · step counts in 111 countries",
        "Paluch et al · Lancet 2022 · daily steps and mortality meta-analysis",
        "DataReportal · Digital 2024 Global Overview Report",
        "Pew Research Center · 2019 US reading habits",
        "Newport, C · Deep Work (2016)",
        "Ericsson · deliberate practice ceiling research",
        "Monteiro et al · BMJ 2019 · ultra-processed foods US NHANES",
        "WHO · World Health Statistics 2023 · life expectancy"
    )
}
