package com.isaacshub.app.vault.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultConnectionTest {

    @Test
    fun `parses a valid pairing payload`() {
        val raw = """{"type":"isaacs-hub-storage-pairing","version":1,"baseUrl":"http://10.0.0.5:4000/","apiKey":"secret123"}"""
        val result = parsePairingPayload(raw)
        assertEquals(VaultConnection("http://10.0.0.5:4000", "secret123"), result)
    }

    @Test
    fun `rejects a payload with the wrong type tag`() {
        val raw = """{"type":"something-else","baseUrl":"http://10.0.0.5:4000","apiKey":"secret123"}"""
        assertNull(parsePairingPayload(raw))
    }

    @Test
    fun `rejects malformed json`() {
        assertNull(parsePairingPayload("not json at all"))
    }

    @Test
    fun `rejects a payload missing the api key`() {
        val raw = """{"type":"isaacs-hub-storage-pairing","baseUrl":"http://10.0.0.5:4000","apiKey":""}"""
        assertNull(parsePairingPayload(raw))
    }
}
