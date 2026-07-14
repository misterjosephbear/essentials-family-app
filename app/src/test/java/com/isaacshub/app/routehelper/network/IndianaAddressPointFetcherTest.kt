package com.isaacshub.app.routehelper.network

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndianaAddressPointFetcherTest {

    @Test
    fun `rejects a non-5-digit zip before making any network call`() = runBlocking {
        val fetcher = IndianaAddressPointFetcher()
        val result = fetcher.fetchAddressesForZip("not-a-zip")
        assertTrue(result is AddressFetchResult.Failure)
    }

    @Test
    fun `rejects a zip with sql-significant characters instead of embedding them in the query`() = runBlocking {
        val fetcher = IndianaAddressPointFetcher()
        val result = fetcher.fetchAddressesForZip("47246' OR '1'='1")
        assertTrue(result is AddressFetchResult.Failure)
    }

    @Test
    fun `buildLabel treats JSON null fields as absent instead of the literal string 'null'`() {
        // Regression test: add_full is null for this dataset's Bartholomew County records, and
        // org.json's optString returns the literal string "null" for a JSON-null value rather than
        // blank - both together produced address rows that displayed as the word "null".
        val attrs = JSONObject(
            """{"add_number": 1006, "st_name": "JACKSON", "st_postyp": "ST", "st_predir": null, "st_posdir": null, "addnum_pre": null, "addnum_suf": null, "st_premod": null, "st_pretyp": null, "st_posmod": null}"""
        )
        assertEquals("1006 JACKSON ST", IndianaAddressPointFetcher().buildLabel(attrs))
    }

    @Test
    fun `buildLabel returns null when there's no usable house number`() {
        val attrs = JSONObject("""{"add_number": null, "st_name": "JACKSON"}""")
        assertNull(IndianaAddressPointFetcher().buildLabel(attrs))
    }
}
