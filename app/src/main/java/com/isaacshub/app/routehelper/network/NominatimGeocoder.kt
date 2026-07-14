package com.isaacshub.app.routehelper.network

import android.util.Log
import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "NominatimGeocoder"
private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"

data class ZipLocation(val center: GeoPoint, val county: String, val stateAbbreviation: String)

/** Resolves a US ZIP code to a center point and its owning county/state, via the free Nominatim geocoder. */
class NominatimGeocoder {

    suspend fun geocodeZip(zipCode: String): ZipLocation? = withContext(Dispatchers.IO) {
        val encodedZip = URLEncoder.encode(zipCode, "UTF-8")
        val url = URL(
            "https://nominatim.openstreetmap.org/search?postalcode=$encodedZip&country=us&format=json&addressdetails=1&limit=1"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Nominatim returned HTTP $code for ZIP $zipCode")
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(body)
            if (results.length() == 0) {
                Log.w(TAG, "Nominatim found no match for ZIP $zipCode")
                return@withContext null
            }
            val result = results.getJSONObject(0)
            val center = GeoPoint(result.getString("lat").toDouble(), result.getString("lon").toDouble())
            val address = result.optJSONObject("address")
            val county = address?.optString("county").orEmpty()
            val stateAbbreviation = address?.optString("ISO3166-2-lvl4").orEmpty().substringAfter("-", "")
            if (county.isBlank() || stateAbbreviation.isBlank()) {
                Log.w(TAG, "Nominatim result for ZIP $zipCode missing county/state: $address")
                return@withContext null
            }
            ZipLocation(center, county, stateAbbreviation)
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim lookup failed for ZIP $zipCode", e)
            null
        } finally {
            conn.disconnect()
        }
    }
}
