package com.kyrix.ftxmusic.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kyrix.ftxmusic.BuildConfig
import com.kyrix.ftxmusic.data.charts.ChartRegion
import com.kyrix.ftxmusic.data.charts.ChartRepository
import com.kyrix.ftxmusic.data.local.UserLibrary
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.playback.EqualizerController
import com.kyrix.ftxmusic.playback.PlaybackManager
import com.kyrix.ftxmusic.ui.components.ArtworkCard
import com.kyrix.ftxmusic.ui.components.BouncyEntrance
import com.kyrix.ftxmusic.ui.components.bouncyClick
import com.kyrix.ftxmusic.ui.components.EmptyState
import com.kyrix.ftxmusic.ui.components.EqualizerSheet
import com.kyrix.ftxmusic.ui.components.ErrorState
import com.kyrix.ftxmusic.ui.components.GlassIconButton
import com.kyrix.ftxmusic.ui.components.GlassSheet
import com.kyrix.ftxmusic.ui.components.LocalSheetDismiss
import com.kyrix.ftxmusic.ui.components.GlassSurface
import com.kyrix.ftxmusic.ui.components.LocalBottomContentPadding
import com.kyrix.ftxmusic.ui.components.SectionHeader
import com.kyrix.ftxmusic.ui.components.SkeletonCardColumn
import com.kyrix.ftxmusic.ui.components.SkeletonTrackRow
import com.kyrix.ftxmusic.ui.components.TrackRow
import com.kyrix.ftxmusic.ui.components.SongActionSheet
import com.kyrix.ftxmusic.ui.home.HomeViewModel
import com.kyrix.ftxmusic.ui.artwork.ArtworkProfile
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted
import com.kyrix.ftxmusic.ui.theme.FtxWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class QuickAction(val label: String, val icon: ImageVector, val query: String)

private val QUICK_ACTIONS = listOf(
    QuickAction("Pop", Icons.Rounded.MusicNote, "musik pop"),
    QuickAction("Rilis Baru", Icons.Rounded.FiberNew, "rilis musik baru"),
    QuickAction("Akustik", Icons.Rounded.Piano, "musik akustik"),
    QuickAction("Fokus", Icons.Rounded.Headphones, "musik fokus"),
)

private val GENRES = listOf(
    "Lofi" to "lofi",
    "Pop Indonesia" to "pop indonesia",
    "Rock" to "rock",
    "Jazz" to "jazz",
    "Dangdut" to "dangdut",
    "Acoustic" to "acoustic",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playback: PlaybackManager,
    charts: ChartRepository,
    equalizer: EqualizerController,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenCategory: (label: String, query: String) -> Unit,
    onOpenPlayer: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(playback.youtube, charts) }
        },
    )
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.refreshOnOpen()
    }
    val playerState by playback.state.collectAsStateWithLifecycle()
    val library by UserLibrary.state.collectAsStateWithLifecycle()
    var showInfo by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var optionsTrack by remember { mutableStateOf<YoutubeTrack?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val bottomContentPadding = LocalBottomContentPadding.current
    val recentSeed = library.recent.firstOrNull()?.videoId

    LaunchedEffect(recentSeed) {
        viewModel.ensureRecommended()
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomContentPadding),
    ) {
        item {
            HomeHeader(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                onInfo = { showInfo = true },
            )
        }

        item {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                shape = RoundedCornerShape(FtxRadius.pill),
                fillAlpha = 0.8f,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                onClick = { onOpenSearch("") },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = FtxTextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "Cari lagu, artis, atau playlist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FtxTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(18.dp))
                QUICK_ACTIONS.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    if (rowIndex > 0) Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEachIndexed { columnIndex, action ->
                            BouncyEntrance(
                                index = rowIndex * 2 + columnIndex,
                                modifier = Modifier.weight(1f),
                            ) {
                                QuickActionTile(
                                    action = action,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenCategory(action.label, action.query) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (library.recent.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(
                    title = "Baru diputar",
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(library.recent.take(10), key = { _, track -> track.videoId }) { index, track ->
                        BouncyEntrance(index = index) {
                            ArtworkCard(
                                track = track,
                                cardSize = 140.dp,
                                profile = ArtworkProfile.RECENT,
                                onClick = { playback.playTracks(listOf(track), 0) },
                                onLongClick = { optionsTrack = track },
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                title = "Trending ${ui.region.label}",
                modifier = Modifier.padding(horizontal = 18.dp),
                actionIcon = Icons.Rounded.Refresh,
                actionDescription = "Perbarui tangga lagu",
                onAction = viewModel::refreshTrending,
            )
            Spacer(Modifier.height(4.dp))
            ChartMetaRow(
                modifier = Modifier.padding(horizontal = 18.dp),
                cached = ui.trending.cached,
                loading = ui.trending.loading,
                fetchedAt = ui.trending.fetchedAt,
                source = ui.trending.source,
                regionLabel = ui.region.label,
                onRegionClick = { showRegionPicker = true },
            )
            if (ui.trending.error != null && ui.trending.entries.isNotEmpty()) {
                InlineChartError(
                    message = ui.trending.error!!,
                    onRetry = viewModel::retryTrending,
                )
            }
            Spacer(Modifier.height(6.dp))
            when {
                ui.trending.loading && ui.trending.entries.isEmpty() -> {
                    Column(Modifier.padding(horizontal = 10.dp)) {
                        repeat(3) { SkeletonTrackRow() }
                    }
                }
                ui.trending.error != null && ui.trending.entries.isEmpty() -> {
                    ErrorState(
                        message = ui.trending.error!!,
                        onRetry = viewModel::retryTrending,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
                ui.trending.entries.isEmpty() -> {
                    EmptyState(
                        title = "Tangga lagu kosong",
                        message = "YouTube Music tidak menyediakan tangga lagu untuk wilayah ini.",
                        actionLabel = "Pilih wilayah",
                        onAction = { showRegionPicker = true },
                    )
                }
                else -> {
                    val tracks = ui.trending.entries.map { it.toTrack() }
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        shape = RoundedCornerShape(FtxRadius.lg),
                        fillAlpha = 0.30f,
                        tintAlpha = 0.040f,
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                    ) {
                        Column {
                            ui.trending.entries.take(6).forEachIndexed { index, entry ->
                                val track = entry.toTrack()
                                TrackRow(
                                    track = track,
                                    rank = entry.rank,
                                    isCurrent = playerState.mediaId == entry.videoId,
                                    isPlaying = playerState.isPlaying,
                                    isBuffering = playerState.isBuffering,
                                    onClick = { playback.playTracks(tracks, index) },
                                    onLongClick = { optionsTrack = track },
                                    onMoreClick = { optionsTrack = track },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                title = "Video Musik Global",
                modifier = Modifier.padding(horizontal = 18.dp),
                actionIcon = Icons.Rounded.Refresh,
                actionDescription = "Perbarui tangga lagu video",
                onAction = viewModel::refreshGlobal,
            )
            Spacer(Modifier.height(4.dp))
            ChartMetaRow(
                modifier = Modifier.padding(horizontal = 18.dp),
                cached = ui.global.cached,
                loading = ui.global.loading,
                fetchedAt = ui.global.fetchedAt,
                source = ui.global.source,
            )
            if (ui.global.error != null && ui.global.entries.isNotEmpty()) {
                InlineChartError(
                    message = ui.global.error!!,
                    onRetry = viewModel::retryGlobal,
                )
            }
            Spacer(Modifier.height(6.dp))
            when {
                ui.global.loading && ui.global.entries.isEmpty() -> {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(2) { SkeletonCardColumn() }
                    }
                }
                ui.global.error != null && ui.global.entries.isEmpty() -> {
                    ErrorState(
                        message = ui.global.error!!,
                        onRetry = viewModel::retryGlobal,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
                ui.global.entries.isEmpty() -> {
                    Text(
                        text = "Tangga lagu video global sedang kosong.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FtxTextMuted,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                }
                else -> {
                    val tracks = ui.global.entries.map { it.toTrack() }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(ui.global.entries, key = { _, entry -> entry.videoId }) { index, entry ->
                            BouncyEntrance(index = index) {
                                ArtworkCard(
                                    track = entry.toTrack(),
                                    cardSize = 148.dp,
                                    rank = entry.rank,
                                    onClick = { playback.playTracks(tracks, index) },
                                    onLongClick = { optionsTrack = entry.toTrack() },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                title = "Jelajahi Genre",
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(GENRES, key = { _, genre -> genre.first }) { index, (label, query) ->
                    BouncyEntrance(index = index) {
                        GlassSurface(
                            shape = RoundedCornerShape(FtxRadius.pill),
                            fillAlpha = 0.7f,
                            blur = false,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.bouncyClick(
                                pressHighlight = true,
                                onClick = { onOpenCategory(label, query) },
                            ),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        if (ui.recommended.loading || ui.recommended.items.isNotEmpty()) {
            item {
                Spacer(Modifier.height(22.dp))
                SectionHeader(
                    title = "Rekomendasi untukmu",
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Spacer(Modifier.height(6.dp))
                if (ui.recommended.loading && ui.recommended.items.isEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(2) { SkeletonCardColumn() }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(ui.recommended.items, key = { _, track -> track.videoId }) { index, track ->
                            BouncyEntrance(index = index) {
                                ArtworkCard(
                                    track = track,
                                    cardSize = 148.dp,
                                    onClick = {
                                        playback.playTracks(ui.recommended.items, index)
                                    },
                                    onLongClick = { optionsTrack = track },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            Text(
                text = "Musik dari YouTube untuk penggunaan pribadi.",
                style = MaterialTheme.typography.labelMedium,
                color = FtxTextMuted,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
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

    if (showRegionPicker) {
        GlassSheet(
            onDismiss = { showRegionPicker = false },
            maxHeightFraction = 0.78f,
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            Text(
                text = "Wilayah tangga lagu",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Sumber resmi: YouTube Music Charts",
                style = MaterialTheme.typography.bodyMedium,
                color = FtxTextMuted,
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(ChartRegion.SELECTABLE, key = { it.code }) { region ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.selectRegion(region)
                                showRegionPicker = false
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = region.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = region.code,
                            style = MaterialTheme.typography.labelMedium,
                            color = FtxTextMuted,
                        )
                        if (region.code == ui.region.code) {
                            Spacer(Modifier.size(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Dipilih",
                                tint = FtxAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        GlassSheet(
            onDismiss = { showInfo = false },
            maxHeightFraction = 0.92f,
            contentPadding = PaddingValues(horizontal = 22.dp),
        ) {
            val dismissSheet = LocalSheetDismiss.current ?: { showInfo = false }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Rizumu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Versi ${BuildConfig.VERSION_NAME} • Native Android",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Musik diputar dari YouTube. Gunakan untuk keperluan pribadi; " +
                        "streaming di luar aplikasi resmi dapat melanggar Ketentuan Layanan YouTube.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tangga lagu berasal dari YouTube Music Charts. " +
                        "Peringkat ditampilkan sesuai urutan sumber; data disimpan sementara " +
                        "dan dapat ditampilkan dari cache saat offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                )
                Spacer(Modifier.height(18.dp))
                InfoActionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Equalizer",
                    supporting = "Pratinjau kontrol audio",
                    onClick = {
                        showInfo = false
                        showEqualizer = true
                    },
                )
                Spacer(Modifier.height(8.dp))
                InfoActionRow(
                    icon = Icons.Rounded.Refresh,
                    label = "Cek pembaruan",
                    supporting = if (checkingUpdate) "Memeriksa GitHub Releases..." else "Versi ${BuildConfig.VERSION_NAME}",
                    loading = checkingUpdate,
                    onClick = {
                        showInfo = false
                        onCheckUpdate()
                    },
                )
                Spacer(Modifier.height(8.dp))
                InfoActionRow(
                    icon = Icons.Rounded.Upload,
                    label = "Ekspor backup",
                    supporting = "Simpan playlist, suka, dan riwayat sebagai JSON",
                    onClick = {
                        showInfo = false
                        onExportBackup()
                    },
                )
                Spacer(Modifier.height(8.dp))
                InfoActionRow(
                    icon = Icons.Rounded.Download,
                    label = "Impor backup",
                    supporting = "Gabungkan data dari file JSON",
                    onClick = {
                        showInfo = false
                        onImportBackup()
                    },
                )
                Spacer(Modifier.height(18.dp))
                InfoActionRow(
                    icon = Icons.Rounded.DeleteSweep,
                    label = "Bersihkan data lokal",
                    supporting = "Hapus suka dan riwayat mendengarkan",
                    tint = FtxWarning,
                    onClick = { showClearConfirm = true },
                )
                Spacer(Modifier.height(16.dp))
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClick(onClick = dismissSheet),
                    shape = RoundedCornerShape(FtxRadius.md),
                    fillAlpha = 0.55f,
                    blur = false,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(vertical = 13.dp),
                ) {
                    Text(
                        text = "Tutup",
                        style = MaterialTheme.typography.labelLarge,
                        color = FtxText,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Bersihkan data lokal?") },
            text = {
                Text(
                    "Favorit dan riwayat mendengarkan akan dihapus dari perangkat. " +
                        "Playlist, unduhan, dan file audio tidak terhapus.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        UserLibrary.clearAll()
                        showClearConfirm = false
                        showInfo = false
                    },
                ) {
                    Text("Bersihkan", color = FtxWarning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Batal", color = FtxTextMuted)
                }
            },
        )
    }

    if (showEqualizer) {
        EqualizerSheet(
            controller = equalizer,
            onDismiss = { showEqualizer = false },
        )
    }
    }
}

@Composable
private fun InfoActionRow(
    icon: ImageVector,
    label: String,
    supporting: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    tint: androidx.compose.ui.graphics.Color = FtxText,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(FtxRadius.md),
        fillAlpha = 0.80f,
        blur = false,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tint,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelMedium,
                    color = FtxTextMuted,
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.5.dp,
                    color = FtxTextMuted,
                )
            }
        }
    }
}

@Composable
private fun ChartMetaRow(
    cached: Boolean,
    loading: Boolean,
    fetchedAt: Long?,
    source: String?,
    modifier: Modifier = Modifier,
    regionLabel: String? = null,
    onRegionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (regionLabel != null && onRegionClick != null) {
            GlassSurface(
                shape = RoundedCornerShape(FtxRadius.pill),
                fillAlpha = 0.7f,
                blur = false,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                onClick = onRegionClick,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = FtxAccent,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = regionLabel,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "Pilih wilayah",
                        tint = FtxTextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
        } else {
            Icon(
                imageVector = if (cached) Icons.Rounded.CloudOff else Icons.Rounded.Public,
                contentDescription = null,
                tint = FtxTextMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text = chartStatusText(cached, fetchedAt, source),
            style = MaterialTheme.typography.labelSmall,
            color = FtxTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (loading) {
            Spacer(Modifier.size(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = FtxAccent,
            )
        }
    }
}

@Composable
private fun InlineChartError(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Gagal memperbarui: $message",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text("Coba lagi", style = MaterialTheme.typography.labelMedium, color = FtxAccent)
        }
    }
}

private fun chartStatusText(
    cached: Boolean,
    fetchedAt: Long?,
    source: String?,
): String {
    val label = source?.takeIf { it.isNotBlank() } ?: "YouTube Music Charts"
    val prefix = if (cached) "Cache · $label" else label
    val time = fetchedAt?.let {
        SimpleDateFormat("d MMM, HH.mm", Locale.forLanguageTag("id-ID")).format(Date(it))
    }
    return if (time == null) prefix else "$prefix · $time"
}

@Composable
private fun HomeHeader(
    modifier: Modifier = Modifier,
    onInfo: () -> Unit,
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = greeting.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = FtxTextMuted,
            )
            Text(
                text = "Rizumu",
                style = MaterialTheme.typography.displaySmall,
                color = FtxText,
            )
        }
        GlassIconButton(
            icon = Icons.Rounded.Info,
            contentDescription = "Tentang Rizumu",
            onClick = onInfo,
        )
    }
}

@Composable
private fun QuickActionTile(
    action: QuickAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassSurface(
        modifier = modifier.bouncyClick(pressHighlight = true, onClick = onClick),
        shape = RoundedCornerShape(FtxRadius.md),
        fillAlpha = 0.65f,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(FtxAccent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = FtxAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
