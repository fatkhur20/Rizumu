package com.kyrix.ftxmusic.data.yt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YoutubeUrlTest {

    @Test
    fun `extracts id from watch url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            videoIdFromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RDdQw4w9WgXcQ"),
        )
    }

    @Test
    fun `extracts id from music url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            videoIdFromUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ&feature=share"),
        )
    }

    @Test
    fun `extracts id from short url`() {
        assertEquals("dQw4w9WgXcQ", videoIdFromUrl("https://youtu.be/dQw4w9WgXcQ?t=10"))
    }

    @Test
    fun `extracts id from shorts url`() {
        assertEquals("dQw4w9WgXcQ", videoIdFromUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test
    fun `accepts raw id`() {
        assertEquals("dQw4w9WgXcQ", videoIdFromUrl("dQw4w9WgXcQ"))
    }

    @Test
    fun `rejects unrelated url`() {
        assertNull(videoIdFromUrl("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(videoIdFromUrl(""))
        assertNull(videoIdFromUrl(null))
    }

    @Test
    fun `extracts playlist id`() {
        assertEquals(
            "PL1234567890",
            playlistIdFromUrl("https://www.youtube.com/playlist?list=PL1234567890"),
        )
        assertNull(playlistIdFromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }
}
