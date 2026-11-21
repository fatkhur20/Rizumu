package com.rizumu.music.data.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartParserTest {

    private val indexJson = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicCarouselShelfRenderer": {
                              "contents": [
                                {
                                  "musicTwoRowItemRenderer": {
                                    "title": { "runs": [ { "text": "Trending 20 Indonesia" } ] },
                                    "navigationEndpoint": { "browseEndpoint": { "browseId": "VLOLAK5uy_test" } }
                                  }
                                },
                                {
                                  "musicTwoRowItemRenderer": {
                                    "title": { "runs": [ { "text": "Daily Top Music Videos - Indonesia" } ] },
                                    "navigationEndpoint": { "browseEndpoint": { "browseId": "VLPLdaily" } }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
    """.trimIndent()

    private val playlistJson = """
        {
          "contents": {
            "twoColumnBrowseResultsRenderer": {
              "secondaryContents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "musicPlaylistShelfRenderer": {
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "customIndexColumn": {
                                "musicCustomIndexColumnRenderer": { "text": { "runs": [ { "text": "1" } ] } }
                              },
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      { "url": "https://img/small.jpg", "width": 60, "height": 60 },
                                      { "url": "https://img/large.jpg", "width": 400, "height": 400 }
                                    ]
                                  }
                                }
                              },
                              "playlistItemData": { "videoId": "vidOne" },
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Laut Kidul" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Jeyma Monica" } ] }
                                  }
                                }
                              ],
                              "fixedColumns": [
                                {
                                  "musicResponsiveListItemFixedColumnRenderer": {
                                    "text": { "runs": [ { "text": "5.57" } ] }
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "customIndexColumn": {
                                "musicCustomIndexColumnRenderer": { "text": { "runs": [ { "text": "2" } ] } }
                              },
                              "playlistItemData": { "videoId": "vidTwo" },
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Terbelah Jadi Dua" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Jeyma Monica" } ] }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `parses chart playlists and selects trending songs`() {
        val playlists = ChartParser.parseChartPlaylists(indexJson)
        assertEquals(2, playlists.size)
        assertEquals("VLOLAK5uy_test", ChartParser.selectTrendingSongs(playlists)?.browseId)
        assertEquals("VLPLdaily", ChartParser.selectDailyMusicVideos(playlists)?.browseId)
    }

    @Test
    fun `falls back to localized trending title when playlist id is not auto generated`() {
        val json = """
            {"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[{"musicCarouselShelfRenderer":{"contents":[
            {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Trending 20 Brasil"}]},
            "navigationEndpoint":{"browseEndpoint":{"browseId":"VLPLcustom"}}}}]}}]}}}}]}}}}
        """.trimIndent()
        val playlists = ChartParser.parseChartPlaylists(json)
        assertEquals("VLPLcustom", ChartParser.selectTrendingSongs(playlists)?.browseId)
    }

    @Test
    fun `preserves provider ranking and builds chart entries`() {
        val fetchedAt = 1_700_000_000_000L
        val snapshot = ChartParser.parsePlaylist(
            json = playlistJson,
            region = "ID",
            kind = ChartKind.SONGS,
            chartTitle = "Trending 20 Indonesia",
            fetchedAt = fetchedAt,
            limit = 20,
        )

        assertEquals("ID", snapshot.region)
        assertEquals(ChartKind.SONGS, snapshot.kind)
        assertEquals("Trending 20 Indonesia", snapshot.chartTitle)
        assertEquals(ChartParser.SOURCE, snapshot.source)
        assertEquals(2, snapshot.entries.size)

        val first = snapshot.entries[0]
        assertEquals(1, first.rank)
        assertEquals("Laut Kidul", first.title)
        assertEquals("Jeyma Monica", first.artist)
        assertEquals("vidOne", first.videoId)
        assertEquals(357L, first.durationSec ?: -1L)
        assertEquals("https://img/large.jpg", first.thumbnailUrl)
        assertEquals("ID", first.chartRegion)
        assertEquals(fetchedAt, first.fetchedAt)
        assertEquals(ChartParser.SOURCE, first.source)

        assertEquals(2, snapshot.entries[1].rank)
        assertEquals("vidTwo", snapshot.entries[1].videoId)
        assertNull(snapshot.entries[1].durationSec)
        assertEquals("Terbelah Jadi Dua", snapshot.entries[1].title)
    }

    @Test
    fun `respects limit without changing order`() {
        val snapshot = ChartParser.parsePlaylist(
            json = playlistJson,
            region = "ID",
            kind = ChartKind.SONGS,
            chartTitle = "Trending 20 Indonesia",
            fetchedAt = 1L,
            limit = 1,
        )
        assertEquals(1, snapshot.entries.size)
        assertEquals(1, snapshot.entries[0].rank)
        assertEquals("vidOne", snapshot.entries[0].videoId)
    }

    @Test
    fun `parses chart durations`() {
        assertEquals(357L, ChartParser.parseDuration("5.57"))
        assertEquals(357L, ChartParser.parseDuration("5:57"))
        assertEquals(3723L, ChartParser.parseDuration("1.02.03"))
        assertEquals(42L, ChartParser.parseDuration("42"))
        assertNull(ChartParser.parseDuration(""))
        assertNull(ChartParser.parseDuration(null))
    }

    @Test
    fun `chart entry maps to playable track without altering rank data`() {
        val entry = ChartEntry(
            rank = 3,
            title = "Song",
            artist = "Artist",
            thumbnailUrl = null,
            durationSec = 200L,
            videoId = "abc123",
            chartRegion = "ID",
            fetchedAt = 5L,
            source = ChartParser.SOURCE,
        )
        val track = entry.toTrack()
        assertEquals("abc123", track.videoId)
        assertEquals(200L, track.durationSec)
        assertEquals(3, entry.rank)
        assertTrue(entry.source.contains("Tangga lagu"))
        assertFalse(entry.source.isBlank())
    }
}
