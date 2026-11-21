package com.rizumu.music.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rizumu.music.ui.rememberReducedMotion
import com.rizumu.music.ui.theme.FtxText

private const val BAR_COUNT = 4

private val BarDurations = listOf(450, 600, 520, 700)
private val BarDelays = listOf(0, 120, 60, 200)
private val StaticHeights = listOf(0.52f, 0.88f, 0.44f, 0.70f)

@Composable
fun NowPlayingBars(
    isPlaying: Boolean,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = FtxText,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState(initial = lifecycle.currentState)
    val reduceMotion = rememberReducedMotion()
    val active = isPlaying &&
        !isBuffering &&
        lifecycleState.isAtLeast(Lifecycle.State.STARTED) &&
        !reduceMotion

    if (active) {
        val transition = rememberInfiniteTransition(label = "nowPlayingBars")
        val bar0 = transition.bar(0)
        val bar1 = transition.bar(1)
        val bar2 = transition.bar(2)
        val bar3 = transition.bar(3)
        BarsCanvas(
            modifier = modifier,
            size = size,
            color = color,
            bar0 = bar0,
            bar1 = bar1,
            bar2 = bar2,
            bar3 = bar3,
        )
    } else {
        BarsCanvas(
            modifier = modifier,
            size = size,
            color = color,
            staticHeights = StaticHeights,
        )
    }
}

@Composable
private fun androidx.compose.animation.core.InfiniteTransition.bar(index: Int): State<Float> =
    animateFloat(
        initialValue = 0.26f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BarDurations[index],
                easing = EaseInOut,
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(BarDelays[index]),
        ),
        label = "nowPlayingBar$index",
    )

@Composable
private fun BarsCanvas(
    modifier: Modifier,
    size: Dp,
    color: Color,
    bar0: State<Float>? = null,
    bar1: State<Float>? = null,
    bar2: State<Float>? = null,
    bar3: State<Float>? = null,
    staticHeights: List<Float>? = null,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Sedang diputar" },
    ) {
        val gap = this.size.width * 0.16f
        val barWidth = (this.size.width - gap * (BAR_COUNT - 1)) / BAR_COUNT
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        val animated = bar0 != null
        repeat(BAR_COUNT) { index ->
            val fraction = when (index) {
                0 -> if (animated) bar0!!.value else staticHeights!![0]
                1 -> if (animated) bar1!!.value else staticHeights!![1]
                2 -> if (animated) bar2!!.value else staticHeights!![2]
                else -> if (animated) bar3!!.value else staticHeights!![3]
            }.coerceIn(0.18f, 1f)
            val barHeight = this.size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(
                    x = index * (barWidth + gap),
                    y = this.size.height - barHeight,
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = corner,
            )
        }
    }
}
