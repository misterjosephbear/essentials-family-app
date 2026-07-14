package com.isaacshub.app.routehelper.network

import android.content.Context
import android.util.Log
import com.isaacshub.app.routehelper.domain.GeoPoint
import com.isaacshub.app.routehelper.domain.HouseNumberRange
import com.isaacshub.app.routehelper.domain.TigerAddressFeature
import com.isaacshub.app.routehelper.domain.filterAddressesNearBuildings
import com.isaacshub.app.routehelper.domain.interpolateAddresses
import com.isaacshub.app.routehelper.domain.parseParity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

private const val TAG = "TigerAddressFetcher"
private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"
private const val TIGER_YEAR = "2023"

/**
 * Address source for Route Helper: Census TIGER/Line ADDRFEAT data, which covers every road in the
 * country with left/right house-number ranges - unlike OpenStreetMap, whose per-building address
 * tagging is only as complete as local volunteer mapping, and is near-empty in most rural counties.
 * Each county's ADDRFEAT shapefile (~1MB zipped) is downloaded once from census.gov and cached under
 * filesDir, so repeat ZIP lookups in the same county are instant and offline-capable afterward.
 */
class TigerAddressFetcher(private val context: Context) {

    private val geocoder = NominatimGeocoder()
    private val fipsLookup by lazy { CountyFipsLookup(context) }
    private val buildingFootprintFetcher = BuildingFootprintFetcher(context)

    /** Just the center point of a ZIP code, for zooming a map to it (e.g. testing mode) without a full address fetch. */
    suspend fun geocodeZip(zip: String): GeoPoint? = withContext(Dispatchers.IO) {
        geocoder.geocodeZip(zip)?.center
    }

    suspend fun fetchAddressesForZip(zip: String): AddressFetchResult = withContext(Dispatchers.IO) {
        val location = geocoder.geocodeZip(zip)
            ?: return@withContext AddressFetchResult.Failure("Couldn't find that ZIP code")
        val fips = fipsLookup.lookup(location.stateAbbreviation, location.county)
            ?: return@withContext AddressFetchResult.Failure(
                "Couldn't map ${location.county}, ${location.stateAbbreviation} to a Census county"
            )

        val (dbfBytes, shpBytes) = try {
            loadCountyShapefile(fips.stateFp, fips.countyFp)
        } catch (e: Exception) {
            return@withContext AddressFetchResult.Failure("Couldn't download Census address data: ${e.message}")
        }

        val rawRecords = TigerDbfParser.parse(dbfBytes)
        val lines = TigerShpParser.parseLines(shpBytes)
        val interpolated = rawRecords.zip(lines)
            .asSequence()
            .filter { (record, _) -> !record.isDeleted && (record.zipL == zip || record.zipR == zip) }
            .flatMap { (record, points) ->
                val feature = TigerAddressFeature(
                    streetName = record.fullName,
                    leftRange = HouseNumberRange(record.lFromHn.toIntOrNull(), record.lToHn.toIntOrNull(), parseParity(record.parityL)),
                    rightRange = HouseNumberRange(record.rFromHn.toIntOrNull(), record.rToHn.toIntOrNull(), parseParity(record.parityR)),
                    zipLeft = record.zipL,
                    zipRight = record.zipR,
                    vertices = points
                )
                interpolateAddresses(feature, zip)
            }
            .toList()

        val buildingCentroids = buildingFootprintFetcher.fetchNearbyBuildingCentroids(location.center)
        if (buildingCentroids.isEmpty()) {
            Log.w(TAG, "No building footprints available for ZIP $zip - skipping phantom-address filter")
        }
        val filtered = filterAddressesNearBuildings(interpolated, buildingCentroids)

        AddressFetchResult.Success(filtered.map { FetchedAddress(it.label, it.location) })
    }

    private fun loadCountyShapefile(stateFp: String, countyFp: String): Pair<ByteArray, ByteArray> {
        val cacheDir = File(context.filesDir, "tiger_cache").apply { mkdirs() }
        val fileName = "tl_${TIGER_YEAR}_$stateFp${countyFp}_addrfeat.zip"
        val zipFile = File(cacheDir, fileName)
        if (!zipFile.exists()) {
            downloadTo(zipFile, "https://www2.census.gov/geo/tiger/TIGER$TIGER_YEAR/ADDRFEAT/$fileName")
        }

        var dbfBytes: ByteArray? = null
        var shpBytes: ByteArray? = null
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name.endsWith(".dbf") -> dbfBytes = zis.readBytes()
                    entry.name.endsWith(".shp") -> shpBytes = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        if (dbfBytes == null || shpBytes == null) {
            zipFile.delete()
        }
        return (dbfBytes ?: error("Missing .dbf in county address data")) to
            (shpBytes ?: error("Missing .shp in county address data"))
    }

    private fun downloadTo(target: File, url: String) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                error("HTTP $code fetching county address data")
            }
            val tmp = File(target.parentFile, "${target.name}.part")
            conn.inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
            tmp.renameTo(target)
        } finally {
            conn.disconnect()
        }
    }
}
