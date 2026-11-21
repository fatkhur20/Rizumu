package com.rizumu.music.data.local.playlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, p.createdAt,
            (SELECT COUNT(*) FROM playlist_tracks t WHERE t.playlistId = p.id) AS trackCount,
            (SELECT t.thumbnailUrl FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1) AS firstThumbnail,
            (SELECT t.videoId FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1) AS firstVideoId,
            (SELECT t.thumbnailUrl FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1 OFFSET 1) AS secondThumbnail,
            (SELECT t.videoId FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1 OFFSET 1) AS secondVideoId,
            (SELECT t.thumbnailUrl FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1 OFFSET 2) AS thirdThumbnail,
            (SELECT t.videoId FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1 OFFSET 2) AS thirdVideoId,
            (SELECT t.thumbnailUrl FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1 OFFSET 3) AS fourthThumbnail,
            (SELECT t.videoId FROM playlist_tracks t
                WHERE t.playlistId = p.id ORDER BY t.addedAt ASC LIMIT 1 OFFSET 3) AS fourthVideoId
        FROM playlists p
        ORDER BY p.createdAt DESC
        """,
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    suspend fun playlistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): PlaylistEntity?

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY addedAt ASC")
    fun observeTracks(id: Long): Flow<List<PlaylistTrackEntity>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deleteTrack(playlistId: Long, videoId: String)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY addedAt ASC")
    suspend fun tracksOnce(id: Long): List<PlaylistTrackEntity>

    @Query("SELECT COALESCE(MAX(addedAt), 0) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun maxAddedAt(id: Long): Long

    @Query("UPDATE playlist_tracks SET addedAt = :addedAt WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun setAddedAt(playlistId: Long, videoId: String, addedAt: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)
}
