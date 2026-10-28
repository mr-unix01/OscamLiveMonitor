package com.mrunix.oscamlivemonitor

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerPersistenceInstrumentedTest {

    private lateinit var context: Context

    private val preferencesName =
        "server_persistence_instrumented_test"

    @Before
    fun setUp() {
        context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        preferences().edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        preferences().edit()
            .clear()
            .commit()
    }

    @Test
    fun serverDuplicatiRicevonoIdDiversi() {
        val primo =
            OscamServer(
                nome = "DM 920",
                host = "192.168.1.2",
                porta = "8010",
                username = "user",
                password = "password"
            )

        val secondo =
            OscamServer(
                nome = "DM 920",
                host = "192.168.1.2",
                porta = "8010",
                username = "user",
                password = "password"
            )

        assertNotEquals(
            primo.id,
            secondo.id
        )
    }

    @Test
    fun salvaERicaricaMantieneIdDeiDuplicati() {
        val primo =
            OscamServer(
                nome = "DM 920",
                host = "192.168.1.2",
                porta = "8010",
                username = "user1",
                password = "password1"
            )

        val secondo =
            OscamServer(
                nome = "DM 920",
                host = "192.168.1.2",
                porta = "8010",
                username = "user2",
                password = "password2"
            )

        salvaServerSalvati(
            preferences = preferences(),
            server = listOf(
                primo,
                secondo
            )
        )

        val caricati =
            caricaServerSalvati(
                preferences()
            )

        assertEquals(
            2,
            caricati.size
        )

        assertEquals(
            primo.id,
            caricati[0].id
        )

        assertEquals(
            secondo.id,
            caricati[1].id
        )

        assertNotEquals(
            caricati[0].id,
            caricati[1].id
        )

        assertEquals(
            "password1",
            caricati[0].password
        )

        assertEquals(
            "password2",
            caricati[1].password
        )
    }

    @Test
    fun vecchioJsonSenzaIdVieneRiconosciutoEMigrato() {
        val legacyJson =
            """
            [
              {
                "nome":"DM 920",
                "host":"192.168.1.2",
                "porta":"8010",
                "username":"user1",
                "password":"password1"
              },
              {
                "nome":"DM 920",
                "host":"192.168.1.2",
                "porta":"8010",
                "username":"user2",
                "password":"password2"
              }
            ]
            """.trimIndent()

        preferences().edit()
            .putString(
                "server_salvati",
                legacyJson
            )
            .commit()

        assertTrue(
            serverSalvatiRichiedonoMigrazione(
                legacyJson
            )
        )

        val caricati =
            caricaServerSalvati(
                preferences()
            )

        assertEquals(
            2,
            caricati.size
        )

        assertTrue(
            caricati[0].id.isNotBlank()
        )

        assertTrue(
            caricati[1].id.isNotBlank()
        )

        assertNotEquals(
            caricati[0].id,
            caricati[1].id
        )

        salvaServerSalvati(
            preferences = preferences(),
            server = caricati
        )

        val jsonMigrato =
            preferences()
                .getString(
                    "server_salvati",
                    ""
                )
                .orEmpty()

        assertFalse(
            serverSalvatiRichiedonoMigrazione(
                jsonMigrato
            )
        )

        val ricaricati =
            caricaServerSalvati(
                preferences()
            )

        assertEquals(
            caricati[0].id,
            ricaricati[0].id
        )

        assertEquals(
            caricati[1].id,
            ricaricati[1].id
        )
    }

    private fun preferences() =
        context.getSharedPreferences(
            preferencesName,
            Context.MODE_PRIVATE
        )
}
