package com.kyrix.ftxmusic.data.local.playlist

import android.content.Context
import androidx.room.withTransaction
import com.kyrix.ftxmusic.data.backup.BackupPlaylist
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object PlaylistStore {

    private lateinit var db: PlaylistDatabase
    private lateinit var dao: PlaylistDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _playlists = MutableStateFlow<List<PlaylistSummary>>(emptyList())
    val playlists: StateFlow<List<PlaylistSummary>> = _playlists.asStateFlow()

    fun init(context: Context) {
        if (::dao.isInitialized) return
        db = PlaylistDatabase.get(context)
        dao = db.playlistDao()
        scope.launch {
            dao.observePlaylists().collect { _playlists.value = it }
        }
    }

    fun observePlaylist(id: Long): Flow<PlaylistEntity?> = dao.observePlaylist(id)

    fun observeTracks(id: Long): Flow<List<YoutubeTrack>> =
        dao.observeTracks(id).map { rows -> rows.map { it.toTrack() } }

    fun create(name: String) {
        val normalized = PlaylistNames.normalize(name) ?: return
        scope.launch {
            dao.insertPlaylist(PlaylistEntity(name = normalized, createdAt = System.currentTimeMillis()))
        }
    }

    fun createAndAdd(name: String, track: YoutubeTrack) {
        val normalized = PlaylistNames.normalize(name) ?: return
        scope.launch {
            db.withTransaction {
                val now = System.currentTimeMillis()
                val id = dao.insertPlaylist(PlaylistEntity(name = normalized, createdAt = now))
                dao.insertTrack(track.toEntity(id, now))
            }
        }
    }

    fun addTrack(playlistId: Long, track: YoutubeTrack) {
        scope.launch {
            db.withTransaction {
                val next = maxOf(System.currentTimeMillis(), dao.maxAddedAt(playlistId) + 1)
                dao.insertTrack(track.toEntity(playlistId, next))
            }
        }
    }

    fun rename(playlistId: Long, name: String) {
        val normalized = PlaylistNames.normalize(name) ?: return
        scope.launch { dao.renamePlaylist(playlistId, normalized) }
    }

    fun move(playlistId: Long, videoId: String, up: Boolean) {
        scope.launch {
            db.withTransaction {
                val tracks = dao.tracksOnce(playlistId)
                val index = tracks.indexOfFirst { it.videoId == videoId }
                val target = if (up) index - 1 else index + 1
                if (index < 0 || target !in tracks.indices) return@withTransaction
                val current = tracks[index]
                val neighbour = tracks[target]
                dao.setAddedAt(current.playlistId, current.videoId, neighbour.addedAt)
                dao.setAddedAt(neighbour.playlistId, neighbour.videoId, current.addedAt)
            }
        }
    }

    fun reorder(playlistId: Long, fromIndex: Int, toIndex: Int) {
        scope.launch {
            db.withTransaction {
                val tracks = dao.tracksOnce(playlistId)
                val reordered = PlaylistOrder.move(tracks, fromIndex, toIndex)
                if (reordered == tracks) return@withTransaction
                val base = tracks.minOfOrNull { it.addedAt } ?: System.currentTimeMillis()
                reordered.forEachIndexed { index, track ->
                    dao.setAddedAt(playlistId, track.videoId, base + index)
                }
            }
        }
    }

    fun removeTrack(playlistId: Long, videoId: String) {
        scope.launch { dao.deleteTrack(playlistId, videoId) }
    }

    fun delete(playlistId: Long) {
        scope.launch { dao.deletePlaylist(playlistId) }
    }

    suspend fun snapshot(): List<BackupPlaylist> =
        dao.playlistsOnce().map { playlist ->
            BackupPlaylist(
                name = playlist.name,
                createdAt = playlist.createdAt,
                tracks = dao.tracksOnce(playlist.id).map { it.toTrack() },
            )
        }

    suspend fun mergeBackup(backups: List<BackupPlaylist>): Int {
        var created = 0
        db.withTransaction {
            backups.forEach { backup ->
                val name = PlaylistNames.normalize(backup.name) ?: return@forEach
                val existing = dao.findByName(name)
                val playlistId = if (existing == null) {
                    created++
                    dao.insertPlaylist(
                        PlaylistEntity(
                            name = name,
                            createdAt = backup.createdAt.takeIf { it > 0L }
                                ?: System.currentTimeMillis(),
                        ),
                    )
                } else {
                    existing.id
                }
                val knownIds = dao.tracksOnce(playlistId)
                    .mapTo(mutableSetOf()) { it.videoId }
                var addedAt = maxOf(
                    dao.maxAddedAt(playlistId) + 1L,
                    System.currentTimeMillis(),
                )
                backup.tracks.forEach { track ->
                    if (track.videoId.isNotBlank() && knownIds.add(track.videoId)) {
                        dao.insertTrack(track.toEntity(playlistId, addedAt++))
                    }
                }
            }
        }
        return created
    }

    private fun YoutubeTrack.toEntity(playlistId: Long, addedAt: Long) = PlaylistTrackEntity(
        playlistId = playlistId,
        videoId = videoId,
        title = title,
        artist = artist,
        durationSec = durationSec,
        thumbnailUrl = thumbnailUrl,
        addedAt = addedAt,
    )

    private fun PlaylistTrackEntity.toTrack() = YoutubeTrack(
        videoId = videoId,
        title = title,
        artist = artist,
        durationSec = durationSec,
        thumbnailUrl = thumbnailUrl,
    )
}
