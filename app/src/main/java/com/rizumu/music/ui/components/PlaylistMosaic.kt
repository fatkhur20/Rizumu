package com.rizumu.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rizumu.music.data.local.playlist.PlaylistCover
import com.rizumu.music.ui.artwork.ArtworkProfile
import com.rizumu.music.ui.theme.FtxRadius
import com.rizumu.music.ui.theme.FtxSurfaceVariant
import com.rizumu.music.ui.theme.FtxTextMuted

@Composable
fun PlaylistMosaic(
    covers: List<PlaylistCover>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(FtxRadius.md),
    gap: Dp = 1.dp,
    iconSize: Dp = 20.dp,
) {
    val slots = covers.take(4)
    Box(
        modifier = modifier
            .clip(shape)
            .background(FtxSurfaceVariant),
    ) {
        when (slots.size) {
            0 -> Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = FtxTextMuted,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(iconSize),
            )
            1 -> MosaicArtwork(
                cover = slots[0],
                modifier = Modifier.fillMaxSize(),
                iconSize = iconSize,
            )
            2 -> Row(Modifier.fillMaxSize()) {
                MosaicArtwork(
                    cover = slots[0],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    iconSize = iconSize,
                )
                Spacer(Modifier.width(gap))
                MosaicArtwork(
                    cover = slots[1],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    iconSize = iconSize,
                )
            }
            3 -> Row(Modifier.fillMaxSize()) {
                MosaicArtwork(
                    cover = slots[0],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    iconSize = iconSize,
                )
                Spacer(Modifier.width(gap))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    MosaicArtwork(
                        cover = slots[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        iconSize = iconSize,
                    )
                    Spacer(Modifier.height(gap))
                    MosaicArtwork(
                        cover = slots[2],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        iconSize = iconSize,
                    )
                }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    MosaicArtwork(
                        cover = slots[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        iconSize = iconSize,
                    )
                    Spacer(Modifier.width(gap))
                    MosaicArtwork(
                        cover = slots[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        iconSize = iconSize,
                    )
                }
                Spacer(Modifier.height(gap))
                Row(Modifier.weight(1f)) {
                    MosaicArtwork(
                        cover = slots[2],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        iconSize = iconSize,
                    )
                    Spacer(Modifier.width(gap))
                    MosaicArtwork(
                        cover = slots[3],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        iconSize = iconSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun MosaicArtwork(
    cover: PlaylistCover,
    modifier: Modifier,
    iconSize: Dp,
) {
    TrackArtwork(
        url = cover.thumbnailUrl,
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        iconSize = iconSize,
        videoId = cover.videoId,
        profile = ArtworkProfile.LIST,
    )
}
