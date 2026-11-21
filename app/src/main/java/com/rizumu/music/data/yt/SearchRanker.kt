package com.rizumu.music.data.yt

object SearchRanker {

    fun rank(items: List<YoutubeTrack>, query: String): List<YoutubeTrack> {
        if (items.size < 2) return items
        val tokens = query.lowercase()
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
        if (tokens.isEmpty()) return items

        val scored = items.mapIndexed { index, track ->
            val title = track.title.lowercase()
            val artist = track.artist.lowercase()
            var score = 0
            if (title.contains(query.lowercase())) score += 4
            for (token in tokens) {
                if (title.contains(token)) score += 2
                if (artist.contains(token)) score += 1
            }
            Indexed(track, index, score)
        }
        val relevant = scored.filter { it.score > 0 }
        val source = if (relevant.size >= 5) relevant else scored
        return source.sortedWith(compareByDescending<Indexed> { it.score }.thenBy { it.index })
            .map { it.track }
    }

    private data class Indexed(
        val track: YoutubeTrack,
        val index: Int,
        val score: Int,
    )
}
