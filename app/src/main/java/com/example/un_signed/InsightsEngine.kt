package com.example.un_signed

import java.time.DayOfWeek
import java.time.LocalDate

/** Computed daily/weekly snapshot for the briefing overlay + export. */
data class HealthSnapshot(
    val date: LocalDate,
    val waterConsumedMl: Int,
    val waterGoalMl: Int,
    val exerciseTodayMin: Int,
    val exerciseWeekMin: Int,
    val sleepLastHours: Double,
    val sleepTargetHours: Double,
    val junkToday: Int,
    val junkWeekAvg: Double,
    val streakWater: Int,       // consecutive days meeting ≥ 80% water goal
    val streakExercise: Int,    // consecutive days ≥ 20 min exercise
    val streakSleep: Int,       // consecutive days sleep ≥ target-1hr
    val streakJunkClean: Int    // consecutive days junk == 0
) {
    val waterPercent: Float get() = if (waterGoalMl > 0) waterConsumedMl.toFloat() / waterGoalMl else 0f
    val exerciseWeekPercent: Float get() = (exerciseWeekMin.toFloat() / 150f).coerceIn(0f, 2f)
    val sleepPercent: Float get() = if (sleepTargetHours > 0) (sleepLastHours / sleepTargetHours).toFloat() else 0f
}

object InsightsEngine {

    fun snapshot(profile: UserProfile, today: LocalDate = LocalDate.now()): HealthSnapshot {
        // Water
        val waterHistory = FitDataRepository.loadWaterHistory()
        val todayWater = waterHistory[today.toString()]
        val waterConsumed = todayWater?.let { it.glassesConsumed * it.glassMl } ?: 0
        val waterGoal = todayWater?.goalMl ?: WaterGoal.compute(profile, null)

        // Exercise
        val exercise = FitDataRepository.loadExerciseEntries()
        val exerciseToday = exercise.filter { it.dateIso == today.toString() }.sumOf { it.minutes }
        val weekStart = today.with(DayOfWeek.MONDAY)
        val exerciseWeek = exercise
            .filter {
                val d = try { LocalDate.parse(it.dateIso) } catch (_: Exception) { null }
                d != null && !d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6))
            }
            .sumOf { it.minutes }

        // Sleep
        val sleep = FitDataRepository.loadSleepEntries().sortedByDescending { it.wakeDateIso }
        val lastSleep = sleep.firstOrNull()?.durationHours ?: 0.0

        // Junk
        val junkHistory = FitDataRepository.loadJunkHistory()
        val junkToday = junkHistory[today.toString()] ?: 0
        val junkWeek = (0..6).map { junkHistory[today.minusDays(it.toLong()).toString()] ?: 0 }
        val junkAvg = if (junkWeek.isNotEmpty()) junkWeek.average() else 0.0

        return HealthSnapshot(
            date = today,
            waterConsumedMl = waterConsumed,
            waterGoalMl = waterGoal,
            exerciseTodayMin = exerciseToday,
            exerciseWeekMin = exerciseWeek,
            sleepLastHours = lastSleep,
            sleepTargetHours = profile.recommendedSleepHours,
            junkToday = junkToday,
            junkWeekAvg = junkAvg,
            streakWater = streakOf(today) { d -> (waterHistory[d.toString()]?.let { it.glassesConsumed * it.glassMl } ?: 0) >= (waterHistory[d.toString()]?.goalMl?.let { (it * 0.8).toInt() } ?: Int.MAX_VALUE) },
            streakExercise = streakOf(today) { d -> exercise.filter { it.dateIso == d.toString() }.sumOf { it.minutes } >= 20 },
            streakSleep = streakOf(today) { d ->
                val s = sleep.firstOrNull { it.wakeDateIso == d.toString() } ?: return@streakOf false
                s.durationHours >= (profile.recommendedSleepHours - 1.0)
            },
            streakJunkClean = streakOf(today) { d -> (junkHistory[d.toString()] ?: 0) == 0 }
        )
    }

    /** Returns the number of consecutive days ending at `today` for which `predicate` is true. */
    private fun streakOf(today: LocalDate, predicate: (LocalDate) -> Boolean): Int {
        var count = 0
        var d = today
        while (predicate(d) && count < 365) { count += 1; d = d.minusDays(1) }
        // if today doesn't qualify, still allow the "yesterday-ending" streak
        if (count == 0 && predicate(today.minusDays(1))) {
            var yd = today.minusDays(1)
            while (predicate(yd) && count < 365) { count += 1; yd = yd.minusDays(1) }
        }
        return count
    }

    /** Human-readable nudge lines for the briefing overlay. */
    fun nudges(snapshot: HealthSnapshot, profile: UserProfile, weather: WeatherData): List<String> {
        val out = mutableListOf<String>()
        val name = profile.name.ifBlank { "there" }

        // Greeting
        val hourOfDay = java.time.LocalTime.now().hour
        val greet = when {
            hourOfDay < 12 -> "Good morning, $name."
            hourOfDay < 17 -> "Good afternoon, $name."
            else -> "Good evening, $name."
        }
        out += greet

        // Water
        val waterPct = snapshot.waterPercent
        if (waterPct >= 1f) {
            out += "Water goal hit — nice."
        } else if (waterPct >= 0.5f) {
            val remainingMl = snapshot.waterGoalMl - snapshot.waterConsumedMl
            out += "%.1fL to go on water today.".format(remainingMl / 1000.0)
        } else {
            out += "Only %d%% of water target so far — sip more.".format((waterPct * 100).toInt())
        }
        if (weather.isValid && weather.temperatureC > 30) {
            out += "It's %.0f°C outside — hydrate extra.".format(weather.temperatureC)
        }

        // Exercise
        val exW = snapshot.exerciseWeekMin
        when {
            exW >= 150 -> out += "You've hit 150 min this week — solid."
            exW >= 100 -> out += "${150 - exW} more exercise min this week to hit the WHO target."
            exW > 0 -> out += "Started the week — ${150 - exW} min left to hit target."
            else -> out += "No exercise logged this week yet."
        }

        // Sleep
        if (snapshot.sleepLastHours > 0) {
            val diff = snapshot.sleepLastHours - snapshot.sleepTargetHours
            when {
                diff >= -0.5 -> out += "Sleep last night was on target (%.1fh).".format(snapshot.sleepLastHours)
                diff >= -1.5 -> out += "Slightly under-slept last night (%.1fh vs %.1f target).".format(snapshot.sleepLastHours, snapshot.sleepTargetHours)
                else -> out += "Sleep debt building — %.1fh vs %.1f target.".format(snapshot.sleepLastHours, snapshot.sleepTargetHours)
            }
        }

        // Junk
        if (snapshot.junkToday >= 3) out += "${snapshot.junkToday} junk items today — try to slow down."
        else if (snapshot.streakJunkClean >= 3) out += "${snapshot.streakJunkClean}-day clean streak on junk — keep it up!"

        // Streaks
        if (snapshot.streakWater >= 3) out += "🔥 ${snapshot.streakWater}-day water streak."
        if (snapshot.streakExercise >= 3) out += "🔥 ${snapshot.streakExercise}-day exercise streak."
        if (snapshot.streakSleep >= 3) out += "🔥 ${snapshot.streakSleep}-day healthy sleep streak."

        return out
    }
}
