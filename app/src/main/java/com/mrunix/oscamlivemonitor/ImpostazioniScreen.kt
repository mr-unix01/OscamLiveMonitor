package com.mrunix.oscamlivemonitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val contesto = androidx.compose.ui.platform.LocalContext.current

    val preferenzeFile = androidx.compose.runtime.remember(contesto) {
        contesto.getSharedPreferences(
            "oscam_monitor",
            android.content.Context.MODE_PRIVATE
        )
    }

    val mostraFileNascosti = androidx.compose.runtime.remember(preferenzeFile) {
        androidx.compose.runtime.mutableStateOf(
            preferenzeFile.getBoolean("file_mostra_nascosti", true)
        )
    }

    val ricordaUltimaCartella = androidx.compose.runtime.remember(preferenzeFile) {
        androidx.compose.runtime.mutableStateOf(
            preferenzeFile.getBoolean("file_ricorda_cartella", false)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro"
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Preferenze generali",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aspetto",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        val temi = listOf(
            "system" to "Sistema",
            "light" to "Chiaro",
            "dark" to "Scuro"
        )

        temi.forEach { (valore, etichetta) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onTemaAppChange(valore)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = temaApp == valore,
                    onClick = {
                        onTemaAppChange(valore)
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = etichetta,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Lingua",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "La scelta della lingua sarà disponibile in un prossimo aggiornamento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        listOf("Sistema", "Italiano", "English").forEachIndexed { indice, nome ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = indice == 0,
                    onClick = null,
                    enabled = false
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = nome,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Schermo",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Schermo sempre acceso",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Switch(
                checked = schermoSempreAcceso,
                onCheckedChange = onSchermoSempreAccesoChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "File",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mostra file nascosti",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Switch(
                checked = mostraFileNascosti.value,
                onCheckedChange = { attivo ->
                    mostraFileNascosti.value = attivo
                    preferenzeFile.edit()
                        .putBoolean("file_mostra_nascosti", attivo)
                        .apply()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ricorda ultima cartella",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Switch(
                checked = ricordaUltimaCartella.value,
                onCheckedChange = { attivo ->
                    ricordaUltimaCartella.value = attivo
                    preferenzeFile.edit()
                        .putBoolean("file_ricorda_cartella", attivo)
                        .apply()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aggiornamento automatico",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            Switch(
                checked = aggiornamentoAutomaticoDashboard,
                onCheckedChange =
                    onAggiornamentoAutomaticoDashboardChange
            )
        }

        if (aggiornamentoAutomaticoDashboard) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Intervallo di aggiornamento",
                style = MaterialTheme.typography.titleSmall
            )

            listOf(5, 10, 15, 30, 60).forEach { secondi ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntervalloDashboardSecondiChange(secondi)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected =
                            intervalloDashboardSecondi == secondi,
                        onClick = {
                            onIntervalloDashboardSecondiChange(secondi)
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "$secondi secondi",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
