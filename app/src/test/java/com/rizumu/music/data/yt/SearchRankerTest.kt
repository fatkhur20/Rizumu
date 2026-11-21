package com.rizumu.music.data.yt

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRankerTest {

    private fun track(id: String, title: String, artist: String) = YoutubeTrack(
        videoId = id,
        title = title,
        artist = artist,
        durationSec = 0L,
        thumbnailUrl = null,
    )

    @Test
    fun `relevant tracks are moved to the front`() {
        val unrelated = (0 until 6).map { track("id$it", "Lagu Acak $it", "Penyanyi $it") }
        val relevant = track("romi1", "Romi Jahat - Satu Satu", "Romi Jahat")

        val ranked = SearchRanker.rank(unrelated + relevant, "romi")

        assertEquals("romi1", ranked.first().videoId)
    }

    @Test
    fun `weak signal keeps all results sorted`() {
        val items = listOf(
            track("a", "Romi A", "Lain"),
            track("b", "Lain", "Romi B"),
            track("c", "Lain", "Lain"),
        )

        val ranked = SearchRanker.rank(items, "romi")

        assertEquals(listOf("a", "b", "c"), ranked.map { it.videoId })
    }

    @Test
    fun `single item is returned unchanged`() {
        val item = track("only", "Romi", "Romi")

        assertEquals(listOf(item), SearchRanker.rank(listOf(item), "romi"))
    }
    @Test
    fun `relevant count is exposed for fallback decision`() {
        val items = listOf(
            YoutubeTrack("1", "Romi Jahat", "Romi Jahat", 0L, null),
            YoutubeTrack("2", "Lain", "Lain", 0L, null),
        )

        assertEquals(1, SearchRanker.countRelevant(items, "romi"))
    }

}
