package com.example.rewind1137.domain.usecase

import com.example.rewind1137.domain.model.LibraryFilter
import com.example.rewind1137.domain.model.Song
import com.example.rewind1137.domain.model.SortOption
import com.example.rewind1137.domain.repository.MusicRepository
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(excludeShortClips: Boolean = false): List<Song> {
        return repository.fetchLocalSongs(excludeShortClips)
    }

    fun filterAndSort(
        songs: List<Song>,
        query: String,
        sortOption: SortOption,
        filter: LibraryFilter,
        favorites: Set<Long>
    ): List<Song> {
        var result = if (filter == LibraryFilter.FAVORITES) {
            songs.filter { favorites.contains(it.id) }
        } else {
            songs
        }

        if (query.isNotBlank()) {
            result = result.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)
            }
        }

        return when (sortOption) {
            SortOption.DATE_ADDED_DESC -> result.sortedByDescending { it.dateAdded }
            SortOption.TITLE_ASC -> result.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> result.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> result.sortedBy { it.artist.lowercase() }
            SortOption.DURATION_DESC -> result.sortedByDescending { it.duration }
        }
    }
}
