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

/**
 * Assigns each of the nearest addresses a "sticky" on-screen slot so the list never reshuffles
 * mid-drive: a candidate keeps [previousSlots]'s slot as long as it's still among [nearest], even if
 * its rank within that set changes. A slot only turns over - to null, then to a newcomer - when its
 * candidate is routed (and so drops out of the unrouted candidate pool entirely) or gets overtaken by
 * five closer addresses. Newcomers fill vacated slots in nearest-first order.
 */
fun stickyNearestSlots(previousSlots: List<Long?>, nearest: List<CandidateAddress>): List<Long?> {
    val nearestIds = nearest.map { it.id }.toSet()
    val retained = previousSlots.map { id -> id?.takeIf { it in nearestIds } }
    val assignedIds = retained.filterNotNull().toSet()
    val newcomers = nearest.map { it.id }.filterNot { it in assignedIds }.iterator()
    return retained.map { id -> id ?: newcomers.nextOrNull() }
}

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
