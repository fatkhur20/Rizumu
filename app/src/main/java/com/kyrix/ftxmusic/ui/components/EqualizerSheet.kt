package com.kyrix.ftxmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrix.ftxmusic.playback.EqualizerController
import com.kyrix.ftxmusic.ui.theme.FtxRadius
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted

@Composable
fun EqualizerSheet(
    controller: EqualizerController,
    onDismiss: () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()

    GlassSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(FtxRadius.pill),
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "Segera hadir",
                            style = MaterialTheme.typography.labelSmall,
                            color = FtxTextMuted,
                        )
                    }
                }
                Text(
                    text = "Suara adaptif untuk audio engine berikutnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FtxTextMuted,
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = null,
                enabled = false,
                colors = SwitchDefaults.colors(
                    disabledUncheckedThumbColor = Color.White.copy(alpha = 0.42f),
                    disabledUncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                    disabledUncheckedBorderColor = Color.White.copy(alpha = 0.18f),
                ),
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            controller.presets.forEach { preset ->
                GlassChoiceChip(
                    label = preset.label,
                    selected = false,
                    enabled = false,
                    onClick = {},
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            controller.bands.forEach { band ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(160.dp)
                            .background(
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(2.dp),
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.5f)
                                .background(
                                    Color.White.copy(alpha = 0.72f),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(10.dp)
                                .background(Color.White.copy(alpha = 0.86f), CircleShape),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = band.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = FtxTextMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Equalizer akan aktif bersama audio engine baru.",
            style = MaterialTheme.typography.bodyMedium,
            color = FtxTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}
