package com.isaacshub.app.vault.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.isaacshub.app.vault.domain.MediaPhoto
import java.io.InputStream

/** Thin wrapper over MediaStore - the Android-specific half of photo backup, kept separate from the pure sync-planning logic in domain/ so that logic stays unit-testable without instrumentation. */
class MediaStorePhotoScanner(private val context: Context) {

    fun queryPhotos(): List<MediaPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE
        )
        val photos = mutableListOf<MediaPhoto>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                photos += MediaPhoto(
                    id = cursor.getLong(idCol),
                    displayName = cursor.getString(nameCol) ?: "photo_${cursor.getLong(idCol)}",
                    dateAddedEpochSeconds = cursor.getLong(dateCol),
                    mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                )
            }
        }
        return photos
    }

    fun openInputStream(photoId: Long): InputStream? {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId)
        return context.contentResolver.openInputStream(uri)
    }
}
