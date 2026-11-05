package com.mrunix.oscamlivemonitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
            Text(stringResource(R.string.dialog_network_title))
        },
        text = {
            Text(
                stringResource(R.string.dialog_network_message)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onContinue
            ) {
                Text(stringResource(R.string.dialog_continue))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onNotNow
            ) {
                Text(stringResource(R.string.dialog_not_now))
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
                stringResource(R.string.dialog_restart_title, nomeServer)
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.dialog_restart_message,
                    nomeServer, host, porta
                )
            )
        },
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFF7043)
                ),
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.dialog_restart_confirm, nomeServer))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
