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

class FileManager @Inject constructor(
    private val context: Context,
    private val sanitizer: DataSanitizer,
    private val encryption: MediaEncryption,
    private val logger: AppLogger
) {
    private val baseFolder = "MessaggiEffimeri"

    fun saveBitmap(bitmap: Bitmap, appName: String): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${appName}_${timestamp}.jpg"

        logger.i("saveBitmap called: fileName=$fileName, bitmap=${bitmap.width}x${bitmap.height}", "FileManager")

        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveBitmapMediaStore(bitmap, fileName)
            } else {
                saveBitmapLegacy(bitmap, fileName)
            }

            if (uri != null) {
                logger.i("Saved successfully: $fileName -> $uri", "FileManager")
            } else {
                logger.e("Save returned null URI for: $fileName", null, "FileManager")
            }
            uri
        } catch (e: Exception) {
            logger.e("Exception saving $fileName: ${e.message}", e, "FileManager")
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun saveBitmapMediaStore(bitmap: Bitmap, fileName: String): Uri? {
        // Note: IS_PENDING omitted — Samsung devices have known issues with it
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$baseFolder")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            logger.e("contentResolver.insert returned null for $fileName", null, "FileManager")
            return null
        }

        logger.d("MediaStore entry created: $uri", "FileManager")

        context.contentResolver.openOutputStream(uri).use { outputStream ->
            if (outputStream != null) {
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                outputStream.flush()
                logger.d("Compressed=$compressed for $fileName", "FileManager")
            } else {
                logger.e("openOutputStream returned null for $uri", null, "FileManager")
                context.contentResolver.delete(uri, null, null)
                return null
            }
        }

        return uri
    }

    private fun saveBitmapLegacy(bitmap: Bitmap, fileName: String): Uri? {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val folder = File(imagesDir, baseFolder)
        if (!folder.exists()) {
            val created = folder.mkdirs()
            if (!created) {
                logger.e("Failed to create directory: ${folder.absolutePath}", null, "FileManager")
                return null
            }
        }
        val imageFile = File(folder, fileName)
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
        }
        logger.d("Legacy saved to: ${imageFile.absolutePath}", "FileManager")
        return Uri.fromFile(imageFile)
    }

    fun getSavedFilesCount(): Int {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            baseFolder
        )
        return folder.listFiles()?.size ?: 0
    }
}
