package com.isaacshub.app.vault.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val displayName: String,
    val dateAddedEpochSeconds: Long,
    val mediaType: MediaType,
    val mimeType: String = if (mediaType == MediaType.VIDEO) "video/mp4" else "image/jpeg"
)

data class PendingUpload(
    val photo: MediaItem,
    val remotePath: String
)

private val PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM")

private fun remoteFolderFor(mediaType: MediaType) = when (mediaType) {
    MediaType.IMAGE -> "Photos"
    MediaType.VIDEO -> "Videos"
}

/**
 * Which of [items] (photos and videos) haven't been synced yet (added after [lastSyncEpochMillis]),
 * each paired with where it lands in the vault - organized by media type then year/month of when it
 * was added, so a large library doesn't dump everything into one flat directory.
 */
fun planPendingUploads(
    items: List<MediaItem>,
    lastSyncEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): List<PendingUpload> = items
    .filter { it.dateAddedEpochSeconds * 1000 > lastSyncEpochMillis }
    .sortedBy { it.dateAddedEpochSeconds }
    .map { item ->
        val date = Instant.ofEpochSecond(item.dateAddedEpochSeconds).atZone(zone)
        val folder = remoteFolderFor(item.mediaType)
        PendingUpload(item, "$folder/${date.format(PATH_FORMATTER)}/${item.displayName}")
    }
