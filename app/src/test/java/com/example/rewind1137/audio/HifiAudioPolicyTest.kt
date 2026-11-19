package com.example.rewind1137.audio

import com.example.rewind1137.data.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifiAudioPolicyTest {

    @Test
    fun `hifi on disables all dsp and enables replay gain and float output`() {
        val settings = UserSettings(
            equalizerEnabled = true,
            bassBoostEnabled = true,
            virtualizerEnabled = true,
            replayGainEnabled = false,
            hifiModeEnabled = true
        )

        val effective = HifiAudioPolicy.resolve(settings, vocalRemovalEnabled = true)

        assertFalse(effective.equalizer)
        assertFalse(effective.bassBoost)
        assertFalse(effective.virtualizer)
        assertFalse(effective.vocalRemoval)
        assertTrue(effective.replayGain)
        assertTrue(effective.floatOutput)
    }

    @Test
    fun `hifi off respects user settings and disables float output`() {
        val settings = UserSettings(
            equalizerEnabled = false,
            bassBoostEnabled = true,
            virtualizerEnabled = false,
            replayGainEnabled = false,
            hifiModeEnabled = false
        )

        val effective = HifiAudioPolicy.resolve(settings, vocalRemovalEnabled = true)

        assertFalse(effective.equalizer)
        assertTrue(effective.bassBoost)
        assertFalse(effective.virtualizer)
        assertTrue(effective.vocalRemoval)
        assertFalse(effective.replayGain)
        assertFalse(effective.floatOutput)
    }

    @Test
    fun `vocal removal is respected only when hifi is off`() {
        val hifiOff = UserSettings(hifiModeEnabled = false)
        val hifiOn = UserSettings(hifiModeEnabled = true)

        assertTrue(HifiAudioPolicy.resolve(hifiOff, vocalRemovalEnabled = true).vocalRemoval)
        assertFalse(HifiAudioPolicy.resolve(hifiOn, vocalRemovalEnabled = true).vocalRemoval)
    }
}
