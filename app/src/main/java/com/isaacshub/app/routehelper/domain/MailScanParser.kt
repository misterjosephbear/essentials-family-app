package com.isaacshub.app.routehelper.domain

/** One address-shaped block pulled out of OCR'd mail text, plus the recipient's last name if a name line preceded it. */
data class ScannedAddress(
    val addressText: String,
    val recipientLastName: String?,
    val streetNumber: String,
    val streetName: String,
    val city: String?,
    val state: String?,
    val zip: String?
)

// Very lenient street line regex - captures house number and street name separately
private val STREET_LINE_REGEX = Regex(
    """^(\d+[A-Za-z]?)\s+(.{2,})$""",  // Group 1: House number, Group 2: Street name
    RegexOption.IGNORE_CASE
)
// Lenient city/state/ZIP - captures each component separately
private val CITY_STATE_ZIP_REGEX = Regex(
    """^([A-Za-z .'-]+?),?\s*([A-Za-z]{2})\s*(\d{5})(?:-?\s*\d{4})?$""",  // Group 1: City, Group 2: State, Group 3: ZIP
    RegexOption.IGNORE_CASE
)
// Very lenient name line - 1-5 words with letters, spaces, dots, hyphens, apostrophes
private val NAME_LINE_REGEX = Regex("""^[A-Za-z.'\- ]+$""")

/**
 * Finds candidate US mailing addresses in text OCR'd from a photographed mail piece, recognizing the
 * standard block of an optional name line, a street line (house number through street-type word), and
 * an optional city/state/zip line right after it. A mail piece can carry more than one address-shaped
 * block (return address plus delivery address, a forwarding label, etc.), so this returns all of them
 * rather than assuming the first match is correct - callers should let the user pick when there's more
 * than one.
 */
fun parseScannedAddresses(recognizedText: String): List<ScannedAddress> {
    val lines = recognizedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val results = mutableListOf<ScannedAddress>()
    for (i in lines.indices) {
        val line = lines[i]
        val streetMatch = STREET_LINE_REGEX.matchEntire(line) ?: continue

        val streetNumber = streetMatch.groupValues[1]
        val streetName = streetMatch.groupValues[2].trim()

        // Try to find city/state/ZIP on next line
        val cityStateZipLine = lines.getOrNull(i + 1)
        val cityStateZipMatch = cityStateZipLine?.let { CITY_STATE_ZIP_REGEX.matchEntire(it) }

        val city = cityStateZipMatch?.groupValues?.get(1)?.trim()
        val state = cityStateZipMatch?.groupValues?.get(2)?.trim()?.uppercase()
        val zip = cityStateZipMatch?.groupValues?.get(3)?.trim()

        val addressText = if (cityStateZipLine != null && cityStateZipMatch != null) {
            "$line, $cityStateZipLine"
        } else {
            line
        }

        // Try to find name on previous line
        val nameLine = lines.getOrNull(i - 1)?.takeIf { NAME_LINE_REGEX.matches(it) }
        val lastName = nameLine?.trim()?.split(Regex("\\s+"))?.lastOrNull()?.let(::toTitleCase)

        results.add(ScannedAddress(addressText, lastName, streetNumber, streetName, city, state, zip))
    }
    return results.distinctBy { it.addressText.uppercase() }
}

private fun toTitleCase(word: String): String = word.lowercase().replaceFirstChar { it.uppercase() }
