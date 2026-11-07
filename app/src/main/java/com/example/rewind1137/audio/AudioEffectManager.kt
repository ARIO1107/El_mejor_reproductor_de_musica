package com.example.rewind1137.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.rewind1137.data.model.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AudioEffectConfig(
    val equalizerEnabled: Boolean = true,
    val bassBoostEnabled: Boolean = false,
    val virtualizerEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0
)

@OptIn(UnstableApi::class)
class AudioEffectManager(private val player: ExoPlayer) {
    companion object {
        private const val TAG = "AudioEffectManager"
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _effectState = MutableStateFlow(AudioEffectConfig())
    val effectState: StateFlow<AudioEffectConfig> = _effectState

    private var audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var pendingInitialization = false

    fun initEffects() {
        audioSessionId = player.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            Log.w(TAG, "Audio session ID not available yet, scheduling retry")
            pendingInitialization = true
            return
        }
        pendingInitialization = false
        initializeEffects()
    }

    /**
     * Retry initialization when audio session becomes available.
     * Called from Player.Listener.onAudioSessionIdChanged().
     * @param presetToRestore Optional preset to re-apply after re-initialization.
     */
    fun onAudioSessionIdChanged(presetToRestore: EqualizerPreset? = null) {
        val newSessionId = player.audioSessionId
        if (newSessionId != C.AUDIO_SESSION_ID_UNSET) {
            audioSessionId = newSessionId
            if (pendingInitialization) {
                initializeEffects()
                pendingInitialization = false
                presetToRestore?.let { applyPreset(it) }
            }
        }
    }

    private fun initializeEffects() {
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _effectState.value.equalizerEnabled
            }
            Log.d(TAG, "Equalizer initialized with ${equalizer?.numberOfBands} bands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Equalizer", e)
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _effectState.value.bassBoostEnabled
                setStrength(_effectState.value.bassBoostStrength.toShort())
            }
            Log.d(TAG, "BassBoost initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BassBoost", e)
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _effectState.value.virtualizerEnabled
                setStrength(_effectState.value.virtualizerStrength.toShort())
            }
            Log.d(TAG, "Virtualizer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Virtualizer", e)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _effectState.value = _effectState.value.copy(equalizerEnabled = enabled)
        equalizer?.enabled = enabled
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        _effectState.value = _effectState.value.copy(bassBoostEnabled = enabled)
        bassBoost?.enabled = enabled
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        _effectState.value = _effectState.value.copy(virtualizerEnabled = enabled)
        virtualizer?.enabled = enabled
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 100)
        _effectState.value = _effectState.value.copy(bassBoostStrength = clamped)
        bassBoost?.setStrength(clamped.toShort())
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 100)
        _effectState.value = _effectState.value.copy(virtualizerStrength = clamped)
        virtualizer?.setStrength(clamped.toShort())
    }

    fun getBassBoostAvailable(): Boolean {
        return bassBoost != null
    }

    fun getBandLevelRange(): ShortArray? {
        return equalizer?.bandLevelRange
    }

    fun getNumberOfBands(): Int {
        return equalizer?.numberOfBands?.toInt() ?: 0
    }

    fun setBandLevel(bandIndex: Int, levelInDb: Short) {
        equalizer?.setBandLevel(bandIndex.toShort(), levelInDb)
    }

    fun getBandLevel(bandIndex: Int): Short {
        return equalizer?.getBandLevel(bandIndex.toShort()) ?: 0
    }

    fun applyPreset(preset: EqualizerPreset) {
        val range = equalizer?.bandLevelRange ?: return
        val minDb = range[0].toInt()
        val maxDb = range[1].toInt()
        val numBands = equalizer?.numberOfBands?.toInt() ?: 0

        for (i in 0 until numBands) {
            val level = preset.bandLevels.getOrElse(i) { 0 }.coerceIn(minDb, maxDb)
            try {
                equalizer?.setBandLevel(i.toShort(), level.toShort())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set band $i level", e)
            }
        }

        setBassBoostStrength(preset.bassBoost)
        setVirtualizerStrength(preset.virtualizer)

        Log.d(TAG, "Applied preset '${preset.name}' (${numBands} bands)")
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects", e)
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
        }
    }

    fun getAudioSessionId(): Int = audioSessionId
}