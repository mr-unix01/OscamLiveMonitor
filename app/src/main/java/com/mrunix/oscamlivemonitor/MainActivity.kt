
package com.mrunix.oscamlivemonitor

import com.mrunix.oscamlivemonitor.ui.theme.OscamPalette

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.clickable

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(
            AppLanguage.localizedBaseContext(newBase)
        )
    }

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
            val preferenzeTema = remember {
                getSharedPreferences(
                    "oscam_monitor",
                    android.content.Context.MODE_PRIVATE
                )
            }

            var temaApp by remember {
                mutableStateOf(
                    preferenzeTema.getString("tema_app", "system")
                        ?: "system"
                )
            }

            val temaSistemaScuro =
                androidx.compose.foundation.isSystemInDarkTheme()

            val temaEffettivoScuro = when (temaApp) {
                "light" -> false
                "dark" -> true
                else -> temaSistemaScuro
            }

            OscamLiveMonitorTheme(
                darkTheme = temaEffettivoScuro
            ) {
                var editorLandscapeIme by remember {
                    mutableStateOf(false)
                }

                val window = this@MainActivity.window

                DisposableEffect(editorLandscapeIme, temaEffettivoScuro) {
                    val controller =
                        androidx.core.view.WindowCompat.getInsetsController(
                            window,
                            window.decorView
                        )

                    controller.isAppearanceLightStatusBars =
                        !temaEffettivoScuro
                    controller.isAppearanceLightNavigationBars =
                        !temaEffettivoScuro

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
                        },
                        temaApp = temaApp,
                        linguaApp = AppLanguage.selected(this@MainActivity),
                        onLinguaAppChange = { scelta ->
                            AppLanguage.change(this@MainActivity, scelta)
                        },
                        onTemaAppChange = { scelta ->
                            if (scelta in listOf("system", "light", "dark")) {
                                temaApp = scelta
                                preferenzeTema.edit()
                                    .putString("tema_app", scelta)
                                    .apply()
                            }
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
    onEditorLandscapeImeChange: (Boolean) -> Unit = {},
    temaApp: String = "system",
    onTemaAppChange: (String) -> Unit = {},
    linguaApp: String = AppLanguage.SYSTEM,
    onLinguaAppChange: (String) -> Unit = {}
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

    val temaScuro = com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
    val schermoCompatto =
        BuildConfig.TABLET_MODE ||
                androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 500

    val preferences = remember {
        context.getSharedPreferences(
            "oscam_monitor",
            android.content.Context.MODE_PRIVATE
        )
    }

    var schermoSempreAcceso by remember {
        mutableStateOf(
            preferences.getBoolean("schermo_sempre_acceso", false)
        )
    }

    val vistaApp = androidx.compose.ui.platform.LocalView.current

    DisposableEffect(vistaApp, schermoSempreAcceso) {
        vistaApp.keepScreenOn = schermoSempreAcceso
        onDispose {
            vistaApp.keepScreenOn = false
        }
    }

    var aggiornamentoAutomaticoDashboard by remember {
        mutableStateOf(
            preferences.getBoolean("dashboard_auto_refresh", true)
        )
    }

    var intervalloDashboardSecondi by remember {
        mutableStateOf(
            preferences.getInt("dashboard_refresh_seconds", 5)
                .takeIf { it in listOf(5, 10, 15, 30, 60) } ?: 5
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
    var mostraImpostazioni by rememberSaveable { mutableStateOf(false) }
    var mostraInformazioni by rememberSaveable { mutableStateOf(false) }
    var mostraServerHub by rememberSaveable { mutableStateOf(false) }

    // Menu superiore HOME, indipendente dai singoli server.
    var menuSuperioreAperto by rememberSaveable { mutableStateOf(false) }
    var trascinamentoMenu by remember { mutableFloatStateOf(0f) }

    var sezioneStrumentiIniziale by remember {
        mutableStateOf(TerminalToolsInitialSection.TERMINAL)
    }

    var serverSalvati by remember {
        mutableStateOf(caricaServerSalvati(preferences))
    }

    var serverIdAttivo by remember {
        val idSalvato =
            preferences.getString(
                "server_id",
                ""
            ).orEmpty()

        val nomeSalvato =
            preferences.getString(
                "nome_server",
                ""
            ).orEmpty()

        val serverIniziale =
            serverSalvati.firstOrNull { server ->
                server.id == idSalvato
            } ?: serverSalvati.firstOrNull { server ->
                server.nome == nomeSalvato &&
                    server.host.trim() == host.trim() &&
                    server.porta.trim() == porta.trim()
            } ?: serverSalvati.firstOrNull { server ->
                server.host.trim() == host.trim() &&
                    server.porta.trim() == porta.trim()
            }

        mutableStateOf(
            serverIniziale?.id.orEmpty()
        )
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

    fun apriDashboardServer() {
        if (!permessoReteConcesso) {
            val messaggio =
                "Concedi prima l'accesso alla rete locale"

            impostaStatoDashboard(messaggio)
            mostraSnackbar(messaggio)
            mostraSpiegazionePermesso = true
            return
        }

        if (host.isBlank() || porta.isBlank()) {
            val messaggio =
                context.getString(R.string.main_dashboard_host_port)

            impostaStatoDashboard(messaggio)
            mostraSnackbar(messaggio)
            return
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
        impostaStatoDashboard(context.getString(R.string.main_dashboard_connecting))

        mostraServers = true
        mostraReaders = true
        mostraProxies = true
        mostraClients = true

        coroutineScope.launch {
            if (aggiornaDashboardDaOscam()) {
                mostraServerHub = false
                mostraConnessione = false
            }
        }
    }

    fun tornaAlPannelloDaDashboard() {
        mostraConnessione = true
        mostraServerHub = true

        dashboardViewModel?.resetDashboardState()
        impostaStatoDashboard("")

        mostraServers = true
        mostraReaders = true
        mostraProxies = true
        mostraClients = true
    }

    val richiestaPermessoRete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concesso ->
        permessoReteConcesso = concesso

        if (concesso) {
            mostraSnackbar(context.getString(R.string.network_permission_granted))
        } else {
            val nuovoStato =
                context.getString(R.string.network_permission_required)

            impostaStatoDashboard(nuovoStato)
            mostraSnackbar(nuovoStato)
        }
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
                val chiave = server.id
                statoServerSalvati[chiave] = OscamServerConnectionState.CHECKING
            }

            while (true) {
                kotlinx.coroutines.coroutineScope {
                    serverSalvati.forEach { server ->
                        launch {
                            val chiave =
                                server.id

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
                                chiave == serverIdAttivo &&
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
                                        context.getString(R.string.main_server_back_online, server.nome)
                                    OscamServerConnectionState.AUTH_ERROR ->
                                        context.getString(R.string.main_server_auth_error, server.nome)
                                    OscamServerConnectionState.REACHABLE_NO_OSCAM ->
                                        context.getString(R.string.main_server_no_oscam_response, server.nome)
                                    OscamServerConnectionState.UNREACHABLE ->
                                        context.getString(R.string.main_server_unreachable, server.nome)
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

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel
            ?.connessionePersaEventi
            ?.collect {
                mostraSnackbar(context.getString(R.string.main_connection_lost))
            }
    }

    DisposableEffect(
        mostraConnessione,
        mostraWebIf,
        mostraLiveLog,
        riavvioInCorso,
        host,
        porta,
        username,
        password,
        dashboardViewModel,
        aggiornamentoAutomaticoDashboard,
        intervalloDashboardSecondi,
        mostraImpostazioni
    ) {
        val pollingAttivo =
            !mostraConnessione &&
                !mostraWebIf &&
                !mostraLiveLog &&
                !mostraImpostazioni &&
                !riavvioInCorso &&
                aggiornamentoAutomaticoDashboard

        if (pollingAttivo) {
            // Primo refresh immediato: quando si chiude WebIF/Live Log
            // la dashboard non aspetta più 5 secondi prima di aggiornarsi.
            dashboardViewModel?.avviaPolling(
                host = host,
                porta = porta,
                username = username,
                password = password,
                intervalloSecondi = intervalloDashboardSecondi
            )
        } else {
            dashboardViewModel?.fermaPolling()
        }

        onDispose {
            dashboardViewModel?.fermaPolling()
        }
    }

    if (mostraConfermaRiavvio) {
        val serverAttivo =
            serverSalvati.firstOrNull { server ->
                server.id == serverIdAttivo
            }

        val nomeServerAttivo =
            serverAttivo?.nome
                ?: host.trim()
                    .ifBlank { context.getString(R.string.main_dashboard_active_server) }

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
                        context.getString(R.string.main_dashboard_host_port_first)
                    impostaStatoDashboard(messaggio)
                    mostraSnackbar(messaggio)
                } else {
                    riavvioInCorso = true
                    impostaStatoDashboard(context.getString(R.string.main_restarting_oscam))

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
                            context.getString(R.string.main_reconnecting_oscam)
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
                                    context.getString(R.string.main_reconnecting_attempt, tentativo)
                                )
                                kotlinx.coroutines.delay(2000)
                            }
                        }

                        riavvioInCorso = false

                        if (riconnesso) {
                            mostraConnessione = false
                            impostaStatoDashboard(context.getString(R.string.main_dashboard_connected))
                        } else {
                            val messaggio =
                                context.getString(R.string.main_restart_unreachable_error)
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
                        context.getString(R.string.main_test_invalid_host_port)
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
                                context.getString(R.string.main_test_invalid_response)
                            )
                        } else {
                            mostraSnackbarDialog(
                                context.getString(R.string.main_test_connection_success)
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
                        context.getString(R.string.main_server_required_fields)
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

                    serverIdAttivo = nuovoServer.id
                    host = nuovoServer.host
                    porta = nuovoServer.porta
                    username = nuovoServer.username
                    password = nuovoServer.password

                    preferences.edit()
                        .putString(
                            "server_id",
                            nuovoServer.id
                        )
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
                        context.getString(R.string.main_server_added, nuovoServer.nome)
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
                        context.getString(R.string.main_server_required_fields)
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
                            serverIdAttivo == serverOriginale.id

                        if (eraSelezionato) {
                            host =
                                serverAggiornato.host

                            porta =
                                serverAggiornato.porta

                            username =
                                serverAggiornato.username

                            password =
                                serverAggiornato.password

                            serverIdAttivo =
                                serverAggiornato.id

                            preferences.edit()
                                .putString(
                                    "server_id",
                                    serverAggiornato.id
                                )
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
                            context.getString(R.string.main_server_modified, serverAggiornato.nome)
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
                            serverIdAttivo == serverOriginale.id

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
                                serverIdAttivo =
                                    nuovoSelezionato.id

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
                                        "server_id",
                                        nuovoSelezionato.id
                                    )
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
                                serverIdAttivo = ""
                                host = ""
                                porta = ""
                                username = ""
                                password = ""

                                preferences.edit()
                                    .remove("server_id")
                                    .remove("nome_server")
                                    .remove("host")
                                    .remove("porta")
                                    .remove("username")
                                    .remove("password")
                                    .apply()
                            }
                        }

                        mostraSnackbar(
                            context.getString(R.string.main_server_deleted, serverOriginale.nome)
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

    androidx.activity.compose.BackHandler(
        enabled =
            (mostraServerHub || !mostraConnessione) &&
            !mostraImpostazioni &&
            !mostraInformazioni &&
            !mostraRepositoryGitHub &&
            !mostraWebIf &&
            !mostraLiveLog &&
            !mostraStrumenti
    ) {
        if (mostraServerHub) {
            mostraServerHub = false
        } else {
            tornaAlPannelloDaDashboard()
        }
    }

    androidx.activity.compose.BackHandler(enabled = menuSuperioreAperto) {
        menuSuperioreAperto = false
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

        val contenutoPrincipale: @Composable () -> Unit = contenuto@ {
            if (mostraConnessione) {
                val temaScuroHome =
                    com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
                val sfondoHome =
                    if (temaScuroHome) OscamPalette.SheetDark else Color.White
                val bluHome =
                    if (temaScuroHome) OscamPalette.BannerDark else OscamPalette.BannerLight

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(sfondoHome)
                        .imePadding(),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        ExpressiveAppHeader(
                            compatto = schermoCompatto,
                            statiServer = serverSalvati.map { server ->
                                statoServerSalvati[server.id]
                                    ?: OscamServerConnectionState.CHECKING
                            },
                            onMenuClick = {
                                trascinamentoMenu = 0f
                                menuSuperioreAperto = true
                            }
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bluHome)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = sfondoHome,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 28.dp,
                                            topEnd = 28.dp
                                        )
                                    )
                            ) {
                                ServerSelectionHeader(
                                    modifier = Modifier.padding(
                                        start = if (schermoCompatto) 12.dp else 16.dp,
                                        end = if (schermoCompatto) 12.dp else 16.dp,
                                        top = 8.dp,
                                        bottom = 12.dp
                                    ),
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
                            }
                        }
                    }

                    if (serverSalvati.isEmpty()) {
                        item {
                            EmptyServerState(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(
                            count = serverSalvati.size,
                            key = { index ->
                                serverSalvati[index].id
                            }
                        ) { index ->
                            val server = serverSalvati[index]

                            val serverSelezionato =
                                server.id == serverIdAttivo

                            OscamServerCard(
                                modifier = Modifier.padding(
                                    horizontal = if (schermoCompatto) 12.dp else 16.dp,
                                    vertical = 4.dp
                                ),
                                server = server,
                                showDivider = index < serverSalvati.lastIndex,
                                selected = serverSelezionato,
                                connectionState =
                                    statoServerSalvati[
                                        server.id
                                    ] ?: OscamServerConnectionState.CHECKING,
                                onSelect = {
                                    serverIdAttivo = server.id
                                    host = server.host
                                    porta = server.porta
                                    username = server.username
                                    password = server.password

                                    preferences.edit()
                                        .putString(
                                            "server_id",
                                            server.id
                                        )
                                        .putString(
                                            "nome_server",
                                            server.nome
                                        )
                                        .putString(
                                            "host",
                                            server.host
                                        )
                                        .putString(
                                            "porta",
                                            server.porta
                                        )
                                        .putString(
                                            "username",
                                            server.username
                                        )
                                        .putString(
                                            "password",
                                            SecurePasswordCrypto.encrypt(
                                                server.password
                                            )
                                        )
                                        .apply()

                                    impostaStatoDashboard("")
                                },
                                onOpen = {
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(180)
                                        mostraServerHub = true
                                    }
                                },
                                onEdit = {
                                    serverDaModificare = server
                                    modificaNomeServer = server.nome
                                    modificaHostServer = server.host
                                    modificaPortaServer = server.porta
                                    modificaUsernameServer =
                                        server.username
                                    modificaPasswordServer =
                                        server.password
                                    mostraPasswordModifica = false
                                },
                                onReorder = {
                                    mostraRiordinaServer = true
                                },
                                onDelete = {
                                    serverDaEliminare = server
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                return@contenuto
            }

            val scuroDashboardGrafica =
                com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
            val bluDashboardGrafica =
                if (scuroDashboardGrafica) OscamPalette.BannerDark else OscamPalette.BannerLight
            val sfondoDashboardGrafica =
                if (scuroDashboardGrafica) OscamPalette.SheetDark else Color.White

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(sfondoDashboardGrafica)
                    .verticalScroll(scrollHome)
                    .imePadding()
            ) {
            val serverAttivoDashboard =
                serverSalvati.firstOrNull { server ->
                    server.id == serverIdAttivo
                }

            val nomeServerDashboard =
                serverAttivoDashboard?.nome
                    ?: preferences.getString("nome_server", null)
                        ?.takeIf { it.isNotBlank() }
                    ?: host.trim()

            val descrizioneStatoSconosciuto = stringResource(R.string.dashboard_unknown_status)

            val statoPerAccessibilita = when {
                stato.equals("Connesso", ignoreCase = true) ->
                    context.getString(R.string.main_dashboard_connected)
                stato.equals("Connessione persa", ignoreCase = true) ->
                    context.getString(R.string.main_connection_lost)
                else -> stato
            }

            val coloreStatoDashboard = when {
                stato.equals("Connesso", ignoreCase = true) ||
                    stato.equals("Connected", ignoreCase = true) ||
                    stato.contains("selezionato", ignoreCase = true) ->
                    Color(0xFF7DDB9A)

                stato.contains("autentic", ignoreCase = true) ||
                    stato.contains("authentication", ignoreCase = true) ->
                    Color(0xFFFFB300)

                stato.startsWith("ERRORE:", ignoreCase = true) ||
                    stato.contains("persa", ignoreCase = true) ||
                    stato.contains("non raggiungibile", ignoreCase = true) ||
                    stato.contains("necessario", ignoreCase = true) ||
                    stato.contains("lost", ignoreCase = true) ||
                    stato.contains("unreachable", ignoreCase = true) ->
                    Color(0xFFEF5350)

                stato.contains("Connessione", ignoreCase = true) ||
                    stato.contains("riconnessione", ignoreCase = true) ||
                    stato.contains("Riavvio", ignoreCase = true) ||
                    stato.contains("verifica", ignoreCase = true) ||
                    stato.contains("Connecting", ignoreCase = true) ||
                    stato.contains("Reconnecting", ignoreCase = true) ||
                    stato.contains("Restarting", ignoreCase = true) ->
                    Color(0xFFFFB74D)

                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bluDashboardGrafica)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 26.dp
                    ),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    modifier = Modifier.size(46.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    onClick = {
                        tornaAlPannelloDaDashboard()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.dashboard_back_panel),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = nomeServerDashboard,
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = if (schermoCompatto) 20.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    coloreStatoDashboard,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                                .semantics {
                                    contentDescription =
                                        if (stato.isBlank()) {
                                            descrizioneStatoSconosciuto
                                        } else {
                                            statoPerAccessibilita
                                        }
                                }
                        )
                    }

                    Text(
                        text = "${host.trim()}:${porta.trim()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bluDashboardGrafica)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            sfondoDashboardGrafica,
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 28.dp
                            )
                        )
                        .padding(
                            horizontal = if (schermoCompatto) 12.dp else 16.dp,
                            vertical = if (schermoCompatto) 10.dp else 16.dp
                        )
                ) {
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
                                context.getString(R.string.main_dashboard_host_port_first)
                            impostaStatoDashboard(messaggio)
                            mostraSnackbar(messaggio)
                        } else {
                            mostraConfermaRiavvio = true
                        }
                    }
                )
            }

            if (!mostraConnessione && ultimoAggiornamento.isNotBlank()) {
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
                            contentDescription = stringResource(R.string.dashboard_refresh),
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
                        text = stringResource(R.string.dashboard_updated_at, ultimoAggiornamento),
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
                        titolo = stringResource(R.string.dashboard_users_clients),
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
                                        context.getString(R.string.dashboard_no_users)

                                    utentiConnessi == 0 ->
                                        if (totaleUtenti == 1) {
                                            context.getString(R.string.dashboard_no_clients_one)
                                        } else {
                                            context.getString(R.string.dashboard_no_clients_many, totaleUtenti)
                                        }

                                    totaleUtenti == 1 ->
                                        context.getString(R.string.dashboard_online_one)

                                    else ->
                                        context.getString(R.string.dashboard_online_many, totaleUtenti)
                                }
                            }
                    )
                }
            }

            if (!mostraConnessione && mostraServers && elencoServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                TitoloSezioneDashboard(
                    titolo = stringResource(R.string.dashboard_services),
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
                    titolo = stringResource(R.string.dashboard_readers_list),
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
                    titolo = stringResource(R.string.dashboard_proxies_list),
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
                    titolo = stringResource(R.string.dashboard_connected_clients),
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


        // Il menu si sovrappone alla HOME, senza spostare l'elenco dei server.
        // Il pannello segue il dito verso l'alto e poi scivola fuori dallo schermo.
        if (mostraConnessione && !mostraServerHub &&
            !mostraImpostazioni && !mostraInformazioni &&
            !mostraRepositoryGitHub && !mostraWebIf &&
            !mostraLiveLog && !mostraStrumenti
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = menuSuperioreAperto,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f))
                        .clickable { menuSuperioreAperto = false }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = menuSuperioreAperto,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { -it }
                ),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { -it }
                ),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopStart)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false
                            )
                            val touchSlop = viewConfiguration.touchSlop
                            var distanza = 0f
                            var trascina = false

                            do {
                                val event = awaitPointerEvent(
                                    pass = PointerEventPass.Initial
                                )
                                val change = event.changes.firstOrNull {
                                    it.id == down.id
                                } ?: break

                                if (!change.pressed) break

                                val dy = change.positionChange().y
                                distanza += dy

                                if (!trascina && distanza < -touchSlop) {
                                    trascina = true
                                }

                                if (trascina) {
                                    change.consume()
                                    trascinamentoMenu =
                                        (trascinamentoMenu + dy)
                                            .coerceAtMost(0f)
                                }
                            } while (true)

                            if (trascina &&
                                trascinamentoMenu < -72.dp.toPx()
                            ) {
                                menuSuperioreAperto = false
                            } else {
                                trascinamentoMenu = 0f
                            }
                        }
                    }
            ) {
                DisposableEffect(Unit) {
                    onDispose {
                        trascinamentoMenu = 0f
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                0, trascinamentoMenu.toInt()
                            )
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    color = if (com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current) OscamPalette.SheetDark else Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 14.dp)
                    ) {
                          val scuroMenu =
                              com.mrunix.oscamlivemonitor.ui.theme.LocalAppDarkTheme.current
                          val testoMenu =
                              if (scuroMenu) Color.White else Color(0xFF152235)
                          val iconaMenu =
                              if (scuroMenu) OscamPalette.AccentDark else OscamPalette.BannerLight
                          val sfondoIconaMenu =
                              if (scuroMenu) OscamPalette.PanelDark else OscamPalette.PanelLight
                          val separatoreMenu =
                              if (scuroMenu) OscamPalette.DividerDark else OscamPalette.DividerLight

                          Column(
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .padding(bottom = 12.dp),
                              horizontalAlignment = androidx.compose.ui.Alignment.Start
                          ) {
                              Image(
                                  painter = painterResource(R.drawable.oscam_drawer_logo),
                                  contentDescription = stringResource(R.string.info_logo),
                                  modifier = Modifier
                                      .size(40.dp)
                                      .clip(androidx.compose.foundation.shape.CircleShape)
                              )

                              Spacer(modifier = Modifier.height(8.dp))

                              Text(
                                  text = "OSCam Live Monitor",
                                  fontSize = 20.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = testoMenu
                              )
                          }

                          HorizontalDivider(color = separatoreMenu)
                          Spacer(modifier = Modifier.height(8.dp))

                          Row(
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .clickable {
                                      menuSuperioreAperto = false
                                      mostraImpostazioni = true
                                  }
                                  .padding(vertical = 12.dp),
                              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                          ) {
                              Surface(
                                  shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                                  color = sfondoIconaMenu
                              ) {
                                  Icon(
                                      imageVector = Icons.Default.Settings,
                                      contentDescription = null,
                                      tint = iconaMenu,
                                      modifier = Modifier.padding(10.dp).size(22.dp)
                                  )
                              }
                              Spacer(modifier = Modifier.width(12.dp))
                              Text(
                                  text = stringResource(R.string.settings_title),
                                  color = testoMenu,
                                  fontSize = 16.sp,
                                  fontWeight = FontWeight.SemiBold
                              )
                          }

                          HorizontalDivider(
                              modifier = Modifier.padding(start = 54.dp),
                              color = separatoreMenu
                          )

                          Row(
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .clickable {
                                      menuSuperioreAperto = false
                                      mostraInformazioni = true
                                  }
                                  .padding(vertical = 12.dp),
                              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                          ) {
                              Surface(
                                  shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                                  color = sfondoIconaMenu
                              ) {
                                  Icon(
                                      imageVector = Icons.Default.Info,
                                      contentDescription = null,
                                      tint = iconaMenu,
                                      modifier = Modifier.padding(10.dp).size(22.dp)
                                  )
                              }
                              Spacer(modifier = Modifier.width(12.dp))
                              Text(
                                  text = stringResource(R.string.info_title),
                                  color = testoMenu,
                                  fontSize = 16.sp,
                                  fontWeight = FontWeight.SemiBold
                              )
                          }
                    }
                }
            }
        }

        if (mostraServerHub) {
            val serverHub =
                serverSalvati.firstOrNull { server ->
                    server.id == serverIdAttivo
                }

            if (serverHub != null) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ServerHubScreen(
                        server = serverHub,
                        connectionState =
                            statoServerSalvati[
                                serverHub.id
                            ] ?: OscamServerConnectionState.CHECKING,
                        onBack = {
                            mostraServerHub = false
                        },
                        onDashboard = {
                            apriDashboardServer()
                        },
                        onWebIf = {
                            mostraWebIf = true
                        },
                        onLiveLog = {
                            mostraLiveLog = true
                        },
                        onTerminal = {
                            sezioneStrumentiIniziale =
                                TerminalToolsInitialSection.TERMINAL
                            mostraStrumenti = true
                        },
                        onFiles = {
                            sezioneStrumentiIniziale =
                                TerminalToolsInitialSection.FILES
                            mostraStrumenti = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (mostraImpostazioni) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ImpostazioniScreen(
                    onClose = {
                        mostraImpostazioni = false
                    },
                    temaApp = temaApp,
                    onTemaAppChange = onTemaAppChange,
                    linguaApp = linguaApp,
                    onLinguaAppChange = onLinguaAppChange,
                    schermoSempreAcceso = schermoSempreAcceso,
                    onSchermoSempreAccesoChange = { attivo ->
                        schermoSempreAcceso = attivo
                        preferences.edit()
                            .putBoolean("schermo_sempre_acceso", attivo)
                            .apply()
                    },
                    aggiornamentoAutomaticoDashboard =
                        aggiornamentoAutomaticoDashboard,
                    onAggiornamentoAutomaticoDashboardChange = {
                        aggiornamentoAutomaticoDashboard = it
                        preferences.edit()
                            .putBoolean("dashboard_auto_refresh", it)
                            .apply()
                    },
                    intervalloDashboardSecondi =
                        intervalloDashboardSecondi,
                    onIntervalloDashboardSecondiChange = {
                        intervalloDashboardSecondi = it
                        preferences.edit()
                            .putInt("dashboard_refresh_seconds", it)
                            .apply()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                        mostraInformazioni = true
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
                        it.id == serverIdAttivo
                    }

                TerminalToolsScreen(
                    serverKey =
                        serverTerminale?.id
                            ?: "${host.trim()}_${porta.trim()}",
                    serverName = serverTerminale?.nome ?: host.trim(),
                    defaultHost = host.trim(),
                    onClose = {
                        mostraStrumenti = false
                    },
                    modifier = Modifier.fillMaxSize(),
                    initialSection = sezioneStrumentiIniziale,
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


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OscamLiveMonitorTheme {
        Greeting("OSCam Live Monitor")
    }
}

