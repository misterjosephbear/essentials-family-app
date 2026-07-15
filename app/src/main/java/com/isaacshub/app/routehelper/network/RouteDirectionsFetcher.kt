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

    companion object {
        private const val MAX_WAYPOINTS_PER_REQUEST = 100  // OSRM public API limit
    }

    suspend fun fetchDrivingRoute(waypoints: List<GeoPoint>): List<GeoPoint>? = withContext(Dispatchers.IO) {
        if (waypoints.size < 2) return@withContext null

        // If too many waypoints, split into chunks and fetch each chunk
        if (waypoints.size > MAX_WAYPOINTS_PER_REQUEST) {
            Log.d(TAG, "Route has ${waypoints.size} waypoints, splitting into chunks of $MAX_WAYPOINTS_PER_REQUEST")
            return@withContext fetchRouteInChunks(waypoints)
        }

        // Single request for routes under the limit
        fetchSingleRoute(waypoints)
    }

    private suspend fun fetchRouteInChunks(waypoints: List<GeoPoint>): List<GeoPoint>? {
        val allPoints = mutableListOf<GeoPoint>()

        // Split waypoints into overlapping chunks (overlap by 1 to ensure continuity)
        var startIndex = 0
        while (startIndex < waypoints.size) {
            val endIndex = minOf(startIndex + MAX_WAYPOINTS_PER_REQUEST, waypoints.size)
            val chunk = waypoints.subList(startIndex, endIndex)

            Log.d(TAG, "Fetching chunk $startIndex to $endIndex (${chunk.size} waypoints)")
            val chunkRoute = fetchSingleRoute(chunk)

            if (chunkRoute == null) {
                Log.w(TAG, "Failed to fetch chunk $startIndex-$endIndex, aborting")
                return null
            }

            // Add points from this chunk (skip first point if not first chunk to avoid duplicates)
            if (startIndex == 0) {
                allPoints.addAll(chunkRoute)
            } else {
                allPoints.addAll(chunkRoute.drop(1))
            }

            // Move to next chunk, overlapping by 1 waypoint
            startIndex = endIndex - 1
            if (startIndex >= waypoints.size - 1) break
        }

        Log.d(TAG, "Combined ${allPoints.size} total route points from chunks")
        return allPoints
    }

    private suspend fun fetchSingleRoute(waypoints: List<GeoPoint>): List<GeoPoint>? {
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
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            if (root.optString("code") != "Ok") {
                Log.w(TAG, "OSRM route failed: ${root.optString("message", root.optString("code"))}")
                return null
            }
            val routes = root.getJSONArray("routes")
            if (routes.length() == 0) return null
            val coordinates = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
            return (0 until coordinates.length()).map { i ->
                val pair = coordinates.getJSONArray(i)
                GeoPoint(latitude = pair.getDouble(1), longitude = pair.getDouble(0))
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM route fetch failed for ${waypoints.size} waypoints", e)
            return null
        } finally {
            conn.disconnect()
        }
    }
}
