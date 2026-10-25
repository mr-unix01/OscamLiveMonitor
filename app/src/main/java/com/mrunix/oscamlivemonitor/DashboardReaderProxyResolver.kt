package com.mrunix.oscamlivemonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun risolviTipiReaderProxyDaConfigurazione(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String,
    voci: List<OscamApi.ReaderListVoce>
): Map<String, String> =
    withContext(Dispatchers.IO) {
        voci.associate { voce ->
            val html =
                api.scaricaReaderConfigHtml(
                    host = host.trim(),
                    porta = porta.trim(),
                    username = username,
                    password = password,
                    nomeReader = voce.nome
                )

            val protocollo =
                if (!html.startsWith("ERRORE:")) {
                    api.estraiProtocolloReaderConfigHtml(
                        html
                    )
                } else {
                    ""
                }

            chiaveNomeReaderProxy(voce.nome) to
                tipoDaProtocolloReaderProxy(protocollo)
        }
    }

suspend fun caricaReaderListDashboard(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String
): List<OscamApi.ReaderListVoce> =
    withContext(Dispatchers.IO) {
        val risultato =
            api.scaricaReaderListJson(
                host = host.trim(),
                porta = porta.trim(),
                username = username,
                password = password
            )

        if (!risultato.startsWith("ERRORE:")) {
            api.estraiReaderListJson(risultato)
        } else {
            emptyList()
        }
    }

data class DashboardReaderProxyResolution(
    val tipiReaderProxySalvati: Map<String, String>,
    val datiReaderProxy: DashboardReaderProxyData?,
    val elencoReaders: List<String>,
    val elencoProxies: List<String>
)

suspend fun risolviDashboardReaderProxy(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String,
    tipiSalvati: Map<String, String>,
    elencoReadersStatus: List<String>,
    elencoProxiesStatus: List<String>
): DashboardReaderProxyResolution {
    val readerList =
        caricaReaderListDashboard(
            api = api,
            host = host,
            porta = porta,
            username = username,
            password = password
        )

    val tipiReaderProxySalvati =
        integraTipiReaderProxyDashboard(
            tipiSalvati = tipiSalvati,
            elencoReadersStatus = elencoReadersStatus,
            elencoProxiesStatus = elencoProxiesStatus,
            readerList = readerList
        )

    /*
     * Per una voce disabilitata e mai classificata
     * non ci fidiamo di readerlist.type:
     * leggiamo una sola volta il protocollo reale
     * dalla configurazione OSCam.
     *
     * Questo rende la classificazione indipendente
     * dal nome del reader/proxy.
     */
    val vociDisabilitateSenzaTipo =
        readerDisabilitatiSenzaTipo(
            readerList = readerList,
            tipiReaderProxySalvati = tipiReaderProxySalvati
        )

    if (vociDisabilitateSenzaTipo.isNotEmpty()) {
        val tipiDaConfigurazione =
            risolviTipiReaderProxyDaConfigurazione(
                api = api,
                host = host,
                porta = porta,
                username = username,
                password = password,
                voci = vociDisabilitateSenzaTipo
            )

        tipiReaderProxySalvati.putAll(
            filtraTipiReaderProxyValidi(
                tipiDaConfigurazione
            )
        )
    }

    val readersStatusPerNome =
        indicizzaStatusReaderProxy(
            elencoReadersStatus
        )

    val proxiesStatusPerNome =
        indicizzaStatusReaderProxy(
            elencoProxiesStatus
        )

    val datiReaderProxy =
        costruisciDashboardReaderProxy(
            readerList = readerList,
            tipiReaderProxySalvati = tipiReaderProxySalvati,
            readersStatusPerNome = readersStatusPerNome,
            proxiesStatusPerNome = proxiesStatusPerNome
        )

    return DashboardReaderProxyResolution(
        tipiReaderProxySalvati =
            tipiReaderProxySalvati.toMap(),
        datiReaderProxy = datiReaderProxy,
        elencoReaders =
            datiReaderProxy?.elencoReaders
                ?: elencoReadersStatus,
        elencoProxies =
            datiReaderProxy?.elencoProxies
                ?: elencoProxiesStatus
    )
}
