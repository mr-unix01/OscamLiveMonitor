
package com.mrunix.oscamlivemonitor

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mrunix.oscamlivemonitor.ui.theme.OscamLiveMonitorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dashboardViewModel =
            androidx.lifecycle.ViewModelProvider(
                this,
                DashboardViewModel.Factory(
                    api = OscamApi(),
                    preferences =
                        getSharedPreferences(
                            "oscam_monitor",
                            android.content.Context.MODE_PRIVATE
                        )
                )
            )[DashboardViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            OscamLiveMonitorTheme {
                var editorLandscapeIme by remember {
                    mutableStateOf(false)
                }

                val window = this@MainActivity.window

                DisposableEffect(editorLandscapeIme) {
                    val controller =
                        androidx.core.view.WindowCompat.getInsetsController(
                            window,
                            window.decorView
                        )

                    if (editorLandscapeIme) {
                        controller.hide(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()
                        )
                    } else {
                        controller.show(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()
                        )
                    }

                    onDispose {
                        controller.show(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets =
                        if (editorLandscapeIme) {
                            WindowInsets.navigationBars
                        } else {
                            ScaffoldDefaults.contentWindowInsets
                        }
                ) { innerPadding ->
                    Greeting(
                        name = "OSCam Live Monitor",
                        modifier = Modifier.padding(innerPadding),
                        dashboardViewModel = dashboardViewModel,
                        onEditorLandscapeImeChange = {
                            editorLandscapeIme = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel? = null,
    onEditorLandscapeImeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    val lifecycleOwner =
        context as? androidx.lifecycle.LifecycleOwner

    var activityAttiva by remember {
        mutableStateOf(
            lifecycleOwner?.lifecycle?.currentState?.isAtLeast(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) ?: true
        )
    }

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose {}
        } else {
            val observer =
                androidx.lifecycle.LifecycleEventObserver { _, _ ->
                    activityAttiva =
                        lifecycleOwner.lifecycle.currentState.isAtLeast(
                            androidx.lifecycle.Lifecycle.State.STARTED
                        )
                }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    val temaScuro = androidx.compose.foundation.isSystemInDarkTheme()
    val schermoCompatto =
        BuildConfig.TABLET_MODE ||
                androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 500

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
            SecurePasswordCrypto.decrypt(
                preferences.getString("password", "") ?: ""
            )
        )
    }

    var statoPreview by rememberSaveable { mutableStateOf("") }
    var mostraRepositoryGitHub by rememberSaveable { mutableStateOf(false) }
    var refreshManualeInCorso by remember { mutableStateOf(false) }

    val dashboardState =
        if (dashboardViewModel != null) {
            dashboardViewModel.uiState.collectAsState().value
        } else {
            DashboardUiState()
        }

    val stato =
        if (dashboardViewModel != null) {
            dashboardState.stato
        } else {
            statoPreview
        }

    fun impostaStatoDashboard(nuovoStato: String) {
        if (dashboardViewModel != null) {
            dashboardViewModel.impostaStato(nuovoStato)
        } else {
            statoPreview = nuovoStato
        }
    }

    val ultimoAggiornamento =
        dashboardState.ultimoAggiornamento

    val erroriRefreshConsecutivi =
        dashboardState.erroriRefreshConsecutivi

    val versioneOscam = dashboardState.versioneOscam
    val uptime = dashboardState.uptime
    val cpuOscam = dashboardState.cpuOscam
    val ramOscam = dashboardState.ramOscam

    val servers = dashboardState.servers
    val readers = dashboardState.readers
    val proxies = dashboardState.proxies
    val clients = dashboardState.clients
    val users = dashboardState.users

    val elencoServers = dashboardState.elencoServers

    val elencoReaders = dashboardState.elencoReaders

    val elencoProxies = dashboardState.elencoProxies

    val elencoClients = dashboardState.elencoClients

    val elencoUsers = dashboardState.elencoUsers

    val readerAbilitati = dashboardState.readerAbilitati

    val userAbilitati = dashboardState.userAbilitati

    var toggleReadersInCorso by remember {
        mutableStateOf(emptySet<String>())
    }

    var toggleProxiesInCorso by remember {
        mutableStateOf(emptySet<String>())
    }

    var toggleUsersInCorso by remember {
        mutableStateOf(emptySet<String>())
    }

    var mostraConnessione by rememberSaveable { mutableStateOf(true) }
    var mostraWebIf by rememberSaveable { mutableStateOf(false) }
    var mostraLiveLog by rememberSaveable { mutableStateOf(false) }
    var mostraStrumenti by rememberSaveable { mutableStateOf(false) }
    var mostraInformazioni by rememberSaveable { mutableStateOf(false) }

    var serverSalvati by remember {
        mutableStateOf(caricaServerSalvati(preferences))
    }

    val statoServerSalvati = remember {
        androidx.compose.runtime.mutableStateMapOf<String, OscamServerConnectionState>()
    }

    val statoServerPrecedente = remember {
        mutableMapOf<String, OscamServerConnectionState>()
    }

    var refreshServerSalvati by remember { mutableIntStateOf(0) }
    var refreshServerInCorso by remember { mutableStateOf(false) }

    var mostraAggiungiServer by remember { mutableStateOf(false) }

    var serverDaModificare by remember { mutableStateOf<OscamServer?>(null) }
    var modificaNomeServer by remember { mutableStateOf("") }
    var modificaHostServer by remember { mutableStateOf("") }
    var modificaPortaServer by remember { mutableStateOf("") }
    var modificaUsernameServer by remember { mutableStateOf("") }
    var modificaPasswordServer by remember { mutableStateOf("") }
    var mostraPasswordModifica by remember { mutableStateOf(false) }

    var serverDaEliminare by remember { mutableStateOf<OscamServer?>(null) }
    var mostraRiordinaServer by remember { mutableStateOf(false) }

    var nuovoNomeServer by remember { mutableStateOf("") }
    var nuovoHostServer by remember { mutableStateOf("") }
    var nuovaPortaServer by remember { mutableStateOf("") }
    var nuovoUsernameServer by remember { mutableStateOf("") }
    var nuovaPasswordServer by remember { mutableStateOf("") }
    var mostraPasswordNuovo by remember { mutableStateOf(false) }
    var testConnessioneNuovoInCorso by remember { mutableStateOf(false) }
    var testConnessioneNuovoJob by remember {
        mutableStateOf<kotlinx.coroutines.Job?>(null)
    }
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
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogSnackbarHostState = remember { SnackbarHostState() }

    fun mostraSnackbar(messaggio: String) {
        val testo = if (messaggio.startsWith("ERRORE:", ignoreCase = true)) {
            messaggio.substringAfter(":").trim()
        } else {
            messaggio
        }

        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = testo,
                duration = SnackbarDuration.Short
            )
        }
    }

    fun mostraSnackbarDialog(messaggio: String) {
        coroutineScope.launch {
            dialogSnackbarHostState.currentSnackbarData?.dismiss()
            dialogSnackbarHostState.showSnackbar(
                message = messaggio,
                duration = SnackbarDuration.Short
            )
        }
    }

    suspend fun aggiornaDashboardDaOscam(
        mostraErroreSubito: Boolean = true
    ): Boolean {
        val viewModel =
            dashboardViewModel
                ?: return false

        val aggiornato =
            viewModel.aggiornaDashboard(
                host = host,
                porta = porta,
                username = username,
                password = password,
                mostraErroreSubito = mostraErroreSubito
            )

        if (!aggiornato && mostraErroreSubito) {
            val statoErrore =
                viewModel.uiState.value.stato

            if (
                statoErrore.startsWith(
                    "ERRORE:",
                    ignoreCase = true
                )
            ) {
                mostraSnackbar(statoErrore)
            }
        }

        return aggiornato
    }

    val richiestaPermessoRete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concesso ->
        permessoReteConcesso = concesso

        val nuovoStato =
            if (concesso) {
                "Accesso alla rete locale consentito"
            } else {
                "Permesso rete locale necessario per collegarsi a OSCam"
            }

        impostaStatoDashboard(nuovoStato)
        mostraSnackbar(nuovoStato)
    }

    LaunchedEffect(Unit) {
        val jsonServerSalvati =
            preferences.getString(
                "server_salvati",
                null
            )

        if (
            serverSalvati.isNotEmpty() &&
            (
                jsonServerSalvati == null ||
                serverSalvatiRichiedonoMigrazione(
                    jsonServerSalvati
                )
            )
        ) {
            salvaServerSalvati(
                preferences = preferences,
                server = serverSalvati
            )
        }

        val passwordSalvata =
            preferences.getString(
                "password",
                ""
            ) ?: ""

        if (
            passwordSalvata.isNotEmpty() &&
            !SecurePasswordCrypto.isEncrypted(
                passwordSalvata
            )
        ) {
            preferences.edit()
                .putString(
                    "password",
                    SecurePasswordCrypto.encrypt(
                        passwordSalvata
                    )
                )
                .apply()
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
        serverSalvati,
        permessoReteConcesso,
        refreshServerSalvati,
        activityAttiva
    ) {
        if (
            mostraConnessione &&
            permessoReteConcesso &&
            activityAttiva
        ) {
            statoServerSalvati.clear()

            serverSalvati.forEach { server ->
                val chiave = server.host.trim() + ":" + server.porta.trim()
                statoServerSalvati[chiave] = OscamServerConnectionState.CHECKING
            }

            while (true) {
                kotlinx.coroutines.coroutineScope {
                    serverSalvati.forEach { server ->
                        launch {
                            val chiave =
                                server.host.trim() + ":" + server.porta.trim()

                            val nuovoStato = withContext(Dispatchers.IO) {
                                if (
                                    !api.portaOscamRaggiungibile(
                                        server.host,
                                        server.porta
                                    )
                                ) {
                                    OscamServerConnectionState.UNREACHABLE
                                } else {
                                    val risultato = api.scaricaStatusJson(
                                        host = server.host,
                                        porta = server.porta,
                                        username = server.username,
                                        password = server.password
                                    )

                                    when {
                                        risultato.startsWith(
                                            "ERRORE: HTTP 401",
                                            ignoreCase = true
                                        ) ||
                                        risultato.startsWith(
                                            "ERRORE: HTTP 403",
                                            ignoreCase = true
                                        ) ->
                                            OscamServerConnectionState.AUTH_ERROR

                                        !risultato.startsWith(
                                            "ERRORE:",
                                            ignoreCase = true
                                        ) &&
                                        api.statusJsonOscamValido(risultato) ->
                                            OscamServerConnectionState.ACTIVE

                                        else ->
                                            OscamServerConnectionState.REACHABLE_NO_OSCAM
                                    }
                                }
                            }

                            val statoPrecedente = statoServerPrecedente[chiave]
                            statoServerSalvati[chiave] = nuovoStato

                            if (
                                nuovoStato == OscamServerConnectionState.ACTIVE &&
                                chiave == host.trim() + ":" + porta.trim() &&
                                stato.startsWith("ERRORE:", ignoreCase = true)
                            ) {
                                impostaStatoDashboard("")
                            }

                            if (
                                statoPrecedente != null &&
                                statoPrecedente != nuovoStato
                            ) {
                                val messaggio = when (nuovoStato) {
                                    OscamServerConnectionState.ACTIVE ->
                                        "${server.nome}: OSCam di nuovo attivo"
                                    OscamServerConnectionState.AUTH_ERROR ->
                                        "${server.nome}: errore autenticazione OSCam"
                                    OscamServerConnectionState.REACHABLE_NO_OSCAM ->
                                        "${server.nome}: raggiungibile, ma OSCam non risponde"
                                    OscamServerConnectionState.UNREACHABLE ->
                                        "${server.nome}: non raggiungibile"
                                    OscamServerConnectionState.CHECKING -> null
                                }

                                if (messaggio != null && !mostraStrumenti) {
                                    mostraSnackbar(messaggio)
                                }
                            }

                            statoServerPrecedente[chiave] = nuovoStato
                        }
                    }
                }

                if (refreshServerInCorso) {
                    refreshServerInCorso = false
                }

                kotlinx.coroutines.delay(5000)
            }
        }
    }

    LaunchedEffect(
        mostraConnessione,
        mostraWebIf,
        mostraLiveLog,
        riavvioInCorso,
        host,
        porta,
        username,
        password
    ) {
        if (
            !mostraConnessione &&
            !mostraWebIf &&
            !mostraLiveLog &&
            !riavvioInCorso
        ) {
            while (true) {
                // Primo refresh immediato: quando si chiude WebIF/Live Log
                // la dashboard non aspetta più 5 secondi prima di aggiornarsi.
                val conteggioErrori =
                    dashboardViewModel
                        ?.aggiornaDashboardPolling(
                            host = host,
                            porta = porta,
                            username = username,
                            password = password
                        )
                        ?: 0

                if (conteggioErrori == 3) {
                    mostraSnackbar("Connessione persa")
                }

                kotlinx.coroutines.delay(5000)
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

        RiavviaOscamDialog(
            nomeServer = nomeServerAttivo,
            host = host.trim(),
            porta = porta.trim(),
            onDismissRequest = {
                if (!riavvioInCorso) {
                    mostraConfermaRiavvio = false
                }
            },
            onCancel = {
                mostraConfermaRiavvio = false
            },
            onConfirm = {
                mostraConfermaRiavvio = false

                if (host.isBlank() || porta.isBlank()) {
                    val messaggio =
                        "Inserisci prima Host/IP e Porta"
                    impostaStatoDashboard(messaggio)
                    mostraSnackbar(messaggio)
                } else {
                    riavvioInCorso = true
                    impostaStatoDashboard("Riavvio OSCam...")

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
                            impostaStatoDashboard(risultatoRiavvio)
                            mostraSnackbar(risultatoRiavvio)
                            return@launch
                        }

                        impostaStatoDashboard(
                            "Riconnessione OSCam..."
                        )
                        kotlinx.coroutines.delay(3000)

                        var riconnesso = false

                        for (tentativo in 1..10) {
                            val aggiornato =
                                aggiornaDashboardDaOscam(
                                    mostraErroreSubito = false
                                )

                            if (aggiornato) {
                                riconnesso = true
                                break
                            }

                            if (tentativo < 10) {
                                impostaStatoDashboard(
                                    "Riconnessione OSCam... ($tentativo/10)"
                                )
                                kotlinx.coroutines.delay(2000)
                            }
                        }

                        riavvioInCorso = false

                        if (riconnesso) {
                            mostraConnessione = false
                            impostaStatoDashboard("Connesso")
                        } else {
                            val messaggio =
                                "ERRORE: OSCam non raggiungibile dopo il riavvio"
                            impostaStatoDashboard(messaggio)
                            mostraSnackbar(messaggio)
                        }
                    }
                }
            }
        )
    }

    if (mostraSpiegazionePermesso) {
        SpiegazionePermessoReteDialog(
            onDismiss = {
                mostraSpiegazionePermesso = false
            },
            onContinue = {
                mostraSpiegazionePermesso = false

                if (Build.VERSION.SDK_INT >= 37) {
                    richiestaPermessoRete.launch(
                        Manifest.permission.ACCESS_LOCAL_NETWORK
                    )
                }
            },
            onNotNow = {
                mostraSpiegazionePermesso = false
                val messaggio =
                    "Permesso rete locale necessario per OSCam"
                impostaStatoDashboard(messaggio)
                mostraSnackbar(messaggio)
            }
        )
    }

    if (mostraAggiungiServer) {
        AggiungiServerDialog(
            nomeServer = nuovoNomeServer,
            onNomeChange = {
                nuovoNomeServer = it
            },
            hostServer = nuovoHostServer,
            onHostChange = {
                nuovoHostServer = it
            },
            portaServer = nuovaPortaServer,
            onPortaChange = {
                nuovaPortaServer = it
            },
            usernameServer = nuovoUsernameServer,
            onUsernameChange = {
                nuovoUsernameServer = it
            },
            passwordServer = nuovaPasswordServer,
            onPasswordChange = {
                nuovaPasswordServer = it
            },
            mostraPassword = mostraPasswordNuovo,
            onTogglePassword = {
                mostraPasswordNuovo =
                    !mostraPasswordNuovo
            },
            testConnessioneInCorso =
                testConnessioneNuovoInCorso,
            dialogSnackbarHostState =
                dialogSnackbarHostState,
            onDismiss = {
                testConnessioneNuovoJob?.cancel()
                testConnessioneNuovoJob = null
                testConnessioneNuovoInCorso = false
                mostraAggiungiServer = false
            },
            onTestConnection = test@{
                val hostPulito =
                    nuovoHostServer.trim()

                val portaPulita =
                    nuovaPortaServer.trim()

                if (!permessoReteConcesso) {
                    mostraSnackbarDialog(
                        "Concedi prima l'accesso alla rete locale"
                    )
                    return@test
                }

                if (
                    hostPulito.isBlank() ||
                    portaPulita.toIntOrNull() == null
                ) {
                    mostraSnackbarDialog(
                        "Inserisci Host/IP e una porta valida"
                    )
                    return@test
                }

                testConnessioneNuovoInCorso = true

                testConnessioneNuovoJob =
                    coroutineScope.launch {
                        val risultato =
                            withContext(Dispatchers.IO) {
                                api.scaricaStatusJson(
                                    host = hostPulito,
                                    porta = portaPulita,
                                    username = nuovoUsernameServer,
                                    password = nuovaPasswordServer
                                )
                            }

                        testConnessioneNuovoInCorso = false

                        if (
                            risultato.startsWith(
                                "ERRORE:",
                                ignoreCase = true
                            )
                        ) {
                            mostraSnackbarDialog(
                                risultato
                                    .substringAfter(":")
                                    .trim()
                            )
                        } else if (
                            !api.statusJsonOscamValido(
                                risultato
                            )
                        ) {
                            mostraSnackbarDialog(
                                "Risposta ricevuta, ma non è un server OSCam valido"
                            )
                        } else {
                            mostraSnackbarDialog(
                                "Connessione riuscita"
                            )
                        }
                    }
            },
            onSave = {
                val nomePulito =
                    nuovoNomeServer.trim()

                val hostPulito =
                    nuovoHostServer.trim()

                val portaPulita =
                    nuovaPortaServer.trim()

                if (
                    nomePulito.isBlank() ||
                    hostPulito.isBlank() ||
                    portaPulita.toIntOrNull() == null
                ) {
                    mostraSnackbarDialog(
                        "Inserisci nome, Host/IP e una porta valida"
                    )
                } else {
                    val nuovoServer =
                        OscamServer(
                            nome = nomePulito,
                            host = hostPulito,
                            porta = portaPulita,
                            username =
                                nuovoUsernameServer,
                            password =
                                nuovaPasswordServer
                        )

                    serverSalvati =
                        serverSalvati + nuovoServer

                    salvaServerSalvati(
                        preferences = preferences,
                        server = serverSalvati
                    )

                    host = nuovoServer.host
                    porta = nuovoServer.porta
                    username = nuovoServer.username
                    password = nuovoServer.password

                    preferences.edit()
                        .putString(
                            "nome_server",
                            nuovoServer.nome
                        )
                        .putString(
                            "host",
                            nuovoServer.host
                        )
                        .putString(
                            "porta",
                            nuovoServer.porta
                        )
                        .putString(
                            "username",
                            nuovoServer.username
                        )
                        .putString(
                            "password",
                            SecurePasswordCrypto.encrypt(
                                nuovoServer.password
                            )
                        )
                        .apply()

                    nuovoNomeServer = ""
                    nuovoHostServer = ""
                    nuovaPortaServer = ""
                    nuovoUsernameServer = ""
                    nuovaPasswordServer = ""

                    testConnessioneNuovoJob?.cancel()
                    testConnessioneNuovoJob = null
                    testConnessioneNuovoInCorso = false
                    mostraAggiungiServer = false

                    mostraSnackbar(
                        "Server ${nuovoServer.nome} aggiunto"
                    )
                }
            }
        )
    }

    serverDaModificare?.let { serverOriginale ->
        ModificaServerDialog(
            nomeServer = modificaNomeServer,
            onNomeChange = {
                modificaNomeServer = it
            },
            hostServer = modificaHostServer,
            onHostChange = {
                modificaHostServer = it
            },
            portaServer = modificaPortaServer,
            onPortaChange = {
                modificaPortaServer = it
            },
            usernameServer = modificaUsernameServer,
            onUsernameChange = {
                modificaUsernameServer = it
            },
            passwordServer = modificaPasswordServer,
            onPasswordChange = {
                modificaPasswordServer = it
            },
            mostraPassword = mostraPasswordModifica,
            onTogglePassword = {
                mostraPasswordModifica =
                    !mostraPasswordModifica
            },
            dialogSnackbarHostState = dialogSnackbarHostState,
            onDismiss = {
                serverDaModificare = null
            },
            onDelete = {
                serverDaEliminare = serverOriginale
                serverDaModificare = null
            },
            onSave = {
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
                    mostraSnackbarDialog(
                        "Inserisci nome, Host/IP e una porta valida"
                    )
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
                                    SecurePasswordCrypto.encrypt(
                                        serverAggiornato.password
                                    )
                                )
                                .apply()
                        }

                        mostraSnackbar(
                            "Server ${serverAggiornato.nome} modificato"
                        )
                    }

                    serverDaModificare = null
                }
            }
        )
    }

    serverDaEliminare?.let { serverOriginale ->
        EliminaServerDialog(
            nomeServer = serverOriginale.nome,
            onDismiss = {
                serverDaEliminare = null
            },
            onConfirm = {
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
                                        SecurePasswordCrypto.encrypt(
                                            nuovoSelezionato.password
                                        )
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

                        mostraSnackbar(
                            "Server ${serverOriginale.nome} eliminato"
                        )
                    }

                    serverDaEliminare = null
            }
        )
    }

    if (mostraRiordinaServer) {
        RiordinaServerDialog(
            server = serverSalvati,
            onDismiss = {
                mostraRiordinaServer = false
            },
            onMoveUp = { indice ->
                val nuovaLista =
                    serverSalvati.toMutableList()

                val elemento =
                    nuovaLista.removeAt(indice)

                nuovaLista.add(
                    indice - 1,
                    elemento
                )

                serverSalvati = nuovaLista

                salvaServerSalvati(
                    preferences = preferences,
                    server = serverSalvati
                )
            },
            onMoveDown = { indice ->
                val nuovaLista =
                    serverSalvati.toMutableList()

                val elemento =
                    nuovaLista.removeAt(indice)

                nuovaLista.add(
                    indice + 1,
                    elemento
                )

                serverSalvati = nuovaLista

                salvaServerSalvati(
                    preferences = preferences,
                    server = serverSalvati
                )
            }
        )
    }

    // La dashboard resta sempre composta anche mentre WebIF o Live Log sono aperti.
    // In questo modo, alla chiusura torna immediatamente completa con gli ultimi dati.
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val refreshHomeAttivo =
            mostraConnessione &&
                !mostraInformazioni &&
                !mostraRepositoryGitHub &&
                !mostraWebIf &&
                !mostraLiveLog &&
                !mostraStrumenti &&
                permessoReteConcesso &&
                serverSalvati.isNotEmpty()

        val scrollHome = rememberScrollState()

        val sogliaAutoscrollClient =
            with(androidx.compose.ui.platform.LocalDensity.current) {
                240.dp.roundToPx()
            }

        val contenutoPrincipale: @Composable () -> Unit = {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollHome)
                .imePadding()
                .padding(
                    horizontal = if (schermoCompatto) 12.dp else 16.dp,
                    vertical = if (schermoCompatto) 10.dp else 16.dp
                )
        ) {
            ExpressiveAppHeader(
                compatto = schermoCompatto,
                totaleServer = serverSalvati.size,
                statiServer = statoServerSalvati.values
            )

            Spacer(
                modifier = Modifier.height(
                    if (schermoCompatto) 6.dp else 10.dp
                )
            )

            if (mostraConnessione) {
                ServerSelectionHeader(
                    onAddServer = {
                        nuovoNomeServer = ""
                        nuovoHostServer = ""
                        nuovaPortaServer = ""
                        nuovoUsernameServer = ""
                        nuovaPasswordServer = ""
                        mostraPasswordNuovo = false
                        mostraAggiungiServer = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (serverSalvati.isEmpty()) {
                    EmptyServerState()
                } else {
                    serverSalvati.forEach { server ->
                        val serverSelezionato =
                            host.trim() == server.host &&
                                    porta.trim() == server.porta

                        OscamServerCard(
                            server = server,
                            selected = serverSelezionato,
                          connectionState = statoServerSalvati[server.host.trim() + ":" + server.porta.trim()]
                              ?: OscamServerConnectionState.CHECKING,
                            onSelect = {
                                host = server.host
                                porta = server.porta
                                username = server.username
                                password = server.password

                                preferences.edit()
                                    .putString("nome_server", server.nome)
                                    .putString("host", server.host)
                                    .putString("porta", server.porta)
                                    .putString("username", server.username)
                                    .putString(
                                        "password",
                                        SecurePasswordCrypto.encrypt(
                                            server.password
                                        )
                                    )
                                    .apply()

                                impostaStatoDashboard("")
                            },
                            onEdit = {
                                serverDaModificare = server
                                modificaNomeServer = server.nome
                                modificaHostServer = server.host
                                modificaPortaServer = server.porta
                                modificaUsernameServer = server.username
                                modificaPasswordServer = server.password
                                mostraPasswordModifica = false
                            },
                            onReorder = {
                                mostraRiordinaServer = true
                            },
                            onDelete = {
                                serverDaEliminare = server
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (schermoCompatto) 58.dp else 56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF66BB6A)
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    onClick = {
                        if (!permessoReteConcesso) {
                            val messaggio =
                                "Concedi prima l'accesso alla rete locale"
                            impostaStatoDashboard(messaggio)
                            mostraSnackbar(messaggio)
                            mostraSpiegazionePermesso = true
                            return@Button
                        }

                        if (host.isBlank() || porta.isBlank()) {
                            val messaggio =
                                "Inserisci Host/IP e Porta"
                            impostaStatoDashboard(messaggio)
                            mostraSnackbar(messaggio)
                            return@Button
                        }

                        preferences.edit()
                            .putString("host", host.trim())
                            .putString("porta", porta.trim())
                            .putString("username", username)
                            .putString(
                                "password",
                                SecurePasswordCrypto.encrypt(password)
                            )
                            .apply()

                        dashboardViewModel?.resetDashboardState()
                        impostaStatoDashboard("Connessione...")

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
                        text = "▶  Connetti",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    enabled = host.isNotBlank(),
                    onClick = {
                        mostraStrumenti = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Strumenti")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    onClick = {
                        mostraInformazioni = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Informazioni")
                }

            }

            if (mostraConnessione && stato.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (stato.isNotBlank()) {
                val coloreStato = when {
                    stato.equals("Connesso", ignoreCase = true) ->
                        if (temaScuro) Color(0xFF66BB6A) else Color(0xFF2E7D32)

                    stato.contains("selezionato", ignoreCase = true) ->
                        if (temaScuro) Color(0xFF66BB6A) else Color(0xFF2E7D32)

                    stato.contains("Connessione", ignoreCase = true) ->
                        if (temaScuro) Color(0xFFFFB74D) else Color(0xFFD96B00)

                    stato.startsWith("ERRORE:", ignoreCase = true) ->
                        Color(0xFFEF5350)

                    stato.contains("necessario", ignoreCase = true) ->
                        Color(0xFFEF5350)

                    else ->
                        MaterialTheme.colorScheme.primary
                }

                val serverAttivoDashboard =
                    if (!mostraConnessione) {
                        serverSalvati.firstOrNull { server ->
                            server.host.trim() == host.trim() &&
                                    server.porta.trim() == porta.trim()
                        }
                    } else {
                        null
                    }

                val nomeServerDashboard =
                    serverAttivoDashboard?.nome
                        ?: preferences.getString("nome_server", null)
                            ?.takeIf { it.isNotBlank() }
                        ?: host.trim()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = if (temaScuro) {
                            coloreStato.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            if (temaScuro) 1.dp else 1.4.dp,
                            coloreStato.copy(alpha = if (temaScuro) 0.70f else 0.85f)
                        ),
                        shadowElevation = if (temaScuro) 2.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 15.dp,
                                vertical = 9.dp
                            ),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(if (schermoCompatto) 10.dp else 11.dp)
                                    .background(
                                        color = coloreStato,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = stato,
                                color = coloreStato,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (schermoCompatto) 14.sp else 15.sp
                            )
                        }
                    }

                    if (
                        !mostraConnessione &&
                        stato.equals("Connesso", ignoreCase = true) &&
                        host.isNotBlank()
                    ) {
                        Spacer(modifier = Modifier.width(12.dp))

                        androidx.compose.material3.Surface(
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = if (temaScuro) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 7.dp
                                ),
                                horizontalAlignment = androidx.compose.ui.Alignment.Start
                            ) {
                                Text(
                                    text = nomeServerDashboard,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )

                                Text(
                                    text = "${host.trim()}:${porta.trim()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }


            if (!mostraConnessione) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardQuickAction(
                        titolo = "Server",
                      icona = Icons.Default.Storage,
                        coloreAccento = Color(0xFF29B6F6),
                        onClick = {
                            mostraConnessione = true
                            dashboardViewModel?.resetDashboardState()
                            impostaStatoDashboard("")

                            mostraServers = true
                            mostraReaders = true
                            mostraProxies = true
                            mostraClients = true
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DashboardQuickAction(
                        titolo = "WebIF",
                        icona = Icons.Default.Language,
                        coloreAccento = Color(0xFF29B6F6),
                        onClick = {
                            mostraWebIf = true
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DashboardQuickAction(
                        titolo = "Live Log",
                        icona = Icons.AutoMirrored.Filled.ReceiptLong,
                        coloreAccento = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            mostraLiveLog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (
                !mostraConnessione &&
                (versioneOscam.isNotBlank() ||
                uptime.isNotBlank() ||
                cpuOscam.isNotBlank() ||
                ramOscam.isNotBlank())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                ExpressiveOscamInfoCard(
                    versione = versioneOscam,
                    uptime = uptime,
                    cpu = cpuOscam,
                    ram = ramOscam,
                    compatto = schermoCompatto,
                    riavvioInCorso = riavvioInCorso,
                    onRiavviaClick = {
                        if (host.isBlank() || porta.isBlank()) {
                            val messaggio =
                                "Inserisci prima Host/IP e Porta"
                            impostaStatoDashboard(messaggio)
                            mostraSnackbar(messaggio)
                        } else {
                            mostraConfermaRiavvio = true
                        }
                    }
                )
            }

            if (mostraConnessione) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
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
                        modifier = Modifier.height(56.dp)
                    )

                }
            } else if (ultimoAggiornamento.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (refreshManualeInCorso) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF66BB6A)
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aggiorna dashboard",
                            tint = Color(0xFF66BB6A),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    if (!refreshManualeInCorso) {
                                        refreshManualeInCorso = true
                    
                                        coroutineScope.launch {
                                            try {
                                                aggiornaDashboardDaOscam(
                                                    mostraErroreSubito = true
                                                )
                                            } finally {
                                                refreshManualeInCorso = false
                                            }
                                        }
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))

                    Text(
                        text = "Aggiornato alle $ultimoAggiornamento",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (
                !mostraConnessione &&
                (servers.isNotBlank() ||
                readers.isNotBlank() ||
                proxies.isNotBlank() ||
                clients.isNotBlank() ||
                users.isNotBlank())
            ) {
                Spacer(modifier = Modifier.height(20.dp))

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

                    val utentiOnline =
                        elencoUsers.count { user ->
                            user.lineSequence()
                                .firstOrNull()
                                ?.substringAfterLast("—", "")
                                ?.trim()
                                ?.equals("ONLINE", ignoreCase = true) == true
                        }

                    val utentiConnessi =
                        elencoUsers.count { user ->
                            val statoUser =
                                user.lineSequence()
                                    .firstOrNull()
                                    ?.substringAfterLast("—", "")
                                    ?.trim()
                                    ?.uppercase()
                                    .orEmpty()

                            statoUser == "ONLINE" ||
                                statoUser == "CONNESSO" ||
                                statoUser == "CONNECTED"
                        }

                    DashboardCard(
                        titolo = "Users / Clients",
                        compatto = schermoCompatto,
                        valore = "$utentiOnline/$utentiConnessi",
                        icona = Icons.Default.Devices,
                        coloreAccento = Color(0xFFFFA726),
                        aperta = mostraClients,
                        onClick = {
                            mostraClients = !mostraClients
                        },
                        modifier = Modifier.weight(1f),
                        didascalia =
                            run {
                                val totaleUtenti =
                                    users.trim().toIntOrNull() ?: 0

                                when {
                                    totaleUtenti == 0 ->
                                        "nessun utente configurato"

                                    utentiConnessi == 0 ->
                                        if (totaleUtenti == 1) {
                                            "nessun client connesso · 1 utente"
                                        } else {
                                            "nessun client connesso · $totaleUtenti utenti"
                                        }

                                    totaleUtenti == 1 ->
                                        "online/connessi · 1 utente"

                                    else ->
                                        "online/connessi · $totaleUtenti utenti"
                                }
                            }
                    )
                }
            }

            if (!mostraConnessione && mostraServers && elencoServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

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

            if (!mostraConnessione && mostraReaders && elencoReaders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

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
                        compatto = schermoCompatto,
                        mostraToggleReader = true,
                        readerAbilitato = readerAbilitati[
                            reader.substringBeforeLast("—", reader).trim()
                        ] ?: true,
                        toggleReaderInCorso =
                            reader.substringBeforeLast("—", reader).trim() in
                                toggleReadersInCorso,
                        onToggleReader = {
                            val nomeReader =
                                reader.substringBeforeLast("—", reader).trim()

                            val attualmenteAbilitato =
                                readerAbilitati[nomeReader] ?: true

                            toggleReadersInCorso =
                                toggleReadersInCorso + nomeReader

                            coroutineScope.launch {
                                val esito =
                                    withContext(Dispatchers.IO) {
                                        api.impostaReaderAbilitato(
                                            host = host.trim(),
                                            porta = porta.trim(),
                                            username = username,
                                            password = password,
                                            nomeReader = nomeReader,
                                            abilita = !attualmenteAbilitato
                                        )
                                    }

                                if (!esito.startsWith("ERRORE:")) {
                                    val nuovoStato =
                                        !attualmenteAbilitato

                                    dashboardViewModel
                                        ?.aggiornaReaderAbilitatoLocale(
                                            nomeReader = nomeReader,
                                            abilitato = nuovoStato
                                        )

                                    kotlinx.coroutines.delay(400)

                                    aggiornaDashboardDaOscam(
                                        mostraErroreSubito = false
                                    )
                                }

                                toggleReadersInCorso =
                                    toggleReadersInCorso - nomeReader
                            }
                        }
                    )

                    if (indice < elencoReaders.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (!mostraConnessione && mostraProxies && elencoProxies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

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
                        compatto = schermoCompatto,
                        mostraToggleReader = true,
                        readerAbilitato = readerAbilitati[
                            proxy.substringBeforeLast("—", proxy).trim()
                        ] ?: true,
                        toggleReaderInCorso =
                            proxy.substringBeforeLast("—", proxy).trim() in
                                toggleProxiesInCorso,
                        onToggleReader = {
                            val nomeProxy =
                                proxy.substringBeforeLast("—", proxy).trim()

                            val attualmenteAbilitato =
                                readerAbilitati[nomeProxy] ?: true

                            toggleProxiesInCorso =
                                toggleProxiesInCorso + nomeProxy

                            coroutineScope.launch {
                                val esito =
                                    withContext(Dispatchers.IO) {
                                        api.impostaReaderAbilitato(
                                            host = host.trim(),
                                            porta = porta.trim(),
                                            username = username,
                                            password = password,
                                            nomeReader = nomeProxy,
                                            abilita = !attualmenteAbilitato
                                        )
                                    }

                                if (!esito.startsWith("ERRORE:")) {
                                    val nuovoStato =
                                        !attualmenteAbilitato

                                    dashboardViewModel
                                        ?.aggiornaProxyAbilitatoLocale(
                                            nomeProxy = nomeProxy,
                                            abilitato = nuovoStato
                                        )

                                    kotlinx.coroutines.delay(400)

                                    aggiornaDashboardDaOscam(
                                        mostraErroreSubito = false
                                    )
                                }

                                toggleProxiesInCorso =
                                    toggleProxiesInCorso - nomeProxy
                            }
                        }
                    )

                    if (indice < elencoProxies.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (!mostraConnessione && mostraClients && elencoUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                TitoloSezioneDashboard(
                    titolo = "Users",
                    icona = Icons.Default.Devices,
                    coloreAccento = Color(0xFFFFA726),
                    compatto = schermoCompatto
                )

                Spacer(modifier = Modifier.height(8.dp))

                elencoUsers.forEachIndexed { indice, user ->
                    ClientInfoCard(
                        testo = user,
                        compatto = schermoCompatto,
                        mostraToggleUser = true,
                        userAbilitato = userAbilitati[
                            user.lineSequence()
                                .firstOrNull()
                                ?.substringBeforeLast("—")
                                ?.trim()
                                .orEmpty()
                        ] ?: true,
                        toggleUserInCorso =
                            user.lineSequence()
                                .firstOrNull()
                                ?.substringBeforeLast("—")
                                ?.trim()
                                .orEmpty() in toggleUsersInCorso,
                        onToggleUser = {
                            val nomeUser =
                                user.lineSequence()
                                    .firstOrNull()
                                    ?.substringBeforeLast("—")
                                    ?.trim()
                                    .orEmpty()

                            val attualmenteAbilitato =
                                userAbilitati[nomeUser] ?: true

                            toggleUsersInCorso =
                                toggleUsersInCorso + nomeUser

                            coroutineScope.launch {
                                val esito =
                                    withContext(Dispatchers.IO) {
                                        api.impostaUserAbilitato(
                                            host = host.trim(),
                                            porta = porta.trim(),
                                            username = username,
                                            password = password,
                                            nomeUser = nomeUser,
                                            abilita = !attualmenteAbilitato
                                        )
                                    }

                                if (!esito.startsWith("ERRORE:")) {
                                    dashboardViewModel
                                        ?.aggiornaUserAbilitatoLocale(
                                            nomeUser = nomeUser,
                                            abilitato =
                                                !attualmenteAbilitato
                                        )

                                    kotlinx.coroutines.delay(1500)

                                    aggiornaDashboardDaOscam(
                                        mostraErroreSubito = false
                                    )
                                }

                                toggleUsersInCorso =
                                    toggleUsersInCorso - nomeUser
                            }
                        }
                    )

                    if (indice < elencoUsers.lastIndex) {
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            }

            if (!mostraConnessione && mostraClients && elencoClients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                TitoloSezioneDashboard(
                    titolo = "Client connessi",
                    icona = Icons.Default.Devices,
                    coloreAccento = Color(0xFFFFA726),
                    compatto = schermoCompatto
                )

                Spacer(modifier = Modifier.height(8.dp))

                elencoClients.forEachIndexed { indice, client ->
                    ClientInfoCard(
                        testo = client,
                        compatto = schermoCompatto,
                        onHeightIncrease = { incremento ->
                            val distanzaDalFondo =
                                scrollHome.maxValue - scrollHome.value

                            if (
                                schermoCompatto &&
                                distanzaDalFondo <= sogliaAutoscrollClient
                            ) {
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(50)

                                    scrollHome.animateScrollTo(
                                        (
                                            scrollHome.value +
                                                incremento
                                        ).coerceAtMost(
                                            scrollHome.maxValue
                                        )
                                    )
                                }
                            }
                        }
                    )

                    if (indice < elencoClients.lastIndex) {
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            }
        }
        }

        if (refreshHomeAttivo) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = refreshServerInCorso,
                onRefresh = {
                    refreshServerInCorso = true
                    refreshServerSalvati++
                },
                modifier = Modifier.fillMaxSize()
            ) {
                contenutoPrincipale()
            }
        } else {
            contenutoPrincipale()
        }

        if (mostraInformazioni) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                InformazioniScreen(
                    onClose = {
                        mostraInformazioni = false
                    },
                    onOpenGitHub = {
                        mostraInformazioni = false
                        mostraRepositoryGitHub = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (mostraRepositoryGitHub) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                GitHubRepositoryScreen(
                    onClose = {
                        mostraRepositoryGitHub = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (mostraWebIf) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                WebIfScreen(
                    host = host,
                    porta = porta,
                    username = username,
                    password = password,
                    onClose = {
                        mostraWebIf = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (mostraLiveLog) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LiveLogScreen(
                    host = host,
                    porta = porta,
                    username = username,
                    password = password,
                    onClose = {
                        mostraLiveLog = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (mostraStrumenti) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val serverTerminale =
                    serverSalvati.firstOrNull {
                        it.host.trim() == host.trim() &&
                        it.porta.trim() == porta.trim()
                    }

                TerminalToolsScreen(
                    serverKey = "${host.trim()}_${porta.trim()}",
                    serverName = serverTerminale?.nome ?: host.trim(),
                    defaultHost = host.trim(),
                    onClose = {
                        mostraStrumenti = false
                        mostraConnessione = true
                    },
                    modifier = Modifier.fillMaxSize(),
                    onEditorLandscapeImeChange =
                        onEditorLandscapeImeChange
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}


@Composable
fun LiveLogScreen(
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

    val api = remember { OscamApi() }

    var righeLog by remember(host, porta, username, password) {
        mutableStateOf(emptyList<String>())
    }

    var ultimoId by remember(host, porta, username, password) {
        mutableStateOf("start")
    }

    var inPausa by remember { mutableStateOf(false) }
    var erroreLiveLog by remember { mutableStateOf("") }
    var erroriLiveLogConsecutivi by remember { mutableStateOf(0) }
    var riconnessioneLiveLog by remember { mutableStateOf(false) }
    var primaRichiestaCompletata by remember { mutableStateOf(false) }

    val scrollVerticale = rememberScrollState()
    val scrollOrizzontale = rememberScrollState()

    LaunchedEffect(
        host,
        porta,
        username,
        password,
        inPausa
    ) {
        if (!inPausa) {
            while (true) {
                val risposta = withContext(Dispatchers.IO) {
                    api.scaricaLiveLog(
                        host = host,
                        porta = porta,
                        username = username,
                        password = password,
                        lastId = ultimoId
                    )
                }

                primaRichiestaCompletata = true

                if (risposta.startsWith("ERRORE:")) {
                    erroriLiveLogConsecutivi += 1

                    if (erroriLiveLogConsecutivi >= 3) {
                        riconnessioneLiveLog = false
                        erroreLiveLog = risposta
                    } else {
                        riconnessioneLiveLog = true
                        erroreLiveLog = ""
                    }
                } else {
                    val risultato = api.estraiLiveLog(risposta)

                    if (!risultato.valido) {
                        erroriLiveLogConsecutivi = 3
                        riconnessioneLiveLog = false
                        erroreLiveLog =
                            "Live Log non disponibile su questo server"
                    } else {
                        erroriLiveLogConsecutivi = 0
                        riconnessioneLiveLog = false
                        erroreLiveLog = ""

                        if (risultato.ultimoId.isNotBlank()) {
                            ultimoId = risultato.ultimoId
                        }

                        if (risultato.righe.isNotEmpty()) {
                            righeLog =
                                (righeLog + risultato.righe)
                                    .takeLast(300)
                        }
                    }
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    LaunchedEffect(righeLog.size, inPausa) {
        if (!inPausa && righeLog.isNotEmpty()) {
            kotlinx.coroutines.delay(40)

            scrollVerticale.scrollTo(
                scrollVerticale.maxValue
            )
        }
    }

    val temaScuroLive =
        androidx.compose.foundation.isSystemInDarkTheme()

    val coloreLive =
        when {
            inPausa ->
                Color(0xFFFFB74D)

            erroreLiveLog.isNotBlank() ->
                Color(0xFFEF5350)

            riconnessioneLiveLog ->
                Color(0xFFFFB74D)

            else ->
                Color(0xFF66BB6A)
        }

    val statoLive =
        when {
            inPausa ->
                "PAUSA"

            erroreLiveLog.isNotBlank() ->
                "ERRORE"

            riconnessioneLiveLog ->
                "RICONNESSIONE"

            primaRichiestaCompletata ->
                "LIVE"

            else ->
                "CONNESSIONE"
        }

    val coloreNormaleLog =
        if (temaScuroLive) {
            Color(0xFFD7E0D8)
        } else {
            Color(0xFF263229)
        }

    val testoLogColorato =
        remember(
            righeLog,
            erroreLiveLog,
            temaScuroLive
        ) {
            androidx.compose.ui.text.buildAnnotatedString {
                if (righeLog.isNotEmpty()) {
                    righeLog.forEachIndexed { indice, riga ->
                        val colore =
                            when {
                                riga.contains(
                                    "ERROR",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "FAIL",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "NOT FOUND",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "REJECTED",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFFEF5350)

                                riga.contains(
                                    "WARN",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "TIMEOUT",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFFFFB74D)

                                riga.contains(
                                    "EMM",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFF42A5F5)

                                riga.contains(
                                    "ECM",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "FOUND",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "CARDOK",
                                    ignoreCase = true
                                ) ||
                                riga.contains(
                                    "CONNECTED",
                                    ignoreCase = true
                                ) ->
                                    Color(0xFF66BB6A)

                                else ->
                                    coloreNormaleLog
                            }

                        pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = colore
                            )
                        )

                        append(riga)

                        pop()

                        if (indice < righeLog.lastIndex) {
                            append("\n")
                        }
                    }
                } else {
                    pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = coloreNormaleLog
                        )
                    )

                    append(
                        if (erroreLiveLog.isNotBlank()) {
                            "Nessun dato Live Log"
                        } else {
                            "In attesa delle righe OSCam..."
                        }
                    )

                    pop()
                }
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            )
    ) {
        androidx.compose.material3.Surface(
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.55f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 9.dp
                    ),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onClose,
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            14.dp
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = 10.dp,
                            vertical = 7.dp
                        )
                ) {
                    Text("✕")
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Live Log",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${host.trim()}:${porta.trim()}",
                        fontSize = 11.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            50
                        ),
                    color = coloreLive.copy(alpha = 0.12f),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            coloreLive.copy(alpha = 0.45f)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.size(7.dp),
                            shape =
                                androidx.compose.foundation.shape.CircleShape,
                            color = coloreLive
                        ) {}

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = statoLive,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = coloreLive
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        15.dp
                    ),
                onClick = {
                    inPausa = !inPausa
                }
            ) {
                Text(
                    text =
                        if (inPausa) {
                            "▶  Riprendi"
                        } else {
                            "⏸  Pausa"
                        },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        15.dp
                    ),
                onClick = {
                    righeLog = emptyList()
                }
            ) {
                Text(
                    text = "⌫  Pulisci",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (erroreLiveLog.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = erroreLiveLog,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color =
                if (temaScuroLive) {
                    Color(0xFF0B0F0C)
                } else {
                    Color(0xFFF4F7F4)
                },
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.65f
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 11.dp,
                            vertical = 7.dp
                        ),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "OSCam LOG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF66BB6A)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${righeLog.size}/300 righe",
                        fontSize = 10.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.45f
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(10.dp)
                        .verticalScroll(scrollVerticale)
                        .horizontalScroll(scrollOrizzontale)
                ) {
                    Text(
                        text = testoLogColorato,
                        fontFamily =
                            androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}



private data class RisultatoAggiornamento(
    val versioneDisponibile: String,
    val aggiornamentoDisponibile: Boolean,
    val downloadUrl: String?,
    val releaseUrl: String
)

private val githubUpdateClient = OkHttpClient()

private fun confrontaVersioni(
    versioneAttuale: String,
    versioneNuova: String
): Int {
    val attuale = versioneAttuale
        .removePrefix("v")
        .split(".")
        .map { it.toIntOrNull() ?: 0 }

    val nuova = versioneNuova
        .removePrefix("v")
        .split(".")
        .map { it.toIntOrNull() ?: 0 }

    val lunghezza = maxOf(attuale.size, nuova.size)

    for (indice in 0 until lunghezza) {
        val a = attuale.getOrElse(indice) { 0 }
        val n = nuova.getOrElse(indice) { 0 }

        if (a < n) return -1
        if (a > n) return 1
    }

    return 0
}

private suspend fun controllaAggiornamentoGitHub(
    versioneAttuale: String,
    variante: String
): RisultatoAggiornamento = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://api.github.com/repos/mr-unix01/OscamLiveMonitor/releases/latest")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "OSCam-Live-Monitor")
        .build()

    githubUpdateClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            if (response.code == 404) {
                throw IllegalStateException(
                    "Nessuna release stabile pubblica disponibile."
                )
            }

            throw IllegalStateException(
                "GitHub ha restituito l'errore HTTP ${response.code}."
            )
        }

        val body = response.body?.string()
            ?: throw IllegalStateException("Risposta GitHub vuota.")

        val json = JSONObject(body)

        val tag = json.optString("tag_name").ifBlank {
            throw IllegalStateException("Versione della release non disponibile.")
        }

        val versioneDisponibile = tag.removePrefix("v")
        val releaseUrl = json.optString("html_url")

        var downloadUrl: String? = null
        val assets = json.optJSONArray("assets")

        if (assets != null) {
            for (indice in 0 until assets.length()) {
                val asset = assets.optJSONObject(indice) ?: continue
                val nome = asset.optString("name")

                if (
                    nome.endsWith(".apk", ignoreCase = true) &&
                    nome.contains(variante, ignoreCase = true)
                ) {
                    downloadUrl = asset.optString("browser_download_url")
                        .takeIf { it.isNotBlank() }
                    break
                }
            }
        }

        RisultatoAggiornamento(
            versioneDisponibile = versioneDisponibile,
            aggiornamentoDisponibile =
                confrontaVersioni(versioneAttuale, versioneDisponibile) < 0,
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl
        )
    }
}

@Composable
fun InformazioniScreen(
    onClose: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onClose()
    }

    val context = LocalContext.current

    val packageInfo = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        }
    }

    val versioneApp = packageInfo.versionName ?: "—"

    val buildApp =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }

    val coroutineScope = rememberCoroutineScope()
    var controlloAggiornamentoInCorso by remember { mutableStateOf(false) }
    var risultatoAggiornamento by remember {
        mutableStateOf<RisultatoAggiornamento?>(null)
    }
    var erroreAggiornamento by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(erroreAggiornamento) {
        if (erroreAggiornamento == "Nessuna release stabile pubblica disponibile.") {
            kotlinx.coroutines.delay(3000)
            erroreAggiornamento = null
        }
    }

    LaunchedEffect(risultatoAggiornamento) {
        if (risultatoAggiornamento?.aggiornamentoDisponibile == false) {
            kotlinx.coroutines.delay(3000)
            risultatoAggiornamento = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(9.dp),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color =
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Informazioni",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.20f)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .padding(22.dp)
                        .size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OSCam Live Monitor",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Monitor Android per OSCam",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Versione",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = versioneApp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Build",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildApp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Autore",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "mr-unix",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            onClick = onOpenGitHub
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = R.drawable.ic_github
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Repository GitHub")
        }

        Spacer(modifier = Modifier.height(10.dp))

        androidx.compose.material3.Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            enabled = !controlloAggiornamentoInCorso,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF2E7D32).copy(alpha = 0.45f),
                disabledContentColor = Color.White.copy(alpha = 0.70f)
            ),
            onClick = {
                controlloAggiornamentoInCorso = true
                risultatoAggiornamento = null
                erroreAggiornamento = null

                coroutineScope.launch {
                    try {
                        risultatoAggiornamento =
                            controllaAggiornamentoGitHub(
                                versioneAttuale = versioneApp,
                                variante = BuildConfig.VARIANTE_APP
                            )
                    } catch (e: Exception) {
                        erroreAggiornamento =
                            e.message ?: "Impossibile controllare gli aggiornamenti."
                    } finally {
                        controlloAggiornamentoInCorso = false
                    }
                }
            }
        ) {
            if (controlloAggiornamentoInCorso) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Controllo in corso...")
            } else {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Controlla aggiornamenti")
            }
        }

        risultatoAggiornamento?.let { risultato ->
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text =
                    if (risultato.aggiornamentoDisponibile) {
                        "Nuova versione disponibile: ${risultato.versioneDisponibile}"
                    } else {
                        "Hai già l'ultima versione disponibile."
                    },
                fontSize = 13.sp,
                fontWeight =
                    if (risultato.aggiornamentoDisponibile) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                color =
                    if (risultato.aggiornamentoDisponibile) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (
                risultato.aggiornamentoDisponibile &&
                risultato.downloadUrl != null
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(risultato.downloadUrl)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Scarica aggiornamento")
                }
            }
        }

        erroreAggiornamento?.let { errore ->
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = errore,
                fontSize = 13.sp,
                color =
                    if (errore == "Nessuna release stabile pubblica disponibile.") {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "OSCam Live Monitor è un progetto indipendente e non ufficiale.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}


@Composable
fun GitHubRepositoryScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onClose()
    }

    var caricamento by remember { mutableStateOf(true) }
    var erroreCaricamento by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(9.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = R.drawable.ic_github
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
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
                        text = "Repository GitHub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "mr-unix01/OscamLiveMonitor",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (!erroreCaricamento) {
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webContext ->
                        android.webkit.WebView(webContext).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true

                            webViewClient =
                                object : android.webkit.WebViewClient() {
                                    override fun onPageStarted(
                                        view: android.webkit.WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?
                                    ) {
                                        caricamento = true
                                        erroreCaricamento = false
                                    }

                                    override fun onPageFinished(
                                        view: android.webkit.WebView?,
                                        url: String?
                                    ) {
                                        caricamento = false
                                    }

                                    override fun onReceivedError(
                                        view: android.webkit.WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        error: android.webkit.WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            caricamento = false
                                            erroreCaricamento = true
                                        }
                                    }
                                }

                            loadUrl(
                                "https:" + "//github.com/" +
                                    "mr-unix01/OscamLiveMonitor"
                            )
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment =
                        androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Impossibile caricare la repository",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Controlla la connessione Internet e riprova.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (caricamento && !erroreCaricamento) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .size(36.dp),
                    strokeWidth = 3.dp,
                    color = Color(0xFF66BB6A)
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
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            shape =
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.55f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(9.dp),
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            14.dp
                        ),
                    color =
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            13.dp
                        ),
                    color = Color(0xFF29B6F6).copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF29B6F6),
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
                        text = "WebIF OSCam",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${host.trim()}:${porta.trim()}",
                        fontSize = 12.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                androidx.compose.material3.Surface(
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            50
                        ),
                    color = Color(0xFF4CAF50).copy(alpha = 0.11f),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF4CAF50).copy(alpha = 0.40f)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.size(7.dp),
                            shape =
                                androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFF66BB6A)
                        ) {}

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = "ONLINE",
                            color = Color(0xFF66BB6A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
                                            OSCam Live Monitor ha già provato a riconnettersi.
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OscamLiveMonitorTheme {
        Greeting("OSCam Live Monitor")
    }
}

