package com.mrunix.oscamlivemonitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OscamApiParsingInstrumentedTest {

    private val api = OscamApi()

    private val statusJson = """
        {
          "oscam": {
            "version": "2.26.10-11968",
            "runtime": "12345",
            "status": {
              "client": [
                {
                  "type": "s",
                  "connection": { "status": "OK" }
                },
                {
                  "type": "h",
                  "rname_enc": "WebIF",
                  "connection": { "status": "CONNECTED" }
                },
                {
                  "type": "r",
                  "rname_enc": "Reader%2bUno",
                  "connection": { "status": "CARDOK" }
                },
                {
                  "type": "r",
                  "rname_enc": "ReaderDue",
                  "connection": { "status": "OFFLINE" }
                },
                {
                  "type": "p",
                  "rname_enc": "ProxyUno",
                  "connection": { "status": "ACTIVE" }
                },
                {
                  "type": "c",
                  "name_enc": "dm920",
                  "connection": { "status": "CONNECTED" }
                }
              ]
            }
          }
        }
    """.trimIndent()

    @Test
    fun statusJsonValido_vieneRiconosciuto() {
        assertTrue(api.statusJsonOscamValido(statusJson))
        assertFalse(api.statusJsonOscamValido("""{"oscam":{}}"""))
        assertFalse(api.statusJsonOscamValido("non-json"))
    }

    @Test
    fun versioneERuntime_vengonoEstratti() {
        assertEquals(
            "2.26.10-11968",
            api.estraiVersioneJson(statusJson)
        )
        assertEquals(
            "12345",
            api.estraiRuntimeJson(statusJson)
        )
    }

    @Test
    fun conteggiDashboard_rispettanoGliStatiAttivi() {
        assertEquals("2/2", api.estraiServersJson(statusJson))
        assertEquals("1/2", api.estraiReadersJson(statusJson))
        assertEquals("1/1", api.estraiProxiesJson(statusJson))
        assertEquals("1/1", api.estraiClientsJson(statusJson))
    }

    @Test
    fun elencoReaders_mantieneNomeEStato() {
        assertEquals(
            listOf(
                "Reader+Uno — CARDOK",
                "ReaderDue — OFFLINE"
            ),
            api.estraiElencoReadersJson(statusJson)
        )
    }

    @Test
    fun jsonNonValido_usaFallbackPrevisti() {
        assertEquals(
            "Sconosciuta",
            api.estraiVersioneJson("non-json")
        )
        assertEquals(
            "Sconosciuto",
            api.estraiRuntimeJson("non-json")
        )
        assertEquals("0/0", api.estraiReadersJson("non-json"))
    }

    @Test
    fun datiStatusDashboard_mantengonoMappingCompleto() {
        val dati =
            estraiDatiStatusDashboard(
                api = api,
                risultato = statusJson
            )

        assertEquals("2.26.10-11968", dati.versioneOscam)
        assertEquals("12345", dati.uptime)
        assertEquals("Sconosciuta", dati.cpuOscam)
        assertEquals("Sconosciuta", dati.ramOscam)

        assertEquals("2/2", dati.servers)
        assertEquals("1/2", dati.readers)
        assertEquals("1/1", dati.proxies)
        assertEquals("1/1", dati.clients)

        assertEquals(
            listOf(
                "OSCam Core — OK",
                "WebIF — CONNECTED"
            ),
            dati.elencoServers
        )

        assertEquals(
            listOf(
                "Reader+Uno — CARDOK",
                "ReaderDue — OFFLINE"
            ),
            dati.elencoReaders
        )

        assertEquals(
            listOf(
                "ProxyUno — ACTIVE"
            ),
            dati.elencoProxies
        )

        assertEquals(
            listOf(
                "dm920 — CONNECTED"
            ),
            dati.elencoClients
        )
    }

    @Test
    fun cacheTipiReaderProxy_roundTripMantieneReaderEProxy() {
        val originale =
            mapOf(
                "readeruno" to "r",
                "proxyuno" to "p"
            )

        assertEquals(
            originale,
            decodificaTipiReaderProxy(
                codificaTipiReaderProxy(originale)
            )
        )
    }

    @Test
    fun cacheTipiReaderProxy_jsonNonValido_restituisceMappaVuota() {
        assertTrue(
            decodificaTipiReaderProxy(
                "non-json"
            ).isEmpty()
        )
    }

}
