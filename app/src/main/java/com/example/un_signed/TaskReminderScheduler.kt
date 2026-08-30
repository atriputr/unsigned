package com.example.un_signed

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

object TaskReminderScheduler {

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_DATE_ISO = "date_iso"
    const val EXTRA_TASK_TEXT = "task_text"
    const val EXTRA_IS_POMODORO = "is_pomodoro"
    const val EXTRA_TICK_INDEX = "tick_index"
    const val EXTRA_FINAL_TRIGGER_MS = "final_trigger_ms"
    const val EXTRA_INTERVAL_MIN = "interval_min"

    private const val MAX_POMODORO_TICKS = 20

    private fun baseRequestCode(taskId: String): Int = taskId.hashCode() and 0x0FFFFFF

    private fun pendingIntentFor(ctx: Context, task: CalendarTask, date: LocalDate, isPomodoro: Boolean, tickIndex: Int, finalTriggerMs: Long, intervalMin: Int): PendingIntent {
        val intent = Intent(ctx, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_DATE_ISO, date.toString())
            putExtra(EXTRA_TASK_TEXT, task.text)
            putExtra(EXTRA_IS_POMODORO, isPomodoro)
            putExtra(EXTRA_TICK_INDEX, tickIndex)
            putExtra(EXTRA_FINAL_TRIGGER_MS, finalTriggerMs)
            putExtra(EXTRA_INTERVAL_MIN, intervalMin)
        }
        val requestCode = baseRequestCode(task.id) + tickIndex
        return PendingIntent.getBroadcast(ctx, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun scheduleExactAt(ctx: Context, triggerMillis: Long, pendingIntent: PendingIntent) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (PermissionsManager.canScheduleExactAlarms(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerMillis, 15 * 60_000L, pendingIntent)
            }
        } catch (_: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerMillis, 15 * 60_000L, pendingIntent)
        }
    }

    /** Schedules a reminder for [task] on [date]. Uses a repeating Pomodoro cadence when opted-in and due soon. */
    fun scheduleReminder(ctx: Context, date: LocalDate, task: CalendarTask, intervalMin: Int) {
        cancelReminders(ctx, task.id)

        val zone = ZoneId.systemDefault()
        val minutesOfDay = task.timeMinutesOfDay ?: (9 * 60)
        val triggerTime = date.atStartOfDay(zone).plusMinutes(minutesOfDay.toLong())
        val triggerMillis = triggerTime.toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        if (triggerMillis <= now) return

        val dueSoon = triggerMillis - now <= 24 * 60 * 60_000L
        if (task.pomodoroReminders && dueSoon) {
            scheduleNextPomodoroTick(ctx, date, task, tickIndex = 0, finalTriggerMs = triggerMillis, intervalMin = intervalMin)
        } else {
            val pi = pendingIntentFor(ctx, task, date, isPomodoro = false, tickIndex = 0, finalTriggerMs = triggerMillis, intervalMin = intervalMin)
            scheduleExactAt(ctx, triggerMillis, pi)
        }
    }

    /** Called by the receiver to arm the next Pomodoro-cadence ping, if any remain before the due time. */
    fun scheduleNextPomodoroTick(ctx: Context, date: LocalDate, task: CalendarTask, tickIndex: Int, finalTriggerMs: Long, intervalMin: Int) {
        if (tickIndex >= MAX_POMODORO_TICKS) return
        val now = System.currentTimeMillis()
        val nextTrigger = (now + intervalMin * 60_000L).coerceAtMost(finalTriggerMs)
        if (nextTrigger <= now) return

        val pi = pendingIntentFor(ctx, task, date, isPomodoro = true, tickIndex = tickIndex + 1, finalTriggerMs = finalTriggerMs, intervalMin = intervalMin)
        scheduleExactAt(ctx, nextTrigger, pi)
    }

    /** Cancels any pending single or Pomodoro-chain alarms for [taskId]. */
    fun cancelReminders(ctx: Context, taskId: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val base = taskId.hashCode() and 0x0FFFFFF
        for (offset in 0..MAX_POMODORO_TICKS) {
            val intent = Intent(ctx, TaskReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                ctx, base + offset, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }
}
