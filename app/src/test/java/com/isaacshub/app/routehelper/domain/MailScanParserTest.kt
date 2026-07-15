package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MailScanParserTest {

    @Test
    fun `single address with name line is parsed with last name`() {
        val text = "JOHN SMITH\n123 MAIN ST\nSpringfield, IL 62704"
        val results = parseScannedAddresses(text)
        assertEquals(1, results.size)
        assertEquals("123 MAIN ST, Springfield, IL 62704", results[0].addressText)
        assertEquals("Smith", results[0].recipientLastName)
    }

    @Test
    fun `street line without city state zip still parses`() {
        val results = parseScannedAddresses("456 Oak Avenue")
        assertEquals(1, results.size)
        assertEquals("456 Oak Avenue", results[0].addressText)
        assertEquals(null, results[0].recipientLastName)
    }

    @Test
    fun `two address blocks both surface as candidates`() {
        val text = """
            JANE DOE
            789 Elm Drive
            Carmel, IN 46032
            RETURN TO
            12 Birch Court
            Fishers, IN 46037
        """.trimIndent()
        val results = parseScannedAddresses(text)
        assertEquals(2, results.size)
        assertTrue(results.any { it.addressText == "789 Elm Drive, Carmel, IN 46032" })
        assertTrue(results.any { it.addressText == "12 Birch Court, Fishers, IN 46037" })
    }

    @Test
    fun `no street shaped line yields no candidates`() {
        val results = parseScannedAddresses("Happy Birthday!\nWishing you the best.")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `duplicate address lines are only returned once`() {
        val text = "100 First St\n100 First St"
        val results = parseScannedAddresses(text)
        assertEquals(1, results.size)
    }
}
