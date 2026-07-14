package com.isaacshub.app.routehelper.network

import android.content.Context

data class CountyFips(val stateFp: String, val countyFp: String)

/**
 * Looks up Census state/county FIPS codes from the bundled `tiger_county_fips.csv` asset
 * (STATE|STATEFP|COUNTYFP|COUNTYNAME, trimmed from the Census Bureau's national county gazetteer),
 * needed to build the TIGER/Line ADDRFEAT download URL for a county.
 */
class CountyFipsLookup(private val context: Context) {

    private val byKey: Map<String, CountyFips> by lazy {
        context.assets.open("tiger_county_fips.csv").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 4) return@mapNotNull null
                val (state, stateFp, countyFp, countyName) = parts
                key(state, countyName) to CountyFips(stateFp, countyFp)
            }.toMap()
        }
    }

    fun lookup(stateAbbreviation: String, countyName: String): CountyFips? =
        byKey[key(stateAbbreviation, countyName)]

    private fun key(state: String, county: String) = "${state.trim().uppercase()}|${county.trim().lowercase()}"
}
