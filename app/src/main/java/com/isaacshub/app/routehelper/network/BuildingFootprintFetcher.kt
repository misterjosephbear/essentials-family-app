package com.isaacshub.app.routehelper.network

import android.content.Context
import android.util.Log
import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "BuildingFootprintFetcher"
private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"

/** Zoom level Microsoft's Global ML Building Footprints dataset is partitioned at. */
private const val TILE_ZOOM = 9

/** Bing Maps tile system: lat/lon -> the zoom-9 quadkey identifying which dataset partition covers it. */
fun quadKeyFor(point: GeoPoint, zoom: Int = TILE_ZOOM): String {
    val sinLat = sin(point.latitude * PI / 180)
    val x = (point.longitude + 180) / 360
    val y = 0.5 - ln((1 + sinLat) / (1 - sinLat)) / (4 * PI)
    val mapSize = 1L shl zoom
    val tileX = min((x * mapSize).toLong(), mapSize - 1)
    val tileY = min((y * mapSize).toLong(), mapSize - 1)

    val builder = StringBuilder()
    for (i in zoom downTo 1) {
        var digit = 0
        val mask = 1L shl (i - 1)
        if (tileX and mask != 0L) digit += 1
        if (tileY and mask != 0L) digit += 2
        builder.append(digit)
    }
    return builder.toString()
}

/**
 * Looks up real building footprints near a point, from Microsoft's Global ML Building Footprints
 * dataset (satellite-derived, ODbL-licensed, free). The dataset is partitioned into ~2,400 US tiles;
 * `building_footprint_tiles.csv` (bundled asset) maps each zoom-9 quadkey to its download URL, sourced
 * from Microsoft's `dataset-links.csv` index as of the 2026-02-03 dataset snapshot - if Microsoft
 * rotates the dataset's storage path, this asset will need refreshing from the same source.
 */
class BuildingFootprintFetcher(private val context: Context) {

    private val tileUrlByQuadKey: Map<String, String> by lazy {
        context.assets.open("building_footprint_tiles.csv").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split(",", limit = 2)
                if (parts.size != 2) null else parts[0] to parts[1]
            }.toMap()
        }
    }

    /** Best-effort: returns an empty list (never throws) if the tile isn't indexed or the download/parse fails. */
    suspend fun fetchNearbyBuildingCentroids(center: GeoPoint): List<GeoPoint> = withContext(Dispatchers.IO) {
        val quadKey = quadKeyFor(center)
        val url = tileUrlByQuadKey[quadKey]
        if (url == null) {
            Log.w(TAG, "No building footprint tile indexed for quadkey $quadKey")
            return@withContext emptyList()
        }
        try {
            val bytes = loadTile(quadKey, url)
            parseCentroids(bytes)
        } catch (e: Exception) {
            Log.w(TAG, "Building footprint fetch failed for quadkey $quadKey: ${e.message}", e)
            emptyList()
        }
    }

    private fun loadTile(quadKey: String, url: String): ByteArray {
        val cacheDir = File(context.filesDir, "building_footprint_cache").apply { mkdirs() }
        val cacheFile = File(cacheDir, "$quadKey.csv.gz")
        if (!cacheFile.exists()) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            try {
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) error("HTTP $code fetching building footprint tile")
                val tmp = File(cacheDir, "$quadKey.csv.gz.part")
                conn.inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                tmp.renameTo(cacheFile)
            } finally {
                conn.disconnect()
            }
        }
        return GZIPInputStream(cacheFile.inputStream()).use { it.readBytes() }
    }

    private fun parseCentroids(bytes: ByteArray): List<GeoPoint> =
        String(bytes, Charsets.UTF_8).lineSequence()
            .mapNotNull { line -> if (line.isBlank()) null else firstRingPointOf(line) }
            .toList()
}

/**
 * A tile is ~70k lines - full JSON parsing (JSONObject/JSONArray per line) took over a minute on a
 * real device for that volume, almost entirely GC/allocation overhead from building nested JSON
 * structures we don't need. Since this is only ever compared against a ~40m proximity threshold, a
 * building's first ring vertex is close enough to its true centroid to use directly, and scanning
 * for it as plain text (no parser, no per-line object graph) is orders of magnitude faster.
 */
internal fun firstRingPointOf(geojsonLine: String): GeoPoint? {
    val key = "\"coordinates\":"
    val keyIndex = geojsonLine.indexOf(key)
    if (keyIndex == -1) return null
    var i = keyIndex + key.length
    while (i < geojsonLine.length && (geojsonLine[i] == '[' || geojsonLine[i] == ' ')) i++

    val lonStart = i
    while (i < geojsonLine.length && isNumberChar(geojsonLine[i])) i++
    val lon = geojsonLine.substring(lonStart, i).toDoubleOrNull() ?: return null

    while (i < geojsonLine.length && (geojsonLine[i] == ',' || geojsonLine[i] == ' ')) i++
    val latStart = i
    while (i < geojsonLine.length && isNumberChar(geojsonLine[i])) i++
    val lat = geojsonLine.substring(latStart, i).toDoubleOrNull() ?: return null

    return GeoPoint(latitude = lat, longitude = lon)
}

private fun isNumberChar(c: Char) = c.isDigit() || c == '-' || c == '.' || c == '+' || c == 'e' || c == 'E'
