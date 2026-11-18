package com.kyrix.ftxmusic.data.charts

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ChartRepository(
    private val cache: ChartCache,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val mutex = Mutex()

    suspend fun getTrendingSongs(
        region: ChartRegion,
        limit: Int = DEFAULT_LIMIT,
        forceRefresh: Boolean = false,
    ): ChartResult = load(region, ChartKind.SONGS, limit, forceRefresh)

    suspend fun getTrendingMusicVideos(
        region: ChartRegion,
        limit: Int = DEFAULT_LIMIT,
        forceRefresh: Boolean = false,
    ): ChartResult = load(region, ChartKind.MUSIC_VIDEOS, limit, forceRefresh)

    suspend fun getGlobalTopMusicVideos(
        limit: Int = DEFAULT_LIMIT,
        forceRefresh: Boolean = false,
    ): ChartResult = load(ChartRegion.GLOBAL, ChartKind.MUSIC_VIDEOS, limit, forceRefresh)

    fun cached(region: ChartRegion, kind: ChartKind): ChartSnapshot? =
        cache.read(region.code, kind)

    fun lastFetch(region: ChartRegion, kind: ChartKind): Long? =
        cache.read(region.code, kind)?.fetchedAt

    private suspend fun load(
        region: ChartRegion,
        kind: ChartKind,
        limit: Int,
        forceRefresh: Boolean,
    ): ChartResult {
        val boundedLimit = limit.coerceIn(1, 100)
        val existing = cache.read(region.code, kind)

        if (!forceRefresh && existing != null && isFresh(existing)) {
            return ChartResult(existing, fromCache = true)
        }

        return mutex.withLock {
            val current = cache.read(region.code, kind)
            if (!forceRefresh && current != null && isFresh(current)) {
                return@withLock ChartResult(current, fromCache = true)
            }
            try {
                val fetchedAt = clock()
                val playlists = fetchChartIndex(region)
                val playlist = when (kind) {
                    ChartKind.SONGS -> ChartParser.selectTrendingSongs(playlists)
                    ChartKind.MUSIC_VIDEOS -> ChartParser.selectDailyMusicVideos(playlists)
                } ?: throw ChartException(
                    when (kind) {
                        ChartKind.SONGS -> "YouTube Music tidak menyediakan tangga lagu untuk wilayah ini."
                        ChartKind.MUSIC_VIDEOS -> "YouTube Music tidak menyediakan tangga lagu video untuk wilayah ini."
                    },
                )
                val snapshot = fetchPlaylist(playlist, region.code, kind, fetchedAt, boundedLimit)
                if (snapshot.entries.isEmpty()) {
                    throw ChartException("Tangga lagu dari YouTube Music sedang kosong.")
                }
                cache.write(snapshot)
                ChartResult(snapshot, fromCache = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val fallback = current ?: cache.read(region.code, kind)
                if (fallback != null) {
                    ChartResult(fallback, fromCache = true, refreshError = userMessage(error))
                } else {
                    throw ChartException(userMessage(error), error)
                }
            }
        }
    }

    private fun isFresh(snapshot: ChartSnapshot): Boolean =
        clock() - snapshot.fetchedAt in 0 until CACHE_TTL_MS

    private suspend fun fetchChartIndex(region: ChartRegion): List<ChartPlaylist> {
        val body = baseBody(if (region.code == ChartRegion.GLOBAL.code) "US" else region.code).apply {
            put("browseId", CHARTS_BROWSE_ID)
            put(
                "formData",
                JSONObject().put("selectedValues", JSONArray().put(region.code)),
            )
        }
        val json = post(body)
        val playlists = ChartParser.parseChartPlaylists(json)
        if (playlists.isEmpty()) {
            throw ChartException("Struktur tangga lagu YouTube Music tidak dikenali.")
        }
        return playlists
    }

    private suspend fun fetchPlaylist(
        playlist: ChartPlaylist,
        region: String,
        kind: ChartKind,
        fetchedAt: Long,
        limit: Int,
    ): ChartSnapshot {
        val json = post(baseBody().apply { put("browseId", playlist.browseId) })
        return ChartParser.parsePlaylist(
            json = json,
            region = region,
            kind = kind,
            chartTitle = playlist.title,
            fetchedAt = fetchedAt,
            limit = limit,
        )
    }

    private fun baseBody(gl: String = "US"): JSONObject {
        val clientVersion = "1." +
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(clock())) +
            ".01.00"
        return JSONObject().apply {
            put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", clientVersion)
                        put("hl", "en")
                        put("gl", gl)
                    },
                ),
            )
        }
    }

    private suspend fun post(body: JSONObject): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BROWSE_URL)
            .header("Content-Type", "application/json")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/charts")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0",
            )
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw when (response.code) {
                    429 -> ChartException("Terlalu banyak permintaan ke YouTube Music. Coba lagi nanti.")
                    403 -> ChartException("YouTube Music menolak permintaan tangga lagu dari jaringan ini.")
                    else -> ChartException("YouTube Music mengembalikan HTTP ${response.code}.")
                }
            }
            if (text.isBlank()) throw ChartException("Tangga lagu YouTube Music tidak mengembalikan data.")
            text
        }
    }

    private fun userMessage(error: Throwable): String {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is ChartException) return cause.message ?: DEFAULT_ERROR
            cause = cause.cause
        }
        return when (error) {
            is IOException -> "Tidak bisa menghubungi YouTube Music. Periksa koneksi."
            else -> error.message ?: DEFAULT_ERROR
        }
    }

    private companion object {
        const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?alt=json&prettyPrint=false"
        const val CHARTS_BROWSE_ID = "FEmusic_charts"
        const val DEFAULT_LIMIT = 20
        const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L
        const val DEFAULT_ERROR = "Tidak bisa memuat tangga lagu."
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
