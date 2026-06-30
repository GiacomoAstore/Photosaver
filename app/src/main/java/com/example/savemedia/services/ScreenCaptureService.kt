package com.example.savemedia.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.FileManager
import com.example.savemedia.utils.OptimizedScreenCapture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureService : Service() {

    @Inject lateinit var fileManager: FileManager
    @Inject lateinit var logger: AppLogger
    @Inject lateinit var optimizedCapture: OptimizedScreenCapture

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false
    private var captureHandlerThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var timeoutHandler: Handler? = null

    companion object {
        const val ACTION_START_CAPTURE = "com.example.savemedia.START_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_APP_NAME = "extra_app_name"
        private const val CAPTURE_TIMEOUT_MS = 3000L
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }

    private val binder = LocalBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_CAPTURE) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            }
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "UnknownApp"
            if (resultCode != 0 && data != null) {
                logger.i("Received capture request for $appName", "ScreenCapture")
                startCapture(resultCode, data, appName)
            } else {
                logger.w("Invalid capture request: resultCode=$resultCode, data=$data", "ScreenCapture")
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startCapture(resultCode: Int, data: Intent, appName: String = "Manual") {
        if (isCapturing) {
            logger.w("Already capturing, ignoring request", "ScreenCapture")
            return
        }

        isCapturing = true
        logger.i("Starting capture for $appName", "ScreenCapture")
        startForegroundService()

        // Create a dedicated handler thread for ImageReader callbacks
        captureHandlerThread = HandlerThread("ScreenCaptureThread").also { it.start() }
        captureHandler = Handler(captureHandlerThread!!.looper)
        timeoutHandler = Handler(captureHandlerThread!!.looper)

        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            mediaProjection = mpManager.getMediaProjection(resultCode, data)
        } catch (e: Exception) {
            logger.e("Failed to get MediaProjection: ${e.message}", e, "ScreenCapture")
            isCapturing = false
            return
        }

        if (mediaProjection == null) {
            logger.e("MediaProjection is null after getMediaProjection()", null, "ScreenCapture")
            isCapturing = false
            return
        }

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        logger.d("Screen: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi", "ScreenCapture")

        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            captureHandler
        )

        // Set up a timeout to avoid hanging indefinitely if no frame arrives
        timeoutHandler?.postDelayed({
            if (isCapturing) {
                logger.w("Capture timeout after ${CAPTURE_TIMEOUT_MS}ms, releasing resources", "ScreenCapture")
                stopCapture()
            }
        }, CAPTURE_TIMEOUT_MS)

        imageReader?.setOnImageAvailableListener({ reader ->
            if (isCapturing) {
                val bitmap = optimizedCapture.captureFrame(reader, screenWidth, screenHeight)
                if (bitmap != null) {
                    logger.i("Frame captured, saving for $appName", "ScreenCapture")
                    val savedUri = fileManager.saveBitmap(bitmap, appName)
                    if (savedUri != null) {
                        logger.i("Screenshot saved successfully: $savedUri", "ScreenCapture")
                    } else {
                        logger.e("saveBitmap returned null — file NOT saved for $appName", null, "ScreenCapture")
                    }
                    stopCapture()
                } else {
                    logger.d("captureFrame returned null, waiting for next frame", "ScreenCapture")
                }
            }
        }, captureHandler)
    }

    fun stopCapture() {
        isCapturing = false
        timeoutHandler?.removeCallbacksAndMessages(null)
        timeoutHandler = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        captureHandlerThread?.quitSafely()
        captureHandlerThread = null
        captureHandler = null
        logger.d("Capture stopped and resources released", "ScreenCapture")
    }

    private fun startForegroundService() {
        val channelId = "ScreenCaptureChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Screen Capture", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Capturing Screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(2, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
    }
}
