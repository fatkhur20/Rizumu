package com.kyrix.ftxmusic.data.yt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSearchParserTest {

    private val fixture = """
        {
          "contents": {
            "twoColumnSearchResultsRenderer": {
              "primaryContents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "itemSectionRenderer": {
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Cherry Wine" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        { "text": "Hozier" },
                                        { "text": " • " },
                                        { "text": "Hozier" },
                                        { "text": " • " },
                                        { "text": "4:12" }
                                      ]
                                    }
                                  }
                                }
                              ],
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      { "url": "https://lh3.googleusercontent.com/abc=w60-h60-l90-rj" },
                                      { "url": "https://lh3.googleusercontent.com/abc=w120-h120-l90-rj" }
                                    ]
                                  }
                                }
                              },
                              "playlistItemData": { "videoId": "aaaaaaa1111" }
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Hozier" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Artis" } ] }
                                  }
                                }
                              ],
                              "navigationEndpoint": {
                                "browseEndpoint": { "browseId": "UCabc" }
                              }
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Take Me to Church" } ] }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        { "text": "Hozier" },
                                        { "text": " • " },
                                        { "text": "298M views" },
                                        { "text": " • " },
                                        { "text": "4:09" }
                                      ]
                                    }
                                  }
                                }
                              ],
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      { "url": "https://i.ytimg.com/vi/bbbbbbb2222/hqdefault.jpg" }
                                    ]
                                  }
                                }
                              },
                              "playlistItemData": { "videoId": "bbbbbbb2222" }
                            }
                          }
                        ],
                        "continuations": [
                          {
                            "nextContinuationData": { "continuation": "CONT-123" }
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
    fun `parses songs and skips artist rows`() {
        val page = MusicSearchParser.parse(fixture)

        assertEquals(2, page.items.size)
        assertEquals("Cherry Wine", page.items[0].title)
        assertEquals("Hozier", page.items[0].artist)
        assertEquals(252L, page.items[0].durationSec)
        assertEquals("aaaaaaa1111", page.items[0].videoId)
    }

    @Test
    fun `upgrades square cover to 1200`() {
        val page = MusicSearchParser.parse(fixture)

        assertEquals(
            "https://lh3.googleusercontent.com/abc=w1200-h1200-l90-rj",
            page.items[0].thumbnailUrl,
        )
    }

    @Test
    fun `parses video rows without touching ytimg covers`() {
        val page = MusicSearchParser.parse(fixture)
        val video = page.items[1]

        assertEquals(249L, video.durationSec)
        assertEquals("https://i.ytimg.com/vi/bbbbbbb2222/hqdefault.jpg", video.thumbnailUrl)
    }

    @Test
    fun `exposes continuation`() {
        assertEquals("CONT-123", MusicSearchParser.parse(fixture).continuation)
    }

    @Test
    fun `garbage json yields empty page`() {
        val page = MusicSearchParser.parse("{not json")

        assertTrue(page.items.isEmpty())
        assertNull(page.continuation)
    }

    @Test
    fun `filter params are the verified innertube values`() {
        assertEquals("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D", MusicSearch.paramsFor(MusicSearchFilter.SONGS))
        assertEquals("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D", MusicSearch.paramsFor(MusicSearchFilter.VIDEOS))
        assertNull(MusicSearch.paramsFor(MusicSearchFilter.ALL))
    }
}
