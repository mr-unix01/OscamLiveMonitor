package com.mrunix.oscamlivemonitor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpressiveSectionLabel(
    titolo: String,
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    coloreAccento: Color,
    compatto: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = coloreAccento.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = coloreAccento,
                modifier = Modifier
                    .padding(if (compatto) 6.dp else 7.dp)
                    .size(if (compatto) 17.dp else 19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = titolo,
            fontSize = if (compatto) 18.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


@Composable
fun ExpressiveMiniInfo(
    etichetta: String,
    valore: String,
    compatto: Boolean,
    modifier: Modifier = Modifier
) {
    val temaScuroMini = androidx.compose.foundation.isSystemInDarkTheme()

    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = if (temaScuroMini) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        } else {
            Color.White
        },
        border = androidx.compose.foundation.BorderStroke(
            if (temaScuroMini) 1.dp else 1.1.dp,
            if (temaScuroMini) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            } else {
                Color(0xFFD5DDD5)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compatto) 10.dp else 12.dp,
                vertical = if (compatto) 8.dp else 10.dp
            )
        ) {
            Text(
                text = etichetta,
                fontSize = if (compatto) 10.sp else 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            val valoreDisponibile =
                valore.isNotBlank() &&
                    !valore.equals("N/A", ignoreCase = true) &&
                    !valore.equals("Sconosciuta", ignoreCase = true) &&
                    !valore.equals("Sconosciuto", ignoreCase = true)

            Text(
                text = if (valoreDisponibile) valore else "—",
                fontSize = if (compatto) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (valoreDisponibile) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}


@Composable
fun ExpressiveActiveServerCard(
    nome: String,
    host: String,
    porta: String,
    compatto: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 20.dp else 24.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.10f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF4CAF50).copy(alpha = 0.30f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compatto) 14.dp else 18.dp,
                    vertical = if (compatto) 12.dp else 15.dp
                ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.16f)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .padding(10.dp)
                        .size(if (compatto) 20.dp else 23.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = nome,
                    fontSize = if (compatto) 17.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (host.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text =
                            if (porta.isNotBlank()) "$host:$porta" else host,
                        fontSize = if (compatto) 12.sp else 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = Color(0xFF66BB6A).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "LIVE",
                    color = Color(0xFF66BB6A),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compatto) 10.sp else 11.sp,
                    modifier = Modifier.padding(
                        horizontal = 9.dp,
                        vertical = 5.dp
                    )
                )
            }
        }
    }
}


@Composable
fun ExpressiveOscamInfoCard(
    versione: String,
    uptime: String,
    cpu: String,
    ram: String,
    compatto: Boolean,
    riavvioInCorso: Boolean,
    onRiavviaClick: () -> Unit
) {
    val temaScuroCard =
        androidx.compose.foundation.isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (temaScuroCard) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = Color(0xFF29B6F6).copy(alpha = 0.13f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier
                            .padding(9.dp)
                            .size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            if (versione.isNotBlank())
                                "OSCam $versione"
                            else
                                "OSCam",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (uptime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Uptime  $uptime",
                            fontSize = 12.sp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            }

            if (cpu.isNotBlank() || ram.isNotBlank()) {
                Spacer(modifier = Modifier.height(11.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cpu.isNotBlank()) {
                        ExpressiveMiniInfo(
                            etichetta = "CPU",
                            valore = cpu,
                            compatto = compatto,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (ram.isNotBlank()) {
                        ExpressiveMiniInfo(
                            etichetta = "RAM",
                            valore = ram,
                            compatto = compatto,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = !riavvioInCorso,
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                ),
                colors =
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF7043),
                        containerColor = Color.Transparent
                    ),
                onClick = onRiavviaClick
            ) {
                Text(
                    text =
                        if (riavvioInCorso)
                            "↻  Riavvio..."
                        else
                            "↻  Riavvia OSCam",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
fun TitoloSezioneDashboard(
    titolo: String,
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    coloreAccento: Color,
    compatto: Boolean
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = coloreAccento.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = coloreAccento,
                modifier = Modifier
                    .padding(if (compatto) 6.dp else 7.dp)
                    .size(if (compatto) 17.dp else 19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = titolo,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compatto) 16.sp else 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


@Composable
fun coloreStatoDashboard(stato: String): Color {
    return when {
        stato.contains("NOT FOUND", ignoreCase = true) ||
                stato.contains("NOT OK", ignoreCase = true) ||
                stato.contains("ERROR", ignoreCase = true) ||
                stato.contains("OFFLINE", ignoreCase = true) ||
                stato.contains("DISABLED", ignoreCase = true) ||
                stato.contains("DISABILITATO", ignoreCase = true) ->
            Color(0xFFF44336)

        stato.contains("TIMEOUT", ignoreCase = true) ->
            Color(0xFFFF9800)

        stato.contains("CARDOK", ignoreCase = true) ||
                stato.contains("CONNECTED", ignoreCase = true) ||
                stato.contains("CONNESSO", ignoreCase = true) ||
                stato.contains("ONLINE", ignoreCase = true) ||
                stato.equals("OK", ignoreCase = true) ||
                stato.contains("ACTIVE", ignoreCase = true) ||
                stato.contains("ATTIVO", ignoreCase = true) ->
            Color(0xFF4CAF50)

        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun BadgeStatoDashboard(
    stato: String,
    compatto: Boolean
) {
    val statoVisualizzato =
        when (stato.trim().uppercase()) {
            "CONNECTED" -> "CONNESSO"
            "ACTIVE" -> "ATTIVO"
            "DISABLED" -> "DISABILITATO"
            else -> stato
        }

    val colore = coloreStatoDashboard(statoVisualizzato)
    val temaScuroBadge = androidx.compose.foundation.isSystemInDarkTheme()

    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = colore.copy(
            alpha = if (temaScuroBadge) 0.11f else 0.07f
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colore.copy(
                alpha = if (temaScuroBadge) 0.32f else 0.38f
            )
        )
    ) {
        Text(
            text = statoVisualizzato.ifBlank { "—" },
            color = colore,
            fontWeight = FontWeight.Bold,
            fontSize = if (compatto) 10.sp else 11.sp,
            modifier = Modifier.padding(
                horizontal = if (compatto) 7.dp else 9.dp,
                vertical = if (compatto) 3.dp else 4.dp
            )
        )
    }
}
