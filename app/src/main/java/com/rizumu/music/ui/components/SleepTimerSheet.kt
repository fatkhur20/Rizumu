package com.rizumu.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizumu.music.playback.SleepTimerController
import com.rizumu.music.playback.SleepTimerMath
import com.rizumu.music.ui.theme.FtxAccent
import com.rizumu.music.ui.theme.FtxRadius
import com.rizumu.music.ui.theme.FtxTextMuted

private val timerOptions = listOf(15, 30, 45, 60)

@Composable
fun SleepTimerSheet(
    controller: SleepTimerController,
    onDismiss: () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()

    GlassSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.80f,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = if (state.active) FtxAccent else FtxTextMuted,
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Timer tidur",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = if (state.active) {
                        if (state.fading) {
                            "Memudarkan volume..."
                        } else {
                            "Sisa ${SleepTimerMath.remainingLabel(state.remainingMs)}"
                        }
                    } else {
                        "Pemutaran berhenti otomatis setelah timer habis."
                    },
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
            timerOptions.forEach { minutes ->
                val selected = state.active && state.durationMs == minutes * 60_000L
                GlassChoiceChip(
                    label = "$minutes mnt",
                    selected = selected,
                    onClick = { controller.start(minutes) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.active) {
            Spacer(Modifier.height(14.dp))
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClick(onClick = { controller.cancel() }),
                shape = RoundedCornerShape(FtxRadius.md),
                fillAlpha = 0.34f,
                tintAlpha = 0.05f,
                blur = false,
                shadowElevation = 0.dp,
                contentPadding = PaddingValues(vertical = 13.dp),
            ) {
                Text(
                    text = "Batalkan timer",
                    style = MaterialTheme.typography.labelLarge,
                    color = FtxTextMuted,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
