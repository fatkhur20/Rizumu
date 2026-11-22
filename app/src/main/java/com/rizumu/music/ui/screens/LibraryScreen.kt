package com.rizumu.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizumu.music.data.local.ListeningStatsLogic
import com.rizumu.music.data.local.UserLibrary
import com.rizumu.music.data.local.download.DownloadEntity
import com.rizumu.music.data.local.download.DownloadStatus
import com.rizumu.music.data.local.download.DownloadStore
import com.rizumu.music.data.local.download.toTrack
import com.rizumu.music.data.local.playlist.PlaylistCover
import com.rizumu.music.data.local.playlist.PlaylistStore
import com.rizumu.music.data.local.playlist.covers
import com.rizumu.music.data.yt.YoutubeTrack
import com.rizumu.music.playback.PlaybackManager
import com.rizumu.music.ui.components.BouncyEntrance
import com.rizumu.music.ui.components.ComingSoonBadge
import com.rizumu.music.ui.components.bouncyClick
import com.rizumu.music.ui.components.CreatePlaylistDialog
import com.rizumu.music.ui.components.EmptyState
import com.rizumu.music.ui.components.GlassSurface
import com.rizumu.music.ui.components.LocalBottomContentPadding
import com.rizumu.music.ui.components.PlaylistMosaic
import com.rizumu.music.ui.components.TrackArtwork
import com.rizumu.music.ui.components.TrackRow
import com.rizumu.music.ui.components.SongActionSheet
import com.rizumu.music.ui.formatListened
import com.rizumu.music.ui.theme.FtxAccent
import com.rizumu.music.ui.theme.FtxRadius
import com.rizumu.music.ui.theme.FtxText
import com.rizumu.music.ui.theme.FtxTextMuted
import java.util.Locale

private enum class LibrarySection { Favorites, History, Playlists, Downloads }

@Composable
fun LibraryScreen(
    playback: PlaybackManager,
    onOpenSearch: (String) -> Unit = {},
    onOpenPlaylist: (Long) -> Unit = {},
) {
    val library by UserLibrary.state.collectAsStateWithLifecycle()
    val playlists by PlaylistStore.playlists.collectAsStateWithLifecycle()
    val downloads by DownloadStore.downloads.collectAsStateWithLifecycle()
    val playerState by playback.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sectionName by rememberSaveable { mutableStateOf(LibrarySection.Favorites.name) }
    val section = remember(sectionName) { LibrarySection.valueOf(sectionName) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    var optionsTrack by remember { mutableStateOf<YoutubeTrack?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val bottomContentPadding = LocalBottomContentPadding.current
    val completedDownloads = downloads.filter { it.status == DownloadStatus.COMPLETE }
    val playableDownloads = completedDownloads.map { it.toTrack() }

    val tracks: List<YoutubeTrack> = when (section) {
        LibrarySection.Favorites -> if (sortAscending) {
            library.favorites.sortedBy { it.title.lowercase() }
        } else {
            library.favorites
        }
        LibrarySection.History -> library.recent
        LibrarySection.Playlists, LibrarySection.Downloads -> emptyList()
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Koleksi pribadi kamu.",
                style = MaterialTheme.typography.bodyMedium,
                color = FtxTextMuted,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BouncyEntrance(index = 0, modifier = Modifier.weight(1f)) {
                    CollectionTile(
                        icon = Icons.Rounded.Favorite,
                        title = "Disukai",
                        subtitle = "${library.favorites.size} lagu",
                        selected = section == LibrarySection.Favorites,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { sectionName = LibrarySection.Favorites.name },
                    )
                }
                BouncyEntrance(index = 1, modifier = Modifier.weight(1f)) {
                    CollectionTile(
                        icon = Icons.Rounded.History,
                        title = "Riwayat",
                        subtitle = "${library.recent.size} lagu",
                        selected = section == LibrarySection.History,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { sectionName = LibrarySection.History.name },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BouncyEntrance(index = 2, modifier = Modifier.weight(1f)) {
                    CollectionTile(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "Playlist",
                        subtitle = "${playlists.size} playlist",
                        selected = section == LibrarySection.Playlists,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { sectionName = LibrarySection.Playlists.name },
                    )
                }
                BouncyEntrance(index = 3, modifier = Modifier.weight(1f)) {
                    CollectionTile(
                        icon = Icons.Rounded.Download,
                        title = "Unduhan",
                        subtitle = if (completedDownloads.isEmpty()) {
                            "Belum ada"
                        } else {
                            "${completedDownloads.size} lagu · ${formatBytes(DownloadStore.totalBytes())}"
                        },
                        selected = section == LibrarySection.Downloads,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { sectionName = LibrarySection.Downloads.name },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val topArtist = ListeningStatsLogic.topArtists(library.stats, 1).firstOrNull()
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FtxRadius.lg),
                fillAlpha = 0.42f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Total dengar: ${formatListened(ListeningStatsLogic.totalMs(library.stats))} · ${library.stats.tracks.size} lagu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxText,
                )
                if (topArtist != null) {
                    Text(
                        text = "Artis teratas: ${topArtist.first} · ${formatListened(topArtist.second)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = FtxTextMuted,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (section) {
                        LibrarySection.Favorites -> "Lagu disukai"
                        LibrarySection.History -> "Baru diputar"
                        LibrarySection.Playlists -> "Playlist kamu"
                        LibrarySection.Downloads -> "Unduhan offline"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (section == LibrarySection.Playlists) {
                    TextButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = FtxAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Buat",
                            style = MaterialTheme.typography.labelLarge,
                            color = FtxAccent,
                        )
                    }
                }
                if (section == LibrarySection.Favorites && library.favorites.isNotEmpty()) {
                    TextButton(onClick = { sortAscending = !sortAscending }) {
                        Text(
                            text = if (sortAscending) "A–Z" else "Terbaru",
                            style = MaterialTheme.typography.labelLarge,
                            color = FtxAccent,
                        )
                    }
                }
                if (section == LibrarySection.History && library.recent.isNotEmpty()) {
                    TextButton(onClick = UserLibrary::clearHistory) {
                        Text(
                            text = "Bersihkan",
                            style = MaterialTheme.typography.labelLarge,
                            color = FtxTextMuted,
                        )
                    }
                }
            }
        }

        if (section == LibrarySection.Downloads) {
            if (downloads.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Download,
                    title = "Belum ada unduhan",
                    message = "Buka menu sebuah lagu lalu pilih Unduh offline.",
                    actionLabel = "Cari lagu",
                    onAction = { onOpenSearch("") },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        top = 4.dp,
                        end = 10.dp,
                        bottom = 4.dp + bottomContentPadding,
                    ),
                ) {
                    itemsIndexed(downloads, key = { _, download -> download.videoId }) { index, download ->
                        BouncyEntrance(index = index) {
                            DownloadRow(
                                download = download,
                                isCurrent = playerState.mediaId == download.videoId,
                                isPlaying = playerState.isPlaying,
                                isBuffering = playerState.isBuffering,
                                onClick = {
                                    val playIndex = playableDownloads.indexOfFirst {
                                        it.videoId == download.videoId
                                    }
                                    when {
                                        playIndex >= 0 -> playback.playTracks(
                                            playableDownloads,
                                            playIndex,
                                        )
                                        download.status == DownloadStatus.FAILED -> {
                                            DownloadStore.enqueue(context, download.toTrack())
                                        }
                                    }
                                },
                                onDelete = { DownloadStore.remove(context, download.videoId) },
                            )
                        }
                    }
                }
            }
        } else if (section == LibrarySection.Playlists) {
            if (playlists.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Belum ada playlist",
                    message = "Buat playlist lalu tambahkan lagu dari menu tekan-lama.",
                    actionLabel = "Buat playlist",
                    onAction = { showCreateDialog = true },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 4.dp,
                        end = 12.dp,
                        bottom = 4.dp + bottomContentPadding,
                    ),
                ) {
                    itemsIndexed(playlists, key = { _, summary -> summary.playlist.id }) { index, summary ->
                        BouncyEntrance(index = index) {
                            PlaylistRow(
                                name = summary.playlist.name,
                                trackCount = summary.trackCount,
                                covers = summary.covers,
                                onClick = { onOpenPlaylist(summary.playlist.id) },
                            )
                        }
                    }
                }
            }
        } else if (tracks.isEmpty()) {
            EmptyState(
                icon = if (section == LibrarySection.Favorites) Icons.Rounded.Favorite else Icons.Rounded.History,
                title = if (section == LibrarySection.Favorites) {
                    "Belum ada lagu disukai"
                } else {
                    "Belum ada riwayat"
                },
                message = if (section == LibrarySection.Favorites) {
                    "Tekan lama sebuah lagu lalu pilih Sukai, atau lewat Now Playing."
                } else {
                    "Lagu yang kamu putar akan muncul di sini."
                },
                actionLabel = "Cari lagu",
                onAction = { onOpenSearch("") },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = 4.dp,
                    end = 10.dp,
                    bottom = 4.dp + bottomContentPadding,
                ),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.videoId }) { index, track ->
                    BouncyEntrance(index = index) {
                        TrackRow(
                            track = track,
                            isCurrent = playerState.mediaId == track.videoId,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            showDuration = section == LibrarySection.History,
                            trailing = if (section == LibrarySection.History) {
                                {
                                    val count = library.stats.tracks[track.videoId]?.playCount ?: 0
                                    if (count > 0) {
                                        Text(
                                            text = "${count}×",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FtxTextMuted,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                            onClick = { playback.playTracks(tracks, index) },
                            onLongClick = { optionsTrack = track },
                            onMoreClick = { optionsTrack = track },
                        )
                    }
                }
            }
        }
    }

    optionsTrack?.let { track ->
        SongActionSheet(
            playback = playback,
            track = track,
            onDismiss = { optionsTrack = null },
            onOpenArtist = { artist -> onOpenSearch(artist) },
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> PlaylistStore.create(name) },
        )
    }
}

@Composable
private fun DownloadRow(
    download: DownloadEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FtxRadius.sm))
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            url = download.thumbnailUrl,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            iconSize = 18.dp,
            videoId = download.videoId,
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = download.title.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) FtxAccent else FtxText,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = when (download.status) {
                    DownloadStatus.COMPLETE -> "Offline · ${formatBytes(download.bytes)}"
                    DownloadStatus.RUNNING -> "Mengunduh ${download.progress}%"
                    DownloadStatus.QUEUED -> "Menunggu koneksi"
                    DownloadStatus.FAILED -> download.error ?: "Unduhan gagal"
                    else -> download.status
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (download.status == DownloadStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    FtxTextMuted
                },
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (download.status == DownloadStatus.RUNNING) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = download.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = FtxAccent,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
            }
        }
        if (download.status == DownloadStatus.COMPLETE) {
            if (isCurrent) {
                com.rizumu.music.ui.components.NowPlayingBars(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    size = 18.dp,
                )
            }
        } else if (download.status == DownloadStatus.FAILED) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Coba lagi",
                tint = FtxAccent,
                modifier = Modifier.padding(horizontal = 8.dp).size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Hapus unduhan",
                tint = FtxTextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1.0) {
        String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}

@Composable
private fun PlaylistRow(
    name: String,
    trackCount: Int,
    covers: List<PlaylistCover>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FtxRadius.sm))
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistMosaic(
            covers = covers,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(13.dp),
            iconSize = 20.dp,
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = "$trackCount lagu",
                style = MaterialTheme.typography.bodyMedium,
                color = FtxTextMuted,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = FtxTextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CollectionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    comingSoon: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val accent: Color = if (selected) FtxAccent else FtxText
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(FtxRadius.md),
        fillAlpha = if (selected) 0.62f else 0.36f,
        tintAlpha = if (selected) 0.075f else 0.040f,
        contentPadding = PaddingValues(12.dp),
        onClick = if (enabled) onClick else null,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(FtxAccent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) FtxAccent else FtxTextMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (comingSoon) {
                    ComingSoonBadge()
                } else {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = if (selected) FtxAccent else FtxTextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = FtxTextMuted,
            )
        }
    }
}
