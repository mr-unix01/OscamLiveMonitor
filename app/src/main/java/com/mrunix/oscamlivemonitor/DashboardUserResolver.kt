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
