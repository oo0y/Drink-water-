package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "water_settings")
data class WaterSettings(
    @PrimaryKey val id: Int = 1, // Singleton settings
    val dailyTargetMl: Int = 2000,
    val reminderIntervalMinutes: Int = 90, // 1.5 hours by default (ساعة ونصف)
    val isReminderPending: Boolean = false, // When true, forces the unescapable popup to show up!
    val lastReminderTimestamp: Long = System.currentTimeMillis(),
    val isNotificationsEnabled: Boolean = true
) : Serializable
