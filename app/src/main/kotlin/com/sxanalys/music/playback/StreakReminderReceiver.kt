package com.sxanalys.music.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sxanalys.music.utils.StreakReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreakReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != StreakReminder.ACTION) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                StreakReminder.remindIfNeeded(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
