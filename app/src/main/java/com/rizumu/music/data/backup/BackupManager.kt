package com.rizumu.music.data.backup

import com.rizumu.music.data.local.UserLibrary
import com.rizumu.music.data.local.playlist.PlaylistStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RestoreSummary(
    val favorites: Int,
    val recent: Int,
    val playlists: Int,
    val tracks: Int,
)

object BackupManager {

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val library = UserLibrary.state.value
        BackupCodec.encode(
            BackupPayload(
                exportedAt = System.currentTimeMillis(),
                favorites = library.favorites,
                recent = library.recent,
                searches = library.recentSearches,
                playlists = PlaylistStore.snapshot(),
            ),
        )
    }

    suspend fun restore(raw: String): RestoreSummary = withContext(Dispatchers.IO) {
        val payload = BackupCodec.decode(raw)
            ?: throw IllegalArgumentException("File backup tidak valid atau versinya tidak didukung.")
        UserLibrary.mergeBackup(
            favorites = payload.favorites,
            recent = payload.recent,
            searches = payload.searches,
        )
        val createdPlaylists = PlaylistStore.mergeBackup(payload.playlists)
        RestoreSummary(
            favorites = payload.favorites.size,
            recent = payload.recent.size,
            playlists = createdPlaylists,
            tracks = payload.playlists.sumOf { it.tracks.size },
        )
    }
}
