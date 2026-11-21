package com.rizumu.music.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NoOpEqualizerControllerTest {

    @Test
    fun returnsDefaultStateAndIgnoresMutations() {
        val controller = NoOpEqualizerController()
        val initial = controller.state.value

        assertFalse(initial.enabled)
        assertEquals(EqualizerPreset.FLAT, initial.selectedPreset)
        assertEquals(EqualizerBand.entries, initial.gains.keys.toList())
        assertEquals(List(EqualizerBand.entries.size) { 0f }, initial.gains.values.toList())

        controller.setEnabled(true)
        controller.setBandGain(EqualizerBand.HZ_60, 6f)
        controller.applyPreset(EqualizerPreset.ROCK)

        assertEquals(initial, controller.state.value)
        assertEquals(5, controller.bands.size)
        assertEquals(5, controller.presets.size)
    }
}
