package com.example.re_pro.utils

import kotlinx.coroutines.*

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("Desactivado", 0),
    FIFTEEN_MIN("15 Minutos", 15),
    THIRTY_MIN("30 Minutos", 30),
    SIXTY_MIN("1 Hora", 60),
    END_OF_TRACK("Al finalizar la canción", -1)
}

object SleepTimerManager {
    private var timerJob: Job? = null
    var activeOption: SleepTimerOption = SleepTimerOption.OFF
        private set

    fun startTimer(
        option: SleepTimerOption,
        scope: CoroutineScope,
        onTimerFinished: () -> Unit = {}
    ) {
        cancelTimer()
        activeOption = option

        if (option == SleepTimerOption.OFF) return

        if (option == SleepTimerOption.END_OF_TRACK) {
            return
        }

        val totalMillis = option.minutes * 60 * 1000L
        val fadeDurationMillis = 15_000L
        val runMillis = (totalMillis - fadeDurationMillis).coerceAtLeast(0L)

        timerJob = scope.launch(Dispatchers.Main) {
            delay(runMillis)

            val steps = 30
            val delayPerStep = fadeDurationMillis / steps
            for (i in steps downTo 0) {
                val volume = i.toFloat() / steps.toFloat()
                MusicPlayerManager.setVolume(volume)
                delay(delayPerStep)
            }

            MusicPlayerManager.pause()
            MusicPlayerManager.setVolume(1.0f)
            activeOption = SleepTimerOption.OFF
            onTimerFinished()
        }
    }

    fun stopAfterCurrentTrackFinished() {
        if (activeOption != SleepTimerOption.END_OF_TRACK) return
        activeOption = SleepTimerOption.OFF
        MusicPlayerManager.pause()
        MusicPlayerManager.setVolume(1.0f)
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        MusicPlayerManager.setVolume(1.0f)
        activeOption = SleepTimerOption.OFF
    }
}
