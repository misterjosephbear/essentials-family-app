package com.isaacshub.app.routehelper.util

import com.isaacshub.app.routehelper.data.CandidateAddressEntity
import com.isaacshub.app.routehelper.data.RoutedStopEntity

/**
 * Shared utilities for address matching and validation across the app.
 * Used by package scanning, Amazon route scanning, and mail scanning.
 */

/**
 * Find the best matching routed stop for an extracted address using fuzzy matching.
 * Handles street name abbreviations and variations.
 */
fun findBestRoutedStopMatch(
    extractedAddress: String,
    routeStops: List<RoutedStopEntity>
): RoutedStopEntity? {
    val normalizedExtracted = normalizeAddress(extractedAddress)

    // First try exact match
    routeStops.find { stop ->
        normalizeAddress(stop.addressLabel) == normalizedExtracted
    }?.let { return it }

    // Try matching just the street number (address numbers must match)
    val extractedNumber = extractAddressNumber(normalizedExtracted)
    if (extractedNumber == null) {
        // No number found, try substring matching
        return routeStops.find { stop ->
            val normalizedStop = normalizeAddress(stop.addressLabel)
            normalizedStop.contains(normalizedExtracted) ||
            normalizedExtracted.contains(normalizedStop)
        }
    }

    // Find stops with matching address number
    val candidatesWithMatchingNumber = routeStops.filter { stop ->
        extractAddressNumber(normalizeAddress(stop.addressLabel)) == extractedNumber
    }

    if (candidatesWithMatchingNumber.isEmpty()) {
        return null  // No stops with matching number
    }

    if (candidatesWithMatchingNumber.size == 1) {
        return candidatesWithMatchingNumber.first()  // Only one match, return it
    }

    // Multiple stops with same number - use fuzzy matching on street name
    val extractedStreet = extractStreetName(normalizedExtracted)
    return candidatesWithMatchingNumber.maxByOrNull { stop ->
        val stopStreet = extractStreetName(normalizeAddress(stop.addressLabel))
        calculateStreetNameSimilarity(extractedStreet, stopStreet)
    }
}

/**
 * Find the best matching candidate address for an extracted address using fuzzy matching.
 * Used when building routes from scanned addresses.
 */
fun findBestCandidateMatch(
    extractedAddress: String,
    candidates: List<CandidateAddressEntity>,
    similarityThreshold: Double = 0.6
): CandidateAddressEntity? {
    val normalizedExtracted = normalizeAddress(extractedAddress)

    // First try exact match
    candidates.find { candidate ->
        normalizeAddress(candidate.label) == normalizedExtracted
    }?.let { return it }

    // Try matching just the street number (address numbers must match)
    val extractedNumber = extractAddressNumber(normalizedExtracted)
    if (extractedNumber == null) {
        // No number found, try substring matching
        return candidates.find { candidate ->
            val normalizedCandidate = normalizeAddress(candidate.label)
            normalizedCandidate.contains(normalizedExtracted) ||
            normalizedExtracted.contains(normalizedCandidate)
        }
    }

    // Find candidates with matching address number
    val candidatesWithMatchingNumber = candidates.filter { candidate ->
        extractAddressNumber(normalizeAddress(candidate.label)) == extractedNumber
    }

    if (candidatesWithMatchingNumber.isEmpty()) {
        return null  // No candidates with matching number
    }

    if (candidatesWithMatchingNumber.size == 1) {
        return candidatesWithMatchingNumber.first()  // Only one match, return it
    }

    // Multiple candidates with same number - use fuzzy matching on street name
    val extractedStreet = extractStreetName(normalizedExtracted)
    return candidatesWithMatchingNumber.maxByOrNull { candidate ->
        val candidateStreet = extractStreetName(normalizeAddress(candidate.label))
        val similarity = calculateStreetNameSimilarity(extractedStreet, candidateStreet)
        // Filter by similarity threshold
        if (similarity >= similarityThreshold) similarity else 0.0
    }?.takeIf {
        // Only return if similarity meets threshold
        val candidateStreet = extractStreetName(normalizeAddress(it.label))
        calculateStreetNameSimilarity(extractedStreet, candidateStreet) >= similarityThreshold
    }
}

/**
 * Normalize an address for comparison:
 * - Lowercase
 * - Remove extra whitespace
 * - Expand common abbreviations
 */
fun normalizeAddress(address: String): String {
    return address
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")  // Collapse multiple spaces
        .replace("saint", "st")
        .replace("avenue", "ave")
        .replace("street", "st")
        .replace("drive", "dr")
        .replace("road", "rd")
        .replace("boulevard", "blvd")
        .replace("lane", "ln")
        .replace("court", "ct")
        .replace("place", "pl")
        .replace("circle", "cir")
        .replace("north", "n")
        .replace("south", "s")
        .replace("east", "e")
        .replace("west", "w")
}

/**
 * Extract the address number from a normalized address.
 * E.g., "1006 jackson st" -> 1006
 */
fun extractAddressNumber(normalizedAddress: String): Int? {
    return normalizedAddress.split(" ").firstOrNull()?.toIntOrNull()
}

/**
 * Extract the street name from a normalized address (everything after the number).
 * E.g., "1006 jackson st" -> "jackson st"
 */
fun extractStreetName(normalizedAddress: String): String {
    val parts = normalizedAddress.split(" ", limit = 2)
    return if (parts.size > 1) parts[1] else normalizedAddress
}

/**
 * Calculate similarity between two street names.
 * Returns a score from 0.0 (no match) to 1.0 (perfect match).
 */
fun calculateStreetNameSimilarity(street1: String, street2: String): Double {
    // Exact match
    if (street1 == street2) return 1.0

    // One contains the other
    if (street1.contains(street2) || street2.contains(street1)) return 0.8

    // Check if key words match (ignoring common suffixes)
    val words1 = street1.split(" ").filter { it.length > 2 }  // Filter out short words like "st"
    val words2 = street2.split(" ").filter { it.length > 2 }

    val matchingWords = words1.count { w1 ->
        words2.any { w2 -> w1 == w2 || w1.startsWith(w2) || w2.startsWith(w1) }
    }

    return if (words1.isEmpty() || words2.isEmpty()) {
        0.0
    } else {
        matchingWords.toDouble() / maxOf(words1.size, words2.size)
    }
}
