package com.example.savemedia.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.CaptureBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccessibilityMonitorService : AccessibilityService() {

    @Inject lateinit var logger: AppLogger
    @Inject lateinit var captureBridge: CaptureBridge

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val packageName = event.packageName?.toString() ?: return
            if (isMessagingApp(packageName)) {
                val rootNode = rootInActiveWindow ?: return
                if (detectViewOnce(rootNode, packageName)) {
                    logger.i("Detected View Once content in $packageName", "AccessibilityMonitor")
                    triggerCapture(packageName)
                }
            }
        }
    }

    private fun isMessagingApp(packageName: String): Boolean {
        val apps = listOf("com.whatsapp", "org.telegram.messenger", "com.instagram.android")
        return apps.contains(packageName)
    }

    private fun detectViewOnce(node: AccessibilityNodeInfo, packageName: String): Boolean {
        val markers = when (packageName) {
            "com.whatsapp" -> listOf("Photo", "Video", "Foto", "Video", "Immagine")
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

    private fun triggerCapture(packageName: String) {
        if (captureBridge.hasPermission()) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START_CAPTURE
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, captureBridge.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, captureBridge.captureIntent)
                putExtra(ScreenCaptureService.EXTRA_APP_NAME, packageName)
            }
            startService(intent)
        }
    }

    override fun onInterrupt() {}
}
