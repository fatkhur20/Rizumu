package com.rizumu.music.data.yt

object ChannelMatcher {
    fun select(query: String, candidates: List<ChannelHit>): ChannelHit? {
        val term = query.trim().lowercase()
        if (term.isBlank()) return null
        val matches = candidates.filter { it.name.lowercase().contains(term) }
        if (matches.isEmpty() || matches.all { it.subscriberCount < 0 }) return null
        return matches.minWith(
            compareBy<ChannelHit> { if (it.name.lowercase() == term) 0 else 1 }
                .thenByDescending { it.subscriberCount.coerceAtLeast(0L) }
                .thenBy { if (it.verified) 0 else 1 },
        )
    }
}
