package com.example.un_signed

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.time.LocalDate

/** Adds a silent entry into the phone's stock Alarm/Clock app for timed tasks. */
object SystemAlarmSync {

    /** Returns true if an alarm intent was dispatched. Only applies to tasks with a set time. */
    fun addSystemAlarm(ctx: Context, task: CalendarTask, date: LocalDate): Boolean {
        val minutesOfDay = task.timeMinutesOfDay ?: return false
        // Only make sense for alarms due today or in the future — stock Alarm app has no concept of "date".
        if (date.isBefore(LocalDate.now())) return false

        val hour = minutesOfDay / 60
        val minute = minutesOfDay % 60

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, task.text)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
