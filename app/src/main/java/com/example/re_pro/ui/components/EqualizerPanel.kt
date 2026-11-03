package com.example.re_pro.ui.components

import android.media.audiofx.Equalizer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.re_pro.audio.AudioEffectManager
import kotlinx.coroutines.delay

data class BandInfo(
    val frequencyHz: String,
    val index: Int,
    var levelDb: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerPanel(
    audioEffectManager: AudioEffectManager,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val eqState = remember { mutableStateOf<List<BandInfo>>(emptyList()) }
    var expanded by remember { mutableStateOf(true) }

    val bands = audioEffectManager.getNumberOfBands()

    LaunchedEffect(bands) {
        if (bands > 0) {
            val bandList = mutableListOf<BandInfo>()
            val bandLevelRange = audioEffectManager.getBandLevelRange()
            for (i in 0 until bands) {
                val level = audioEffectManager.getBandLevel(i).toFloat()
                bandList.add(
                    BandInfo(
                        frequencyHz = formatFrequency(i, bands),
                        index = i,
                        levelDb = level
                    )
                )
            }
            eqState.value = bandList
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Cleanup handled by AudioEffectManager
        }
    }

    if (expanded) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ecualizador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { expanded = false }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Band count selector
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presetBands = listOf("10", "15", "31", "All")
                    presetBands.forEach { bandCount ->
                        FilterChip(
                            selected = false,
                            onClick = { /* TODO: switch band count */ },
                            label = { Text(bandCount, color = MaterialTheme.colorScheme.onBackground) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frequency band sliders
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(eqState.value) { band ->
                        BandSlider(
                            bandInfo = band,
                            onLevelChange = { newLevel ->
                                val clamped = newLevel.coerceIn(-1000f, 0f)
                                audioEffectManager.setBandLevel(band.index, clamped.toInt().toShort())
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preset buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf("Normal", "Rock", "Pop", "Jazz", "Classical")
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = { /* TODO: apply preset */ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(preset, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    } else {
        TopBarRePro(
            onSettingsClick = {}
        )
    }
}

@Composable
fun BandSlider(
    bandInfo: BandInfo,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableFloatStateOf(bandInfo.levelDb) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bandInfo.frequencyHz,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${currentLevel.toInt()} dB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = currentLevel,
                onValueChange = { newVal ->
                    currentLevel = newVal
                    onLevelChange(newVal)
                },
                valueRange = -1000f..0f,
                steps = 20,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

private fun formatFrequency(index: Int, totalBands: Int): String {
    val frequencies = arrayOf(
        "60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz",
        "68 Hz", "170 Hz", "400 Hz", "1 kHz", "2.5 kHz", "6 kHz"
    )
    return if (index < frequencies.size) frequencies[index] else "${(index + 1) * 100} Hz"
}