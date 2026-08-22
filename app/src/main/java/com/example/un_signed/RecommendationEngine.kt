package com.example.un_signed

import java.time.LocalDate

data class Recommendation(
    val category: String,          // FOOD | HABIT | LIFESTYLE | WARNING
    val priority: Priority,        // High = red, Medium = amber, Low = blue
    val title: String,
    val why: String,               // 1-line reasoning based on user data
    val action: String,            // concrete action to take
    val evidence: String           // source citation
) {
    enum class Priority { High, Medium, Low }
}

/**
 * Analyses the user's routine + profile and produces personalised food & habit picks.
 * Rule-based, evidence-cited — no LLM, purely local.
 */
object RecommendationEngine {

    fun analyse(profile: UserProfile): List<Recommendation> {
        val today = LocalDate.now()
        val snap = InsightsEngine.snapshot(profile, today)
        val out = mutableListOf<Recommendation>()

        // ── HYDRATION ────────────────────────────────────────────
        val waterPct = snap.waterPercent
        when {
            waterPct < 0.5f -> out += Recommendation(
                "HABIT", Recommendation.Priority.High,
                "Set 3 water anchors",
                "You're at %d%% of your %.1fL target — chronic under-hydration hits focus + kidneys.".format((waterPct * 100).toInt(), snap.waterGoalMl / 1000.0),
                "Drink 1 glass on wake · 1 before every meal · 1 mid-afternoon (5 glasses baseline).",
                "EFSA 2010 · 35 mL/kg rule"
            )
            waterPct in 0.5f..0.8f -> out += Recommendation(
                "HABIT", Recommendation.Priority.Medium,
                "One more glass",
                "You're at %d%% — one more glass every 2 hrs closes the gap.".format((waterPct * 100).toInt()),
                "Keep a 500ml bottle at your desk and refill twice.",
                "IOM 2004"
            )
        }

        // ── SLEEP ────────────────────────────────────────────────
        if (snap.sleepLastHours in 0.1..(profile.recommendedSleepHours - 1.5)) {
            out += Recommendation(
                "HABIT", Recommendation.Priority.High,
                "Advance bedtime by 30 min",
                "Last night was %.1fh vs %.1fh target — sleep debt hits cognition + immunity.".format(snap.sleepLastHours, profile.recommendedSleepHours),
                "Set a phone-away alarm 45 min before your target bedtime tonight.",
                "NSF 2015 · Walker 2017"
            )
        } else if (snap.sleepLastHours == 0.0) {
            out += Recommendation(
                "HABIT", Recommendation.Priority.Medium,
                "Log tonight's sleep",
                "No sleep entry yet — you can't improve what you don't measure.",
                "Use the SLEEP quick-button — hold 3s to begin, double-tap on wake.",
                "self-monitoring literature"
            )
        }

        // ── EXERCISE ────────────────────────────────────────────
        val weekMin = snap.exerciseWeekMin
        when {
            weekMin < 60 -> out += Recommendation(
                "HABIT", Recommendation.Priority.High,
                "Start a 10-min daily walk",
                "$weekMin min this week — inactivity is a top-10 mortality risk.",
                "Right after lunch, walk 10 min at a brisk pace. Build to 30 min.",
                "WHO 2020 · Lee Lancet 2012"
            )
            weekMin in 60..149 -> out += Recommendation(
                "HABIT", Recommendation.Priority.Medium,
                "${150 - weekMin} more min this week",
                "You're close to WHO's 150 min baseline for adults.",
                "Add two 20-min brisk walks + one 30-min activity to hit target.",
                "WHO 2020"
            )
        }

        // ── FOOD (junk pattern-based) ──────────────────────────
        val junkLog = FitDataRepository.loadJunkLogEntries()
        val recentJunk = junkLog.filter { it.dateIso >= today.minusDays(7).toString() }

        // Sugar-heavy pattern
        val avgSugar = if (recentJunk.isNotEmpty()) recentJunk.sumOf { it.sugarG } / recentJunk.size else 0.0
        if (avgSugar > 15.0) {
            out += Recommendation(
                "FOOD", Recommendation.Priority.High,
                "Swap sugary items for whole fruit",
                "Your logged junk averages %.0fg sugar/serving — high glycemic spikes.".format(avgSugar),
                "Try an apple + peanut butter or dates in place of candy/soda cravings.",
                "Ludwig 2018 · low-glycemic swaps"
            )
        }

        // NOVA-4 heavy
        val nova4 = recentJunk.count { it.novaGroup == 4 }
        if (nova4 >= 5) {
            out += Recommendation(
                "FOOD", Recommendation.Priority.High,
                "Reduce ultra-processed intake",
                "$nova4 ultra-processed items in the last week — linked to metabolic syndrome.",
                "Cook one home-made snack this week: roasted chickpeas, veg poha, oats-yogurt bowl.",
                "BMJ 2019 · Monteiro NOVA-4"
            )
        }

        // Fibre suggestion (if junk-heavy days)
        if (snap.junkWeekAvg >= 1.5) {
            out += Recommendation(
                "FOOD", Recommendation.Priority.Medium,
                "Add 25g fibre daily",
                "You average %.1f junk items/day — fibre offsets sugar spike + feeds gut.".format(snap.junkWeekAvg),
                "Start each day with 1 bowl of oats + banana + chia (≈ 10g fibre).",
                "USDA + WHO fibre guidelines"
            )
        }

        // ── BODY-SPECIFIC (BMI-based) ──────────────────────────
        val bmi = profile.bmi
        when {
            bmi >= 30.0 -> out += Recommendation(
                "LIFESTYLE", Recommendation.Priority.High,
                "Focus on caloric deficit + strength",
                "BMI %.1f is in obese range — muscle-preserving weight loss reduces disease risk.".format(bmi),
                "Target 500 kcal/day deficit (%.0f → %.0f kcal) + 2× strength sessions/week.".format(profile.tdee, profile.tdee - 500),
                "AHA/ACC 2013 · obesity guideline"
            )
            bmi in 25.0..29.99 -> out += Recommendation(
                "LIFESTYLE", Recommendation.Priority.Medium,
                "Nudge weight back to healthy range",
                "BMI %.1f is overweight — small tweaks work best long-term.".format(bmi),
                "300 kcal deficit + 30 min walk daily → ~2-3 kg loss over 6-8 weeks.",
                "NIH weight management"
            )
            bmi in 18.5..24.99 && bmi > 0 -> out += Recommendation(
                "LIFESTYLE", Recommendation.Priority.Low,
                "Maintain — you're in the healthy band",
                "BMI %.1f is optimal for most adults.".format(bmi),
                "Focus on strength, sleep, and stress management rather than weight.",
                "WHO 1997 BMI classification"
            )
            bmi in 0.01..18.49 -> out += Recommendation(
                "LIFESTYLE", Recommendation.Priority.Medium,
                "Add nutrient-dense calories",
                "BMI %.1f is under 18.5 — low body weight can affect immunity + bone density.".format(bmi),
                "Add 300 kcal via nuts, ghee, paneer, whole-fat yogurt.",
                "WHO underweight"
            )
        }

        // ── STREAK REINFORCEMENT (positive nudges) ─────────────
        if (snap.streakWater >= 3) out += Recommendation(
            "HABIT", Recommendation.Priority.Low,
            "🔥 Water streak: ${snap.streakWater} days",
            "Don't break the chain — consistency compounds.",
            "Same routine tomorrow: 3 water anchors before noon.",
            "habit formation research"
        )
        if (snap.streakJunkClean >= 3) out += Recommendation(
            "HABIT", Recommendation.Priority.Low,
            "🔥 ${snap.streakJunkClean}-day clean streak",
            "Metabolic health improves noticeably after 7 clean days.",
            "One more clean day — you're building genuine momentum.",
            "Monteiro BMJ 2019"
        )

        // ── AGE-SPECIFIC HINTS ─────────────────────────────────
        when {
            profile.ageYears in 40..59 -> out += Recommendation(
                "LIFESTYLE", Recommendation.Priority.Medium,
                "Add resistance training",
                "You're in the 40-59 band — muscle mass declines ~1%/yr without it.",
                "2 strength sessions/week (bodyweight + resistance bands) prevents sarcopenia.",
                "ACSM position statement"
            )
            profile.ageYears >= 60 -> out += Recommendation(
                "LIFESTYLE", Recommendation.Priority.High,
                "Balance + protein focus",
                "60+ requires higher protein (1.2 g/kg) and daily balance work.",
                "Add legumes/eggs to every meal + 5 min balance drills each morning.",
                "PROT-AGE study group"
            )
        }

        return out.sortedByDescending {
            when (it.priority) {
                Recommendation.Priority.High -> 3
                Recommendation.Priority.Medium -> 2
                Recommendation.Priority.Low -> 1
            }
        }
    }
}
