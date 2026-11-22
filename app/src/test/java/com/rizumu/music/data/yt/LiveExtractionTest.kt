package com.rizumu.music.data.yt

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import kotlin.test.assertTrue

class LiveExtractionTest {

    @Test
    fun searchAndResolveFirstTrack() = runBlocking {
        assumeTrue("set FTX_LIVE=1 to run live network tests", System.getenv("FTX_LIVE") == "1")
        NewPipe.init(FtxDownloader())

        val client = YoutubeClient()
        val page = client.search("lofi hip hop")
        println("search items: ${page.items.size}")
        page.items.take(3).forEach { println(" - ${it.videoId} | ${it.title} | ${it.artist}") }
        assertTrue(page.items.isNotEmpty(), "search returned no tracks")

        val first = page.items.first()
        val audio = client.resolveAudio(first.videoId)
        println("resolved: ${audio.mimeType} ${audio.bitrate}kbps ${audio.durationSec}s")
        println("url: ${audio.url.take(120)}")
        assertTrue(audio.url.startsWith("http"), "audio url is not http")
        assertTrue(audio.durationSec > 0, "duration was not extracted")
    }



    @Test
    fun singleWordArtistRanking() = runBlocking {
        assumeTrue("set FTX_LIVE=1 to run live network tests", System.getenv("FTX_LIVE") == "1")
        NewPipe.init(FtxDownloader())

        val page = YoutubeClient().search("romi", SearchFilter.ALL)
        println("items: ${page.items.size}")
        page.items.take(6).forEach { println(" - ${it.title} | ${it.artist}") }
        assertTrue(page.items.isNotEmpty())
    }

}
