package com.rizumu.music.data.local

import android.content.Context
import android.content.SharedPreferences
import com.rizumu.music.data.yt.YoutubeTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight local persistence for likes, recently played tracks and search history.
 * Room-backed library (playlists, downloads) is still planned for a later phase.
 */
object UserLibrary {

    data class Snapshot(
        val favorites: List<YoutubeTrack> = emptyList(),
        val recent: List<YoutubeTrack> = emptyList(),
        val recentSearches: List<String> = emptyList(),
    )

    private const val PREFS_NAME = "ftx_user_library"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENT = "recent"
    private const val KEY_SEARCHES = "recent_searches"
    private const val MAX_RECENT = 30
    private const val MAX_FAVORITES = 300
    private const val MAX_SEARCHES = 10

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _state.value = Snapshot(
            favorites = readTracks(KEY_FAVORITES),
            recent = readTracks(KEY_RECENT),
            recentSearches = readStrings(KEY_SEARCHES),
        )
    }

    fun isFavorite(videoId: String?): Boolean {
        if (videoId.isNullOrBlank()) return false
        return _state.value.favorites.any { it.videoId == videoId }
    }

    fun toggleFavorite(track: YoutubeTrack) {
        val current = _state.value.favorites
        val updated = if (current.any { it.videoId == track.videoId }) {
            current.filterNot { it.videoId == track.videoId }
        } else {
            (listOf(track) + current).take(MAX_FAVORITES)
        }
        writeTracks(KEY_FAVORITES, updated)
        _state.value = _state.value.copy(favorites = updated)
    }

    fun recordPlayed(track: YoutubeTrack) {
        if (track.videoId.isBlank()) return
        val updated = (listOf(track) + _state.value.recent.filterNot { it.videoId == track.videoId })
            .take(MAX_RECENT)
        writeTracks(KEY_RECENT, updated)
        _state.value = _state.value.copy(recent = updated)
    }

    fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _state.value.recentSearches.filterNot {
            it.equals(trimmed, ignoreCase = true)
        }).take(MAX_SEARCHES)
        writeStrings(KEY_SEARCHES, updated)
        _state.value = _state.value.copy(recentSearches = updated)
    }

    fun removeSearch(query: String) {
        val updated = _state.value.recentSearches.filterNot { it == query }
        writeStrings(KEY_SEARCHES, updated)
        _state.value = _state.value.copy(recentSearches = updated)
    }

    fun clearHistory() {
        writeTracks(KEY_RECENT, emptyList())
        writeStrings(KEY_SEARCHES, emptyList())
        _state.value = _state.value.copy(recent = emptyList(), recentSearches = emptyList())
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _state.value = Snapshot()
    }

    fun mergeBackup(
        favorites: List<YoutubeTrack>,
        recent: List<YoutubeTrack>,
        searches: List<String>,
    ) {
        val current = _state.value
        val mergedFavorites = (current.favorites + favorites)
            .filter { it.videoId.isNotBlank() }
            .distinctBy { it.videoId }
            .take(MAX_FAVORITES)
        val mergedRecent = (current.recent + recent)
            .filter { it.videoId.isNotBlank() }
            .distinctBy { it.videoId }
            .take(MAX_RECENT)
        val mergedSearches = (current.recentSearches + searches)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(MAX_SEARCHES)

        writeTracks(KEY_FAVORITES, mergedFavorites)
        writeTracks(KEY_RECENT, mergedRecent)
        writeStrings(KEY_SEARCHES, mergedSearches)
        _state.value = Snapshot(
            favorites = mergedFavorites,
            recent = mergedRecent,
            recentSearches = mergedSearches,
        )
    }

    private fun readTracks(key: String): List<YoutubeTrack> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    parseTrack(array.optJSONObject(index))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeTracks(key: String, tracks: List<YoutubeTrack>) {
        val array = JSONArray()
        tracks.forEach { array.put(it.toJson()) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun readStrings(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeStrings(key: String, values: List<String>) {
        prefs.edit().putString(key, JSONArray(values).toString()).apply()
    }

    private fun YoutubeTrack.toJson(): JSONObject = JSONObject()
        .put("videoId", videoId)
        .put("title", title)
        .put("artist", artist)
        .put("durationSec", durationSec)
        .put("thumbnailUrl", thumbnailUrl ?: JSONObject.NULL)

    private fun parseTrack(json: JSONObject?): YoutubeTrack? {
        if (json == null) return null
        val videoId = json.optString("videoId").takeIf { it.isNotBlank() } ?: return null
        return YoutubeTrack(
            videoId = videoId,
            title = json.optString("title"),
            artist = json.optString("artist"),
            durationSec = json.optLong("durationSec", 0L),
            thumbnailUrl = if (json.isNull("thumbnailUrl")) null else json.optString("thumbnailUrl"),
        )
    }
}
