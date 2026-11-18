package com.kyrix.ftxmusic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyrix.ftxmusic.data.local.playlist.PlaylistNames
import com.kyrix.ftxmusic.ui.theme.FtxAccent
import com.kyrix.ftxmusic.ui.theme.FtxTextMuted

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    title: String = "Playlist baru",
    confirmLabel: String = "Buat",
    initialName: String = "",
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val normalized = PlaylistNames.normalize(name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= PlaylistNames.MAX_LENGTH) name = it },
                    singleLine = true,
                    label = { Text("Nama playlist") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (name.isNotEmpty() && normalized == null) {
                    Text(
                        text = "Nama tidak boleh kosong",
                        style = MaterialTheme.typography.labelSmall,
                        color = FtxTextMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalized != null,
                onClick = {
                    normalized?.let(onCreate)
                    onDismiss()
                },
            ) {
                Text(confirmLabel, color = if (normalized != null) FtxAccent else FtxTextMuted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = FtxTextMuted) }
        },
    )
}
