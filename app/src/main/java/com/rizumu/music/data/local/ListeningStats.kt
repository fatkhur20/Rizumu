package com.rizumu.music.data.local

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrackListeningStat(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val playCount: Int,
    val listenedMs: Long,
    val lastPlayedAt: Long,
)

data class ListeningStats(
    val tracks: Map<String, TrackListeningStat> = emptyMap(),
    val byArtist: Map<String, Long> = emptyMap(),
    val byDay: Map<String, Long> = emptyMap(),
)

object ListeningStatsLogic {
    const val MAX_DELTA_MS = 2_000L
    const val FLUSH_THRESHOLD_MS = 30_000L

    private const val MAX_TRACKS = 500
    private const val MAX_ARTISTS = 500
    private const val MAX_DAYS = 365

    fun acceptableDelta(deltaMs: Long): Boolean =
        deltaMs > 0L && deltaMs <= MAX_DELTA_MS

    fun dayKey(now: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

    fun normalizeArtist(artist: String): String =
        artist.trim().lowercase(Locale.US).removeSuffix(" - topic")

    fun registerPlay(
        stats: ListeningStats,
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        now: Long,
    ): ListeningStats {
        if (videoId.isBlank()) return stats
        val existing = stats.tracks[videoId]
        val updated = existing?.copy(
            title = title.ifBlank { existing.title },
            artist = artist.ifBlank { existing.artist },
            thumbnailUrl = thumbnailUrl ?: existing.thumbnailUrl,
            playCount = existing.playCount + 1,
            lastPlayedAt = now,
        ) ?: TrackListeningStat(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            playCount = 1,
            listenedMs = 0L,
            lastPlayedAt = now,
        )
        return stats.copy(tracks = capTracks(stats.tracks + (videoId to updated)))
    }

    fun addListening(
        stats: ListeningStats,
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        deltaMs: Long,
        now: Long,
    ): ListeningStats {
        if (videoId.isBlank() || deltaMs <= 0L) return stats
        val existing = stats.tracks[videoId]
        val updated = existing?.copy(
            title = title.ifBlank { existing.title },
            artist = artist.ifBlank { existing.artist },
            thumbnailUrl = thumbnailUrl ?: existing.thumbnailUrl,
            listenedMs = existing.listenedMs + deltaMs,
            lastPlayedAt = now,
        ) ?: TrackListeningStat(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            playCount = 0,
            listenedMs = deltaMs,
            lastPlayedAt = now,
        )
        val artistKey = normalizeArtist(artist.ifBlank { existing?.artist.orEmpty() })
        val day = dayKey(now)
        return stats.copy(
            tracks = capTracks(stats.tracks + (videoId to updated)),
            byArtist = capMap(stats.byArtist + (artistKey to (stats.byArtist[artistKey] ?: 0L) + deltaMs), MAX_ARTISTS),
            byDay = capMap(stats.byDay + (day to (stats.byDay[day] ?: 0L) + deltaMs), MAX_DAYS),
        )
    }

    fun merge(a: ListeningStats, b: ListeningStats): ListeningStats {
        val tracks = mutableMapOf<String, TrackListeningStat>()
        for ((id, left) in a.tracks) {
            val right = b.tracks[id]
            tracks[id] = if (right == null) left else TrackListeningStat(
                videoId = id,
                title = left.title.ifBlank { right.title },
                artist = left.artist.ifBlank { right.artist },
                thumbnailUrl = left.thumbnailUrl ?: right.thumbnailUrl,
                playCount = left.playCount + right.playCount,
                listenedMs = left.listenedMs + right.listenedMs,
                lastPlayedAt = maxOf(left.lastPlayedAt, right.lastPlayedAt),
            )
        }
        b.tracks.forEach { (id, right) -> tracks.putIfAbsent(id, right) }
        val artists = mergeMaps(a.byArtist, b.byArtist, MAX_ARTISTS)
        val days = mergeMaps(a.byDay, b.byDay, MAX_DAYS)
        return ListeningStats(capTracks(tracks), artists, days)
    }

    fun totalMs(stats: ListeningStats): Long = stats.byDay.values.sum()

    fun topArtists(stats: ListeningStats, limit: Int = 5): List<Pair<String, Long>> =
        stats.byArtist.toList()
            .sortedByDescending { it.second }
            .take(limit)

    private fun capTracks(tracks: Map<String, TrackListeningStat>): Map<String, TrackListeningStat> =
        if (tracks.size <= MAX_TRACKS) {
            tracks
        } else {
            tracks.toList()
                .sortedByDescending { it.second.lastPlayedAt }
                .take(MAX_TRACKS)
                .toMap()
        }

    private fun capMap(values: Map<String, Long>, limit: Int): Map<String, Long> =
        if (values.size <= limit) {
            values
        } else {
            values.toList()
                .sortedByDescending { it.first }
                .take(limit)
                .toMap()
        }

    private fun mergeMaps(a: Map<String, Long>, b: Map<String, Long>, limit: Int): Map<String, Long> {
        val merged = mutableMapOf<String, Long>()
        a.forEach { (key, value) -> merged[key] = value }
        b.forEach { (key, value) -> merged[key] = (merged[key] ?: 0L) + value }
        return capMap(merged, limit)
    }
}

object ListeningStatsJson {
    fun toJson(stats: ListeningStats): JSONObject = JSONObject().apply {
        put("tracks", JSONArray().apply {
            stats.tracks.values.sortedByDescending { it.lastPlayedAt }.forEach { track ->
                put(
                    JSONObject()
                        .put("videoId", track.videoId)
                        .put("title", track.title)
                        .put("artist", track.artist)
                        .put("thumbnailUrl", track.thumbnailUrl ?: JSONObject.NULL)
                        .put("playCount", track.playCount)
                        .put("listenedMs", track.listenedMs)
                        .put("lastPlayedAt", track.lastPlayedAt),
                )
            }
        })
        put("byArtist", JSONObject(stats.byArtist))
        put("byDay", JSONObject(stats.byDay))
    }

    fun fromJson(raw: String?): ListeningStats {
        if (raw.isNullOrBlank()) return ListeningStats()
        return runCatching {
            val root = JSONObject(raw)
            val tracks = mutableMapOf<String, TrackListeningStat>()
            val array = root.optJSONArray("tracks")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val id = json.optString("videoId").trim()
                    if (id.isBlank()) continue
                    tracks[id] = TrackListeningStat(
                        videoId = id,
                        title = json.optString("title"),
                        artist = json.optString("artist"),
                        thumbnailUrl = if (json.isNull("thumbnailUrl")) null else json.optString("thumbnailUrl"),
                        playCount = json.optInt("playCount", 0).coerceAtLeast(0),
                        listenedMs = json.optLong("listenedMs", 0L).coerceAtLeast(0L),
                        lastPlayedAt = json.optLong("lastPlayedAt", 0L).coerceAtLeast(0L),
                    )
                }
            }
            ListeningStats(
                tracks = tracks,
                byArtist = root.optJSONObject("byArtist").toLongMap(),
                byDay = root.optJSONObject("byDay").toLongMap(),
            )
        }.getOrDefault(ListeningStats())
    }

    private fun JSONObject?.toLongMap(): Map<String, Long> {
        if (this == null) return emptyMap()
        val result = mutableMapOf<String, Long>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = optLong(key, 0L).coerceAtLeast(0L)
        }
        return result
    }
}
