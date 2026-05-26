package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WaterLog::class,
        WaterSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao
    abstract fun waterSettingsDao(): WaterSettingsDao
}
