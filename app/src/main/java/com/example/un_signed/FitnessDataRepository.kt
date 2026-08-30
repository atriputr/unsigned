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

    /** Reads today's step count from Health Connect, falls back to the device sensor, else returns an "unavailable" sample. */
    suspend fun getTodaySteps(ctx: Context): FitnessSample {
        val today = LocalDate.now()
        client(ctx)?.let { hc ->
            try {
                if (hc.permissionController.getGrantedPermissions().contains(HealthPermission.getReadPermission(StepsRecord::class))) {
                    val zone = ZoneId.systemDefault()
                    val start = today.atStartOfDay(zone).toInstant()
                    val end = Instant.now()
                    val response = hc.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(start, end)
                        )
                    )
                    val steps = response[StepsRecord.COUNT_TOTAL]?.toInt()
                    if (steps != null) {
                        val sample = FitnessSample(dateIso = today.toString(), steps = steps, source = "health_connect")
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
                return FitnessSample(dateIso = today.toString(), steps = steps, source = "step_sensor")
            }
        }

        return FitDataRepository.loadFitnessSamples().firstOrNull { it.dateIso == today.toString() }
            ?: FitnessSample(dateIso = today.toString(), steps = 0, source = "unavailable")
    }
}
