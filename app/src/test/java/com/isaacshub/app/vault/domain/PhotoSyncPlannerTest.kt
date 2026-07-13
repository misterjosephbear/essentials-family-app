package com.isaacshub.app.vault.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class PhotoSyncPlannerTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun `only photos added after the last sync are pending`() {
        val photos = listOf(
            MediaPhoto(1, "old.jpg", dateAddedEpochSeconds = 1_000),
            MediaPhoto(2, "new.jpg", dateAddedEpochSeconds = 5_000)
        )
        val pending = planPendingUploads(photos, lastSyncEpochMillis = 3_000_000, zone = zone)
        assertEquals(listOf(2L), pending.map { it.photo.id })
    }

    @Test
    fun `pending uploads are sorted oldest first`() {
        val photos = listOf(
            MediaPhoto(1, "b.jpg", dateAddedEpochSeconds = 5_000),
            MediaPhoto(2, "a.jpg", dateAddedEpochSeconds = 4_000)
        )
        val pending = planPendingUploads(photos, lastSyncEpochMillis = 0, zone = zone)
        assertEquals(listOf(2L, 1L), pending.map { it.photo.id })
    }

    @Test
    fun `remote path is organized by year and month`() {
        // 1_700_000_000 seconds = 2023-11-14 in UTC
        val photos = listOf(MediaPhoto(1, "beach.jpg", dateAddedEpochSeconds = 1_700_000_000))
        val pending = planPendingUploads(photos, lastSyncEpochMillis = 0, zone = zone)
        assertEquals("Photos/2023/11/beach.jpg", pending.single().remotePath)
    }

    @Test
    fun `no photos are pending when nothing is newer than the last sync`() {
        val photos = listOf(MediaPhoto(1, "old.jpg", dateAddedEpochSeconds = 1_000))
        val pending = planPendingUploads(photos, lastSyncEpochMillis = 5_000_000, zone = zone)
        assertEquals(emptyList<PendingUpload>(), pending)
    }
}
