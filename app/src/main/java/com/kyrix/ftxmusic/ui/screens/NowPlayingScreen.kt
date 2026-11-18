@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.kyrix.ftxmusic.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.kyrix.ftxmusic.data.local.UserLibrary
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.playback.EqualizerController
import com.kyrix.ftxmusic.playback.PlaybackManager
import com.kyrix.ftxmusic.playback.PlaybackSpeed
import com.kyrix.ftxmusic.playback.PlayerUiState
import com.kyrix.ftxmusic.ui.artwork.ArtworkProfile
import com.kyrix.ftxmusic.ui.components.EqualizerSheet
import com.kyrix.ftxmusic.ui.components.GlassIconButton
import com.kyrix.ftxmusic.ui.components.GlassSheet
import com.kyrix.ftxmusic.ui.components.GlassToolbarButton
import com.kyrix.ftxmusic.ui.components.GlassSurface
import com.kyrix.ftxmusic.ui.components.NowPlayingBars
import com.kyrix.ftxmusic.ui.components.rememberLightHaptic
import com.kyrix.ftxmusic.ui.components.LyricsPanel
import com.kyrix.ftxmusic.ui.components.SkeletonTrackRow
import com.kyrix.ftxmusic.ui.components.PlaybackSpeedSheet
import com.kyrix.ftxmusic.ui.components.SleepTimerSheet
import com.kyrix.ftxmusic.ui.components.SongActionSheet
import com.kyrix.ftxmusic.ui.components.bouncyClick
import com.kyrix.ftxmusic.ui.components.sharedBoundsModifier
import com.kyrix.ftxmusic.ui.components.sharedElementModifier
import com.kyrix.ftxmusic.ui.components.TrackArtwork
import com.kyrix.ftxmusic.ui.formatDurationMs
import com.kyrix.ftxmusic.ui.rememberReducedMotion
import com.kyrix.ftxmusic.ui.theme.FtxBlack
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxAccentDim
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    playback: PlaybackManager,
    equalizer: EqualizerController,
    onClose: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayAt: (Int) -> Unit,
    backProgress: () -> Float = { 0f },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var optionsTrack by remember { mutableStateOf<YoutubeTrack?>(null) }
    var queueOptionsIndex by remember { mutableStateOf<Int?>(null) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var draggingQueueIndex by remember { mutableIntStateOf(-1) }
    var queueDragOffset by remember { mutableFloatStateOf(0f) }
    val library by UserLibrary.state.collectAsStateWithLifecycle()
    val sleepTimer by playback.sleepTimer.state.collectAsStateWithLifecycle()
    val haptic = rememberLightHaptic()
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val closeThreshold = with(density) {
        (LocalConfiguration.current.screenHeightDp * 0.25f).dp.toPx()
    }
    val track = remember(state.mediaId, state.title, state.artist, state.artworkUrl, state.durationMs) {
        state.mediaId?.let { id ->
            YoutubeTrack(
                videoId = id,
                title = state.title,
                artist = state.artist,
                durationSec = state.durationMs / 1000L,
                thumbnailUrl = state.artworkUrl,
            )
        }
    }
    val isFavorite = state.mediaId != null && library.favorites.any { it.videoId == state.mediaId }
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
    val coverScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "coverPlayPauseScale",
    )

    val fraction = when {
        dragFraction != null -> dragFraction!!
        state.durationMs > 0L -> (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        else -> 0f
    }

    Box(
        modifier = modifier
            .then(
                sharedBoundsModifier(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    key = "now-player-container",
                ),
            )
            .fillMaxSize()
            .background(FtxBlack)
            .graphicsLayer {
                val back = backProgress()
                translationY = dragOffset + back * 120.dp.toPx()
                val collapse = ((dragOffset + back * 260.dp.toPx()) / 1200f).coerceIn(0f, 1f)
                alpha = (1f - collapse * 0.45f).coerceIn(0.55f, 1f)
                val scale = 1f - collapse * 0.10f
                scaleX = scale
                scaleY = scale
                shape = RoundedCornerShape((24f * collapse).dp)
                clip = collapse > 0f
            }
            .pointerInput(closeThreshold) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > closeThreshold) {
                            onClose()
                            dragOffset = 0f
                        } else {
                            scope.launch {
                                animate(
                                    dragOffset,
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) { value, _ ->
                                    dragOffset = value
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animate(dragOffset, 0f, animationSpec = tween(180)) { value, _ ->
                                dragOffset = value
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                )
            },
    ) {
        Crossfade(
            targetState = state.artworkUrl,
            animationSpec = tween(450),
            label = "nowPlayingBackdrop",
        ) { artworkUrl ->
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.15f
                            scaleY = 1.15f
                        }
                        .blur(64.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            FtxBlack.copy(alpha = 0.45f),
                            FtxBlack.copy(alpha = 0.86f),
                            FtxBlack,
                        ),
                    ),
                ),
        )

        CompositionLocalProvider(LocalContentColor provides FtxText) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Tutup",
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Text(
                        text = "DIPUTAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = FtxTextMuted,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    GlassToolbarButton(
                        icon = Icons.Rounded.Lyrics,
                        contentDescription = if (showLyrics) "Sembunyikan lirik" else "Lirik",
                        onClick = { showLyrics = !showLyrics },
                        active = showLyrics,
                    )
                    Spacer(Modifier.size(8.dp))
                    GlassToolbarButton(
                        icon = Icons.Rounded.Tune,
                        contentDescription = "Equalizer",
                        onClick = { showEqualizer = true },
                    )
                    Spacer(Modifier.size(8.dp))
                    GlassToolbarButton(
                        icon = Icons.Rounded.Bedtime,
                        contentDescription = if (sleepTimer.active) "Timer tidur aktif" else "Timer tidur",
                        onClick = { showSleepTimer = true },
                        active = sleepTimer.active,
                    )
                    Spacer(Modifier.size(8.dp))
                    GlassToolbarButton(
                        icon = Icons.Rounded.QueueMusic,
                        contentDescription = "Antrian",
                        onClick = { showQueue = true },
                    )
                }

                Spacer(Modifier.weight(0.26f))

                if (showLyrics) {
                    LyricsPanel(
                        track = track,
                        positionMs = state.positionMs,
                        onSeek = onSeek,
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .aspectRatio(1f),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .aspectRatio(1f)
                            .then(coverShared)
                            .graphicsLayer {
                                scaleX = coverScale
                                scaleY = coverScale
                            }
                            .shadow(
                                elevation = 28.dp,
                                shape = RoundedCornerShape(30.dp),
                                clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.5f),
                                spotColor = FtxAccent.copy(alpha = 0.12f),
                            ),
                    ) {
                        TrackArtwork(
                            url = state.artworkUrl,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(30.dp),
                            iconSize = 64.dp,
                            videoId = state.mediaId,
                            profile = ArtworkProfile.NOW_PLAYING,
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.title.ifBlank { "Belum ada lagu" },
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = titleShared,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = state.artist.ifBlank { "Pilih lagu dari pencarian" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = FtxTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = artistShared,
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    GlassIconButton(
                        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus dari Disukai" else "Sukai",
                        tint = if (isFavorite) FtxAccent else FtxText,
                        onClick = {
                            track?.let {
                                UserLibrary.toggleFavorite(it)
                                haptic()
                            }
                        },
                        glow = isFavorite,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Slider(
                    value = fraction,
                    onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        val target = dragFraction
                        if (target != null && state.durationMs > 0L) {
                            onSeek((target * state.durationMs).toLong())
                        }
                        dragFraction = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = FtxAccent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    track = { slider ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(slider.value.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .background(FtxAccent),
                            )
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDurationMs(state.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = FtxTextMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    GlassSurface(
                        shape = RoundedCornerShape(FtxRadius.pill),
                        fillAlpha = 0.52f,
                        tintAlpha = 0.05f,
                        blur = false,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        modifier = Modifier.bouncyClick(onClick = { showSpeed = true }),
                    ) {
                        Text(
                            text = PlaybackSpeed.label(state.playbackSpeed),
                            style = MaterialTheme.typography.labelMedium,
                            color = FtxAccent,
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = formatDurationMs(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = FtxTextMuted,
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (state.error != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Acak",
                            tint = if (state.shuffle) FtxAccent else FtxTextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Sebelumnya",
                            tint = FtxText,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    PlayPauseButton(
                        isPlaying = state.isPlaying,
                        onClick = onTogglePlayPause,
                        sharedModifier = playShared,
                    )
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Berikutnya",
                            tint = FtxText,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    IconButton(onClick = onCycleRepeat) {
                        Icon(
                            imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Ulangi",
                            tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) FtxAccent else FtxTextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(Modifier.weight(0.34f))
            }
        }
    }

    if (showQueue) {
        GlassSheet(
            onDismiss = { showQueue = false },
            maxHeightFraction = 0.90f,
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Antrian",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "${state.queue.size} lagu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FtxTextMuted,
                    )
                }
                TextButton(
                    onClick = { playback.clearQueue() },
                    enabled = state.queue.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Bersihkan antrian")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
            ) {
                itemsIndexed(
                    items = state.queue,
                    key = { index, item -> "${index}-${item.mediaId}" },
                ) { index, item ->
                    val itemTrack = YoutubeTrack(
                        videoId = item.mediaId,
                        title = item.title,
                        artist = item.artist,
                        durationSec = 0L,
                        thumbnailUrl = item.artworkUrl,
                    )
                    val active = index == state.currentIndex
                    var rowHeightPx by remember { mutableIntStateOf(0) }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                playback.removeAt(index)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Hapus dari antrian",
                                    tint = Color.White,
                                )
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (draggingQueueIndex == index) 2f else 0f)
                                .graphicsLayer {
                                    translationY = if (draggingQueueIndex == index) {
                                        queueDragOffset
                                    } else {
                                        0f
                                    }
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Color.White.copy(alpha = if (active) 0.14f else 0.06f),
                                )
                                .bouncyClick(
                                    onClick = {
                                        onPlayAt(index)
                                        showQueue = false
                                    },
                                )
                                .onSizeChanged { rowHeightPx = it.height }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) FtxText else FtxTextMuted,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(28.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            TrackArtwork(
                                url = item.artworkUrl,
                                modifier = Modifier.size(42.dp),
                                iconSize = 16.dp,
                                videoId = item.mediaId,
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FtxText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FtxTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (active) {
                                NowPlayingBars(
                                    isPlaying = state.isPlaying,
                                    isBuffering = state.isBuffering,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    size = 18.dp,
                                )
                            }
                            IconButton(
                                onClick = {
                                    optionsTrack = itemTrack
                                    queueOptionsIndex = index
                                    showQueue = false
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Opsi lagu",
                                    tint = FtxTextMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .pointerInput(index) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingQueueIndex = index
                                                queueDragOffset = 0f
                                                haptic()
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                queueDragOffset += dragAmount.y
                                            },
                                            onDragEnd = {
                                                val from = draggingQueueIndex
                                                if (from >= 0 && rowHeightPx > 0) {
                                                    val delta = (queueDragOffset / rowHeightPx).roundToInt()
                                                    val to = (from + delta).coerceIn(0, state.queue.lastIndex)
                                                    if (to != from) playback.moveQueueItem(from, to)
                                                }
                                                draggingQueueIndex = -1
                                                queueDragOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggingQueueIndex = -1
                                                queueDragOffset = 0f
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Ubah urutan",
                                    tint = FtxTextMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
                if (state.queue.size <= 1) {
                    item(key = "queue-empty") {
                        QueueEmptySuggestions(
                            playback = playback,
                            seed = listOf(state.artist, state.title)
                                .firstOrNull { it.isNotBlank() }
                                ?: "Musik populer Indonesia",
                            excludeId = state.mediaId,
                        )
                    }
                }
            }
        }
    }

    optionsTrack?.let { track ->
        val removeIndex = queueOptionsIndex
        SongActionSheet(
            playback = playback,
            track = track,
            onDismiss = {
                optionsTrack = null
                queueOptionsIndex = null
            },
            onOpenArtist = { artist ->
                onClose()
                onOpenArtist(artist)
            },
            onPlayNow = {
                val index = state.queue.indexOfFirst { it.mediaId == track.videoId }
                if (index >= 0) onPlayAt(index)
            },
            onRemoveFromQueue = removeIndex?.let { index ->
                { if (index in state.queue.indices) playback.removeAt(index) }
            },
        )
    }

    if (showEqualizer) {
        EqualizerSheet(
            controller = equalizer,
            onDismiss = { showEqualizer = false },
        )
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            controller = playback.sleepTimer,
            onDismiss = { showSleepTimer = false },
        )
    }

    if (showSpeed) {
        PlaybackSpeedSheet(
            current = state.playbackSpeed,
            onSelect = playback::setPlaybackSpeed,
            onDismiss = { showSpeed = false },
        )
    }
}

@Composable
private fun QueueEmptySuggestions(
    playback: PlaybackManager,
    seed: String,
    excludeId: String?,
) {
    var loading by remember(seed) { mutableStateOf(true) }
    var suggestions by remember(seed) { mutableStateOf<List<YoutubeTrack>>(emptyList()) }

    LaunchedEffect(seed) {
        loading = true
        suggestions = runCatching {
            playback.youtube.search("$seed mix").items
        }.getOrDefault(emptyList())
            .filter { it.videoId != excludeId }
            .distinctBy { it.videoId }
            .take(4)
        loading = false
    }

    Column(Modifier.padding(top = 14.dp, bottom = 24.dp)) {
        Text(
            text = "Belum ada lagu berikutnya",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Tambahkan saran lagu mirip ke antrian.",
            style = MaterialTheme.typography.bodyMedium,
            color = FtxTextMuted,
        )
        Spacer(Modifier.height(10.dp))
        when {
            loading -> repeat(2) { SkeletonTrackRow() }
            suggestions.isEmpty() -> Text(
                text = "Saran belum tersedia. Coba cari lagu lain.",
                style = MaterialTheme.typography.bodyMedium,
                color = FtxTextMuted,
            )
            else -> suggestions.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .bouncyClick(onClick = { playback.enqueue(track) })
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrackArtwork(
                        url = track.thumbnailUrl,
                        modifier = Modifier.size(40.dp),
                        iconSize = 16.dp,
                        videoId = track.videoId,
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FtxTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Tambah ke antrian",
                        tint = FtxAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    sharedModifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberLightHaptic()
    val reduceMotion = rememberReducedMotion()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "playPauseScale",
    )
    Box(
        modifier = Modifier
            .then(sharedModifier)
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.50f),
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(FtxAccent, FtxAccentDim)))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptic()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = FtxBlack,
            modifier = Modifier.size(38.dp),
        )
    }
}
