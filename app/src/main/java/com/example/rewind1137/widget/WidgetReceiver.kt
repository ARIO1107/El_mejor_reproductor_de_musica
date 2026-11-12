package com.example.rewind1137.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.rewind1137.MainActivity
import com.example.rewind1137.service.MusicService

class WidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetAction = intent.action ?: return

        if (widgetAction == ACTION_OPEN_APP) {
            openApp(context)
            return
        }

        // Recupera el ultimo estado persistido si el proceso estaba muerto.
        RewindWidgetState.restore(context)

        // Sin cancion cargada no hay nada que controlar: abrimos la app para
        // que el usuario elija musica en lugar de dejar el boton sin respuesta.
        if (!RewindWidgetState.hasSong) {
            openApp(context)
            return
        }

        val serviceAction = when (widgetAction) {
            ACTION_PLAY_PAUSE -> MusicService.ACTION_PLAY_PAUSE
            ACTION_NEXT -> MusicService.ACTION_NEXT
            ACTION_PREVIOUS -> MusicService.ACTION_PREVIOUS
            ACTION_TOGGLE_FAVORITE -> MusicService.ACTION_TOGGLE_FAVORITE
            ACTION_TOGGLE_MODE -> MusicService.ACTION_TOGGLE_MODE
            else -> return
        }

        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = serviceAction
            if (serviceAction == MusicService.ACTION_TOGGLE_FAVORITE) {
                putExtra("SONG_ID", RewindWidgetState.currentSongId)
            }
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun openApp(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        runCatching { context.startActivity(launchIntent) }
    }
}
