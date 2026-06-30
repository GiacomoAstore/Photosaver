package com.example.savemedia.utils

import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimizedScreenCapture @Inject constructor(
    private val logger: AppLogger
) {
    fun captureFrame(imageReader: ImageReader, width: Int, height: Int): Bitmap? {
        return try {
            val image = imageReader.acquireLatestImage() ?: run {
                logger.w("acquireLatestImage returned null", "ScreenCapture")
                return null
            }
            val bitmap = convertToBitmap(image, width, height)
            image.close()

            if (bitmap != null) {
                logger.d("Frame captured successfully: ${bitmap.width}x${bitmap.height}", "ScreenCapture")
            } else {
                logger.w("convertToBitmap returned null", "ScreenCapture")
            }
            bitmap
        } catch (e: Exception) {
            logger.e("Error capturing frame", e, "ScreenCapture")
            null
        }
    }

    private fun convertToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // Create bitmap with padding included (required by copyPixelsFromBuffer)
        val rawBitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        rawBitmap.copyPixelsFromBuffer(buffer)

        // Crop to actual screen dimensions, removing row padding artifacts
        return if (rowPadding > 0) {
            val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
            rawBitmap.recycle()
            cropped
        } else {
            rawBitmap
        }
    }
}
