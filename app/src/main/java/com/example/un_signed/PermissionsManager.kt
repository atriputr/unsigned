package com.example.un_signed

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Central place to query the runtime permissions this feature set needs.
 *
 * Every check is wrapped in runCatching: some OEM Android forks (seen on Sony Xperia 5 III)
 * throw SecurityException when `checkSelfPermission` is called right after the user has
 * revoked that permission from Settings → App info → Permissions. A single unhandled
 * SecurityException there is enough to crash the process on cold start.
 */
object PermissionsManager {

    private fun check(ctx: Context, perm: String): Boolean = runCatching {
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun hasCalendarPermission(ctx: Context): Boolean =
        check(ctx, Manifest.permission.READ_CALENDAR) && check(ctx, Manifest.permission.WRITE_CALENDAR)

    fun hasNotificationPermission(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            check(ctx, Manifest.permission.POST_NOTIFICATIONS)
        else true

    fun hasActivityRecognitionPermission(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            check(ctx, Manifest.permission.ACTIVITY_RECOGNITION)
        else true

    fun canScheduleExactAlarms(ctx: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() == true
        } else true
    }.getOrDefault(false)

    /** Runtime permissions still missing for Calendar sync (READ/WRITE_CALENDAR). */
    fun missingCalendarPermissions(ctx: Context): List<String> =
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            .filter { !check(ctx, it) }

    /** Runtime permissions still missing for reminders + fitness tracking. */
    fun missingReminderAndFitnessPermissions(ctx: Context): List<String> {
        val missing = mutableListOf<String>()
        if (!hasNotificationPermission(ctx) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }
        if (!hasActivityRecognitionPermission(ctx) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            missing += Manifest.permission.ACTIVITY_RECOGNITION
        }
        return missing
    }
}
