package com.kyrix.ftxmusic.data.lyrics

import com.kyrix.ftxmusic.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * LRCLIB (https://lrclib.net) client. Free, no API key, public domain lyrics
 * database. Queries by track + artist, then picks the closest duration and
 * prefers a synced LRC over plain text.
 */
class LyricsClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun fetch(title: String, artist: String, durationSec: Long): Lyrics? =
        withContext(Dispatchers.IO) {
            val cleanTitle = LyricsQuery.cleanTitle(title)
            if (cleanTitle.isBlank()) return@withContext null
            val cleanArtist = LyricsQuery.cleanArtist(artist)

            val candidates = runCatching {
                searchByTrack(cleanTitle, cleanArtist)
            }.getOrElse { error ->
                if (error is LyricsException) throw error
                throw LyricsException("Tidak bisa memuat lirik. Periksa koneksi.")
            }

            val fallback = if (candidates.isEmpty()) {
                val query = listOf(cleanArtist, cleanTitle).filter { it.isNotBlank() }.joinToString(" ")
                runCatching { searchByQuery(query) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val best = choose(candidates.ifEmpty { fallback }, durationSec) ?: return@withContext null
            Lyrics(
                synced = LyricsParser.parseLrc(best.synced),
                plain = LyricsParser.plainLines(best.plain),
                rawSynced = best.synced,
                rawPlain = best.plain,
                instrumental = best.instrumental,
            )
        }

    private fun searchByTrack(title: String, artist: String): List<Candidate> {
        val url = BASE.toHttpUrl().newBuilder().apply {
            addQueryParameter("track_name", title)
            if (artist.isNotBlank()) addQueryParameter("artist_name", artist)
        }.build()
        return read(url)
    }

    private fun searchByQuery(query: String): List<Candidate> {
        if (query.isBlank()) return emptyList()
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()
        return read(url)
    }

    private fun read(url: HttpUrl): List<Candidate> {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Rizumu/${BuildConfig.VERSION_NAME} (personal Android player)")
            .build()

        val body = try {
            http.newCall(request).execute().use { response ->
                when {
                    response.code == 429 -> throw LyricsException(
                        "Terlalu banyak permintaan lirik. Coba lagi nanti.",
                    )
                    response.code == 404 -> return emptyList()
                    !response.isSuccessful -> throw LyricsException(
                        "LRCLIB mengembalikan HTTP ${response.code}.",
                    )
                    else -> response.body?.string().orEmpty()
                }
            }
        } catch (error: IOException) {
            throw LyricsException("Tidak bisa memuat lirik. Periksa koneksi.")
        }

        val array = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { json ->
                    Candidate(
                        synced = json.optStringOrNull("syncedLyrics"),
                        plain = json.optStringOrNull("plainLyrics"),
                        duration = json.optLongOrNull("duration"),
                        instrumental = json.optBoolean("instrumental", false),
                    )
                }?.let(::add)
            }
        }
    }

    private fun choose(candidates: List<Candidate>, durationSec: Long): Candidate? {
        val usable = candidates.filter {
            !it.synced.isNullOrBlank() || !it.plain.isNullOrBlank() || it.instrumental
        }
        if (usable.isEmpty()) return null

        fun score(candidate: Candidate): Long {
            val kind = when {
                !candidate.synced.isNullOrBlank() -> 0
                candidate.instrumental -> 1
                else -> 2
            }
            val diff = if (durationSec > 0 && candidate.duration != null) {
                abs(candidate.duration - durationSec)
            } else {
                300L
            }
            return kind * 10_000L + diff
        }

        return usable.minByOrNull(::score)
    }

    private data class Candidate(
        val synced: String?,
        val plain: String?,
        val duration: Long?,
        val instrumental: Boolean,
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    companion object {
        private const val BASE = "https://lrclib.net/api/search"
    }
}
