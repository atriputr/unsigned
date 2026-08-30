package com.example.un_signed

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// ── User profile ──────────────────────────────────────────────
data class UserProfile(
    val name: String = "",
    val ageYears: Int = 0,
    val gender: String = "",              // "Male" | "Female" | "Other"
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val activityLevel: String = "Moderate", // Sedentary | Light | Moderate | Active | VeryActive
    val setupComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val emails: List<String> = emptyList()   // gmail / tutamail / apple id etc. (local identifiers)
) {
    val bmi: Double
        get() = if (heightCm > 0) {
            val m = heightCm / 100.0
            weightKg / (m * m)
        } else 0.0

    val bmiCategory: String
        get() = when {
            bmi <= 0 -> "—"
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }

    // Mifflin-St Jeor Basal Metabolic Rate
    val bmr: Double
        get() {
            if (weightKg <= 0 || heightCm <= 0 || ageYears <= 0) return 0.0
            val base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
            return if (gender.equals("Male", ignoreCase = true)) base + 5 else base - 161
        }

    val tdee: Double
        get() {
            val factor = when (activityLevel) {
                "Sedentary" -> 1.2
                "Light" -> 1.375
                "Moderate" -> 1.55
                "Active" -> 1.725
                "VeryActive" -> 1.9
                else -> 1.5
            }
            return bmr * factor
        }

    val recommendedSleepHours: Double
        get() = when {
            ageYears <= 0 -> 8.0
            ageYears < 18 -> 9.0
            ageYears < 65 -> 8.0
            else -> 7.5
        }

    // Baseline water in ml — 35ml/kg (medical standard)
    val baseWaterMl: Int
        get() = (weightKg * 35).toInt()
}

// ── Weather (cached in FitDataRepository) ─────────────────────
data class WeatherData(
    val temperatureC: Double = Double.NaN,
    val condition: String = "",
    val locationName: String = "",
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
    val fetchedAt: Long = 0L
) {
    val isValid: Boolean get() = !temperatureC.isNaN() && fetchedAt > 0
    fun isStale(ttlMs: Long = 3600_000L): Boolean =
        !isValid || System.currentTimeMillis() - fetchedAt > ttlMs
}

// ── Water tracking ────────────────────────────────────────────
data class WaterDailyLog(
    val date: String = "",
    val glassesConsumed: Int = 0,
    val glassMl: Int = 250,
    val goalMl: Int = 2000,
    val temperatureC: Double? = null
) {
    val consumedMl: Int get() = glassesConsumed * glassMl
    val percent: Float get() = if (goalMl > 0) (consumedMl.toFloat() / goalMl).coerceIn(0f, 2f) else 0f
    val targetGlasses: Int get() = ((goalMl + glassMl - 1) / glassMl).coerceAtLeast(1)
}

// ── Exercise ──────────────────────────────────────────────────
data class ExerciseEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateIso: String = LocalDate.now().toString(),
    val minutes: Int = 0,
    val activity: String = "General",
    val intensity: String = "Moderate",     // Light | Moderate | Vigorous
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ── Sleep ─────────────────────────────────────────────────────
data class SleepEntry(
    val id: String = UUID.randomUUID().toString(),
    val wakeDateIso: String = LocalDate.now().toString(),
    val bedtimeMs: Long = 0L,
    val wakeMs: Long = 0L,
    val quality: Int = 3,                    // 1-5
    val disturbances: Int = 0,               // how many times sleep was interrupted
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val durationMs: Long get() = (wakeMs - bedtimeMs).coerceAtLeast(0)
    val durationHours: Double get() = durationMs / 3_600_000.0
}

// ── Generic log entry (Meds / Suppliments / Severe / Diet) ────
data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",               // meds | suppliments | severe | diet
    val subcategory: String = "",            // meal type, drug class, symptom name
    val note: String = "",
    val severity: Int = 0,                   // used by "severe" 0-10
    val timestamp: Long = System.currentTimeMillis()
) {
    val dateIso: String
        get() = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
}

// ── Skill (Hobby / Minor / Major) ─────────────────────────────
data class SkillItem(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",               // hobby | minor | major
    val name: String = "",
    val totalMinutes: Int = 0,
    val sessions: List<SkillSession> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class SkillSession(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val notes: String = ""
)

// ── Activity session (Cycling / Yoga / Walking) ───────────────
data class ActivitySession(
    val id: String = UUID.randomUUID().toString(),
    val activity: String = "",               // cycling | yoga | walking
    val dateIso: String = LocalDate.now().toString(),
    val durationMinutes: Int = 0,
    val distanceKm: Double? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ── Timer persisted state (survives app kill) ─────────────────
data class TimerPersistedState(
    val active: Boolean = false,            // there IS a timer in progress
    val running: Boolean = false,           // ticking (vs paused)
    val totalMs: Long = 0L,
    val remainingAtSaveMs: Long = 0L,       // remaining at moment of save
    val savedAtEpochMs: Long = 0L,          // wall-clock at save
    val label: String = ""
)

// ── Stopwatch persisted state ─────────────────────────────────
data class StopwatchPersistedState(
    val laps: List<LapEntry> = emptyList(),
    val elapsedMsAtSave: Long = 0L,
    val running: Boolean = false,
    val savedAtEpochMs: Long = 0L,          // wall-clock at save moment
    val lastLapMs: Long = 0L
)

// ── Sleep session in progress (survives app kill) ────────────
data class SleepSessionState(
    val active: Boolean = false,
    val startedAtEpochMs: Long = 0L,
    val disturbanceCount: Int = 0
)

// ── Fitness sample (Health Connect / step sensor) ─────────────
data class FitnessSample(
    val dateIso: String = LocalDate.now().toString(),
    val steps: Int = 0,
    val source: String = "unavailable",      // health_connect | step_sensor | unavailable
    val timestamp: Long = System.currentTimeMillis()
)

// ── Junk log entry (rich, product-catalogued) ────────────────
data class JunkLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateIso: String = LocalDate.now().toString(),
    val type: String = "food",                 // food | liquid
    val category: String = "",                 // OFF category tag e.g. "chips"
    val brand: String = "",
    val productName: String = "",
    val productId: String = "",                // OFF barcode
    val servingGrams: Int = 30,
    val country: String = "",                  // ISO-2
    val nutriscore: String = "",               // a-e
    val novaGroup: Int = 0,                    // 1..4
    val kcal: Int = 0,
    val sugarG: Double = 0.0,
    val satFatG: Double = 0.0,
    val saltG: Double = 0.0,
    val additivesCount: Int = 0,
    val overallSeverity: String = "Ok",        // Ok | Watch | Bad | Critical
    val timestamp: Long = System.currentTimeMillis()
)
