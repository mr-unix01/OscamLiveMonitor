package com.mrunix.oscamonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mrunix.oscamonitor.ui.theme.OscamMonitorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OscamMonitorTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Greeting(
                        name = "Oscam Monitor",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val temaScuro = androidx.compose.foundation.isSystemInDarkTheme()
    val schermoCompatto = false

    val preferences = remember {
        context.getSharedPreferences(
            "oscam_monitor",
            android.content.Context.MODE_PRIVATE
        )
    }

    var host by remember {
        mutableStateOf(
            preferences.getString("host", "") ?: ""
        )
    }

    var porta by remember {
        mutableStateOf(
            preferences.getString("porta", "") ?: ""
        )
    }

    var username by remember {
        mutableStateOf(
            preferences.getString("username", "") ?: ""
        )
    }

    var password by remember {
        mutableStateOf(
            preferences.getString("password", "") ?: ""
        )
    }

    var stato by remember { mutableStateOf("") }
    var ultimoAggiornamento by remember { mutableStateOf("") }
    var erroriRefreshConsecutivi by remember { mutableStateOf(0) }

    var versioneOscam by remember { mutableStateOf("") }
    var uptime by remember { mutableStateOf("") }
    var cpuOscam by remember { mutableStateOf("") }
    var ramOscam by remember { mutableStateOf("") }

    var servers by remember { mutableStateOf("") }
    var readers by remember { mutableStateOf("") }
    var proxies by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf("") }

    var elencoServers by remember {
        mutableStateOf(emptyList<String>())
    }

    var elencoReaders by remember {
        mutableStateOf(emptyList<String>())
    }

    var elencoProxies by remember {
        mutableStateOf(emptyList<String>())
    }

    var elencoClients by remember {
        mutableStateOf(emptyList<String>())
    }

    var mostraConnessione by remember { mutableStateOf(true) }
    var mostraWebIf by remember { mutableStateOf(false) }

    var serverSalvati by remember {
        mutableStateOf(caricaServerSalvati(preferences))
    }

    var mostraAggiungiServer by remember { mutableStateOf(false) }

    var serverDaModificare by remember { mutableStateOf<OscamServer?>(null) }
    var modificaNomeServer by remember { mutableStateOf("") }
    var modificaHostServer by remember { mutableStateOf("") }
    var modificaPortaServer by remember { mutableStateOf("") }
    var modificaUsernameServer by remember { mutableStateOf("") }
    var modificaPasswordServer by remember { mutableStateOf("") }
    var mostraPasswordModifica by remember { mutableStateOf(false) }

    var serverDaEliminare by remember { mutableStateOf<OscamServer?>(null) }

    var nuovoNomeServer by remember { mutableStateOf("") }
    var nuovoHostServer by remember { mutableStateOf("") }
    var nuovaPortaServer by remember { mutableStateOf("") }
    var nuovoUsernameServer by remember { mutableStateOf("") }
    var nuovaPasswordServer by remember { mutableStateOf("") }
    var mostraPasswordNuovo by remember { mutableStateOf(false) }
    var mostraPasswordConnessione by remember { mutableStateOf(false) }

    var mostraServers by remember { mutableStateOf(true) }
    var mostraReaders by remember { mutableStateOf(true) }
    var mostraProxies by remember { mutableStateOf(true) }
    var mostraClients by remember { mutableStateOf(true) }

    var mostraSpiegazionePermesso by remember {
        mutableStateOf(false)
    }

    var mostraConfermaRiavvio by remember {
        mutableStateOf(false)
    }

    var riavvioInCorso by remember {
        mutableStateOf(false)
    }

    var permessoReteConcesso by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 37 ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_LOCAL_NETWORK
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val api = remember { OscamApi() }
    val coroutineScope = rememberCoroutineScope()

    suspend fun aggiornaDashboardDaOscam(mostraErroreSubito: Boolean = true): Boolean {
        val risultato = withContext(Dispatchers.IO) {
            api.scaricaStatusJson(
                host = host.trim(),
                porta = porta.trim(),
                username = username,
                password = password
            )
        }

        if (risultato.startsWith("ERRORE:")) {
            if (mostraErroreSubito) {
                stato = risultato
            }
            return false
        }

        versioneOscam =
            api.estraiVersioneJson(risultato)

        uptime =
            api.estraiRuntimeJson(risultato)

        cpuOscam =
            api.estraiCpuJson(risultato)

        ramOscam =
            api.estraiMemoriaUsataJson(risultato)

        servers =
            api.estraiServersJson(risultato)

        readers =
            api.estraiReadersJson(risultato)

        proxies =
            api.estraiProxiesJson(risultato)

        clients =
            api.estraiClientsJson(risultato)

        val elencoServersOriginale =
            api.estraiElencoServersJson(risultato)

        elencoServers =
            elencoServersOriginale.mapIndexed { indice, server ->
                val statoServer =
                    server
                        .substringAfterLast("—", "")
                        .trim()
                        .ifBlank {
                            when {
                                server.contains(
                                    "NOT OK",
                                    ignoreCase = true
                                ) ->
                                    "NOT OK"

                                server.contains(
                                    "OFFLINE",
                                    ignoreCase = true
                                ) ->
                                    "OFFLINE"

                                server.contains(
                                    "ERROR",
                                    ignoreCase = true
                                ) ->
                                    "ERROR"

                                server.contains(
                                    "OK",
                                    ignoreCase = true
                                ) ->
                                    "OK"

                                else ->
                                    "Stato sconosciuto"
                            }
                        }

                when (indice) {
                    0 ->
                        "OSCam Core — $statoServer"

                    1 ->
                        "WebIF — $statoServer"

                    else ->
                        server
                }
            }

        elencoReaders =
            api.estraiElencoReadersJson(risultato)

        elencoProxies =
            api.estraiElencoProxiesJson(risultato)

        elencoClients =
            api.estraiElencoClientsJson(risultato)

        ultimoAggiornamento =
            java.text.SimpleDateFormat(
                "HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

        erroriRefreshConsecutivi = 0
        stato = "Connesso"
        return true
    }

    val richiestaPermessoRete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concesso ->
        permessoReteConcesso = concesso

        stato = if (concesso) {
            "Accesso alla rete locale consentito"
        } else {
            "Permesso rete locale necessario per collegarsi a OSCam"
        }
    }

    LaunchedEffect(Unit) {
        if (
            serverSalvati.isNotEmpty() &&
            preferences.getString("server_salvati", null) == null
        ) {
            salvaServerSalvati(
                preferences = preferences,
                server = serverSalvati
            )
        }

        if (
            Build.VERSION.SDK_INT >= 37 &&
            !permessoReteConcesso
        ) {
            mostraSpiegazionePermesso = true
        }
    }

    LaunchedEffect(
        mostraConnessione,
        mostraWebIf,
        host,
        porta,
        username,
        password
    ) {
        if (
            !mostraConnessione &&
            !mostraWebIf
        ) {
            while (true) {
                kotlinx.coroutines.delay(5000)

                val aggiornato =
                    aggiornaDashboardDaOscam(
                        mostraErroreSubito = false
                    )

                if (aggiornato) {
                    erroriRefreshConsecutivi = 0
                } else {
                    erroriRefreshConsecutivi += 1

                    if (erroriRefreshConsecutivi >= 3) {
                        stato = "Connessione persa"
                    }
                }
            }
        }
    }

    if (mostraConfermaRiavvio) {
        val serverAttivo =
            serverSalvati.firstOrNull { server ->
                server.host.trim() == host.trim() &&
                        server.porta.trim() == porta.trim()
            }

        val nomeServerAttivo =
            serverAttivo?.nome
                ?: host.trim()
                    .ifBlank { "server attivo" }

        AlertDialog(
            onDismissRequest = {
                if (!riavvioInCorso) {
                    mostraConfermaRiavvio = false
                }
            },
            title = {
                Text(
                    "Riavviare $nomeServerAttivo?"
                )
            },
            text = {
                Text(
                    "Verrà riavviato SOLO il server OSCam attivo:\n\n" +
                            "$nomeServerAttivo\n" +
                            "${host.trim()}:${porta.trim()}\n\n" +
                            "Gli altri server salvati non verranno toccati. " +
                            "I client di questo server verranno temporaneamente " +
                            "disconnessi e OSCam Pulse proverà a riconnettersi " +
                            "automaticamente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostraConfermaRiavvio = false

                        if (host.isBlank() || porta.isBlank()) {
                            stato =
                                "Inserisci prima Host/IP e Porta"
                            return@TextButton
                        }

                        riavvioInCorso = true
                        stato = "Riavvio OSCam..."

                        coroutineScope.launch {
                            val risultatoRiavvio =
                                withContext(Dispatchers.IO) {
                                    api.riavviaOscam(
                                        host = host.trim(),
                                        porta = porta.trim(),
                                        username = username,
                                        password = password
                                    )
                                }

                            if (
                                risultatoRiavvio.startsWith(
                                    "ERRORE:",
                                    ignoreCase = true
                                )
                            ) {
                                riavvioInCorso = false
                                stato = risultatoRiavvio
                                return@launch
                            }

                            kotlinx.coroutines.delay(3000)

                            var riconnesso = false

                            for (tentativo in 1..10) {
                                if (aggiornaDashboardDaOscam()) {
                                    riconnesso = true
                                    break
                                }

                                kotlinx.coroutines.delay(2000)
                            }

                            riavvioInCorso = false

                            if (riconnesso) {
                                mostraConnessione = false
                                stato = "Connesso"
                            } else {
                                stato =
                                    "OSCam non ancora raggiungibile dopo il riavvio"
                            }
                        }
                    }
                ) {
                    Text("Riavvia $nomeServerAttivo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostraConfermaRiavvio = false
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    if (mostraSpiegazionePermesso) {
        AlertDialog(
            onDismissRequest = {
                mostraSpiegazionePermesso = false
            },
            title = {
                Text("Accesso alla rete locale")
            },
            text = {
                Text(
                    "OSCam Pulse deve accedere alla rete locale " +
                            "per collegarsi al WebIF di OSCam."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostraSpiegazionePermesso = false

                        if (Build.VERSION.SDK_INT >= 37) {
                            richiestaPermessoRete.launch(
                                Manifest.permission.ACCESS_LOCAL_NETWORK
                            )
                        }
                    }
                ) {
                    Text("Continua")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostraSpiegazionePermesso = false
                        stato =
                            "Permesso rete locale necessario per OSCam"
                    }
                ) {
                    Text("Non ora")
                }
            }
        )
    }

    if (mostraAggiungiServer) {
        AlertDialog(
            onDismissRequest = {
                mostraAggiungiServer = false
            },
            title = {
                Text("Aggiungi server OSCam")
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = nuovoNomeServer,
                        onValueChange = { nuovoNomeServer = it },
                        label = { Text("Nome server") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = nuovoHostServer,
                        onValueChange = { nuovoHostServer = it },
                        label = { Text("Host/IP") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = nuovaPortaServer,
                        onValueChange = { nuovaPortaServer = it },
                        label = { Text("Porta") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = nuovoUsernameServer,
                        onValueChange = { nuovoUsernameServer = it },
                        label = { Text("Username") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = nuovaPasswordServer,
                        onValueChange = { nuovaPasswordServer = it },
                        label = { Text("Password") },
                        visualTransformation =
                            if (mostraPasswordNuovo) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    mostraPasswordNuovo =
                                        !mostraPasswordNuovo
                                }
                            ) {
                                Text("👁")
                            }
                        },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nomePulito = nuovoNomeServer.trim()
                        val hostPulito = nuovoHostServer.trim()
                        val portaPulita = nuovaPortaServer.trim()

                        if (
                            nomePulito.isBlank() ||
                            hostPulito.isBlank() ||
                            portaPulita.toIntOrNull() == null
                        ) {
                            stato = "Inserisci nome, Host/IP e una porta valida"
                        } else {
                            val nuovoServer = OscamServer(
                                nome = nomePulito,
                                host = hostPulito,
                                porta = portaPulita,
                                username = nuovoUsernameServer,
                                password = nuovaPasswordServer
                            )

                            serverSalvati = serverSalvati + nuovoServer

                            salvaServerSalvati(
                                preferences = preferences,
                                server = serverSalvati
                            )

                            host = nuovoServer.host
                            porta = nuovoServer.porta
                            username = nuovoServer.username
                            password = nuovoServer.password

                            preferences.edit()
                                .putString("nome_server", nuovoServer.nome)
                                .putString("host", nuovoServer.host)
                                .putString("porta", nuovoServer.porta)
                                .putString("username", nuovoServer.username)
                                .putString("password", nuovoServer.password)
                                .apply()

                            nuovoNomeServer = ""
                            nuovoHostServer = ""
                            nuovaPortaServer = ""
                            nuovoUsernameServer = ""
                            nuovaPasswordServer = ""
                            mostraAggiungiServer = false
                            stato = "Server ${nuovoServer.nome} aggiunto"
                        }
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostraAggiungiServer = false
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    serverDaModificare?.let { serverOriginale ->
        AlertDialog(
            onDismissRequest = {
                serverDaModificare = null
            },
            title = {
                Text("Modifica server OSCam")
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = modificaNomeServer,
                        onValueChange = {
                            modificaNomeServer = it
                        },
                        label = {
                            Text("Nome server")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = modificaHostServer,
                        onValueChange = {
                            modificaHostServer = it
                        },
                        label = {
                            Text("Host/IP")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = modificaPortaServer,
                        onValueChange = {
                            modificaPortaServer = it
                        },
                        label = {
                            Text("Porta")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = modificaUsernameServer,
                        onValueChange = {
                            modificaUsernameServer = it
                        },
                        label = {
                            Text("Username")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = modificaPasswordServer,
                        onValueChange = {
                            modificaPasswordServer = it
                        },
                        label = {
                            Text("Password")
                        },
                        visualTransformation =
                            if (mostraPasswordModifica) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    mostraPasswordModifica =
                                        !mostraPasswordModifica
                                }
                            ) {
                                Text("👁")
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            serverDaEliminare = serverOriginale
                            serverDaModificare = null
                        }
                    ) {
                        Text("Elimina server")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nomePulito =
                            modificaNomeServer.trim()

                        val hostPulito =
                            modificaHostServer.trim()

                        val portaPulita =
                            modificaPortaServer.trim()

                        if (
                            nomePulito.isBlank() ||
                            hostPulito.isBlank() ||
                            portaPulita.toIntOrNull() == null
                        ) {
                            stato =
                                "Inserisci nome, Host/IP e una porta valida"
                        } else {
                            val serverAggiornato =
                                serverOriginale.copy(
                                    nome = nomePulito,
                                    host = hostPulito,
                                    porta = portaPulita,
                                    username = modificaUsernameServer,
                                    password = modificaPasswordServer
                                )

                            val indice =
                                serverSalvati.indexOf(
                                    serverOriginale
                                )

                            if (indice >= 0) {
                                val nuovaLista =
                                    serverSalvati.toMutableList()

                                nuovaLista[indice] =
                                    serverAggiornato

                                serverSalvati =
                                    nuovaLista

                                salvaServerSalvati(
                                    preferences = preferences,
                                    server = serverSalvati
                                )

                                val eraSelezionato =
                                    host.trim() == serverOriginale.host &&
                                            porta.trim() == serverOriginale.porta

                                if (eraSelezionato) {
                                    host =
                                        serverAggiornato.host

                                    porta =
                                        serverAggiornato.porta

                                    username =
                                        serverAggiornato.username

                                    password =
                                        serverAggiornato.password

                                    preferences.edit()
                                        .putString(
                                            "nome_server",
                                            serverAggiornato.nome
                                        )
                                        .putString(
                                            "host",
                                            serverAggiornato.host
                                        )
                                        .putString(
                                            "porta",
                                            serverAggiornato.porta
                                        )
                                        .putString(
                                            "username",
                                            serverAggiornato.username
                                        )
                                        .putString(
                                            "password",
                                            serverAggiornato.password
                                        )
                                        .apply()
                                }

                                stato =
                                    "Server ${serverAggiornato.nome} modificato"
                            }

                            serverDaModificare = null
                        }
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        serverDaModificare = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    serverDaEliminare?.let { serverOriginale ->
        AlertDialog(
            onDismissRequest = {
                serverDaEliminare = null
            },
            title = {
                Text("Eliminare server?")
            },
            text = {
                Text(
                    "Vuoi eliminare definitivamente " +
                            "\"${serverOriginale.nome}\"?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val indice =
                            serverSalvati.indexOf(
                                serverOriginale
                            )

                        if (indice >= 0) {
                            val eraSelezionato =
                                host.trim() == serverOriginale.host &&
                                        porta.trim() == serverOriginale.porta

                            val nuovaLista =
                                serverSalvati.toMutableList()

                            nuovaLista.removeAt(indice)

                            serverSalvati =
                                nuovaLista

                            salvaServerSalvati(
                                preferences = preferences,
                                server = serverSalvati
                            )

                            if (eraSelezionato) {
                                val nuovoSelezionato =
                                    serverSalvati.firstOrNull()

                                if (nuovoSelezionato != null) {
                                    host =
                                        nuovoSelezionato.host

                                    porta =
                                        nuovoSelezionato.porta

                                    username =
                                        nuovoSelezionato.username

                                    password =
                                        nuovoSelezionato.password

                                    preferences.edit()
                                        .putString(
                                            "nome_server",
                                            nuovoSelezionato.nome
                                        )
                                        .putString(
                                            "host",
                                            nuovoSelezionato.host
                                        )
                                        .putString(
                                            "porta",
                                            nuovoSelezionato.porta
                                        )
                                        .putString(
                                            "username",
                                            nuovoSelezionato.username
                                        )
                                        .putString(
                                            "password",
                                            nuovoSelezionato.password
                                        )
                                        .apply()
                                } else {
                                    host = ""
                                    porta = ""
                                    username = ""
                                    password = ""

                                    preferences.edit()
                                        .remove("nome_server")
                                        .remove("host")
                                        .remove("porta")
                                        .remove("username")
                                        .remove("password")
                                        .apply()
                                }
                            }

                            stato =
                                "Server ${serverOriginale.nome} eliminato"
                        }

                        serverDaEliminare = null
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        serverDaEliminare = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    if (mostraWebIf) {
        WebIfScreen(
            host = host,
            porta = porta,
            username = username,
            password = password,
            onClose = {
                mostraWebIf = false
            },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
        ) {
            Text(
                text = "OSCam Pulse",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Text(
                text = "Real-time OSCam Dashboard",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(
                    if (schermoCompatto) 12.dp else 16.dp
                )
            )

            if (mostraConnessione) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Server OSCam",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF4CAF50)
                        ),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        onClick = {
                            nuovoNomeServer = ""
                            nuovoHostServer = ""
                            nuovaPortaServer = ""
                            nuovoUsernameServer = ""
                            nuovaPasswordServer = ""
                            mostraPasswordNuovo = false
                            mostraAggiungiServer = true
                        }
                    ) {
                        Text("＋ Aggiungi")
                    }
                }

                if (serverSalvati.isEmpty()) {
                    Text("Nessun server salvato")
                } else {
                    serverSalvati.forEach { server ->
                        val serverSelezionato =
                            host.trim() == server.host &&
                                    porta.trim() == server.porta

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = if (schermoCompatto) 3.dp else 5.dp
                                ),
                            shape =
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    if (schermoCompatto) 14.dp else 16.dp
                                ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (serverSelezionato) 2.dp else 1.dp,
                                color = if (serverSelezionato) {
                                    Color(0xFF4CAF50)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (serverSelezionato) {
                                    if (temaScuro) {
                                        Color(0xFF153A22)
                                    } else {
                                        Color(0xFFE3F4E7)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            onClick = {
                                host = server.host
                                porta = server.porta
                                username = server.username
                                password = server.password

                                preferences.edit()
                                    .putString("nome_server", server.nome)
                                    .putString("host", server.host)
                                    .putString("porta", server.porta)
                                    .putString("username", server.username)
                                    .putString("password", server.password)
                                    .apply()

                                stato = "Server ${server.nome} selezionato"
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        if (schermoCompatto) 9.dp else 12.dp
                                    ),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = server.nome,
                                        fontSize = if (schermoCompatto) 16.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (serverSelezionato) {
                                            if (temaScuro) {
                                                Color.White
                                            } else {
                                                Color(0xFF16351F)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "🌐 ${server.host}  •  ${server.porta}",
                                        fontSize = if (schermoCompatto) 12.sp else 13.sp,
                                        color = if (serverSelezionato) {
                                            if (temaScuro) {
                                                Color(0xFFD6EBDD)
                                            } else {
                                                Color(0xFF365D40)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                if (serverSelezionato) {
                                    Text(
                                        text = "● Attivo",
                                        color = Color(0xFF66BB6A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (schermoCompatto) 11.sp else 12.sp
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        serverDaModificare =
                                            server

                                        modificaNomeServer =
                                            server.nome

                                        modificaHostServer =
                                            server.host

                                        modificaPortaServer =
                                            server.porta

                                        modificaUsernameServer =
                                            server.username

                                        modificaPasswordServer =
                                            server.password

                                        mostraPasswordModifica =
                                            false
                                    }
                                ) {
                                    Text(
                                        text = "✎ Modifica",
                                        fontSize =
                                            if (schermoCompatto) 12.sp else 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Connessione",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host/IP") },
                    leadingIcon = { Text("🌐") },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(
                        if (schermoCompatto) 6.dp else 8.dp
                    )
                )

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = porta,
                    onValueChange = { porta = it },
                    label = { Text("Porta") },
                    leadingIcon = { Text("🔌") },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(
                        if (schermoCompatto) 6.dp else 8.dp
                    )
                )

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Text("👤") },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(
                        if (schermoCompatto) 6.dp else 8.dp
                    )
                )

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Text("🔒") },
                    visualTransformation =
                        if (mostraPasswordConnessione) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                mostraPasswordConnessione =
                                    !mostraPasswordConnessione
                            }
                        ) {
                            Text("👁")
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (schermoCompatto) 48.dp else 52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    onClick = {
                        if (!permessoReteConcesso) {
                            stato =
                                "Concedi prima l'accesso alla rete locale"
                            mostraSpiegazionePermesso = true
                            return@Button
                        }

                        if (host.isBlank() || porta.isBlank()) {
                            stato = "Inserisci Host/IP e Porta"
                            return@Button
                        }

                        preferences.edit()
                            .putString("host", host.trim())
                            .putString("porta", porta.trim())
                            .putString("username", username)
                            .putString("password", password)
                            .apply()

                        stato = "Connessione..."
                        versioneOscam = ""
                        uptime = ""
                        cpuOscam = ""
                        ramOscam = ""

                        servers = ""
                        readers = ""
                        proxies = ""
                        clients = ""

                        elencoServers = emptyList()
                        elencoReaders = emptyList()
                        elencoProxies = emptyList()
                        elencoClients = emptyList()

                        mostraServers = true
                        mostraReaders = true
                        mostraProxies = true
                        mostraClients = true

                        coroutineScope.launch {
                            if (aggiornaDashboardDaOscam()) {
                                mostraConnessione = false
                            }
                        }
                    }
                ) {
                    Text(
                        text = "●  Connetti",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (schermoCompatto) 46.dp else 50.dp),
                    enabled = !riavvioInCorso,
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            14.dp
                        ),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFFF7043)
                        ),
                    colors =
                        androidx.compose.material3.ButtonDefaults
                            .outlinedButtonColors(
                                contentColor = Color(0xFFFF7043)
                            ),
                    onClick = {
                        if (host.isBlank() || porta.isBlank()) {
                            stato =
                                "Inserisci prima Host/IP e Porta"
                        } else {
                            mostraConfermaRiavvio = true
                        }
                    }
                ) {
                    Text(
                        if (riavvioInCorso) {
                            "↻ Riavvio..."
                        } else {
                            "↻ Riavvia OSCam"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (stato.isNotBlank()) {
                val coloreStato = when {
                    stato.equals("Connesso", ignoreCase = true) ->
                        Color(0xFF66BB6A)

                    stato.contains("selezionato", ignoreCase = true) ->
                        Color(0xFF66BB6A)

                    stato.contains("Connessione", ignoreCase = true) ->
                        Color(0xFFFFB74D)

                    stato.startsWith("ERRORE:", ignoreCase = true) ->
                        Color(0xFFEF5350)

                    stato.contains("necessario", ignoreCase = true) ->
                        Color(0xFFEF5350)

                    else ->
                        MaterialTheme.colorScheme.primary
                }

                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = coloreStato.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        coloreStato.copy(alpha = 0.55f)
                    )
                ) {
                    Text(
                        text = "●  $stato",
                        color = coloreStato,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                    )
                }
            }

            if (!mostraConnessione) {
                val serverAttivoDashboard =
                    serverSalvati.firstOrNull { server ->
                        server.host.trim() == host.trim() &&
                                server.porta.trim() == porta.trim()
                    }

                val nomeServerDashboard =
                    serverAttivoDashboard?.nome
                        ?: preferences.getString("nome_server", null)
                            ?.takeIf { it.isNotBlank() }
                        ?: host.trim()

                if (host.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = nomeServerDashboard,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (porta.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${host.trim()}:${porta.trim()}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = {
                        mostraConnessione = true
                        stato = ""
                        versioneOscam = ""
                        uptime = ""
                        cpuOscam = ""
                        ramOscam = ""

                        servers = ""
                        readers = ""
                        proxies = ""
                        clients = ""

                        elencoServers = emptyList()
                        elencoReaders = emptyList()
                        elencoProxies = emptyList()
                        elencoClients = emptyList()

                        mostraServers = true
                        mostraReaders = true
                        mostraProxies = true
                        mostraClients = true
                    }
                ) {
                    Text("⚙  Impostazioni")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF29B6F6)
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF29B6F6)
                    ),
                    onClick = {
                        mostraWebIf = true
                    }
                ) {
                    Text("🌐  Apri WebIF")
                }
            }

            if (versioneOscam.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "OSCam $versioneOscam",
                    fontWeight = FontWeight.Bold
                )
            }

            if (uptime.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Uptime: $uptime")
            }

            if (cpuOscam.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "CPU OSCam: $cpuOscam")
            }

            if (ramOscam.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "RAM OSCam: $ramOscam")
            }

            if (mostraConnessione) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (temaScuro) {
                                R.drawable.mr_unix_footer
                            } else {
                                R.drawable.mr_unix_footer_light
                            }
                        ),
                        contentDescription = "mr-unix",
                        modifier = Modifier.height(72.dp)
                    )
                }
            } else if (ultimoAggiornamento.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF66BB6A),
                        modifier = Modifier
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))

                    Text(
                        text = "Aggiornato alle $ultimoAggiornamento",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (
                servers.isNotBlank() ||
                readers.isNotBlank() ||
                proxies.isNotBlank() ||
                clients.isNotBlank()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            if (schermoCompatto) 6.dp else 8.dp
                        )
                ) {
                    DashboardCard(
                        titolo = "Server",
                        compatto = schermoCompatto,
                        valore = servers,
                        icona = Icons.Default.Storage,
                        coloreAccento = Color(0xFF42A5F5),
                        aperta = mostraServers,
                        onClick = {
                            mostraServers = !mostraServers
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        titolo = "Readers",
                        compatto = schermoCompatto,
                        valore = readers,
                        icona = Icons.Default.CreditCard,
                        coloreAccento = Color(0xFF66BB6A),
                        aperta = mostraReaders,
                        onClick = {
                            mostraReaders = !mostraReaders
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            if (schermoCompatto) 6.dp else 8.dp
                        )
                ) {
                    DashboardCard(
                        titolo = "Proxies",
                        compatto = schermoCompatto,
                        valore = proxies,
                        icona = Icons.Default.SwapHoriz,
                        coloreAccento = Color(0xFFAB47BC),
                        aperta = mostraProxies,
                        onClick = {
                            mostraProxies = !mostraProxies
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        titolo = "Clients",
                        compatto = schermoCompatto,
                        valore = clients,
                        icona = Icons.Default.Devices,
                        coloreAccento = Color(0xFFFFA726),
                        aperta = mostraClients,
                        onClick = {
                            mostraClients = !mostraClients
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (mostraServers && elencoServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                TitoloSezioneDashboard(
                    titolo = "Servizi OSCam",
                    icona = Icons.Default.Storage,
                    coloreAccento = Color(0xFF42A5F5),
                    compatto = schermoCompatto
                )

                Spacer(modifier = Modifier.height(8.dp))

                elencoServers.forEachIndexed { indice, server ->
                    VoceStatoCard(
                        testo = server,
                        icona = if (indice == 0) {
                            Icons.Default.Storage
                        } else {
                            Icons.Default.Language
                        },
                        coloreAccento = Color(0xFF42A5F5),
                        compatto = schermoCompatto
                    )

                    if (indice < elencoServers.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (mostraReaders && elencoReaders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                TitoloSezioneDashboard(
                    titolo = "Elenco reader",
                    icona = Icons.Default.CreditCard,
                    coloreAccento = Color(0xFF66BB6A),
                    compatto = schermoCompatto
                )

                Spacer(modifier = Modifier.height(8.dp))

                elencoReaders.forEachIndexed { indice, reader ->
                    VoceStatoCard(
                        testo = reader,
                        icona = Icons.Default.CreditCard,
                        coloreAccento = Color(0xFF66BB6A),
                        compatto = schermoCompatto
                    )

                    if (indice < elencoReaders.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (mostraProxies && elencoProxies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                TitoloSezioneDashboard(
                    titolo = "Elenco proxy",
                    icona = Icons.Default.SwapHoriz,
                    coloreAccento = Color(0xFFAB47BC),
                    compatto = schermoCompatto
                )

                Spacer(modifier = Modifier.height(8.dp))

                elencoProxies.forEachIndexed { indice, proxy ->
                    VoceStatoCard(
                        testo = proxy,
                        icona = Icons.Default.SwapHoriz,
                        coloreAccento = Color(0xFFAB47BC),
                        compatto = schermoCompatto
                    )

                    if (indice < elencoProxies.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (mostraClients && elencoClients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                TitoloSezioneDashboard(
                    titolo = "Elenco client",
                    icona = Icons.Default.Devices,
                    coloreAccento = Color(0xFFFFA726),
                    compatto = schermoCompatto
                )

                Spacer(modifier = Modifier.height(8.dp))

                elencoClients.forEachIndexed { indice, client ->
                    ClientInfoCard(
                        testo = client,
                        compatto = schermoCompatto
                    )

                    if (indice < elencoClients.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
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

        Spacer(modifier = Modifier.width(9.dp))

        Text(
            text = titolo,
            fontWeight = FontWeight.Bold,
            fontSize = if (compatto) 17.sp else 19.sp,
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
                stato.contains("DISABLED", ignoreCase = true) ->
            Color(0xFFF44336)

        stato.contains("TIMEOUT", ignoreCase = true) ->
            Color(0xFFFF9800)

        stato.contains("CARDOK", ignoreCase = true) ||
                stato.contains("CONNECTED", ignoreCase = true) ||
                stato.equals("OK", ignoreCase = true) ||
                stato.contains("ACTIVE", ignoreCase = true) ->
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
    val colore = coloreStatoDashboard(stato)

    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = colore.copy(alpha = 0.13f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colore.copy(alpha = 0.38f)
        )
    ) {
        Text(
            text = stato.ifBlank { "—" },
            color = colore,
            fontWeight = FontWeight.Bold,
            fontSize = if (compatto) 10.sp else 11.sp,
            modifier = Modifier.padding(
                horizontal = if (compatto) 8.dp else 10.dp,
                vertical = if (compatto) 4.dp else 5.dp
            )
        )
    }
}

@Composable
fun VoceStatoCard(
    testo: String,
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    coloreAccento: Color,
    compatto: Boolean
) {
    val nome = testo
        .substringBeforeLast("—", testo)
        .trim()

    val stato = testo
        .substringAfterLast("—", "")
        .trim()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 14.dp else 16.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            coloreAccento.copy(alpha = 0.22f)
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compatto) 11.dp else 14.dp,
                    vertical = if (compatto) 10.dp else 12.dp
                ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
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
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            BadgeStatoDashboard(
                stato = stato,
                compatto = compatto
            )
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.Top
    ) {
        Text(
            text = etichetta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compatto) 12.sp else 13.sp,
            modifier = Modifier.width(
                if (compatto) 72.dp else 90.dp
            )
        )

        Text(
            text = valore,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = if (compatto) 13.sp else 14.sp,
            modifier = Modifier.weight(1f)
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
        Column(
            modifier = Modifier.padding(
                horizontal = if (compatto) 9.dp else 11.dp,
                vertical = if (compatto) 7.dp else 8.dp
            )
        ) {
            Text(
                text = etichetta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compatto) 10.sp else 11.sp
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = valore,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = if (compatto) 13.sp else 14.sp
            )
        }
    }
}

@Composable
fun ClientInfoCard(
    testo: String,
    compatto: Boolean
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            if (compatto) 16.dp else 18.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFFFA726).copy(alpha = 0.30f)
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                if (compatto) 12.dp else 15.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = Color(0xFFFFA726).copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier
                            .padding(if (compatto) 7.dp else 8.dp)
                            .size(if (compatto) 19.dp else 21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = nome.ifBlank { "Client" },
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compatto) 15.sp else 17.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                BadgeStatoDashboard(
                    stato = stato,
                    compatto = compatto
                )
            }

            if (
                !canale.isNullOrBlank() ||
                !provider.isNullOrBlank() ||
                !caid.isNullOrBlank() ||
                !ecm.isNullOrBlank() ||
                !reader.isNullOrBlank()
            ) {
                Spacer(modifier = Modifier.height(11.dp))

                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f)
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!canale.isNullOrBlank()) {
                RigaDettaglioClient(
                    etichetta = "Canale",
                    valore = canale,
                    compatto = compatto
                )
            }

            if (!provider.isNullOrBlank()) {
                if (!canale.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                RigaDettaglioClient(
                    etichetta = "Provider",
                    valore = provider,
                    compatto = compatto
                )
            }

            if (!caid.isNullOrBlank() || !ecm.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!caid.isNullOrBlank()) {
                        MiniDatoClient(
                            etichetta = "CAID",
                            valore = caid,
                            compatto = compatto,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!ecm.isNullOrBlank()) {
                        MiniDatoClient(
                            etichetta = "ECM",
                            valore = ecm,
                            compatto = compatto,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (!reader.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(9.dp))

                RigaDettaglioClient(
                    etichetta = "Reader",
                    valore = reader,
                    compatto = compatto
                )
            }
        }
    }
}


@Composable
fun WebIfScreen(
    host: String,
    porta: String,
    username: String,
    password: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onClose()
    }

    val urlWebIf = "http://${host.trim()}:${porta.trim()}"

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClose
            ) {
                Text("✕ Chiudi")
            }

            Text(
                text = "WebIF OSCam",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { webContext ->
                android.webkit.WebView(webContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    webChromeClient = android.webkit.WebChromeClient()

                    webViewClient =
                        object : android.webkit.WebViewClient() {
                            private var tentativiRiconnessione = 0
                            private var riconnessioneProgrammata = false
                            private var errorePaginaCorrente = false
                            private val massimoTentativi = 3

                            private fun paginaRiconnessione(tentativo: Int): String =
                                """
                                <!doctype html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <style>
                                        body {
                                            margin: 0;
                                            background: #121212;
                                            color: #e8e8e8;
                                            font-family: sans-serif;
                                            display: flex;
                                            min-height: 100vh;
                                            align-items: center;
                                            justify-content: center;
                                            text-align: center;
                                        }
                                        .box { padding: 28px; }
                                        .title {
                                            color: #4CAF50;
                                            font-size: 22px;
                                            font-weight: 700;
                                            margin-bottom: 12px;
                                        }
                                        .text { font-size: 16px; line-height: 1.5; }
                                    </style>
                                </head>
                                <body>
                                    <div class="box">
                                        <div class="title">Riconnessione WebIF…</div>
                                        <div class="text">Tentativo $tentativo di $massimoTentativi</div>
                                    </div>
                                </body>
                                </html>
                                """.trimIndent()

                            private fun paginaRiprova(): String =
                                """
                                <!doctype html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <style>
                                        body {
                                            margin: 0;
                                            background: #121212;
                                            color: #e8e8e8;
                                            font-family: sans-serif;
                                            display: flex;
                                            min-height: 100vh;
                                            align-items: center;
                                            justify-content: center;
                                            text-align: center;
                                        }
                                        .box { padding: 28px; }
                                        .title {
                                            color: #ff7043;
                                            font-size: 22px;
                                            font-weight: 700;
                                            margin-bottom: 12px;
                                        }
                                        .text {
                                            font-size: 16px;
                                            line-height: 1.5;
                                            margin-bottom: 24px;
                                        }
                                        .button {
                                            display: inline-block;
                                            padding: 12px 24px;
                                            border: 1px solid #4CAF50;
                                            border-radius: 14px;
                                            color: #4CAF50;
                                            text-decoration: none;
                                            font-size: 17px;
                                            font-weight: 700;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div class="box">
                                        <div class="title">WebIF non raggiungibile</div>
                                        <div class="text">
                                            La connessione è stata interrotta.<br>
                                            OSCam Pulse ha già provato a riconnettersi.
                                        </div>
                                        <a class="button" href="oscampulse://retry">Riprova</a>
                                    </div>
                                </body>
                                </html>
                                """.trimIndent()

                            private fun gestisciErrorePrincipale(
                                view: android.webkit.WebView?
                            ) {
                                if (view == null || riconnessioneProgrammata) {
                                    return
                                }

                                errorePaginaCorrente = true
                                view.stopLoading()

                                if (tentativiRiconnessione < massimoTentativi) {
                                    tentativiRiconnessione++
                                    riconnessioneProgrammata = true

                                    view.loadDataWithBaseURL(
                                        null,
                                        paginaRiconnessione(tentativiRiconnessione),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )

                                    view.postDelayed({
                                        riconnessioneProgrammata = false
                                        view.loadUrl(urlWebIf)
                                    }, 2500L)
                                } else {
                                    view.loadDataWithBaseURL(
                                        null,
                                        paginaRiprova(),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }

                            private fun riprovaManuale(
                                view: android.webkit.WebView?
                            ) {
                                if (view == null) {
                                    return
                                }

                                tentativiRiconnessione = 0
                                riconnessioneProgrammata = false
                                errorePaginaCorrente = false
                                view.loadUrl(urlWebIf)
                            }

                            override fun onPageStarted(
                                view: android.webkit.WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                if (url?.startsWith("http://") == true ||
                                    url?.startsWith("https://") == true
                                ) {
                                    errorePaginaCorrente = false
                                }

                                super.onPageStarted(view, url, favicon)
                            }

                            override fun onPageFinished(
                                view: android.webkit.WebView?,
                                url: String?
                            ) {
                                super.onPageFinished(view, url)

                                if (!errorePaginaCorrente &&
                                    (url?.startsWith("http://") == true ||
                                            url?.startsWith("https://") == true)
                                ) {
                                    tentativiRiconnessione = 0
                                    riconnessioneProgrammata = false
                                }
                            }

                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    gestisciErrorePrincipale(view)
                                } else {
                                    super.onReceivedError(view, request, error)
                                }
                            }

                            @Suppress("DEPRECATION")
                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                if (android.os.Build.VERSION.SDK_INT <
                                    android.os.Build.VERSION_CODES.M
                                ) {
                                    gestisciErrorePrincipale(view)
                                } else {
                                    super.onReceivedError(
                                        view,
                                        errorCode,
                                        description,
                                        failingUrl
                                    )
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                if (request?.url?.scheme == "oscampulse") {
                                    riprovaManuale(view)
                                    return true
                                }

                                return false
                            }

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                url: String?
                            ): Boolean {
                                if (url?.startsWith("oscampulse://") == true) {
                                    riprovaManuale(view)
                                    return true
                                }

                                return false
                            }

                            override fun onReceivedHttpAuthRequest(
                                view: android.webkit.WebView?,
                                handler: android.webkit.HttpAuthHandler?,
                                hostRichiesto: String?,
                                realm: String?
                            ) {
                                if (
                                    username.isNotBlank() ||
                                    password.isNotBlank()
                                ) {
                                    handler?.proceed(
                                        username,
                                        password
                                    )
                                } else {
                                    super.onReceivedHttpAuthRequest(
                                        view,
                                        handler,
                                        hostRichiesto,
                                        realm
                                    )
                                }
                            }
                        }

                    loadUrl(urlWebIf)
                }
            }
        )
    }
}

@Composable
fun StatoColorato(
    testo: String
) {
    val colore = when {
        testo.contains("NOT FOUND", ignoreCase = true) ->
            Color(0xFFF44336)

        testo.contains("ERROR", ignoreCase = true) ->
            Color(0xFFF44336)

        testo.contains("OFFLINE", ignoreCase = true) ->
            Color(0xFFF44336)

        testo.contains("DISABLED", ignoreCase = true) ->
            Color(0xFFF44336)

        testo.contains("TIMEOUT", ignoreCase = true) ->
            Color(0xFFFF9800)

        testo.contains("CARDOK", ignoreCase = true) ->
            Color(0xFF4CAF50)

        testo.contains("CONNECTED", ignoreCase = true) ->
            Color(0xFF4CAF50)

        testo.contains("OK", ignoreCase = true) ->
            Color(0xFF4CAF50)

        else ->
            MaterialTheme.colorScheme.onBackground
    }

    Text(
            text = testo,
            color = colore,
            fontWeight = FontWeight.SemiBold
        )
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape =
            androidx.compose.foundation.shape.RoundedCornerShape(
                if (compatto) 14.dp else 16.dp
            ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            coloreAccento.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                if (compatto) 10.dp else 14.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = coloreAccento.copy(alpha = 0.14f)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = icona,
                        contentDescription = null,
                        tint = coloreAccento,
                        modifier = Modifier.padding(
                            if (compatto) 5.dp else 7.dp
                        )
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                Text(
                    text = titolo,
                    fontSize = if (compatto) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                androidx.compose.material3.Icon(
                    imageVector =
                        if (aperta) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                    contentDescription =
                        if (aperta) {
                            "Nascondi $titolo"
                        } else {
                            "Mostra $titolo"
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (compatto) 6.dp else 10.dp
                )
            )

            Text(
                text = valore,
                fontSize = if (compatto) 23.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = coloreAccento
            )
        }
    }
}

private fun caricaServerSalvati(
    preferences: android.content.SharedPreferences
): List<OscamServer> {
    val risultato = mutableListOf<OscamServer>()

    val jsonSalvato =
        preferences.getString("server_salvati", "") ?: ""

    if (jsonSalvato.isNotBlank()) {
        try {
            val array = org.json.JSONArray(jsonSalvato)

            for (indice in 0 until array.length()) {
                val oggetto = array.getJSONObject(indice)

                risultato.add(
                    OscamServer(
                        nome = oggetto.optString("nome", "Server"),
                        host = oggetto.optString("host", ""),
                        porta = oggetto.optString("porta", ""),
                        username = oggetto.optString("username", ""),
                        password = oggetto.optString("password", "")
                    )
                )
            }
        } catch (_: Exception) {
            risultato.clear()
        }
    }

    if (risultato.isEmpty()) {
        val hostEsistente =
            preferences.getString("host", "") ?: ""

        val portaEsistente =
            preferences.getString("porta", "") ?: ""

        if (
            hostEsistente.isNotBlank() &&
            portaEsistente.isNotBlank()
        ) {
            risultato.add(
                OscamServer(
                    nome =
                        preferences.getString(
                            "nome_server",
                            "Server principale"
                        ) ?: "Server principale",
                    host = hostEsistente,
                    porta = portaEsistente,
                    username =
                        preferences.getString(
                            "username",
                            ""
                        ) ?: "",
                    password =
                        preferences.getString(
                            "password",
                            ""
                        ) ?: ""
                )
            )
        }
    }

    return risultato
}

private fun salvaServerSalvati(
    preferences: android.content.SharedPreferences,
    server: List<OscamServer>
) {
    val array = org.json.JSONArray()

    server.forEach { elemento ->
        val oggetto = org.json.JSONObject()

        oggetto.put("nome", elemento.nome)
        oggetto.put("host", elemento.host)
        oggetto.put("porta", elemento.porta)
        oggetto.put("username", elemento.username)
        oggetto.put("password", elemento.password)

        array.put(oggetto)
    }

    preferences.edit()
        .putString("server_salvati", array.toString())
        .apply()
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OscamMonitorTheme {
        Greeting("Oscam Monitor")
    }
}

