package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DatabaseProvider
import com.example.data.database.WaterLog
import com.example.data.database.WaterSettings
import com.example.data.repository.WaterRepository
import com.example.receiver.WaterAlarmHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "WaterViewModel"
    private val repository: WaterRepository

    init {
        val db = DatabaseProvider.getDatabase(application)
        repository = WaterRepository(db.waterLogDao(), db.waterSettingsDao())
    }

    // Settings StateFlow
    val settingsState: StateFlow<WaterSettings> = repository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterSettings()
        )

    // Today's Logs StateFlow
    val todayLogsState: StateFlow<List<WaterLog>> = repository.getTodayLogsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived states: Total intake ml of today
    val totalIntakeMlState: StateFlow<Int> = todayLogsState
        .combine(settingsState) { logs, _ ->
            logs.sumOf { it.amountMl }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Derived: Progress (0.0 to 1.0+)
    val intakeProgressState: StateFlow<Float> = combine(totalIntakeMlState, settingsState) { total, settings ->
        if (settings.dailyTargetMl > 0) {
            total.toFloat() / settings.dailyTargetMl
        } else {
            0f
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    // UI actions
    fun addIntake(amountMl: Int) {
        viewModelScope.launch {
            repository.insertLog(amountMl)
            // If reminder was pending, also dismiss it
            if (settingsState.value.isReminderPending) {
                repository.setReminderPending(false)
                // Schedule next alarm since they drank
                WaterAlarmHelper.scheduleNextAlarm(getApplication(), settingsState.value.reminderIntervalMinutes)
            }
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    fun dismissReminderConfirmDrank(amountMl: Int) {
        viewModelScope.launch {
            repository.insertLog(amountMl)
            repository.setReminderPending(false)
            // Schedule next reminder!
            WaterAlarmHelper.scheduleNextAlarm(getApplication(), settingsState.value.reminderIntervalMinutes)
            Log.d(TAG, "Dismissed reminder: confirmed drank $amountMl ml. Next scheduled.")
        }
    }

    fun dismissReminderConfirmOnly() {
        viewModelScope.launch {
            repository.setReminderPending(false)
            // Schedule next reminder!
            WaterAlarmHelper.scheduleNextAlarm(getApplication(), settingsState.value.reminderIntervalMinutes)
            Log.d(TAG, "Dismissed reminder: confirmed without logging. Next scheduled.")
        }
    }

    fun setReminderStateDirectly(isPending: Boolean) {
        viewModelScope.launch {
            repository.setReminderPending(isPending)
        }
    }

    fun undoLastIntake() {
        viewModelScope.launch {
            repository.undoLastLog()
        }
    }

    fun clearAllIntakes() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun updateSettings(targetMl: Int, intervalMinutes: Int, isNotificationsEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(targetMl, intervalMinutes, isNotificationsEnabled)
            
            // Re-schedule the alarm system with the new interval
            if (isNotificationsEnabled) {
                WaterAlarmHelper.scheduleNextAlarm(getApplication(), intervalMinutes)
            } else {
                WaterAlarmHelper.cancelAlarm(getApplication())
            }
        }
    }

    // Helper to start the notification system initially if it's not active
    fun startSchedulerIfNeeded() {
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            if (settings.isNotificationsEnabled) {
                WaterAlarmHelper.scheduleNextAlarm(getApplication(), settings.reminderIntervalMinutes)
            }
        }
    }

    // Test tool to trigger a reminder popup after a 5 seconds delay
    fun triggerTestAlarmImmediate() {
        viewModelScope.launch {
            // Cancel current alarm to not interfere
            WaterAlarmHelper.cancelAlarm(getApplication())
            // Schedule next alarm in 0 minutes (which is immediate or in 5 seconds)
            // Let's schedule it 5 seconds from now using standard AlarmHelper logic in receiver
            val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(getApplication(), com.example.receiver.WaterAlarmReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                getApplication(),
                4040,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + 5000L // 5 seconds
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "Test alarm scheduled to trigger in 5 seconds.")
        }
    }
}
