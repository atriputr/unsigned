package com.example.un_signed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_ID) ?: return
        val dateIso = intent.getStringExtra(TaskReminderScheduler.EXTRA_DATE_ISO) ?: return
        val taskText = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TEXT) ?: ""
        val isPomodoro = intent.getBooleanExtra(TaskReminderScheduler.EXTRA_IS_POMODORO, false)
        val tickIndex = intent.getIntExtra(TaskReminderScheduler.EXTRA_TICK_INDEX, 0)
        val finalTriggerMs = intent.getLongExtra(TaskReminderScheduler.EXTRA_FINAL_TRIGGER_MS, 0L)
        val intervalMin = intent.getIntExtra(TaskReminderScheduler.EXTRA_INTERVAL_MIN, 15)

        showNotification(context, taskId, taskText, isPomodoro)

        if (isPomodoro && System.currentTimeMillis() < finalTriggerMs) {
            val date = try { LocalDate.parse(dateIso) } catch (_: Exception) { LocalDate.now() }
            FitDataRepository.init(context)
            val task = FitDataRepository.loadCalendarTasks()[date]?.firstOrNull { it.id == taskId }
            if (task != null) {
                TaskReminderScheduler.scheduleNextPomodoroTick(context, date, task, tickIndex, finalTriggerMs, intervalMin)
            }
        }
    }

    private fun showNotification(context: Context, taskId: String, taskText: String, isPomodoro: Boolean) {
        if (!PermissionsManager.hasNotificationPermission(context)) return

        val channelId = "task_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isPomodoro) "Pomodoro reminder" else "Task reminder")
            .setContentText(taskText.ifBlank { "You have a task due" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(taskId.hashCode(), notification)
        } catch (_: SecurityException) {
            // notification permission revoked between check and notify — ignore
        }
    }
}
