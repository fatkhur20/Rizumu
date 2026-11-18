package com.kyrix.ftxmusic.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.kyrix.ftxmusic.ui.rememberReducedMotion
import kotlinx.coroutines.delay

@Composable
fun rememberLightHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}

/**
 * Press feedback with a spring scale only (graphicsLayer, no relayout) plus a
 * light haptic. Supports long-press so list rows/cards keep their context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressHighlight: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = rememberReducedMotion()
    val haptic = rememberLightHaptic()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bouncyClickScale",
    )
    val highlight by animateFloatAsState(
        targetValue = if (pressed && enabled && pressHighlight) 0.06f else 0f,
        animationSpec = tween(90),
        label = "bouncyClickHighlight",
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (highlight > 0f) {
                drawRect(Color.White.copy(alpha = highlight))
            }
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = {
                haptic()
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    haptic()
                    it()
                }
            },
        )
}

/** Cards/rows appear one by one with a soft spring scale and a 200 ms fade. */
@Composable
fun BouncyEntrance(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = rememberReducedMotion()

    LaunchedEffect(Unit) {
        if (index > 0) delay(index.coerceAtMost(8) * 60L)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "bouncyEntranceScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0f,
        animationSpec = tween(200),
        label = "bouncyEntranceAlpha",
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        content = content,
    )
}

@Composable
fun BouncyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
