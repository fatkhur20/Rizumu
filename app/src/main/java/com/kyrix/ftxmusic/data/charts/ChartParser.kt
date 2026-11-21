package com.kyrix.ftxmusic.data.charts

import org.json.JSONArray
import org.json.JSONObject

data class ChartPlaylist(val title: String, val browseId: String)

object ChartParser {

    const val SOURCE = "Tangga lagu"

    fun parseChartPlaylists(json: String): List<ChartPlaylist> {
        val root = JSONObject(json)
        val sections = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        val found = LinkedHashMap<String, ChartPlaylist>()
        if (sections != null) {
            for (i in 0 until sections.length()) {
                val section = sections.optJSONObject(i) ?: continue
                val carousel = section.optJSONObject("musicCarouselShelfRenderer") ?: continue
                val contents = carousel.optJSONArray("contents") ?: continue
                for (j in 0 until contents.length()) {
                    val item = contents.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                    val title = runsText(item.optJSONObject("title"))
                    val browseId = item.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId")
                        .orEmpty()
                    if (title.isNotBlank() && browseId.startsWith("VL")) {
                        found[browseId] = ChartPlaylist(title, browseId)
                    }
                }
            }
        }

        if (found.isEmpty()) {
            // Defensive fallback: collect chart playlist tiles anywhere in the response.
            for (item in collect(root, "musicTwoRowItemRenderer")) {
                val title = runsText(item.optJSONObject("title"))
                val browseId = item.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                    .orEmpty()
                if (title.isNotBlank() && browseId.startsWith("VL")) {
                    found[browseId] = ChartPlaylist(title, browseId)
                }
            }
        }

        return found.values.toList()
    }

    fun selectTrendingSongs(playlists: List<ChartPlaylist>): ChartPlaylist? =
        playlists.firstOrNull { it.browseId.startsWith("VLOLAK5uy_") }
            ?: playlists.firstOrNull { it.title.startsWith("Trending 20", ignoreCase = true) }

    fun selectDailyMusicVideos(playlists: List<ChartPlaylist>): ChartPlaylist? =
        playlists.firstOrNull { it.title.startsWith("Daily Top Music Videos", ignoreCase = true) }
            ?: playlists.firstOrNull {
                it.title.contains("Top Music Videos", ignoreCase = true) &&
                    !it.title.contains("Live", ignoreCase = true)
            }
            ?: playlists.firstOrNull {
                it.title.contains("Music Videos", ignoreCase = true) &&
                    !it.title.contains("Live", ignoreCase = true)
            }

    fun parsePlaylist(
        json: String,
        region: String,
        kind: ChartKind,
        chartTitle: String,
        fetchedAt: Long,
        limit: Int,
    ): ChartSnapshot {
        val root = JSONObject(json)
        val shelfItems = root.optJSONObject("contents")
            ?.optJSONObject("twoColumnBrowseResultsRenderer")
            ?.optJSONObject("secondaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?.optJSONObject(0)
            ?.optJSONObject("musicPlaylistShelfRenderer")
            ?.optJSONArray("contents")

        val renderers = if (shelfItems != null) {
            buildList {
                for (i in 0 until shelfItems.length()) {
                    shelfItems.optJSONObject(i)
                        ?.optJSONObject("musicResponsiveListItemRenderer")
                        ?.let(::add)
                }
            }
        } else {
            collect(root, "musicResponsiveListItemRenderer")
        }

        val entries = ArrayList<ChartEntry>(minOf(limit, renderers.size))
        renderers.forEachIndexed { index, item ->
            if (entries.size >= limit) return@forEachIndexed
            val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
                ?: findString(item, "videoId")?.takeIf { it.isNotBlank() }
                ?: return@forEachIndexed
            val title = flexText(item, 0)
            if (title.isBlank()) return@forEachIndexed
            val artist = flexText(item, 1)
            val rank = runsText(
                item.optJSONObject("customIndexColumn")
                    ?.optJSONObject("musicCustomIndexColumnRenderer")
                    ?.optJSONObject("text"),
            ).toIntOrNull() ?: (index + 1)
            val duration = parseDuration(fixedText(item, 0))
            val thumbnail = largestThumbnail(item.optJSONObject("thumbnail"))

            entries += ChartEntry(
                rank = rank,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnail,
                durationSec = duration,
                videoId = videoId,
                chartRegion = region,
                fetchedAt = fetchedAt,
                source = SOURCE,
            )
        }

        return ChartSnapshot(
            region = region,
            kind = kind,
            chartTitle = chartTitle,
            entries = entries,
            fetchedAt = fetchedAt,
            source = SOURCE,
        )
    }

    fun parseDuration(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val parts = text.split(Regex("[.:]")).mapNotNull { it.trim().toIntOrNull() }
        if (parts.isEmpty() || parts.any { it < 0 }) return null
        return when (parts.size) {
            1 -> parts[0].toLong()
            2 -> parts[0] * 60L + parts[1]
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            else -> null
        }
    }

    private fun flexText(item: JSONObject, index: Int): String {
        val column = item.optJSONArray("flexColumns")
            ?.optJSONObject(index)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?: return ""
        return runsText(column.optJSONObject("text"))
    }

    private fun fixedText(item: JSONObject, index: Int): String {
        val column = item.optJSONArray("fixedColumns")
            ?.optJSONObject(index)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            ?: return ""
        return runsText(column.optJSONObject("text"))
    }

    private fun runsText(text: JSONObject?): String {
        if (text == null) return ""
        val runs = text.optJSONArray("runs")
        if (runs != null) {
            val builder = StringBuilder()
            for (i in 0 until runs.length()) {
                builder.append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
            return builder.toString().trim()
        }
        return text.optString("simpleText").trim()
    }

    private fun largestThumbnail(thumbnail: JSONObject?): String? {
        val thumbnails = thumbnail
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: return null
        var best: String? = null
        var bestArea = -1L
        for (i in 0 until thumbnails.length()) {
            val item = thumbnails.optJSONObject(i) ?: continue
            val url = item.optString("url").takeIf { it.isNotBlank() } ?: continue
            val area = item.optLong("width", 0L) * item.optLong("height", 0L)
            if (area >= bestArea) {
                best = url
                bestArea = area
            }
        }
        return best
    }

    private fun findString(node: Any?, key: String): String? {
        when (node) {
            is JSONObject -> {
                val direct = node.optString(key, "")
                if (direct.isNotBlank()) return direct
                val keys = node.keys()
                while (keys.hasNext()) {
                    findString(node.opt(keys.next()), key)?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findString(node.opt(i), key)?.let { return it }
                }
            }
        }
        return null
    }

    private fun collect(node: Any?, key: String): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        fun walk(current: Any?) {
            when (current) {
                is JSONObject -> {
                    current.optJSONObject(key)?.let(out::add)
                    val keys = current.keys()
                    while (keys.hasNext()) walk(current.opt(keys.next()))
                }
                is JSONArray -> {
                    for (i in 0 until current.length()) walk(current.opt(i))
                }
            }
        }
        walk(node)
        return out
    }
}
