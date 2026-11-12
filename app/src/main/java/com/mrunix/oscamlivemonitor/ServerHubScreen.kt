package com.mrunix.oscamlivemonitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
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


    val scuro = com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val blu = if (scuro) Color(0xFF112B4F) else Color(0xFF1565C0)
    val sfondo = if (scuro) Color(0xFF171A1F) else Color.White
    val testo = if (scuro) Color.White else Color(0xFF152235)
    val icone = if (scuro) Color(0xFF4DA3FF) else Color(0xFF1565C0)
    val sfondoIcone = if (scuro) Color(0xFF1D3249) else Color(0xFFE5F1FF)
    val separatore = if (scuro) Color(0xFF303842) else Color(0xFFE2E8F0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(sfondo),
        contentPadding = PaddingValues(0.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(blu)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 24.dp,
                            bottom = 50.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.clickable(onClick = onBack),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = Color.White,
                            modifier = Modifier
                                .padding(11.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = server.nome,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(7.dp))

                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        connectionColor,
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                                    .semantics {
                                        contentDescription = connectionText
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "${server.host}:${server.porta}",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 13.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            sfondo,
                            RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 28.dp
                            )
                        )
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 18.dp,
                            bottom = 22.dp
                        )
                ) {
                    actions.forEachIndexed { index, action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 66.dp)
                                .clickable(onClick = action.onClick)
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = sfondoIcone
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = icone,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = action.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = testo,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "›",
                                fontSize = 26.sp,
                                color = if (scuro)
                                    Color(0xFFABB7C7)
                                else
                                    Color(0xFF59677B)
                            )
                        }

                        if (index < actions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = 68.dp,
                                    end = 6.dp
                                ),
                                thickness = 1.dp,
                                color = separatore
                            )
                        }
                    }
                }
            }
        }
    }

}
