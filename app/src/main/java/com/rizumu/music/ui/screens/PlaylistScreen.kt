package com.rizumu.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizumu.music.data.local.playlist.PlaylistCover
import com.rizumu.music.data.local.playlist.PlaylistStore
import com.rizumu.music.data.yt.YoutubeTrack
import com.rizumu.music.playback.PlaybackManager
import com.rizumu.music.ui.components.CreatePlaylistDialog
import com.rizumu.music.ui.components.EmptyState
import com.rizumu.music.ui.components.LocalBottomContentPadding
import com.rizumu.music.ui.components.PlaylistMosaic
import com.rizumu.music.ui.components.SongActionSheet
import com.rizumu.music.ui.components.TrackRow
import com.rizumu.music.ui.components.rememberLightHaptic
import com.rizumu.music.ui.theme.FtxAccent
import com.rizumu.music.ui.theme.FtxBlack
import com.rizumu.music.ui.theme.FtxRadius
import com.rizumu.music.ui.theme.FtxSurface
import com.rizumu.music.ui.theme.FtxTextMuted
import kotlin.math.roundToInt

@Composable
fun PlaylistScreen(
    playlistId: Long,
    playback: PlaybackManager,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit = {},
) {
    val playlistFlow = remember(playlistId) { PlaylistStore.observePlaylist(playlistId) }
    val tracksFlow = remember(playlistId) { PlaylistStore.observeTracks(playlistId) }
    val playlist by playlistFlow.collectAsStateWithLifecycle(initialValue = null)
    val tracks by tracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val playerState by playback.state.collectAsStateWithLifecycle()
    var optionsTrack by remember { mutableStateOf<YoutubeTrack?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(64f) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = rememberLightHaptic()
    val bottomContentPadding = LocalBottomContentPadding.current
    val covers = remember(tracks) {
        tracks.map { PlaylistCover(it.thumbnailUrl, it.videoId) }.take(4)
    }

    val startDrag: (Int) -> Unit = { index ->
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        rowHeightPx = visible?.size?.toFloat() ?: with(density) { 64.dp.toPx() }
        draggingIndex = index
        dragOffset = 0f
        haptic()
    }
    val finishDrag: () -> Unit = {
        val from = draggingIndex
        if (from != null && tracks.isNotEmpty()) {
            val shift = (dragOffset / rowHeightPx).roundToInt()
            val target = (from + shift).coerceIn(0, tracks.lastIndex)
            if (target != from) {
                PlaylistStore.reorder(playlistId, from, target)
            }
        }
        draggingIndex = null
        dragOffset = 0f
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Kembali",
                )
            }
            PlaylistMosaic(
                covers = covers,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(16.dp),
                iconSize = 24.dp,
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${tracks.size} lagu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                )
            }
            IconButton(onClick = { showRename = true }, enabled = playlist != null) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Ganti nama playlist",
                    tint = FtxTextMuted,
                )
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Hapus playlist",
                    tint = FtxTextMuted,
                )
            }
        }

        if (tracks.isNotEmpty()) {
            Button(
                onClick = { playback.playTracks(tracks, 0) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FtxAccent,
                    contentColor = FtxBlack,
                ),
                shape = RoundedCornerShape(FtxRadius.sm),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Putar semua")
            }
            if (tracks.size > 1) {
                Text(
                    text = "Tahan ikon ≡ lalu geser untuk mengubah urutan.",
                    style = MaterialTheme.typography.labelMedium,
                    color = FtxTextMuted,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
                )
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = 2.dp,
                    end = 10.dp,
                    bottom = 4.dp + bottomContentPadding,
                ),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.videoId }) { index, track ->
                    val isDragging = draggingIndex == index
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffset else 0f
                                val scale = if (isDragging) 1.015f else 1f
                                scaleX = scale
                                scaleY = scale
                                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                                shape = RoundedCornerShape(FtxRadius.sm)
                                clip = false
                            },
                    ) {
                        TrackRow(
                            track = track,
                            isCurrent = playerState.mediaId == track.videoId,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            onClick = { playback.playTracks(tracks, index) },
                            onLongClick = { optionsTrack = track },
                            onMoreClick = { optionsTrack = track },
                            trailing = {
                                val handleInteraction = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = handleInteraction,
                                            indication = null,
                                            onClick = {},
                                        )
                                        .pointerInput(index, tracks) {
                                            detectDragGestures(
                                                onDragStart = { startDrag(index) },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                },
                                                onDragEnd = finishDrag,
                                                onDragCancel = finishDrag,
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
                            },
                        )
                    }
                }
            }
        } else {
            EmptyState(
                icon = Icons.Rounded.PlaylistPlay,
                title = "Playlist masih kosong",
                message = "Tekan lama lagu mana pun lalu pilih Tambahkan ke Playlist.",
            )
        }
    }

    optionsTrack?.let { track ->
        SongActionSheet(
            playback = playback,
            track = track,
            onDismiss = { optionsTrack = null },
            onOpenArtist = onOpenArtist,
            playlistId = playlistId,
        )
    }

    if (showRename) {
        CreatePlaylistDialog(
            title = "Ganti nama playlist",
            confirmLabel = "Simpan",
            initialName = playlist?.name.orEmpty(),
            onDismiss = { showRename = false },
            onCreate = { name -> PlaylistStore.rename(playlistId, name) },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus playlist?") },
            text = {
                Text("\"${playlist?.name.orEmpty()}\" akan dihapus. Lagu aslinya tidak terpengaruh.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        PlaylistStore.delete(playlistId)
                        onBack()
                    },
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Batal", color = FtxTextMuted)
                }
            },
            containerColor = FtxSurface,
        )
    }
}
