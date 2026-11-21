package com.rizumu.music.data.lyrics

import android.content.Context
import android.content.SharedPreferences
import com.rizumu.music.data.local.playlist.LyricsEntity
import com.rizumu.music.data.local.playlist.LyricsDao
import com.rizumu.music.data.local.playlist.PlaylistDatabase
import com.rizumu.music.data.yt.YoutubeTrack

/**
 * Lyrics lookup with a Room-backed cache. Keeps working offline: cached lyrics
 * are reused for [TTL_MS], then refreshed in the background of the request.
 */
object LyricsStore {

    private const val TTL_MS = 30L * 24 * 60 * 60 * 1000
    private const val PREFS = "ftx_lyrics_prefs"
    private const val KEY_OFFSET = "offset_ms"
    const val DEFAULT_OFFSET_MS = 300
    const val MIN_OFFSET_MS = -5_000
    const val MAX_OFFSET_MS = 5_000

    private var dao: LyricsDao? = null
    private var prefs: SharedPreferences? = null
    private val client = LyricsClient()
    private val lock = Any()

    fun init(context: Context) {
        if (dao != null) return
        synchronized(lock) {
            if (dao == null) {
                dao = PlaylistDatabase.get(context).lyricsDao()
                prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            }
        }
    }

    /** Positive values delay the highlight (lyrics appear later). */
    fun offsetMs(): Int =
        prefs?.getInt(KEY_OFFSET, DEFAULT_OFFSET_MS) ?: DEFAULT_OFFSET_MS

    fun setOffsetMs(value: Int) {
        val clamped = value.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)
        prefs?.edit()?.putInt(KEY_OFFSET, clamped)?.apply()
    }

    suspend fun load(track: YoutubeTrack, force: Boolean = false): LyricsResult {
        val store = dao ?: return LyricsResult.Failed("Cache lirik belum siap.")

        val cached = runCatching { store.get(track.videoId) }.getOrNull()
        if (cached != null && !force && isFresh(cached)) {
            cached.toResult(fromCache = true)?.let { return it }
        }

        return try {
            val fetched = client.fetch(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationSec,
            )
            if (fetched == null || isEmpty(fetched)) {
                cached?.toResult(fromCache = true) ?: LyricsResult.Unavailable
            } else {
                val entity = LyricsEntity(
                    videoId = track.videoId,
                    trackName = track.title,
                    artistName = track.artist,
                    syncedLrc = fetched.rawSynced,
                    plainLyrics = fetched.rawPlain,
                    instrumental = fetched.instrumental,
                    fetchedAt = System.currentTimeMillis(),
                )
                runCatching { store.put(entity) }
                LyricsResult.Ready(fetched, fromCache = false)
            }
        } catch (error: LyricsException) {
            cached?.toResult(fromCache = true)
                ?: LyricsResult.Failed(error.message ?: "Gagal memuat lirik.")
        } catch (error: Exception) {
            cached?.toResult(fromCache = true)
                ?: LyricsResult.Failed("Gagal memuat lirik.")
        }
    }

    private fun isFresh(entity: LyricsEntity): Boolean =
        System.currentTimeMillis() - entity.fetchedAt < TTL_MS

    private fun isEmpty(lyrics: Lyrics): Boolean =
        lyrics.synced.isEmpty() && lyrics.plain.isEmpty() && !lyrics.instrumental

    private fun LyricsEntity.toResult(fromCache: Boolean): LyricsResult.Ready? {
        val lyrics = Lyrics(
            synced = LyricsParser.parseLrc(syncedLrc),
            plain = LyricsParser.plainLines(plainLyrics),
            rawSynced = syncedLrc,
            rawPlain = plainLyrics,
            instrumental = instrumental,
        )
        return if (isEmpty(lyrics)) null else LyricsResult.Ready(lyrics, fromCache)
    }
}
