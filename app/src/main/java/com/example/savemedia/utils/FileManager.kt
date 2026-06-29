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

class FileManager(private val context: Context) {
    private val baseFolder = "MessaggiEffimeri"

    fun saveBitmap(bitmap: Bitmap, appName: String): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${appName}_${timestamp}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$baseFolder")
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it).use { outputStream ->
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }
            }
            uri
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = File(imagesDir, baseFolder)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val imageFile = File(folder, fileName)
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            Uri.fromFile(imageFile)
        }
    }

    fun getSavedFilesCount(): Int {
        val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), baseFolder)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            File(imagesDir, baseFolder)
        }
        return folder.listFiles()?.size ?: 0
    }
}
