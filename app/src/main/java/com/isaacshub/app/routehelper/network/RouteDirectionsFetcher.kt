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

        // Detect turn-around points and handle them specially
        val turnaroundIndices = detectTurnarounds(waypoints)
        if (turnaroundIndices.isNotEmpty()) {
            Log.d(TAG, "Detected ${turnaroundIndices.size} turnarounds at indices: $turnaroundIndices")
            return@withContext fetchRouteWithTurnarounds(waypoints, turnaroundIndices)
        }

        // No turnarounds - process normally
        if (waypoints.size > MAX_WAYPOINTS_PER_REQUEST) {
            Log.d(TAG, "Route has ${waypoints.size} waypoints, splitting into chunks of $MAX_WAYPOINTS_PER_REQUEST")
            return@withContext fetchRouteInChunks(waypoints)
        }

        fetchSingleRoute(waypoints)
    }

    /**
     * Detects indices of waypoints that are turn-around points (where bearing changes > 90°).
     */
    private fun detectTurnarounds(waypoints: List<GeoPoint>): List<Int> {
        if (waypoints.size < 3) return emptyList()

        val turnarounds = mutableListOf<Int>()

        for (i in 1 until waypoints.size - 1) {
            val prev = waypoints[i - 1]
            val current = waypoints[i]
            val next = waypoints[i + 1]

            val bearingIn = calculateBearing(prev, current)
            val bearingOut = calculateBearing(current, next)
            val bearingDiff = normalizeBearingDiff(bearingOut - bearingIn)

            if (bearingDiff > 90.0) {
                turnarounds.add(i)
                Log.d(TAG, "Detected turnaround at waypoint $i: bearing change ${bearingDiff.toInt()}°")
            }
        }

        return turnarounds
    }

    /**
     * Fetches route with turnarounds by splitting into segments and handling turnarounds with straight lines.
     * For turnaround stops, we skip the OSRM routing since the driver does a K-turn right at the address.
     */
    private suspend fun fetchRouteWithTurnarounds(waypoints: List<GeoPoint>, turnaroundIndices: List<Int>): List<GeoPoint>? {
        val result = mutableListOf<GeoPoint>()
        var segmentStart = 0

        for (turnaroundIdx in turnaroundIndices.sorted() + listOf(waypoints.size)) {
            // Fetch OSRM route up to (but not including) the turnaround
            if (turnaroundIdx > segmentStart) {
                val segment = waypoints.subList(segmentStart, turnaroundIdx + if (turnaroundIdx < waypoints.size) 1 else 0)
                val segmentRoute = if (segment.size > MAX_WAYPOINTS_PER_REQUEST) {
                    fetchRouteInChunks(segment)
                } else {
                    fetchSingleRoute(segment)
                }

                if (segmentRoute == null) return null

                // Add segment, avoiding duplicates at joins
                if (result.isEmpty()) {
                    result.addAll(segmentRoute)
                } else {
                    result.addAll(segmentRoute.drop(1))
                }
            }

            // Handle turnaround with straight line back
            if (turnaroundIdx < waypoints.size - 1) {
                val turnaroundStop = waypoints[turnaroundIdx]
                val nextStop = waypoints[turnaroundIdx + 1]

                // Add straight line from turnaround back to next stop (driver does K-turn here)
                result.add(turnaroundStop)
                result.add(nextStop)

                Log.d(TAG, "Added straight-line turnaround at waypoint $turnaroundIdx")
                segmentStart = turnaroundIdx + 1
            }
        }

        return result
    }

    /**
     * Create a new point offset from the given point by a distance in a specific bearing.
     * Used to create virtual turnaround waypoints.
     */
    private fun offsetPoint(point: GeoPoint, bearingDegrees: Double, distanceMeters: Double): GeoPoint {
        val earthRadiusMeters = 6371000.0
        val bearingRad = Math.toRadians(bearingDegrees)
        val latRad = Math.toRadians(point.latitude)
        val lonRad = Math.toRadians(point.longitude)
        val angularDistance = distanceMeters / earthRadiusMeters

        val newLatRad = kotlin.math.asin(
            kotlin.math.sin(latRad) * kotlin.math.cos(angularDistance) +
            kotlin.math.cos(latRad) * kotlin.math.sin(angularDistance) * kotlin.math.cos(bearingRad)
        )

        val newLonRad = lonRad + kotlin.math.atan2(
            kotlin.math.sin(bearingRad) * kotlin.math.sin(angularDistance) * kotlin.math.cos(latRad),
            kotlin.math.cos(angularDistance) - kotlin.math.sin(latRad) * kotlin.math.sin(newLatRad)
        )

        return GeoPoint(
            latitude = Math.toDegrees(newLatRad),
            longitude = Math.toDegrees(newLonRad)
        )
    }

    /**
     * Calculate bearing in degrees from point A to point B.
     * Returns value in range [0, 360).
     */
    private fun calculateBearing(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
                kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)

        val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
        return (bearing + 360) % 360  // Normalize to 0-360
    }

    /**
     * Normalize bearing difference to range [0, 180].
     * This handles the wraparound (e.g., 350° to 10° is a 20° change, not 340°).
     */
    private fun normalizeBearingDiff(diff: Double): Double {
        val normalized = ((diff % 360) + 360) % 360
        return if (normalized > 180) 360 - normalized else normalized
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
