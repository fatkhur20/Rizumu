package com.kyrix.ftxmusic

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kyrix.ftxmusic.data.charts.ChartCache
import com.kyrix.ftxmusic.data.charts.ChartRepository
import com.kyrix.ftxmusic.data.local.UserLibrary
import com.kyrix.ftxmusic.data.local.download.DownloadStore
import com.kyrix.ftxmusic.data.local.playlist.PlaylistStore
import com.kyrix.ftxmusic.data.lyrics.LyricsStore
import com.kyrix.ftxmusic.data.update.UpdateRepository
import com.kyrix.ftxmusic.data.yt.FtxDownloader
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe

class FtxApp : Application(), ImageLoaderFactory {

    lateinit var playback: PlaybackManager
        private set

    lateinit var charts: ChartRepository
        private set

    lateinit var updates: UpdateRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("artwork_cache"))
                .maxSizeBytes(256L * 1024L * 1024L)
                .build()
        }
        .crossfade(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        UserLibrary.init(this)
        PlaylistStore.init(this)
        DownloadStore.init(this)
        LyricsStore.init(this)
        charts = ChartRepository(ChartCache(this))
        updates = UpdateRepository(this)
        NewPipe.init(FtxDownloader())
        playback = PlaybackManager(this)

        appScope.launch {
            var lastMediaId: String? = null
            playback.state.collect { state ->
                val id = state.mediaId ?: return@collect
                if (id == lastMediaId) return@collect
                lastMediaId = id
                UserLibrary.recordPlayed(
                    YoutubeTrack(
                        videoId = id,
                        title = state.title,
                        artist = state.artist,
                        durationSec = state.durationMs / 1000L,
                        thumbnailUrl = state.artworkUrl,
                    ),
                )
            }
        }
    }
}
