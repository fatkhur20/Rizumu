package com.rizumu.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rizumu.music.playback.PlaybackSpeed
import com.rizumu.music.ui.theme.FtxAccent
import com.rizumu.music.ui.theme.FtxTextMuted

@Composable
fun PlaybackSpeedSheet(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.72f,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        val dismissSheet = LocalSheetDismiss.current ?: onDismiss
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint = FtxAccent,
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Kecepatan putar",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Saat ini ${PlaybackSpeed.label(current)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaybackSpeed.presets.forEach { speed ->
                val selected = kotlin.math.abs(current - speed) < 0.001f
                GlassChoiceChip(
                    label = PlaybackSpeed.label(speed),
                    selected = selected,
                    onClick = {
                        onSelect(speed)
                        dismissSheet()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Kecepatan mengubah tempo tanpa mengubah pitch.",
            style = MaterialTheme.typography.labelSmall,
            color = FtxTextMuted,
        )
    }
}
