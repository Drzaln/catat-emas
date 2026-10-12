package com.rizal.catatemas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rizal.catatemas.update.UpdateState
import java.io.File

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: (String) -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ada pembaruan") },
            text = {
                Text("Catat Emas v${state.version} sudah tersedia. Unduh dan pasang sekarang?")
            },
            confirmButton = {
                TextButton(onClick = { onDownload(state.version) }) { Text("Unduh & pasang") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Nanti") }
            },
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Mengunduh pembaruan") },
            text = {
                Column {
                    Text("v${state.version} — ${state.progress}%")
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {},
        )

        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Siap dipasang") },
            text = {
                Text(
                    "Android akan minta konfirmasi pemasangan v${state.version}. " +
                        "Kalau pemasangan diblokir, izinkan \"pasang aplikasi tidak dikenal\" untuk Catat Emas lalu tekan Pasang lagi.",
                )
            },
            confirmButton = {
                TextButton(onClick = { onInstall(state.file) }) { Text("Pasang") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Nanti") }
            },
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pembaruan gagal") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            },
        )

        UpdateState.Idle -> Unit
    }
}
