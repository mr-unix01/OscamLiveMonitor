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
