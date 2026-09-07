package com.mrunix.oscamlivemonitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolsHeader(
    serverName: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Indietro"
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(13.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Strumenti",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = serverName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(54.dp),
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
                modifier = Modifier.size(20.dp)
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
    onFile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolsModeButton(
            title = "Terminale",
            icon = Icons.Default.Terminal,
            selected = !fileSelected,
            accent = Color(0xFF66BB6A),
            onClick = onTerminal,
            modifier = Modifier.weight(1f)
        )

        ToolsModeButton(
            title = "File",
            icon = Icons.Default.Folder,
            selected = fileSelected,
            accent = Color(0xFFFFB300),
            onClick = onFile,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ToolsBinarySelector(
    leftTitle: String,
    rightTitle: String,
    leftSelected: Boolean,
    accent: Color,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple(leftTitle, leftSelected, onLeft),
            Triple(rightTitle, !leftSelected, onRight)
        ).forEach { (title, selected, action) ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                onClick = action,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    if (selected) 1.4.dp else 1.dp,
                    if (selected) accent.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        accent.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    }
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (selected)
                            FontWeight.Bold
                        else
                            FontWeight.Medium,
                        color = if (selected)
                            accent
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
    text: String
) {
    val color = when {
        text.contains("Errore", ignoreCase = true) ->
            Color(0xFFEF5350)

        text.contains("Disconnesso", ignoreCase = true) ||
        text.contains("Non connesso", ignoreCase = true) ->
            MaterialTheme.colorScheme.onSurfaceVariant

        text.contains("Connessione", ignoreCase = true) ->
            Color(0xFFFFB74D)

        text.contains("Connesso", ignoreCase = true) ->
            Color(0xFF66BB6A)

        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
