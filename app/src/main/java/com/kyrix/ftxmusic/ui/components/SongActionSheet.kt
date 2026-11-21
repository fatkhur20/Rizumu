package com.kyrix.ftxmusic.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrix.ftxmusic.data.local.UserLibrary
import com.kyrix.ftxmusic.data.local.download.DownloadStatus
import com.kyrix.ftxmusic.data.local.download.DownloadStore
import com.kyrix.ftxmusic.data.local.playlist.PlaylistStore
import com.kyrix.ftxmusic.data.local.playlist.PlaylistSummary
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.playback.PlaybackManager
import com.kyrix.ftxmusic.ui.artwork.ArtworkProfile
import com.kyrix.ftxmusic.ui.theme.FtxGlassBorder
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted

/**
 * Single reusable song action sheet. Every entry point (Home, trending, search,
 * recently played, library, queue, playlist detail) uses this composable so
 * actions never drift.
 *
 * "Buka Artis" opens the real search for the artist (no fabricated artist page).
 * When [playlistId] is set, a "Hapus dari playlist" action is offered.
 */
@Composable
fun SongActionSheet(
    playback: PlaybackManager,
    track: YoutubeTrack,
    onDismiss: () -> Unit,
    onOpenArtist: ((String) -> Unit)? = null,
    onPlayNow: (() -> Unit)? = null,
    playlistId: Long? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
) {
    val library by UserLibrary.state.collectAsStateWithLifecycle()
    val playlists by PlaylistStore.playlists.collectAsStateWithLifecycle()
    val downloads by DownloadStore.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var addingToPlaylist by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val known = library.favorites.firstOrNull { it.videoId == track.videoId }
        ?: library.recent.firstOrNull { it.videoId == track.videoId }
    val effective = if (known == null) {
        track
    } else {
        track.copy(
            durationSec = track.durationSec.takeIf { it > 0L } ?: known.durationSec,
            thumbnailUrl = track.thumbnailUrl ?: known.thumbnailUrl,
        )
    }
    val isFavorite = library.favorites.any { it.videoId == track.videoId }
    val download = downloads.firstOrNull { it.videoId == track.videoId }
    val downloadLabel = when (download?.status) {
        DownloadStatus.COMPLETE -> "Hapus unduhan"
        DownloadStatus.RUNNING, DownloadStatus.QUEUED -> "Mengunduh ${download?.progress ?: 0}%"
        DownloadStatus.FAILED -> "Coba unduh lagi"
        else -> "Unduh offline"
    }
    val downloadSubtitle = when (download?.status) {
        DownloadStatus.COMPLETE -> "Tersimpan di perangkat"
        DownloadStatus.RUNNING, DownloadStatus.QUEUED -> "Berjalan di latar belakang"
        DownloadStatus.FAILED -> download?.error
        else -> "Dengarkan tanpa koneksi"
    }
    val downloadIcon = when (download?.status) {
        DownloadStatus.COMPLETE -> Icons.Rounded.DownloadDone
        DownloadStatus.RUNNING, DownloadStatus.QUEUED -> Icons.Rounded.Download
        DownloadStatus.FAILED -> Icons.Rounded.Download
        else -> Icons.Rounded.Download
    }
    fun feedback() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    GlassSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.90f,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
        ) {
            if (addingToPlaylist) {
                PlaylistPickerContent(
                    playlists = playlists,
                    onBack = { addingToPlaylist = false },
                    onPick = { summary ->
                        feedback()
                        PlaylistStore.addTrack(summary.playlist.id, effective)
                        Toast.makeText(
                            context,
                            "Ditambahkan ke ${summary.playlist.name}",
                            Toast.LENGTH_SHORT,
                        ).show()
                        onDismiss()
                    },
                    onCreate = {
                        feedback()
                        showCreateDialog = true
                    },
                )
            } else {
                TrackHeader(track = effective)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = FtxGlassBorder, thickness = 1.dp)
                Spacer(Modifier.height(4.dp))

                SheetAction(
                    icon = Icons.Rounded.PlayArrow,
                    label = "Putar sekarang",
                    tint = FtxAccent,
                    onClick = {
                        feedback()
                        if (onPlayNow != null) onPlayNow() else playback.playTracks(listOf(effective), 0)
                        onDismiss()
                    },
                )
                SheetAction(
                    icon = Icons.Rounded.SkipNext,
                    label = "Putar berikutnya",
                    onClick = {
                        feedback()
                        playback.playNext(effective)
                        onDismiss()
                    },
                )
                SheetAction(
                    icon = Icons.Rounded.PlaylistAdd,
                    label = "Tambahkan ke antrean",
                    onClick = {
                        feedback()
                        playback.enqueue(effective)
                        onDismiss()
                    },
                )
                SheetAction(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (isFavorite) "Hapus dari Disukai" else "Tambahkan ke Disukai",
                    tint = if (isFavorite) FtxAccent else FtxText,
                    onClick = {
                        feedback()
                        UserLibrary.toggleFavorite(effective)
                        onDismiss()
                    },
                )
                SheetAction(
                    icon = downloadIcon,
                    label = downloadLabel,
                    subtitle = downloadSubtitle,
                    tint = if (download?.status == DownloadStatus.COMPLETE) FtxAccent else FtxText,
                    enabled = download?.status != DownloadStatus.RUNNING &&
                        download?.status != DownloadStatus.QUEUED,
                    onClick = {
                        feedback()
                        if (download?.status == DownloadStatus.COMPLETE) {
                            DownloadStore.remove(context, effective.videoId)
                            Toast.makeText(context, "Unduhan dihapus", Toast.LENGTH_SHORT).show()
                        } else {
                            DownloadStore.enqueue(context, effective)
                            Toast.makeText(
                                context,
                                "Unduhan dimulai di latar belakang",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        onDismiss()
                    },
                )
                SheetAction(
                    icon = Icons.Rounded.PlaylistPlay,
                    label = "Tambahkan ke Playlist",
                    subtitle = if (playlists.isEmpty()) "Belum ada playlist" else null,
                    onClick = {
                        feedback()
                        addingToPlaylist = true
                    },
                )
                if (playlistId != null) {
                    SheetAction(
                        icon = Icons.Rounded.ArrowUpward,
                        label = "Pindah ke atas",
                        onClick = {
                            feedback()
                            PlaylistStore.move(playlistId, effective.videoId, up = true)
                            onDismiss()
                        },
                    )
                    SheetAction(
                        icon = Icons.Rounded.ArrowDownward,
                        label = "Pindah ke bawah",
                        onClick = {
                            feedback()
                            PlaylistStore.move(playlistId, effective.videoId, up = false)
                            onDismiss()
                        },
                    )
                    SheetAction(
                        icon = Icons.Rounded.RemoveCircleOutline,
                        label = "Hapus dari playlist",
                        onClick = {
                            feedback()
                            PlaylistStore.removeTrack(playlistId, effective.videoId)
                            Toast.makeText(context, "Dihapus dari playlist", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                    )
                }
                if (onRemoveFromQueue != null) {
                    SheetAction(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Hapus dari antrian",
                        onClick = {
                            feedback()
                            onRemoveFromQueue()
                            onDismiss()
                        },
                    )
                }
                SheetAction(
                    icon = Icons.Rounded.Person,
                    label = "Buka Artis",
                    subtitle = if (effective.artist.isBlank() || onOpenArtist == null) {
                        "Tidak tersedia"
                    } else {
                        "Cari lagu artis ini"
                    },
                    enabled = effective.artist.isNotBlank() && onOpenArtist != null,
                    onClick = {
                        if (effective.artist.isNotBlank() && onOpenArtist != null) {
                            feedback()
                            onOpenArtist(effective.artist)
                            onDismiss()
                        }
                    },
                )
                SheetAction(
                    icon = Icons.Rounded.Share,
                    label = "Bagikan",
                    onClick = {
                        feedback()
                        shareTrack(context, effective)
                        onDismiss()
                    },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                PlaylistStore.createAndAdd(name, effective)
                Toast.makeText(context, "Ditambahkan ke $name", Toast.LENGTH_SHORT).show()
                onDismiss()
            },
        )
    }
}

@Composable
private fun PlaylistPickerContent(
    playlists: List<PlaylistSummary>,
    onBack: () -> Unit,
    onPick: (PlaylistSummary) -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Kembali",
                tint = FtxText,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = "Tambahkan ke Playlist",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (playlists.isEmpty()) {
                    "Buat playlist pertamamu"
                } else {
                    "${playlists.size} playlist"
                },
                style = MaterialTheme.typography.bodySmall,
                color = FtxTextMuted,
            )
        }
    }

    if (playlists.isNotEmpty()) {
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(playlists, key = { it.playlist.id }) { summary ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(summary) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistPlay,
                        contentDescription = null,
                        tint = FtxTextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = summary.playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${summary.trackCount} lagu",
                            style = MaterialTheme.typography.labelSmall,
                            color = FtxTextMuted,
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = "Lagu belum masuk playlist mana pun karena playlist masih kosong.",
            style = MaterialTheme.typography.bodyMedium,
            color = FtxTextMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }

    HorizontalDivider(color = FtxGlassBorder, thickness = 1.dp)
    Spacer(Modifier.height(4.dp))
    SheetAction(
        icon = Icons.Rounded.Add,
        label = "Playlist baru",
        tint = FtxAccent,
        onClick = onCreate,
    )
}

@Composable
private fun TrackHeader(track: YoutubeTrack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            url = track.thumbnailUrl,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(14.dp),
            iconSize = 20.dp,
            videoId = track.videoId,
            profile = ArtworkProfile.LIST,
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artist.ifBlank { "Artis tidak diketahui" },
                style = MaterialTheme.typography.bodyMedium,
                color = FtxTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun shareTrack(context: android.content.Context, track: YoutubeTrack) {
    val url = "https://youtu.be/${track.videoId}"
    val text = buildString {
        append(track.title)
        if (track.artist.isNotBlank()) append(" — ").append(track.artist)
        append('\n').append(url)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, "Bagikan lagu"))
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = FtxText,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else FtxTextMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) tint else FtxTextMuted.copy(alpha = 0.55f),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = FtxTextMuted.copy(alpha = if (enabled) 0.9f else 0.5f),
                )
            }
        }
    }
}
