package com.mrunix.oscamlivemonitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class OscamServerConnectionState {
    CHECKING,
    ACTIVE,
    AUTH_ERROR,
    REACHABLE_NO_OSCAM,
    UNREACHABLE
}

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
        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(50.dp),
            border = BorderStroke(
                1.2.dp,
                if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current)
                    Color(0xFF4DA3FF)
                else
                    Color(0xFF1565C0)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor =
                    if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current)
                        Color(0xFF112B4F)
                    else Color.Transparent,
                contentColor =
                    if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current)
                        Color(0xFF4DA3FF)
                    else
                        Color(0xFF1565C0)
            ),
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
                text = stringResource(R.string.home_add),
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
                text = stringResource(R.string.home_no_servers),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = stringResource(R.string.home_add_first),
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
    connectionState: OscamServerConnectionState,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    val darkTheme = com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    val descrizioneApri = stringResource(R.string.home_open_server, server.nome)
    val selectionColor = Color(0xFF4DA3FF)

    val (connectionText, connectionColor) = when (connectionState) {
        OscamServerConnectionState.CHECKING ->
            stringResource(R.string.home_checking) to Color(0xFFFF9800)
        OscamServerConnectionState.ACTIVE ->
            stringResource(R.string.home_active) to Color(0xFF4CAF50)
        OscamServerConnectionState.AUTH_ERROR ->
            stringResource(R.string.home_auth_error) to Color(0xFFFF9800)
        OscamServerConnectionState.REACHABLE_NO_OSCAM ->
            stringResource(R.string.home_no_oscam) to Color(0xFFFFB300)
        OscamServerConnectionState.UNREACHABLE ->
            stringResource(R.string.home_unreachable) to Color(0xFFEF5350)
    }
    val connectionTextColor = when {
        darkTheme -> connectionColor
        connectionState == OscamServerConnectionState.ACTIVE ->
            Color(0xFF17692F)
        connectionState == OscamServerConnectionState.UNREACHABLE ->
            Color(0xFFB3261E)
        else -> connectionColor
    }

    var menuExpanded by remember(server.nome, server.host, server.porta) {
        mutableStateOf(false)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (selected) 18.dp else 0.dp),
        border = if (selected) {
            BorderStroke(1.dp, selectionColor.copy(alpha = 0.7f))
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                if (darkTheme) Color(0xFF173F62)
                else Color(0xFFE7F2FF)
            } else {
                Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        onClick = {
            if (selected) onOpen() else onSelect()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = if (selected) {
                    if (darkTheme) Color(0xFF4DA3FF)
                    else Color(0xFF1565C0)
                } else if (darkTheme) {
                    Color(0xFF1D3249)
                } else {
                    Color(0xFFE5F1FF)
                },
                border = null
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (selected) {
                            if (darkTheme) Color(0xFF112B4F)
                            else Color.White
                        } else if (darkTheme) {
                            Color(0xFF4DA3FF)
                        } else {
                            Color(0xFF1565C0)
                        },
                        modifier = Modifier.size(21.dp)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                connectionColor,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = connectionText,
                        color = connectionTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "· ${server.host}:${server.porta}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                }
            }

            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = {
                    onSelect()
                    onOpen()
                }
            ) {
                Text(
                    text = "›",
                    fontSize = 28.sp,
                    color = if (selected) {
                        if (darkTheme) Color(0xFF4DA3FF)
                        else Color(0xFF1565C0)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.semantics {
                        contentDescription = descrizioneApri
                    }
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.home_server_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_edit)) },
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
                        text = { Text(stringResource(R.string.home_reorder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Reorder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onReorder()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_delete)) },
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

        if (showDivider && !selected) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    start = 64.dp,
                    end = 12.dp
                ),
                thickness = 1.dp,
                color = if (darkTheme)
                    Color(0xFF354454)
                else
                    Color(0xFFE2E8F0)
            )
        }
    }
}

