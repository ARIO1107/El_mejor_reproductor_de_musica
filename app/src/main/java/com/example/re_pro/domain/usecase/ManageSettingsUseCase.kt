package com.example.re_pro.domain.usecase

import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.domain.repository.SettingsRepository
import javax.inject.Inject

class ManageSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend fun setExcludeShortClips(exclude: Boolean) = repository.setExcludeShortClips(exclude)
    suspend fun setThemeMode(themeMode: AppThemeMode) = repository.setThemeMode(themeMode)
    suspend fun setBackgroundImageUri(uri: String?) = repository.setBackgroundImageUri(uri)
    suspend fun setBackgroundBlur(blur: Float) = repository.setBackgroundBlur(blur)
    suspend fun setBackgroundOpacity(opacity: Float) = repository.setBackgroundOpacity(opacity)
    suspend fun setGridColumns(columns: Int) = repository.setGridColumns(columns)
    suspend fun isFirstLaunch(): Boolean = repository.isFirstLaunch()
    suspend fun markFirstLaunchComplete() = repository.setFirstLaunchComplete()
}
