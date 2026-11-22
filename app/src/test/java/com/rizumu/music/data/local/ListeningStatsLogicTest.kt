package com.rizumu.music.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ListeningStatsLogicTest {

    private fun play(videoId: String, artist: String, count: Int = 1): ListeningStats {
        var stats = ListeningStats()
        repeat(count) { index ->
            stats = ListeningStatsLogic.registerPlay(
                stats, videoId, "Judul $videoId", artist, null, 1_000L * index,
            )
        }
        return stats
    }

    @Test
    fun `play and listening time accumulate for same track`() {
        var stats = play("v1", "Romi Jahat")
        stats = ListeningStatsLogic.addListening(
            stats, "v1", "Satu Satu", "Romi Jahat", null, 10_000L, 2_000L,
        )
        stats = ListeningStatsLogic.addListening(
            stats, "v1", "Satu Satu", "Romi Jahat", null, 5_000L, 3_000L,
        )

        assertEquals(1, stats.tracks["v1"]!!.playCount)
        assertEquals(15_000L, stats.tracks["v1"]!!.listenedMs)
        assertEquals(15_000L, ListeningStatsLogic.totalMs(stats))
        assertEquals("romi jahat", stats.byArtist.keys.single())
    }

    @Test
    fun `non positive and seek sized deltas are ignored`() {
        var stats = play("v1", "Artis")
        stats = ListeningStatsLogic.addListening(stats, "v1", "", "", null, -1L, 0L)

        assertEquals(0L, stats.tracks["v1"]!!.listenedMs)
        assertEquals(false, ListeningStatsLogic.acceptableDelta(-1L))
        assertEquals(false, ListeningStatsLogic.acceptableDelta(0L))
        assertEquals(false, ListeningStatsLogic.acceptableDelta(ListeningStatsLogic.MAX_DELTA_MS + 1L))
        assertEquals(true, ListeningStatsLogic.acceptableDelta(ListeningStatsLogic.MAX_DELTA_MS))
    }

    @Test
    fun `day key uses local calendar date`() {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            assertEquals("2026-11-22", ListeningStatsLogic.dayKey(1_795_305_600_000L))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `top artists are sorted by listened time`() {
        var stats = play("v1", "A")
        stats = ListeningStatsLogic.addListening(stats, "v1", "", "A", null, 5_000L, 0L)
        stats = ListeningStatsLogic.addListening(stats, "v2", "", "B", null, 9_000L, 0L)

        assertEquals("b", ListeningStatsLogic.topArtists(stats, 1).single().first)
    }

    @Test
    fun `merge sums counts and time and json round trips`() {
        var a = play("v1", "A")
        a = ListeningStatsLogic.addListening(a, "v1", "", "A", null, 4_000L, 0L)
        var b = play("v1", "A", 2)
        b = ListeningStatsLogic.addListening(b, "v1", "", "A", null, 6_000L, 0L)

        val merged = ListeningStatsLogic.merge(a, b)
        assertEquals(3, merged.tracks["v1"]!!.playCount)
        assertEquals(10_000L, merged.tracks["v1"]!!.listenedMs)

        val decoded = ListeningStatsJson.fromJson(ListeningStatsJson.toJson(merged).toString())
        assertEquals(merged.tracks["v1"]!!.playCount, decoded.tracks["v1"]!!.playCount)
        assertEquals(merged.byDay, decoded.byDay)
        assertTrue(decoded.byArtist.containsKey("a"))
    }
}
