package com.isaacshub.app.routehelper.network

import com.isaacshub.app.routehelper.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Builds tiny synthetic .dbf/.shp byte arrays matching the real TIGER/Line ADDRFEAT layout, to verify the hand-rolled binary parsers without needing a real Census download in tests. */
class TigerShapefileParserTest {

    private val fieldOrder = listOf(
        "TLID" to 10, "TFIDL" to 10, "TFIDR" to 10, "ARIDL" to 22, "ARIDR" to 22, "LINEARID" to 22,
        "FULLNAME" to 100, "LFROMHN" to 12, "LTOHN" to 12, "RFROMHN" to 12, "RTOHN" to 12,
        "ZIPL" to 5, "ZIPR" to 5, "EDGE_MTFCC" to 5, "ROAD_MTFCC" to 5, "PARITYL" to 1, "PARITYR" to 1,
        "PLUS4L" to 4, "PLUS4R" to 4, "LFROMTYP" to 1, "LTOTYP" to 1, "RFROMTYP" to 1, "RTOTYP" to 1,
        "OFFSETL" to 1, "OFFSETR" to 1
    )

    private fun headerLen() = 32 + fieldOrder.size * 32 + 1

    private fun buildDbf(rows: List<Map<String, String>>): ByteArray {
        val recordLen = 1 + fieldOrder.sumOf { it.second }
        val out = ByteArrayOutputStream()

        val header = ByteArray(32)
        header[0] = 0x03
        writeLeInt32(header, 4, rows.size)
        writeLeInt16(header, 8, headerLen())
        writeLeInt16(header, 10, recordLen)
        out.write(header)

        for ((name, len) in fieldOrder) {
            val descriptor = ByteArray(32)
            name.toByteArray(Charsets.US_ASCII).copyInto(descriptor, 0, 0, minOf(name.length, 11))
            descriptor[11] = 'C'.code.toByte()
            descriptor[16] = len.toByte()
            out.write(descriptor)
        }
        out.write(0x0D)

        for (row in rows) {
            out.write(' '.code)
            for ((name, len) in fieldOrder) {
                out.write((row[name].orEmpty()).padEnd(len).take(len).toByteArray(Charsets.US_ASCII))
            }
        }
        return out.toByteArray()
    }

    private fun buildShp(lines: List<List<GeoPoint>>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(ByteArray(100))

        lines.forEachIndexed { index, points ->
            val contentSize = 4 + 32 + 4 + 4 + 4 + points.size * 16
            val content = ByteArray(contentSize)
            writeLeInt32(content, 0, 3)
            writeLeInt32(content, 36, 1)
            writeLeInt32(content, 40, points.size)
            writeLeInt32(content, 44, 0)
            points.forEachIndexed { i, p ->
                val offset = 48 + i * 16
                writeLeDouble(content, offset, p.longitude)
                writeLeDouble(content, offset + 8, p.latitude)
            }

            val recordHeader = ByteArray(8)
            writeBeInt32(recordHeader, 0, index + 1)
            writeBeInt32(recordHeader, 4, contentSize / 2)
            out.write(recordHeader)
            out.write(content)
        }
        return out.toByteArray()
    }

    private fun writeLeInt32(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
        b[offset + 2] = ((value shr 16) and 0xFF).toByte()
        b[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeLeInt16(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeBeInt32(b: ByteArray, offset: Int, value: Int) {
        b[offset] = ((value shr 24) and 0xFF).toByte()
        b[offset + 1] = ((value shr 16) and 0xFF).toByte()
        b[offset + 2] = ((value shr 8) and 0xFF).toByte()
        b[offset + 3] = (value and 0xFF).toByte()
    }

    private fun writeLeDouble(b: ByteArray, offset: Int, value: Double) {
        val bits = value.toRawBits()
        for (i in 0 until 8) {
            b[offset + i] = ((bits shr (8 * i)) and 0xFF).toByte()
        }
    }

    @Test
    fun `parses dbf rows in order`() {
        val dbf = buildDbf(
            listOf(
                mapOf("FULLNAME" to "Main St", "LFROMHN" to "100", "LTOHN" to "110", "ZIPL" to "47280", "PARITYL" to "E"),
                mapOf("FULLNAME" to "Oak Ave", "LFROMHN" to "1", "LTOHN" to "9", "ZIPL" to "47280", "PARITYL" to "O")
            )
        )
        val records = TigerDbfParser.parse(dbf)
        assertEquals(2, records.size)
        assertEquals("Main St", records[0].fullName)
        assertEquals("100", records[0].lFromHn)
        assertEquals("47280", records[0].zipL)
        assertFalse(records[0].isDeleted)
        assertEquals("Oak Ave", records[1].fullName)
    }

    @Test
    fun `marks deleted rows`() {
        val dbf = buildDbf(listOf(mapOf("FULLNAME" to "Deleted Ln")))
        dbf[headerLen()] = '*'.code.toByte()
        val records = TigerDbfParser.parse(dbf)
        assertTrue(records.single().isDeleted)
    }

    @Test
    fun `parses shp polylines in order`() {
        val shp = buildShp(
            listOf(
                listOf(GeoPoint(39.0, -85.0), GeoPoint(39.001, -85.001)),
                listOf(GeoPoint(40.0, -86.0), GeoPoint(40.001, -86.001), GeoPoint(40.002, -86.002))
            )
        )
        val lines = TigerShpParser.parseLines(shp)
        assertEquals(2, lines.size)
        assertEquals(2, lines[0].size)
        assertEquals(39.0, lines[0][0].latitude, 0.0001)
        assertEquals(-85.0, lines[0][0].longitude, 0.0001)
        assertEquals(3, lines[1].size)
        assertEquals(40.002, lines[1][2].latitude, 0.0001)
    }
}
