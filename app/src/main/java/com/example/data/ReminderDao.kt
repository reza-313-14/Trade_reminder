package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_settings ORDER BY createdAt DESC")
    fun getAllRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminder_settings WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminder_settings WHERE isActive = 1")
    suspend fun getActiveReminders(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT * FROM reminder_history_logs ORDER BY triggeredAt DESC")
    fun getAllHistoryFlow(): Flow<List<ReminderHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ReminderHistory)

    @Query("DELETE FROM reminder_history_logs")
    suspend fun clearAllHistory()
}
