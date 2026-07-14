package com.isaacshub.app.routehelper.network

import com.isaacshub.app.routehelper.domain.GeoPoint

/** One row of a TIGER/Line ADDRFEAT .dbf table - a street segment's name, address ranges, and ZIPs on each side. */
data class TigerRawRecord(
    val isDeleted: Boolean,
    val fullName: String,
    val lFromHn: String,
    val lToHn: String,
    val rFromHn: String,
    val rToHn: String,
    val zipL: String,
    val zipR: String,
    val parityL: String,
    val parityR: String
)

private fun leInt32(b: ByteArray, offset: Int): Int =
    (b[offset].toInt() and 0xFF) or
        ((b[offset + 1].toInt() and 0xFF) shl 8) or
        ((b[offset + 2].toInt() and 0xFF) shl 16) or
        ((b[offset + 3].toInt() and 0xFF) shl 24)

private fun leInt16(b: ByteArray, offset: Int): Int =
    (b[offset].toInt() and 0xFF) or ((b[offset + 1].toInt() and 0xFF) shl 8)

private fun beInt32(b: ByteArray, offset: Int): Int =
    ((b[offset].toInt() and 0xFF) shl 24) or
        ((b[offset + 1].toInt() and 0xFF) shl 16) or
        ((b[offset + 2].toInt() and 0xFF) shl 8) or
        (b[offset + 3].toInt() and 0xFF)

private fun leDouble(b: ByteArray, offset: Int): Double {
    var bits = 0L
    for (i in 0 until 8) {
        bits = bits or ((b[offset + i].toLong() and 0xFF) shl (8 * i))
    }
    return Double.fromBits(bits)
}

/**
 * Parses the .dbf attribute table of a TIGER/Line ADDRFEAT shapefile - a simple fixed-width binary
 * format (32-byte header + field descriptors, then one fixed-width record per row). Field layout is
 * fixed by the Census Bureau's ADDRFEAT schema, not user data, so it's hardcoded rather than parsed
 * from the field descriptors.
 */
object TigerDbfParser {
    private val FIELDS = listOf(
        "TLID" to 10, "TFIDL" to 10, "TFIDR" to 10, "ARIDL" to 22, "ARIDR" to 22, "LINEARID" to 22,
        "FULLNAME" to 100, "LFROMHN" to 12, "LTOHN" to 12, "RFROMHN" to 12, "RTOHN" to 12,
        "ZIPL" to 5, "ZIPR" to 5, "EDGE_MTFCC" to 5, "ROAD_MTFCC" to 5, "PARITYL" to 1, "PARITYR" to 1,
        "PLUS4L" to 4, "PLUS4R" to 4, "LFROMTYP" to 1, "LTOTYP" to 1, "RFROMTYP" to 1, "RTOTYP" to 1,
        "OFFSETL" to 1, "OFFSETR" to 1
    )

    /** One entry per row in file order (including deleted rows) so callers can zip this 1:1 against [TigerShpParser] output. */
    fun parse(bytes: ByteArray): List<TigerRawRecord> {
        val numRecords = leInt32(bytes, 4)
        val headerLen = leInt16(bytes, 8)
        val recordLen = leInt16(bytes, 10)

        val records = mutableListOf<TigerRawRecord>()
        var offset = headerLen
        repeat(numRecords) {
            if (offset + recordLen > bytes.size) return@repeat
            val isDeleted = bytes[offset] == '*'.code.toByte()
            var fieldOffset = offset + 1
            val values = HashMap<String, String>(FIELDS.size)
            for ((name, length) in FIELDS) {
                values[name] = String(bytes, fieldOffset, length, Charsets.US_ASCII).trim()
                fieldOffset += length
            }
            records += TigerRawRecord(
                isDeleted = isDeleted,
                fullName = values["FULLNAME"].orEmpty(),
                lFromHn = values["LFROMHN"].orEmpty(),
                lToHn = values["LTOHN"].orEmpty(),
                rFromHn = values["RFROMHN"].orEmpty(),
                rToHn = values["RTOHN"].orEmpty(),
                zipL = values["ZIPL"].orEmpty(),
                zipR = values["ZIPR"].orEmpty(),
                parityL = values["PARITYL"].orEmpty(),
                parityR = values["PARITYR"].orEmpty()
            )
            offset += recordLen
        }
        return records
    }
}

/**
 * Parses the .shp geometry file of a TIGER/Line ADDRFEAT shapefile - every ADDRFEAT record is a
 * PolyLine (shape type 3). Multi-part lines are flattened into a single ordered point list, since
 * address ranges in this dataset are always carried by single-part segments.
 */
object TigerShpParser {
    private const val FILE_HEADER_SIZE = 100
    private const val RECORD_HEADER_SIZE = 8

    /** One entry per record in file order, matching [TigerDbfParser]'s row order. */
    fun parseLines(bytes: ByteArray): List<List<GeoPoint>> {
        val lines = mutableListOf<List<GeoPoint>>()
        var offset = FILE_HEADER_SIZE
        while (offset + RECORD_HEADER_SIZE <= bytes.size) {
            val contentLenBytes = beInt32(bytes, offset + 4) * 2
            val contentStart = offset + RECORD_HEADER_SIZE
            val shapeType = leInt32(bytes, contentStart)
            if (shapeType == 0 || contentLenBytes < 44) {
                lines.add(emptyList())
            } else {
                val numParts = leInt32(bytes, contentStart + 36)
                val numPoints = leInt32(bytes, contentStart + 40)
                val pointsStart = contentStart + 44 + 4 * numParts
                lines.add(
                    (0 until numPoints).map { i ->
                        val pointOffset = pointsStart + 16 * i
                        GeoPoint(latitude = leDouble(bytes, pointOffset + 8), longitude = leDouble(bytes, pointOffset))
                    }
                )
            }
            offset = contentStart + contentLenBytes
        }
        return lines
    }
}
