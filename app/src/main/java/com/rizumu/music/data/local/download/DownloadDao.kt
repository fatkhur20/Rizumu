package com.rizumu.music.data.local.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun get(videoId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query(
        """
        UPDATE downloads
        SET status = :status, progress = :progress, error = NULL, updatedAt = :updatedAt
        WHERE videoId = :videoId
        """,
    )
    suspend fun markRunning(
        videoId: String,
        status: String,
        progress: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE downloads
        SET progress = :progress, updatedAt = :updatedAt
        WHERE videoId = :videoId
        """,
    )
    suspend fun updateProgress(videoId: String, progress: Int, updatedAt: Long)

    @Query(
        """
        UPDATE downloads
        SET status = :status, progress = 100, fileName = :fileName, mimeType = :mimeType,
            bytes = :bytes, error = NULL, updatedAt = :updatedAt
        WHERE videoId = :videoId
        """,
    )
    suspend fun markComplete(
        videoId: String,
        status: String,
        fileName: String,
        mimeType: String?,
        bytes: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE downloads
        SET status = :status, error = :error, updatedAt = :updatedAt
        WHERE videoId = :videoId
        """,
    )
    suspend fun markFailed(
        videoId: String,
        status: String,
        error: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}
