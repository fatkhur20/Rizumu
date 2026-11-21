package com.rizumu.music.data.local.playlist

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val addedAt: Long,
)

data class PlaylistSummary(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int,
    val firstThumbnail: String?,
    val firstVideoId: String?,
    val secondThumbnail: String? = null,
    val secondVideoId: String? = null,
    val thirdThumbnail: String? = null,
    val thirdVideoId: String? = null,
    val fourthThumbnail: String? = null,
    val fourthVideoId: String? = null,
)

data class PlaylistCover(
    val thumbnailUrl: String?,
    val videoId: String?,
)

val PlaylistSummary.covers: List<PlaylistCover>
    get() = listOf(
        PlaylistCover(firstThumbnail, firstVideoId),
        PlaylistCover(secondThumbnail, secondVideoId),
        PlaylistCover(thirdThumbnail, thirdVideoId),
        PlaylistCover(fourthThumbnail, fourthVideoId),
    ).filter { it.thumbnailUrl != null || it.videoId != null }
