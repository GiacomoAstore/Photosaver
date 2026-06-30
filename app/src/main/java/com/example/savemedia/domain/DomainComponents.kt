package com.example.savemedia.domain

enum class MediaType { IMAGE, VIDEO }

data class MediaEntity(
    val id: String,
    val fileName: String,
    val filePath: String,
    val sourceApp: String,
    val timestamp: Long,
    val size: Long,
    val mediaType: MediaType
)

sealed class MediaSaverException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class StorageFullException(val freeSpace: Long, val requiredSpace: Long) :
        MediaSaverException("Storage full. Required: ${requiredSpace}MB, Free: ${freeSpace}MB")
    class CaptureFailedException(val errorCode: Int, message: String = "Capture failed") :
        MediaSaverException(message)
    class PermissionDeniedException(val permission: String) :
        MediaSaverException("Permission denied: $permission")
    class FileCorruptedException(val filePath: String, cause: Throwable) :
        MediaSaverException("File corrupted: $filePath", cause)
}

interface MediaRepository {
    suspend fun saveMedia(bitmap: android.graphics.Bitmap, appName: String): MediaEntity
    fun observeSavedMedia(): kotlinx.coroutines.flow.Flow<List<MediaEntity>>
    suspend fun getAllSavedMedia(): List<MediaEntity>
}
