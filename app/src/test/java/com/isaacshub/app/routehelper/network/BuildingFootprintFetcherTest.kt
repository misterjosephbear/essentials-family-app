package com.isaacshub.app.routehelper.network

import com.isaacshub.app.routehelper.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildingFootprintFetcherTest {

    @Test
    fun `firstRingPointOf extracts the first lon-lat pair from a geojsonl line`() {
        val line = """{"type": "Feature", "properties": { "height": -1.0, "confidence": -1.0},"geometry": {"type": "Polygon","coordinates": [[[-85.07952376067796, 39.28442756853421], [-85.07952700955234, 39.28453546511142]]]}}"""
        val point = firstRingPointOf(line)
        assertEquals(GeoPoint(39.28442756853421, -85.07952376067796), point)
    }

    @Test
    fun `firstRingPointOf returns null for a line with no coordinates key`() {
        assertNull(firstRingPointOf("""{"type": "Feature"}"""))
    }

    @Test
    fun `quadKeyFor matches known Bing tile system reference values`() {
        // Cross-checked against the standard Bing Maps tile system formula for these coordinates.
        assertEquals("032000132", quadKeyFor(GeoPoint(39.2988675, -85.7400666)))
        assertEquals("032000123", quadKeyFor(GeoPoint(39.2976804, -85.9581559)))
    }

    @Test
    fun `quadKeyFor produces a string of length equal to the zoom level`() {
        assertEquals(9, quadKeyFor(GeoPoint(39.3, -85.75)).length)
        assertEquals(5, quadKeyFor(GeoPoint(39.3, -85.75), zoom = 5).length)
    }

    @Test
    fun `quadKeyFor is stable for nearby points within the same tile`() {
        val a = quadKeyFor(GeoPoint(39.3042, -85.7608))
        val b = quadKeyFor(GeoPoint(39.3043, -85.7609))
        assertEquals(a, b)
    }
}
