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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kyrix.ftxmusic.data.local.UserLibrary
import com.kyrix.ftxmusic.data.yt.SearchFilter
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.playback.PlaybackManager
import com.kyrix.ftxmusic.ui.components.BouncyEntrance
import com.kyrix.ftxmusic.ui.components.EmptyState
import com.kyrix.ftxmusic.ui.components.ErrorState
import com.kyrix.ftxmusic.ui.components.GlassSurface
import com.kyrix.ftxmusic.ui.components.LocalBottomContentPadding
import com.kyrix.ftxmusic.ui.components.ftxGlass
import com.kyrix.ftxmusic.ui.components.SkeletonTrackRow
import com.kyrix.ftxmusic.ui.components.bouncyClick
import com.kyrix.ftxmusic.ui.components.TrackRow
import com.kyrix.ftxmusic.ui.components.SongActionSheet
import com.kyrix.ftxmusic.ui.search.SearchViewModel
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted
import kotlinx.coroutines.flow.distinctUntilChanged

private val SUGGESTED_QUERIES = listOf(
    "Lofi",
    "Pop Indonesia",
    "Dangdut",
    "Jazz",
    "Rock",
    "Acoustic",
)

private data class GenreCard(val label: String)

private val GENRE_CARDS = listOf(
    GenreCard("Pop Indonesia"),
    GenreCard("Dangdut"),
    GenreCard("K-Pop"),
    GenreCard("Indie"),
    GenreCard("Lofi"),
    GenreCard("Jazz"),
)

private val SEARCH_IDEAS = listOf(
    "K-Pop",
    "Indie Indonesia",
    "Cover akustik",
    "Lagu galau",
    "Dangdut koplo",
)

@Composable
fun SearchScreen(
    playback: PlaybackManager,
    initialQuery: String = "",
    initialGenre: String = "",
    requestKey: Int = 0,
) {
    val viewModel: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(playback.youtube, initialQuery, initialGenre) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by playback.state.collectAsStateWithLifecycle()
    val library by UserLibrary.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val bottomContentPadding = LocalBottomContentPadding.current
    var optionsTrack by remember { mutableStateOf<YoutubeTrack?>(null) }
    var appliedRequestKey by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(listState, viewModel) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { index -> viewModel.maybeLoadMore(index) }
    }

    LaunchedEffect(initialQuery, initialGenre, requestKey) {
        if (requestKey == appliedRequestKey) return@LaunchedEffect
        appliedRequestKey = requestKey
        val current = viewModel.state.value
        when {
            initialGenre.isNotBlank() -> {
                val resolvedQuery = initialQuery.ifBlank { initialGenre }
                if (current.activeGenre != initialGenre || current.activeQuery != resolvedQuery) {
                    viewModel.openCategory(initialGenre, resolvedQuery)
                }
            }
            initialQuery.isNotBlank() -> {
                if (initialQuery != current.activeQuery || !current.hasSearched) {
                    viewModel.submitQuery(initialQuery)
                }
            }
            else -> viewModel.clearGenre()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Cari",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .ftxGlass(shape = RoundedCornerShape(FtxRadius.pill), fillAlpha = 0.62f),
        ) {
        OutlinedTextField(
            value = state.queryText,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(FtxRadius.pill),
            placeholder = { Text("Lagu, artis, atau playlist") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = FtxTextMuted,
                )
            },
            trailingIcon = {
                if (state.queryText.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hapus pencarian",
                        tint = FtxTextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.onQueryChange("")
                                focusManager.clearFocus()
                            },
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.submitQuery(state.queryText)
                },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = FtxAccent,
            ),
        )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchFilterChip("Semua", state.filter == SearchFilter.ALL) {
                viewModel.onFilterChange(SearchFilter.ALL)
            }
            SearchFilterChip("Terbaru", state.filter == SearchFilter.RECENT) {
                viewModel.onFilterChange(SearchFilter.RECENT)
            }
            SearchFilterChip("7 hari", state.filter == SearchFilter.WEEK) {
                viewModel.onFilterChange(SearchFilter.WEEK)
            }
            SearchFilterChip("30 hari", state.filter == SearchFilter.MONTH) {
                viewModel.onFilterChange(SearchFilter.MONTH)
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                state.activeGenre?.let { genre ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Hasil kategori",
                                style = MaterialTheme.typography.labelMedium,
                                color = FtxTextMuted,
                            )
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = viewModel::clearGenre) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup kategori",
                                tint = FtxTextMuted,
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    when {
                        state.queryText.isBlank() && state.activeGenre == null -> DiscoveryContent(
                            recentSearches = library.recentSearches,
                            onPick = { query ->
                                focusManager.clearFocus()
                                viewModel.submitQuery(query)
                            },
                            onCategory = { category ->
                                focusManager.clearFocus()
                                viewModel.openCategory(category)
                            },
                            onRemove = UserLibrary::removeSearch,
                        )

                        state.isLoading -> Column {
                            repeat(6) { SkeletonTrackRow() }
                        }

                        state.error != null -> ErrorState(
                            message = state.error!!,
                            onRetry = viewModel::retry,
                        )

                        state.hasSearched && state.results.isEmpty() -> EmptyState(
                            title = "Tidak ada hasil",
                            message = "Coba kata kunci lain atau ubah filter pencarian.",
                        )

                        else -> LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = 24.dp + bottomContentPadding,
                            ),
                        ) {
                            itemsIndexed(
                                items = state.results,
                                key = { index, track -> "${track.videoId}-$index" },
                            ) { index, track ->
                                TrackRow(
                                    track = track,
                                    isCurrent = playerState.mediaId == track.videoId,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                                    onClick = { playback.playTracks(state.results, index) },
                                    onLongClick = { optionsTrack = track },
                                    onMoreClick = { optionsTrack = track },
                                )
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = FtxAccent,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                        }
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
            onOpenArtist = { artist -> viewModel.submitQuery(artist) },
        )
    }
}

@Composable
private fun DiscoveryContent(
    recentSearches: List<String>,
    onPick: (String) -> Unit,
    onCategory: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val bottomContentPadding = LocalBottomContentPadding.current
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp + bottomContentPadding),
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Riwayat pencarian",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { recentSearches.forEach(onRemove) }) {
                        Text("Hapus", color = FtxTextMuted)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentSearches, key = { it }) { query ->
                        HistoryChip(query = query, onClick = { onPick(query) }, onRemove = { onRemove(query) })
                    }
                }
            }
        }

        item {
            Text(
                text = "Ide pencarian",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SEARCH_IDEAS, key = { it }) { query ->
                    SuggestionChip(query = query, icon = Icons.Rounded.Search, onClick = { onCategory(query) })
                }
            }
        }

        item {
            Text(
                text = "Jelajahi genre",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GENRE_CARDS.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEachIndexed { columnIndex, card ->
                            BouncyEntrance(
                                index = rowIndex * 2 + columnIndex,
                                modifier = Modifier.weight(1f),
                            ) {
                                GenreTile(
                                    card = card,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onCategory(card.label) },
                                )
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text(
                text = "Saran untukmu",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SUGGESTED_QUERIES, key = { it }) { query ->
                    SuggestionChip(query = query, onClick = { onCategory(query) })
                }
            }
        }

        item {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                shape = RoundedCornerShape(FtxRadius.lg),
                fillAlpha = 0.55f,
                blur = false,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(FtxAccent.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = FtxAccent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "Ketik judul lagu atau nama artis. Tekan lama hasil untuk opsi antrian dan suka.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FtxTextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(query: String, onClick: () -> Unit, onRemove: () -> Unit) {
    GlassSurface(

        blur = false,
        contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = FtxTextMuted,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(text = query, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.size(6.dp))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Hapus $query",
                tint = FtxTextMuted,
                modifier = Modifier
                    .size(15.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    query: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    GlassSurface(

        blur = false,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FtxAccent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.size(6.dp))
            }
            Text(text = query, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SearchFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(FtxRadius.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color.White.copy(alpha = 0.18f),
            selectedLabelColor = FtxText,
            containerColor = Color.White.copy(alpha = 0.08f),
            labelColor = FtxTextMuted,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.14f),
            selectedBorderColor = Color.White.copy(alpha = 0.35f),
        ),
    )
}

@Composable
private fun GenreTile(
    card: GenreCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassSurface(
        modifier = modifier
            .height(78.dp)
            .bouncyClick(pressHighlight = true, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        fillAlpha = 0.62f,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                text = card.label,
                style = MaterialTheme.typography.titleMedium,
                color = FtxText,
            )
            Text(
                text = "Jelajahi",
                style = MaterialTheme.typography.labelMedium,
                color = FtxTextMuted,
            )
        }
    }
}
