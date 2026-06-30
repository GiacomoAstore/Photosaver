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
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.FileManager
import com.example.savemedia.utils.OptimizedScreenCapture
import com.example.savemedia.utils.SmartThrottler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureService : Service() {

    @Inject lateinit var fileManager: FileManager
    @Inject lateinit var logger: AppLogger
    @Inject lateinit var optimizedCapture: OptimizedScreenCapture
    @Inject lateinit var throttler: SmartThrottler

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false

    companion object {
        const val ACTION_START_CAPTURE = "com.example.savemedia.START_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_APP_NAME = "extra_app_name"
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }

    private val binder = LocalBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_CAPTURE) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "UnknownApp"
            if (resultCode != 0 && data != null) {
                startCapture(resultCode, data, appName)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startCapture(resultCode: Int, data: Intent, appName: String = "Manual") {
        if (isCapturing) return
        if (!throttler.shouldCapture()) return

        isCapturing = true
        startForegroundService()

        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            if (isCapturing) {
                val bitmap = optimizedCapture.captureFrame(reader, metrics.widthPixels, metrics.heightPixels)
                if (bitmap != null) {
                    fileManager.saveBitmap(bitmap, appName)
                    logger.i("Optimized frame captured and saved", "ScreenCapture", mapOf("app" to appName))
                    stopCapture()
                }
            }
        }, null)
    }

    fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
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
