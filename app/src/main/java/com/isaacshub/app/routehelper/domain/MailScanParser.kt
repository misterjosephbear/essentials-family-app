package com.isaacshub.app.routehelper.domain

/** One address-shaped block pulled out of OCR'd mail text, plus the recipient's last name if a name line preceded it. */
data class ScannedAddress(val addressText: String, val recipientLastName: String?)

// More lenient street line regex - accepts any line starting with house number
private val STREET_LINE_REGEX = Regex(
    """^\d+[A-Za-z]?\s+.{3,}""",  // At least a house number and some street name (3+ chars)
    RegexOption.IGNORE_CASE
)
// More lenient city/state/zip - handles OCR errors in spacing and punctuation
private val CITY_STATE_ZIP_REGEX = Regex("""^[A-Za-z .'-]+,?\s*[A-Z]{2}\s*\d{5}(-?\d{4})?$""")
// More lenient name line - accepts 1-4 words with common name characters
private val NAME_LINE_REGEX = Regex("""^[A-Za-z.'-]+(?:\s+[A-Za-z.'-]+){0,3}$""")

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
        if (!STREET_LINE_REGEX.matches(line)) continue
        val cityStateZip = lines.getOrNull(i + 1)?.takeIf { CITY_STATE_ZIP_REGEX.matches(it) }
        val addressText = if (cityStateZip != null) "$line, $cityStateZip" else line
        val nameLine = lines.getOrNull(i - 1)?.takeIf { NAME_LINE_REGEX.matches(it) }
        val lastName = nameLine?.trim()?.split(Regex("\\s+"))?.lastOrNull()?.let(::toTitleCase)
        results.add(ScannedAddress(addressText, lastName))
    }
    return results.distinctBy { it.addressText.uppercase() }
}

private fun toTitleCase(word: String): String = word.lowercase().replaceFirstChar { it.uppercase() }
