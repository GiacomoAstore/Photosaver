package com.example.savemedia.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartThrottler @Inject constructor(
    private val logger: AppLogger
) {
    private var lastCaptureTime = 0L
    private var consecutiveFails = 0
    private var currentInterval = 1000L // ms

    fun shouldCapture(): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLast = now - lastCaptureTime

        if (consecutiveFails > 5) {
            currentInterval = minOf(currentInterval * 2, 10000L)
        }

        return timeSinceLast >= currentInterval
    }

    fun onCaptureSuccess() {
        consecutiveFails = 0
        currentInterval = maxOf(currentInterval / 2, 500L)
        lastCaptureTime = System.currentTimeMillis()
        logger.d("Throttler: interval=${currentInterval}ms", "Throttler")
    }

    fun onCaptureFail() {
        consecutiveFails++
        logger.w("Throttler: fail #$consecutiveFails", "Throttler")
    }
}
