package com.mrunix.oscamlivemonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun caricaUserStatsDashboard(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String
): String =
    withContext(Dispatchers.IO) {
        api.scaricaUserStatsJson(
            host = host.trim(),
            porta = porta.trim(),
            username = username,
            password = password
        )
    }

suspend fun caricaUserConfigDashboard(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String
): String =
    withContext(Dispatchers.IO) {
        api.scaricaUserConfig(
            host = host.trim(),
            porta = porta.trim(),
            username = username,
            password = password
        )
    }


fun integraDettagliClientNegliUsers(
    elencoUsers: List<String>,
    elencoClients: List<String>
): List<String> {
    fun nomeVoce(testo: String): String =
        testo.lineSequence()
            .firstOrNull()
            .orEmpty()
            .substringBeforeLast("—", "")
            .trim()
            .lowercase()

    val clientsPerNome = elencoClients.groupBy(::nomeVoce)

    return elencoUsers.map { utente ->
        val righe = utente.lines()
        val intestazione = righe.firstOrNull().orEmpty()

        val stato = intestazione
            .substringAfterLast("—", "")
            .trim()

        if (
            stato.equals("OFFLINE", ignoreCase = true) ||
            stato.equals("DISABILITATO", ignoreCase = true)
        ) {
            return@map utente
        }

        val corrispondenze =
            clientsPerNome[nomeVoce(utente)].orEmpty()

        // Evita di associare la sessione sbagliata
        // se più client usano lo stesso account.
        if (corrispondenze.size != 1) {
            return@map utente
        }

        val righeClient = corrispondenze.single()
            .lines()
            .drop(1)
            .map { it.trim() }

        val canalePresente = righe.any {
            it.startsWith("Canale:", ignoreCase = true)
        }

        val aggiunte = righeClient.filter { riga ->
            when {
                riga.startsWith("Canale:", true) ->
                    !canalePresente

                riga.startsWith("Provider:", true) -> true
                riga.startsWith("CAID ", true) -> true
                riga.startsWith("ECM ", true) -> true
                riga.startsWith("Reader:", true) -> true
                else -> false
            }
        }

        if (aggiunte.isEmpty()) {
            return@map utente
        }

        val ecmLive = aggiunte.any {
            it.startsWith("CAID ", true) ||
                it.startsWith("ECM ", true)
        }

        val righeBase = if (ecmLive) {
            righe.filterNot {
                it.startsWith("ECM ", ignoreCase = true)
            }
        } else {
            righe
        }

        (
            listOf(righeBase.first()) +
                aggiunte +
                righeBase.drop(1)
        ).joinToString("\n")
    }
}

data class DashboardUserData(
    val users: String,
    val elencoUsers: List<String>?,
    val userAbilitati: Map<String, Boolean>?
)

suspend fun caricaDashboardUsers(
    api: OscamApi,
    host: String,
    porta: String,
    username: String,
    password: String
): DashboardUserData? {
    val risultatoUserStats =
        caricaUserStatsDashboard(
            api = api,
            host = host,
            porta = porta,
            username = username,
            password = password
        )

    if (risultatoUserStats.startsWith("ERRORE:")) {
        return null
    }

    val users =
        api.estraiUsersJson(risultatoUserStats)

    val paginaUserConfig =
        caricaUserConfigDashboard(
            api = api,
            host = host,
            porta = porta,
            username = username,
            password = password
        )

    if (paginaUserConfig.startsWith("ERRORE:")) {
        return DashboardUserData(
            users = users,
            elencoUsers = null,
            userAbilitati = null
        )
    }

    return DashboardUserData(
        users = users,
        elencoUsers =
            api.estraiElencoUsersJson(
                json = risultatoUserStats,
                htmlUserConfig = paginaUserConfig
            ),
        userAbilitati =
            api.estraiAbilitazioneUsersHtml(
                paginaUserConfig
            )
    )
}
