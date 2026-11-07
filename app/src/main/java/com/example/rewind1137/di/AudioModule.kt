package com.example.rewind1137.di

import android.content.Context
import com.example.rewind1137.audio.AudioFocusManager
import com.example.rewind1137.audio.MediaSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioFocusManager(@ApplicationContext context: Context): AudioFocusManager {
        return AudioFocusManager(context)
    }

    @Provides
    @Singleton
    fun provideMediaSessionManager(@ApplicationContext context: Context): MediaSessionManager {
        return MediaSessionManager(context)
    }
}
