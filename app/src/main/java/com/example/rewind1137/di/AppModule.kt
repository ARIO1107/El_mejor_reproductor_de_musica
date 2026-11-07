package com.example.rewind1137.di

import android.content.Context
import androidx.room.Room
import com.example.rewind1137.audio.AudioEffectManager
import com.example.rewind1137.data.database.AppDatabase
import com.example.rewind1137.data.database.dao.CacheMetadataDao
import com.example.rewind1137.data.repository.HybridMusicRepository
import com.example.rewind1137.data.repository.SettingsRepository
import com.example.rewind1137.ui.connection.PlaybackConnection
import com.example.rewind1137.utils.MusicPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "re_pro_database"
        )
            .addMigrations(AppDatabase.getMigration12())
            .build()
    }

    @Provides
    @Singleton
    fun provideCacheMetadataDao(database: AppDatabase): CacheMetadataDao {
        return database.cacheMetadataDao()
    }

    @Provides
    @Singleton
    fun provideHybridMusicRepository(
        @ApplicationContext context: Context,
        cacheMetadataDao: CacheMetadataDao
    ): HybridMusicRepository {
        return HybridMusicRepository(context, cacheMetadataDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): androidx.media3.exoplayer.ExoPlayer {
        return MusicPlayerManager.getPlayer(context)
    }

    @Provides
    @Singleton
    fun provideAudioEffectManager(exoPlayer: androidx.media3.exoplayer.ExoPlayer): AudioEffectManager {
        return AudioEffectManager(exoPlayer)
    }
}
