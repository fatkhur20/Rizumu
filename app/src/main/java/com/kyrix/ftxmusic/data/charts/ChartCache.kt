package com.kyrix.ftxmusic.data.charts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ChartCache(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(region: String, kind: ChartKind): ChartSnapshot? {
        val raw = prefs.getString(key(region, kind), null) ?: return null
        return runCatching { decode(raw) }.getOrNull()
    }

    fun write(snapshot: ChartSnapshot) {
        prefs.edit()
            .putString(key(snapshot.region, snapshot.kind), encode(snapshot))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun key(region: String, kind: ChartKind) = "chart_${kind.key}_$region"

    private fun encode(snapshot: ChartSnapshot): String {
        val entries = JSONArray()
        snapshot.entries.forEach { entry ->
            entries.put(
                JSONObject().apply {
                    put("rank", entry.rank)
                    put("title", entry.title)
                    put("artist", entry.artist)
                    put("thumbnailUrl", entry.thumbnailUrl)
                    put("durationSec", entry.durationSec)
                    put("videoId", entry.videoId)
                    put("chartRegion", entry.chartRegion)
                    put("fetchedAt", entry.fetchedAt)
                    put("source", entry.source)
                },
            )
        }
        return JSONObject().apply {
            put("region", snapshot.region)
            put("kind", snapshot.kind.key)
            put("chartTitle", snapshot.chartTitle)
            put("fetchedAt", snapshot.fetchedAt)
            put("source", snapshot.source)
            put("entries", entries)
        }.toString()
    }

    private fun decode(raw: String): ChartSnapshot {
        val root = JSONObject(raw)
        val region = root.getString("region")
        val kind = ChartKind.entries.firstOrNull { it.key == root.getString("kind") }
            ?: throw IllegalArgumentException("Unknown chart kind")
        val fetchedAt = root.getLong("fetchedAt")
        val source = root.optString("source").ifBlank { ChartParser.SOURCE }
        val array = root.optJSONArray("entries") ?: JSONArray()
        val entries = ArrayList<ChartEntry>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val videoId = item.optString("videoId")
            val title = item.optString("title")
            if (videoId.isBlank() || title.isBlank()) continue
            entries += ChartEntry(
                rank = item.optInt("rank", entries.size + 1),
                title = title,
                artist = item.optString("artist"),
                thumbnailUrl = if (item.isNull("thumbnailUrl")) null else item.optString("thumbnailUrl"),
                durationSec = if (item.isNull("durationSec")) null else item.optLong("durationSec"),
                videoId = videoId,
                chartRegion = item.optString("chartRegion").ifBlank { region },
                fetchedAt = item.optLong("fetchedAt", fetchedAt),
                source = item.optString("source").ifBlank { source },
            )
        }
        return ChartSnapshot(
            region = region,
            kind = kind,
            chartTitle = root.optString("chartTitle"),
            entries = entries,
            fetchedAt = fetchedAt,
            source = source,
        )
    }

    private companion object {
        const val PREFS = "ftx_charts"
    }
}
