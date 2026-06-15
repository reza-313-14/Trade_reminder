package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Reminder
import com.example.data.ReminderHistory
import com.example.data.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository

    val reminders: StateFlow<List<Reminder>>
    val history: StateFlow<List<ReminderHistory>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ReminderRepository(application, database.reminderDao())
        
        reminders = repository.remindersFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        history = repository.historyFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addReminder(label: String, intervalSeconds: Long) {
        viewModelScope.launch {
            repository.insertReminder(label, intervalSeconds)
        }
    }

    fun addActiveReminder(label: String, intervalSeconds: Long) {
        viewModelScope.launch {
            val newId = repository.insertReminder(label, intervalSeconds)
            repository.toggleReminderActive(newId.toInt(), true)
        }
    }

    fun toggleReminder(reminderId: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderActive(reminderId, isActive)
        }
    }

    fun stopAllReminders() {
        viewModelScope.launch {
            reminders.value.forEach { reminder ->
                if (reminder.isActive) {
                    repository.toggleReminderActive(reminder.id, false)
                }
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
