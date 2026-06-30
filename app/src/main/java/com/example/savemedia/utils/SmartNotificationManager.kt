package com.example.savemedia.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.savemedia.domain.MediaEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartNotificationManager @Inject constructor(
    private val context: Context,
    private val logger: AppLogger
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "MediaSavedChannel"

    private var lastBatchTime = 0L
    private var batchCount = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Media Saved Notifications", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMediaSavedNotification(media: MediaEntity) {
        val now = System.currentTimeMillis()
        if (now - lastBatchTime < 5 * 60 * 1000) {
            batchCount++
            showBatchNotification(batchCount)
        } else {
            batchCount = 1
            lastBatchTime = now
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .setContentTitle("Media Saved")
                .setContentText("Captured from ${media.sourceApp}")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(media.id.hashCode(), notification)
        }
    }

    private fun showBatchNotification(count: Int) {
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("$count media saved")
            .setContentText("Check the gallery for details")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup("MEDIA_BATCH")
            .setGroupSummary(true)
            .build()

        notificationManager.notify(1001, summary)
    }
}
