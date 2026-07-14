package com.isaacshub.app.routehelper.network

import com.isaacshub.app.routehelper.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val USER_AGENT = "IsaacsHub/1.0 (personal route-building app)"
private const val SERVICE_URL =
    "https://gisdata.in.gov/server/rest/services/Hosted/Address_Points_of_Indiana_2022/FeatureServer/0/query"
private const val PAGE_SIZE = 2000
private val ZIP_PATTERN = Regex("^\\d{5}$")

/**
 * Indiana's own address point data: real, county-maintained points (E911/assessor-sourced),
 * published as open data by the state's GIS office. Every point is a real, assessed address, so
 * unlike interpolating from a Census TIGER house-number range, there's no numbering-gap phantom-
 * address problem to filter - a candidate is either a real address or it isn't in the results at
 * all. Indiana-only; the caller should fall back to [TigerAddressFetcher] for ZIPs this returns
 * nothing for (out of state, or this service unreachable).
 */
class IndianaAddressPointFetcher {

    suspend fun fetchAddressesForZip(zip: String): AddressFetchResult = withContext(Dispatchers.IO) {
        if (!ZIP_PATTERN.matches(zip)) {
            return@withContext AddressFetchResult.Failure("Not a 5-digit ZIP code")
        }
        try {
            val addresses = mutableListOf<FetchedAddress>()
            var offset = 0
            while (true) {
                val page = queryPage(zip, offset)
                addresses += page
                if (page.size < PAGE_SIZE) break
                offset += PAGE_SIZE
            }
            AddressFetchResult.Success(addresses)
        } catch (e: Exception) {
            AddressFetchResult.Failure("Couldn't fetch Indiana address points: ${e.message}")
        }
    }

    private val outFields = listOf(
        "addnum_pre", "add_number", "addnum_suf", "st_premod", "st_predir", "st_pretyp",
        "st_name", "st_postyp", "st_posdir", "st_posmod", "latitude", "longitude"
    ).joinToString(",")

    private fun queryPage(zip: String, offset: Int): List<FetchedAddress> {
        val params = listOf(
            "where" to "geozip='$zip'",
            "outFields" to outFields,
            "f" to "json",
            "resultOffset" to offset.toString(),
            "resultRecordCount" to PAGE_SIZE.toString()
        )
        val query = params.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
        val conn = URL("$SERVICE_URL?$query").openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) error("HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.has("error")) {
                error(json.getJSONObject("error").optString("message", "unknown error"))
            }
            val features = json.optJSONArray("features") ?: JSONArray()
            return (0 until features.length()).mapNotNull { i ->
                val attrs = features.getJSONObject(i).getJSONObject("attributes")
                val label = buildLabel(attrs) ?: return@mapNotNull null
                val lat = attrs.optDouble("latitude", Double.NaN)
                val lon = attrs.optDouble("longitude", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
                FetchedAddress(label, GeoPoint(lat, lon))
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * `add_full` is null for most/all Bartholomew County records in this dataset, so the label is
     * built from the individual number/street-name components instead. `optString` on a field whose
     * value is JSON null returns the literal string "null" rather than blank (an org.json quirk) -
     * [cleanString] guards against that so a missing directional/suffix doesn't leak "null" into it.
     */
    internal fun buildLabel(attrs: JSONObject): String? {
        val houseNumber = attrs.optInt("add_number", -1).takeIf { it > 0 } ?: return null
        val parts = listOfNotNull(
            cleanString(attrs, "addnum_pre"),
            houseNumber.toString() + (cleanString(attrs, "addnum_suf") ?: ""),
            cleanString(attrs, "st_premod"),
            cleanString(attrs, "st_predir"),
            cleanString(attrs, "st_pretyp"),
            cleanString(attrs, "st_name"),
            cleanString(attrs, "st_postyp"),
            cleanString(attrs, "st_posdir"),
            cleanString(attrs, "st_posmod")
        )
        return parts.joinToString(" ").takeIf { it.isNotBlank() }
    }

    private fun cleanString(attrs: JSONObject, key: String): String? {
        if (attrs.isNull(key)) return null
        return attrs.optString(key).trim().takeIf { it.isNotBlank() }
    }
}
