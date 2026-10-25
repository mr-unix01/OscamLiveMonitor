package com.mrunix.oscamlivemonitor

fun chiaveNomeReaderProxy(nome: String): String =
    nome.trim().lowercase(java.util.Locale.ROOT)

fun tipoDaProtocolloReaderProxy(
    protocollo: String
): String {
    return when (
        protocollo.trim().lowercase()
    ) {
        /*
         * Reader locali / fisici.
         */
        "internal",
        "mouse",
        "mp35",
        "pcsc",
        "sc8in1",
        "smargo",
        "smartreader",
        "serial",
        "drecas",
        "emu",
        "stapi",
        "stapi5",
        "stinger",
        "phoenix" -> "r"

        /*
         * Reader di rete: nella Status WebIF
         * vengono mostrati come Proxy.
         */
        "camd33",
        "camd35",
        "cs357x",
        "cs378x",
        "newcamd",
        "newcamd525",
        "newcamd524",
        "cccam",
        "cccam_ext",
        "gbox",
        "radegast",
        "scam",
        "ghttp",
        "constcw",
        "csp",
        "pandora" -> "p"

        else -> ""
    }
}

fun normalizzaElencoServersDashboard(
    elencoServersOriginale: List<String>
): List<String> =
    elencoServersOriginale.mapIndexed { indice, server ->
        val statoServer =
            server
                .substringAfterLast("—", "")
                .trim()
                .ifBlank {
                    when {
                        server.contains(
                            "NOT OK",
                            ignoreCase = true
                        ) ->
                            "NOT OK"

                        server.contains(
                            "OFFLINE",
                            ignoreCase = true
                        ) ->
                            "OFFLINE"

                        server.contains(
                            "ERROR",
                            ignoreCase = true
                        ) ->
                            "ERROR"

                        server.contains(
                            "OK",
                            ignoreCase = true
                        ) ->
                            "OK"

                        else ->
                            "Stato sconosciuto"
                    }
                }

        when (indice) {
            0 ->
                "OSCam Core — $statoServer"

            1 ->
                "WebIF — $statoServer"

            else ->
                server
        }
    }

fun tipoEffettivo(
    voce: OscamApi.ReaderListVoce,
    tipiReaderProxySalvati: Map<String, String>
): String {
    val tipoSalvato =
        tipiReaderProxySalvati[
            chiaveNomeReaderProxy(voce.nome)
        ].orEmpty()

    return when {
        !voce.abilitato ->
            tipoSalvato

        voce.tipo == "r" || voce.tipo == "p" ->
            voce.tipo

        else ->
            tipoSalvato
    }
}

fun voceDashboard(
    voce: OscamApi.ReaderListVoce,
    voceStatus: String?
): String {
    if (!voce.abilitato) {
        return "${voce.nome} — DISABILITATO"
    }

    if (!voceStatus.isNullOrBlank()) {
        return voceStatus
    }

    val statoFallback =
        voce.stato
            .ifBlank { "OFFLINE" }

    return "${voce.nome} — $statoFallback"
}

fun statoAttivo(voce: String?): Boolean {
    val statoVoce =
        voce
            ?.substringAfterLast("—", "")
            ?.trim()
            ?.uppercase()
            .orEmpty()

    return statoVoce == "OK" ||
        statoVoce == "ACTIVE" ||
        statoVoce == "CONNECTED" ||
        statoVoce == "CARDOK"
}

fun indicizzaStatusReaderProxy(
    voci: List<String>
): Map<String, String> =
    voci.associateBy { voce ->
        chiaveNomeReaderProxy(
            voce.substringBeforeLast("—", voce).trim()
        )
    }

data class DashboardReaderProxyData(
    val readerAbilitati: Map<String, Boolean>,
    val elencoReaders: List<String>,
    val elencoProxies: List<String>,
    val readers: String,
    val proxies: String
)

fun costruisciDashboardReaderProxy(
    readerList: List<OscamApi.ReaderListVoce>,
    tipiReaderProxySalvati: Map<String, String>,
    readersStatusPerNome: Map<String, String>,
    proxiesStatusPerNome: Map<String, String>
): DashboardReaderProxyData? {
    if (readerList.isEmpty()) {
        return null
    }

    val readerAbilitati =
        readerList.associate { voce ->
            voce.nome to voce.abilitato
        }

    val catalogoReaders =
        readerList.filter { voce ->
            tipoEffettivo(
                voce,
                tipiReaderProxySalvati
            ) == "r"
        }

    val catalogoProxies =
        readerList.filter { voce ->
            tipoEffettivo(
                voce,
                tipiReaderProxySalvati
            ) == "p"
        }

    val elencoReaders =
        catalogoReaders.map { voce ->
            voceDashboard(
                voce = voce,
                voceStatus =
                    readersStatusPerNome[
                        chiaveNomeReaderProxy(voce.nome)
                    ]
            )
        }

    val elencoProxies =
        catalogoProxies.map { voce ->
            voceDashboard(
                voce = voce,
                voceStatus =
                    proxiesStatusPerNome[
                        chiaveNomeReaderProxy(voce.nome)
                    ]
            )
        }

    val readersAttivi =
        catalogoReaders.count { voce ->
            statoAttivo(
                readersStatusPerNome[
                    chiaveNomeReaderProxy(voce.nome)
                ]
            )
        }

    val proxiesAttivi =
        catalogoProxies.count { voce ->
            statoAttivo(
                proxiesStatusPerNome[
                    chiaveNomeReaderProxy(voce.nome)
                ]
            )
        }

    return DashboardReaderProxyData(
        readerAbilitati = readerAbilitati,
        elencoReaders = elencoReaders,
        elencoProxies = elencoProxies,
        readers = "$readersAttivi/${catalogoReaders.size}",
        proxies = "$proxiesAttivi/${catalogoProxies.size}"
    )
}

fun chiaveTipiReaderProxyDashboard(
    host: String,
    porta: String
): String =
    "reader_proxy_types_v2_" +
        host.trim() + "_" +
        porta.trim()

fun decodificaTipiReaderProxy(
    jsonSalvato: String?
): MutableMap<String, String> {
    val tipi =
        mutableMapOf<String, String>()

    if (jsonSalvato.isNullOrBlank()) {
        return tipi
    }

    try {
        val json =
            org.json.JSONObject(jsonSalvato)

        val chiavi = json.keys()

        while (chiavi.hasNext()) {
            val nome = chiavi.next()

            val tipo =
                json.optString(nome, "")
                    .trim()
                    .lowercase()

            if (tipo == "r" || tipo == "p") {
                tipi[nome] = tipo
            }
        }
    } catch (_: Exception) {
        Unit
    }

    return tipi
}

fun codificaTipiReaderProxy(
    tipi: Map<String, String>
): String {
    val json =
        org.json.JSONObject()

    tipi.forEach { (nome, tipo) ->
        json.put(nome, tipo)
    }

    return json.toString()
}
