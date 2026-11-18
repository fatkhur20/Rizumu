package com.kyrix.ftxmusic.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsQueryTest {

    @Test
    fun `strips official video suffix`() {
        assertEquals(
            "Cherry Wine",
            LyricsQuery.cleanTitle("Cherry Wine (Official Video)"),
        )
    }

    @Test
    fun `strips multiple noise tags`() {
        assertEquals(
            "Take Me to Church",
            LyricsQuery.cleanTitle("Take Me to Church [Official Music Video] (HD)"),
        )
    }

    @Test
    fun `keeps meaningful parentheses`() {
        assertEquals(
            "Halo",
            LyricsQuery.cleanTitle("Halo (Live at the Grammy Awards)"),
        )
        assertEquals(
            "Somebody That I Used to Know",
            LyricsQuery.cleanTitle("Somebody That I Used to Know"),
        )
    }

    @Test
    fun `falls back to original when everything is stripped`() {
        assertEquals("(Official Video)", LyricsQuery.cleanTitle("(Official Video)"))
    }

    @Test
    fun `cleans topic channel and featured artist`() {
        assertEquals("Hozier", LyricsQuery.cleanArtist("Hozier - Topic"))
        assertEquals("Calvin Harris", LyricsQuery.cleanArtist("Calvin Harris feat. Rihanna"))
        assertEquals("Calvin Harris", LyricsQuery.cleanArtist("Calvin Harris ft. Rihanna"))
    }
}
