package com.mrunix.oscamlivemonitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun SpiegazionePermessoReteDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onNotNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Accesso alla rete locale")
        },
        text = {
            Text(
                "OSCam Live Monitor deve accedere alla rete locale " +
                    "per collegarsi al WebIF di OSCam."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onContinue
            ) {
                Text("Continua")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onNotNow
            ) {
                Text("Non ora")
            }
        }
    )
}
