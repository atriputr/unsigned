package com.example.un_signed

import kotlin.math.roundToInt

// ── User preferences (units, theme, haptics) ────────────────
data class AppPreferences(
    val weightUnit: String = "kg",         // kg | lb
    val heightUnit: String = "cm",         // cm | in
    val tempUnit: String = "C",            // C | F
    val distanceUnit: String = "km",       // km | mi
    val volumeUnit: String = "L",          // L | oz
    val theme: String = "DARK",            // DARK | CREAM | AMBER
    val hapticsEnabled: Boolean = true,
    val startOfWeek: String = "MONDAY",    // MONDAY | SUNDAY
    val languageCode: String = "en",       // en, hi, ru, zh, ja, fr, de, es, it, pt...
    val syncTasksToPhoneCalendar: Boolean = false,
    val syncTasksToPhoneAlarms: Boolean = false,
    val pomodoroReminderIntervalMin: Int = 15
)

// ── Weight log entry (long-term body-tracking) ──────────────
data class WeightEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dateIso: String = java.time.LocalDate.now().toString(),
    val weightKg: Double = 0.0,            // always stored in kg
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ── Focus session (Pomodoro) ────────────────────────────────
data class FocusSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val completedCycles: Int = 0,
    val skillId: String? = null,           // optional link to SkillItem
    val skillName: String = "",
    val notes: String = ""
) {
    val totalFocusMinutes: Int get() = completedCycles * focusMinutes
}

// ── Focus timer persisted state ─────────────────────────────
data class FocusTimerState(
    val active: Boolean = false,
    val running: Boolean = false,
    val phase: String = "FOCUS",           // FOCUS | BREAK
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val cycleGoal: Int = 4,                // total focus cycles targeted
    val completedCycles: Int = 0,
    val remainingMsAtSave: Long = 0L,
    val savedAtEpochMs: Long = 0L,
    val skillId: String? = null,
    val skillName: String = ""
)

/** All unit conversions — internally we always store SI units (kg, cm, C, km, L). */
object Units {
    // ── Weight ──────────────────────────────────────────────
    fun kgToLb(kg: Double): Double = kg * 2.20462
    fun lbToKg(lb: Double): Double = lb / 2.20462
    fun displayWeight(kg: Double, unit: String): String = when (unit) {
        "lb" -> "%.1f lb".format(kgToLb(kg))
        else -> "%.1f kg".format(kg)
    }
    fun weightSuffix(unit: String) = if (unit == "lb") "lb" else "kg"
    fun weightToKg(value: Double, unit: String): Double =
        if (unit == "lb") lbToKg(value) else value

    // ── Height ──────────────────────────────────────────────
    fun cmToInches(cm: Double): Double = cm / 2.54
    fun inchesToCm(inches: Double): Double = inches * 2.54
    /** Split inches into ft'in" */
    fun cmToFeetInches(cm: Double): Pair<Int, Int> {
        val totalIn = cmToInches(cm).roundToInt()
        return totalIn / 12 to totalIn % 12
    }
    fun feetInchesToCm(ft: Int, inches: Int): Double = inchesToCm((ft * 12 + inches).toDouble())
    fun displayHeight(cm: Double, unit: String): String = when (unit) {
        "in" -> {
            val (ft, inches) = cmToFeetInches(cm)
            "${ft}'${inches}\""
        }
        else -> "%.0f cm".format(cm)
    }
    fun heightSuffix(unit: String) = if (unit == "in") "ft/in" else "cm"

    // ── Temperature ─────────────────────────────────────────
    fun cToF(c: Double): Double = c * 9.0 / 5.0 + 32.0
    fun fToC(f: Double): Double = (f - 32.0) * 5.0 / 9.0
    fun displayTemp(c: Double, unit: String, decimals: Int = 0): String = when (unit) {
        "F" -> "%.${decimals}f°F".format(cToF(c))
        else -> "%.${decimals}f°C".format(c)
    }

    // ── Distance ────────────────────────────────────────────
    fun kmToMi(km: Double): Double = km * 0.621371
    fun miToKm(mi: Double): Double = mi / 0.621371
    fun displayDistance(km: Double, unit: String, decimals: Int = 1): String = when (unit) {
        "mi" -> "%.${decimals}f mi".format(kmToMi(km))
        else -> "%.${decimals}f km".format(km)
    }
    fun distanceSuffix(unit: String) = if (unit == "mi") "mi" else "km"
    fun distanceToKm(value: Double, unit: String): Double =
        if (unit == "mi") miToKm(value) else value

    // ── Volume ──────────────────────────────────────────────
    fun mlToOz(ml: Double): Double = ml * 0.033814
    fun ozToMl(oz: Double): Double = oz / 0.033814
    /** Preferred display for water volumes (auto litre / oz-cup formatting). */
    fun displayVolume(ml: Int, unit: String): String = when (unit) {
        "oz" -> "%.1f oz".format(mlToOz(ml.toDouble()))
        else -> "%.2f L".format(ml / 1000.0)
    }
    fun volumeSuffix(unit: String) = if (unit == "oz") "oz" else "L"

    // ── BMI is unit-agnostic (metric formula, always kg/m²) — display only ─
    fun formatBmi(bmi: Double) = "%.1f".format(bmi)
}
