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

data class DashboardStatusSummary(
    val versioneOscam: String,
    val uptime: String,
    val cpuOscam: String,
    val ramOscam: String,
    val servers: String,
    val readers: String,
    val proxies: String,
    val clients: String
)

fun estraiRiepilogoStatusDashboard(
    api: OscamApi,
    risultato: String
): DashboardStatusSummary =
    DashboardStatusSummary(
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
            api.estraiClientsJson(risultato)
    )
