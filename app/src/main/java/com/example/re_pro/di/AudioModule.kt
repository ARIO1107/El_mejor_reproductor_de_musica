package com.example.re_pro.di

import android.content.Context
import com.example.re_pro.audio.AudioFocusManager
import com.example.re_pro.audio.MediaSessionManager
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
