package com.example.savemedia.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.CaptureBridge
import com.example.savemedia.utils.SmartThrottler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccessibilityMonitorService : AccessibilityService() {

    @Inject lateinit var logger: AppLogger
    @Inject lateinit var captureBridge: CaptureBridge
    @Inject lateinit var throttler: SmartThrottler

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        if (isMessagingApp(packageName)) {
            val className = event.className?.toString() ?: ""
            logger.i("Event in $packageName: Class=$className Type=${AccessibilityEvent.eventTypeToString(event.eventType)}", "AccessibilityMonitor")
            
            // Log target for View Once activities
            if (className.contains("ViewOnce") || className.contains("PhotoView") || className.contains("GalleryPicker")) {
                logger.i("Detected potential View Once Activity: $className", "AccessibilityMonitor")
                triggerCapture(packageName)
                return
            }

            val rootNode = rootInActiveWindow ?: return
            if (detectViewOnce(rootNode, packageName)) {
                logger.i("!!! DETECTED VIEW ONCE BY TEXT !!! triggering capture for $packageName", "AccessibilityMonitor")
                triggerCapture(packageName)
            }
        }
    }

    internal fun isMessagingApp(packageName: String): Boolean {
        val apps = listOf("com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "com.instagram.android")
        return apps.contains(packageName)
    }

    internal fun detectViewOnce(node: AccessibilityNodeInfo, packageName: String): Boolean {
        val markers = when (packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> listOf("Photo", "Video", "Foto", "Video", "Immagine")
            "org.telegram.messenger" -> listOf("View Once", "Visualizza una volta", "Photo", "Video")
            "com.instagram.android" -> listOf("View Once", "Vedi una volta")
            else -> listOf("Photo", "Video")
        }
        return findMarker(node, markers)
    }

    private fun findMarker(n: AccessibilityNodeInfo, markers: List<String>): Boolean {
        if (n.text != null && markers.any { it.equals(n.text.toString(), ignoreCase = true) }) {
            // Further refinement: check if the node is visible and potentially full screen
            return true
        }
        for (i in 0 until n.childCount) {
            val child = n.getChild(i)
            if (child != null && findMarker(child, markers)) return true
        }
        return false
    }

    internal fun triggerCapture(packageName: String) {
        // Throttle captures to avoid rapid-fire triggers from multiple accessibility events
        if (!throttler.shouldCapture()) {
            logger.d("Capture throttled for $packageName", "AccessibilityMonitor")
            return
        }

        if (captureBridge.hasPermission()) {
            logger.i("Triggering capture for $packageName", "AccessibilityMonitor")
            throttler.onCaptureStarted()
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START_CAPTURE
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, captureBridge.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, captureBridge.captureIntent)
                putExtra(ScreenCaptureService.EXTRA_APP_NAME, packageName)
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(this, intent)
            } catch (e: Exception) {
                logger.e("Failed to start ScreenCaptureService: ${e.message}", e, "AccessibilityMonitor")
                throttler.onCaptureFail()
            }
        } else {
            logger.w("Cannot trigger capture: MediaProjection permission missing", "AccessibilityMonitor")
        }
    }

    override fun onInterrupt() {}
}
