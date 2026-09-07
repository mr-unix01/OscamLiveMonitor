package com.mrunix.oscamlivemonitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componenti grafici per la selezione dei server OSCam.
 *
 * Nessun controllo di rete viene eseguito qui: la schermata resta puramente UI
 * così da non aggiungere round-trip o ritardi quando l'app viene usata tramite
 * WireGuard/VPN.
 */
@Composable
fun ServerSelectionHeader(
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "I tuoi server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Scegli un server salvato oppure inserisci i dati manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        FilledTonalButton(
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            onClick = onAddServer
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Aggiungi",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EmptyServerState(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nessun server salvato",
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Aggiungi il tuo primo server OSCam per trovarlo subito qui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OscamServerCard(
    server: OscamServer,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val selectionGreen = Color(0xFF4CAF50)
    val selectedContainer = if (darkTheme) {
        Color(0xFF17351F)
    } else {
        Color(0xFFEAF6EC)
    }

    var menuExpanded by remember(server.nome, server.host, server.porta) {
        mutableStateOf(false)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (selected) 1.6.dp else 1.dp,
            color = if (selected) {
                selectionGreen
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                selectedContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 1.dp else 0.dp
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = if (selected) {
                    selectionGreen.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (selected) {
                            selectionGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = server.nome,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected && darkTheme) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (selected) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = selectionGreen.copy(alpha = 0.16f),
                            border = BorderStroke(
                                1.dp,
                                selectionGreen.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = "SELEZIONATO",
                                color = if (darkTheme) {
                                    Color(0xFF81C784)
                                } else {
                                    Color(0xFF2E7D32)
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 7.dp,
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (selected) {
                            selectionGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "${server.host}:${server.porta}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected && darkTheme) {
                            Color(0xFFC9D8CC)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Azioni server",
                        tint = if (selected && darkTheme) {
                            Color(0xFFE4EAE5)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifica") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Elimina") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
