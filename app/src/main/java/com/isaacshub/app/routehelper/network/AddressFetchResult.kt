package com.isaacshub.app.routehelper.network

import com.isaacshub.app.routehelper.domain.GeoPoint

data class FetchedAddress(val label: String, val location: GeoPoint)

sealed interface AddressFetchResult {
    data class Success(val addresses: List<FetchedAddress>) : AddressFetchResult
    data class Failure(val reason: String) : AddressFetchResult
}
