package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_history_logs")
data class ReminderHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val reminderLabel: String,
    val triggeredAt: Long = System.currentTimeMillis()
)
