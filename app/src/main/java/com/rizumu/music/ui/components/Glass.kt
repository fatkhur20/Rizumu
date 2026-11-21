package com.rizumu.music.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rizumu.music.ui.theme.FtxGlassHighlight
import com.rizumu.music.ui.theme.FtxRadius
import com.rizumu.music.ui.theme.FtxText
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = compositionLocalOf<HazeState?> { null }

/** Separate state for the floating chrome, whose source includes scrolled content. */
val LocalChromeHazeState = compositionLocalOf<HazeState?> { null }

private val GlassBorderBrush = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f)),
)

private val GlassBlurRadius = 28.dp
private const val GlassFallbackTint = 0.32f

@Composable
fun Modifier.ftxGlass(
    shape: Shape = RoundedCornerShape(FtxRadius.lg),
    fillAlpha: Float = 0.82f,
    glow: Boolean = false,
    blur: Boolean = true,
    shadowElevation: Dp = if (glow) 18.dp else 8.dp,
    tintAlpha: Float? = null,
    transparent: Boolean = false,
): Modifier {
    val hazeState = LocalHazeState.current
    val effectiveTint = tintAlpha
        ?: (0.08f + (fillAlpha - 0.55f) * 0.10f).coerceIn(0.08f, 0.12f)
    val fallbackTintAlpha = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        GlassFallbackTint
    } else {
        effectiveTint
    }
    val shadowed = if (!transparent && shadowElevation > 0.dp) {
        this.shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.24f),
            spotColor = Color.Black.copy(alpha = 0.32f),
        )
    } else {
        this
    }
    val base = shadowed.clip(shape)
    val blurred = if (hazeState != null && blur) {
        base.hazeChild(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Color.Transparent,
                blurRadius = GlassBlurRadius,
                tints = listOf(HazeTint(Color.White.copy(alpha = effectiveTint))),
                noiseFactor = 0f,
                fallbackTint = HazeTint(Color.White.copy(alpha = fallbackTintAlpha)),
            ),
        )
    } else {
        base.background(Color.White.copy(alpha = fallbackTintAlpha), shape)
    }
    val filled = if (transparent) {
        blurred
    } else {
        blurred.background(
            Brush.verticalGradient(
                colors = listOf(
                    FtxGlassHighlight.copy(alpha = 0.04f),
                    Color.Transparent,
                ),
            ),
            shape,
        )
    }
    return filled
        .border(1.dp, GlassBorderBrush, shape)
        .drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.20f),
                start = Offset(stroke, stroke / 2f),
                end = Offset(size.width - stroke, stroke / 2f),
                strokeWidth = stroke,
            )
        }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(FtxRadius.lg),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fillAlpha: Float = 0.82f,
    glow: Boolean = false,
    blur: Boolean = true,
    shadowElevation: Dp = if (glow) 18.dp else 8.dp,
    tintAlpha: Float? = null,
    transparent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .ftxGlass(
                shape = shape,
                fillAlpha = fillAlpha,
                glow = glow,
                blur = blur,
                shadowElevation = shadowElevation,
                tintAlpha = tintAlpha,
                transparent = transparent,
            )
            .then(clickModifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FtxText,
    buttonSize: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    glow: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .ftxGlass(shape = CircleShape, fillAlpha = 0.72f, glow = glow)
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
