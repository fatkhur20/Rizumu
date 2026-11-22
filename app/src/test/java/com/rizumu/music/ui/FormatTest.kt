package com.rizumu.music.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun listenedTimeLabel() {
        assertEquals("0 m", formatListened(0L))
        assertEquals("1 m", formatListened(60_000L))
        assertEquals("1 jam 5 m", formatListened(65 * 60_000L))
    }
}
