package com.kyrix.ftxmusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.kyrix.ftxmusic.MainActivity
import com.kyrix.ftxmusic.data.local.download.DownloadStore
import com.kyrix.ftxmusic.data.yt.YoutubeClient
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
)

data class PlayerUiState(
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val mediaId: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = 0,
    val playbackSpeed: Float = 1f,
    val error: String? = null,
)

class PlaybackManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefetching = ConcurrentHashMap.newKeySet<String>()

    val youtube = YoutubeClient()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(USER_AGENT)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(20_000)
        .setReadTimeoutMs(30_000)

    private val baseDataSourceFactory = DefaultDataSource.Factory(
        appContext,
        httpDataSourceFactory,
    )

    private val resolvingFactory = ResolvingDataSource.Factory(baseDataSourceFactory) { dataSpec ->
        resolveDataSpec(dataSpec)
    }

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingFactory))
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(BACK_BUFFER_MS, true)
                .build(),
        )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        .build()
        .apply {
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }

    val sleepTimer = SleepTimerController(player, scope)

    val mediaSession: MediaSession = MediaSession.Builder(
        appContext,
        player,
    )
        .setSessionActivity(sessionActivity())
        .build()

    private var retryAttempts = 0

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            retryAttempts = 0
            _state.update { it.copy(error = null) }
            prefetchNext()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (PlaybackErrorPolicy.shouldRetry(retryAttempts)) {
                retryAttempts++
                _state.update { it.copy(error = null) }
                youtube.invalidate(player.currentMediaItem?.mediaId.orEmpty())
                player.prepare()
                player.playWhenReady = true
            } else {
                _state.update { it.copy(error = PlaybackErrorPolicy.friendlyError(error)) }
            }
        }
    }

    init {
        player.addListener(listener)
        refreshState()
        scope.launch {
            while (isActive) {
                val current = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET }
                if (player.isPlaying || _state.value.positionMs != current) {
                    _state.update {
                        it.copy(
                            positionMs = current,
                            durationMs = duration ?: it.durationMs,
                        )
                    }
                }
                delay(500)
            }
        }
    }

    fun playTracks(tracks: List<YoutubeTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        retryAttempts = 0
        _state.update { it.copy(error = null) }
        val items = tracks.map { it.toMediaItem() }
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.playWhenReady = true
        prefetchNext()
    }

    fun playNext(track: YoutubeTrack) {
        val item = track.toMediaItem()
        if (player.mediaItemCount == 0) {
            player.setMediaItem(item)
            player.prepare()
            player.playWhenReady = true
            prefetchNext()
            return
        }
        val insertIndex = (player.currentMediaItemIndex + 1)
            .coerceIn(0, player.mediaItemCount)
        player.addMediaItem(insertIndex, item)
        refreshState()
    }

    fun enqueue(track: YoutubeTrack) {
        val wasEmpty = player.mediaItemCount == 0
        player.addMediaItem(track.toMediaItem())
        if (wasEmpty) {
            player.prepare()
            player.playWhenReady = true
            prefetchNext()
        } else {
            refreshState()
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        if (player.currentPosition > 3_000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: positionMs
        player.seekTo(positionMs.coerceIn(0L, duration))
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playAt(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun setPlaybackSpeed(value: Float) {
        player.setPlaybackSpeed(PlaybackSpeed.clamp(value))
        refreshState()
    }

    fun removeAt(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from == to) return
        if (from !in 0 until player.mediaItemCount) return
        if (to !in 0 until player.mediaItemCount) return
        player.moveMediaItem(from, to)
    }

    fun clearQueue() {
        player.stop()
        player.clearMediaItems()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun refreshState() {
        val current = player.currentMediaItem
        val metadata = current?.mediaMetadata
        val duration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
        val queue = (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            QueueItem(
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUrl = item.mediaMetadata.artworkUri?.toString(),
            )
        }

        _state.update { previous ->
            previous.copy(
                hasMedia = current != null,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                title = metadata?.title?.toString().orEmpty(),
                artist = metadata?.artist?.toString().orEmpty(),
                artworkUrl = metadata?.artworkUri?.toString(),
                mediaId = current?.mediaId,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                queue = queue,
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                playbackSpeed = player.playbackParameters.speed,
            )
        }
    }

    private fun prefetchNext() {
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex !in 0 until player.mediaItemCount) return
        val videoId = player.getMediaItemAt(nextIndex).mediaId
        if (videoId.isBlank() || !prefetching.add(videoId)) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { youtube.resolveAudio(videoId) }
            } catch (_: Exception) {
            } finally {
                prefetching.remove(videoId)
            }
        }
    }

    private fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SCHEME) return dataSpec
        val videoId = dataSpec.uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: return dataSpec
        DownloadStore.fileFor(videoId)?.let { local ->
            return dataSpec.withUri(Uri.fromFile(local))
        }
        return try {
            val resolved = runBlocking { withTimeout(30_000L) { youtube.resolveAudio(videoId) } }
            dataSpec.withUri(Uri.parse(resolved.url))
        } catch (error: Exception) {
            throw IOException("Gagal menyiapkan audio ($videoId)", error)
        }
    }

    private fun YoutubeTrack.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(videoId)
        .setUri("$SCHEME://track/$videoId")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(thumbnailUrl?.let(Uri::parse))
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()

    private fun sessionActivity(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun retryCurrent() {
        retryAttempts = 0
        _state.update { it.copy(error = null) }
        youtube.invalidate(player.currentMediaItem?.mediaId.orEmpty())
        player.prepare()
        player.playWhenReady = true
    }

    companion object {
        private const val SCHEME = "ftx"
        private const val MIN_BUFFER_MS = 15_000
        private const val MAX_BUFFER_MS = 60_000
        private const val BUFFER_FOR_PLAYBACK_MS = 1_000
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_500
        private const val BACK_BUFFER_MS = 10_000
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
