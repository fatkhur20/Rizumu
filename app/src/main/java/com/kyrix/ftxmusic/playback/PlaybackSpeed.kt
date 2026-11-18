package com.kyrix.ftxmusic.playback

import kotlin.math.roundToInt

object PlaybackSpeed {
    const val MIN = 0.75f
    const val MAX = 1.5f
    val presets = listOf(0.75f, 1.0f, 1.25f, 1.5f)

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)

    fun label(value: Float): String {
        val hundred = (clamp(value) * 100).roundToInt()
        val whole = hundred / 100
        val fraction = hundred % 100
        return when {
            fraction == 0 -> "$whole.0x"
            fraction % 10 == 0 -> "$whole.${fraction / 10}x"
            else -> "$whole.${fraction.toString().padStart(2, '0')}x"
        }
    }
}
