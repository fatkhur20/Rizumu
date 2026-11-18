package com.kyrix.ftxmusic.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyrix.ftxmusic.data.update.AvailableUpdate
import com.kyrix.ftxmusic.ui.theme.FtxText
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted
import java.io.File
import java.util.Locale

sealed interface UpdateDialogState {
    data class Available(val update: AvailableUpdate) : UpdateDialogState
    data class Downloading(
        val update: AvailableUpdate,
        val progress: Float,
    ) : UpdateDialogState
    data class InstallPending(
        val update: AvailableUpdate,
        val file: File,
        val message: String,
    ) : UpdateDialogState
    data class Message(
        val title: String,
        val message: String,
    ) : UpdateDialogState
}

@Composable
fun UpdateDialog(
    state: UpdateDialogState,
    onPrimary: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
) {
    val downloading = state is UpdateDialogState.Downloading
    Dialog(
        onDismissRequest = { if (!downloading) onLater() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !downloading,
            dismissOnClickOutside = !downloading,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                fillAlpha = 0.96f,
                blur = false,
                glow = true,
                contentPadding = PaddingValues(22.dp),
            ) {
                Column {
                    when (state) {
                        is UpdateDialogState.Available -> {
                            Text(
                                text = "Update tersedia v${state.update.version}",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${state.update.apkSize?.let(::formatBytes) ?: "Ukuran tidak diketahui"} · GitHub Releases",
                                style = MaterialTheme.typography.labelMedium,
                                color = FtxTextMuted,
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Catatan rilis",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = state.update.notes.ifBlank { "Catatan rilis tidak tersedia." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = FtxTextMuted,
                                maxLines = 12,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = onSkip) {
                                    Text("Lewati versi ini", color = FtxTextMuted)
                                }
                                TextButton(onClick = onLater) {
                                    Text("Nanti", color = FtxTextMuted)
                                }
                                TextButton(onClick = onPrimary) {
                                    Text("Perbarui", color = FtxText)
                                }
                            }
                        }

                        is UpdateDialogState.Downloading -> {
                            Text(
                                text = "Mengunduh v${state.update.version}",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "APK disimpan sementara di cache aplikasi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FtxTextMuted,
                            )
                            Spacer(Modifier.height(18.dp))
                            BouncyProgress(state.progress)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = FtxTextMuted,
                            )
                        }

                        is UpdateDialogState.InstallPending -> {
                            Text(
                                text = "Siap dipasang",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FtxTextMuted,
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = onLater) {
                                    Text("Nanti", color = FtxTextMuted)
                                }
                                TextButton(onClick = onPrimary) {
                                    Text("Pasang", color = FtxText)
                                }
                            }
                        }

                        is UpdateDialogState.Message -> {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FtxTextMuted,
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = onPrimary) {
                                    Text("Tutup", color = FtxText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BouncyProgress(progress: Float) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "updateDownloadProgress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animated
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(Color.White.copy(alpha = 0.88f), CircleShape),
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1024.0
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return String.format(Locale.US, "%.1f %s", value, units[index])
}
