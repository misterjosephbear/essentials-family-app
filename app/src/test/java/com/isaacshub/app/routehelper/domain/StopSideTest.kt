package com.isaacshub.app.routehelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopSideTest {

    @Test
    fun `left produces the left-side note`() {
        assertEquals("House on the left side of the road", StopSide.LEFT.note)
    }

    @Test
    fun `right produces the right-side note`() {
        assertEquals("House on the right side of the road", StopSide.RIGHT.note)
    }

    @Test
    fun `in drive produces the mailbox note`() {
        assertEquals("Mailbox in driveway", StopSide.IN_DRIVE.note)
    }

    @Test
    fun `none produces no note`() {
        assertNull(StopSide.NONE.note)
    }
}
