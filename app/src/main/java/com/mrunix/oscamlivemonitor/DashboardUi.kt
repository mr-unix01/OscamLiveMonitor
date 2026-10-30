package com.mrunix.oscamlivemonitor

import androidx.compose.material.icons.filled.Menu

import androidx.compose.foundation.clickable

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    val temaScuroMini = com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

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
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

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
    val temaScuroBadge = com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

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


@Composable
fun VoceStatoCard(
    testo: String,
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    coloreAccento: Color,
    compatto: Boolean,
    mostraToggleReader: Boolean = false,
    readerAbilitato: Boolean = true,
    toggleReaderInCorso: Boolean = false,
    onToggleReader: (() -> Unit)? = null
) {
    val temaScuroVoce =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    val nome = testo
        .substringBeforeLast("—", testo)
        .trim()

    val stato = testo
        .substringAfterLast("—", "")
        .trim()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 18.dp else 22.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            coloreAccento.copy(
                alpha = if (temaScuroVoce) 0.28f else 0.36f
            )
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (temaScuroVoce) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compatto) 11.dp else 14.dp,
                    vertical = if (compatto) 9.dp else 11.dp
                ),
            verticalAlignment =
                androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                color = coloreAccento.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = icona,
                    contentDescription = null,
                    tint = coloreAccento,
                    modifier = Modifier
                        .padding(if (compatto) 7.dp else 8.dp)
                        .size(if (compatto) 18.dp else 20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = nome,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compatto) 14.sp else 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(7.dp))

            BadgeStatoDashboard(
                stato = stato,
                compatto = compatto
            )

            if (mostraToggleReader) {
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        onToggleReader?.invoke()
                    },
                    enabled = !toggleReaderInCorso,
                    modifier = Modifier.size(
                        if (compatto) 32.dp else 36.dp
                    )
                ) {
                    if (toggleReaderInCorso) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(
                                if (compatto) 17.dp else 19.dp
                            ),
                            strokeWidth = 2.dp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription =
                                "Abilita/disabilita reader",
                            tint =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(
                                if (compatto) 18.dp else 20.dp
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun RigaDettaglioClient(
    etichetta: String,
    valore: String,
    compatto: Boolean
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = etichetta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compatto) 10.sp else 11.sp
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = valore,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = if (compatto) 12.sp else 13.sp
        )
    }
}


@Composable
fun MiniDatoClient(
    etichetta: String,
    valore: String,
    compatto: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compatto) 6.dp else 8.dp,
                vertical = if (compatto) 3.dp else 4.dp
            ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = etichetta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compatto) 9.sp else 10.sp,
                softWrap = !compatto
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = valore,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = if (compatto) 11.sp else 12.sp,
                softWrap = !compatto
            )
        }
    }
}


@Composable
fun ExpressiveAppHeader(
    compatto: Boolean,
    onMenuClick: () -> Unit = {}
) {
    val temaScuroHeader = com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 20.dp else 24.dp
        ),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compatto) 14.dp else 18.dp,
                    vertical = if (compatto) 11.dp else 14.dp
                ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.clickable(onClick = onMenuClick),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (temaScuroHeader) {
                    Color(0xFF4DA3FF).copy(alpha = 0.16f)
                } else {
                    Color(0xFF4DA3FF).copy(alpha = 0.12f)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = if (temaScuroHeader) Color(0xFF4DA3FF) else Color(0xFF1565C0),
                    modifier = Modifier
                        .padding(if (compatto) 11.dp else 13.dp)
                        .size(if (compatto) 24.dp else 28.dp)
                )
            }

            Spacer(modifier = Modifier.width(if (compatto) 12.dp else 16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "OSCam",
                        fontSize = if (compatto) 25.sp else 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (temaScuroHeader) MaterialTheme.colorScheme.onSurface else Color(0xFF1F2320)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = "Live Monitor",
                        fontSize = if (compatto) 25.sp else 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (temaScuroHeader) Color(0xFF4DA3FF) else Color(0xFF1565C0)
                    )
                }

            }
        }
    }
}


@Composable
fun ClientInfoCard(
    testo: String,
    compatto: Boolean,
    mostraToggleUser: Boolean = false,
    userAbilitato: Boolean = true,
    toggleUserInCorso: Boolean = false,
    onToggleUser: (() -> Unit)? = null,
    onHeightIncrease: ((Int) -> Unit)? = null
) {
    val righe = testo
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val intestazione = righe.firstOrNull().orEmpty()

    val nome = intestazione
        .substringBeforeLast("—", intestazione)
        .trim()

    val stato = intestazione
        .substringAfterLast("—", "")
        .trim()

    val canale = righe
        .firstOrNull { it.startsWith("Canale:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()

    val provider = righe
        .firstOrNull { it.startsWith("Provider:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()

    val reader = righe
        .firstOrNull { it.startsWith("Reader:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()

    val rigaEcm = righe
        .firstOrNull {
            it.startsWith("CAID ", ignoreCase = true) ||
                    it.startsWith("ECM ", ignoreCase = true)
        }
        .orEmpty()

    val caid = rigaEcm
        .split("•")
        .map { it.trim() }
        .firstOrNull { it.startsWith("CAID ", ignoreCase = true) }
        ?.substringAfter(" ")
        ?.trim()

    val ecm = rigaEcm
        .split("•")
        .map { it.trim() }
        .firstOrNull { it.startsWith("ECM ", ignoreCase = true) }
        ?.substringAfter(" ")
        ?.trim()

    val temaScuroClient =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    var altezzaPrecedenteCard by remember {
        mutableIntStateOf(0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                val precedente = altezzaPrecedenteCard

                if (
                    compatto &&
                    precedente > 0 &&
                    size.height > precedente
                ) {
                    onHeightIncrease?.invoke(
                        size.height - precedente
                    )
                }

                altezzaPrecedenteCard = size.height
            },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 18.dp else 22.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (temaScuroClient) 1.dp else 1.1.dp,
            Color(0xFFFFA726).copy(
                alpha = if (temaScuroClient) 0.30f else 0.36f
            )
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                if (temaScuroClient) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                } else {
                    Color.White
                }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (temaScuroClient) 2.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compatto) 10.dp else 13.dp,
                vertical = if (compatto) 6.dp else 9.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = Color(0xFFFFA726).copy(
                        alpha = if (temaScuroClient) 0.15f else 0.10f
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier
                            .padding(if (compatto) 5.dp else 7.dp)
                            .size(if (compatto) 17.dp else 20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = nome.ifBlank { "Client" },
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compatto) 14.sp else 16.sp,
                        maxLines = 1
                    )

                    if (mostraToggleUser && !canale.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = canale,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = if (compatto) 10.sp else 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                BadgeStatoDashboard(
                    stato = stato,
                    compatto = compatto
                )

                if (mostraToggleUser) {
                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = { onToggleUser?.invoke() },
                        enabled = !toggleUserInCorso,
                        modifier = Modifier.size(
                            if (compatto) 30.dp else 34.dp
                        )
                    ) {
                        if (toggleUserInCorso) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(
                                    if (compatto) 16.dp else 18.dp
                                ),
                                strokeWidth = 2.dp,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Abilita/disabilita user",
                                tint =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(
                                    if (compatto) 17.dp else 19.dp
                                )
                            )
                        }
                    }
                }
            }

            val haDettagli =
                (!mostraToggleUser && !canale.isNullOrBlank()) ||
                !provider.isNullOrBlank() ||
                !caid.isNullOrBlank() ||
                !ecm.isNullOrBlank() ||
                !reader.isNullOrBlank()

            if (haDettagli) {
                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.60f
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (haDettagli) {
                if (compatto) {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (!mostraToggleUser && !canale.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "Canale",
                                valore = canale,
                                compatto = compatto
                            )
                        }

                        if (!caid.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "CAID",
                                valore = caid,
                                compatto = compatto
                            )
                        }

                        if (!provider.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "Provider",
                                valore = provider,
                                compatto = compatto
                            )
                        }

                        if (!ecm.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "ECM",
                                valore = ecm,
                                compatto = compatto
                            )
                        }

                        if (!reader.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "Reader",
                                valore = reader,
                                compatto = compatto
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        if (!mostraToggleUser && !canale.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "Canale",
                                valore = canale,
                                compatto = compatto
                            )
                        }

                        if (!caid.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "CAID",
                                valore = caid,
                                compatto = compatto
                            )
                        }

                        if (!provider.isNullOrBlank()) {
                            MiniDatoClient(
                                etichetta = "Provider",
                                valore = provider,
                                compatto = compatto
                            )
                        }
                    }

                    if (!ecm.isNullOrBlank() || !reader.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            if (!ecm.isNullOrBlank()) {
                                MiniDatoClient(
                                    etichetta = "ECM",
                                    valore = ecm,
                                    compatto = compatto
                                )
                            }

                            if (!reader.isNullOrBlank()) {
                                MiniDatoClient(
                                    etichetta = "Reader",
                                    valore = reader,
                                    compatto = compatto
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DashboardCard(
    titolo: String,
    compatto: Boolean,
    valore: String,
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    coloreAccento: Color,
    aperta: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    didascalia: String? = null
) {
    val temaScuroCard =
        com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current

    Card(
        modifier = modifier
            .heightIn(min = if (compatto) 104.dp else 118.dp),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 18.dp else 22.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (temaScuroCard) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compatto) 11.dp else 14.dp,
                    vertical = if (compatto) 10.dp else 13.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = coloreAccento.copy(
                        alpha = if (aperta) 0.18f else 0.10f
                    )
                ) {
                    Icon(
                        imageVector = icona,
                        contentDescription = null,
                        tint = coloreAccento,
                        modifier = Modifier
                            .padding(if (compatto) 6.dp else 7.dp)
                            .size(if (compatto) 18.dp else 20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = titolo,
                    modifier = Modifier.weight(1f),
                    fontSize = if (compatto) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Icon(
                    imageVector =
                        if (aperta)
                            Icons.Default.ExpandLess
                        else
                            Icons.Default.ExpandMore,
                    contentDescription =
                        if (aperta)
                            "Nascondi $titolo"
                        else
                            "Mostra $titolo",
                    tint =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (!didascalia.isNullOrBlank()) 4.dp else 8.dp
                )
            )

            Text(
                text = valore.ifBlank { "0" },
                modifier = if (BuildConfig.TABLET_MODE) {
                    Modifier.padding(start = 6.dp)
                } else {
                    Modifier
                },
                fontSize = if (compatto) 27.sp else 31.sp,
                lineHeight = if (compatto) 29.sp else 33.sp,
                fontWeight = FontWeight.ExtraBold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            if (!didascalia.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = didascalia,
                    fontSize = if (compatto) 9.sp else 11.sp,
                    lineHeight = if (compatto) 10.sp else 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
