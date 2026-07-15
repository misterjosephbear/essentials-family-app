package com.isaacshub.app.routehelper.network

import android.util.Log
import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RouteDirectionsFetcher"
private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"

/**
 * Fetches a real road-following path through a sequence of stops from OSRM's free public routing
 * server, matching the OSM-only approach used for the map itself and for address lookups elsewhere
 * in Route Helper. Returns null on any failure (including too few waypoints) - callers should fall
 * back to a straight line between stops rather than showing nothing.
 */
class RouteDirectionsFetcher {

    suspend fun fetchDrivingRoute(waypoints: List<GeoPoint>): List<GeoPoint>? = withContext(Dispatchers.IO) {
        if (waypoints.size < 2) return@withContext null
        val coords = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
        val url = URL("https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "OSRM returned HTTP $code for ${waypoints.size} waypoints")
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            if (root.optString("code") != "Ok") {
                Log.w(TAG, "OSRM route failed: ${root.optString("message", root.optString("code"))}")
                return@withContext null
            }
            val routes = root.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null
            val coordinates = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
            (0 until coordinates.length()).map { i ->
                val pair = coordinates.getJSONArray(i)
                GeoPoint(latitude = pair.getDouble(1), longitude = pair.getDouble(0))
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM route fetch failed", e)
            null
        } finally {
            conn.disconnect()
        }
    }
}
