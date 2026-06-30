package com.example.savemedia.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartThrottler @Inject constructor(
    private val logger: AppLogger
) {
    private var lastCaptureTime = 0L
    private var consecutiveFails = 0
    private var currentInterval = 2000L // ms - minimum interval between captures

    /**
     * Check if enough time has passed since the last capture to allow a new one.
     * This should be called at the trigger point (e.g. AccessibilityMonitorService),
     * NOT during frame processing.
     */
    fun shouldCapture(): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLast = now - lastCaptureTime

        if (consecutiveFails > 5) {
            currentInterval = minOf(currentInterval * 2, 10000L)
        }

        val allowed = timeSinceLast >= currentInterval
        if (!allowed) {
            logger.d("Throttler: blocked (${timeSinceLast}ms < ${currentInterval}ms)", "Throttler")
        }
        return allowed
    }

    /**
     * Call when a new capture session is about to start.
     * Records the capture time immediately to prevent duplicate triggers.
     */
    fun onCaptureStarted() {
        lastCaptureTime = System.currentTimeMillis()
        logger.d("Throttler: capture started, interval=${currentInterval}ms", "Throttler")
    }

    fun onCaptureSuccess() {
        consecutiveFails = 0
        currentInterval = maxOf(currentInterval / 2, 2000L)
        logger.d("Throttler: success, interval=${currentInterval}ms", "Throttler")
    }

    fun onCaptureFail() {
        consecutiveFails++
        logger.w("Throttler: fail #$consecutiveFails", "Throttler")
    }
}
