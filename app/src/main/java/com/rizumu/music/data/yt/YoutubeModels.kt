package com.rizumu.music.data.yt

import org.schabi.newpipe.extractor.Page

data class YoutubeTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
)

data class SearchPage(
    val items: List<YoutubeTrack>,
    val next: PageToken?,
)

class PageToken internal constructor(
    internal val page: Page? = null,
    internal val continuation: String? = null,
) {
    internal constructor(continuation: String) : this(page = null, continuation = continuation)
}

data class ResolvedAudio(
    val videoId: String,
    val url: String,
    val mimeType: String,
    val suffix: String,
    val bitrate: Int,
    val durationSec: Long,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
)

data class PlaylistFetch(
    val sourceId: String,
    val name: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val items: List<YoutubeTrack>,
    val next: PageToken?,
)
