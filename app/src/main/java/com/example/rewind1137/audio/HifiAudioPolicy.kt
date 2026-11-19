package com.example.rewind1137.audio

import com.example.rewind1137.data.model.UserSettings

/**
 * Define el comportamiento "HiFi transparente": sin DSP de coloración, con ReplayGain y
 * salida float cuando es posible. No implica másteres externos ni conversión lossy→lossless.
 */
object HifiAudioPolicy {

    data class Effective(
        val equalizer: Boolean,
        val bassBoost: Boolean,
        val virtualizer: Boolean,
        val vocalRemoval: Boolean,
        val replayGain: Boolean,
        val floatOutput: Boolean
    )

    fun resolve(settings: UserSettings, vocalRemovalEnabled: Boolean): Effective {
        return if (settings.hifiModeEnabled) {
            Effective(
                equalizer = false,
                bassBoost = false,
                virtualizer = false,
                vocalRemoval = false,
                replayGain = true,
                floatOutput = true
            )
        } else {
            Effective(
                equalizer = settings.equalizerEnabled,
                bassBoost = settings.bassBoostEnabled,
                virtualizer = settings.virtualizerEnabled,
                vocalRemoval = vocalRemovalEnabled,
                replayGain = settings.replayGainEnabled,
                floatOutput = false
            )
        }
    }
}
