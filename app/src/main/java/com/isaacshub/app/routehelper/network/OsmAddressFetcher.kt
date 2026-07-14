package com.isaacshub.app.routehelper.network

import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class FetchedAddress(val label: String, val location: GeoPoint)

private data class BoundingBox(val south: Double, val north: Double, val west: Double, val east: Double)

private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"

/**
 * Looks up every address OpenStreetMap knows about within a ZIP code, for the one-time fetch when
 * building a new route. Two public OSM services, chained: Nominatim resolves the ZIP to a bounding
 * box, then Overpass returns every node/way tagged with a house number inside it. Both are free,
 * unauthenticated, low-volume-fair-use services - fine for one lookup per new route, not for
 * frequent polling.
 */
class OsmAddressFetcher {

    suspend fun fetchAddressesForZip(zipCode: String): List<FetchedAddress> = withContext(Dispatchers.IO) {
        val bbox = fetchZipBoundingBox(zipCode) ?: return@withContext emptyList()
        fetchAddressesInBoundingBox(bbox)
    }

    private fun fetchZipBoundingBox(zipCode: String): BoundingBox? {
        val encodedZip = URLEncoder.encode(zipCode, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?postalcode=$encodedZip&country=us&format=json&limit=1")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(body)
            if (results.length() == 0) return null
            val box = results.getJSONObject(0).getJSONArray("boundingbox")
            BoundingBox(
                south = box.getString(0).toDouble(),
                north = box.getString(1).toDouble(),
                west = box.getString(2).toDouble(),
                east = box.getString(3).toDouble()
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchAddressesInBoundingBox(bbox: BoundingBox): List<FetchedAddress> {
        val query = """
            [out:json][timeout:60];
            (
              node["addr:housenumber"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
              way["addr:housenumber"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
            );
            out center;
        """.trimIndent()

        val url = URL("https://overpass-api.de/api/interpreter")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 20_000
        conn.readTimeout = 90_000
        return try {
            val payload = "data=" + URLEncoder.encode(query, "UTF-8")
            conn.outputStream.use { it.write(payload.toByteArray()) }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseOverpassResponse(body)
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    private fun parseOverpassResponse(body: String): List<FetchedAddress> {
        val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        return (0 until elements.length()).mapNotNull { i ->
            val element = elements.getJSONObject(i)
            val tags = element.optJSONObject("tags") ?: return@mapNotNull null
            val houseNumber = tags.optString("addr:housenumber").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val street = tags.optString("addr:street").takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val point = when {
                element.has("lat") && element.has("lon") ->
                    GeoPoint(element.getDouble("lat"), element.getDouble("lon"))
                element.has("center") -> {
                    val center = element.getJSONObject("center")
                    GeoPoint(center.getDouble("lat"), center.getDouble("lon"))
                }
                else -> null
            } ?: return@mapNotNull null

            FetchedAddress("$houseNumber $street", point)
        }
    }
}
