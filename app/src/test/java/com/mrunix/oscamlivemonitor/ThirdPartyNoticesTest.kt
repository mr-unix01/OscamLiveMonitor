package com.mrunix.oscamlivemonitor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ThirdPartyNoticesTest {

    @Test
    fun licenzeRepositoryERisorsaAndroidCoincidono() {
        val originale = File("../THIRD_PARTY_NOTICES")
            .readText(Charsets.UTF_8)

        val risorsa = File("src/main/res/raw/third_party_notices.txt")
            .readText(Charsets.UTF_8)

        fun normalizza(testo: String): String =
            testo.replace("\r\n", "\n").replace("\r", "\n")

        assertEquals(
            "Le licenze nel repository e nell'app devono coincidere",
            normalizza(originale),
            normalizza(risorsa)
        )
    }
}
