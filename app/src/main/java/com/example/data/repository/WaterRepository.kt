package com.example.data.repository

import com.example.data.database.WaterLog
import com.example.data.database.WaterLogDao
import com.example.data.database.WaterSettings
import com.example.data.database.WaterSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class WaterRepository(
    private val logDao: WaterLogDao,
    private val settingsDao: WaterSettingsDao
) {

    // Helper to get start of today (midnight)
    fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Helper to get end of today
    fun getEndOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    // Today's logs reactively
    fun getTodayLogsFlow(): Flow<List<WaterLog>> {
        val start = getStartOfDay()
        val end = getEndOfDay()
        return logDao.getAllLogsFlow().map { logs ->
            logs.filter { it.timestamp in start..end }
        }
    }

    // All logs reactively
    fun getAllLogsFlow(): Flow<List<WaterLog>> = logDao.getAllLogsFlow()

    // Settings reactively (with guarantees that a default exists if none is in DB)
    fun getSettingsFlow(): Flow<WaterSettings> {
        return settingsDao.getSettingsFlow().map { dbSettings ->
            if (dbSettings == null) {
                val defaultSettings = WaterSettings()
                settingsDao.insertOrUpdateSettings(defaultSettings)
                defaultSettings
            } else {
                dbSettings
            }
        }
    }

    // Direct fetch of settings
    suspend fun getSettingsDirect(): WaterSettings {
        val dbSettings = settingsDao.getSettingsDirect()
        return if (dbSettings == null) {
            val defaultSettings = WaterSettings()
            settingsDao.insertOrUpdateSettings(defaultSettings)
            defaultSettings
        } else {
            dbSettings
        }
    }

    // Operations
    suspend fun insertLog(amountMl: Int) {
        val log = WaterLog(amountMl = amountMl)
        logDao.insertLog(log)
    }

    suspend fun deleteLog(id: Int) {
        logDao.deleteLog(id)
    }

    suspend fun undoLastLog() {
        logDao.deleteLastLog()
    }

    suspend fun clearLogs() {
        logDao.clearAllLogs()
    }

    suspend fun updateSettings(
        targetMl: Int,
        intervalMinutes: Int,
        isNotificationsEnabled: Boolean
    ) {
        val current = getSettingsDirect()
        val updated = current.copy(
            dailyTargetMl = targetMl,
            reminderIntervalMinutes = intervalMinutes,
            isNotificationsEnabled = isNotificationsEnabled
        )
        settingsDao.insertOrUpdateSettings(updated)
    }

    suspend fun setReminderPending(pending: Boolean) {
        // Initialize settings if they do not exist
        getSettingsDirect()
        settingsDao.updateReminderPending(pending)
    }

    suspend fun updateLastReminderTimestamp(timestamp: Long) {
        getSettingsDirect()
        settingsDao.updateLastReminderTimestamp(timestamp)
    }
}
