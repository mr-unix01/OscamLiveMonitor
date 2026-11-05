package com.mrunix.oscamlivemonitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ServerHubAction(
    val title: String,
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
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val (connectionText, connectionColor) =
        when (connectionState) {
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

    val actions = listOf(
        ServerHubAction(
            title = stringResource(R.string.settings_dashboard),
            icon = Icons.Default.PlayArrow,
            onClick = onDashboard
        ),
        ServerHubAction(
            title = "WebIF",
            icon = Icons.Default.Language,
            onClick = onWebIf
        ),
        ServerHubAction(
            title = "Live Log",
            icon = Icons.Default.Description,
            onClick = onLiveLog
        ),
        ServerHubAction(
            title = stringResource(R.string.hub_terminal),
            icon = Icons.Default.Terminal,
            onClick = onTerminal
        ),
        ServerHubAction(
            title = stringResource(R.string.settings_files),
            icon = Icons.Default.Folder,
            onClick = onFiles
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
                        contentDescription = stringResource(R.string.settings_back)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = server.nome,
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow =
                                androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.hub_server_status, connectionText),
                            tint = connectionColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${server.host}:${server.porta}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = Color(0xFF4DA3FF),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = action.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
