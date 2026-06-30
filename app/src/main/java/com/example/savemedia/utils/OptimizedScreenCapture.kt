package com.example.savemedia.utils

import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimizedScreenCapture @Inject constructor(
    private val logger: AppLogger,
    private val throttler: SmartThrottler
) {
    fun captureFrame(imageReader: ImageReader, width: Int, height: Int): Bitmap? {
        if (!throttler.shouldCapture()) return null

        return try {
            val image = imageReader.acquireLatestImage() ?: return null
            val bitmap = convertToBitmap(image, width, height)
            image.close()

            if (bitmap != null) {
                throttler.onCaptureSuccess()
            } else {
                throttler.onCaptureFail()
            }
            bitmap
        } catch (e: Exception) {
            logger.e("Error capturing frame", e, "ScreenCapture")
            throttler.onCaptureFail()
            null
        }
    }

    private fun convertToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return bitmap.copy(Bitmap.Config.RGB_565, false)
    }
}
