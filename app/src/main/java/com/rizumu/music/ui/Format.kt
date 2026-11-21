package com.rizumu.music.ui

import java.util.Locale

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

fun formatDurationSec(seconds: Long): String {
    if (seconds <= 0L) return ""
    return formatDurationMs(seconds * 1000L)
}
