package com.example.savemedia.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.savemedia.MainActivity
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.RecoveryManager
import com.example.savemedia.utils.CaptureMethodDetector
import com.example.savemedia.utils.SecureContentCapture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaSaveService : Service() {

    @Inject lateinit var logger: AppLogger
    @Inject lateinit var recoveryManager: RecoveryManager
    @Inject lateinit var detector: CaptureMethodDetector
    @Inject lateinit var secureCapture: SecureContentCapture

    private val CHANNEL_ID = "MediaSaveServiceChannel"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.i("MediaSaveService starting", "MediaSaveService")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Saver Active")
            .setContentText("Monitoring for media...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        serviceScope.launch {
            while (isActive) {
                recoveryManager.monitorAndRecover()
                delay(60000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Media Save Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
