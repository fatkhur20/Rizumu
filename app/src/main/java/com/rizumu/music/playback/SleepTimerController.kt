package com.rizumu.music.playback

import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class SleepTimerState(
    val active: Boolean = false,
    val durationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val fading: Boolean = false,
)

object SleepTimerMath {
    const val FADE_STEPS = 24

    fun fadeVolume(startVolume: Float, step: Int, steps: Int = FADE_STEPS): Float {
        if (steps <= 0) return 0f
        val fraction = (1f - step.toFloat() / steps).coerceIn(0f, 1f)
        return (startVolume * fraction).coerceIn(0f, 1f)
    }

    fun remainingLabel(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        return String.format(
            Locale.US,
            "%d:%02d",
            totalSeconds / 60L,
            totalSeconds % 60L,
        )
    }
}

class SleepTimerController(
    private val player: Player,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var generation = 0

    fun start(minutes: Int) {
        if (minutes <= 0) return
        generation += 1
        val currentGeneration = generation
        job?.cancel()
        val totalMs = minutes * 60_000L
        job = scope.launch {
            val end = SystemClock.elapsedRealtime() + totalMs
            _state.value = SleepTimerState(
                active = true,
                durationMs = totalMs,
                remainingMs = totalMs,
            )
            try {
                while (true) {
                    val remaining = (end - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    if (remaining <= 0L) break
                    _state.value = SleepTimerState(
                        active = true,
                        durationMs = totalMs,
                        remainingMs = remaining,
                    )
                    delay(minOf(500L, remaining))
                }
                _state.value = SleepTimerState(
                    active = true,
                    durationMs = totalMs,
                    remainingMs = 0L,
                    fading = true,
                )
                fadeAndPause()
            } finally {
                if (currentGeneration == generation) {
                    _state.value = SleepTimerState()
                }
            }
        }
    }

    fun cancel() {
        generation += 1
        job?.cancel()
        job = null
        _state.value = SleepTimerState()
    }

    private suspend fun fadeAndPause() {
        val startVolume = player.volume
        try {
            for (step in 0..SleepTimerMath.FADE_STEPS) {
                player.volume = SleepTimerMath.fadeVolume(startVolume, step)
                delay(FADE_STEP_MS)
            }
            player.pause()
        } finally {
            player.volume = startVolume
        }
    }

    private companion object {
        const val FADE_STEP_MS = 125L
    }
}
