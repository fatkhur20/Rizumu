package com.rizumu.music.data.yt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.ConcurrentHashMap

open class YoutubeClient(
    private val service: StreamingService = ServiceList.YouTube,
    private val musicSearch: MusicSearch = MusicSearch(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val resolvedCache = ConcurrentHashMap<String, CachedAudio>()
    private val locks = ConcurrentHashMap<String, Any>()

    open suspend fun search(
        query: String,
        filter: String = YoutubeSearchQueryHandlerFactory.MUSIC_SONGS,
        token: PageToken? = null,
    ): SearchPage = withContext(io) {
        require(query.isNotBlank()) { "query is blank" }

        if (token?.page == null) {
            if (filter == SearchFilter.ALL) {
                val songs = runCatching {
                    musicSearch.search(query, MusicSearchFilter.SONGS)
                }.getOrNull()
                val videos = runCatching {
                    musicSearch.search(query, MusicSearchFilter.VIDEOS)
                }.getOrNull()
                val merged = mutableListOf<YoutubeTrack>()
                val seen = HashSet<String>()
                listOfNotNull(songs, videos).forEach { page ->
                    page.items.forEach { track ->
                        if (seen.add(track.videoId)) merged.add(track)
                    }
                }
                if (merged.isNotEmpty()) {
                    return@withContext SearchPage(
                        items = SearchRanker.rank(merged, query),
                        next = null,
                    )
                }
            } else {
                val innerTube = runCatching {
                    musicSearch.search(query, filter.toMusicFilter(), token?.continuation)
                }.getOrNull()
                if (innerTube != null && innerTube.items.isNotEmpty()) {
                    return@withContext SearchPage(
                        items = SearchRanker.rank(innerTube.items, query),
                        next = innerTube.continuation?.let { PageToken(it) },
                    )
                }
            }
        }

        val filters = when (filter) {
            YoutubeSearchQueryHandlerFactory.MUSIC_SONGS,
            YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS -> listOf(filter)
            else -> emptyList()
        }
        val extractor = service.getSearchExtractor(
            YoutubeSearchQueryHandlerFactory.getInstance()
                .fromQuery(query.trim(), filters, ""),
        )

        val page: ListExtractor.InfoItemsPage<InfoItem> = if (token == null) {
            extractor.fetchPage()
            extractor.initialPage
        } else {
            extractor.getPage(token.page ?: error("missing page token"))
        }

        SearchPage(
            items = page.items.mapNotNull { it.toYoutubeTrack() },
            next = page.nextPageOrNull(),
        )
    }

    private fun String.toMusicFilter(): MusicSearchFilter = when (this) {
        YoutubeSearchQueryHandlerFactory.MUSIC_SONGS -> MusicSearchFilter.SONGS
        YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS -> MusicSearchFilter.VIDEOS
        SearchFilter.RECENT -> MusicSearchFilter.RECENT
        SearchFilter.WEEK -> MusicSearchFilter.WEEK
        SearchFilter.MONTH -> MusicSearchFilter.MONTH
        else -> MusicSearchFilter.ALL
    }

    suspend fun resolveAudio(videoId: String): ResolvedAudio = withContext(io) {
        cachedAudio(videoId)?.let { return@withContext it }

        val lock = locks.computeIfAbsent(videoId) { Any() }
        synchronized(lock) {
            cachedAudio(videoId) ?: extractAudio(videoId).also { storeAudio(videoId, it) }
        }
    }

    private fun extractAudio(videoId: String): ResolvedAudio {
        val extractor = service.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()

        val streams = extractor.audioStreams
        val audioOnly = streams.filter { it.deliveryMethod == DeliveryMethod.DASH }
        val chosen = (audioOnly.ifEmpty { streams }).sortedWith(
            compareByDescending<AudioStream> { it.format?.mimeType == "audio/mp4" }
                .thenByDescending { it.averageBitrate in 1..MAX_AUDIO_BITRATE }
                .thenByDescending { it.averageBitrate },
        ).firstOrNull() ?: error("No audio stream available for $videoId")

        return ResolvedAudio(
            videoId = videoId,
            url = chosen.content,
            mimeType = chosen.format?.mimeType ?: "audio/mp4",
            suffix = chosen.format?.suffix ?: "m4a",
            bitrate = chosen.averageBitrate,
            durationSec = runCatching { extractor.length }.getOrDefault(0L),
            title = extractor.name.orEmpty(),
            artist = extractor.uploaderName.orEmpty(),
            thumbnailUrl = runCatching { extractor.thumbnails.bestThumbnailUrl() }
                .getOrNull() ?: thumbnailFallback(videoId),
        )
    }

    private fun cachedAudio(videoId: String): ResolvedAudio? {
        val cached = resolvedCache[videoId] ?: return null
        return if (cached.expiresAtMillis > System.currentTimeMillis()) cached.audio else null
    }

    private fun storeAudio(videoId: String, audio: ResolvedAudio) {
        resolvedCache[videoId] = CachedAudio(
            audio = audio,
            expiresAtMillis = System.currentTimeMillis() + RESOLVE_TTL_MS,
        )
        if (resolvedCache.size > MAX_CACHE_ENTRIES) {
            val now = System.currentTimeMillis()
            resolvedCache.entries.removeIf { it.value.expiresAtMillis <= now }
            if (resolvedCache.size > MAX_CACHE_ENTRIES) {
                resolvedCache.keys.take(resolvedCache.size - MAX_CACHE_ENTRIES)
                    .forEach { resolvedCache.remove(it) }
            }
        }
    }

    suspend fun fetchPlaylist(sourceUrl: String): PlaylistFetch = withContext(io) {
        val extractor = service.getPlaylistExtractor(sourceUrl)
        extractor.fetchPage()
        val page = extractor.initialPage
        PlaylistFetch(
            sourceId = extractor.id,
            name = extractor.name.orEmpty(),
            uploader = runCatching { extractor.uploaderName }.getOrDefault(""),
            thumbnailUrl = runCatching { extractor.thumbnails.bestThumbnailUrl() }
                .getOrNull(),
            items = page.items.mapNotNull { it.toYoutubeTrack() },
            next = page.nextPageOrNull(),
        )
    }

    suspend fun fetchPlaylistNext(sourceUrl: String, token: PageToken): PlaylistFetch =
        withContext(io) {
            val extractor = service.getPlaylistExtractor(sourceUrl)
            val page = extractor.getPage(token.page ?: error("token halaman tidak valid"))
            PlaylistFetch(
                sourceId = extractor.id,
                name = extractor.name.orEmpty(),
                uploader = runCatching { extractor.uploaderName }.getOrDefault(""),
                thumbnailUrl = runCatching { extractor.thumbnails.bestThumbnailUrl() }
                    .getOrNull(),
                items = page.items.mapNotNull { it.toYoutubeTrack() },
                next = page.nextPageOrNull(),
            )
        }

    fun invalidate(videoId: String) {
        if (videoId.isNotBlank()) resolvedCache.remove(videoId)
    }

    fun clearUrlCache() {
        resolvedCache.clear()
        locks.clear()
    }

    private fun InfoItem.toYoutubeTrack(): YoutubeTrack? {
        val item = this as? StreamInfoItem ?: return null
        val id = videoIdFromUrl(item.url) ?: return null
        return YoutubeTrack(
            videoId = id,
            title = item.name.orEmpty(),
            artist = item.uploaderName.orEmpty(),
            durationSec = item.duration,
            thumbnailUrl = item.thumbnails.bestThumbnailUrl() ?: thumbnailFallback(id),
        )
    }

    private fun <T : InfoItem> ListExtractor.InfoItemsPage<T>.nextPageOrNull(): PageToken? =
        if (hasNextPage()) nextPage?.let { PageToken(it) } else null

    private data class CachedAudio(
        val audio: ResolvedAudio,
        val expiresAtMillis: Long,
    )

    companion object {
        private const val RESOLVE_TTL_MS = 45 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 80
        private const val MAX_AUDIO_BITRATE = 160_000
    }
}
