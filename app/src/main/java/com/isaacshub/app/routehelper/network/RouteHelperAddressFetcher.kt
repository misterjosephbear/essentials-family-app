package com.isaacshub.app.routehelper.network

import android.content.Context
import android.util.Log
import com.isaacshub.app.routehelper.domain.GeoPoint

private const val TAG = "RouteHelperAddressFetcher"

/**
 * Address source for Route Helper: tries Indiana's own real address-point data first (accurate,
 * no phantom-address problem), falling back to Census TIGER/Line house-number ranges (plus a
 * building-footprint sanity filter) for ZIPs outside Indiana or if the state service is down.
 */
class RouteHelperAddressFetcher(context: Context) {

    private val indianaFetcher = IndianaAddressPointFetcher()
    private val tigerFetcher = TigerAddressFetcher(context)

    /** Just the center point of a ZIP code, for zooming a map to it (e.g. testing mode) without a full address fetch. */
    suspend fun geocodeZip(zip: String): GeoPoint? = tigerFetcher.geocodeZip(zip)

    suspend fun fetchAddressesForZip(zip: String): AddressFetchResult {
        val fromIndiana = indianaFetcher.fetchAddressesForZip(zip)
        if (fromIndiana is AddressFetchResult.Success && fromIndiana.addresses.isNotEmpty()) {
            return fromIndiana
        }
        if (fromIndiana is AddressFetchResult.Failure) {
            Log.w(TAG, "Indiana address points unavailable for ZIP $zip: ${fromIndiana.reason}")
        }
        return tigerFetcher.fetchAddressesForZip(zip)
    }
}
