package com.mrunix.oscamlivemonitor

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class DashboardViewModel(
    private val api: OscamApi,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val refreshMutex = Mutex()

    private var pollingJob: Job? = null

    private val _connessionePersaEventi =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1
        )

    val connessionePersaEventi: SharedFlow<Unit> =
        _connessionePersaEventi.asSharedFlow()

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    fun resetDashboardState() {
        _uiState.value = DashboardUiState()
    }

    suspend fun aggiornaDashboard(
        host: String,
        porta: String,
        username: String,
        password: String,
        mostraErroreSubito: Boolean = true
    ): Boolean {
        refreshMutex.lock()

        try {
            val risultato =
                caricaStatusDashboard(
                    api = api,
                    host = host,
                    porta = porta,
                    username = username,
                    password = password
                )

            if (risultato.startsWith("ERRORE:")) {
                if (mostraErroreSubito) {
                    _uiState.value =
                        _uiState.value.copy(
                            stato = risultato
                        )
                }

                return false
            }

            val datiStatus =
                estraiDatiStatusDashboard(
                    api = api,
                    risultato = risultato
                )

            _uiState.value =
                _uiState.value.copy(
                    versioneOscam =
                        datiStatus.versioneOscam,
                    uptime =
                        datiStatus.uptime,
                    cpuOscam =
                        datiStatus.cpuOscam,
                    ramOscam =
                        datiStatus.ramOscam,
                    servers =
                        datiStatus.servers,
                    readers =
                        datiStatus.readers,
                    proxies =
                        datiStatus.proxies,
                    clients =
                        datiStatus.clients
                )

            val datiUsers =
                caricaDashboardUsers(
                    api = api,
                    host = host,
                    porta = porta,
                    username = username,
                    password = password
                )

            if (datiUsers != null) {
                _uiState.value =
                    _uiState.value.copy(
                        users = datiUsers.users
                    )

                if (
                    datiUsers.elencoUsers != null &&
                    datiUsers.userAbilitati != null
                ) {
                    _uiState.value =
                        _uiState.value.copy(
                            elencoUsers =
                                datiUsers.elencoUsers,
                            userAbilitati =
                                datiUsers.userAbilitati
                        )
                }
            }

            _uiState.value =
                _uiState.value.copy(
                    elencoServers =
                        datiStatus.elencoServers
                )

            val elencoReadersStatus =
                datiStatus.elencoReaders

            val elencoProxiesStatus =
                datiStatus.elencoProxies

            val chiaveTipiReaderProxy =
                chiaveTipiReaderProxyDashboard(
                    host = host,
                    porta = porta
                )

            val risoluzioneReaderProxy =
                risolviDashboardReaderProxy(
                    api = api,
                    host = host,
                    porta = porta,
                    username = username,
                    password = password,
                    tipiSalvati =
                        decodificaTipiReaderProxy(
                            preferences.getString(
                                chiaveTipiReaderProxy,
                                null
                            )
                        ),
                    elencoReadersStatus =
                        elencoReadersStatus,
                    elencoProxiesStatus =
                        elencoProxiesStatus
                )

            if (
                risoluzioneReaderProxy
                    .tipiReaderProxySalvati
                    .isNotEmpty()
            ) {
                preferences.edit()
                    .putString(
                        chiaveTipiReaderProxy,
                        codificaTipiReaderProxy(
                            risoluzioneReaderProxy
                                .tipiReaderProxySalvati
                        )
                    )
                    .apply()
            }

            _uiState.value =
                _uiState.value.copy(
                    elencoReaders =
                        risoluzioneReaderProxy
                            .elencoReaders,
                    elencoProxies =
                        risoluzioneReaderProxy
                            .elencoProxies
                )

            val datiReaderProxy =
                risoluzioneReaderProxy
                    .datiReaderProxy

            if (datiReaderProxy != null) {
                _uiState.value =
                    _uiState.value.copy(
                        readerAbilitati =
                            datiReaderProxy
                                .readerAbilitati,
                        readers =
                            datiReaderProxy.readers,
                        proxies =
                            datiReaderProxy.proxies
                    )
            }

            _uiState.value =
                _uiState.value.copy(
                    elencoClients =
                        datiStatus.elencoClients
                )

            _uiState.value =
                _uiState.value.copy(
                    ultimoAggiornamento =
                        oraAggiornamentoDashboard(),
                    erroriRefreshConsecutivi = 0,
                    stato = "Connesso"
                )

            return true
        } finally {
            refreshMutex.unlock()
        }
    }

    suspend fun aggiornaDashboardPolling(
        host: String,
        porta: String,
        username: String,
        password: String
    ): Int {
        val aggiornato =
            aggiornaDashboard(
                host = host,
                porta = porta,
                username = username,
                password = password,
                mostraErroreSubito = false
            )

        return if (aggiornato) {
            0
        } else {
            registraErroreRefresh()
        }
    }

    fun avviaPolling(
        host: String,
        porta: String,
        username: String,
        password: String,
        intervalloSecondi: Int = 5
    ) {
        fermaPolling()

        pollingJob =
            viewModelScope.launch {
                while (true) {
                    val conteggioErrori =
                        aggiornaDashboardPolling(
                            host = host,
                            porta = porta,
                            username = username,
                            password = password
                        )

                    if (conteggioErrori == 3) {
                        _connessionePersaEventi.emit(Unit)
                    }

                    delay(
                        intervalloSecondi.coerceIn(5, 60) * 1000L
                    )
                }
            }
    }

    fun fermaPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun impostaStato(stato: String) {
        _uiState.value =
            _uiState.value.copy(
                stato = stato
            )
    }

    fun registraErroreRefresh(): Int {
        val nuovoConteggio =
            _uiState.value.erroriRefreshConsecutivi + 1

        _uiState.value =
            _uiState.value.copy(
                erroriRefreshConsecutivi = nuovoConteggio,
                stato =
                    if (nuovoConteggio >= 3) {
                        "Connessione persa"
                    } else {
                        _uiState.value.stato
                    }
            )

        return nuovoConteggio
    }

    fun aggiornaReaderAbilitatoLocale(
        nomeReader: String,
        abilitato: Boolean
    ) {
        val statoCorrente = _uiState.value

        _uiState.value =
            statoCorrente.copy(
                readerAbilitati =
                    statoCorrente.readerAbilitati +
                        (nomeReader to abilitato),
                elencoReaders =
                    aggiornaVoceReaderProxyLocale(
                        elenco = statoCorrente.elencoReaders,
                        nome = nomeReader,
                        abilitato = abilitato
                    )
            )
    }

    fun aggiornaProxyAbilitatoLocale(
        nomeProxy: String,
        abilitato: Boolean
    ) {
        val statoCorrente = _uiState.value

        _uiState.value =
            statoCorrente.copy(
                readerAbilitati =
                    statoCorrente.readerAbilitati +
                        (nomeProxy to abilitato),
                elencoProxies =
                    aggiornaVoceReaderProxyLocale(
                        elenco = statoCorrente.elencoProxies,
                        nome = nomeProxy,
                        abilitato = abilitato
                    )
            )
    }

    fun aggiornaUserAbilitatoLocale(
        nomeUser: String,
        abilitato: Boolean
    ) {
        _uiState.value =
            _uiState.value.copy(
                userAbilitati =
                    _uiState.value.userAbilitati +
                        (nomeUser to abilitato)
            )
    }

    private fun aggiornaVoceReaderProxyLocale(
        elenco: List<String>,
        nome: String,
        abilitato: Boolean
    ): List<String> =
        elenco.map { voce ->
            val nomeVoce =
                voce.substringBeforeLast(
                    "—",
                    voce
                ).trim()

            if (nomeVoce == nome) {
                "$nome — " +
                    if (abilitato) {
                        "OFFLINE"
                    } else {
                        "DISABILITATO"
                    }
            } else {
                voce
            }
        }

    class Factory(
        private val api: OscamApi,
        private val preferences: SharedPreferences
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            if (
                modelClass.isAssignableFrom(
                    DashboardViewModel::class.java
                )
            ) {
                return DashboardViewModel(
                    api = api,
                    preferences = preferences
                ) as T
            }

            throw IllegalArgumentException(
                "ViewModel sconosciuto: ${modelClass.name}"
            )
        }
    }
}
