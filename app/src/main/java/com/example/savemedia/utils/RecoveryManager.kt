package com.example.savemedia.utils

import android.content.Context
import android.content.Intent
import com.example.savemedia.services.MediaSaveService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryManager @Inject constructor(
    private val logger: AppLogger,
    private val healthChecker: HealthChecker,
    private val context: Context
) {
    private var failureCount = 0
    private val MAX_FAILURES = 3

    fun monitorAndRecover() {
        val health = healthChecker.checkAll()

        if (!health.isHealthy) {
            failureCount++
            logger.w("Health check failed #$failureCount", "Recovery",
                mapOf("issues" to health.issues.toString()))

            when {
                failureCount == 1 -> restartServices()
                failureCount == 2 -> clearCacheAndRestart()
                failureCount >= MAX_FAILURES -> fullResetAndAlert()
            }
        } else {
            failureCount = 0
        }
    }

    private fun restartServices() {
        logger.i("Attempting to restart services", "Recovery")
        context.stopService(Intent(context, MediaSaveService::class.java))
        context.startForegroundService(Intent(context, MediaSaveService::class.java))
    }

    private fun clearCacheAndRestart() {
        logger.i("Clearing cache and restarting", "Recovery")
        context.cacheDir.deleteRecursively()
        restartServices()
    }

    private fun fullResetAndAlert() {
        logger.c("Critical failure. Full reset required.", null, "Recovery")
    }
}
