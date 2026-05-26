package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.DatabaseProvider
import com.example.data.database.WaterLog
import com.example.data.repository.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterAlarmReceiver : BroadcastReceiver() {
    private val TAG = "WaterAlarmReceiver"
    private val CHANNEL_ID = "water_reminder_channel_v1"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received!")

        val action = intent.action
        if (action == "ACTION_DRANK_QUICK") {
            // User clicked the quick "I drank 250ml" notification action button
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DatabaseProvider.getDatabase(context)
                    val repository = WaterRepository(db.waterLogDao(), db.waterSettingsDao())
                    
                    // Log water
                    repository.insertLog(250)
                    
                    // Mark reminder as cleared
                    repository.setReminderPending(false)
                    
                    // Reschedule
                    val settings = repository.getSettingsDirect()
                    WaterAlarmHelper.scheduleNextAlarm(context, settings.reminderIntervalMinutes)
                    
                    // Remove notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(1001)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ACTION_DRANK_QUICK", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Standard alarm trigger
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseProvider.getDatabase(context)
                val repository = WaterRepository(db.waterLogDao(), db.waterSettingsDao())
                val settings = repository.getSettingsDirect()

                // Mark reminder as pending/active in DB (forces pop-up overlay when entering the app)
                repository.setReminderPending(true)
                repository.updateLastReminderTimestamp(System.currentTimeMillis())

                if (settings.isNotificationsEnabled) {
                    sendNotification(context)
                }

                // Schedule next periodic reminder automatically
                WaterAlarmHelper.scheduleNextAlarm(context, settings.reminderIntervalMinutes)

            } catch (e: Exception) {
                Log.e(TAG, "Error in onReceive processing", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel if Android Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات شرب الماء (رواء)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة مخصصة لإشعارك عند حان وقت شرب الماء بانتظام."
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open Main Screen (this will trigger the full overlay because isReminderPending = true)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            2020,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to log 250ml water immediately from notification
        val quickDrankIntent = Intent(context, WaterAlarmReceiver::class.java).apply {
            action = "ACTION_DRANK_QUICK"
        }
        val quickDrankPendingIntent = PendingIntent.getBroadcast(
            context,
            2021,
            quickDrankIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard system icon, or custom later
            .setContentTitle("حان وقت شرب الماء! 💧")
            .setContentText("مرّت ساعة ونصف! حافظ على رطوبة جسمك واشرب كأس ماء الآن لتجنب الجفاف.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "مرّت ساعة ونصف! جسمك يحتاج إلى ترطيب الآن. لا تنسى شرب كأس من الماء العذب للمحافظة على كليتيك وصحتك العامة وضغط الدم."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openAppPendingIntent, true) // Force popup overlay window output
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "لقد شربت 250 مل 👍",
                quickDrankPendingIntent
            )
            .build()

        notificationManager.notify(1001, notification)
    }
}

object WaterAlarmHelper {
    private const val REQUEST_CODE = 4040

    fun scheduleNextAlarm(context: Context, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)

        // Determine if we can use exact alarm (needs permission on S/31+)
        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canUseExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.d("WaterAlarmHelper", "Scheduled *exact* next alarm in $intervalMinutes minutes.")
            } else {
                // Fallback to non-exact but allowed while idle
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.d("WaterAlarmHelper", "Scheduled *non-exact* next alarm in $intervalMinutes minutes (App doesn't hold Schedule Exact Alarm permission).")
            }
        } catch (e: SecurityException) {
            // Last resort fallback
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.e("WaterAlarmHelper", "Failed scheduling exact alarm due to SecurityException, fell back to safe non-exact schedule.", e)
            } catch (ex: Exception) {
                Log.e("WaterAlarmHelper", "Failed scheduling alarm altogether.", ex)
            }
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("WaterAlarmHelper", "Active alarm cancelled.")
        }
    }
}
