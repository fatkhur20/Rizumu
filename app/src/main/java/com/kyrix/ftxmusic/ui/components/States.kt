package com.kyrix.ftxmusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyrix.ftxmusic.ui.rememberReducedMotion
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxSurface
import com.kyrix.ftxmusic.ui.theme.FtxSurfaceVariant
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(FtxRadius.sm),
) {
    if (rememberReducedMotion()) {
        Box(modifier.background(FtxSurfaceVariant.copy(alpha = 0.6f), shape))
        return
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            FtxSurface.copy(alpha = 0.55f),
            FtxSurfaceVariant.copy(alpha = 0.95f),
            FtxSurface.copy(alpha = 0.55f),
        ),
        start = Offset(x = -400f + progress * 900f, y = 0f),
        end = Offset(x = progress * 900f, y = 260f),
    )
    Box(modifier.background(brush, shape))
}

@Composable
fun SkeletonTrackRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(Modifier.size(50.dp), RoundedCornerShape(12.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            SkeletonBox(Modifier.fillMaxWidth(0.66f).height(13.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.4f).height(11.dp))
        }
        Spacer(Modifier.width(12.dp))
        SkeletonBox(Modifier.size(width = 34.dp, height = 11.dp))
    }
}

@Composable
fun SkeletonCardColumn(modifier: Modifier = Modifier) {
    Column(modifier) {
        SkeletonBox(Modifier.size(140.dp), RoundedCornerShape(FtxRadius.md))
        Spacer(Modifier.height(10.dp))
        SkeletonBox(Modifier.fillMaxWidth(0.8f).height(12.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonBox(Modifier.fillMaxWidth(0.55f).height(10.dp))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 1.dp),
        )
        when {
            actionIcon != null && onAction != null -> {
                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionDescription,
                        tint = FtxAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            actionLabel != null && onAction != null -> {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = FtxAccent,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.MusicOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FtxSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FtxTextMuted,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = FtxTextMuted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = FtxAccent)
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = FtxTextMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = FtxTextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onRetry) {
            Text("Coba lagi", color = FtxAccent)
        }
    }
}

@Composable
fun ComingSoonBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(FtxAccent.copy(alpha = 0.12f), RoundedCornerShape(FtxRadius.pill))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "SEGERA",
            style = MaterialTheme.typography.labelSmall,
            color = FtxAccent,
        )
    }
}
