package com.kyrix.ftxmusic.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkUrlsTest {

    @Test
    fun `video thumbnail chain prefers maxresdown`() {
        val urls = ArtworkUrls.chain(
            url = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            videoId = "dQw4w9WgXcQ",
            profile = ArtworkProfile.CARD,
        )

        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg", urls.first())
        assertEquals(4, urls.size)
        assertTrue(urls.contains("https://i.ytimg.com/vi/dQw4w9WgXcQ/sddefault.jpg"))
        assertTrue(urls.contains("https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg"))
    }

    @Test
    fun `original ytimg thumbnail is not duplicated`() {
        val urls = ArtworkUrls.chain(
            url = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            videoId = "dQw4w9WgXcQ",
            profile = ArtworkProfile.LIST,
        )

        assertEquals(urls.distinct(), urls)
        assertEquals(4, urls.size)
    }

    @Test
    fun `invalid video id is ignored`() {
        val urls = ArtworkUrls.chain(
            url = "https://example.com/cover.jpg",
            videoId = "too-short",
            profile = ArtworkProfile.LIST,
        )

        assertEquals(listOf("https://example.com/cover.jpg"), urls)
    }

    @Test
    fun `square album cover is tried before 16by9 variants`() {
        val urls = ArtworkUrls.chain(
            url = "https://lh3.googleusercontent.com/abc=w120-h120-l90-rj",
            videoId = "dQw4w9WgXcQ",
            profile = ArtworkProfile.NOW_PLAYING,
        )

        assertEquals(
            "https://lh3.googleusercontent.com/abc=w1024-h1024-l90-rj",
            urls.first(),
        )
        assertTrue(urls.contains("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg"))
    }

    @Test
    fun `google cdn square size is upgraded`() {
        val resized = ArtworkUrls.resize(
            "https://lh3.googleusercontent.com/abc=w60-h60-l90-rj",
            ArtworkProfile.CARD.targetPx,
        )

        assertEquals(
            "https://lh3.googleusercontent.com/abc=w512-h512-l90-rj",
            resized,
        )
    }

    @Test
    fun `google cdn size prefix and path sizes are upgraded`() {
        assertEquals(
            "https://lh3.googleusercontent.com/abc=s512",
            ArtworkUrls.resize("https://lh3.googleusercontent.com/abc=s120", 512),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/w256-h256/abc",
            ArtworkUrls.resize("https://lh3.googleusercontent.com/w60-h60/abc", 256),
        )
    }

    @Test
    fun `non google url is untouched`() {
        val url = "https://example.com/cover.jpg?size=60"
        assertEquals(url, ArtworkUrls.resize(url, 512))
    }

    @Test
    fun `null metadata yields empty chain`() {
        assertTrue(ArtworkUrls.chain(null, null, ArtworkProfile.LIST).isEmpty())
        assertTrue(ArtworkUrls.chain(" ", "  ", ArtworkProfile.LIST).isEmpty())
    }
}
