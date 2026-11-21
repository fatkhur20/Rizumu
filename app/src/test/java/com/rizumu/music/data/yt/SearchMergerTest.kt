package com.rizumu.music.data.yt

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchMergerTest {

    private fun track(id: String) = YoutubeTrack(
        videoId = id,
        title = id,
        artist = "",
        durationSec = 0L,
        thumbnailUrl = null,
    )

    @Test
    fun `primary items stay first and duplicates are removed`() {
        val merged = SearchMerger.merge(
            listOf(track("a"), track("b")),
            listOf(track("b"), track("c")),
        )

        assertEquals(listOf("a", "b", "c"), merged.map { it.videoId })
    }
}
