package com.rizumu.music.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

    @Test
    fun `parses timestamps with and without fractions`() {
        val lines = LyricsParser.parseLrc(
            """
            [00:12.50]First line
            [00:20]Second line
            [01:02.123]Third line
            """.trimIndent(),
        )

        assertEquals(3, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("First line", lines[0].text)
        assertEquals(20_000L, lines[1].timeMs)
        assertEquals(62_123L, lines[2].timeMs)
    }

    @Test
    fun `supports multiple timestamps per line`() {
        val lines = LyricsParser.parseLrc("[00:05.00][00:15.00]Chorus")

        assertEquals(2, lines.size)
        assertEquals(5_000L, lines[0].timeMs)
        assertEquals(15_000L, lines[1].timeMs)
        assertTrue(lines.all { it.text == "Chorus" })
    }

    @Test
    fun `applies offset tag`() {
        val lines = LyricsParser.parseLrc("[offset:-500]\n[00:10.00]Shifted")

        assertEquals(9_500L, lines.single().timeMs)
    }

    @Test
    fun `ignores metadata and empty lines`() {
        val lines = LyricsParser.parseLrc(
            """
            [ar:Hozier]
            [ti:Cherry Wine]
            [00:01.00]
            [00:02.00]Real line
            """.trimIndent(),
        )

        assertEquals(listOf("Real line"), lines.map { it.text })
    }

    @Test
    fun `sorts by time`() {
        val lines = LyricsParser.parseLrc("[00:30.00]Later\n[00:10.00]Earlier")

        assertEquals(listOf("Earlier", "Later"), lines.map { it.text })
    }

    @Test
    fun `blank input yields empty list`() {
        assertTrue(LyricsParser.parseLrc(null).isEmpty())
        assertTrue(LyricsParser.parseLrc("   \n  ").isEmpty())
        assertTrue(LyricsParser.plainLines(null).isEmpty())
    }

    @Test
    fun `positive offset delays the highlight`() {
        val lines = LyricsParser.parseLrc("[00:10.00]Two\n[00:20.00]Three")

        assertEquals(0, LyricsParser.activeIndex(lines, 20_200, offsetMs = 300))
        assertEquals(1, LyricsParser.activeIndex(lines, 20_300, offsetMs = 300))
    }

    @Test
    fun `negative offset advances the highlight`() {
        val lines = LyricsParser.parseLrc("[00:10.00]Two\n[00:20.00]Three")

        assertEquals(0, LyricsParser.activeIndex(lines, 19_600, offsetMs = -300))
        assertEquals(1, LyricsParser.activeIndex(lines, 19_700, offsetMs = -300))
    }

    @Test
    fun `active index follows playback position`() {
        val lines = LyricsParser.parseLrc(
            "[00:00.00]One\n[00:10.00]Two\n[00:20.00]Three",
        )

        assertEquals(0, LyricsParser.activeIndex(lines, 0))
        assertEquals(0, LyricsParser.activeIndex(lines, 9_999))
        assertEquals(1, LyricsParser.activeIndex(lines, 10_000))
        assertEquals(2, LyricsParser.activeIndex(lines, 60_000))
        assertEquals(-1, LyricsParser.activeIndex(emptyList(), 1_000))
    }
}
