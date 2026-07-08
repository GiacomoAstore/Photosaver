package com.example.savemedia.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.savemedia.domain.MediaEntity
import com.example.savemedia.domain.MediaRepository
import com.example.savemedia.domain.MediaType
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager,
    private val logger: AppLogger
) : MediaRepository {

    override suspend fun saveMedia(bitmap: Bitmap, appName: String): MediaEntity = withContext(Dispatchers.IO) {
        val uri = fileManager.saveBitmap(bitmap, appName) ?: throw Exception("Save failed")
        MediaEntity(
            id = UUID.randomUUID().toString(),
            fileName = "${appName}_${System.currentTimeMillis()}.jpg",
            filePath = uri.toString(),
            sourceApp = appName,
            timestamp = System.currentTimeMillis(),
            size = 0,
            mediaType = MediaType.IMAGE
        )
    }

    override fun observeSavedMedia(): Flow<List<MediaEntity>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                // onChange is a plain callback — must launch a coroutine to call suspend fun
                launch { trySend(getAllSavedMedia()) }
            }
        }
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)

        trySend(getAllSavedMedia())

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.onStart { emit(getAllSavedMedia()) }

    override suspend fun getAllSavedMedia(): List<MediaEntity> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaEntity>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%MediaSaver%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val date = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(MediaEntity(
                        id = id.toString(),
                        fileName = name,
                        filePath = contentUri.toString(),
                        sourceApp = name.split("_").firstOrNull() ?: "Unknown",
                        timestamp = date,
                        size = size,
                        mediaType = MediaType.IMAGE
                    ))
                }
            }
        } catch (e: Exception) {
            logger.e("Failed to query MediaStore", e, "MediaRepository")
        }
        mediaList
    }
}
