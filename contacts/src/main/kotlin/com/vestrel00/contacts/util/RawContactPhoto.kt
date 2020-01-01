package com.vestrel00.contacts.util

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.RawContacts
import com.vestrel00.contacts.ContactsPermissions
import com.vestrel00.contacts.Fields
import com.vestrel00.contacts.entities.INVALID_ID
import com.vestrel00.contacts.entities.MimeType
import com.vestrel00.contacts.entities.MutableRawContact
import com.vestrel00.contacts.entities.RawContact
import com.vestrel00.contacts.entities.cursor.PhotoCursor
import com.vestrel00.contacts.entities.table.Table
import com.vestrel00.contacts.equalTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun RawContact.setPhoto(photoBytes: ByteArray, context: Context): Boolean =
    setRawContactPhoto(id, photoBytes, context)

fun RawContact.setPhoto(photoInputStream: InputStream, context: Context): Boolean =
    setPhoto(photoInputStream.readBytes(), context)

fun RawContact.setPhoto(photoBitmap: Bitmap, context: Context): Boolean =
    setPhoto(photoBitmap.bytes(), context)

fun RawContact.setPhoto(photoDrawable: BitmapDrawable, context: Context): Boolean =
    setPhoto(photoDrawable.bitmap.bytes(), context)

fun MutableRawContact.setPhoto(photoBytes: ByteArray, context: Context): Boolean =
    setRawContactPhoto(id, photoBytes, context)

fun MutableRawContact.setPhoto(photoInputStream: InputStream, context: Context): Boolean =
    setPhoto(photoInputStream.readBytes(), context)

fun MutableRawContact.setPhoto(photoBitmap: Bitmap, context: Context): Boolean =
    setPhoto(photoBitmap.bytes(), context)

fun MutableRawContact.setPhoto(photoDrawable: BitmapDrawable, context: Context): Boolean =
    setPhoto(photoDrawable.bitmap.bytes(), context)

fun RawContact.removePhoto(context: Context): Boolean = removeRawContactPhoto(id, context)

fun MutableRawContact.removePhoto(context: Context): Boolean = removeRawContactPhoto(id, context)

fun RawContact.photoInputStream(context: Context): InputStream? =
    photoInputStream(id, context)

fun RawContact.photoBytes(context: Context): ByteArray? = photoInputStream(context)?.apply {
    it.readBytes()
}
fun RawContact.photoBitmap(context: Context): Bitmap? = photoInputStream(context)?.apply {
    BitmapFactory.decodeStream(it)
}

fun RawContact.photoBitmapDrawable(context: Context): BitmapDrawable? =
    photoInputStream(context)?.apply {
        BitmapDrawable(context.resources, it)
    }

fun MutableRawContact.photoInputStream(context: Context): InputStream? =
    photoInputStream(id, context)

fun MutableRawContact.photoBytes(context: Context): ByteArray? =
    photoInputStream(context)?.apply {
        it.readBytes()
    }

fun MutableRawContact.photoBitmap(context: Context): Bitmap? = photoInputStream(context)?.apply {
    BitmapFactory.decodeStream(it)
}

fun MutableRawContact.photoBitmapDrawable(context: Context): BitmapDrawable? =
    photoInputStream(context)?.apply {
        BitmapDrawable(context.resources, it)
    }

fun RawContact.photoThumbnailInputStream(context: Context): InputStream? =
    photoThumbnailInputStream(id, context)

fun RawContact.photoThumbnailBytes(context: Context): ByteArray? =
    photoThumbnailInputStream(context)?.apply {
        it.readBytes()
    }

fun RawContact.photoThumbnailBitmap(context: Context): Bitmap? =
    photoThumbnailInputStream(context)?.apply {
        BitmapFactory.decodeStream(it)
    }

fun RawContact.photoThumbnailBitmapDrawable(context: Context): BitmapDrawable? =
    photoThumbnailInputStream(context)?.apply {
        BitmapDrawable(context.resources, it)
    }

fun MutableRawContact.photoThumbnailInputStream(context: Context): InputStream? =
    photoThumbnailInputStream(id, context)

fun MutableRawContact.photoThumbnailBytes(context: Context): ByteArray? =
    photoThumbnailInputStream(context)?.apply {
        it.readBytes()
    }

fun MutableRawContact.photoThumbnailBitmap(context: Context): Bitmap? =
    photoThumbnailInputStream(context)?.apply {
        BitmapFactory.decodeStream(it)
    }

fun MutableRawContact.photoThumbnailBitmapDrawable(context: Context): BitmapDrawable? =
    photoThumbnailInputStream(context)?.apply {
        BitmapDrawable(context.resources, it)
    }

internal fun setRawContactPhoto(
    rawContactId: Long,
    photoBytes: ByteArray,
    context: Context
): Boolean {
    if (!ContactsPermissions(context).canInsertUpdateDelete() || rawContactId == INVALID_ID) {
        return false
    }

    var isSuccessful = false
    try {
        val photoUri = Uri.withAppendedPath(
            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
            RawContacts.DisplayPhoto.CONTENT_DIRECTORY
        )

        val fd = context.contentResolver
            .openAssetFileDescriptor(photoUri, "rw")!!
        val os = fd.createOutputStream()

        os.write(photoBytes)

        os.close()
        fd.close()

        isSuccessful = true
    } finally {
        return isSuccessful
    }
}

// Removing the photo data row does the trick!
private fun removeRawContactPhoto(rawContactId: Long, context: Context): Boolean {
    if (!ContactsPermissions(context).canInsertUpdateDelete() || rawContactId == INVALID_ID) {
        return false
    }

    val deleteRawContactPhotoOperation = ContentProviderOperation.newDelete(Table.DATA.uri)
        .withSelection(
            "${(Fields.RawContactId equalTo rawContactId)
                    and (Fields.MimeType equalTo MimeType.PHOTO)}",
            null
        )
        .build()

    // Delete returns the number of rows deleted, which doesn't indicate if the delete operation
    // succeeded or not because there may have not been a row to delete. Therefore, we use
    // applyBatch instead, which should indicate success or failure via exception throwing.
    try {
        context.contentResolver.applyBatch(
            ContactsContract.AUTHORITY,
            arrayListOf(deleteRawContactPhotoOperation)
        )
    } catch (exception: Exception) {
        return false
    }

    return true
}

private fun photoInputStream(rawContactId: Long, context: Context): InputStream? {
    if (!ContactsPermissions(context).canQuery() || rawContactId == INVALID_ID) {
        return null
    }

    val cursor = context.contentResolver.query(
        Table.DATA.uri,
        arrayOf(Fields.Photo.PhotoFileId.columnName),
        "${(Fields.RawContactId equalTo rawContactId)
                and (Fields.MimeType equalTo MimeType.PHOTO)}",
        null,
        null
    )

    val photoFileId = if (cursor != null && cursor.moveToNext()) {
        PhotoCursor(cursor).photoFileId
    } else {
        null
    }

    cursor?.close()

    if (photoFileId != null) {
        val photoUri =
            ContentUris.withAppendedId(ContactsContract.DisplayPhoto.CONTENT_URI, photoFileId)

        var inputStream: InputStream? = null
        try {
            val fd = context.contentResolver.openAssetFileDescriptor(photoUri, "r")
            inputStream = fd?.createInputStream()
        } finally {
            return inputStream
        }
    }

    return null
}

private fun photoThumbnailInputStream(rawContactId: Long, context: Context): InputStream? {
    if (!ContactsPermissions(context).canQuery() || rawContactId == INVALID_ID) {
        return null
    }

    val cursor = context.contentResolver.query(
        Table.DATA.uri,
        arrayOf(Fields.Photo.PhotoThumbnail.columnName),
        "${(Fields.RawContactId equalTo rawContactId)
                and (Fields.MimeType equalTo MimeType.PHOTO)}",
        null,
        null
    )

    var photoThumbnail: ByteArray? = null
    if (cursor != null && cursor.moveToNext()) {
        photoThumbnail = PhotoCursor(cursor).photoThumbnail

        cursor.close()
    }

    return if (photoThumbnail != null) ByteArrayInputStream(photoThumbnail) else null
}

internal inline fun <T> InputStream.apply(block: (InputStream) -> T): T {
    val t = block(this)
    close()
    return t
}

internal fun Bitmap.bytes(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return outputStream.toByteArray()
}