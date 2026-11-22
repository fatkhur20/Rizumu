package com.rizumu.music.data.backup

import com.rizumu.music.data.local.ListeningStats
import com.rizumu.music.data.local.ListeningStatsJson
import com.rizumu.music.data.local.playlist.PlaylistNames
import com.rizumu.music.data.yt.YoutubeTrack
import org.json.JSONArray
import org.json.JSONObject

data class BackupPlaylist(
    val name: String,
    val createdAt: Long,
    val tracks: List<YoutubeTrack>,
)

data class BackupPayload(
    val exportedAt: Long,
    val favorites: List<YoutubeTrack>,
    val recent: List<YoutubeTrack>,
    val searches: List<String>,
    val playlists: List<BackupPlaylist>,
    val stats: ListeningStats = ListeningStats(),
)

object BackupCodec {
    const val SCHEMA_VERSION = 2
    private const val MAX_TRACKS = 5_000
    private const val MAX_PLAYLISTS = 500
    private const val MAX_SEARCHES = 100

    fun encode(payload: BackupPayload): String = JSONObject()
        .put("schema", SCHEMA_VERSION)
        .put("exportedAt", payload.exportedAt)
        .put("favorites", payload.favorites.toJson())
        .put("recent", payload.recent.toJson())
        .put("searches", JSONArray(payload.searches))
        .put("stats", ListeningStatsJson.toJson(payload.stats))
        .put("playlists", JSONArray().apply {
            payload.playlists.forEach { playlist ->
                put(
                    JSONObject()
                        .put("name", playlist.name)
                        .put("createdAt", playlist.createdAt)
                        .put("tracks", playlist.tracks.toJson()),
                )
            }
        })
        .toString(2)

    fun decode(raw: String): BackupPayload? = runCatching {
        val root = JSONObject(raw)
        val schema = root.optInt("schema", -1)
        if (schema !in 1..SCHEMA_VERSION) return null
        BackupPayload(
            exportedAt = root.optLong("exportedAt", 0L),
            favorites = root.optJSONArray("favorites").toTracks(),
            recent = root.optJSONArray("recent").toTracks(),
            searches = root.optJSONArray("searches").toStrings(),
            playlists = root.optJSONArray("playlists").toPlaylists(),
            stats = if (schema == SCHEMA_VERSION) {
                ListeningStatsJson.fromJson(root.optJSONObject("stats")?.toString())
            } else {
                ListeningStats()
            },
        )
    }.getOrNull()

    private fun List<YoutubeTrack>.toJson(): JSONArray = JSONArray().apply {
        forEach { track ->
            put(
                JSONObject()
                    .put("videoId", track.videoId)
                    .put("title", track.title)
                    .put("artist", track.artist)
                    .put("durationSec", track.durationSec)
                    .put("thumbnailUrl", track.thumbnailUrl ?: JSONObject.NULL),
            )
        }
    }

    private fun JSONArray?.toTracks(): List<YoutubeTrack> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until minOf(length(), MAX_TRACKS)) {
                parseTrack(optJSONObject(index))?.let(::add)
            }
        }.distinctBy { it.videoId }
    }

    private fun JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until minOf(length(), MAX_SEARCHES)) {
                optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }.distinctBy { it.lowercase() }
    }

    private fun JSONArray?.toPlaylists(): List<BackupPlaylist> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until minOf(length(), MAX_PLAYLISTS)) {
                val json = optJSONObject(index) ?: continue
                val name = PlaylistNames.normalize(json.optString("name")) ?: continue
                add(
                    BackupPlaylist(
                        name = name,
                        createdAt = json.optLong("createdAt", 0L),
                        tracks = json.optJSONArray("tracks").toTracks(),
                    ),
                )
            }
        }.distinctBy { it.name.lowercase() }
    }

    private fun parseTrack(json: JSONObject?): YoutubeTrack? {
        if (json == null) return null
        val videoId = json.optString("videoId").trim()
        if (videoId.isBlank()) return null
        return YoutubeTrack(
            videoId = videoId,
            title = json.optString("title"),
            artist = json.optString("artist"),
            durationSec = json.optLong("durationSec", 0L).coerceAtLeast(0L),
            thumbnailUrl = if (json.isNull("thumbnailUrl")) {
                null
            } else {
                json.optString("thumbnailUrl").takeIf { it.isNotBlank() }
            },
        )
    }
}
