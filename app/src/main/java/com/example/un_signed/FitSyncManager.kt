package com.example.un_signed

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * SYNC WITH FIT — comprehensive Health Connect import that pulls every metric needed to drive
 * the Pulse-grade scoring features (Recovery, Strain, Sleep, Stress, Energy Bank, Pulse Age,
 * Fitness Level, Health Monitor).
 *
 * Workflow:
 *   1. Caller checks [hasAllPermissions] → if false, [requestPermissionsContract] launches the HC
 *      permission sheet. Caller re-runs sync afterwards.
 *   2. [syncAll] walks every supported record type, reads yesterday + today, folds results
 *      into a FitnessSample for today, and persists it. Also persists weight/height into the
 *      user profile when fresher than what we have.
 *   3. [progress] is a StateFlow the UI can observe for live status: "Pulling HRV …" etc.
 *
 * All reads are permission-guarded individually — one missing grant does not take the rest down.
 */
object FitSyncManager {

    /**
     * All read permissions a full Pulse sync needs. Built per-item inside a try block so a
     * single `HealthPermission.getReadPermission()` throwing (which should be impossible but
     * has been seen on some OEM Health Connect forks) can't bring the whole singleton down —
     * every reference to FitSyncManager would otherwise crash the Activity at init time.
     *
     * Lazy so classloading is deferred until first use, keeping cold-start fast.
     */
    val READ_PERMISSIONS: Set<String> by lazy {
        val out = linkedSetOf<String>()
        fun add(cls: kotlin.reflect.KClass<out androidx.health.connect.client.records.Record>) {
            runCatching { out += HealthPermission.getReadPermission(cls) }
        }
        add(StepsRecord::class)
        add(DistanceRecord::class)
        add(ActiveCaloriesBurnedRecord::class)
        add(TotalCaloriesBurnedRecord::class)
        add(HeartRateRecord::class)
        add(HeartRateVariabilityRmssdRecord::class)
        add(RestingHeartRateRecord::class)
        add(RespiratoryRateRecord::class)
        add(OxygenSaturationRecord::class)
        add(BodyTemperatureRecord::class)
        add(SleepSessionRecord::class)
        add(ExerciseSessionRecord::class)
        add(Vo2MaxRecord::class)
        add(BodyFatRecord::class)
        add(LeanBodyMassRecord::class)
        add(WeightRecord::class)
        add(HeightRecord::class)
        add(BloodPressureRecord::class)
        add(BloodGlucoseRecord::class)
        out
    }

    data class SyncResult(
        val fitness: FitnessSample,
        val bpImported: Int,
        val glucoseImported: Int,
        val weightSynced: Boolean,
        val heightSynced: Boolean,
        val granted: Int,
        val requested: Int,
        val durationMs: Long
    ) {
        /** Pretty one-liner for toast / summary panel. */
        fun summary(): String {
            val present = buildList {
                if (fitness.heartRateBpm != null) add("HR")
                if (fitness.hrvMs != null) add("HRV")
                if (fitness.restingHrBpm != null) add("RHR")
                if (fitness.respRateBpm != null) add("Resp")
                if (fitness.spo2Percent != null) add("SpO₂")
                if (fitness.bodyTempC != null) add("Temp")
                if (fitness.vo2Max != null) add("VO₂max")
                if (fitness.bodyFatPercent != null) add("BodyFat")
                if (fitness.leanMassKg != null) add("Lean")
                if (fitness.sleepMinutes != null) add("Sleep")
                if (fitness.exerciseMinutes != null) add("Exercise")
                if (fitness.steps > 0) add("Steps")
                if (fitness.activeKcal > 0) add("Kcal")
                if (fitness.distanceMeters > 0) add("Distance")
                if (bpImported > 0) add("$bpImported BP")
                if (glucoseImported > 0) add("$glucoseImported Glucose")
                if (weightSynced) add("Weight")
                if (heightSynced) add("Height")
            }
            return if (present.isEmpty()) "No metrics available from Fit yet"
            else present.joinToString(" · ")
        }
    }

    /** UI-observable progress: current task label + 0..1 completion. */
    data class Progress(val task: String, val fraction: Float, val finished: Boolean = false)

    private val _progress = MutableStateFlow(Progress("", 0f))
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private fun client(ctx: Context): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE)
            HealthConnectClient.getOrCreate(ctx) else null

    /** Health Connect SDK availability — UI surfaces an install-HC prompt when NEEDS_UPDATE. */
    fun availability(ctx: Context): FitnessDataRepository.Availability =
        FitnessDataRepository.availability(ctx)

    /** Launcher contract the activity binds. Caller: `rememberLauncherForActivityResult(contract)`. */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(ctx: Context): Boolean {
        val hc = client(ctx) ?: return false
        return try {
            hc.permissionController.getGrantedPermissions().containsAll(READ_PERMISSIONS)
        } catch (_: Exception) { false }
    }

    suspend fun grantedCount(ctx: Context): Int {
        val hc = client(ctx) ?: return 0
        return try {
            hc.permissionController.getGrantedPermissions().intersect(READ_PERMISSIONS).size
        } catch (_: Exception) { 0 }
    }

    /**
     * Full sync. Reads every record we have permission for, folds into today's FitnessSample,
     * stores BP/glucose readings into the vitals log, syncs weight+height into the user profile
     * when fresher than existing. Returns a [SyncResult] the UI can render.
     */
    suspend fun syncAll(ctx: Context): SyncResult = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val hc = client(ctx) ?: return@withContext SyncResult(
            FitnessSample(source = "unavailable"), 0, 0, false, false, 0, READ_PERMISSIONS.size, 0L
        )

        val granted = try { hc.permissionController.getGrantedPermissions() } catch (_: Exception) { emptySet() }
        val todayDate = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val startToday = todayDate.atStartOfDay(zone).toInstant()
        val startYesterday = todayDate.minusDays(1).atStartOfDay(zone).toInstant()
        val now = Instant.now()
        val todayFilter = TimeRangeFilter.between(startToday, now)
        val last24h = TimeRangeFilter.between(startYesterday, now)
        val last7d = TimeRangeFilter.between(now.minus(7, ChronoUnit.DAYS), now)
        val last30d = TimeRangeFilter.between(now.minus(30, ChronoUnit.DAYS), now)

        var task = 0
        val total = 15f
        fun tick(label: String) {
            task++
            _progress.value = Progress(label, task / total)
        }

        // ── Steps / distance / active kcal (today aggregate) ─
        tick("steps")
        val base = runCatching { FitnessDataRepository.getTodayFitness(ctx) }.getOrDefault(
            FitnessSample(dateIso = todayDate.toString(), source = "unavailable")
        )

        // ── Total calories (today) ────────────────────────────
        tick("total calories")
        val totalKcal = read(granted, TotalCaloriesBurnedRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, todayFilter))
                    .records.sumOf { it.energy.inKilocalories }.toInt()
            }.getOrNull()?.takeIf { it > 0 }
        }

        // ── HR (latest today) ─────────────────────────────────
        tick("heart rate")
        val hrLatest: Int? = read(granted, HeartRateRecord::class.java) {
            runCatching {
                var latestTs = Long.MIN_VALUE
                var latest: Long? = null
                hc.readRecords(ReadRecordsRequest(HeartRateRecord::class, todayFilter)).records
                    .forEach { rec ->
                        rec.samples.forEach { s ->
                            val t = s.time.toEpochMilli()
                            if (t > latestTs) { latestTs = t; latest = s.beatsPerMinute }
                        }
                    }
                latest?.toInt()
            }.getOrNull()
        } ?: base.heartRateBpm

        // ── HRV (RMSSD, latest 24h) ───────────────────────────
        tick("HRV")
        val hrv: Double? = read(granted, HeartRateVariabilityRmssdRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, last24h))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.heartRateVariabilityMillis
            }.getOrNull()
        }

        // ── Resting HR (latest 7d) ────────────────────────────
        tick("resting HR")
        val rhr: Int? = read(granted, RestingHeartRateRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, last7d))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.beatsPerMinute?.toInt()
            }.getOrNull()
        }

        // ── Respiratory rate (latest 24h) ─────────────────────
        tick("respiratory rate")
        val resp: Double? = read(granted, RespiratoryRateRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, last24h))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.rate
            }.getOrNull()
        }

        // ── SpO2 (latest today) ───────────────────────────────
        tick("SpO₂")
        val spo2: Int? = read(granted, OxygenSaturationRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, todayFilter))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.percentage?.value?.toInt()
            }.getOrNull()
        } ?: base.spo2Percent

        // ── Body temperature (latest today) ───────────────────
        tick("body temperature")
        val temp: Double? = read(granted, BodyTemperatureRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, todayFilter))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.temperature?.inCelsius
            }.getOrNull()
        } ?: base.bodyTempC

        // ── VO2 max (latest 30d) ──────────────────────────────
        tick("VO₂ max")
        val vo2: Double? = read(granted, Vo2MaxRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(Vo2MaxRecord::class, last30d))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.vo2MillilitersPerMinuteKilogram
            }.getOrNull()
        }

        // ── Body fat % (latest 30d) ───────────────────────────
        tick("body fat")
        val bodyFat: Double? = read(granted, BodyFatRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(BodyFatRecord::class, last30d))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.percentage?.value
            }.getOrNull()
        }

        // ── Lean body mass (latest 30d) ───────────────────────
        tick("lean mass")
        val lean: Double? = read(granted, LeanBodyMassRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(LeanBodyMassRecord::class, last30d))
                    .records.maxByOrNull { it.time.toEpochMilli() }?.mass?.inKilograms
            }.getOrNull()
        }

        // ── Sleep (last 24h total minutes) ────────────────────
        tick("sleep")
        val sleepMin: Int? = read(granted, SleepSessionRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(SleepSessionRecord::class, last24h))
                    .records.sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
                    .toInt().takeIf { it > 0 }
            }.getOrNull()
        }

        // ── Exercise (today total minutes) ────────────────────
        tick("exercise")
        val exerciseMin: Int? = read(granted, ExerciseSessionRecord::class.java) {
            runCatching {
                hc.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, todayFilter))
                    .records.sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
                    .toInt().takeIf { it > 0 }
            }.getOrNull()
        }

        // ── Weight (latest 30d) → sync into UserProfile if newer ─
        tick("weight")
        var weightSynced = false
        read(granted, WeightRecord::class.java) {
            runCatching {
                val latest = hc.readRecords(ReadRecordsRequest(WeightRecord::class, last30d))
                    .records.maxByOrNull { it.time.toEpochMilli() }
                if (latest != null) {
                    val kg = latest.weight.inKilograms
                    val profile = FitDataRepository.loadUserProfile()
                    if (kg > 20.0 && kg < 300.0 && kotlin.math.abs(kg - profile.weightKg) > 0.1) {
                        FitDataRepository.saveUserProfile(profile.copy(weightKg = kg))
                        weightSynced = true
                    }
                }
            }
        }

        // ── Height (latest 30d) → sync into UserProfile if newer ─
        tick("height")
        var heightSynced = false
        read(granted, HeightRecord::class.java) {
            runCatching {
                val latest = hc.readRecords(ReadRecordsRequest(HeightRecord::class, last30d))
                    .records.maxByOrNull { it.time.toEpochMilli() }
                if (latest != null) {
                    val cm = latest.height.inMeters * 100.0
                    val profile = FitDataRepository.loadUserProfile()
                    if (cm > 50.0 && cm < 250.0 && kotlin.math.abs(cm - profile.heightCm) > 0.5) {
                        FitDataRepository.saveUserProfile(profile.copy(heightCm = cm))
                        heightSynced = true
                    }
                }
            }
        }

        // ── BP readings (last 7d) → append new ones to vitals log ─
        tick("blood pressure")
        val bpAdded = read(granted, BloodPressureRecord::class.java) {
            runCatching {
                val existing = FitDataRepository.loadBpReadings().associateBy { it.timestamp }
                val fetched = hc.readRecords(ReadRecordsRequest(BloodPressureRecord::class, last30d))
                    .records
                val fresh = fetched.filter { it.time.toEpochMilli() !in existing.keys }
                    .map { r ->
                        BpReading(
                            systolic = r.systolic.inMillimetersOfMercury.toInt(),
                            diastolic = r.diastolic.inMillimetersOfMercury.toInt(),
                            pulse = 0,
                            timestamp = r.time.toEpochMilli(),
                            notes = "imported from Fit"
                        )
                    }
                if (fresh.isNotEmpty()) {
                    FitDataRepository.saveBpReadings(
                        (FitDataRepository.loadBpReadings() + fresh).sortedBy { it.timestamp }
                    )
                }
                fresh.size
            }.getOrNull() ?: 0
        } ?: 0

        // ── Glucose readings (last 30d) → append new ones ─────
        tick("glucose")
        val glucoseAdded = read(granted, BloodGlucoseRecord::class.java) {
            runCatching {
                val existing = FitDataRepository.loadGlucoseReadings().associateBy { it.timestamp }
                val fetched = hc.readRecords(ReadRecordsRequest(BloodGlucoseRecord::class, last30d))
                    .records
                val fresh = fetched.filter { it.time.toEpochMilli() !in existing.keys }
                    .map { r ->
                        GlucoseReading(
                            mgPerDl = r.level.inMilligramsPerDeciliter.toInt(),
                            context = "Random",
                            timestamp = r.time.toEpochMilli(),
                            notes = "imported from Fit"
                        )
                    }
                if (fresh.isNotEmpty()) {
                    FitDataRepository.saveGlucoseReadings(
                        (FitDataRepository.loadGlucoseReadings() + fresh).sortedBy { it.timestamp }
                    )
                }
                fresh.size
            }.getOrNull() ?: 0
        } ?: 0

        // ── Fold into today's FitnessSample + persist ─────────
        val folded = base.copy(
            heartRateBpm = hrLatest,
            spo2Percent = spo2,
            bodyTempC = temp,
            hrvMs = hrv,
            restingHrBpm = rhr,
            respRateBpm = resp,
            vo2Max = vo2,
            bodyFatPercent = bodyFat,
            leanMassKg = lean,
            totalKcal = totalKcal,
            sleepMinutes = sleepMin,
            exerciseMinutes = exerciseMin,
            source = if (base.source == "unavailable") "health_connect" else base.source,
            timestamp = System.currentTimeMillis()
        )
        FitDataRepository.upsertFitnessSample(folded)

        _progress.value = Progress("done", 1f, finished = true)

        SyncResult(
            fitness = folded,
            bpImported = bpAdded,
            glucoseImported = glucoseAdded,
            weightSynced = weightSynced,
            heightSynced = heightSynced,
            granted = granted.intersect(READ_PERMISSIONS).size,
            requested = READ_PERMISSIONS.size,
            durationMs = System.currentTimeMillis() - t0
        )
    }

    fun resetProgress() { _progress.value = Progress("", 0f) }

    // ── Direct provider navigation (fallbacks when the permission launcher is a no-op) ─────
    //
    // Pulse Mobile learned this the hard way: on some Android OEM builds and some HC versions,
    // the permission-sheet intent silently never resolves. The reliable escape hatch is to open
    // Health Connect's main settings activity directly — from there the user can navigate to
    // "Permissions" for our app manually.

    const val HC_PACKAGE = "com.google.android.apps.healthdata"
    const val FIT_PACKAGE = "com.google.android.apps.fitness"
    private const val HC_SETTINGS_ACTION = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    private const val HC_MANAGE_ACTION = "androidx.health.ACTION_MANAGE_HEALTH_DATA"

    /**
     * Open the Health Connect app's own settings. Returns true if an activity was launched.
     * Falls back through: our-app permissions → HC main → Play Store listing.
     */
    fun openHealthConnectSettings(ctx: Context): Boolean {
        val candidates = listOf(
            Intent(HC_SETTINGS_ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(HC_MANAGE_ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ctx.packageManager.getLaunchIntentForPackage(HC_PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        for (intent in candidates) {
            if (intent == null) continue
            try { ctx.startActivity(intent); return true } catch (_: Throwable) { }
        }
        return openPlayStoreForPackage(ctx, HC_PACKAGE)
    }

    /** Open Google Fit app so the user can confirm it's actually populated with Fitbit/band data. */
    fun openGoogleFit(ctx: Context): Boolean {
        val launch = ctx.packageManager.getLaunchIntentForPackage(FIT_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launch != null) {
            try { ctx.startActivity(launch); return true } catch (_: Throwable) { }
        }
        return openPlayStoreForPackage(ctx, FIT_PACKAGE)
    }

    private fun openPlayStoreForPackage(ctx: Context, pkg: String): Boolean {
        val fallbacks = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                .setPackage("com.android.vending")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        for (i in fallbacks) {
            try { ctx.startActivity(i); return true } catch (_: Throwable) { }
        }
        return false
    }

    /**
     * Force an eager HC client init before any permission launch. Some HC versions / OEM
     * builds silently reject permission intents if the client has never been constructed.
     */
    fun ensureInitialized(ctx: Context): Boolean = try {
        client(ctx) != null
    } catch (_: Throwable) { false }

    /** Permission-gate helper — returns null when permission for [recordClass] isn't granted. */
    private inline fun <T> read(
        granted: Set<String>,
        recordClass: Class<*>,
        block: () -> T?
    ): T? {
        @Suppress("UNCHECKED_CAST")
        val perm = HealthPermission.getReadPermission(
            recordClass.kotlin as kotlin.reflect.KClass<out androidx.health.connect.client.records.Record>
        )
        if (perm !in granted) return null
        return block()
    }
}
