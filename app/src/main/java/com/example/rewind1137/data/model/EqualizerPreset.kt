package com.example.rewind1137.data.model

data class EqualizerPreset(
    val id: String,
    val name: String,
    val bandLevels: List<Int>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0
) {
    companion object {
        val BUILTIN_PRESETS = listOf(
            EqualizerPreset("builtin_normal", "Normal", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
            EqualizerPreset("builtin_pop", "Pop", listOf(-120, -60, 0, 80, 120, 80, 0, -40, -80, -100)),
            EqualizerPreset("builtin_rock", "Rock", listOf(120, 80, -60, -120, -80, 0, 80, 120, 140, 120)),
            EqualizerPreset("builtin_bass", "Bajos", listOf(180, 140, 80, 20, -40, -60, -20, 40, 60, 80)),
            EqualizerPreset("builtin_vocal", "Vocal", listOf(-80, -40, 0, 60, 120, 140, 120, 80, 40, 0))
        )
    }
}
