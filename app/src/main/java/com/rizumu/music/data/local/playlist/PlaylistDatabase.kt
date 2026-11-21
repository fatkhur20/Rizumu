package com.rizumu.music.data.local.playlist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, LyricsEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile
        private var instance: PlaylistDatabase? = null

        fun get(context: Context): PlaylistDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlaylistDatabase::class.java,
                    "ftx_playlists.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lyrics` (
                        `videoId` TEXT NOT NULL,
                        `trackName` TEXT NOT NULL,
                        `artistName` TEXT NOT NULL,
                        `syncedLrc` TEXT,
                        `plainLyrics` TEXT,
                        `instrumental` INTEGER NOT NULL DEFAULT 0,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`videoId`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
