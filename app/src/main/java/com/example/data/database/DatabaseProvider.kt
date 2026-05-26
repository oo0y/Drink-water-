package com.example.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room

object DatabaseProvider {
    private const val TAG = "DatabaseProvider"

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "server_mod_database"
            )
            .fallbackToDestructiveMigration()
            .build()
            INSTANCE = instance
            instance
        }
    }
}
