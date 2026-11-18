package com.kyrix.ftxmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyrix.ftxmusic.playback.PlayerUiState
import com.kyrix.ftxmusic.ui.artwork.ArtworkProfile
import com.kyrix.ftxmusic.ui.rememberReducedMotion
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted
import kotlinx.coroutines.delay

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberLightHaptic()
    val reduceMotion = rememberReducedMotion()
    val pressed by interaction.collectIsPressedAsState()
    var opening by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed && !reduceMotion -> 0.96f
            opening && !reduceMotion -> 1.04f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "miniPlayerPress",
    )
    val pressTint by animateFloatAsState(
        targetValue = if (pressed) 0.07f else 0f,
        animationSpec = tween(90),
        label = "miniPlayerTint",
    )

    LaunchedEffect(opening) {
        if (opening) {
            delay(90)
            opening = false
            onOpen()
        }
    }

    ChromeGlassSurface(
        modifier = modifier
            .width(ChromeSpec.Width)
            .height(ChromeSpec.Height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptic()
                    opening = true
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(
                url = state.artworkUrl,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(11.dp),
                iconSize = 18.dp,
                videoId = state.mediaId,
                profile = ArtworkProfile.MINI,
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "Rizumu" },
                    style = MaterialTheme.typography.titleSmall,
                    color = FtxText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = state.artist.ifBlank { "Siap memutar" },
                    style = MaterialTheme.typography.bodySmall,
                    color = FtxTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BouncyIconButton(
                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = FtxAccent,
                onClick = onTogglePlayPause,
                buttonSize = 38.dp,
                iconSize = 23.dp,
            )

            BouncyIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Berikutnya",
                tint = FtxText,
                onClick = onNext,
                buttonSize = 38.dp,
                iconSize = 23.dp,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.14f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(FtxAccent),
            )
        }

        if (pressTint > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = pressTint)),
            )
        }
    }
}
