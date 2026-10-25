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
