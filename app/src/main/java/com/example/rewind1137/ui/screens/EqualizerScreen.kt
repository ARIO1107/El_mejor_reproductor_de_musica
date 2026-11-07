package com.example.rewind1137.ui.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.audio.AudioEffectManager
import com.example.rewind1137.data.model.EqualizerPreset
import com.example.rewind1137.ui.components.DynamicBackground
import com.example.rewind1137.ui.components.GlassmorphicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    audioEffectManager: AudioEffectManager,
    onBackClick: () -> Unit,
    userPresets: List<EqualizerPreset>,
    activePresetId: String,
    onSavePreset: (EqualizerPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSelectPreset: (String) -> Unit,
    onUpdatePreset: (EqualizerPreset) -> Unit,
    onToggleEqualizer: (Boolean) -> Unit = {},
    albumArtUri: Uri? = null
) {
    val activePreset = remember(activePresetId, userPresets) {
        userPresets.firstOrNull { it.id == activePresetId }
    }

    val eqEnabled = remember { mutableStateOf(audioEffectManager.effectState.value.equalizerEnabled) }

    val effectConfig by audioEffectManager.effectState.collectAsState()
    LaunchedEffect(effectConfig.equalizerEnabled) {
        eqEnabled.value = effectConfig.equalizerEnabled
    }

    var initialized by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingPresetName by remember { mutableStateOf("") }
    var bassVal by remember(activePresetId) { mutableIntStateOf(activePreset?.bassBoost ?: 0) }
    var virtVal by remember(activePresetId) { mutableIntStateOf(activePreset?.virtualizer ?: 0) }

    DisposableEffect(Unit) {
        try {
            audioEffectManager.initEffects()
        } catch (_: Exception) {}
        initialized = true
        onDispose {
            try { audioEffectManager.release() } catch (_: Exception) {}
        }
    }

    val bands = remember(initialized) { audioEffectManager.getNumberOfBands() }
    val bandRange = remember(initialized) { audioEffectManager.getBandLevelRange() }
    val numBands = bands.coerceIn(0, 10)
    val eqAvailable = initialized && numBands > 0 && bandRange != null

    val minDb = bandRange?.get(0)?.toInt() ?: -1500
    val maxDb = bandRange?.get(1)?.toInt() ?: 1500

    val bandStates = remember(numBands, initialized) {
        (0 until numBands).map { i ->
            val current = try { audioEffectManager.getBandLevel(i).toInt() } catch (_: Exception) { 0 }
            mutableIntStateOf(current)
        }
    }

    fun applyBandValues(values: List<Int>) {
        if (!eqAvailable) return
        for (i in 0 until numBands) {
            val v = values.getOrElse(i) { 0 }.coerceIn(minDb, maxDb)
            bandStates[i].intValue = v
            try {
                audioEffectManager.setBandLevel(i, v.toShort())
            } catch (_: Exception) {}
        }
    }

    fun loadPreset(preset: EqualizerPreset) {
        applyBandValues(preset.bandLevels)
        onSelectPreset(preset.id)
    }

    fun resetEqualizer() {
        val normalPreset = EqualizerPreset.BUILTIN_PRESETS.first()
        applyBandValues(normalPreset.bandLevels)
        try {
            audioEffectManager.setBassBoostStrength(0)
            audioEffectManager.setVirtualizerStrength(0)
        } catch (_: Exception) {}
        bassVal = 0
        virtVal = 0
        onSelectPreset(normalPreset.id)
    }

    fun persistPreset(transform: (EqualizerPreset) -> EqualizerPreset) {
        val active = userPresets.firstOrNull { it.id == activePresetId }
        if (active != null) {
            onUpdatePreset(transform(active))
        }
    }

    LaunchedEffect(activePresetId, eqAvailable) {
        val preset = userPresets.firstOrNull { it.id == activePresetId } ?: return@LaunchedEffect
        if (eqAvailable) {
            applyBandValues(preset.bandLevels)
            try {
                audioEffectManager.setBassBoostStrength(preset.bassBoost)
                audioEffectManager.setVirtualizerStrength(preset.virtualizer)
            } catch (_: Exception) {}
            bassVal = preset.bassBoost
            virtVal = preset.virtualizer
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = albumArtUri)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Ecualizador Pro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text("Ajustes de audio y respuesta de frecuencia", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        if (eqAvailable) {
                            IconButton(onClick = { resetEqualizer() }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reestablecer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                pendingPresetName = ""
                                showSaveDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Guardar preset", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Switch(
                            checked = eqEnabled.value,
                            onCheckedChange = { checked ->
                                eqEnabled.value = checked
                                try {
                                    audioEffectManager.setEqualizerEnabled(checked)
                                    audioEffectManager.setBassBoostEnabled(checked)
                                    audioEffectManager.setVirtualizerEnabled(checked)
                                } catch (_: Exception) {}
                                onToggleEqualizer(checked)
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    windowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                if (!eqAvailable) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ecualizador no disponible. Inicia la rewindducción de una canción primero.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    EqualizerResponseCurve(
                        bandLevels = bandStates.map { it.intValue },
                        minDb = minDb,
                        maxDb = maxDb,
                        enabled = eqEnabled.value,
                        primaryColor = MaterialTheme.colorScheme.primary
                    )
                }

                Text("Presets Predefinidos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userPresets.forEach { preset ->
                        val isBuiltIn = preset.id.startsWith("builtin_")
                        val isSelected = activePresetId == preset.id
                        FilterChip(
                            selected = isSelected,
                            enabled = eqEnabled.value,
                            onClick = { loadPreset(preset) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(preset.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
                                    if (!isBuiltIn) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onDeletePreset(preset.id) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                if (eqAvailable) {
                    Text("Bandas de Frecuencia ($numBands)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz", "68 Hz", "170 Hz", "400 Hz", "1 kHz", "2.5 kHz", "6 kHz", "8 kHz", "12 kHz", "16 kHz", "20 kHz")

                    for (i in 0 until numBands) {
                        val label = bandLabels.getOrElse(i) { "${(i + 1) * 100} Hz" }
                        val state = bandStates[i]
                        val levelDb = state.intValue / 100f

                        GlassmorphicContainer(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            blurRadius = 16.dp,
                            glassAlpha = if (eqEnabled.value) 0.35f else 0.15f,
                            borderAlpha = 0.15f
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (eqEnabled.value) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.getDefault(), "%+.1f dB", levelDb),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (eqEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Slider(
                                    value = state.intValue.toFloat(),
                                    onValueChange = { state.intValue = it.toInt() },
                                    onValueChangeFinished = {
                                        try { audioEffectManager.setBandLevel(i, state.intValue.toShort()) } catch (_: Exception) {}
                                        persistPreset { p ->
                                            val levels = p.bandLevels.toMutableList()
                                            while (levels.size <= i) levels.add(0)
                                            levels[i] = state.intValue
                                            p.copy(bandLevels = levels)
                                        }
                                    },
                                    valueRange = minDb.toFloat()..maxDb.toFloat(),
                                    enabled = eqEnabled.value,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Text("Efectos de Sonido", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                // Bass Boost Card
                GlassmorphicContainer(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    blurRadius = 16.dp,
                    glassAlpha = if (eqEnabled.value) 0.35f else 0.15f,
                    borderAlpha = 0.15f
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = if (eqEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bajos (Bass Boost)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (eqEnabled.value) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$bassVal%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (eqEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(
                            value = bassVal.toFloat(),
                            onValueChange = { bassVal = it.toInt() },
                            onValueChangeFinished = {
                                try { audioEffectManager.setBassBoostStrength(bassVal) } catch (_: Exception) {}
                                persistPreset { it.copy(bassBoost = bassVal) }
                            },
                            valueRange = 0f..100f,
                            enabled = eqEnabled.value
                        )
                    }
                }

                // Virtualizer 3D Card
                GlassmorphicContainer(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    blurRadius = 16.dp,
                    glassAlpha = if (eqEnabled.value) 0.35f else 0.15f,
                    borderAlpha = 0.15f
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SurroundSound, contentDescription = null, tint = if (eqEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Virtualizador 3D", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (eqEnabled.value) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$virtVal%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (eqEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(
                            value = virtVal.toFloat(),
                            onValueChange = { virtVal = it.toInt() },
                            onValueChangeFinished = {
                                try { audioEffectManager.setVirtualizerStrength(virtVal) } catch (_: Exception) {}
                                persistPreset { it.copy(virtualizer = virtVal) }
                            },
                            valueRange = 0f..100f,
                            enabled = eqEnabled.value
                        )
                    }
                }

                Spacer(modifier = Modifier.height(180.dp))
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar preset personalizado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = pendingPresetName,
                    onValueChange = { pendingPresetName = it },
                    label = { Text("Nombre del preset", color = MaterialTheme.colorScheme.onBackground) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingPresetName.isNotBlank()) {
                            val currentLevels = bandStates.map { it.intValue }
                            val preset = EqualizerPreset(
                                id = "custom_${System.currentTimeMillis()}",
                                name = pendingPresetName.trim(),
                                bandLevels = currentLevels,
                                bassBoost = bassVal,
                                virtualizer = virtVal
                            )
                            onSavePreset(preset)
                            showSaveDialog = false
                        }
                    },
                    enabled = pendingPresetName.isNotBlank()
                ) {
                    Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun EqualizerResponseCurve(
    bandLevels: List<Int>,
    minDb: Int,
    maxDb: Int,
    enabled: Boolean,
    primaryColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                drawLine(
                    color = primaryColor.copy(alpha = 0.25f),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.dp.toPx()
                )

                if (bandLevels.isNotEmpty()) {
                    val path = Path()
                    val range = (maxDb - minDb).coerceAtLeast(1).toFloat()
                    val stepX = if (bandLevels.size > 1) width / (bandLevels.size - 1) else width

                    val points = bandLevels.mapIndexed { index, level ->
                        val norm = (level - minDb).toFloat() / range
                        val y = height - (norm * height)
                        val x = index * stepX
                        Offset(x, y)
                    }

                    path.moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2f
                        path.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }

                    val strokeColor = if (enabled) primaryColor else Color.Gray.copy(alpha = 0.5f)

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(strokeColor.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                }
            }
        }
    }
}
