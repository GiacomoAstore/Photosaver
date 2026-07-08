package com.example.savemedia.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    private val context: Context,
    private val logger: AppLogger
) {
    private val baseFolder = "MediaSaver"

    fun saveBitmap(bitmap: Bitmap, appName: String): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${appName}_${timestamp}.jpg"

        logger.i("Saving bitmap: $fileName", "FileManager")

        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveBitmapMediaStore(bitmap, fileName)
            } else {
                saveBitmapLegacy(bitmap, fileName)
            }

            if (uri != null) {
                logger.i("Successfully saved to: $uri", "FileManager")
            }
            uri
        } catch (e: Exception) {
            logger.e("Failed to save bitmap", e, "FileManager")
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun saveBitmapMediaStore(bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$baseFolder")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        } ?: run {
            context.contentResolver.delete(uri, null, null)
            return null
        }

        return uri
    }

    private fun saveBitmapLegacy(bitmap: Bitmap, fileName: String): Uri? {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val folder = File(imagesDir, baseFolder)
        if (!folder.exists()) folder.mkdirs()
        
        val imageFile = File(folder, fileName)
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
        return Uri.fromFile(imageFile)
    }
}
