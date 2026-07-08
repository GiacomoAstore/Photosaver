package com.example.savemedia.domain.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.savemedia.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnhancedContentDetector @Inject constructor(
    private val logger: AppLogger
) {
    private val supportedPackages = listOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.instagram.android",
        "com.snapchat.android"
    )

    private val viewOnceMarkers = listOf(
        "visualizza una volta",
        "view once",
        "foto",
        "video",
        "photo",
        "immagine",
        "vedi una volta",
        "snap",
        "tocca per visualizzare",
        "tieni premuto per vedere"
    )

    fun isTargetApp(packageName: String): Boolean = supportedPackages.contains(packageName)

    fun detectViewOnce(event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        val packageName = event.packageName?.toString() ?: ""

        // Strategy 1: Viewer Activity Detection (Full Screen)
        if (isViewOnceActivity(packageName, className)) {
            logger.i("Detected Media Viewer Activity: $className", "EnhancedDetector")
            return true
        }

        return false
    }

    fun isMediaViewer(pkg: String, cls: String): Boolean {
        return isViewOnceActivity(pkg, cls)
    }

    fun hasViewOnceMarker(rootNode: AccessibilityNodeInfo?): Boolean {
        return rootNode != null && findTextMarker(rootNode)
    }

    private fun isViewOnceActivity(pkg: String, cls: String): Boolean {
        return when {
            pkg.contains("whatsapp") -> cls.contains("PhotoViewer") || cls.contains("ViewOnce") || cls.contains("Gallery") || cls.contains("MediaView")
            pkg.contains("telegram") -> cls.contains("MediaViewer") || cls.contains("VideoPlayer") || cls.contains("PhotoViewer")
            pkg.contains("instagram") -> cls.contains("DirectStoryViewer") || cls.contains("VisualViewer")
            pkg.contains("snapchat") -> cls.contains("SnapActivity") || cls.contains("StoryPlayer")
            else -> false
        }
    }

    private fun findTextMarker(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        if (viewOnceMarkers.any { text.contains(it) }) return true
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findTextMarker(child)) return true
        }
        return false
    }
}
