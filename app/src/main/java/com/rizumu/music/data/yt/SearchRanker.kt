package com.rizumu.music.data.yt

object SearchRanker {
    private const val MIN_RELEVANT = 5

    fun rank(items: List<YoutubeTrack>, query: String): List<YoutubeTrack> {
        if (items.size < 2) return items
        val scored = score(items, query) ?: return items
        val relevant = scored.filter { it.score > 0 }
        val source = if (relevant.size >= MIN_RELEVANT) relevant else scored
        return source.sortedWith(compareByDescending<Indexed> { it.score }.thenBy { it.index })
            .map { it.track }
    }

    fun countRelevant(items: List<YoutubeTrack>, query: String): Int =
        score(items, query)?.count { it.score > 0 } ?: 0

    private fun score(items: List<YoutubeTrack>, query: String): List<Indexed>? {
        val tokens = query.lowercase()
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
        if (tokens.isEmpty()) return null
        val phrase = query.lowercase()
        return items.mapIndexed { index, track ->
            val title = track.title.lowercase()
            val artist = track.artist.lowercase()
            var points = 0
            if (title.contains(phrase)) points += 4
            for (token in tokens) {
                if (title.contains(token)) points += 2
                if (artist.contains(token)) points += 1
            }
            Indexed(track, index, points)
        }
    }

    private data class Indexed(
        val track: YoutubeTrack,
        val index: Int,
        val score: Int,
    )
}
