package com.mrunix.oscamlivemonitor

import com.mrunix.oscamlivemonitor.ui.theme.OscamPalette

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolsHeader(
    title: String,
    icon: ImageVector,
    serverName: String,
    serverHost: String,
    onBack: () -> Unit,
    onFullscreen: (() -> Unit)? = null,
    compact: Boolean = false,
    flat: Boolean = false
) {
    val scuroTools =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    val bluTools =
        if (scuroTools) OscamPalette.BannerDark else OscamPalette.BannerLight

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape =
            if (flat) RoundedCornerShape(0.dp)
            else RoundedCornerShape(20.dp),
        color = bluTools,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (flat) 16.dp else if (compact) 4.dp else 9.dp,
                end = if (flat) 16.dp else if (compact) 4.dp else 9.dp,
                top = if (flat) 24.dp else if (compact) 2.dp else 9.dp,
                bottom = if (flat) 26.dp else if (compact) 2.dp else 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.White.copy(alpha = 0.16f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(if (compact) 34.dp else 42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(13.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(if (compact) 6.dp else 8.dp)
                        .size(if (compact) 17.dp else 20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = if (compact) 16.sp else 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = if (
                        serverHost.isBlank() ||
                        serverName.equals(serverHost, ignoreCase = true)
                    ) {
                        serverName
                    } else {
                        "$serverName  •  $serverHost"
                    },
                    fontSize = if (compact) 10.sp else 12.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1
                )
            }

            if (onFullscreen != null) {
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onFullscreen,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = stringResource(R.string.tools_expand_terminal),
                            tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolsModeButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        modifier = modifier.height(if (compact) 40.dp else 54.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) {
                accent.copy(alpha = 0.80f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                accent.copy(alpha = 0.11f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            }
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 17.dp else 20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ToolsModeSelector(
    fileSelected: Boolean,
    onTerminal: () -> Unit,
    onFile: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolsModeButton(
            title = stringResource(R.string.hub_terminal),
            icon = Icons.Default.Terminal,
            selected = !fileSelected,
            accent = OscamPalette.AccentDark,
            onClick = onTerminal,
            modifier = Modifier.weight(1f),
            compact = compact
        )

        ToolsModeButton(
            title = stringResource(R.string.settings_files),
            icon = Icons.Default.Folder,
            selected = fileSelected,
            accent = OscamPalette.AccentDark,
            onClick = onFile,
            modifier = Modifier.weight(1f),
            compact = compact
        )
    }
}

@Composable
fun ToolsBinarySelector(
    leftTitle: String,
    rightTitle: String,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    leftSelected: Boolean,
    accent: Color,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val temaScuro =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val accentEffettivo =
        if (temaScuro) accent else OscamPalette.BannerLight

    val accentTesto =
        if (temaScuro) accent else OscamPalette.BannerLight

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            leftTitle to leftIcon,
            rightTitle to rightIcon
        ).forEachIndexed { index, (title, icon) ->
            val selected =
                if (index == 0) leftSelected else !leftSelected

            val action =
                if (index == 0) onLeft else onRight
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                onClick = action,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) {
                        accentEffettivo.copy(
                            alpha = if (temaScuro) 0.75f else 0.85f
                        )
                    } else {
                        if (temaScuro) Color(0xFF303842) else Color(0xFFE2E8F0)
                    }
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        if (temaScuro) {
                            Color(0xFF1D3249)
                        } else {
                            Color(0xFFE5F1FF)
                        }
                    } else {
                        if (temaScuro) OscamPalette.SheetDark else Color.White
                    }
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selected)
                            accentTesto
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = title,
                        fontWeight = if (selected)
                            FontWeight.Bold
                        else
                            FontWeight.Medium,
                        color = if (selected)
                            accentTesto
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ToolsConnectionStatus(
    text: String,
    modifier: Modifier = Modifier
) {
    val temaScuroStatus =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val nonConnessoStatus =
        listOf(
            "Disconnesso",
            "Non connesso",
            "Disconnected",
            "Not connected"
        ).any { text.contains(it, ignoreCase = true) }

    val color = when {
        text.contains("Errore", ignoreCase = true) ||
        text.contains("Error", ignoreCase = true) ->
            Color(0xFFEF5350)

        nonConnessoStatus ->
            MaterialTheme.colorScheme.onSurfaceVariant

        text.contains("Connessione", ignoreCase = true) ||
        text.contains("Connecting", ignoreCase = true) ->
            Color(0xFFFFB74D)

        text.contains("Connesso", ignoreCase = true) ||
        text.contains("Connected", ignoreCase = true) ->
            Color(0xFF66BB6A)

        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color =
            if (nonConnessoStatus) {
                if (temaScuroStatus) Color(0xFF1E2A3B)
                else Color(0xFFE8F1FD)
            } else {
                color.copy(alpha = 0.10f)
            },
        border =
            if (nonConnessoStatus) null
            else BorderStroke(
                1.dp,
                color.copy(alpha = 0.32f)
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(7.dp),
                shape = RoundedCornerShape(50),
                color = color
            ) {}

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
