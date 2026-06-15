package com.example.data

import android.content.Context
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val context: Context, private val reminderDao: ReminderDao) {
    private val scheduler by lazy { AlarmScheduler(context) }

    val remindersFlow: Flow<List<Reminder>> = reminderDao.getAllRemindersFlow()
    val historyFlow: Flow<List<ReminderHistory>> = reminderDao.getAllHistoryFlow()

    suspend fun insertReminder(label: String, intervalSeconds: Long): Long {
        val reminder = Reminder(
            label = label,
            intervalSeconds = intervalSeconds,
            isActive = false
        )
        return reminderDao.insertReminder(reminder)
    }

    suspend fun toggleReminderActive(reminderId: Int, isActive: Boolean) {
        val reminder = reminderDao.getReminderById(reminderId) ?: return
        
        val nextTrigger = if (isActive) {
            System.currentTimeMillis() + (reminder.intervalSeconds * 1000)
        } else {
            0
        }

        val updatedReminder = reminder.copy(
            isActive = isActive,
            nextTriggerAt = nextTrigger
        )
        reminderDao.updateReminder(updatedReminder)

        if (isActive) {
            scheduler.schedule(updatedReminder)
        } else {
            scheduler.cancel(updatedReminder)
        }
    }

    suspend fun deleteReminder(reminder: Reminder) {
        scheduler.cancel(reminder)
        reminderDao.deleteReminder(reminder)
    }

    suspend fun rescheduleAllActive() {
        val active = reminderDao.getActiveReminders()
        val now = System.currentTimeMillis()
        for (reminder in active) {
            val updated = if (reminder.nextTriggerAt < now) {
                reminder.copy(nextTriggerAt = now + (reminder.intervalSeconds * 1000))
            } else {
                reminder
            }
            if (updated !== reminder) {
                reminderDao.updateReminder(updated)
            }
            scheduler.schedule(updated)
        }
    }

    suspend fun clearHistory() {
        reminderDao.clearAllHistory()
    }
}
