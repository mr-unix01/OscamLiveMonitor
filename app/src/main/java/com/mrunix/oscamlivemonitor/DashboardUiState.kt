package com.mrunix.oscamlivemonitor

data class DashboardUiState(
    val stato: String = "",
    val versioneOscam: String = "",
    val uptime: String = "",
    val cpuOscam: String = "",
    val ramOscam: String = "",
    val servers: String = "",
    val readers: String = "",
    val proxies: String = "",
    val clients: String = "",
    val users: String = "",
    val elencoUsers: List<String> = emptyList(),
    val userAbilitati: Map<String, Boolean> = emptyMap(),
    val elencoServers: List<String> = emptyList(),
    val elencoReaders: List<String> = emptyList(),
    val elencoProxies: List<String> = emptyList(),
    val readerAbilitati: Map<String, Boolean> = emptyMap(),
    val elencoClients: List<String> = emptyList(),
    val ultimoAggiornamento: String = "",
    val erroriRefreshConsecutivi: Int = 0
)
