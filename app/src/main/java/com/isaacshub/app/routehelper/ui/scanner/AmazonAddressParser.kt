package com.isaacshub.app.routehelper.ui.scanner

/**
 * Extracted address with optional sequence number from scanner screen.
 */
internal data class ExtractedAddress(
    val address: String,
    val sequenceNumber: Int?,  // Sequence number from scanner (if present)
    val quantity: Int? = null,  // Package quantity (from direction sheets)
    val routeId: String? = null  // Route ID like "L001" (from direction sheets)
)

/**
 * Extract structured stop data from Amazon direction sheet format.
 *
 * Format from direction sheets:
 * Route Stop Sequence Direction                    Distance Qty
 * L001  1    1        ARRIVE AT 1005 SHOWERS DR   0.2 MI   2
 *
 * This parser extracts:
 * - Route ID (e.g., "L001")
 * - Stop number
 * - Sequence number
 * - Address (from "ARRIVE AT" lines, ignoring turn-by-turn directions)
 * - Package quantity
 */
internal fun extractFromDirectionSheet(text: String, expectedNextSequence: Int? = null): ExtractedAddress? {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    // Look for route ID in header or first data line
    var routeId: String? = null
    val routeIdPattern = Regex("""([A-Z]\d{3})""")

    for (line in lines) {
        val lowerLine = line.lowercase()

        // Try to extract route ID if we haven't found it yet
        if (routeId == null) {
            routeIdPattern.find(line)?.let { match ->
                routeId = match.groupValues[1]
                android.util.Log.d("AmazonScanner", "Found route ID: $routeId")
            }
        }

        // Look for "ARRIVE AT" lines which contain the actual stop addresses
        if (lowerLine.contains("arrive at")) {
            // Pattern: "ARRIVE AT 1005 SHOWERS DR" or "ARRIVE AT 307 OAK MEADOWS DR, ON THE LEFT"
            val arrivePattern = Regex("""arrive\s+at\s+(\d+\s+[A-Za-z][A-Za-z0-9 .'-]+?)(?:,|\s*$)""", RegexOption.IGNORE_CASE)
            arrivePattern.find(line)?.let { match ->
                val address = match.groupValues[1].trim()

                if (isValidStreetAddress(address)) {
                    // Try to extract quantity from the same line or nearby context
                    // Quantity typically appears at the end as a single digit or in a box
                    val qtyPattern = Regex("""(\d+)\s*$""")
                    val quantity = qtyPattern.find(line)?.groupValues?.get(1)?.toIntOrNull()

                    // Try to extract sequence number from the beginning of the full text block
                    val seqPattern = Regex("""^(\d+)\s+(\d+)\s+""")
                    val sequenceMatch = seqPattern.find(text)
                    val sequenceNumber = sequenceMatch?.groupValues?.get(2)?.toIntOrNull()

                    // Validate sequence if expected
                    if (expectedNextSequence != null && sequenceNumber != null && sequenceNumber != expectedNextSequence) {
                        android.util.Log.d("AmazonScanner", "Rejecting seq #$sequenceNumber - expected #$expectedNextSequence")
                        return null
                    }

                    android.util.Log.d("AmazonScanner", "Extracted from direction sheet: seq=${sequenceNumber ?: "?"}, addr=$address, qty=${quantity ?: "?"}, route=$routeId")
                    return ExtractedAddress(
                        address = address,
                        sequenceNumber = sequenceNumber,
                        quantity = quantity,
                        routeId = routeId
                    )
                }
            }
        }
    }

    return null
}

/**
 * Extract address from OCR text, optionally with sequence number.
 *
 * Supports two scanning modes:
 * 1. Package lookahead screens: Lines like "1234 Main St" or "1. 1234 Main St"
 * 2. Direction sheets: Lines like "1. 1234 Main St" where sequence must be sequential
 *
 * Filters out direction text (e.g., "Turn left", "Continue on Main St") by:
 * - Requiring lines to START with sequence number (left-aligned)
 * - Requiring a valid street address (house number + street name)
 * - Rejecting lines with direction keywords (turn, continue, arrive, etc.)
 */
internal fun extractAddress(text: String, expectedNextSequence: Int? = null): ExtractedAddress? {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    // Direction keywords that indicate this is NOT an address line
    val directionKeywords = listOf(
        "turn", "continue", "arrive", "destination", "head", "take", "exit",
        "merge", "keep", "straight", "right", "left", "onto", "toward", "via",
        "roundabout", "ramp", "highway", "route", "mile", "foot", "feet", "ft"
    )

    // Pattern 1: Sequence number with period at start: "1. 1234 Main St"
    val seqWithPeriodPattern = Regex("""^(\d+)\.\s+(\d+\s+[A-Za-z].*)""")
    // Pattern 2: Sequence number without period at start: "42 1234 Main St"
    val seqNoPeriodPattern = Regex("""^(\d+)\s+(\d+\s+[A-Za-z].*)""")
    // Pattern 3: Address only: "1234 Main St"
    val addressOnlyPattern = Regex("""^\d+\s+[A-Za-z].*""")

    for (line in lines) {
        // Skip lines that contain direction keywords
        val lowerLine = line.lowercase()
        if (directionKeywords.any { lowerLine.contains(it) }) {
            android.util.Log.d("AmazonScanner", "Skipping direction line: $line")
            continue
        }

        // Try pattern 1 first (with period) - most common for direction sheets
        seqWithPeriodPattern.matchEntire(line)?.let { match ->
            val seqNum = match.groupValues[1].toIntOrNull()
            val address = match.groupValues[2]
            if (seqNum != null) {
                // If we're expecting a specific sequence (direction sheet mode), validate it
                if (expectedNextSequence != null && seqNum != expectedNextSequence) {
                    android.util.Log.d("AmazonScanner", "Rejecting seq #$seqNum - expected #$expectedNextSequence")
                    return@let
                }

                // Validate it's actually a street address (not directions)
                if (isValidStreetAddress(address)) {
                    android.util.Log.d("AmazonScanner", "Extracted with seq (period): #$seqNum - $address")
                    return ExtractedAddress(address, seqNum)
                }
            }
        }

        // Try pattern 2 (without period)
        seqNoPeriodPattern.matchEntire(line)?.let { match ->
            val seqNum = match.groupValues[1].toIntOrNull()
            val address = match.groupValues[2]
            // Only accept if sequence number is 2+ digits OR line starts with single digit then space then address
            if (seqNum != null && (seqNum >= 10 || line.matches(Regex("""^\d\s+\d+\s+[A-Za-z].*""")))) {
                // Validate expected sequence if provided
                if (expectedNextSequence != null && seqNum != expectedNextSequence) {
                    android.util.Log.d("AmazonScanner", "Rejecting seq #$seqNum - expected #$expectedNextSequence")
                    return@let
                }

                if (isValidStreetAddress(address)) {
                    android.util.Log.d("AmazonScanner", "Extracted with seq (no period): #$seqNum - $address")
                    return ExtractedAddress(address, seqNum)
                }
            }
        }

        // Try pattern 3 (address only) - only if NOT expecting a sequence
        if (expectedNextSequence == null && addressOnlyPattern.matches(line)) {
            if (isValidStreetAddress(line)) {
                android.util.Log.d("AmazonScanner", "Extracted address only: $line")
                return ExtractedAddress(line, null)
            }
        }
    }

    return null
}

/**
 * Validate that a string looks like a real street address, not directions.
 * Must have: house number + street name, and NOT be a direction phrase.
 */
internal fun isValidStreetAddress(address: String): Boolean {
    val lower = address.lowercase()

    // Must start with a house number
    if (!Regex("""^\d+\s+""").containsMatchIn(address)) {
        return false
    }

    // Must contain at least one street name word after the number
    val afterNumber = address.replaceFirst(Regex("""^\d+\s+"""), "")
    if (afterNumber.isBlank() || afterNumber.length < 3) {
        return false
    }

    // Reject if it looks like a distance measurement
    if (Regex("""\d+(\.\d+)?\s*(mi|mile|ft|foot|feet)""").containsMatchIn(lower)) {
        return false
    }

    return true
}
