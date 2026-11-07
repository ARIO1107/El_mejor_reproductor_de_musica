package com.example.rewind1137.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class VisualizerData(
    val waveformBytes: FloatArray = FloatArray(0),
    val isPlaying: Boolean = false
)

class AudioVisualizerState {
    private val _visualizerData = MutableStateFlow(VisualizerData())
    val visualizerData: StateFlow<VisualizerData> = _visualizerData

    fun updateWaveform(bytes: FloatArray) {
        _visualizerData.value = VisualizerData(waveformBytes = bytes, isPlaying = true)
    }

    fun setPlaying(playing: Boolean) {
        _visualizerData.value = _visualizerData.value.copy(isPlaying = playing)
    }

    fun clear() {
        _visualizerData.value = VisualizerData()
    }
}