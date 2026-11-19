package com.example.rewind1137.widget

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.rewind1137.viewmodel.PlaybackMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RewindWidgetStateClearTest {

    @Before
    fun reset() {
        // Simula un proceso nuevo antes de cada prueba.
        val field = RewindWidgetState::class.java.getDeclaredField("initialized")
        field.isAccessible = true
        field.setBoolean(RewindWidgetState, false)

        RewindWidgetState.songTitle = "Rewind 1137"
        RewindWidgetState.songArtist = "Tu musica, tu estilo"
        RewindWidgetState.songAlbum = ""
        RewindWidgetState.isPlaying = false
        RewindWidgetState.hasSong = false
        RewindWidgetState.progressMs = 0L
        RewindWidgetState.durationMs = 0L
        RewindWidgetState.albumArt = null
        RewindWidgetState.upNext = emptyList()
        RewindWidgetState.queueCount = 0
        RewindWidgetState.currentSongId = -1L
        RewindWidgetState.isFavorite = false
        RewindWidgetState.playbackMode = PlaybackMode.SEQUENTIAL
        RewindWidgetState.songUri = null
        RewindWidgetState.albumArtUri = null
    }

    @Test
    fun clear_resetsInMemoryStateAndPersistedHasSong() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        RewindWidgetState.songTitle = "Canción"
        RewindWidgetState.songArtist = "Artista"
        RewindWidgetState.songAlbum = "Álbum"
        RewindWidgetState.hasSong = true
        RewindWidgetState.isPlaying = true
        RewindWidgetState.progressMs = 12_000L
        RewindWidgetState.durationMs = 180_000L
        RewindWidgetState.currentSongId = 42L
        RewindWidgetState.isFavorite = true
        RewindWidgetState.queueCount = 5
        RewindWidgetState.upNext = listOf("Siguiente", "Otra")
        RewindWidgetState.playbackMode = PlaybackMode.SHUFFLE
        RewindWidgetState.songUri = Uri.parse("content://media/1")
        RewindWidgetState.albumArtUri = Uri.parse("content://art/1")

        val init = RewindWidgetState::class.java.getDeclaredField("initialized")
        init.isAccessible = true
        init.setBoolean(RewindWidgetState, true)
        RewindWidgetState.save(context)

        RewindWidgetState.clear(context)

        assertFalse(RewindWidgetState.hasSong)
        assertEquals(-1L, RewindWidgetState.currentSongId)
        assertEquals("Rewind 1137", RewindWidgetState.songTitle)
        assertEquals("Tu musica, tu estilo", RewindWidgetState.songArtist)
        assertTrue(RewindWidgetState.upNext.isEmpty())
        assertEquals(0, RewindWidgetState.queueCount)

        // Simula proceso nuevo: restore no debe recuperar la canción muerta.
        init.setBoolean(RewindWidgetState, false)
        RewindWidgetState.restore(context)

        assertFalse(RewindWidgetState.hasSong)
        assertEquals(-1L, RewindWidgetState.currentSongId)
        assertEquals("Rewind 1137", RewindWidgetState.songTitle)
        assertTrue(RewindWidgetState.upNext.isEmpty())
    }
}
