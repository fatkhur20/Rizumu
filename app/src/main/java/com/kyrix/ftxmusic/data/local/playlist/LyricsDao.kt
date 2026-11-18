package com.kyrix.ftxmusic.data.local.playlist

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val videoId: String,
    val trackName: String,
    val artistName: String,
    val syncedLrc: String?,
    val plainLyrics: String?,
    @ColumnInfo(defaultValue = "0") val instrumental: Boolean,
    val fetchedAt: Long,
)

@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics WHERE videoId = :videoId")
    suspend fun get(videoId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LyricsEntity)
}
