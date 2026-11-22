package com.rizumu.music.data.yt

object SearchMerger {
    fun merge(primary: List<YoutubeTrack>, fallback: List<YoutubeTrack>): List<YoutubeTrack> =
        dedupe(primary + fallback)

    fun dedupe(items: List<YoutubeTrack>): List<YoutubeTrack> {
        val seenId = HashSet<String>()
        val seenTitle = HashSet<String>()
        return items.filter { track ->
            val idKey = track.videoId.trim()
            val titleKey = "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"
            if (idKey.isBlank() || !seenId.add(idKey) || !seenTitle.add(titleKey)) {
                false
            } else {
                true
            }
        }
    }
}
