@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.kyrix.ftxmusic.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyrix.ftxmusic.playback.PlayerUiState
import com.kyrix.ftxmusic.ui.artwork.ArtworkProfile
import com.kyrix.ftxmusic.ui.rememberReducedMotion
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class DockPage { Nav, Player }

data class DockPagerFrame(
    val page: DockPage,
    val offsetFraction: Float,
    val navAlpha: Float,
    val playerAlpha: Float,
)

/** Pure transition rules so the dock can be tested without Compose. */
object DockPagerLogic {
    const val AUTO_RETURN_PLAYING_MS = 2_000L
    const val AUTO_RETURN_PAUSED_MS = 15_000L
    const val FORCE_PLAYER_MS = 600L

    fun initial(hasMedia: Boolean): DockPage = if (hasMedia) DockPage.Player else DockPage.Nav

    fun mediaChanged(hasMedia: Boolean): DockPage =
        if (hasMedia) DockPage.Player else DockPage.Nav

    fun autoReturnDelay(isPlaying: Boolean): Long =
        if (isPlaying) AUTO_RETURN_PLAYING_MS else AUTO_RETURN_PAUSED_MS

    fun targetOffsetFraction(page: DockPage): Float =
        if (page == DockPage.Player) -1f else 0f

    fun navAlpha(page: DockPage): Float = if (page == DockPage.Nav) 1f else 0f

    fun playerAlpha(page: DockPage): Float = if (page == DockPage.Player) 1f else 0f

    fun settled(hasMedia: Boolean): DockPagerFrame {
        val page = mediaChanged(hasMedia)
        return DockPagerFrame(
            page = page,
            offsetFraction = targetOffsetFraction(page),
            navAlpha = navAlpha(page),
            playerAlpha = playerAlpha(page),
        )
    }

    fun autoReturnAt(
        hasMedia: Boolean,
        autoReturnEnabled: Boolean,
        isPlaying: Boolean,
        now: Long,
    ): Long? =
        if (hasMedia && autoReturnEnabled) now + autoReturnDelay(isPlaying) else null

    fun shouldScheduleAutoReturn(
        target: DockPage,
        hasMedia: Boolean,
        autoReturnEnabled: Boolean,
    ): Boolean = target == DockPage.Nav && hasMedia && autoReturnEnabled

    fun shouldAutoReturn(page: DockPage, hasMedia: Boolean, dueAt: Long?, now: Long): Boolean =
        page == DockPage.Nav && hasMedia && dueAt != null && now >= dueAt
}

private const val TAG = "DockPager"

private data class DockNavItem(val route: String, val label: String, val icon: ImageVector)

private val DOCK_NAV = listOf(
    DockNavItem("home", "Home", Icons.Rounded.Home),
    DockNavItem("search", "Cari", Icons.Rounded.Search),
    DockNavItem("library", "Library", Icons.Rounded.LibraryMusic),
)

@Composable
fun DockPager(
    state: PlayerUiState,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    autoReturnToPlayer: Boolean = true,
    playerPageRequest: Int = 0,
    navPageRequest: Int = 0,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(DockPagerLogic.initial(state.hasMedia)) }
    var settleToken by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val heightPx = with(density) { ChromeSpec.Height.toPx() }
    val offset = remember { Animatable(if (page == DockPage.Player) -heightPx else 0f) }
    val scope = rememberCoroutineScope()
    var autoReturnJob by remember { mutableStateOf<Job?>(null) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReducedMotion()
    var peekPlayer by remember { mutableStateOf(false) }
    val pageSpec: AnimationSpec<Float> = if (reduceMotion) tween(0) else spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMedium,
    )
    val alphaSpec: AnimationSpec<Float> = if (reduceMotion) tween(0) else tween(140)
    val navAlpha by animateFloatAsState(
        targetValue = DockPagerLogic.navAlpha(page),
        animationSpec = alphaSpec,
        label = "dockNavAlpha",
    )
    val playerAlpha by animateFloatAsState(
        targetValue = DockPagerLogic.playerAlpha(page),
        animationSpec = alphaSpec,
        label = "dockPlayerAlpha",
    )

    fun cancelAutoReturn() {
        autoReturnJob?.cancel()
        autoReturnJob = null
    }

    fun scheduleAutoReturn() {
        cancelAutoReturn()
        if (!autoReturnToPlayer || !state.hasMedia) return
        val delayMs = DockPagerLogic.autoReturnDelay(state.isPlaying)
        Log.d(TAG, "schedule autoReturn in ${delayMs}ms playing=${state.isPlaying}")
        autoReturnJob = scope.launch {
            delay(delayMs)
            if (page == DockPage.Nav) {
                Log.d(TAG, "autoReturn fired")
                page = DockPage.Player
                settleToken++
            }
        }
    }

    fun changePage(target: DockPage) {
        if (!state.hasMedia && target == DockPage.Player) return
        if (target != page) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val shouldSchedule = DockPagerLogic.shouldScheduleAutoReturn(
            target = target,
            hasMedia = state.hasMedia,
            autoReturnEnabled = autoReturnToPlayer,
        )
        cancelAutoReturn()
        page = target
        settleToken++
        if (shouldSchedule) scheduleAutoReturn()
    }

    LaunchedEffect(state.hasMedia) {
        cancelAutoReturn()
        val target = DockPagerLogic.mediaChanged(state.hasMedia)
        if (state.hasMedia) peekPlayer = true
        page = target
        settleToken++
    }

    LaunchedEffect(playerPageRequest) {
        if (playerPageRequest > 0) {
            cancelAutoReturn()
            if (state.hasMedia && page != DockPage.Player) {
                page = DockPage.Player
                settleToken++
            }
        }
    }

    LaunchedEffect(navPageRequest) {
        if (navPageRequest > 0) {
            cancelAutoReturn()
            if (page != DockPage.Nav) {
                page = DockPage.Nav
                settleToken++
            }
        }
    }

    LaunchedEffect(page, heightPx, settleToken) {
        val target = DockPagerLogic.targetOffsetFraction(page) * heightPx
        if (peekPlayer && page == DockPage.Player) {
            peekPlayer = false
            offset.snapTo(target + with(density) { 8.dp.toPx() })
        }
        offset.animateTo(target, pageSpec)
        offset.snapTo(target)
        Log.d(
            TAG,
            "settled page=$page offset=${offset.value} " +
                "progress=${-offset.value / heightPx} " +
                "navAlpha=${DockPagerLogic.navAlpha(page)} " +
                "playerAlpha=${DockPagerLogic.playerAlpha(page)}",
        )
    }

    LaunchedEffect(state.hasMedia) {
        if (!state.hasMedia) return@LaunchedEffect
        delay(DockPagerLogic.FORCE_PLAYER_MS)
        val target = DockPagerLogic.targetOffsetFraction(DockPage.Player) * heightPx
        if (page != DockPage.Player) {
            page = DockPage.Player
            settleToken++
        }
        if (abs(offset.value - target) > 0.5f) {
            offset.snapTo(target)
            Log.d(TAG, "forced player page offset=${offset.value} progress=1.0")
        }
    }

    LaunchedEffect(page, state.hasMedia) {
        Log.d(
            TAG,
            "state page=$page hasMedia=${state.hasMedia} mediaId=${state.mediaId} " +
                "offset=${offset.value} progress=${-offset.value / heightPx} " +
                "navAlpha=${DockPagerLogic.navAlpha(page)} " +
                "playerAlpha=${DockPagerLogic.playerAlpha(page)}",
        )
    }

    val dragState = rememberDraggableState { delta ->
        val raw = offset.value + delta
        val min = -heightPx
        val resisted = when {
            raw > 0f -> raw * 0.28f
            raw < min -> min + (raw - min) * 0.28f
            else -> raw
        }
        scope.launch { offset.snapTo(resisted) }
    }

    val dragModifier = if (state.hasMedia) {
        Modifier.draggable(
            orientation = Orientation.Vertical,
            state = dragState,
            onDragStopped = { velocity ->
                val fraction = (-offset.value / heightPx).coerceIn(0f, 1f)
                val target = when {
                    velocity < -900f -> DockPage.Player
                    velocity > 900f -> DockPage.Nav
                    page == DockPage.Nav && fraction > 0.30f -> DockPage.Player
                    page == DockPage.Player && fraction < 0.70f -> DockPage.Nav
                    else -> page
                }
                changePage(target)
            },
        )
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .then(
                sharedBoundsModifier(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    key = "now-player-container",
                ),
            )
            .fillMaxWidth()
            .height(ChromeSpec.Height)
            .clip(ChromeSpec.Shape)
            .then(dragModifier)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Tampilkan navigasi") {
                        changePage(DockPage.Nav)
                        true
                    },
                    CustomAccessibilityAction("Tampilkan pemutar") {
                        if (state.hasMedia) changePage(DockPage.Player)
                        state.hasMedia
                    },
                )
            },
    ) {
        ChromeGlassSurface(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().clip(ChromeSpec.Shape)) {
                NavPage(
                    selectedRoute = selectedRoute,
                    onNavigate = { route ->
                        onNavigate(route)
                        changePage(DockPage.Nav)
                    },
                    enabled = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = offset.value
                            alpha = navAlpha
                        },
                )
                PlayerPage(
                    state = state,
                    onOpenPlayer = onOpenPlayer,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = offset.value + heightPx
                            alpha = playerAlpha
                        },
                )
            }
        }
    }
}

@Composable
private fun NavPage(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = DOCK_NAV.indexOfFirst { item ->
        when (item.route) {
            "search" -> selectedRoute?.startsWith("search") == true
            "library" -> selectedRoute == "library" || selectedRoute?.startsWith("playlist") == true
            else -> selectedRoute == item.route
        }
    }.coerceAtLeast(0)

    val scope = rememberCoroutineScope()
    val bubbleSquash = remember { Animatable(1f) }
    BoxWithConstraints(modifier = modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)) {
        val slot = maxWidth / DOCK_NAV.size.toFloat()
        val bubbleX by animateDpAsState(
            targetValue = slot * selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
            label = "dockBubbleX",
        )
        val bubbleWidth by animateDpAsState(
            targetValue = slot,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "dockBubbleWidth",
        )

        Box(
            modifier = Modifier
                .offset(x = bubbleX)
                .width(bubbleWidth)
                .fillMaxHeight()
                .graphicsLayer { scaleY = bubbleSquash.value }
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(22.dp)),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DOCK_NAV.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    targetValue = if (selected) FtxText else FtxTextMuted,
                    animationSpec = tween(180),
                    label = "dockTint",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "dockIconScale",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bouncyClick(
                            enabled = enabled,
                            onClick = {
                                scope.launch {
                                    bubbleSquash.snapTo(0.94f)
                                    bubbleSquash.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.65f,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                                onNavigate(item.route)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier
                                .size(21.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                        AnimatedVisibility(visible = selected) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerPage(
    state: PlayerUiState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val swipe = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        Log.d(TAG, "PlayerPage composed mediaId=${state.mediaId} hasMedia=${state.hasMedia}")
    }
    val sharedKey = state.mediaId ?: "none"
    val coverShared = sharedElementModifier(
        sharedTransitionScope,
        animatedVisibilityScope,
        "now-cover-$sharedKey",
    )
    val titleShared = sharedElementModifier(
        sharedTransitionScope,
        animatedVisibilityScope,
        "now-title-$sharedKey",
    )
    val artistShared = sharedElementModifier(
        sharedTransitionScope,
        animatedVisibilityScope,
        "now-artist-$sharedKey",
    )
    val playShared = sharedElementModifier(
        sharedTransitionScope,
        animatedVisibilityScope,
        "now-play-$sharedKey",
    )
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }

    Row(
        modifier = modifier
            .graphicsLayer { translationX = swipe.value }
            .pointerInput(state.mediaId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            swipe.value < -threshold -> {
                                onNext()
                                scope.launch { swipe.animateTo(0f, spring()) }
                            }
                            swipe.value > threshold -> {
                                onPrevious()
                                scope.launch { swipe.animateTo(0f, spring()) }
                            }
                            else -> scope.launch { swipe.animateTo(0f, spring()) }
                        }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(0f, spring()) } },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch {
                            swipe.snapTo(
                                (swipe.value + amount * 0.55f)
                                    .coerceIn(-threshold * 1.6f, threshold * 1.6f),
                            )
                        }
                    },
                )
            }
            .padding(start = 7.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .bouncyClick(onClick = onOpenPlayer),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(
                url = state.artworkUrl,
                modifier = Modifier.then(coverShared).size(44.dp),
                shape = RoundedCornerShape(12.dp),
                iconSize = 18.dp,
                videoId = state.mediaId,
                profile = ArtworkProfile.MINI,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "Rizumu" },
                    style = MaterialTheme.typography.titleSmall,
                    color = FtxText,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .then(titleShared)
                        .basicMarquee(
                            iterations = 3,
                            initialDelayMillis = 1_200,
                            repeatDelayMillis = 1_200,
                        ),
                )
                Text(
                    text = state.artist.ifBlank { "Siap memutar" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = artistShared,
                )
            }
        }

        PlayPauseRing(
            isPlaying = state.isPlaying,
            progress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            },
            onClick = onTogglePlayPause,
            sharedModifier = playShared,
        )

        Box(
            modifier = Modifier
                .size(38.dp)
                .bouncyClick(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Berikutnya",
                tint = FtxText,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PlayPauseRing(
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    sharedModifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .then(sharedModifier)
            .size(42.dp)
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(stroke),
            )
            if (progress > 0f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.86f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width - stroke,
                        height = size.height - stroke,
                    ),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = FtxText,
            modifier = Modifier.size(22.dp),
        )
    }
}

