package com.kyrix.ftxmusic.data.local.playlist

object PlaylistNames {
    const val MAX_LENGTH = 60

    fun normalize(raw: String): String? {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return null
        return trimmed.take(MAX_LENGTH)
    }
}
