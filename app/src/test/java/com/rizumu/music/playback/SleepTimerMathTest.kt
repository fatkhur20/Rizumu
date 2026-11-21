package com.rizumu.music.playback

import kotlin.test.Test
import kotlin.test.assertEquals

class SleepTimerMathTest {

    @Test
    fun fadeStartsAtCurrentVolume() {
        assertEquals(0.8f, SleepTimerMath.fadeVolume(0.8f, 0), 0.0001f)
    }

    @Test
    fun fadeReachesSilence() {
        assertEquals(0f, SleepTimerMath.fadeVolume(0.8f, SleepTimerMath.FADE_STEPS), 0.0001f)
    }

    @Test
    fun fadeClampsToValidRange() {
        assertEquals(1f, SleepTimerMath.fadeVolume(1f, -2), 0.0001f)
        assertEquals(0f, SleepTimerMath.fadeVolume(1f, 99), 0.0001f)
    }

    @Test
    fun remainingLabelRoundsUpPartialSeconds() {
        assertEquals("15:00", SleepTimerMath.remainingLabel(15 * 60_000L))
        assertEquals("0:01", SleepTimerMath.remainingLabel(1L))
        assertEquals("0:00", SleepTimerMath.remainingLabel(0L))
    }
}
