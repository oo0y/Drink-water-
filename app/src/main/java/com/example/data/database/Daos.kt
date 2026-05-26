package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getLogsForDayFlow(startOfDay: Long, endOfDay: Long): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    suspend fun getLogsForDay(startOfDay: Long, endOfDay: Long): List<WaterLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)

    @Query("DELETE FROM water_logs WHERE id = (SELECT MAX(id) FROM water_logs)")
    suspend fun deleteLastLog()

    @Query("DELETE FROM water_logs")
    suspend fun clearAllLogs()
}

@Dao
interface WaterSettingsDao {
    @Query("SELECT * FROM water_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<WaterSettings?>

    @Query("SELECT * FROM water_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): WaterSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: WaterSettings)

    @Query("UPDATE water_settings SET isReminderPending = :pending WHERE id = 1")
    suspend fun updateReminderPending(pending: Boolean)

    @Query("UPDATE water_settings SET lastReminderTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastReminderTimestamp(timestamp: Long)
}
