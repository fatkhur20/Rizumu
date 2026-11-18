package com.kyrix.ftxmusic.data.charts

import com.kyrix.ftxmusic.data.yt.YoutubeTrack

data class ChartEntry(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationSec: Long?,
    val videoId: String,
    val chartRegion: String,
    val fetchedAt: Long,
    val source: String,
) {
    fun toTrack(): YoutubeTrack = YoutubeTrack(
        videoId = videoId,
        title = title,
        artist = artist,
        durationSec = durationSec ?: 0L,
        thumbnailUrl = thumbnailUrl,
    )
}

enum class ChartKind(val key: String) {
    SONGS("songs"),
    MUSIC_VIDEOS("videos"),
}

data class ChartSnapshot(
    val region: String,
    val kind: ChartKind,
    val chartTitle: String,
    val entries: List<ChartEntry>,
    val fetchedAt: Long,
    val source: String,
)

data class ChartResult(
    val snapshot: ChartSnapshot,
    val fromCache: Boolean,
    val refreshError: String? = null,
)

class ChartException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ChartRegion(val code: String, val label: String) {
    companion object {
        val INDONESIA = ChartRegion("ID", "Indonesia")
        val GLOBAL = ChartRegion("ZZ", "Global")

        val SELECTABLE = listOf(
            INDONESIA,
            ChartRegion("MY", "Malaysia"),
            ChartRegion("SG", "Singapura"),
            ChartRegion("US", "Amerika Serikat"),
            ChartRegion("GB", "Inggris"),
            ChartRegion("JP", "Jepang"),
            ChartRegion("KR", "Korea Selatan"),
            ChartRegion("IN", "India"),
            ChartRegion("AU", "Australia"),
            ChartRegion("DE", "Jerman"),
            ChartRegion("FR", "Prancis"),
            ChartRegion("BR", "Brasil"),
            ChartRegion("MX", "Meksiko"),
            ChartRegion("TH", "Thailand"),
            ChartRegion("VN", "Vietnam"),
            ChartRegion("PH", "Filipina"),
        )

        fun byCode(code: String): ChartRegion =
            SELECTABLE.firstOrNull { it.code == code } ?: INDONESIA
    }
}
