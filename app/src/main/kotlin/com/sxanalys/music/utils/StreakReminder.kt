package com.sxanalys.music.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sxanalys.music.App
import com.sxanalys.music.MainActivity
import com.sxanalys.music.R
import com.sxanalys.music.constants.StreakReminderEnabledKey
import com.sxanalys.music.constants.StreakReminderHourKey
import com.sxanalys.music.db.MusicDatabase
import com.sxanalys.music.playback.StreakReminderReceiver
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Calendar

object StreakReminder {
    const val DEFAULT_HOUR = 19
    const val ACTION = "com.sxanalys.music.action.STREAK_REMINDER"
    private const val CHANNEL_ID = "streak_reminder"
    private const val NOTIFICATION_ID = 7421
    private const val REQUEST_CODE = 7421

    suspend fun schedule(context: Context) {
        val settings = context.dataStore.data.first()
        val enabled = settings[StreakReminderEnabledKey] ?: true
        if (enabled) {
            val hour = (settings[StreakReminderHourKey] ?: DEFAULT_HOUR).coerceIn(0, 23)
            scheduleAt(context, hour)
        } else {
            cancel(context)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun scheduleAt(context: Context, hour: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent(context))
    }

    suspend fun remindIfNeeded(context: Context) {
        val app = context.applicationContext as? App
        if (app != null) {
            val days =
                MusicDatabase(app.internalDatabase.get()).listeningDays().first()
            val today = LocalDate.now()
            val listenedToday = days.any { runCatching { LocalDate.parse(it) }.getOrNull() == today }
            if (!listenedToday) notify(context)
        }
        schedule(context)
    }

    private fun notify(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.streak_reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.small_icon)
                .setContentTitle("Streak lu belum nyala")
                .setContentText("Ayo nyalain streak lu wak")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, StreakReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
