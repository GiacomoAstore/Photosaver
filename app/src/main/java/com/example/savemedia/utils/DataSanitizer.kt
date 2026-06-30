package com.example.savemedia.utils

import com.example.savemedia.domain.MediaEntity
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSanitizer @Inject constructor(
    private val logger: AppLogger
) {
    fun sanitizeMedia(media: MediaEntity): MediaEntity {
        // In a real implementation, we would use ExifInterface to remove metadata from the file.
        // For this task, we randomize the filename as requested.
        val newFileName = UUID.randomUUID().toString() + ".jpg"
        logger.d("Sanitizing media metadata for ${media.fileName}", "DataSanitizer")
        return media.copy(fileName = newFileName)
    }
}
