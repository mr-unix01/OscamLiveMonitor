package com.mrunix.oscamlivemonitor

internal fun caricaServerSalvati(
    preferences: android.content.SharedPreferences
): List<OscamServer> {
    val risultato = mutableListOf<OscamServer>()

    val jsonSalvato =
        preferences.getString("server_salvati", "") ?: ""

    if (jsonSalvato.isNotBlank()) {
        try {
            val array = org.json.JSONArray(jsonSalvato)

            for (indice in 0 until array.length()) {
                val oggetto = array.getJSONObject(indice)

                risultato.add(
                    OscamServer(
                        nome = oggetto.optString("nome", "Server"),
                        host = oggetto.optString("host", ""),
                        porta = oggetto.optString("porta", ""),
                        username = oggetto.optString("username", ""),
                        password = SecurePasswordCrypto.decrypt(
                            oggetto.optString("password", "")
                        )
                    )
                )
            }
        } catch (_: Exception) {
            risultato.clear()
        }
    }

    if (risultato.isEmpty()) {
        val hostEsistente =
            preferences.getString("host", "") ?: ""

        val portaEsistente =
            preferences.getString("porta", "") ?: ""

        if (
            hostEsistente.isNotBlank() &&
            portaEsistente.isNotBlank()
        ) {
            risultato.add(
                OscamServer(
                    nome =
                        preferences.getString(
                            "nome_server",
                            "Server principale"
                        ) ?: "Server principale",
                    host = hostEsistente,
                    porta = portaEsistente,
                    username =
                        preferences.getString(
                            "username",
                            ""
                        ) ?: "",
                    password =
                        SecurePasswordCrypto.decrypt(
                            preferences.getString(
                                "password",
                                ""
                            ) ?: ""
                        )
                )
            )
        }
    }

    return risultato
}

internal fun serverSalvatiRichiedonoMigrazione(
    json: String
): Boolean {
    if (json.isBlank()) {
        return false
    }

    return runCatching {
        val array = org.json.JSONArray(json)

        for (indice in 0 until array.length()) {
            val password =
                array.getJSONObject(indice)
                    .optString(
                        "password",
                        ""
                    )

            if (
                password.isNotEmpty() &&
                !SecurePasswordCrypto.isEncrypted(
                    password
                )
            ) {
                return@runCatching true
            }
        }

        false
    }.getOrDefault(false)
}

internal fun salvaServerSalvati(
    preferences: android.content.SharedPreferences,
    server: List<OscamServer>
) {
    val array = org.json.JSONArray()

    server.forEach { elemento ->
        val oggetto = org.json.JSONObject()

        oggetto.put("nome", elemento.nome)
        oggetto.put("host", elemento.host)
        oggetto.put("porta", elemento.porta)
        oggetto.put("username", elemento.username)
        oggetto.put(
            "password",
            SecurePasswordCrypto.encrypt(
                elemento.password
            )
        )

        array.put(oggetto)
    }

    preferences.edit()
        .putString("server_salvati", array.toString())
        .apply()
}
