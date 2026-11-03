package com.example.re_pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.re_pro.utils.MusicPlayerManager

class AudioBecomesNoisyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            if (MusicPlayerManager.isPlaying()) {
                MusicPlayerManager.pause()
            }
        }
    }
}
