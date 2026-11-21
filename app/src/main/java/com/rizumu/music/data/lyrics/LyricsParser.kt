package com.rizumu.music.data.lyrics

/**
 * Parses LRC files: `[mm:ss]`, `[mm:ss.xx]` and `[mm:ss.xxx]`, multiple
 * timestamps per line, and the optional `[offset:+/-ms]` tag.
 */
object LyricsParser {

    private val TIMESTAMP =
        Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val OFFSET = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)

    fun parseLrc(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val offset = OFFSET.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L

        val lines = mutableListOf<LyricLine>()
        raw.lineSequence().forEach lineLoop@{ line ->
            val matches = TIMESTAMP.findAll(line).toList()
            if (matches.isEmpty()) return@lineLoop
            val text = TIMESTAMP.replace(line, "").trim()
            if (text.isEmpty()) return@lineLoop
            matches.forEach matchLoop@{ match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@matchLoop
                val seconds = match.groupValues[2].toLongOrNull() ?: return@matchLoop
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> fraction.toLongOrNull()?.times(100) ?: 0L
                    2 -> fraction.toLongOrNull()?.times(10) ?: 0L
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val time = minutes * 60_000 + seconds * 1_000 + millis + offset
                if (time >= 0) lines += LyricLine(time, text)
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun plainLines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun activeIndex(
        lines: List<LyricLine>,
        positionMs: Long,
        offsetMs: Long = 0L,
    ): Int {
        if (lines.isEmpty()) return -1
        val adjusted = positionMs - offsetMs
        val index = lines.indexOfLast { it.timeMs <= adjusted }
        return index.coerceAtLeast(0)
    }
}
