package com.example.savemedia.domain.utils

import com.example.savemedia.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MLThrottler @Inject constructor(
    private val logger: AppLogger
) {
    private val captureTimes = mutableListOf<Long>()
    private val WINDOW_SIZE = 5
    private val MIN_INTERVAL_MS = 2000L

    fun shouldCapture(): Boolean {
        val now = System.currentTimeMillis()
        if (captureTimes.isEmpty()) return true

        val lastCapture = captureTimes.last()
        if (now - lastCapture < MIN_INTERVAL_MS) {
            logger.d("Throttled: too soon since last capture", "MLThrottler")
            return false
        }

        if (captureTimes.size >= WINDOW_SIZE) {
            val avgInterval = (captureTimes.last() - captureTimes.first()) / (captureTimes.size - 1)
            if (avgInterval < 5000) {
                if (now - lastCapture < 10000) {
                    logger.w("Adaptive Throttling: high frequency detected", "MLThrottler")
                    return false
                }
            }
        }

        return true
    }

    fun onCaptureStarted() {
        captureTimes.add(System.currentTimeMillis())
        if (captureTimes.size > WINDOW_SIZE) {
            captureTimes.removeAt(0)
        }
    }
}
