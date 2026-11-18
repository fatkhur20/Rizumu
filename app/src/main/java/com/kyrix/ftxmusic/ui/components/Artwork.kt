package com.kyrix.ftxmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyrix.ftxmusic.data.yt.YoutubeTrack
import com.kyrix.ftxmusic.ui.artwork.ArtworkProfile
import com.kyrix.ftxmusic.ui.artwork.ArtworkUrls
import com.kyrix.ftxmusic.ui.theme.FtxBlack
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxSurface
import com.kyrix.ftxmusic.ui.theme.FtxSurfaceVariant
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted

@Composable
fun TrackArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(FtxRadius.md),
    iconSize: Dp = 26.dp,
    videoId: String? = null,
    profile: ArtworkProfile = ArtworkProfile.LIST,
) {
    val context = LocalContext.current
    val urls = remember(url, videoId, profile) { ArtworkUrls.chain(url, videoId, profile) }
    var attempt by remember(url, videoId, profile) { mutableIntStateOf(0) }
    val exhausted = urls.isEmpty() || attempt > urls.lastIndex

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(FtxSurfaceVariant, FtxSurface),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!exhausted) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(urls[attempt.coerceAtMost(urls.lastIndex)])
                    .size(profile.targetPx)
                    .crossfade(180)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    attempt = if (attempt < urls.lastIndex) attempt + 1 else urls.size
                },
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = FtxTextMuted,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun ArtworkCard(
    track: YoutubeTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 142.dp,
    rank: Int? = null,
    profile: ArtworkProfile = ArtworkProfile.CARD,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(cardSize)
            .bouncyClick(onClick = onClick, onLongClick = onLongClick)
            .padding(bottom = 2.dp),
    ) {
        Box {
            TrackArtwork(
                url = track.thumbnailUrl,
                modifier = Modifier.size(cardSize),
                shape = RoundedCornerShape(FtxRadius.md),
                iconSize = 32.dp,
                videoId = track.videoId,
                profile = profile,
            )
            if (rank != null) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .background(
                            if (rank <= 3) FtxAccent else FtxBlack.copy(alpha = 0.72f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rank <= 3) FtxBlack else FtxText,
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = FtxText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 1.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = track.artist.ifBlank { "Unknown artist" },
            style = MaterialTheme.typography.bodyMedium,
            color = FtxTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 1.dp),
        )
    }
}
