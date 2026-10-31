package com.mrunix.oscamlivemonitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ImpostazioniScreen(
    onClose: () -> Unit,
    temaApp: String,
    onTemaAppChange: (String) -> Unit,
    schermoSempreAcceso: Boolean,
    onSchermoSempreAccesoChange: (Boolean) -> Unit,
    aggiornamentoAutomaticoDashboard: Boolean,
    onAggiornamentoAutomaticoDashboardChange: (Boolean) -> Unit,
    intervalloDashboardSecondi: Int,
    onIntervalloDashboardSecondiChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val contesto = LocalContext.current
    val preferenzeFile = remember(contesto) {
        contesto.getSharedPreferences(
            "oscam_monitor",
            android.content.Context.MODE_PRIVATE
        )
    }

    var mostraFileNascosti by remember(preferenzeFile) {
        mutableStateOf(preferenzeFile.getBoolean("file_mostra_nascosti", true))
    }
    var ricordaUltimaCartella by remember(preferenzeFile) {
        mutableStateOf(preferenzeFile.getBoolean("file_ricorda_cartella", false))
    }
    var dialogoTema by remember { mutableStateOf(false) }
    var dialogoIntervallo by remember { mutableStateOf(false) }

    val descrizioneTema = when (temaApp) {
        "light" -> "Chiaro"
        "dark" -> "Scuro"
        else -> "Sistema"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
            }
            Spacer(Modifier.width(8.dp))
            Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)
        }

        SezioneImpostazioni("Generali") {
            VoceScelta(
                Icons.Default.Palette,
                "Aspetto",
                descrizioneTema
            ) { dialogoTema = true }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            VoceScelta(
                Icons.Default.Language,
                "Lingua",
                "Traduzione non ancora disponibile",
                abilitata = false
            ) {}
        }

        SezioneImpostazioni("Schermo") {
            VoceInterruttore(
                Icons.Default.PhoneAndroid,
                "Schermo sempre acceso",
                "Impedisce lo spegnimento durante l'utilizzo",
                schermoSempreAcceso,
                onSchermoSempreAccesoChange
            )
        }

        SezioneImpostazioni("File") {
            VoceInterruttore(
                Icons.Default.Visibility,
                "Mostra file nascosti",
                "Visualizza anche i file nascosti",
                mostraFileNascosti
            ) { attivo ->
                mostraFileNascosti = attivo
                preferenzeFile.edit()
                    .putBoolean("file_mostra_nascosti", attivo)
                    .apply()
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            VoceInterruttore(
                Icons.Default.Folder,
                "Ricorda ultima cartella",
                "Ripristina l'ultima cartella utilizzata",
                ricordaUltimaCartella
            ) { attivo ->
                ricordaUltimaCartella = attivo
                preferenzeFile.edit()
                    .putBoolean("file_ricorda_cartella", attivo)
                    .apply()
            }
        }

        SezioneImpostazioni("Dashboard") {
            VoceInterruttore(
                Icons.Default.Refresh,
                "Aggiornamento automatico",
                "Aggiorna periodicamente i dati della Dashboard",
                aggiornamentoAutomaticoDashboard,
                onAggiornamentoAutomaticoDashboardChange
            )

            if (aggiornamentoAutomaticoDashboard) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                VoceScelta(
                    Icons.Default.Schedule,
                    "Intervallo di aggiornamento",
                    "$intervalloDashboardSecondi secondi"
                ) { dialogoIntervallo = true }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (dialogoTema) {
        DialogoSceltaImpostazioni(
            titolo = "Aspetto",
            opzioni = listOf(
                "system" to "Sistema",
                "light" to "Chiaro",
                "dark" to "Scuro"
            ),
            selezionato = temaApp,
            onScegli = {
                onTemaAppChange(it)
                dialogoTema = false
            },
            onChiudi = { dialogoTema = false }
        )
    }

    if (dialogoIntervallo) {
        DialogoSceltaImpostazioni(
            titolo = "Intervallo di aggiornamento",
            opzioni = listOf(5, 10, 15, 30, 60).map {
                it.toString() to "$it secondi"
            },
            selezionato = intervalloDashboardSecondi.toString(),
            onScegli = {
                onIntervalloDashboardSecondiChange(it.toInt())
                dialogoIntervallo = false
            },
            onChiudi = { dialogoIntervallo = false }
        )
    }
}

@Composable
private fun SezioneImpostazioni(
    titolo: String,
    contenuto: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = titolo,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 8.dp)
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = contenuto)
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
private fun IconaImpostazione(icona: ImageVector) {
    Icon(
        imageVector = icona,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun TestoImpostazione(
    titolo: String,
    descrizione: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(titolo, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(3.dp))
        Text(
            descrizione,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoceScelta(
    icona: ImageVector,
    titolo: String,
    descrizione: String,
    abilitata: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = abilitata, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconaImpostazione(icona)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f)) {
            TestoImpostazione(titolo, descrizione)
        }
        if (abilitata) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VoceInterruttore(
    icona: ImageVector,
    titolo: String,
    descrizione: String,
    attivo: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!attivo) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconaImpostazione(icona)
        Spacer(Modifier.width(16.dp))
        TestoImpostazione(
            titolo,
            descrizione,
            Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = attivo,
            onCheckedChange = onChange
        )
    }
}

@Composable
private fun DialogoSceltaImpostazioni(
    titolo: String,
    opzioni: List<Pair<String, String>>,
    selezionato: String,
    onScegli: (String) -> Unit,
    onChiudi: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onChiudi,
        title = { Text(titolo) },
        text = {
            Column {
                opzioni.forEach { (valore, etichetta) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onScegli(valore) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = valore == selezionato,
                            onClick = { onScegli(valore) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(etichetta)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onChiudi) {
                Text("Annulla")
            }
        }
    )
}
