package com.mrunix.oscamlivemonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun caricaStatusDashboard(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String
): String =
    withContext(Dispatchers.IO) {
        api.scaricaStatusJson(
            host = host.trim(),
            porta = porta.trim(),
            username = username,
            password = password
        )
    }

data class DashboardStatusData(
    val versioneOscam: String,
    val uptime: String,
    val cpuOscam: String,
    val ramOscam: String,
    val servers: String,
    val readers: String,
    val proxies: String,
    val clients: String,
    val elencoServers: List<String>,
    val elencoReaders: List<String>,
    val dettagliReaders: Map<String, List<Pair<String, String>>>,
    val elencoProxies: List<String>,
    val dettagliProxies: Map<String, List<Pair<String, String>>>,
    val elencoClients: List<String>
)

fun estraiDatiStatusDashboard(
    api: OscamApi,
    risultato: String
): DashboardStatusData =
    DashboardStatusData(
        versioneOscam =
            api.estraiVersioneJson(risultato),
        uptime =
            api.estraiRuntimeJson(risultato),
        cpuOscam =
            api.estraiCpuJson(risultato),
        ramOscam =
            api.estraiMemoriaUsataJson(risultato),
        servers =
            api.estraiServersJson(risultato),
        readers =
            api.estraiReadersJson(risultato),
        proxies =
            api.estraiProxiesJson(risultato),
        clients =
            api.estraiClientsJson(risultato),
        elencoServers =
            normalizzaElencoServersDashboard(
                api.estraiElencoServersJson(risultato)
            ),
        elencoReaders =
            api.estraiElencoReadersJson(risultato),
        dettagliReaders =
            api.estraiDettagliReaderProxyJson(risultato, "r"),
        elencoProxies =
            api.estraiElencoProxiesJson(risultato),
        dettagliProxies =
            api.estraiDettagliReaderProxyJson(risultato, "p"),
        elencoClients =
            api.estraiElencoClientsJson(risultato)
    )
