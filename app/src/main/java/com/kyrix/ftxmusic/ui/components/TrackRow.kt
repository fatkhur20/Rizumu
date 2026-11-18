package com.kyrix.ftxmusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.ui.formatDurationSec
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted

@Composable
fun TrackRow(
    track: YoutubeTrack,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    isBuffering: Boolean = false,
    rank: Int? = null,
    showDuration: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FtxRadius.sm))
            .bouncyClick(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) FtxAccent else FtxTextMuted,
                modifier = Modifier.width(22.dp),
            )
        }

        TrackArtwork(
            url = track.thumbnailUrl,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(12.dp),
            iconSize = 22.dp,
            videoId = track.videoId,
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) FtxAccent else FtxText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = track.artist.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodyMedium,
                color = FtxTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isCurrent) {
            NowPlayingBars(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                modifier = Modifier.padding(start = 8.dp),
                size = 20.dp,
            )
        } else if (showDuration) {
            Text(
                text = formatDurationSec(track.durationSec),
                style = MaterialTheme.typography.labelMedium,
                color = FtxTextMuted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (trailing != null) {
            trailing()
        }

        if (onMoreClick != null) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Opsi lain",
                    tint = FtxTextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
