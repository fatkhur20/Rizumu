package com.rizumu.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rizumu.music.data.lyrics.LyricsParser
import com.rizumu.music.data.lyrics.LyricsResult
import com.rizumu.music.data.lyrics.LyricsStore
import com.rizumu.music.data.yt.YoutubeTrack
import com.rizumu.music.ui.components.ftxGlass
import com.rizumu.music.ui.theme.FtxAccent
import com.rizumu.music.ui.theme.FtxText
import com.rizumu.music.ui.theme.FtxTextMuted

private sealed interface PanelState {
    data object Loading : PanelState
    data class Ready(val result: LyricsResult.Ready) : PanelState
    data class Message(val text: String, val retryable: Boolean = true) : PanelState
}

@Composable
fun LyricsPanel(
    track: YoutubeTrack?,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var retry by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf<PanelState>(PanelState.Loading) }
    val videoId = track?.videoId

    LaunchedEffect(videoId, retry) {
        if (track == null) {
            panel = PanelState.Message("Tidak ada lagu yang diputar.", retryable = false)
            return@LaunchedEffect
        }
        panel = PanelState.Loading
        panel = when (val result = LyricsStore.load(track, force = retry > 0)) {
            is LyricsResult.Ready -> PanelState.Ready(result)
            LyricsResult.Unavailable -> PanelState.Message("Lirik tidak tersedia untuk lagu ini.")
            is LyricsResult.Failed -> PanelState.Message(result.message)
        }
    }

    Box(
        modifier = modifier.ftxGlass(
            shape = RoundedCornerShape(26.dp),
            fillAlpha = 0.60f,
        ),
    ) {
        when (val state = panel) {
            PanelState.Loading -> Centered {
                CircularProgressIndicator(color = FtxAccent, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("Memuat lirik…", color = FtxTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            is PanelState.Message -> Centered {
                Text(
                    text = state.text,
                    color = FtxTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (state.retryable) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { retry += 1 }) {
                        Text("Coba lagi", color = FtxAccent)
                    }
                }
            }
            is PanelState.Ready -> LyricsBody(
                result = state.result,
                positionMs = positionMs,
                onSeek = onSeek,
            )
        }
    }
}

@Composable
private fun LyricsBody(
    result: LyricsResult.Ready,
    positionMs: Long,
    onSeek: (Long) -> Unit,
) {
    val lyrics = result.lyrics
    val listState = rememberLazyListState()

    if (lyrics.instrumental) {
        Centered {
            Text(
                "Instrumental",
                style = MaterialTheme.typography.titleMedium,
                color = FtxText,
            )
            Text(
                "Lagu ini tidak memiliki lirik vokal.",
                style = MaterialTheme.typography.bodySmall,
                color = FtxTextMuted,
            )
        }
        return
    }

    if (lyrics.synced.isEmpty()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "LIRIK",
                    style = MaterialTheme.typography.labelMedium,
                    color = FtxTextMuted,
                )
            }
            itemsIndexed(lyrics.plain) { _, line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FtxText.copy(alpha = 0.86f),
                )
            }
        }
        return
    }

    var offsetMs by remember { mutableIntStateOf(LyricsStore.offsetMs()) }

    fun adjust(delta: Int) {
        val next = (offsetMs + delta).coerceIn(
            LyricsStore.MIN_OFFSET_MS,
            LyricsStore.MAX_OFFSET_MS,
        )
        offsetMs = next
        LyricsStore.setOffsetMs(next)
    }

    val active = LyricsParser.activeIndex(lyrics.synced, positionMs, offsetMs.toLong())

    LaunchedEffect(active) {
        if (active >= 0) {
            listState.animateScrollToItem((active - 2).coerceAtLeast(0))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 6.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "LIRIK",
                    style = MaterialTheme.typography.labelMedium,
                    color = FtxTextMuted,
                )
                if (result.fromCache) {
                    Text(
                        "tersimpan offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = FtxAccent.copy(alpha = 0.8f),
                    )
                }
            }
            TextButton(onClick = { adjust(-500) }) {
                Text("−0.5s", color = FtxTextMuted, style = MaterialTheme.typography.labelMedium)
            }
            TextButton(
                onClick = {
                    offsetMs = LyricsStore.DEFAULT_OFFSET_MS
                    LyricsStore.setOffsetMs(offsetMs)
                },
            ) {
                Text(
                    text = formatOffset(offsetMs),
                    color = FtxAccent,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(onClick = { adjust(500) }) {
                Text("+0.5s", color = FtxTextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(lyrics.synced) { index, line ->
                val isActive = index == active
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isActive -> FtxAccent
                        index < active -> FtxTextMuted.copy(alpha = 0.55f)
                        else -> FtxText.copy(alpha = 0.82f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeek((line.timeMs + offsetMs).coerceAtLeast(0L)) }
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}

private fun formatOffset(offsetMs: Int): String {
    val sign = when {
        offsetMs > 0 -> "+"
        offsetMs < 0 -> "-"
        else -> ""
    }
    val abs = kotlin.math.abs(offsetMs)
    val tenths = abs % 1000 / 100
    return "$sign${abs / 1000}.${tenths}s"
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}
