package com.isaacshub.app.routehelper.network

import android.util.Log
import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "OsmAddressFetcher"

data class FetchedAddress(val label: String, val location: GeoPoint)

sealed interface AddressFetchResult {
    data class Success(val addresses: List<FetchedAddress>) : AddressFetchResult
    data class Failure(val reason: String) : AddressFetchResult
}

private data class BoundingBox(val south: Double, val north: Double, val west: Double, val east: Double)

private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"

/** The public overpass-api.de instance times out/errors under load often enough that a single try isn't reliable - fall back through mirrors in order. */
private val OVERPASS_ENDPOINTS = listOf(
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass.openstreetmap.ru/api/interpreter"
)

/**
 * Looks up every address OpenStreetMap knows about within a ZIP code, for the one-time fetch when
 * building a new route. Two public OSM services, chained: Nominatim resolves the ZIP to a bounding
 * box, then Overpass returns every node/way tagged with a house number inside it. Both are free,
 * unauthenticated, low-volume-fair-use services - fine for one lookup per new route, not for
 * frequent polling. A genuinely empty result (a ZIP with no addressed buildings mapped in OSM yet)
 * is a real, if rare, possibility distinct from a fetch failure - the caller can tell them apart.
 */
class OsmAddressFetcher {

    suspend fun fetchAddressesForZip(zipCode: String): AddressFetchResult = withContext(Dispatchers.IO) {
        val bbox = fetchZipBoundingBox(zipCode)
            ?: return@withContext AddressFetchResult.Failure("Couldn't find that ZIP code")
        fetchAddressesInBoundingBox(bbox)
    }

    /** Just the center point of a ZIP code, for zooming a map to it (e.g. testing mode) without doing a full address fetch. */
    suspend fun geocodeZip(zipCode: String): GeoPoint? = withContext(Dispatchers.IO) {
        fetchZipBoundingBox(zipCode)?.let { GeoPoint((it.south + it.north) / 2.0, (it.west + it.east) / 2.0) }
    }

    private fun fetchZipBoundingBox(zipCode: String): BoundingBox? {
        val encodedZip = URLEncoder.encode(zipCode, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?postalcode=$encodedZip&country=us&format=json&limit=1")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        return try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Nominatim returned HTTP $code for ZIP $zipCode")
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(body)
            if (results.length() == 0) {
                Log.w(TAG, "Nominatim found no match for ZIP $zipCode")
                return null
            }
            val box = results.getJSONObject(0).getJSONArray("boundingbox")
            BoundingBox(
                south = box.getString(0).toDouble(),
                north = box.getString(1).toDouble(),
                west = box.getString(2).toDouble(),
                east = box.getString(3).toDouble()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim lookup failed for ZIP $zipCode", e)
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchAddressesInBoundingBox(bbox: BoundingBox): AddressFetchResult {
        val query = """
            [out:json][timeout:55];
            (
              node["addr:housenumber"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
              way["addr:housenumber"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
            );
            out center;
        """.trimIndent()

        var lastFailureReason = "No response from any OpenStreetMap server"
        for (endpoint in OVERPASS_ENDPOINTS) {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            try {
                val payload = "data=" + URLEncoder.encode(query, "UTF-8")
                conn.outputStream.use { it.write(payload.toByteArray()) }
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    lastFailureReason = "$endpoint returned HTTP $code"
                    Log.w(TAG, lastFailureReason)
                    continue
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                return AddressFetchResult.Success(parseOverpassResponse(body))
            } catch (e: Exception) {
                lastFailureReason = "$endpoint failed: ${e.message}"
                Log.w(TAG, lastFailureReason, e)
            } finally {
                conn.disconnect()
            }
        }
        return AddressFetchResult.Failure(lastFailureReason)
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
