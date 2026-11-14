package com.sxanalys.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.sxanalys.music.playback.MusicService

/** Liquid glass player widget. Shows the last played song even when the player is not running. */
class BubbleWidgetReceiver : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            refresh(context, goAsync())
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, goAsync())
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refresh(context, goAsync())
    }

    private fun refresh(context: Context, result: BroadcastReceiver.PendingResult) {
        Thread {
            try {
                // Draw the saved last song first, so the widget is never empty after a restart
                GlassWidget.render(context)
                if (MusicService.isRunning) {
                    context.startService(
                        Intent(context, MusicService::class.java).apply {
                            action = MusicWidgetReceiver.ACTION_UPDATE_WIDGET
                        },
                    )
                }
            } catch (e: Exception) {
                // Service might be restricted in background
            } finally {
                result.finish()
            }
        }.start()
    }
}
