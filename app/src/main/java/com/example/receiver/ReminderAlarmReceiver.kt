package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ReminderHistory
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_LABEL) ?: "Interval Alarm"
        val intervalSeconds = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_INTERVAL, 60)

        if (reminderId == -1) {
            Log.e("ReminderAlarmReceiver", "Received invalid reminder ID")
            return
        }

        Log.d("ReminderAlarmReceiver", "Triggered alarm for reminder ID: $reminderId, label: $label")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.reminderDao()
                val reminder = dao.getReminderById(reminderId)

                if (reminder != null && reminder.isActive) {
                    val now = System.currentTimeMillis()

                    // 1. Show notification
                    showNotification(context, reminderId, label)

                    // 2. Insert into history logs
                    val historyLog = ReminderHistory(
                        reminderId = reminderId,
                        reminderLabel = label,
                        triggeredAt = now
                    )
                    dao.insertHistory(historyLog)

                    // 3. Schedule the next alarm: calculate nextTriggerAt = current trigger (now) + intervalSeconds
                    val nextTrigger = now + (intervalSeconds * 1000)
                    val updatedReminder = reminder.copy(
                        lastTriggeredAt = now,
                        nextTriggerAt = nextTrigger
                    )
                    dao.updateReminder(updatedReminder)

                    // Schedule next using AlarmScheduler
                    val scheduler = AlarmScheduler(context)
                    scheduler.schedule(updatedReminder)
                } else {
                    Log.d("ReminderAlarmReceiver", "Reminder is either null or inactive, skipping rescheduling")
                }
            } catch (e: Exception) {
                Log.e("ReminderAlarmReceiver", "Error processing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, reminderId: Int, label: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "interval_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Interval Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels for interval-triggered alarms"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // fall back to standard Android alarm icon
            .setContentTitle("Reminder Triggered!")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(reminderId, notification)
    }
}
