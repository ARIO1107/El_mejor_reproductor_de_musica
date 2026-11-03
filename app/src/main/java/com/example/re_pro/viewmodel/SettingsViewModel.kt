package com.example.re_pro.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.data.model.EqualizerPreset
import com.example.re_pro.data.model.UserSettings
import com.example.re_pro.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val equalizerPresets: List<EqualizerPreset> = EqualizerPreset.BUILTIN_PRESETS,
    val activeEqualizerPresetId: String = "builtin_normal",
    val backupStatus: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.userSettingsFlow.collectLatest { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepository.equalizerPresetsFlow.collectLatest { presets ->
                _uiState.update { it.copy(equalizerPresets = presets) }
            }
        }
        viewModelScope.launch {
            settingsRepository.activeEqualizerPresetIdFlow.collectLatest { id ->
                _uiState.update { it.copy(activeEqualizerPresetId = id) }
            }
        }
    }

    suspend fun isFirstLaunch(): Boolean = settingsRepository.isFirstLaunch()
    suspend fun markFirstLaunchComplete() = settingsRepository.setFirstLaunchComplete()

    fun setExcludeShortClips(exclude: Boolean) {
        viewModelScope.launch { settingsRepository.setExcludeShortClips(exclude) }
    }

    fun setMinAudioDurationMs(durationMs: Long) {
        viewModelScope.launch { settingsRepository.setMinAudioDurationMs(durationMs) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    fun setBackgroundImageUri(uri: String?) {
        viewModelScope.launch { settingsRepository.setBackgroundImageUri(uri) }
    }

    fun setBackgroundBlur(blur: Float) {
        viewModelScope.launch { settingsRepository.setBackgroundBlur(blur) }
    }

    fun setBackgroundOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.setBackgroundOpacity(opacity) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settingsRepository.setGridColumns(columns) }
    }

    fun setCompactList(compact: Boolean) {
        viewModelScope.launch { settingsRepository.setCompactList(compact) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setCrossfadeSeconds(seconds) }
    }

    fun setReplayGain(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReplayGain(enabled) }
    }

    fun setReplayGainPreamp(preamp: Float) {
        viewModelScope.launch { settingsRepository.setReplayGainPreamp(preamp) }
    }

    fun clearAppCache(context: Context) {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    fun saveEqualizerPreset(preset: EqualizerPreset) {
        viewModelScope.launch { settingsRepository.saveEqualizerPreset(preset) }
    }

    fun deleteEqualizerPreset(presetId: String) {
        viewModelScope.launch { settingsRepository.deleteEqualizerPreset(presetId) }
    }

    fun setActiveEqualizerPreset(presetId: String) {
        viewModelScope.launch { settingsRepository.setActiveEqualizerPreset(presetId) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqualizerEnabled(enabled) }
    }

    fun setAlbumArtMode(mode: AlbumArtMode) {
        viewModelScope.launch { settingsRepository.setAlbumArtMode(mode) }
    }

    fun setNowPlayingAnimation(mode: NowPlayingAnimation) {
        viewModelScope.launch { settingsRepository.setNowPlayingAnimation(mode) }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            try {
                val json = settingsRepository.getAllSettingsJson()
                val fileName = "RePro_Backup_${System.currentTimeMillis()}.json"
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(json.toString(2))
                _uiState.update { it.copy(backupStatus = "Backup guardado en: Downloads/$fileName") }
            } catch (e: Exception) {
                _uiState.update { it.copy(backupStatus = "Error al exportar: ${e.message}") }
            }
        }
    }

    fun importBackup(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()
                    val json = JSONObject(jsonStr)
                    settingsRepository.importSettingsFromJson(json)
                    _uiState.update { it.copy(backupStatus = "Backup importado correctamente") }
                } else {
                    _uiState.update { it.copy(backupStatus = "No se pudo leer el archivo") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(backupStatus = "Error al importar: ${e.message}") }
            }
        }
    }

    fun clearBackupStatus() {
        _uiState.update { it.copy(backupStatus = null) }
    }
}
