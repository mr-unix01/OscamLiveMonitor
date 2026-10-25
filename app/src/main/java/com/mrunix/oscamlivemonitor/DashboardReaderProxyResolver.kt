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
