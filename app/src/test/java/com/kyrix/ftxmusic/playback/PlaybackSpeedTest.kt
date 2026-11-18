package com.kyrix.ftxmusic.playback

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSpeedTest {

    @Test
    fun clampsOutsideSupportedRange() {
        assertEquals(PlaybackSpeed.MIN, PlaybackSpeed.clamp(0.1f))
        assertEquals(PlaybackSpeed.MAX, PlaybackSpeed.clamp(4f))
    }

    @Test
    fun formatsPresetsWithoutNoisyDecimals() {
        assertEquals("0.75x", PlaybackSpeed.label(0.75f))
        assertEquals("1.0x", PlaybackSpeed.label(1f))
        assertEquals("1.25x", PlaybackSpeed.label(1.25f))
        assertEquals("1.5x", PlaybackSpeed.label(1.5f))
    }
}
