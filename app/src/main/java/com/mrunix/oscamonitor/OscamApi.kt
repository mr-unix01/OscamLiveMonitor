package com.mrunix.oscamonitor

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class OscamApi {

    private data class AuthDati(
        val username: String,
        val password: String
    )

    private val nonceCounter = AtomicInteger(0)

    private val client = OkHttpClient.Builder()
        .authenticator { _, response ->
            val authorizationPrecedente =
                response.request.header("Authorization").orEmpty()

            if (
                authorizationPrecedente.startsWith(
                    "Digest",
                    ignoreCase = true
                )
            ) {
                return@authenticator null
            }

            val challenge =
                response.header("WWW-Authenticate")
                    ?: return@authenticator null

            if (
                !challenge.startsWith(
                    "Digest",
                    ignoreCase = true
                )
            ) {
                return@authenticator null
            }

            val authDati =
                response.request.tag(AuthDati::class.java)
                    ?: return@authenticator null

            val uriRichiesta =
                response.request.url.encodedPath +
                        response.request.url.encodedQuery
                            ?.let { "?$it" }
                            .orEmpty()

            val digestAuthorization =
                creaDigestAuthorization(
                    challenge = challenge,
                    method = response.request.method,
                    uri = uriRichiesta,
                    username = authDati.username,
                    password = authDati.password
                )
                    ?: return@authenticator null

            response.request.newBuilder()
                .header(
                    "Authorization",
                    digestAuthorization
                )
                .build()
        }
        .build()

    fun scaricaStatus(
        host: String,
        porta: String,
        username: String,
        password: String
    ): String {
        return scaricaPagina(
            host = host,
            porta = porta,
            username = username,
            password = password,
            percorso = "status.html"
        )
    }

    fun scaricaStatusJson(
        host: String,
        porta: String,
        username: String,
        password: String
    ): String {
        return scaricaPagina(
            host = host,
            porta = porta,
            username = username,
            password = password,
            percorso = "oscamapi.json?part=status"
        )
    }

    fun riavviaOscam(
        host: String,
        porta: String,
        username: String,
        password: String
    ): String {
        return scaricaPagina(
            host = host,
            porta = porta,
            username = username,
            password = password,
            percorso = "shutdown.html?action=Restart"
        )
    }

    fun estraiTitolo(html: String): String {
        val regex = Regex(
            "<title>(.*?)</title>",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )

        return regex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: "Titolo non trovato"
    }

    fun estraiUptime(html: String): String {
        val regex = Regex(
            """id=["']runtime["'][^>]*>\s*([^<]+)<""",
            RegexOption.IGNORE_CASE
        )

        return regex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: "Sconosciuto"
    }

    fun estraiVersioneJson(json: String): String {
        return try {
            JSONObject(json)
                .getJSONObject("oscam")
                .optString("version", "Sconosciuta")
        } catch (e: Exception) {
            "Sconosciuta"
        }
    }

    fun estraiRuntimeJson(json: String): String {
        return try {
            JSONObject(json)
                .getJSONObject("oscam")
                .optString("runtime", "Sconosciuto")
        } catch (e: Exception) {
            "Sconosciuto"
        }
    }

    fun estraiServersJson(json: String): String {
        return estraiContatoreTipi(
            json = json,
            tipi = setOf("s", "h")
        )
    }

    fun estraiReadersJson(json: String): String {
        return estraiContatoreTipi(
            json = json,
            tipi = setOf("r")
        )
    }

    fun estraiProxiesJson(json: String): String {
        return estraiContatoreTipi(
            json = json,
            tipi = setOf("p")
        )
    }

    fun estraiClientsJson(json: String): String {
        return estraiContatoreTipi(
            json = json,
            tipi = setOf("c")
        )
    }

    fun estraiElencoServersJson(json: String): List<String> {
        return try {
            val clients = estraiArrayClient(json)
            val elenco = mutableListOf<String>()

            for (indice in 0 until clients.length()) {
                val elemento = clients.getJSONObject(indice)
                val tipo = elemento.optString("type")

                if (tipo == "s" || tipo == "h") {
                    val nome = if (tipo == "s") {
                        "Server OSCam"
                    } else {
                        elemento
                            .optString("rname_enc", "WebIF")
                            .replace("%2b", "+", ignoreCase = true)
                            .ifBlank { "WebIF" }
                    }

                    val stato = elemento
                        .optJSONObject("connection")
                        ?.optString("status", "Sconosciuto")
                        ?: "Sconosciuto"

                    elenco.add("$nome — $stato")
                }
            }

            elenco
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun estraiElencoReadersJson(json: String): List<String> {
        return try {
            val clients = estraiArrayClient(json)
            val elenco = mutableListOf<String>()

            for (indice in 0 until clients.length()) {
                val elemento = clients.getJSONObject(indice)

                if (elemento.optString("type") == "r") {
                    val nome = elemento
                        .optString("rname_enc", "Reader sconosciuto")
                        .replace("%2b", "+", ignoreCase = true)

                    val stato = elemento
                        .optJSONObject("connection")
                        ?.optString("status", "Sconosciuto")
                        ?: "Sconosciuto"

                    elenco.add("$nome — $stato")
                }
            }

            elenco
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun estraiElencoProxiesJson(json: String): List<String> {
        return try {
            val clients = estraiArrayClient(json)
            val elenco = mutableListOf<String>()

            for (indice in 0 until clients.length()) {
                val elemento = clients.getJSONObject(indice)

                if (elemento.optString("type") == "p") {
                    val nome = elemento
                        .optString("rname_enc", "Proxy sconosciuto")
                        .replace("%2b", "+", ignoreCase = true)

                    val stato = elemento
                        .optJSONObject("connection")
                        ?.optString("status", "Sconosciuto")
                        ?: "Sconosciuto"

                    elenco.add("$nome — $stato")
                }
            }

            elenco
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun estraiElencoClientsJson(json: String): List<String> {
        return try {
            val clients = estraiArrayClient(json)
            val elenco = mutableListOf<String>()

            for (indice in 0 until clients.length()) {
                val elemento = clients.getJSONObject(indice)

                if (elemento.optString("type") == "c") {
                    val nome = elemento
                        .optString("name_enc", "Client sconosciuto")
                        .replace("%2b", "+", ignoreCase = true)

                    val connessione =
                        elemento.optJSONObject("connection")

                    val richiesta =
                        elemento.optJSONObject("request")

                    val stato = connessione
                        ?.optString("status", "Sconosciuto")
                        ?: "Sconosciuto"

                    val canale = richiesta
                        ?.optString("chname", "")
                        ?.replace("%2b", "+", ignoreCase = true)
                        ?.trim()
                        ?.ifBlank { "-" }
                        ?: "-"

                    val provider = richiesta
                        ?.optString("chprovider", "")
                        ?.replace("%2b", "+", ignoreCase = true)
                        ?.trim()
                        ?.ifBlank { "-" }
                        ?: "-"

                    val caid = richiesta
                        ?.optString("caid", "")
                        ?.trim()
                        ?.ifBlank { "-" }
                        ?: "-"

                    val ecmTime = richiesta
                        ?.optString("ecmtime", "")
                        ?.trim()
                        ?.ifBlank { "-" }
                        ?: "-"

                    val reader = richiesta
                        ?.optString("answered", "")
                        ?.replace("%2b", "+", ignoreCase = true)
                        ?.trim()
                        ?.ifBlank { "-" }
                        ?: "-"

                    val righe = mutableListOf<String>()

                    righe.add("$nome — $stato")

                    if (canale != "-") {
                        righe.add("Canale: $canale")
                    }

                    if (provider != "-") {
                        righe.add("Provider: $provider")
                    }

                    val dettagliEcm = mutableListOf<String>()

                    if (caid != "-") {
                        dettagliEcm.add("CAID $caid")
                    }

                    if (ecmTime != "-") {
                        dettagliEcm.add("ECM $ecmTime ms")
                    }

                    if (dettagliEcm.isNotEmpty()) {
                        righe.add(dettagliEcm.joinToString(" • "))
                    }

                    if (reader != "-") {
                        righe.add("Reader: $reader")
                    }

                    elenco.add(
                        righe.joinToString("\n")
                    )
                }
            }

            elenco
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun estraiContatoreTipi(
        json: String,
        tipi: Set<String>
    ): String {
        return try {
            val clients = estraiArrayClient(json)

            var totale = 0
            var attivi = 0

            for (indice in 0 until clients.length()) {
                val elemento = clients.getJSONObject(indice)
                val tipo = elemento.optString("type")

                if (tipo in tipi) {
                    totale++

                    val stato = elemento
                        .optJSONObject("connection")
                        ?.optString("status")
                        ?.uppercase()
                        ?: ""

                    if (
                        stato == "OK" ||
                        stato == "ACTIVE" ||
                        stato == "CONNECTED" ||
                        stato == "CARDOK"
                    ) {
                        attivi++
                    }
                }
            }

            "$attivi/$totale"
        } catch (e: Exception) {
            "0/0"
        }
    }

    fun estraiMemoriaUsataJson(json: String): String {
        return try {
            JSONObject(json)
                .getJSONObject("oscam")
                .getJSONObject("sysinfo")
                .optString(
                    "oscam_rsssize",
                    "Sconosciuta"
                )
        } catch (e: Exception) {
            "Sconosciuta"
        }
    }

    fun estraiCpuJson(json: String): String {
        return try {
            JSONObject(json)
                .getJSONObject("oscam")
                .getJSONObject("sysinfo")
                .optString(
                    "oscam_cpu_sum",
                    "Sconosciuta"
                )
        } catch (e: Exception) {
            "Sconosciuta"
        }
    }

    private fun estraiArrayClient(json: String) =
        JSONObject(json)
            .getJSONObject("oscam")
            .getJSONObject("status")
            .getJSONArray("client")

    private fun scaricaPagina(
        host: String,
        porta: String,
        username: String,
        password: String,
        percorso: String
    ): String {
        val hostPulito = host.trim()
        val portaPulita = porta.trim()

        if (hostPulito.isBlank()) {
            return "ERRORE: Host/IP vuoto"
        }

        if (portaPulita.toIntOrNull() == null) {
            return "ERRORE: Porta non valida"
        }

        return try {
            val request = creaRichiesta(
                host = hostPulito,
                porta = portaPulita,
                username = username,
                password = password,
                percorso = percorso
            )

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "ERRORE: HTTP ${response.code} ${response.message}"
                }

                response.body.string()
            }
        } catch (e: Exception) {
            "ERRORE: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    private fun creaRichiesta(
        host: String,
        porta: String,
        username: String,
        password: String,
        percorso: String
    ): Request {
        val builder = Request.Builder()
            .url("http://$host:$porta/$percorso")

        if (username.isNotBlank() && password.isNotBlank()) {
            builder
                .header(
                    "Authorization",
                    Credentials.basic(username, password)
                )
                .tag(
                    AuthDati::class.java,
                    AuthDati(
                        username = username,
                        password = password
                    )
                )
        }

        return builder.build()
    }

    private fun creaDigestAuthorization(
        challenge: String,
        method: String,
        uri: String,
        username: String,
        password: String
    ): String? {
        return try {
            val parametri =
                parseDigestChallenge(challenge)

            val realm =
                parametri["realm"]
                    ?: return null

            val nonce =
                parametri["nonce"]
                    ?: return null

            val algorithm =
                parametri["algorithm"]
                    ?: "MD5"

            if (
                !algorithm.equals(
                    "MD5",
                    ignoreCase = true
                )
            ) {
                return null
            }

            val qop =
                parametri["qop"]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.firstOrNull {
                        it.equals(
                            "auth",
                            ignoreCase = true
                        )
                    }

            val opaque =
                parametri["opaque"]

            val nc =
                nonceCounter
                    .incrementAndGet()
                    .toString(16)
                    .padStart(8, '0')

            val cnonce =
                md5Hex(
                    "$username:$nonce:$nc:${System.nanoTime()}"
                )
                    .take(16)

            val ha1 =
                md5Hex(
                    "$username:$realm:$password"
                )

            val ha2 =
                md5Hex(
                    "$method:$uri"
                )

            val responseDigest =
                if (qop != null) {
                    md5Hex(
                        "$ha1:$nonce:$nc:$cnonce:$qop:$ha2"
                    )
                } else {
                    md5Hex(
                        "$ha1:$nonce:$ha2"
                    )
                }

            buildString {
                append("Digest ")
                append("username=\"$username\"")
                append(", realm=\"$realm\"")
                append(", nonce=\"$nonce\"")
                append(", uri=\"$uri\"")
                append(", response=\"$responseDigest\"")
                append(", algorithm=$algorithm")

                if (!opaque.isNullOrBlank()) {
                    append(", opaque=\"$opaque\"")
                }

                if (qop != null) {
                    append(", qop=$qop")
                    append(", nc=$nc")
                    append(", cnonce=\"$cnonce\"")
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDigestChallenge(
        challenge: String
    ): Map<String, String> {
        val contenuto =
            challenge
                .replaceFirst(
                    Regex(
                        "^Digest\\s+",
                        RegexOption.IGNORE_CASE
                    ),
                    ""
                )
                .trim()

        val regex =
            Regex(
                """(\w+)=("([^"]*)"|([^,\s]+))"""
            )

        return regex
            .findAll(contenuto)
            .associate { match ->
                val chiave =
                    match.groupValues[1]
                        .lowercase(Locale.ROOT)

                val valore =
                    match.groupValues[3]
                        .ifBlank {
                            match.groupValues[4]
                        }

                chiave to valore
            }
    }

    private fun md5Hex(
        valore: String
    ): String {
        val digest =
            MessageDigest
                .getInstance("MD5")
                .digest(
                    valore.toByteArray(
                        Charsets.ISO_8859_1
                    )
                )

        return digest.joinToString("") { byte ->
            "%02x".format(
                byte.toInt() and 0xff
            )
        }
    }

}

