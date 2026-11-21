package com.rizumu.music.data.yt

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

enum class MusicSearchFilter { ALL, SONGS, VIDEOS, RECENT, WEEK, MONTH }

data class MusicSearchPage(
    val items: List<YoutubeTrack>,
    val continuation: String?,
)

/**
 * Anonymous YouTube Music (WEB_REMIX) InnerTube search.
 *
 * Used because the public music search rows carry the real square album cover
 * (googleusercontent `=w1200-h1200`), while NewPipe/YouTube video thumbnails are
 * 16:9 frames capped at 1280x720 and look blurry when cropped square.
 */
class MusicSearch(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val localeProvider: () -> SearchLocale = { SearchLocale.global() },
) {

    suspend fun search(
        query: String,
        filter: MusicSearchFilter,
        continuation: String? = null,
        locale: SearchLocale? = null,
    ): MusicSearchPage = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("context", contextBody(locale ?: localeProvider()))
            if (continuation != null) {
                put("continuation", continuation)
            } else {
                put("query", query.trim())
                paramsFor(filter)?.let { put("params", it) }
            }
        }
        MusicSearchParser.parse(post(body))
    }

    internal fun contextBody(locale: SearchLocale): JSONObject {
        val version = "1." + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(clock())) + ".01.00"
        return JSONObject().put(
            "client",
            JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", version)
                put("hl", locale.hl)
                put("gl", locale.gl)
            },
        )
    }

    private fun post(body: JSONObject): String {
        val request = Request.Builder()
            .url(SEARCH_URL)
            .header("Content-Type", "application/json")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/search")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0",
            )
            .post(body.toString().toRequestBody(JSON))
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Pencarian gagal (HTTP ${response.code})")
            if (text.isBlank()) throw IOException("Pencarian mengembalikan data kosong")
            return text
        }
    }

    companion object {
        private const val SEARCH_URL =
            "https://music.youtube.com/youtubei/v1/search?alt=json&prettyPrint=false"
        private val JSON = "application/json".toMediaType()
        internal const val PARAM_SONGS = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
        internal const val PARAM_VIDEOS = "EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D"

        internal fun paramsFor(filter: MusicSearchFilter): String? = when (filter) {
            MusicSearchFilter.SONGS -> PARAM_SONGS
            MusicSearchFilter.VIDEOS -> PARAM_VIDEOS
            MusicSearchFilter.RECENT -> SearchFilter.RECENT
            MusicSearchFilter.WEEK -> SearchFilter.WEEK
            MusicSearchFilter.MONTH -> SearchFilter.MONTH
            MusicSearchFilter.ALL -> null
        }
    }
}

object MusicSearchParser {

    private val DURATION = Regex("^\\d{1,3}:\\d{2}$")
    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val GOOGLE_SIZE = Regex("=w\\d+-h\\d+")

    fun parse(raw: String): MusicSearchPage {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return MusicSearchPage(emptyList(), null)
        val rows = mutableListOf<YoutubeTrack>()
        collectRows(root, rows)

        val seen = HashSet<String>()
        val items = rows.filter { seen.add(it.videoId) }
        return MusicSearchPage(items, findContinuation(root))
    }

    private fun collectRows(node: Any?, out: MutableList<YoutubeTrack>) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject("musicResponsiveListItemRenderer")?.let { row ->
                    toTrack(row)?.let(out::add)
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    collectRows(node.opt(keys.next()), out)
                }
            }
            is JSONArray -> {
                for (index in 0 until node.length()) collectRows(node.opt(index), out)
            }
        }
    }

    private fun toTrack(row: JSONObject): YoutubeTrack? {
        val columns = row.optJSONArray("flexColumns") ?: return null
        val title = columnText(columns, 0) ?: return null
        if (title.isBlank()) return null

        val videoId = (row.optJSONObject("playlistItemData")?.optString("videoId")
            ?: watchVideoId(row))
            ?.takeIf { VIDEO_ID.matches(it) }
            ?: return null

        val subtitleRuns = columnRuns(columns, 1)
        val artist = subtitleRuns.firstOrNull { it.isNotBlank() && it != "•" } ?: ""
        val duration = subtitleRuns.lastOrNull { DURATION.matches(it) }
            ?.let(::parseDuration) ?: 0L
        val thumbnail = row.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                (0 until thumbs.length())
                    .mapNotNull { thumbs.optJSONObject(it)?.optString("url") }
                    .lastOrNull { it.isNotBlank() }
            }

        return YoutubeTrack(
            videoId = videoId,
            title = title.trim(),
            artist = artist.trim(),
            durationSec = duration,
            thumbnailUrl = thumbnail?.let(::upgradeCover),
        )
    }

    private fun watchVideoId(row: JSONObject): String? {
        val play = row.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
        if (!play.isNullOrBlank()) return play

        val nav = row.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
        return nav?.takeIf { it.isNotBlank() }
    }

    private fun columnText(columns: JSONArray, index: Int): String? =
        columnRuns(columns, index).joinToString("").takeIf { it.isNotBlank() }

    private fun columnRuns(columns: JSONArray, index: Int): List<String> {
        val column = columns.optJSONObject(index)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?: return emptyList()
        return (0 until column.length()).mapNotNull { position ->
            column.optJSONObject(position)?.optString("text")?.takeIf { it.isNotEmpty() }
        }
    }

    private fun parseDuration(value: String): Long {
        val parts = value.split(":")
        return runCatching {
            when (parts.size) {
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                else -> 0L
            }
        }.getOrDefault(0L)
    }

    internal fun upgradeCover(url: String): String {
        if (!url.contains("googleusercontent.com") && !url.contains("ggpht.com")) return url
        return GOOGLE_SIZE.replace(url, "=w1200-h1200")
    }

    private fun findContinuation(root: JSONObject): String? = findContinuation(root as Any?)

    private fun findContinuation(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject("nextContinuationData")?.optString("continuation")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
                node.optJSONObject("continuationCommand")?.optString("token")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findContinuation(node.opt(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> {
                for (index in 0 until node.length()) {
                    findContinuation(node.opt(index))?.let { return it }
                }
            }
        }
        return null
    }
}
