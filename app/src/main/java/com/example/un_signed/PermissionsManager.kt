package com.example.un_signed

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Central place to query the runtime permissions this feature set needs. */
object PermissionsManager {

    fun hasCalendarPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

    fun hasActivityRecognitionPermission(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        else true

    fun canScheduleExactAlarms(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else true

    /** Runtime permissions still missing for Calendar sync (READ/WRITE_CALENDAR). */
    fun missingCalendarPermissions(ctx: Context): List<String> =
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            .filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }

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
