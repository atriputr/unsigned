package com.example.un_signed

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sources movement data with Health Connect as the primary provider (the modern replacement for
 * the now-shut-down Google Fit REST API), falling back to the device's step-counter sensor when
 * Health Connect isn't installed/available.
 */
object FitnessDataRepository {

    private val readPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    enum class Availability { AVAILABLE, NEEDS_UPDATE, UNAVAILABLE }

    fun availability(ctx: Context): Availability = when (HealthConnectClient.getSdkStatus(ctx)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.UNAVAILABLE
    }

    private fun client(ctx: Context): HealthConnectClient? =
        if (availability(ctx) == Availability.AVAILABLE) HealthConnectClient.getOrCreate(ctx) else null

    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    fun requiredPermissions(): Set<String> = readPermissions

    suspend fun hasAllPermissions(ctx: Context): Boolean {
        val hc = client(ctx) ?: return false
        return try {
            hc.permissionController.getGrantedPermissions().containsAll(readPermissions)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads today's fitness snapshot — steps + active calories + distance — from Health Connect,
     * falling back to the device step-counter for steps only when HC isn't installed. Result is
     * persisted so late-day reads (or no-network state) still show something.
     */
    suspend fun getTodayFitness(ctx: Context): FitnessSample {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val startInstant = today.atStartOfDay(zone).toInstant()
        val endInstant = Instant.now()

        client(ctx)?.let { hc ->
            try {
                val granted = hc.permissionController.getGrantedPermissions()
                val filter = TimeRangeFilter.between(startInstant, endInstant)

                val metrics = mutableSetOf<androidx.health.connect.client.aggregate.AggregateMetric<*>>()
                if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) metrics += StepsRecord.COUNT_TOTAL
                if (granted.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))) metrics += ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                if (granted.contains(HealthPermission.getReadPermission(DistanceRecord::class))) metrics += DistanceRecord.DISTANCE_TOTAL

                if (metrics.isNotEmpty()) {
                    val response = hc.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = filter))
                    val steps = response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
                    val kcal = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt() ?: 0
                    val meters = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toInt() ?: 0

                    // Only trust the response if we got *some* signal (avoid overwriting sensor
                    // fallback with all-zero placeholders when HC has no data yet today).
                    if (steps > 0 || kcal > 0 || meters > 0) {
                        val sample = FitnessSample(
                            dateIso = today.toString(),
                            steps = steps,
                            activeKcal = kcal,
                            distanceMeters = meters,
                            source = "health_connect"
                        )
                        FitDataRepository.upsertFitnessSample(sample)
                        return sample
                    }
                }
            } catch (_: Exception) {
                // fall through to sensor fallback
            }
        }

        if (StepSensorTracker.isAvailable(ctx) && PermissionsManager.hasActivityRecognitionPermission(ctx)) {
            var result: Int? = null
            StepSensorTracker.sampleOnce(ctx) { result = it }
            val steps = result
            if (steps != null) {
                // Rough on-device estimate when HC has no calories/distance to offer.
                val profile = FitDataRepository.loadUserProfile()
                val (estKcal, estMeters) = estimateFromSteps(steps, profile)
                val sample = FitnessSample(
                    dateIso = today.toString(),
                    steps = steps,
                    activeKcal = estKcal,
                    distanceMeters = estMeters,
                    source = "step_sensor"
                )
                FitDataRepository.upsertFitnessSample(sample)
                return sample
            }
        }

        return FitDataRepository.loadFitnessSamples().firstOrNull { it.dateIso == today.toString() }
            ?: FitnessSample(dateIso = today.toString(), steps = 0, source = "unavailable")
    }

    /** Backwards-compatible alias used by older callers. */
    suspend fun getTodaySteps(ctx: Context): FitnessSample = getTodayFitness(ctx)

    /**
     * Rough estimator for the sensor-only path.
     *  - stride ≈ 0.415 × height (Boone & Bove) → falls back to 0.75 m if height unknown.
     *  - calories/step ≈ 0.04 × (weight in kg / 70) — cheap approximation.
     */
    private fun estimateFromSteps(steps: Int, profile: UserProfile): Pair<Int, Int> {
        if (steps <= 0) return 0 to 0
        val strideM = if (profile.heightCm > 0) profile.heightCm / 100.0 * 0.415 else 0.75
        val meters = (steps * strideM).toInt()
        val kg = if (profile.weightKg > 0) profile.weightKg else 70.0
        val kcal = (steps * 0.04 * (kg / 70.0)).toInt()
        return kcal to meters
    }
}
