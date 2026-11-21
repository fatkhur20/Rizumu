package com.rizumu.music.ui.components

import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.rizumu.music.data.local.UserLibrary
import com.rizumu.music.ui.rememberReducedMotion
import com.rizumu.music.ui.theme.BackdropColor
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MeshBase = Color(0xFF0A0A0F)

private val FallbackMesh = listOf(
    Color(0xFF1C2A4A),
    Color(0xFF35204F),
    Color(0xFF0E3B3A),
    Color(0xFF0E1526),
)

private val PaletteCache = LruCache<String, List<Color>>(12)

private const val DriftPeriodNanos = 26_000_000_000L

@Composable
fun LiquidBackdrop(artworkUrl: String? = null) {
    val library by UserLibrary.state.collectAsStateWithLifecycle()
    val coverUrl = artworkUrl ?: library.recent.firstOrNull()?.thumbnailUrl
    val hazeState = LocalHazeState.current
    Box(
        Modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.haze(hazeState) else Modifier),
    ) {
        LiquidBackground(coverUrl)
    }
}

@Composable
private fun LiquidBackground(coverUrl: String?) {
    val context = LocalContext.current
    val reduceMotion = rememberReducedMotion()
    val colors by produceState(initialValue = FallbackMesh, key1 = coverUrl) {
        if (coverUrl.isNullOrBlank()) {
            value = FallbackMesh
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            PaletteCache.get(coverUrl)
                ?: runCatching { buildMesh(context, coverUrl) }.getOrNull()
                    ?.also { PaletteCache.put(coverUrl, it) }
                ?: FallbackMesh
        }
    }
    val drift = rememberDriftNanos(enabled = !reduceMotion)

    if (reduceMotion) {
        MeshCanvas(palette = colors, drift = drift)
    } else {
        Crossfade(
            targetState = colors,
            animationSpec = tween(600),
            label = "meshPalette",
        ) { palette ->
            MeshCanvas(palette = palette, drift = drift)
        }
    }
}

@Composable
private fun rememberDriftNanos(enabled: Boolean): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val drift = remember { mutableLongStateOf(0L) }

    LaunchedEffect(enabled, started) {
        if (!enabled || !started) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                drift.longValue += (now - last).coerceAtLeast(0L)
                last = now
            }
        }
    }
    return drift
}

@Composable
private fun MeshCanvas(palette: List<Color>, drift: State<Long>) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(MeshBase)
        val phase = (drift.value % DriftPeriodNanos).toFloat() / DriftPeriodNanos * (2f * PI.toFloat())
        drawBlob(palette[0], x = 0.18f, y = 0.22f, radius = 0.72f, phase = phase, speed = 0.8f, alpha = 0.46f)
        drawBlob(palette[1], x = 0.82f, y = 0.55f, radius = 0.68f, phase = phase, speed = 1.15f, alpha = 0.42f)
        drawBlob(palette[2], x = 0.38f, y = 0.88f, radius = 0.60f, phase = phase, speed = 0.65f, alpha = 0.40f)
        drawBlob(palette[3], x = 0.72f, y = 0.12f, radius = 0.50f, phase = phase, speed = 1.35f, alpha = 0.35f)
    }
}

private fun DrawScope.drawBlob(
    color: Color,
    x: Float,
    y: Float,
    radius: Float,
    phase: Float,
    speed: Float,
    alpha: Float,
) {
    val center = Offset(
        x = size.width * (x + 0.045f * sin(phase * speed + x * 10f)),
        y = size.height * (y + 0.035f * cos(phase * speed + y * 10f)),
    )
    val blobRadius = size.maxDimension * radius * (1f + 0.035f * sin(phase * speed + radius))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = blobRadius,
        ),
        radius = blobRadius,
        center = center,
    )
}

private suspend fun buildMesh(context: android.content.Context, coverUrl: String): List<Color> {
    val request = ImageRequest.Builder(context)
        .data(coverUrl)
        .size(128, 128)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    val bitmap = (result as? SuccessResult)?.drawable?.toBitmap() ?: return FallbackMesh
    val palette = Palette.from(bitmap).clearFilters().generate()
    val raw = listOf(
        palette.getDarkVibrantColor(0),
        palette.getVibrantColor(0),
        palette.getDarkMutedColor(0),
        palette.getMutedColor(0),
    ).filter { it != 0 && android.graphics.Color.alpha(it) != 0 }
        .distinct()
    if (raw.isEmpty()) return FallbackMesh
    return List(4) { index -> BackdropColor.blob(raw[index % raw.size]) }
}
