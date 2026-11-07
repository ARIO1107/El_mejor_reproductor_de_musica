package com.example.rewind1137.di

import com.example.rewind1137.data.repository.HybridMusicRepository
import com.example.rewind1137.data.repository.HybridMusicRepositoryAdapter
import com.example.rewind1137.data.repository.SettingsRepositoryAdapter
import com.example.rewind1137.domain.repository.MusicRepository
import com.example.rewind1137.domain.repository.SettingsRepository as DomainSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: HybridMusicRepositoryAdapter): MusicRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryAdapter): DomainSettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideMusicRepositoryAdapter(
            hybridMusicRepository: HybridMusicRepository
        ): HybridMusicRepositoryAdapter {
            return HybridMusicRepositoryAdapter(hybridMusicRepository)
        }

        @Provides
        @Singleton
        fun provideSettingsRepositoryAdapter(
            settingsRepository: com.example.rewind1137.data.repository.SettingsRepository
        ): SettingsRepositoryAdapter {
            return SettingsRepositoryAdapter(settingsRepository)
        }
    }
}
