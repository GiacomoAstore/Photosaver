package com.example.savemedia.usecases

import android.graphics.Bitmap
import com.example.savemedia.domain.MediaEntity
import com.example.savemedia.domain.MediaRepository
import com.example.savemedia.utils.AppLogger
import javax.inject.Inject

class SaveMediaUseCase @Inject constructor(
    private val repository: MediaRepository,
    private val logger: AppLogger
) {
    suspend operator fun invoke(bitmap: Bitmap, appName: String): Result<MediaEntity> {
        return try {
            logger.i("Executing SaveMediaUseCase", "SaveMediaUseCase", mapOf("appName" to appName))
            val entity = repository.saveMedia(bitmap, appName)
            Result.success(entity)
        } catch (e: Exception) {
            logger.e("SaveMediaUseCase failed", e, "SaveMediaUseCase")
            Result.failure(e)
        }
    }
}

class GetSavedMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke() = repository.observeSavedMedia()
}
