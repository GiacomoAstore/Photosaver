package com.example.savemedia.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.savemedia.domain.capture.UltimateScreenCapture
import com.example.savemedia.domain.detector.EnhancedContentDetector
import com.example.savemedia.domain.utils.DiagnosticLogger
import com.example.savemedia.domain.utils.MLThrottler
import com.example.savemedia.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccessibilityMonitorService : AccessibilityService() {

    @Inject lateinit var logger: AppLogger
    @Inject lateinit var detector: EnhancedContentDetector
    @Inject lateinit var captureEngine: UltimateScreenCapture
    @Inject lateinit var throttler: MLThrottler
    @Inject lateinit var diagnostics: DiagnosticLogger

    private var isMarkerDetected = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        if (detector.isTargetApp(packageName)) {
            val rootNode = rootInActiveWindow
            
            // Check for marker (e.g., "Foto visualizzabile una volta") in the current view
            if (detector.hasViewOnceMarker(rootNode)) {
                if (!isMarkerDetected) {
                    logger.i("View Once Marker detected in $packageName. Waiting for viewer...", "AccessibilityMonitor")
                    isMarkerDetected = true
                }
            }

            // Check if the event is a window state change (e.g., opening an Activity)
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val className = event.className?.toString() ?: ""
                
                if (detector.isMediaViewer(packageName, className)) {
                    if (isMarkerDetected) {
                        if (throttler.shouldCapture()) {
                            throttler.onCaptureStarted()
                            logger.i("!!! VIEWER OPENED - TRIGGERING CAPTURE !!! for $packageName", "AccessibilityMonitor")
                            
                            ContextCompat.getMainExecutor(applicationContext).execute {
                                Toast.makeText(applicationContext, "📸 Cattura automatica attiva: $packageName", Toast.LENGTH_SHORT).show()
                            }
                            
                            captureEngine.captureViaAccessibility(this, packageName)
                            
                            // Reset marker detection after capture
                            isMarkerDetected = false
                        }
                    } else {
                        logger.d("Viewer detected but no marker seen recently in $packageName", "AccessibilityMonitor")
                    }
                } else if (isMarkerDetected && !className.contains("Toast") && !className.contains("Popup") && !className.contains("Conversation")) {
                    // Reset marker if we detect a window change that is NOT the viewer and NOT a conversation
                    // This helps prevent getting stuck in 'pending' state if user exits the chat
                    isMarkerDetected = false
                }
            }
        }
    }

    override fun onInterrupt() {
        logger.w("Accessibility Service Interrupted", "AccessibilityMonitor")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = info.flags or 
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        
        this.serviceInfo = info

        logger.i("Strong Mode Accessibility Connected & Configured", "AccessibilityMonitor")
        diagnostics.logSystemInfo()
    }
}
