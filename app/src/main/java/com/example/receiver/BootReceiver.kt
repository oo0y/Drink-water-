package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.DatabaseProvider
import com.example.data.repository.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot completed, rescheduling water alarms.")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DatabaseProvider.getDatabase(context)
                    val repository = WaterRepository(db.waterLogDao(), db.waterSettingsDao())
                    val settings = repository.getSettingsDirect()
                    WaterAlarmHelper.scheduleNextAlarm(context, settings.reminderIntervalMinutes)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed rescheduling after reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
