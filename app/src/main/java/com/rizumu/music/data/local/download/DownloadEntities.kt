package com.rizumu.music.data.local.download

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rizumu.music.data.yt.YoutubeTrack

object DownloadStatus {
    const val QUEUED = "queued"
    const val RUNNING = "running"
    const val COMPLETE = "complete"
    const val FAILED = "failed"
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val status: String,
    val progress: Int,
    val fileName: String?,
    val mimeType: String?,
    val bytes: Long,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun DownloadEntity.toTrack(): YoutubeTrack = YoutubeTrack(
    videoId = videoId,
    title = title,
    artist = artist,
    durationSec = durationSec,
    thumbnailUrl = thumbnailUrl,
)
