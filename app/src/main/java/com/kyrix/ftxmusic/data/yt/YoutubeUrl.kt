package com.kyrix.ftxmusic.data.yt

import org.schabi.newpipe.extractor.Image
import java.net.URI

private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

fun videoIdFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    if (VIDEO_ID.matches(trimmed)) return trimmed

    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val path = uri.path.orEmpty()

    if (host == "youtu.be" || host.endsWith(".youtu.be")) {
        return path.trim('/').takeIf { VIDEO_ID.matches(it) }
    }

    if (host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")) {
        if (path.startsWith("/shorts/") || path.startsWith("/embed/") || path.startsWith("/v/")) {
            return path.trim('/').split('/').lastOrNull()?.takeIf { VIDEO_ID.matches(it) }
        }
        val query = uri.rawQuery.orEmpty()
        val value = query.split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=', "")
                if (key == "v") part.substringAfter('=', "") else null
            }
            .firstOrNull()
        return value?.takeIf { VIDEO_ID.matches(it) }
    }

    return null
}

fun playlistIdFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    val query = uri.rawQuery.orEmpty()
    return query.split('&')
        .mapNotNull { part ->
            val key = part.substringBefore('=', "")
            if (key == "list") part.substringAfter('=', "") else null
        }
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
}

fun List<Image>.bestThumbnailUrl(): String? =
    maxByOrNull { it.width.toLong() * it.height.toLong() }?.url
        ?: firstOrNull()?.url

fun thumbnailFallback(videoId: String): String =
    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
