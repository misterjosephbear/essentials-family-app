package com.isaacshub.app.routehelper.domain

data class CandidateAddress(
    val id: Long,
    val label: String,
    val location: GeoPoint
)

/** The [count] closest [candidates] to [from], nearest first - the list shown while driving a route. */
fun nearestAddresses(
    from: GeoPoint,
    candidates: List<CandidateAddress>,
    count: Int = 5
): List<CandidateAddress> = candidates
    .sortedBy { distanceMeters(from, it.location) }
    .take(count)
