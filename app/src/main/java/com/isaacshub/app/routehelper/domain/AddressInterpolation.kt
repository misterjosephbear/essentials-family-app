package com.isaacshub.app.routehelper.domain

/**
 * Only the [nearestAddresses] closest candidates are ever shown or drawn as map markers, so a side
 * with a wide house-number range costs nothing extra to represent in full - there's no "drowning
 * out other streets" effect to guard against. This cap exists purely as a sanity ceiling against
 * corrupt/pathological range data (e.g. a bad record spanning hundreds of thousands of numbers),
 * not as a normal thinning mechanism - thinning a real block's range is what caused specific real
 * addresses (like a house partway through a block) to never appear as a candidate at all.
 */
const val MAX_POINTS_PER_SIDE = 300

enum class AddressParity { ODD, EVEN, BOTH, UNKNOWN }

fun parseParity(code: String): AddressParity = when (code.trim().uppercase()) {
    "O" -> AddressParity.ODD
    "E" -> AddressParity.EVEN
    "B" -> AddressParity.BOTH
    else -> AddressParity.UNKNOWN
}

data class HouseNumberRange(val from: Int?, val to: Int?, val parity: AddressParity)

data class InterpolatedAddress(val label: String, val location: GeoPoint)

/** One TIGER/Line street segment: a name, a left/right house-number range each, and its line geometry. */
data class TigerAddressFeature(
    val streetName: String,
    val leftRange: HouseNumberRange,
    val rightRange: HouseNumberRange,
    val zipLeft: String,
    val zipRight: String,
    val vertices: List<GeoPoint>
)

/** Sample points for whichever side(s) of [feature] serve [targetZip] - a road can have a different ZIP on each side. */
fun interpolateAddresses(feature: TigerAddressFeature, targetZip: String): List<InterpolatedAddress> {
    val results = mutableListOf<InterpolatedAddress>()
    if (feature.zipLeft == targetZip) {
        results += sideAddresses(feature.streetName, feature.leftRange, feature.vertices)
    }
    if (feature.zipRight == targetZip) {
        results += sideAddresses(feature.streetName, feature.rightRange, feature.vertices)
    }
    return results
}

private fun sideAddresses(streetName: String, range: HouseNumberRange, vertices: List<GeoPoint>): List<InterpolatedAddress> {
    val from = range.from ?: return emptyList()
    val to = range.to ?: return emptyList()
    val step = if (range.parity == AddressParity.BOTH) 1 else 2
    val span = (kotlin.math.abs(to - from) / step) + 1
    val count = span.coerceIn(1, MAX_POINTS_PER_SIDE)

    val houseNumbers = sampleHouseNumbers(range, count)
    val points = pointsAlongLine(vertices, houseNumbers.size)
    return houseNumbers.zip(points).map { (houseNumber, point) ->
        InterpolatedAddress("$houseNumber $streetName", point)
    }
}

/** [count] house numbers spread evenly across [range], snapped to the nearest number matching its parity. */
fun sampleHouseNumbers(range: HouseNumberRange, count: Int): List<Int> {
    val from = range.from ?: return emptyList()
    val to = range.to ?: return emptyList()
    if (count <= 0) return emptyList()
    val low = minOf(from, to)
    val high = maxOf(from, to)

    fun snap(n: Int): Int {
        var result = when (range.parity) {
            AddressParity.ODD -> if (n % 2 == 0) n + 1 else n
            AddressParity.EVEN -> if (n % 2 != 0) n + 1 else n
            AddressParity.BOTH, AddressParity.UNKNOWN -> n
        }
        // Snapping can push a number just past the range's edge - step back the other way first,
        // rather than clamping, since clamping alone can land back on the wrong parity.
        if (result > high) result -= 2
        if (result < low) result += 2
        return result.coerceIn(low, high)
    }

    return (0 until count)
        .map { i ->
            val fraction = if (count == 1) 0.5 else i / (count - 1).toDouble()
            low + ((high - low) * fraction).toInt()
        }
        .map { snap(it).coerceIn(low, high) }
        .distinct()
}

/** [count] points spread evenly (by distance) along the polyline described by [vertices], first to last. */
fun pointsAlongLine(vertices: List<GeoPoint>, count: Int): List<GeoPoint> {
    if (vertices.isEmpty() || count <= 0) return emptyList()
    if (vertices.size == 1) return List(count) { vertices.first() }

    val segmentLengths = vertices.zipWithNext { a, b -> distanceMeters(a, b) }
    val totalLength = segmentLengths.sum()
    if (totalLength <= 0.0) return List(count) { vertices.first() }

    return (0 until count).map { i ->
        val fraction = if (count == 1) 0.5 else i / (count - 1).toDouble()
        pointAtDistance(vertices, segmentLengths, fraction * totalLength)
    }
}

private fun pointAtDistance(vertices: List<GeoPoint>, segmentLengths: List<Double>, targetDistance: Double): GeoPoint {
    var remaining = targetDistance
    for (i in segmentLengths.indices) {
        val length = segmentLengths[i]
        if (remaining <= length || i == segmentLengths.lastIndex) {
            val t = if (length > 0) (remaining / length).coerceIn(0.0, 1.0) else 0.0
            val a = vertices[i]
            val b = vertices[i + 1]
            return GeoPoint(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)
        }
        remaining -= length
    }
    return vertices.last()
}
