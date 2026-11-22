package com.rizumu.music.data.yt

object SearchRanker {
    private const val MIN_RELEVANT = 5
    private const val ARTIST_BOOST = 12

    fun rank(items: List<YoutubeTrack>, query: String): List<YoutubeTrack> {
        if (items.size < 2) return items
        val scored = score(items, query) ?: return items
        val relevant = scored.filter { it.score > 0 }
        val source = if (relevant.size >= MIN_RELEVANT) relevant else scored
        val topArtist = topArtist(items, query)
        return source
            .sortedWith(
                compareByDescending<Indexed> {
                    it.score + if (topArtist != null && it.track.artist.trim().lowercase() == topArtist) ARTIST_BOOST else 0
                }.thenBy { it.index },
            )
            .map { it.track }
            .let(SearchMerger::dedupe)
    }

    fun countRelevant(items: List<YoutubeTrack>, query: String): Int =
        score(items, query)?.count { it.score > 0 } ?: 0

    fun topArtist(items: List<YoutubeTrack>, query: String): String? {
        val tokens = tokens(query)
        if (tokens.isEmpty()) return null
        val phrase = query.trim().lowercase()
        return score(items, query)
            ?.groupBy { it.track.artist.trim().lowercase() }
            ?.mapValues { entry -> entry.value.sumOf { it.score } + entry.value.size }
            ?.filter { (artist, _) -> artist.contains(phrase) || tokens.any(artist::contains) }
            ?.maxByOrNull { it.value }
            ?.key
    }

    private fun tokens(query: String): List<String> = query.lowercase()
        .split(Regex("\\s+"))
        .filter { it.length >= 2 }

    private fun score(items: List<YoutubeTrack>, query: String): List<Indexed>? {
        val tokens = tokens(query)
        if (tokens.isEmpty()) return null
        val phrase = query.trim().lowercase()
        return items.mapIndexed { index, track ->
            val artist = track.artist.trim().lowercase()
            val title = track.title.trim().lowercase()
            var points = 0
            if (artist.contains(phrase)) points += 8
            if (title.contains(phrase)) points += 3
            for (token in tokens) {
                if (artist.contains(token)) points += 4
                if (title.contains(token)) points += 2
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
