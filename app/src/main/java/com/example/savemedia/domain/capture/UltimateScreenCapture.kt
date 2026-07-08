package com.example.savemedia.domain.capture

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import android.view.WindowManager
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.CaptureBridge
import com.example.savemedia.utils.FileManager
import com.example.savemedia.utils.OptimizedScreenCapture
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UltimateScreenCapture @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger,
    private val fileManager: FileManager,
    private val captureBridge: CaptureBridge,
    private val optimizedCapture: OptimizedScreenCapture
) {
    fun captureViaAccessibility(service: AccessibilityService, appName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            logger.i("Level 1 Capture: Accessibility", "UltimateCapture")
            service.takeScreenshot(Display.DEFAULT_DISPLAY, ContextCompat.getMainExecutor(context), object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                    if (bitmap != null) {
                        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        fileManager.saveBitmap(softwareBitmap, appName)
                        bitmap.recycle()
                    }
                    result.hardwareBuffer.close()
                }
                override fun onFailure(errorCode: Int) {
                    logger.e("Level 1 failed: $errorCode. Falling back to Level 2.", null, "UltimateCapture")
                    captureViaMediaProjection(appName)
                }
            })
        } else {
            captureViaMediaProjection(appName)
        }
    }

    fun captureViaMediaProjection(appName: String) {
        if (!captureBridge.hasPermission()) {
            logger.w("Level 2 failed: No MediaProjection permission", "UltimateCapture")
            return
        }

        logger.i("Level 2 Capture: MediaProjection", "UltimateCapture")
        val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = mpManager.getMediaProjection(captureBridge.resultCode, captureBridge.captureIntent!!)
        
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        val imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        val handlerThread = HandlerThread("CaptureThread").apply { start() }
        val handler = Handler(handlerThread.looper)

        val virtualDisplay = projection.createVirtualDisplay(
            "UltimateCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, handler
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val bitmap = optimizedCapture.captureFrame(reader, metrics.widthPixels, metrics.heightPixels)
            if (bitmap != null) {
                fileManager.saveBitmap(bitmap, appName)
                
                // Cleanup
                imageReader.setOnImageAvailableListener(null, null)
                virtualDisplay.release()
                projection.stop()
                handlerThread.quitSafely()
            }
        }, handler)
    }
}
