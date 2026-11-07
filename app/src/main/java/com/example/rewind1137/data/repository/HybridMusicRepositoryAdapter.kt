package com.example.rewind1137.data.repository

import com.example.rewind1137.domain.model.PlaybackSource
import com.example.rewind1137.domain.model.Song as DomainSong
import com.example.rewind1137.domain.repository.MusicRepository
import kotlinx.coroutines.runBlocking

class HybridMusicRepositoryAdapter(
    private val delegate: HybridMusicRepository
) : MusicRepository {

    override suspend fun fetchLocalSongs(excludeShortClips: Boolean): List<DomainSong> {
        return delegate.fetchLocalSongs(excludeShortClips).map { it.toDomain() }
    }

    override suspend fun getOfflineQueue(): List<DomainSong> {
        return delegate.getOfflineQueue().map { it.toDomain() }
    }

    override suspend fun batchCacheSongs(songs: List<DomainSong>) {
        delegate.batchCacheSongs(songs.map { it.toData() })
    }

    override fun getPlaybackSource(preferLocal: Boolean, forceRemote: Boolean): PlaybackSource {
        return runBlocking {
            when (val source = delegate.getPlaybackSource(preferLocal, forceRemote)) {
                is HybridMusicRepository.PlaybackSource.Local ->
                    PlaybackSource.Local(source.songs.map { it.toDomain() })
                is HybridMusicRepository.PlaybackSource.Remote ->
                    PlaybackSource.Remote(source.songs.map { it.toDomain() })
                is HybridMusicRepository.PlaybackSource.Hybrid ->
                    PlaybackSource.Hybrid(
                        localSongs = source.localSongs.map { it.toDomain() },
                        remoteAvailable = source.remoteAvailable
                    )
            }
        }
    }

    private fun com.example.rewind1137.data.model.Song.toDomain(): DomainSong {
        return DomainSong(
            id = id,
            title = title,
            artist = artist,
            uri = uri,
            duration = duration,
            albumId = albumId,
            albumArtUri = albumArtUri,
            dateAdded = dateAdded
        )
    }

    private fun DomainSong.toData(): com.example.rewind1137.data.model.Song {
        return com.example.rewind1137.data.model.Song(
            id = id,
            title = title,
            artist = artist,
            uri = uri,
            duration = duration,
            albumId = albumId,
            albumArtUri = albumArtUri,
            dateAdded = dateAdded
        )
    }
}
