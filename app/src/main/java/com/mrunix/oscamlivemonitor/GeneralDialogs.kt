package com.mrunix.oscamlivemonitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults

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

@Composable
internal fun RiavviaOscamDialog(
    nomeServer: String,
    host: String,
    porta: String,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                "Riavviare $nomeServer?"
            )
        },
        text = {
            Text(
                "Verrà riavviato SOLO il server OSCam attivo:\n\n" +
                    "$nomeServer\n" +
                    "$host:$porta\n\n" +
                    "Gli altri server salvati non verranno toccati. " +
                    "I client di questo server verranno temporaneamente " +
                    "disconnessi e OSCam Live Monitor proverà a riconnettersi " +
                    "automaticamente."
            )
        },
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFF7043)
                ),
                onClick = onConfirm
            ) {
                Text("Riavvia $nomeServer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text("Annulla")
            }
        }
    )
}
