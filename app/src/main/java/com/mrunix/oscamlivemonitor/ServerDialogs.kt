package com.mrunix.oscamlivemonitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke

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

@Composable
internal fun RiordinaServerDialog(
    server: List<OscamServer>,
    onDismiss: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Riordina server",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                server.forEachIndexed { indice, elemento ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                18.dp
                            ),
                        color =
                            MaterialTheme.colorScheme.surfaceVariant
                                .copy(alpha = 0.38f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape =
                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                        12.dp
                                    ),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline
                                        .copy(alpha = 0.35f)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Text(
                                text = elemento.nome,
                                modifier = Modifier.weight(1f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                enabled = indice > 0,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Sposta server su"
                                },
                                onClick = {
                                    onMoveUp(indice)
                                }
                            ) {
                                Text(
                                    text = "↑",
                                    fontSize = 22.sp,
                                    color =
                                        if (indice > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.25f)
                                        }
                                )
                            }

                            IconButton(
                                enabled = indice < server.lastIndex,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Sposta server giù"
                                },
                                onClick = {
                                    onMoveDown(indice)
                                }
                            ) {
                                Text(
                                    text = "↓",
                                    fontSize = 22.sp,
                                    color =
                                        if (indice < server.lastIndex) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.25f)
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Fine",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {},
        shape =
            androidx.compose.foundation.shape.RoundedCornerShape(
                28.dp
            ),
        tonalElevation = 0.dp
    )
}
