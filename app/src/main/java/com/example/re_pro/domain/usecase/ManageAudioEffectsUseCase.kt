package com.example.re_pro.domain.usecase

import com.example.re_pro.audio.AudioEffectManager
import javax.inject.Inject

class ManageAudioEffectsUseCase @Inject constructor(
    private val audioEffectManager: AudioEffectManager
) {
    fun init() = audioEffectManager.initEffects()
    fun toggleEqualizer(enabled: Boolean) = audioEffectManager.setEqualizerEnabled(enabled)
    fun toggleBassBoost(enabled: Boolean) = audioEffectManager.setBassBoostEnabled(enabled)
    fun setBassBoostStrength(strength: Int) = audioEffectManager.setBassBoostStrength(strength)
    fun setVirtualizerStrength(strength: Int) = audioEffectManager.setVirtualizerStrength(strength)
    fun release() = audioEffectManager.release()
    fun getManager(): AudioEffectManager = audioEffectManager
}
