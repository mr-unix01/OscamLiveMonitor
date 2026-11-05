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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ImpostazioniScreen(
    onClose: () -> Unit,
    temaApp: String,
    onTemaAppChange: (String) -> Unit,
    linguaApp: String,
    onLinguaAppChange: (String) -> Unit,
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
    var dialogoLingua by remember { mutableStateOf(false) }
    var dialogoIntervallo by remember { mutableStateOf(false) }

    val descrizioneTema = when (temaApp) {
        "light" -> stringResource(R.string.settings_light)
        "dark" -> stringResource(R.string.settings_dark)
        else -> stringResource(R.string.settings_system)
    }

    val descrizioneLingua = when (linguaApp) {
        AppLanguage.ITALIAN -> "Italiano"
        AppLanguage.ENGLISH -> "English"
        else -> stringResource(R.string.settings_system)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        }

        SezioneImpostazioni(stringResource(R.string.settings_general)) {
            VoceScelta(
                Icons.Default.Palette,
                stringResource(R.string.settings_appearance),
                descrizioneTema
            ) { dialogoTema = true }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            VoceScelta(
                Icons.Default.Language,
                stringResource(R.string.settings_language),
                descrizioneLingua
            ) { dialogoLingua = true }
        }

        SezioneImpostazioni(stringResource(R.string.settings_screen)) {
            VoceInterruttore(
                Icons.Default.PhoneAndroid,
                stringResource(R.string.settings_keep_screen_on),
                stringResource(R.string.settings_keep_screen_on_desc),
                schermoSempreAcceso,
                onSchermoSempreAccesoChange
            )
        }

        SezioneImpostazioni(stringResource(R.string.settings_files)) {
            VoceInterruttore(
                Icons.Default.Visibility,
                stringResource(R.string.settings_show_hidden),
                stringResource(R.string.settings_show_hidden_desc),
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
                stringResource(R.string.settings_remember_folder),
                stringResource(R.string.settings_remember_folder_desc),
                ricordaUltimaCartella
            ) { attivo ->
                ricordaUltimaCartella = attivo
                preferenzeFile.edit()
                    .putBoolean("file_ricorda_cartella", attivo)
                    .apply()
            }
        }

        SezioneImpostazioni(stringResource(R.string.settings_dashboard)) {
            VoceInterruttore(
                Icons.Default.Refresh,
                stringResource(R.string.settings_auto_refresh),
                stringResource(R.string.settings_auto_refresh_desc),
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
                    stringResource(R.string.settings_refresh_interval),
                    stringResource(R.string.settings_seconds, intervalloDashboardSecondi)
                ) { dialogoIntervallo = true }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (dialogoTema) {
        DialogoSceltaImpostazioni(
            titolo = stringResource(R.string.settings_appearance),
            opzioni = listOf(
                "system" to stringResource(R.string.settings_system),
                "light" to stringResource(R.string.settings_light),
                "dark" to stringResource(R.string.settings_dark)
            ),
            selezionato = temaApp,
            onScegli = {
                onTemaAppChange(it)
                dialogoTema = false
            },
            onChiudi = { dialogoTema = false }
        )
    }

    if (dialogoLingua) {
        DialogoSceltaImpostazioni(
            titolo = stringResource(R.string.settings_language),
            opzioni = listOf(
                AppLanguage.SYSTEM to
                    stringResource(R.string.settings_system),
                AppLanguage.ITALIAN to "Italiano",
                AppLanguage.ENGLISH to "English"
            ),
            selezionato = linguaApp,
            onScegli = { scelta ->
                dialogoLingua = false
                onLinguaAppChange(scelta)
            },
            onChiudi = { dialogoLingua = false }
        )
    }

    if (dialogoIntervallo) {
        DialogoSceltaImpostazioni(
            titolo = stringResource(R.string.settings_refresh_interval),
            opzioni = listOf(5, 10, 15, 30, 60).map {
                it.toString() to stringResource(R.string.settings_seconds, it)
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
        text = titolo.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 8.dp)
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = contenuto)
    }

    Spacer(Modifier.height(6.dp))
}

@Composable
private fun IconaImpostazione(icona: ImageVector) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun TestoImpostazione(
    titolo: String,
    descrizione: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = titolo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = descrizione,
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconaImpostazione(icona)
        Spacer(Modifier.width(12.dp))
        TestoImpostazione(
            titolo,
            descrizione,
            Modifier.weight(1f)
        )

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
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconaImpostazione(icona)
        Spacer(Modifier.width(12.dp))
        TestoImpostazione(
            titolo,
            descrizione,
            Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = attivo,
            onCheckedChange = onChange,
            modifier = Modifier.height(32.dp)
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
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
