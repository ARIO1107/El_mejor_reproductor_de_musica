package com.example.re_pro.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.re_pro.ui.components.DynamicBackground
import com.example.re_pro.ui.components.NowPlayingAnimationPicker
import com.example.re_pro.viewmodel.NowPlayingAnimation
import com.example.re_pro.viewmodel.SettingsUiState

import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    onBackClick: () -> Unit,
    onExcludeShortClipsChange: (Boolean) -> Unit,
    onMinAudioDurationChange: (Long) -> Unit = {},
    onRescanStorageClick: () -> Unit,
    onOpenEqualizerClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onOpenThemeScreenClick: () -> Unit,
    onOpenFoldersClick: () -> Unit = {},
    onOpenSleepTimerClick: () -> Unit = {},
    onCrossfadeChange: (Int) -> Unit = {},
    onReplayGainChange: (Boolean) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: (Uri) -> Unit = {},
    backupStatus: String? = null,
    onClearBackupStatus: () -> Unit = {},
    albumArtUri: Uri? = null,
    onNowPlayingAnimationChange: (NowPlayingAnimation) -> Unit = {}
) {
    val userSettings = settingsUiState.userSettings
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showAnimationPicker by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) onImportBackup(uri)
    }

    LaunchedEffect(backupStatus) {
        if (backupStatus != null) {
            android.widget.Toast.makeText(context, backupStatus, android.widget.Toast.LENGTH_LONG).show()
            onClearBackupStatus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = albumArtUri)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ajustes y Configuración", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsSectionHeader(title = "Apariencia")
            SettingsClickableItem(
                icon = Icons.Outlined.Palette,
                title = "Tema de la Aplicación",
                subtitle = "Esquema de colores (Actual: ${userSettings.themeMode.title})",
                onClick = onOpenThemeScreenClick
            )

            SettingsClickableItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Animación de Reproducción",
                subtitle = "Indicador de canción en lista y cuadrícula (Actual: ${userSettings.nowPlayingAnimation.title})",
                onClick = { showAnimationPicker = !showAnimationPicker }
            )
            if (showAnimationPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "Elige cómo se indica la canción en reproducción",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                    AnimatedVisibility(visible = showAnimationPicker) {
                        NowPlayingAnimationPicker(
                            current = userSettings.nowPlayingAnimation,
                            onSelect = onNowPlayingAnimationChange
                        )
                    }
                }
            }

            SettingsSectionHeader(title = "Navegación de Audio y Carpetas")

            SettingsClickableItem(
                icon = Icons.Outlined.Folder,
                title = "Explorador de Carpetas y Listas",
                subtitle = "Ver canciones por carpeta local e historial de reproducción",
                onClick = onOpenFoldersClick
            )

            SettingsClickableItem(
                icon = Icons.Outlined.Timer,
                title = "Temporizador de Sueño (Sleep Timer)",
                subtitle = "Apaga la música automáticamente",
                onClick = onOpenSleepTimerClick
            )

            SettingsSwitchItem(
                icon = Icons.Outlined.AudioFile,
                title = "Excluir audios cortos",
                subtitle = "Ocultar archivos de audio menores a ${(userSettings.minAudioDurationMs / 1000).toInt()}s",
                checked = userSettings.excludeShortAudios,
                onCheckedChange = onExcludeShortClipsChange
            )

            if (userSettings.excludeShortAudios) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duración mínima",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(userSettings.minAudioDurationMs / 1000).toInt()}s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = userSettings.minAudioDurationMs.toFloat(),
                        onValueChange = { onMinAudioDurationChange(it.toLong()) },
                        valueRange = 5000f..120000f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SettingsClickableItem(
                icon = Icons.Outlined.Refresh,
                title = "Reescanear almacenamiento",
                subtitle = "Buscar nuevos archivos de música en el dispositivo",
                onClick = onRescanStorageClick
            )

            SettingsSectionHeader(title = "Sonido")
            SettingsClickableItem(
                icon = Icons.Outlined.Equalizer,
                title = "Ecualizador del Sistema",
                subtitle = "Ajusta los graves, agudos y frecuencias de audio",
                onClick = onOpenEqualizerClick
            )

            // Crossfade slider
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crossfade",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (userSettings.crossfadeSeconds == 0) "Desactivado"
                               else "${userSettings.crossfadeSeconds}s",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = userSettings.crossfadeSeconds.toFloat(),
                    onValueChange = { onCrossfadeChange(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ReplayGain toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ReplayGain",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Normalizacion de volumen",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userSettings.replayGainEnabled,
                    onCheckedChange = {
                        onReplayGainChange(it)
                        com.example.re_pro.utils.MusicPlayerManager.setReplayGain(it)
                    }
                )
            }

            SettingsSectionHeader(title = "Backup y Restauracion")
            SettingsClickableItem(
                icon = Icons.Outlined.Backup,
                title = "Exportar Backup",
                subtitle = "Guardar configuracion, favoritas y playlists",
                onClick = onExportBackup
            )
            SettingsClickableItem(
                icon = Icons.Outlined.Restore,
                title = "Importar Backup",
                subtitle = "Restaurar configuracion desde un archivo",
                onClick = { backupLauncher.launch(arrayOf("application/json")) }
            )

            SettingsSectionHeader(title = "Almacenamiento y Rendimiento")
            SettingsClickableItem(
                icon = Icons.Outlined.CleaningServices,
                title = "Limpiar caché de la aplicación",
                subtitle = "Libera espacio temporal de almacenamiento",
                onClick = { showClearCacheDialog = true }
            )

            SettingsSectionHeader(title = "Acerca de Re-Pro")
            SettingsClickableItem(
                icon = Icons.Outlined.Info,
                title = "Versión de la App",
                subtitle = "Re-Pro v2.0 (Edición Comercial)",
                onClick = {}
            )

            SettingsClickableItem(
                icon = Icons.Outlined.Info,
                title = "Licencias de Código Abierto",
                subtitle = "Media3, Jetpack Compose, DataStore, Material 3",
                onClick = { showLicensesDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("Limpiar Caché", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                text = { Text("¿Deseas eliminar todos los datos temporales en caché? Esto no borrará tus canciones locales.", color = MaterialTheme.colorScheme.onBackground) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onClearCacheClick()
                            showClearCacheDialog = false
                        }
                    ) {
                        Text("Limpiar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showLicensesDialog) {
            AlertDialog(
                onDismissRequest = { showLicensesDialog = false },
                title = { Text("Licencias de Software", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Re-Pro utiliza tecnologías de código abierto:", color = MaterialTheme.colorScheme.onBackground)
                        Text("• Android Jetpack Compose (Apache 2.0)", color = MaterialTheme.colorScheme.onBackground)
                        Text("• AndroidX Media3 ExoPlayer (Apache 2.0)", color = MaterialTheme.colorScheme.onBackground)
                        Text("• Jetpack DataStore Preferences (Apache 2.0)", color = MaterialTheme.colorScheme.onBackground)
                        Text("• Material Design 3 Components (Apache 2.0)", color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLicensesDialog = false }) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
