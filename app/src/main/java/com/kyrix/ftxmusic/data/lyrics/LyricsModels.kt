package com.kyrix.ftxmusic.data.lyrics

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class Lyrics(
    val synced: List<LyricLine>,
    val plain: List<String>,
    val rawSynced: String?,
    val rawPlain: String?,
    val instrumental: Boolean = false,
)

sealed interface LyricsResult {
    data class Ready(val lyrics: Lyrics, val fromCache: Boolean) : LyricsResult
    data object Unavailable : LyricsResult
    data class Failed(val message: String) : LyricsResult
}

class LyricsException(message: String) : Exception(message)
