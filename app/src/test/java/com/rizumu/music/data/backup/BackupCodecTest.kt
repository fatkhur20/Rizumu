package com.rizumu.music.data.backup

import com.rizumu.music.data.local.ListeningStats
import com.rizumu.music.data.local.ListeningStatsLogic
import com.rizumu.music.data.yt.YoutubeTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupCodecTest {

    private val favorite = YoutubeTrack(
        videoId = "abc123",
        title = "Lagu Satu",
        artist = "Artis",
        durationSec = 210L,
        thumbnailUrl = "https://example.test/cover.jpg",
    )

    @Test
    fun roundTripPreservesLibraryAndPlaylists() {
        val payload = BackupPayload(
            exportedAt = 1_700_000_000_000L,
            favorites = listOf(favorite),
            recent = listOf(favorite.copy(videoId = "recent1")),
            searches = listOf("lofi", "jazz"),
            playlists = listOf(
                BackupPlaylist(
                    name = "Favorit",
                    createdAt = 123L,
                    tracks = listOf(favorite),
                ),
            ),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(payload))!!

        assertEquals(1_700_000_000_000L, decoded.exportedAt)
        assertEquals(listOf(favorite), decoded.favorites)
        assertEquals("recent1", decoded.recent.single().videoId)
        assertEquals(listOf("lofi", "jazz"), decoded.searches)
        assertEquals("Favorit", decoded.playlists.single().name)
        assertEquals(favorite, decoded.playlists.single().tracks.single())
    }

    @Test
    fun rejectsUnsupportedSchema() {
        assertNull(BackupCodec.decode("""{"schema":3,"favorites":[]}"""))
        assertNull(BackupCodec.decode("not json"))
    }

    @Test
    fun dropsBlankTracksAndNormalizesPlaylistNames() {
        val raw = """
            {
              "schema": 1,
              "favorites": [
                {"videoId": "", "title": "rusak"},
                {"videoId": "ok", "title": "Valid"}
              ],
              "playlists": [
                {"name": "  Nama   Rapi  ", "tracks": [{"videoId": "ok", "title": "Valid"}]}
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(raw)!!

        assertEquals(1, decoded.favorites.size)
        assertEquals("ok", decoded.favorites.single().videoId)
        assertEquals("Nama Rapi", decoded.playlists.single().name)
        assertTrue(decoded.recent.isEmpty())
    }

    @Test
    fun v2RoundTripPreservesStats() {
        val stats = ListeningStatsLogic.addListening(
            ListeningStatsLogic.registerPlay(ListeningStats(), "abc123", "Lagu Satu", "Artis", null, 1_000L),
            "abc123", "Lagu Satu", "Artis", null, 7_000L, 2_000L,
        )
        val payload = BackupPayload(
            exportedAt = 1L,
            favorites = emptyList(),
            recent = emptyList(),
            searches = emptyList(),
            playlists = emptyList(),
            stats = stats,
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(payload))!!

        assertEquals(1, decoded.stats.tracks["abc123"]!!.playCount)
        assertEquals(7_000L, decoded.stats.tracks["abc123"]!!.listenedMs)
        assertEquals(stats.byArtist, decoded.stats.byArtist)
    }

    @Test
    fun v1BackupDecodesWithEmptyStats() {
        val decoded = BackupCodec.decode("""{"schema":1,"favorites":[]}""")!!

        assertTrue(decoded.stats.tracks.isEmpty())
    }

}
