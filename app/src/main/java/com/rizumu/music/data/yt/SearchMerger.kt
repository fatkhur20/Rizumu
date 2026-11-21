package com.rizumu.music.data.yt

object SearchMerger {
    fun merge(primary: List<YoutubeTrack>, fallback: List<YoutubeTrack>): List<YoutubeTrack> {
        val seen = HashSet<String>()
        val result = mutableListOf<YoutubeTrack>()
        for (track in primary + fallback) {
            if (seen.add(track.videoId)) result.add(track)
        }
        return result
    }
}
