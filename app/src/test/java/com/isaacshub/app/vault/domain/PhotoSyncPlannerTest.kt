package com.isaacshub.app.vault.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class PhotoSyncPlannerTest {

    private val zone = ZoneOffset.UTC

    private fun image(id: Long, name: String, dateAddedEpochSeconds: Long) =
        MediaItem(id, name, dateAddedEpochSeconds, MediaType.IMAGE)

    private fun video(id: Long, name: String, dateAddedEpochSeconds: Long) =
        MediaItem(id, name, dateAddedEpochSeconds, MediaType.VIDEO)

    @Test
    fun `only items added after the last sync are pending`() {
        val items = listOf(
            image(1, "old.jpg", dateAddedEpochSeconds = 1_000),
            image(2, "new.jpg", dateAddedEpochSeconds = 5_000)
        )
        val pending = planPendingUploads(items, lastSyncEpochMillis = 3_000_000, zone = zone)
        assertEquals(listOf(2L), pending.map { it.photo.id })
    }

    @Test
    fun `pending uploads are sorted oldest first`() {
        val items = listOf(
            image(1, "b.jpg", dateAddedEpochSeconds = 5_000),
            image(2, "a.jpg", dateAddedEpochSeconds = 4_000)
        )
        val pending = planPendingUploads(items, lastSyncEpochMillis = 0, zone = zone)
        assertEquals(listOf(2L, 1L), pending.map { it.photo.id })
    }

    @Test
    fun `remote path for a photo is organized under Photos by year and month`() {
        // 1_700_000_000 seconds = 2023-11-14 in UTC
        val items = listOf(image(1, "beach.jpg", dateAddedEpochSeconds = 1_700_000_000))
        val pending = planPendingUploads(items, lastSyncEpochMillis = 0, zone = zone)
        assertEquals("Photos/2023/11/beach.jpg", pending.single().remotePath)
    }

    @Test
    fun `remote path for a video is organized under Videos by year and month`() {
        val items = listOf(video(1, "clip.mp4", dateAddedEpochSeconds = 1_700_000_000))
        val pending = planPendingUploads(items, lastSyncEpochMillis = 0, zone = zone)
        assertEquals("Videos/2023/11/clip.mp4", pending.single().remotePath)
    }

    @Test
    fun `photos and videos are planned together, sorted by date`() {
        val items = listOf(
            video(1, "clip.mp4", dateAddedEpochSeconds = 2_000),
            image(2, "pic.jpg", dateAddedEpochSeconds = 1_000)
        )
        val pending = planPendingUploads(items, lastSyncEpochMillis = 0, zone = zone)
        assertEquals(listOf(2L, 1L), pending.map { it.photo.id })
    }

    @Test
    fun `no items are pending when nothing is newer than the last sync`() {
        val items = listOf(image(1, "old.jpg", dateAddedEpochSeconds = 1_000))
        val pending = planPendingUploads(items, lastSyncEpochMillis = 5_000_000, zone = zone)
        assertEquals(emptyList<PendingUpload>(), pending)
    }
}
