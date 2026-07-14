package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StickyNearestSlotsTest {

    private val origin = GeoPoint(40.0, -80.0)
    private fun candidate(id: Long) = CandidateAddress(id, "Address $id", origin)

    @Test
    fun `empty slots fill with newcomers in nearest-first order`() {
        val previousSlots = List<Long?>(5) { null }
        val nearest = listOf(candidate(1), candidate(2), candidate(3))

        val result = stickyNearestSlots(previousSlots, nearest)

        assertEquals(listOf<Long?>(1, 2, 3, null, null), result)
    }

    @Test
    fun `a candidate keeps its slot even when its rank within the nearest set changes`() {
        val previousSlots = listOf<Long?>(1, 2, 3, null, null)
        // 3 is now closer than 1 and 2, but all three are still in the nearest set.
        val nearest = listOf(candidate(3), candidate(1), candidate(2))

        val result = stickyNearestSlots(previousSlots, nearest)

        assertEquals(listOf<Long?>(1, 2, 3, null, null), result)
    }

    @Test
    fun `a candidate that falls out of the nearest set is evicted and replaced by a newcomer`() {
        val previousSlots = listOf<Long?>(1, 2, 3, null, null)
        // 2 got overtaken by 5 closer addresses and is no longer in the nearest set; 4 is new.
        val nearest = listOf(candidate(1), candidate(3), candidate(4))

        val result = stickyNearestSlots(previousSlots, nearest)

        // 4 fills slot 1 - the earliest vacated slot - not slot 3, which is still empty.
        assertEquals(listOf<Long?>(1, 4, 3, null, null), result)
    }

    @Test
    fun `a routed candidate (missing entirely) is evicted the same way`() {
        val previousSlots = listOf<Long?>(1, 2, 3, null, null)
        // 2 was routed and no longer appears among unrouted candidates at all.
        val nearest = listOf(candidate(1), candidate(3))

        val result = stickyNearestSlots(previousSlots, nearest)

        assertEquals(listOf<Long?>(1, null, 3, null, null), result)
    }

    @Test
    fun `newcomers fill the earliest vacated slots first`() {
        val previousSlots = listOf<Long?>(null, 1, null, 2, null)
        val nearest = listOf(candidate(3), candidate(1), candidate(4), candidate(2))

        val result = stickyNearestSlots(previousSlots, nearest)

        assertEquals(listOf<Long?>(3, 1, 4, 2, null), result)
    }
}
