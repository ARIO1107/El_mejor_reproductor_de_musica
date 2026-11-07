package com.example.rewind1137.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.rewind1137.service.MusicService

class WidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            "com.example.rewind1137.ACTION_PLAY_PAUSE" -> MusicService.ACTION_PLAY_PAUSE
            "com.example.rewind1137.ACTION_NEXT" -> MusicService.ACTION_NEXT
            "com.example.rewind1137.ACTION_PREVIOUS" -> MusicService.ACTION_PREVIOUS
            else -> return
        }
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}