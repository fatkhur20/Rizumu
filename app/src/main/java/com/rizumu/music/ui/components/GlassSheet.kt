package com.rizumu.music.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rizumu.music.ui.rememberReducedMotion
import com.rizumu.music.ui.theme.FtxRadius
import com.rizumu.music.ui.theme.FtxText
import com.rizumu.music.ui.theme.FtxTextMuted
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class SheetVisibilityController {
    private var depth = 0
    var open by mutableStateOf(false)
        private set

    internal fun acquire() {
        depth++
        open = true
    }

    internal fun release() {
        depth = (depth - 1).coerceAtLeast(0)
        open = depth > 0
    }
}

val LocalSheetVisibility = compositionLocalOf<SheetVisibilityController?> { null }
val LocalSheetDismiss = compositionLocalOf<(() -> Unit)?> { null }

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val SheetBorderBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f)),
)
private val SheetScrim = Color.Black.copy(alpha = 0.42f)
private val SheetHazeBlur = 40.dp

@Composable
fun GlassSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    maxHeightFraction: Float = 0.90f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibility = LocalSheetVisibility.current
    val reduceMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dismissThreshold = with(density) { 120.dp.toPx() }

    fun requestClose() {
        if (visible) visible = false
    }

    fun settleDrag() {
        scope.launch {
            animate(
                initialValue = dragOffset.floatValue,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            ) { value, _ ->
                dragOffset.floatValue = value
            }
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }
    LaunchedEffect(visible) {
        if (visible) {
            opened = true
        } else if (opened) {
            delay(if (reduceMotion) 0L else 240L)
            onDismiss()
        }
    }
    DisposableEffect(Unit) {
        visibility?.acquire()
        onDispose { visibility?.release() }
    }
    BackHandler { requestClose() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f),
    ) {
        val sheetMaxHeight = maxHeight * maxHeightFraction

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(if (reduceMotion) 0 else 120)),
            exit = fadeOut(tween(if (reduceMotion) 0 else 180)),
        ) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(SheetScrim)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { requestClose() },
                    ),
            )
        }

        CompositionLocalProvider(LocalSheetDismiss provides { requestClose() }) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = if (reduceMotion) {
                        tween(0)
                    } else {
                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                    },
                ) + fadeIn(tween(if (reduceMotion) 0 else 120)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(if (reduceMotion) 0 else 220),
                ) + fadeOut(tween(if (reduceMotion) 0 else 140)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                        .ftxSheetGlass(SheetShape)
                        .graphicsLayer { translationY = dragOffset.floatValue },
                ) {
                    SheetDragHandle(
                        onDrag = { amount ->
                            dragOffset.floatValue = (dragOffset.floatValue + amount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragOffset.floatValue > dismissThreshold) {
                                requestClose()
                            } else {
                                settleDrag()
                            }
                        },
                        onDragCancel = { settleDrag() },
                    )
                    Column(
                        modifier = Modifier
                            .padding(contentPadding)
                            .padding(bottom = navBottom + 14.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetDragHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {},
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f)),
        )
    }
}

@Composable
private fun Modifier.ftxSheetGlass(shape: Shape): Modifier {
    val hazeState = LocalHazeState.current
    val base = clip(shape)
    val blurred = if (hazeState != null) {
        base.hazeChild(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Color.Transparent,
                blurRadius = SheetHazeBlur,
                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.55f))),
                noiseFactor = 0f,
                fallbackTint = HazeTint(Color.Black.copy(alpha = 0.94f)),
            ),
        )
    } else {
        base.background(Color.Black.copy(alpha = 0.94f), shape)
    }
    return blurred
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.12f),
                ),
            ),
            shape,
        )
        .border(1.dp, SheetBorderBrush, shape)
        .drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(stroke, stroke / 2f),
                end = Offset(size.width - stroke, stroke / 2f),
                strokeWidth = stroke,
            )
        }
}

@Composable
fun GlassChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassSurface(
        modifier = modifier.bouncyClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(FtxRadius.pill),
        fillAlpha = if (selected) 0.80f else 0.48f,
        tintAlpha = if (selected) 0.18f else 0.08f,
        blur = false,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) FtxText else FtxTextMuted,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun GlassToolbarButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .ftxGlass(
                shape = CircleShape,
                fillAlpha = if (active) 0.68f else 0.52f,
                tintAlpha = if (active) 0.16f else 0.10f,
                shadowElevation = 0.dp,
            )
            .background(
                if (active) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.20f),
                CircleShape,
            )
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = FtxText,
            modifier = Modifier.size(18.dp),
        )
    }
}
