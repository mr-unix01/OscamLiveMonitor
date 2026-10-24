package com.mrunix.oscamlivemonitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun EliminaServerDialog(
    nomeServer: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Eliminare server?")
        },
        text = {
            Text(
                "Vuoi eliminare definitivamente " +
                    "\"$nomeServer\"?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
            }
        }
    )
}
