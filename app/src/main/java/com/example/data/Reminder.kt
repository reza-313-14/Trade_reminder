package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val intervalSeconds: Long,
    val isActive: Boolean = false,
    val lastTriggeredAt: Long = 0,
    val nextTriggerAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
