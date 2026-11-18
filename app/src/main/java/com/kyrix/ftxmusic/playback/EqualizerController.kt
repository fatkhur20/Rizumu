package com.kyrix.ftxmusic.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EqualizerBand(val frequencyHz: Int, val label: String) {
    HZ_60(60, "60Hz"),
    HZ_230(230, "230Hz"),
    HZ_910(910, "910Hz"),
    HZ_3600(3600, "3.6kHz"),
    HZ_14000(14000, "14kHz"),
}

enum class EqualizerPreset(val label: String) {
    FLAT("Flat"),
    POP("Pop"),
    ROCK("Rock"),
    BASS("Bass"),
    VOCAL("Vokal"),
}

data class EqualizerUiState(
    val enabled: Boolean = false,
    val selectedPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val gains: Map<EqualizerBand, Float> = EqualizerBand.entries.associateWith { 0f },
)

interface EqualizerController {
    val state: StateFlow<EqualizerUiState>
    val bands: List<EqualizerBand>
        get() = EqualizerBand.entries
    val presets: List<EqualizerPreset>
        get() = EqualizerPreset.entries

    fun setEnabled(enabled: Boolean)
    fun setBandGain(band: EqualizerBand, gainDb: Float)
    fun applyPreset(preset: EqualizerPreset)
}

/** Placeholder used until the new audio engine exposes a real equalizer. */
class NoOpEqualizerController : EqualizerController {
    private val _state = MutableStateFlow(EqualizerUiState())
    override val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    override fun setEnabled(enabled: Boolean) = Unit

    override fun setBandGain(band: EqualizerBand, gainDb: Float) = Unit

    override fun applyPreset(preset: EqualizerPreset) = Unit
}
