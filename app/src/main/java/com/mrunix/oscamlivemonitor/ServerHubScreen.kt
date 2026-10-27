package com.mrunix.oscamlivemonitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ServerHubAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ServerHubScreen(
    server: OscamServer,
    connectionState: OscamServerConnectionState,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onWebIf: () -> Unit,
    onLiveLog: () -> Unit,
    onTerminal: () -> Unit,
    onFiles: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val (connectionText, connectionColor) =
        when (connectionState) {
            OscamServerConnectionState.CHECKING ->
                "Verifica..." to MaterialTheme.colorScheme.onSurfaceVariant

            OscamServerConnectionState.ACTIVE ->
                "OSCam attivo" to Color(0xFF4CAF50)

            OscamServerConnectionState.AUTH_ERROR ->
                "Errore autenticazione" to Color(0xFFFF9800)

            OscamServerConnectionState.REACHABLE_NO_OSCAM ->
                "OSCam non risponde" to Color(0xFFFFB300)

            OscamServerConnectionState.UNREACHABLE ->
                "Non raggiungibile" to Color(0xFFEF5350)
        }

    val actions = listOf(
        ServerHubAction(
            title = "Dashboard",
            subtitle = "Apri stato, client, reader ed ECM",
            icon = Icons.Default.PlayArrow,
            onClick = onDashboard
        ),
        ServerHubAction(
            title = "WebIF",
            subtitle = "Apri l'interfaccia web OSCam",
            icon = Icons.Default.Language,
            onClick = onWebIf
        ),
        ServerHubAction(
            title = "Live Log",
            subtitle = "Segui il log OSCam in tempo reale",
            icon = Icons.Default.Description,
            onClick = onLiveLog
        ),
        ServerHubAction(
            title = "Terminale",
            subtitle = "Apri direttamente il terminale remoto",
            icon = Icons.Default.Terminal,
            onClick = onTerminal
        ),
        ServerHubAction(
            title = "File / SFTP",
            subtitle = "Gestisci i file del server",
            icon = Icons.Default.Folder,
            onClick = onFiles
        ),
        ServerHubAction(
            title = "Informazioni",
            subtitle = "Visualizza i dati del server",
            icon = Icons.Default.Info,
            onClick = onInfo
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 10.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro"
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = server.nome,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${server.host}:${server.porta}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.45f
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = connectionColor.copy(alpha = 0.14f)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = connectionColor,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Server OSCam",
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = connectionText,
                            color = connectionColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        items(
            items = actions,
            key = { it.title }
        ) { action ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.32f
                        )
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                ),
                onClick = action.onClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = action.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = action.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
