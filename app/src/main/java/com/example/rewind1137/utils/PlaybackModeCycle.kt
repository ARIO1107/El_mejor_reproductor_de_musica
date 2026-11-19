package com.example.rewind1137.utils

import com.example.rewind1137.viewmodel.PlaybackMode

/** Ciclo compartido entre la UI y el servicio para el botón de modo de reproducción. */
object PlaybackModeCycle {
    fun next(mode: PlaybackMode): PlaybackMode = when (mode) {
        PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
        PlaybackMode.SHUFFLE -> PlaybackMode.REPEAT_ONE
        PlaybackMode.REPEAT_ONE -> PlaybackMode.SEQUENTIAL
    }
}
