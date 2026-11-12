package com.mrunix.oscamlivemonitor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebIfAuthSecurityTest {

    @Test
    fun hostCorrettoAutorizzato() {
        assertTrue(webIfAuthHostAutorizzato("192.168.1.72", "192.168.1.72"))
    }

    @Test
    fun hostDiversoRifiutato() {
        assertFalse(webIfAuthHostAutorizzato("192.168.1.72", "example.com"))
    }

    @Test
    fun hostMancanteRifiutato() {
        assertFalse(webIfAuthHostAutorizzato("192.168.1.72", null))
        assertFalse(webIfAuthHostAutorizzato(null, "192.168.1.72"))
        assertFalse(webIfAuthHostAutorizzato("", "192.168.1.72"))
    }

    @Test
    fun hostnameNonDistingueMaiuscole() {
        assertTrue(webIfAuthHostAutorizzato("Oscam.Local", "oscam.local"))
    }
}
