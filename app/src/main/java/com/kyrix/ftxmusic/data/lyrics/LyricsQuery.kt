package com.kyrix.ftxmusic.data.lyrics

object LyricsQuery {

    private val BRACKETS = Regex("""\s*[(\[][^)\]]*[)\]]""")
    private val NOISE = listOf(
        "official", "video", "music video", "lyric", "lyrics", "audio",
        "visualizer", "remaster", "remastered", "hd", "hq", "4k", "8d",
        "sped up", "speed up", "slowed", "reverb", "nightcore", "karaoke",
        "instrumental", "acoustic version", "live", "performance",
        "m/v", "mv", "from the", "from ", "original soundtrack", "ost",
    )

    fun cleanTitle(raw: String): String {
        var title = raw.trim()
        BRACKETS.findAll(title).forEach { match ->
            val content = match.value.lowercase()
            if (NOISE.any { content.contains(it) }) {
                title = title.replace(match.value, " ")
            }
        }
        title = title.replace(Regex("\\s+"), " ").trim()
        return title.ifBlank { raw.trim() }
    }

    fun cleanArtist(raw: String): String {
        var artist = raw.trim().removeSuffix(" - Topic")
        listOf(" feat.", " Feat.", " ft.", " Ft.").forEach { marker ->
            artist = artist.substringBefore(marker, artist)
        }
        return artist.trim().ifBlank { raw.trim() }
    }
}
