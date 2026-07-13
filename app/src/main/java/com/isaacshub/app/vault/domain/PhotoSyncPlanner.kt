package com.isaacshub.app.vault.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MediaPhoto(
    val id: Long,
    val displayName: String,
    val dateAddedEpochSeconds: Long,
    val mimeType: String = "image/jpeg"
)

data class PendingUpload(
    val photo: MediaPhoto,
    val remotePath: String
)

private val PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM")

/**
 * Which of [photos] haven't been synced yet (added after [lastSyncEpochMillis]), each paired with
 * where it lands in the vault - organized by year/month of when it was added, so a large photo
 * library doesn't dump everything into one flat directory.
 */
fun planPendingUploads(
    photos: List<MediaPhoto>,
    lastSyncEpochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): List<PendingUpload> = photos
    .filter { it.dateAddedEpochSeconds * 1000 > lastSyncEpochMillis }
    .sortedBy { it.dateAddedEpochSeconds }
    .map { photo ->
        val date = Instant.ofEpochSecond(photo.dateAddedEpochSeconds).atZone(zone)
        PendingUpload(photo, "Photos/${date.format(PATH_FORMATTER)}/${photo.displayName}")
    }
