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
