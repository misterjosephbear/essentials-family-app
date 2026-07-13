package com.isaacshub.app.vault.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.isaacshub.app.vault.domain.MediaItem
import com.isaacshub.app.vault.domain.MediaType
import java.io.InputStream

/**
 * Thin wrapper over MediaStore covering both images and videos - the Android-specific half of the
 * backup, kept separate from the pure sync-planning logic in domain/ so that logic stays
 * unit-testable without instrumentation.
 */
class MediaStoreScanner(private val context: Context) {

    fun queryMedia(): List<MediaItem> =
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE) +
            queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO)

    private fun queryCollection(collection: Uri, mediaType: MediaType): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val defaultMime = if (mediaType == MediaType.VIDEO) "video/mp4" else "image/jpeg"
                items += MediaItem(
                    id = id,
                    displayName = cursor.getString(nameCol) ?: "media_$id",
                    dateAddedEpochSeconds = cursor.getLong(dateCol),
                    mediaType = mediaType,
                    mimeType = cursor.getString(mimeCol) ?: defaultMime
                )
            }
        }
        return items
    }

    /**
     * Android redacts sensitive EXIF fields (GPS location, in particular) from files read through
     * MediaStore by default, unless the caller holds ACCESS_MEDIA_LOCATION and explicitly asks for
     * the unredacted original via setRequireOriginal - otherwise every backed-up photo would
     * silently lose its location metadata.
     */
    fun openInputStream(item: MediaItem): InputStream? {
        val baseUri = when (item.mediaType) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val uri = ContentUris.withAppendedId(baseUri, item.id)
        val originalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.setRequireOriginal(uri)
        } else {
            uri
        }
        return context.contentResolver.openInputStream(originalUri)
    }
}
