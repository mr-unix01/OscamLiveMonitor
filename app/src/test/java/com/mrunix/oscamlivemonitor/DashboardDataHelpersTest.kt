package com.mrunix.oscamlivemonitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardDataHelpersTest {

    @Test
    fun tipoProtocollo_distingueReaderProxyESconosciuto() {
        assertEquals(
            "r",
            tipoDaProtocolloReaderProxy("internal")
        )
        assertEquals(
            "p",
            tipoDaProtocolloReaderProxy("cs378x")
        )
        assertEquals(
            "",
            tipoDaProtocolloReaderProxy("sconosciuto")
        )
    }

    @Test
    fun normalizzaServers_rinominaSoloLePrimeDueVoci() {
        assertEquals(
            listOf(
                "OSCam Core — OK",
                "WebIF — CONNECTED",
                "Altro — OFFLINE"
            ),
            normalizzaElencoServersDashboard(
                listOf(
                    "Server OSCam — OK",
                    "WebIF originale — CONNECTED",
                    "Altro — OFFLINE"
                )
            )
        )
    }

    @Test
    fun readerProxy_costruzioneMantieneDisabilitatiEConteggi() {
        val readerList =
            listOf(
                OscamApi.ReaderListVoce(
                    nome = "ReaderLocale",
                    tipo = "r",
                    abilitato = true,
                    stato = "CARDOK"
                ),
                OscamApi.ReaderListVoce(
                    nome = "ProxyRete",
                    tipo = "p",
                    abilitato = true,
                    stato = "ACTIVE"
                ),
                OscamApi.ReaderListVoce(
                    nome = "ReaderSpento",
                    tipo = "",
                    abilitato = false,
                    stato = ""
                )
            )

        val risultato =
            costruisciDashboardReaderProxy(
                readerList = readerList,
                tipiReaderProxySalvati =
                    mapOf(
                        "readerspento" to "r"
                    ),
                readersStatusPerNome =
                    indicizzaStatusReaderProxy(
                        listOf(
                            "ReaderLocale — CARDOK"
                        )
                    ),
                proxiesStatusPerNome =
                    indicizzaStatusReaderProxy(
                        listOf(
                            "ProxyRete — ACTIVE"
                        )
                    )
            )

        requireNotNull(risultato)

        assertEquals(
            mapOf(
                "ReaderLocale" to true,
                "ProxyRete" to true,
                "ReaderSpento" to false
            ),
            risultato.readerAbilitati
        )

        assertEquals(
            listOf(
                "ReaderLocale — CARDOK",
                "ReaderSpento — DISABILITATO"
            ),
            risultato.elencoReaders
        )

        assertEquals(
            listOf(
                "ProxyRete — ACTIVE"
            ),
            risultato.elencoProxies
        )

        assertEquals("1/2", risultato.readers)
        assertEquals("1/1", risultato.proxies)
    }

    @Test
    fun readerProxy_readerListVuota_restituisceNull() {
        assertNull(
            costruisciDashboardReaderProxy(
                readerList = emptyList(),
                tipiReaderProxySalvati = emptyMap(),
                readersStatusPerNome = emptyMap(),
                proxiesStatusPerNome = emptyMap()
            )
        )
    }

    @Test
    fun integraTipi_statusHaPrecedenzaESalvaTipoAttivo() {
        val risultato =
            integraTipiReaderProxyDashboard(
                tipiSalvati =
                    mapOf(
                        "proxyuno" to "r"
                    ),
                elencoReadersStatus = emptyList(),
                elencoProxiesStatus =
                    listOf(
                        "ProxyUno — ACTIVE"
                    ),
                readerList =
                    listOf(
                        OscamApi.ReaderListVoce(
                            nome = "ReaderDue",
                            tipo = "r",
                            abilitato = true,
                            stato = "CARDOK"
                        )
                    )
            )

        assertEquals("p", risultato["proxyuno"])
        assertEquals("r", risultato["readerdue"])
    }

    @Test
    fun readerDisabilitatiSenzaTipo_selezionaSoloQuelliDaRisoluzione() {
        val sconosciuto =
            OscamApi.ReaderListVoce(
                nome = "DaRisolvere",
                tipo = "",
                abilitato = false,
                stato = ""
            )

        val giaConosciuto =
            OscamApi.ReaderListVoce(
                nome = "GiaConosciuto",
                tipo = "",
                abilitato = false,
                stato = ""
            )

        val attivo =
            OscamApi.ReaderListVoce(
                nome = "Attivo",
                tipo = "",
                abilitato = true,
                stato = "CARDOK"
            )

        assertEquals(
            listOf(sconosciuto),
            readerDisabilitatiSenzaTipo(
                readerList =
                    listOf(
                        sconosciuto,
                        giaConosciuto,
                        attivo
                    ),
                tipiReaderProxySalvati =
                    mapOf(
                        "giaconosciuto" to "r"
                    )
            )
        )
    }
}
