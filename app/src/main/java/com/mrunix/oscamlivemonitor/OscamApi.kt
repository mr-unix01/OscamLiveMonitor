package com.mrunix.oscamlivemonitor

import android.util.Base64
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class OscamApi {

    data class LiveLogRisultato(
        val righe: List<String>,
        val ultimoId: String,
        val valido: Boolean
    )

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

    fun scaricaUserStatsJson(
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
            percorso = "oscamapi.json?part=userstats"
        )
    }

    fun scaricaUserConfig(
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
            percorso = "userconfig.html"
        )
    }

    fun scaricaLiveLog(
        host: String,
        porta: String,
        username: String,
        password: String,
        lastId: String
    ): String {
        val idPulito = lastId.trim().ifBlank { "start" }

        // OSCam usa lastid=start per costruire la pagina HTML del Live Log.
        // Il polling JSON vero e proprio usa invece un lastid numerico.
        // Alla prima chiamata ricaviamo quindi l'id corrente dalla pagina HTML;
        // se una build non lo espone nel template, usiamo 0 come fallback.
        val idDaUsare =
            if (idPulito.equals("start", ignoreCase = true)) {
                val paginaIniziale = scaricaPagina(
                    host = host,
                    porta = porta,
                    username = username,
                    password = password,
                    percorso = "logpoll.html?lastid=start"
                )

                if (paginaIniziale.startsWith("ERRORE:")) {
                    return paginaIniziale
                }

                estraiLastIdDaHtml(paginaIniziale)
                    .ifBlank { "0" }
            } else {
                idPulito
            }

        return scaricaPagina(
            host = host,
            porta = porta,
            username = username,
            password = password,
            percorso = "logpoll.html?lastid=$idDaUsare&_=${System.currentTimeMillis()}",
            richiestaAjax = true
        )
    }

    private fun estraiLastIdDaHtml(html: String): String {
        val pattern = listOf(
            Regex(
                """lastid\s*=\s*[\"']?(\d+)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """[\"']lastid[\"']\s*:\s*[\"']?(\d+)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """data-lastid\s*=\s*[\"'](\d+)[\"']""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """id\s*=\s*[\"']lastid[\"'][^>]*value\s*=\s*[\"'](\d+)[\"']""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
        )

        for (regex in pattern) {
            val id = regex.find(html)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                .orEmpty()

            if (id.isNotBlank()) {
                return id
            }
        }

        return ""
    }

    fun estraiLiveLog(json: String): LiveLogRisultato {
        return try {
            val root = JSONObject(json)
            val oscam = root.getJSONObject("oscam")
            val lines = oscam.optJSONArray("lines")

            if (lines == null) {
                return LiveLogRisultato(
                    righe = emptyList(),
                    ultimoId = "",
                    valido = false
                )
            }

            val righe = mutableListOf<String>()
            var ultimoId =
                oscam.optString("lastid", "")
                    .ifBlank { root.optString("lastid", "") }

            for (indice in 0 until lines.length()) {
                val elemento = lines.opt(indice)

                when (elemento) {
                    is JSONObject -> {
                        val idElemento =
                            sequenceOf(
                                "id",
                                "lineid",
                                "lastid",
                                "logid"
                            )
                                .map { chiave ->
                                    elemento.optString(chiave, "").trim()
                                }
                                .firstOrNull { it.isNotBlank() }
                                .orEmpty()

                        if (idElemento.isNotBlank()) {
                            ultimoId = idElemento
                        }

                        // Nelle build OSCam/Streamboard il campo "line" del Live Log
                        // e' codificato in Base64. Lo decodifichiamo prima di mostrarlo.
                        val lineaCodificata =
                            elemento.optString("line", "").trim()

                        val testoDiretto =
                            if (lineaCodificata.isNotBlank()) {
                                decodificaLineaLiveLog(lineaCodificata)
                            } else {
                                sequenceOf(
                                    "text",
                                    "txt",
                                    "message",
                                    "msg",
                                    "log"
                                )
                                    .map { chiave ->
                                        elemento.optString(chiave, "").trimEnd()
                                    }
                                    .firstOrNull { it.isNotBlank() }
                                    .orEmpty()
                            }

                        val testo =
                            if (testoDiretto.isNotBlank()) {
                                testoDiretto
                            } else {
                                val parti = mutableListOf<String>()
                                val chiavi = elemento.keys()

                                while (chiavi.hasNext()) {
                                    val chiave = chiavi.next()

                                    if (
                                        chiave.equals("id", ignoreCase = true) ||
                                        chiave.equals("lineid", ignoreCase = true) ||
                                        chiave.equals("lastid", ignoreCase = true) ||
                                        chiave.equals("logid", ignoreCase = true)
                                    ) {
                                        continue
                                    }

                                    val valore = elemento.opt(chiave)
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty()

                                    if (valore.isNotBlank() && valore != "null") {
                                        parti.add(valore)
                                    }
                                }

                                parti.joinToString(" ")
                            }

                        if (testo.isNotBlank()) {
                            righe.add(testo)
                        }
                    }

                    null, JSONObject.NULL -> Unit

                    else -> {
                        val testo = elemento.toString().trimEnd()
                        if (testo.isNotBlank()) {
                            righe.add(testo)
                        }
                    }
                }
            }

            LiveLogRisultato(
                righe = righe,
                ultimoId = ultimoId,
                valido = true
            )
        } catch (_: Exception) {
            LiveLogRisultato(
                righe = emptyList(),
                ultimoId = "",
                valido = false
            )
        }
    }

    private fun decodificaLineaLiveLog(valore: String): String {
        if (valore.isBlank()) {
            return ""
        }

        return try {
            String(
                Base64.decode(valore, Base64.DEFAULT),
                Charsets.UTF_8
            ).trimEnd('\r', '\n')
        } catch (_: Exception) {
            // Fallback per eventuali build OSCam che restituiscono gia' testo normale.
            valore.trimEnd()
        }
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

    fun estraiUsersJson(json: String): String {
        return try {
            JSONObject(json)
                .getJSONObject("oscam")
                .getJSONObject("totals")
                .optString("total_users", "")
        } catch (_: Exception) {
            ""
        }
    }

    fun estraiElencoUsersJson(
        json: String,
        htmlUserConfig: String
    ): List<String> {
        return try {
            val nomiUtenti =
                Regex(
                    """user_edit\.html\?user=([^"&]+)""",
                    RegexOption.IGNORE_CASE
                )
                    .findAll(htmlUserConfig)
                    .mapNotNull { match ->
                        try {
                            java.net.URLDecoder.decode(
                                match.groupValues[1],
                                "UTF-8"
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    .distinct()
                    .toList()

            val nomiPerMd5 =
                nomiUtenti.associateBy { nome ->
                    "id_" + MessageDigest
                        .getInstance("MD5")
                        .digest(nome.toByteArray(Charsets.UTF_8))
                        .joinToString("") { byte ->
                            "%02x".format(byte.toInt() and 0xff)
                        }
                }

            val oscam = JSONObject(json).getJSONObject("oscam")
            val users = oscam.optJSONArray("users")
                ?: return emptyList()

            val elenco = mutableListOf<String>()

            for (indice in 0 until users.length()) {
                val user = users
                    .getJSONObject(indice)
                    .getJSONObject("user")

                val id = user.optString("usermd5", "")
                val nome =
                    nomiPerMd5[id]
                        ?: "Utente sconosciuto"

                val statoOriginale =
                    user.optString("status", "unknown")

                val stato =
                    when {
                        statoOriginale.equals(
                            "connected",
                            ignoreCase = true
                        ) -> "CONNESSO"

                        statoOriginale.equals(
                            "offline",
                            ignoreCase = true
                        ) -> "OFFLINE"

                        else -> statoOriginale.uppercase()
                    }

                val righe = mutableListOf(
                    "$nome — $stato"
                )

                val ip = user.optString("ip", "").trim()
                val protocollo =
                    user.optString("protocol", "").trim()
                val canale =
                    user.optString(
                        "lastchanneltitle",
                        user.optString("lastchannel", "")
                    ).trim()

                if (ip.isNotBlank()) {
                    righe.add("IP: $ip")
                }

                if (protocollo.isNotBlank()) {
                    righe.add("Protocollo: $protocollo")
                }

                if (canale.isNotBlank()) {
                    righe.add("Canale: $canale")
                }

                elenco.add(righe.joinToString("\n"))
            }

            elenco.sortedWith(
                compareByDescending<String> {
                    it.contains("— CONNESSO")
                }.thenBy {
                    it.substringBefore(" — ").lowercase()
                }
            )
        } catch (_: Exception) {
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
        percorso: String,
        richiestaAjax: Boolean = false
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
                percorso = percorso,
                richiestaAjax = richiestaAjax
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
        percorso: String,
        richiestaAjax: Boolean = false
    ): Request {
        val builder = Request.Builder()
            .url("http://$host:$porta/$percorso")

        if (richiestaAjax) {
            builder
                .header("X-Requested-With", "XMLHttpRequest")
                .header(
                    "Accept",
                    "application/json, text/javascript, */*; q=0.01"
                )
                .header("Cache-Control", "no-cache")
        }

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